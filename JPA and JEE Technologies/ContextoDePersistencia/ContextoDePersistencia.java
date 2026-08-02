//JPA - ENTITYMANAGER, CONTEXTO DE PERSISTÊNCIA E CONSULTAS
//NÃO compila sem jakarta.persistence-api. As entidades (Cliente, Pedido, StatusPedido) são as da
//pasta MapeamentoObjetoRelacional.
//O contexto de persistência é um cache de primeiro nível: um conjunto de entidades GERENCIADAS.
//Enquanto estão lá, o provedor observa as mudanças e as grava sozinho no fim da transação - é por
//isso que existe código que altera um objeto, não chama nenhum "update", e funciona.

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

//CICLO DE VIDA - saber em qual estado o objeto está explica quase todo comportamento "estranho".
//  NEW/TRANSIENT - criado com new, sem id, fora do contexto. Não é gravado.
//  MANAGED       - dentro do contexto; alterações são detectadas e gravadas no flush.
//  DETACHED      - já teve identidade no banco, mas o contexto fechou. Alterações NÃO são gravadas.
//  REMOVED       - marcado para exclusão; o DELETE sai no flush.
//
//  new ─persist()→ MANAGED ─remove()→ REMOVED
//                    │ ↑
//     close/clear/detach │ merge()
//                    ↓ │
//                 DETACHED

class RepositorioPedidoJpa {

    // Em ambiente gerenciado o EntityManager é INJETADO e o contêiner cuida do ciclo de vida e da
    // transação. É o caminho normal.
    @PersistenceContext
    private EntityManager em;

    // persist: entidade NOVA vira gerenciada. Não necessariamente executa o INSERT na hora.
    public void inserir(Pedido pedido) {
        em.persist(pedido);
    }

    // find: consulta o contexto ANTES do banco - se já estiver gerenciada, devolve a MESMA
    // instância sem ir ao banco. Devolve null se não existir.
    public Pedido porId(Long id) {
        return em.find(Pedido.class, id);
    }

    // getReference: devolve um PROXY sem tocar no banco; o SELECT só acontece no primeiro getter,
    // e aí lança EntityNotFoundException se não existir. Útil para associar uma FK.
    public Pedido referencia(Long id) {
        return em.getReference(Pedido.class, id);
    }

    // merge NÃO gerencia o objeto que você passou - ele devolve OUTRA instância, essa sim
    // gerenciada. Continuar mexendo no objeto original não grava nada.
    public Pedido atualizar(Pedido destacado) {
        return em.merge(destacado);   // use o retorno, não o argumento
    }

    public void excluir(Long id) {
        Pedido pedido = em.find(Pedido.class, id);
        if (pedido != null) {
            em.remove(pedido);   // remove() exige entidade GERENCIADA
        }
    }

    // DIRTY CHECKING: nenhum "update" é chamado. O provedor compara o estado no flush e emite o
    // UPDATE apenas se algo mudou.
    public void promover(Long clienteId) {
        Cliente cliente = em.find(Cliente.class, clienteId);
        cliente.promoverParaVip();
    }

    // flush força a sincronização agora; clear esvazia o contexto (tudo vira DETACHED);
    // detach remove uma entidade; refresh recarrega do banco descartando as alterações.
    public void processarLoteGrande(List<Pedido> pedidos) {
        for (int i = 0; i < pedidos.size(); i++) {
            em.persist(pedidos.get(i));

            // Sem isso, o contexto acumula o lote inteiro e a memória estoura.
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    // JPQL consulta o MODELO DE OBJETOS: "Pedido" é a ENTIDADE e "p.cliente.nome" é NAVEGAÇÃO.
    // Quem traduz para junções e nomes de coluna é o provedor.
    public List<Pedido> porNomeDoCliente(String nome) {
        TypedQuery<Pedido> query = em.createQuery(
                "SELECT p FROM Pedido p WHERE p.cliente.nome LIKE :nome ORDER BY p.codigo",
                Pedido.class);

        // Sempre parâmetro nomeado: concatenar String abre SQL injection e impede o cache de plano.
        query.setParameter("nome", "%" + nome + "%");

        return query.getResultList();
    }

    // getSingleResult lança NoResultException quando não encontra e NonUniqueResultException quando
    // encontra mais de um.
    public Pedido porCodigo(String codigo) {
        try {
            return em.createQuery("SELECT p FROM Pedido p WHERE p.codigo = :codigo", Pedido.class)
                     .setParameter("codigo", codigo)
                     .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // PROBLEMA N+1: traz N pedidos em 1 SELECT, mas se depois o código acessar
    // pedido.getCliente().getNome(), sai 1 SELECT por pedido. Com 500 pedidos, 501 idas ao banco.
    public List<Pedido> todosComProblemaNMaisUm() {
        return em.createQuery("SELECT p FROM Pedido p", Pedido.class).getResultList();
    }

    // Solução: JOIN FETCH traz a associação na MESMA consulta. DISTINCT é necessário no fetch de
    // coleção, senão o pedido se repete uma vez por item.
    public List<Pedido> todosComClienteEItens() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Pedido p "
                + "JOIN FETCH p.cliente c "
                + "LEFT JOIN FETCH p.itens i "
                + "WHERE p.status = :status", Pedido.class)
                .setParameter("status", StatusPedido.PAGO)
                .getResultList();
    }

    // Projeção com construtor: traz só as colunas necessárias e devolve um DTO, evitando expor a
    // entidade para fora da camada de persistência.
    public List<ResumoPedido> resumos() {
        return em.createQuery(
                "SELECT new ResumoPedido(p.codigo, p.cliente.nome, SUM(i.quantidade)) "
                + "FROM Pedido p JOIN p.itens i GROUP BY p.codigo, p.cliente.nome",
                ResumoPedido.class)
                .getResultList();
    }

    public List<Pedido> pagina(int numeroDaPagina, int tamanho) {
        return em.createQuery("SELECT p FROM Pedido p ORDER BY p.id", Pedido.class)
                 .setFirstResult(numeroDaPagina * tamanho)
                 .setMaxResults(tamanho)
                 .getResultList();
    }

    // CRITERIA API: JPQL é String e o erro de digitação só aparece em execução. A Criteria é
    // verbosa, mas o compilador acusa - e é a forma correta de montar filtros DINÂMICOS.
    public List<Pedido> buscar(String nomeCliente, StatusPedido status, BigDecimal valorMinimo) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Pedido> consulta = cb.createQuery(Pedido.class);
        Root<Pedido> pedido = consulta.from(Pedido.class);

        List<Predicate> filtros = new java.util.ArrayList<>();

        if (nomeCliente != null && !nomeCliente.isBlank()) {
            Join<Pedido, Cliente> cliente = pedido.join("cliente");
            filtros.add(cb.like(cliente.get("nome"), "%" + nomeCliente + "%"));
        }
        if (status != null) {
            filtros.add(cb.equal(pedido.get("status"), status));
        }
        if (valorMinimo != null) {
            filtros.add(cb.greaterThanOrEqualTo(pedido.get("total"), valorMinimo));
        }

        consulta.select(pedido)
                .where(cb.and(filtros.toArray(new Predicate[0])))
                .orderBy(cb.desc(pedido.get("id")));

        return em.createQuery(consulta).getResultList();
    }

    // OTIMISTA (@Version) não trava nada e detecta o conflito no commit - é o padrão para web.
    // PESSIMISTA trava a linha (SELECT ... FOR UPDATE); use só em transações curtas.
    public Pedido paraAtualizacaoExclusiva(Long id) {
        return em.find(Pedido.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}

// Precisa do construtor com a assinatura exata usada no "SELECT new".
class ResumoPedido {

    private final String codigo;
    private final String nomeCliente;
    private final Long totalDeItens;

    public ResumoPedido(String codigo, String nomeCliente, Long totalDeItens) {
        this.codigo = codigo;
        this.nomeCliente = nomeCliente;
        this.totalDeItens = totalDeItens;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public Long getTotalDeItens() {
        return totalDeItens;
    }
}

// Fora de um servidor, você mesmo controla fábrica, EntityManager e transação.
class UsoEmJavaSe {

    public static void main(String[] args) {

        // A fábrica é CARA e THREAD-SAFE: uma por aplicação. O nome vem da unidade de persistência
        // declarada em META-INF/persistence.xml.
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lojaPU");

        // O EntityManager é BARATO e NÃO é thread-safe: um por requisição.
        EntityManager em = emf.createEntityManager();
        EntityTransaction transacao = em.getTransaction();

        try {
            transacao.begin();

            Cliente cliente = new Cliente("Ana Souza", "11122233344");
            Pedido pedido = new Pedido("PED-1");
            cliente.adicionarPedido(pedido);

            // Só o cliente é persistido: os pedidos vão junto por CascadeType.ALL.
            em.persist(cliente);

            transacao.commit();   // é aqui que o flush acontece e o SQL sai de verdade

        } catch (RuntimeException e) {
            if (transacao.isActive()) {
                transacao.rollback();
            }
            throw e;
        } finally {
            em.close();   // as entidades passam a DETACHED
            emf.close();
        }

        // Depois do close, tocar em uma associação LAZY lança LazyInitializationException - o erro
        // mais frequente de quem usa JPA. As saídas: JOIN FETCH ainda dentro da transação, ou
        // converter para DTO antes de sair da camada de serviço.
    }
}

//O QUE MAIS QUEBRA
//persist x merge     - persist é para entidade nova; o gerenciado do merge é o RETORNO.
//find x getReference - find vai ao banco e devolve null; getReference devolve proxy e só falha no
//                      primeiro acesso.
//Cache de 1º nível   - o contexto de persistência, por EntityManager, sempre ativo.
//Cache de 2º nível   - opcional, compartilhado entre contextos.
//Sem transação ativa, persist/merge/remove não gravam nada.
//
//Padrões por trás: o EntityManager é um Facade sobre JDBC; as entidades LAZY são Proxies; o
//contexto implementa Unit of Work e Identity Map.

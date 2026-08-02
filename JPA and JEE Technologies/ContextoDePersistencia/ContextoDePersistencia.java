//JPA - ENTITYMANAGER, CONTEXTO DE PERSISTÊNCIA E CONSULTAS

//ATENÇÃO: material de estudo. NÃO compila sem jakarta.persistence-api no classpath.
//As entidades usadas aqui (Cliente, Pedido, StatusPedido) são as da pasta
//MapeamentoObjetoRelacional - num projeto real elas estariam no mesmo pacote.

//Se o mapeamento diz COMO a classe vira tabela, este arquivo trata de QUEM executa isso. O
//EntityManager é a interface central da JPA e o ponto onde a maioria dos erros acontece - não por
//sintaxe, mas por não entender o CONTEXTO DE PERSISTÊNCIA.

//O contexto de persistência é um cache de primeiro nível: um conjunto de entidades GERENCIADAS.
//Enquanto uma entidade está lá dentro, o provedor observa as mudanças feitas nela e as grava
//sozinho no fim da transação. É por isso que existe código que altera um objeto e não chama
//nenhum "update" - e funciona.

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

//CICLO DE VIDA DA ENTIDADE - os quatro estados. Saber em qual estado o objeto está explica
//praticamente todo comportamento "estranho" da JPA.
//
//  NEW / TRANSIENT - recém criado com new. Sem id, fora do contexto. Não é gravado.
//  MANAGED         - dentro do contexto. Alterações são detectadas e gravadas no flush.
//  DETACHED        - já teve identidade no banco, mas o contexto fechou ou foi limpo.
//                    Alterações NÃO são gravadas. É o estado de um objeto que atravessou camadas.
//  REMOVED         - marcado para exclusão; o DELETE sai no flush.
//
//  new ─persist()→ MANAGED ─remove()→ REMOVED
//                    │ ↑
//         close/clear/detach │ merge()
//                    ↓ │
//                 DETACHED

class RepositorioPedidoJpa {

    // Em ambiente gerenciado (servidor Jakarta EE, Spring), o EntityManager é INJETADO e o
    // contêiner cuida do ciclo de vida e da transação. Este é o caminho normal.
    @PersistenceContext
    private EntityManager em;

    // ---------------------------------------------------------------------
    // OPERAÇÕES BÁSICAS
    // ---------------------------------------------------------------------

    // persist: passa uma entidade NOVA para o estado gerenciado.
    // Não necessariamente executa o INSERT na hora - normalmente ele sai no flush.
    // Lançará EntityExistsException se o objeto já tiver identidade no contexto.
    public void inserir(Pedido pedido) {
        em.persist(pedido);
    }

    // find: busca pela chave primária. Consulta o contexto ANTES do banco - se a entidade já
    // estiver gerenciada, devolve a MESMA instância, sem ir ao banco.
    // Devolve null se não existir.
    public Pedido porId(Long id) {
        return em.find(Pedido.class, id);
    }

    // getReference: devolve um PROXY sem tocar no banco. Só quando algum getter for chamado é que
    // o SELECT acontece - e aí, se não existir, lança EntityNotFoundException.
    // Útil para associar uma FK sem precisar carregar o objeto inteiro.
    public Pedido referencia(Long id) {
        return em.getReference(Pedido.class, id);
    }

    // merge: copia o estado de uma entidade DETACHED para dentro do contexto.
    // Erro clássico: merge NÃO gerencia o objeto que você passou - ele devolve OUTRA instância,
    // essa sim gerenciada. Continuar mexendo no objeto original não grava nada.
    public Pedido atualizar(Pedido destacado) {
        return em.merge(destacado);   // use o retorno, não o argumento
    }

    public void excluir(Long id) {
        Pedido pedido = em.find(Pedido.class, id);
        if (pedido != null) {
            em.remove(pedido);   // remove() exige entidade GERENCIADA
        }
    }

    // DIRTY CHECKING: nenhum "update" é chamado. Como a entidade está gerenciada, o provedor
    // compara o estado no flush e emite o UPDATE apenas se algo mudou.
    public void promover(Long clienteId) {
        Cliente cliente = em.find(Cliente.class, clienteId);
        cliente.promoverParaVip();
        // fim do método: no commit da transação, sai o UPDATE automaticamente
    }

    // flush  - força a sincronização com o banco AGORA, sem encerrar a transação.
    // clear  - esvazia o contexto; tudo que estava gerenciado passa a DETACHED.
    // detach - remove uma entidade específica do contexto.
    // refresh- recarrega a entidade do banco, descartando as alterações em memória.
    public void processarLoteGrande(List<Pedido> pedidos) {
        for (int i = 0; i < pedidos.size(); i++) {
            em.persist(pedidos.get(i));

            // Sem isso, o contexto acumula todas as entidades do lote e a memória estoura.
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    // ---------------------------------------------------------------------
    // JPQL - consulta sobre o MODELO DE OBJETOS, não sobre tabelas
    // ---------------------------------------------------------------------

    // Repare: "Pedido" é a ENTIDADE e "p.cliente.nome" é NAVEGAÇÃO entre objetos. Quem traduz
    // isso para junções e nomes de coluna é o provedor.
    public List<Pedido> porNomeDoCliente(String nome) {
        TypedQuery<Pedido> query = em.createQuery(
                "SELECT p FROM Pedido p WHERE p.cliente.nome LIKE :nome ORDER BY p.codigo",
                Pedido.class);

        // SEMPRE parâmetro nomeado, nunca concatenação de String: concatenar abre SQL injection e
        // impede o cache de plano de execução.
        query.setParameter("nome", "%" + nome + "%");

        return query.getResultList();
    }

    // getSingleResult lança NoResultException quando não encontra e NonUniqueResultException
    // quando encontra mais de um. Em muitos casos é mais limpo usar getResultStream().findFirst().
    public Pedido porCodigo(String codigo) {
        try {
            return em.createQuery("SELECT p FROM Pedido p WHERE p.codigo = :codigo", Pedido.class)
                     .setParameter("codigo", codigo)
                     .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // O PROBLEMA N+1
    // Esta consulta traz N pedidos em 1 SELECT. Se depois o código percorrer os pedidos acessando
    // pedido.getCliente().getNome(), sairá 1 SELECT por pedido - N+1 no total. Com 500 pedidos,
    // são 501 idas ao banco.
    public List<Pedido> todosComProblemaNMaisUm() {
        return em.createQuery("SELECT p FROM Pedido p", Pedido.class).getResultList();
    }

    // A SOLUÇÃO: JOIN FETCH traz a associação na MESMA consulta.
    // DISTINCT é necessário quando o fetch é de coleção, senão o pedido se repete uma vez por item.
    public List<Pedido> todosComClienteEItens() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Pedido p "
                + "JOIN FETCH p.cliente c "
                + "LEFT JOIN FETCH p.itens i "
                + "WHERE p.status = :status", Pedido.class)
                .setParameter("status", StatusPedido.PAGO)
                .getResultList();
    }

    // Projeção com construtor: traz SÓ as colunas necessárias, em vez de entidades inteiras.
    // Devolve um DTO, o que também evita expor a entidade para fora da camada de persistência.
    public List<ResumoPedido> resumos() {
        return em.createQuery(
                "SELECT new ResumoPedido(p.codigo, p.cliente.nome, SUM(i.quantidade)) "
                + "FROM Pedido p JOIN p.itens i GROUP BY p.codigo, p.cliente.nome",
                ResumoPedido.class)
                .getResultList();
    }

    // Agregação e paginação.
    public List<Pedido> pagina(int numeroDaPagina, int tamanho) {
        return em.createQuery("SELECT p FROM Pedido p ORDER BY p.id", Pedido.class)
                 .setFirstResult(numeroDaPagina * tamanho)
                 .setMaxResults(tamanho)
                 .getResultList();
    }

    // ---------------------------------------------------------------------
    // CRITERIA API - consulta montada por objetos, verificada pelo COMPILADOR
    // ---------------------------------------------------------------------

    // JPQL é String: um erro de digitação só aparece em tempo de execução. A Criteria API é
    // verbosa, mas o compilador acusa o erro - e é a forma correta de montar filtros DINÂMICOS,
    // porque concatenar String de JPQL com if é frágil e perigoso.
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

    // ---------------------------------------------------------------------
    // BLOQUEIO (LOCKING)
    // ---------------------------------------------------------------------

    // OTIMISTA (@Version): não trava nada; detecta o conflito no commit. É o padrão para
    // aplicações web, onde a leitura e a gravação ficam separadas por muito tempo.
    // PESSIMISTA: trava a linha no banco (SELECT ... FOR UPDATE). Use apenas em transações curtas
    // e quando a concorrência sobre a mesma linha for realmente alta.
    public Pedido paraAtualizacaoExclusiva(Long id) {
        return em.find(Pedido.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}

// DTO usado nas projeções. Precisa do construtor com a assinatura exata usada no "SELECT new".
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

// APLICAÇÃO JAVA SE - fora de um servidor, você mesmo controla fábrica, EntityManager e transação.
// É o cenário de estudo e de testes; em produção com Jakarta EE ou Spring, tudo isso é injetado.
class UsoEmJavaSe {

    public static void main(String[] args) {

        // A fábrica é CARA de criar e THREAD-SAFE: uma por aplicação, criada na subida.
        // O nome vem da unidade de persistência declarada em META-INF/persistence.xml.
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lojaPU");

        // O EntityManager é BARATO e NÃO é thread-safe: um por requisição/unidade de trabalho.
        EntityManager em = emf.createEntityManager();
        EntityTransaction transacao = em.getTransaction();

        try {
            transacao.begin();

            Cliente cliente = new Cliente("Ana Souza", "11122233344");
            Pedido pedido = new Pedido("PED-1");
            cliente.adicionarPedido(pedido);   // sincroniza os dois lados

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

        // Depois do close, tocar em uma associação LAZY lança LazyInitializationException. É o
        // erro mais frequente de quem usa JPA: a entidade viajou para a camada de apresentação
        // com o contexto já fechado. As saídas corretas são trazer o necessário com JOIN FETCH
        // ainda dentro da transação, ou converter para DTO antes de sair da camada de serviço.
    }
}

//RESUMO DO QUE MAIS CAI E MAIS QUEBRA
//persist x merge  - persist é para entidade nova; merge é para entidade destacada, e o objeto
//                   gerenciado é o RETORNO, não o argumento.
//find x getReference - find vai ao banco (ou ao cache) e devolve null se não achar;
//                   getReference devolve proxy e só falha quando for acessado.
//LazyInitializationException - associação LAZY acessada com o contexto fechado.
//N+1 - resolvido com JOIN FETCH, @EntityGraph ou batch size.
//Cache de 1º nível - o contexto de persistência, por EntityManager, sempre ativo.
//Cache de 2º nível - opcional, compartilhado entre contextos, configurado no provedor.
//Transação - sem transação ativa, persist/merge/remove não gravam nada.
//
//Ligação com os padrões: o EntityManager é um Facade sobre JDBC; as entidades LAZY são Proxies;
//o contexto de persistência implementa Unit of Work e Identity Map; e o conjunto todo é
//Protected Variations, isolando o domínio do banco de dados concreto.

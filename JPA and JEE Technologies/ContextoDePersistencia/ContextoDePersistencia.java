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

class RepositorioPedidoJpa {

    @PersistenceContext
    private EntityManager em;

    public void inserir(Pedido pedido) {
        em.persist(pedido);
    }

    public Pedido porId(Long id) {
        return em.find(Pedido.class, id);
    }

    public Pedido referencia(Long id) {
        return em.getReference(Pedido.class, id);
    }

    public Pedido atualizar(Pedido destacado) {
        return em.merge(destacado);
    }

    public void excluir(Long id) {
        Pedido pedido = em.find(Pedido.class, id);
        if (pedido != null) {
            em.remove(pedido);
        }
    }

    public void promover(Long clienteId) {
        Cliente cliente = em.find(Cliente.class, clienteId);
        cliente.promoverParaVip();
    }

    public void processarLoteGrande(List<Pedido> pedidos) {
        for (int i = 0; i < pedidos.size(); i++) {
            em.persist(pedidos.get(i));

            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    public List<Pedido> porNomeDoCliente(String nome) {
        TypedQuery<Pedido> query = em.createQuery(
                "SELECT p FROM Pedido p WHERE p.cliente.nome LIKE :nome ORDER BY p.codigo",
                Pedido.class);

        query.setParameter("nome", "%" + nome + "%");

        return query.getResultList();
    }

    public Pedido porCodigo(String codigo) {
        try {
            return em.createQuery("SELECT p FROM Pedido p WHERE p.codigo = :codigo", Pedido.class)
                     .setParameter("codigo", codigo)
                     .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Pedido> todosComProblemaNMaisUm() {
        return em.createQuery("SELECT p FROM Pedido p", Pedido.class).getResultList();
    }

    public List<Pedido> todosComClienteEItens() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Pedido p "
                + "JOIN FETCH p.cliente c "
                + "LEFT JOIN FETCH p.itens i "
                + "WHERE p.status = :status", Pedido.class)
                .setParameter("status", StatusPedido.PAGO)
                .getResultList();
    }

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

    public Pedido paraAtualizacaoExclusiva(Long id) {
        return em.find(Pedido.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}

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

class UsoEmJavaSe {

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("lojaPU");

        EntityManager em = emf.createEntityManager();
        EntityTransaction transacao = em.getTransaction();

        try {
            transacao.begin();

            Cliente cliente = new Cliente("Ana Souza", "11122233344");
            Pedido pedido = new Pedido("PED-1");
            cliente.adicionarPedido(pedido);

            em.persist(cliente);

            transacao.commit();

        } catch (RuntimeException e) {
            if (transacao.isActive()) {
                transacao.rollback();
            }
            throw e;
        } finally {
            em.close();
            emf.close();
        }

    }
}

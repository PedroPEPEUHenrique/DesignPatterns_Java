//JAKARTA EE - CAMADA DE NEGÓCIO: CDI, EJB, TRANSAÇÕES E INTERCEPTADORES
//NÃO compila sem as APIs do Jakarta EE (cdi-api, ejb-api, transaction-api, inject-api). Em Java EE
//8 e anteriores os pacotes começam com javax. As entidades Pedido e StatusPedido são as da pasta
//MapeamentoObjetoRelacional.
//Numa aplicação corporativa, boa parte do trabalho não é regra de negócio - é transação,
//segurança, concorrência, pool e ciclo de vida. A plataforma inverte a responsabilidade: você
//DECLARA o que quer com anotações e o CONTÊINER executa o COMO em volta do seu código. Cada
//serviço do contêiner é, na prática, um Proxy ou Decorator gerado em tempo de execução.

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ApplicationScoped
class TabelaDeFrete {

    private final java.util.Map<String, Integer> tabela = new java.util.HashMap<>();

    @PostConstruct
    void carregar() {
        tabela.put("SUDESTE", 1800);
        tabela.put("SUL", 2200);
        tabela.put("NORDESTE", 3400);
        Logger.getLogger("frete").info("tabela de frete carregada");
    }

    @PreDestroy
    void liberar() {
        tabela.clear();
    }

    int valorEmCentavos(String regiao) {
        return tabela.getOrDefault(regiao, 5000);
    }
}

@Qualifier
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE,
                              java.lang.annotation.ElementType.FIELD,
                              java.lang.annotation.ElementType.PARAMETER})
@interface Pix {
}

@Qualifier
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE,
                              java.lang.annotation.ElementType.FIELD,
                              java.lang.annotation.ElementType.PARAMETER})
@interface Cartao {
}

interface MeioDePagamento {
    String cobrar(BigDecimal valor);
}

@Pix
@ApplicationScoped
class PagamentoPix implements MeioDePagamento {

    @Override
    public String cobrar(BigDecimal valor) {
        return "PIX-" + valor;
    }
}

@Cartao
@ApplicationScoped
class PagamentoCartao implements MeioDePagamento {

    @Override
    public String cobrar(BigDecimal valor) {
        return "CARD-" + valor;
    }
}

@ApplicationScoped
class ConfiguracaoProducer {

    @Produces
    @ApplicationScoped
    Logger criarLogger() {
        return Logger.getLogger("aplicacao");
    }

    @Produces
    @Named("percentualComissao")
    BigDecimal comissao() {
        return new BigDecimal("0.05");
    }
}

@InterceptorBinding
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE,
                              java.lang.annotation.ElementType.METHOD})
@interface Auditado {
}

@Auditado
@Interceptor
class InterceptadorAuditoria {

    @Inject
    private Logger log;

    @AroundInvoke
    Object auditar(InvocationContext contexto) throws Exception {
        long inicio = System.currentTimeMillis();
        log.info("entrando em " + contexto.getMethod().getName());

        try {
            return contexto.proceed();
        } finally {
            log.info("saindo de " + contexto.getMethod().getName()
                     + " em " + (System.currentTimeMillis() - inicio) + "ms");
        }
    }
}

@Stateless
class ServicoPedido {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private TabelaDeFrete tabelaDeFrete;

    @Inject
    @Pix
    private MeioDePagamento pagamento;

    @Inject
    private Logger log;

    @Inject
    private Event<PedidoConfirmado> pedidoConfirmado;

    @Auditado
    @Transactional(Transactional.TxType.REQUIRED)
    public String confirmar(@NotNull Long pedidoId, @Positive BigDecimal valor, String regiao) {

        Pedido pedido = em.find(Pedido.class, pedidoId);
        if (pedido == null) {
            throw new IllegalArgumentException("pedido inexistente: " + pedidoId);
        }

        int frete = tabelaDeFrete.valorEmCentavos(regiao);
        String comprovante = pagamento.cobrar(valor);

        pedido.avancarPara(StatusPedido.PAGO);

        log.info("pedido " + pedidoId + " confirmado, frete " + frete);

        pedidoConfirmado.fire(new PedidoConfirmado(pedidoId, valor));

        return comprovante;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrarTentativa(Long pedidoId, String motivo) {
        log.warning("tentativa em " + pedidoId + ": " + motivo);
    }

    public List<Pedido> pendentes() {
        return em.createQuery("SELECT p FROM Pedido p WHERE p.status = :s", Pedido.class)
                 .setParameter("s", StatusPedido.NOVO)
                 .getResultList();
    }
}

class PedidoConfirmado {

    private final Long pedidoId;
    private final BigDecimal valor;

    PedidoConfirmado(Long pedidoId, BigDecimal valor) {
        this.pedidoId = pedidoId;
        this.valor = valor;
    }

    Long getPedidoId() {
        return pedidoId;
    }

    BigDecimal getValor() {
        return valor;
    }
}

@ApplicationScoped
class NotificadorDeConfirmacao {

    @Inject
    private Logger log;

    void aoConfirmar(@Observes PedidoConfirmado evento) {
        log.info("enviando e-mail de confirmação do pedido " + evento.getPedidoId());
    }
}

@ApplicationScoped
class IntegracaoContabil {

    void aoConfirmar(@Observes PedidoConfirmado evento) {
        Logger.getLogger("contabil").info("lançando receita de " + evento.getValor());
    }
}

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
class MonitorDeIntegracoes {

    @Resource
    private jakarta.ejb.TimerService timerService;

    @Inject
    private Logger log;

    private int execucoes;

    @PostConstruct
    void iniciar() {
        log.info("monitor de integrações no ar");
    }

    @Schedule(hour = "*", minute = "*/15", persistent = false)
    void verificarPendencias() {
        execucoes++;
        log.info("verificação periódica #" + execucoes);
    }
}

@Named("pedidoBean")
@RequestScoped
class PedidoBean {

    @Inject
    private ServicoPedido servicoPedido;

    private Long pedidoId;
    private String mensagem;

    public String confirmar() {
        try {
            String comprovante = servicoPedido.confirmar(pedidoId, new BigDecimal("199.90"), "SUDESTE");
            mensagem = "confirmado: " + comprovante;
            return "sucesso";
        } catch (IllegalArgumentException e) {
            mensagem = "erro: " + e.getMessage();
            return "erro";
        }
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getMensagem() {
        return mensagem;
    }
}

@SessionScoped
class CarrinhoSessao implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> skus = new java.util.ArrayList<>();

    public void adicionar(String sku) {
        skus.add(sku);
    }

    public List<String> getSkus() {
        return skus;
    }
}

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

// ESCOPOS CDI - quanto tempo a instância vive e para quem é compartilhada:
//  @Dependent (padrão) - uma por ponto de injeção
//  @RequestScoped      - uma por requisição HTTP
//  @SessionScoped      - uma por sessão (exige Serializable)
//  @ApplicationScoped  - uma por aplicação; é o Singleton do GoF, gerenciado
//  @ConversationScoped - entre requisição e sessão, controlado pela aplicação

@ApplicationScoped
class TabelaDeFrete {

    private final java.util.Map<String, Integer> tabela = new java.util.HashMap<>();

    // Chamado UMA vez, depois das injeções. No construtor os campos injetados ainda são nulos.
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

// QUALIFICADORES - resolvem a ambiguidade quando há mais de uma implementação da mesma interface.
// Sem eles, o contêiner falha na subida com "ambiguous dependencies". O cliente pede um papel,
// não uma classe.
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

// PRODUTOR - fabrica beans que você não controla ou cuja criação depende de lógica.
// É o Factory Method integrado ao contêiner.
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

// INTERCEPTADORES - o mecanismo por trás de tudo que a plataforma faz "em volta" do seu código.
// É o Decorator aplicado por proxy dinâmico.
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

    // Não chamar proceed() CANCELA a execução do método real - é assim que um interceptador de
    // segurança bloqueia uma chamada.
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

// @Stateless - o contêiner mantém um POOL e entrega qualquer instância a cada chamada. Logo, NÃO
// guarde estado de conversa em atributos: a próxima chamada do mesmo usuário pode cair em outra
// instância. Todo método público é transacional por padrão (REQUIRED).
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

    // Evento CDI: publica um fato sem conhecer quem escuta. É o Observer, com o registro de
    // observadores feito pelo contêiner.
    @Inject
    private Event<PedidoConfirmado> pedidoConfirmado;

    // Transação DECLARATIVA: sem begin, commit ou rollback no código. RuntimeException não tratada
    // provoca rollback automático; exceções CHECKED, por padrão, NÃO provocam.
    @Auditado
    @Transactional(Transactional.TxType.REQUIRED)
    public String confirmar(@NotNull Long pedidoId, @Positive BigDecimal valor, String regiao) {

        Pedido pedido = em.find(Pedido.class, pedidoId);
        if (pedido == null) {
            throw new IllegalArgumentException("pedido inexistente: " + pedidoId);
        }

        int frete = tabelaDeFrete.valorEmCentavos(regiao);
        String comprovante = pagamento.cobrar(valor);

        pedido.avancarPara(StatusPedido.PAGO);   // dirty checking grava sozinho no commit

        log.info("pedido " + pedidoId + " confirmado, frete " + frete);

        pedidoConfirmado.fire(new PedidoConfirmado(pedidoId, valor));

        return comprovante;
    }

    // ATRIBUTOS DE TRANSAÇÃO - o que fazer com a transação do CHAMADOR:
    //  REQUIRED (padrão) - usa a existente ou cria uma
    //  REQUIRES_NEW      - suspende a do chamador e abre outra; para log que deve sobreviver ao
    //                      rollback da transação principal
    //  MANDATORY         - exige transação em curso, senão lança exceção
    //  SUPPORTS          - participa se houver
    //  NOT_SUPPORTED     - suspende a existente
    //  NEVER             - lança exceção se houver transação
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

    // @Observes registra o observador. Nada precisa ser cadastrado em lugar nenhum.
    void aoConfirmar(@Observes PedidoConfirmado evento) {
        log.info("enviando e-mail de confirmação do pedido " + evento.getPedidoId());
    }
}

@ApplicationScoped
class IntegracaoContabil {

    // Segundo observador do MESMO evento; o ServicoPedido não ficou sabendo que ele existe.
    void aoConfirmar(@Observes PedidoConfirmado evento) {
        Logger.getLogger("contabil").info("lançando receita de " + evento.getValor());
    }
}

// @Stateful mantém estado de conversa entre chamadas do MESMO cliente (carrinho de compras). Custa
// memória e complica o balanceamento; hoje se prefere estado no cliente ou em cache distribuído.

// @Singleton do EJB (diferente de @ApplicationScoped do CDI): instância única com controle de
// CONCORRÊNCIA pelo contêiner.
@Singleton
@Startup   // instanciado na subida, não na primeira chamada
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

    // Temporizador declarativo, no lugar de Thread, Timer ou agendador externo. Em cluster, atenção
    // para não rodar em todos os nós.
    @Schedule(hour = "*", minute = "*/15", persistent = false)
    void verificarPendencias() {
        execucoes++;
        log.info("verificação periódica #" + execucoes);
    }
}

// @Named expõe o bean por nome para a camada de visão (JSF).
@Named("pedidoBean")
@RequestScoped
class PedidoBean {

    @Inject
    private ServicoPedido servicoPedido;

    private Long pedidoId;
    private String mensagem;

    // Sem regra de negócio: coleta a entrada, delega e prepara a saída. É o Controller do GRASP.
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

    // @SessionScoped e @ConversationScoped EXIGEM Serializable: o contêiner pode passivar a sessão
    // para disco ou replicá-la no cluster.
    private static final long serialVersionUID = 1L;

    private final List<String> skus = new java.util.ArrayList<>();

    public void adicionar(String sku) {
        skus.add(sku);
    }

    public List<String> getSkus() {
        return skus;
    }
}

//OS PADRÕES POR TRÁS DAS ANOTAÇÕES
//@Inject                    - Dependency Injection = Indirection + Protected Variations
//@ApplicationScoped         - Singleton gerenciado, sem os problemas do estado global estático
//@Produces                  - Factory Method
//@Observes / Event          - Observer
//@Interceptor / @Decorator  - Decorator e Proxy por proxy dinâmico
//@Transactional             - Template Method: o contêiner roda begin/commit/rollback em volta
//Qualificadores             - Strategy escolhida por metadado em vez de por if

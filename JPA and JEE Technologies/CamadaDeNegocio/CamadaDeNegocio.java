//JAKARTA EE - CAMADA DE NEGÓCIO: CDI, EJB, TRANSAÇÕES E INTERCEPTADORES

//ATENÇÃO: material de estudo. NÃO compila sem as APIs do Jakarta EE no classpath
//(jakarta.enterprise.cdi-api, jakarta.ejb-api, jakarta.transaction-api, jakarta.inject-api).
//Em projetos Java EE 8 e anteriores os pacotes começam com javax em vez de jakarta.
//As entidades Pedido e StatusPedido são as da pasta MapeamentoObjetoRelacional.

//O que o Java EE / Jakarta EE resolve: em uma aplicação corporativa, boa parte do trabalho não é
//regra de negócio - é transação, segurança, concorrência, pool de conexões, ciclo de vida de
//objetos, log e monitoramento. Escrever isso em cada classe é repetição e fonte de erro.
//A plataforma inverte a responsabilidade: você declara O QUE quer com anotações e o CONTÊINER
//executa o COMO em volta do seu código.
//Isso é Inversão de Controle levada ao extremo - e cada serviço do contêiner é, na prática, um
//Proxy ou um Decorator gerado em tempo de execução ao redor da sua classe.

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

// ---------------------------------------------------------------------------
// CDI - CONTEXTS AND DEPENDENCY INJECTION
//
// É o modelo de componentes unificado da plataforma. Qualquer classe com construtor sem
// argumentos, dentro de um arquivo com beans.xml (ou com bean-discovery-mode="annotated"), é um
// bean gerenciado: o contêiner a instancia, injeta suas dependências e controla seu ciclo de vida.
// ---------------------------------------------------------------------------

// ESCOPOS - definem QUANTO TEMPO a instância vive e para QUEM ela é compartilhada.
//  @Dependent (padrão)  - uma instância por ponto de injeção, sem estado próprio
//  @RequestScoped       - uma por requisição HTTP
//  @SessionScoped       - uma por sessão de usuário (exige Serializable)
//  @ApplicationScoped   - uma por aplicação; é o Singleton do GoF, só que gerenciado
//  @ConversationScoped  - entre requisição e sessão, controlado pela aplicação

@ApplicationScoped
class TabelaDeFrete {

    private final java.util.Map<String, Integer> tabela = new java.util.HashMap<>();

    // Chamado UMA vez, depois que todas as injeções foram feitas. Não use o construtor para
    // inicialização que dependa de injeção - no construtor os campos injetados ainda são nulos.
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

// QUALIFICADORES - resolvem a ambiguidade quando existe mais de uma implementação da mesma
// interface. Sem eles, o contêiner falha na subida com "ambiguous dependencies".
// É Protected Variations (GRASP) com apoio do contêiner: o cliente pede um papel, não uma classe.
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

// PRODUTOR - fabrica beans que você não controla (classes de biblioteca, objetos de configuração)
// ou cuja criação depende de lógica. É o Factory Method do GoF integrado ao contêiner.
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

// ---------------------------------------------------------------------------
// INTERCEPTADORES - o mecanismo por trás de tudo que a plataforma faz "em volta" do seu código.
// É literalmente o padrão Decorator aplicado por proxy dinâmico.
// ---------------------------------------------------------------------------

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

    // O contexto dá acesso ao método, aos parâmetros e ao "prosseguir". Não chamar proceed()
    // significa CANCELAR a execução do método real - é assim que um interceptador de segurança
    // bloqueia uma chamada.
    @AroundInvoke
    Object auditar(InvocationContext contexto) throws Exception {
        long inicio = System.currentTimeMillis();
        log.info("entrando em " + contexto.getMethod().getName());

        try {
            return contexto.proceed();   // executa o método de negócio
        } finally {
            log.info("saindo de " + contexto.getMethod().getName()
                     + " em " + (System.currentTimeMillis() - inicio) + "ms");
        }
    }
}

// ---------------------------------------------------------------------------
// EJB - ENTERPRISE JAVA BEANS
//
// Hoje o EJB e o CDI convivem: o CDI cuida da injeção e do ciclo de vida, e o EJB entra quando
// você precisa dos serviços que ele oferece de graça - transação, pool, concorrência,
// temporizador e chamada assíncrona.
// ---------------------------------------------------------------------------

// @Stateless - o mais usado. O contêiner mantém um POOL de instâncias e entrega qualquer uma a
// cada chamada. Consequência prática: NÃO guarde estado de conversa em atributos, porque a
// próxima chamada do mesmo usuário pode cair em outra instância.
// Todo método público é transacional por padrão (REQUIRED).
@Stateless
class ServicoPedido {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private TabelaDeFrete tabelaDeFrete;

    // Injeção por qualificador: este serviço quer Pix, e não "alguma" implementação.
    @Inject
    @Pix
    private MeioDePagamento pagamento;

    @Inject
    private Logger log;

    // Evento CDI: publica um fato sem conhecer quem escuta. É o Observer do GoF, com o registro
    // de observadores feito pelo contêiner.
    @Inject
    private Event<PedidoConfirmado> pedidoConfirmado;

    // A transação é DECLARATIVA. Não há begin, commit nem rollback no código - o contêiner abre a
    // transação antes do método e faz commit no retorno. Qualquer RuntimeException não tratada
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

        // Quem escuta este evento roda na MESMA transação (por padrão, síncrono).
        pedidoConfirmado.fire(new PedidoConfirmado(pedidoId, valor));

        return comprovante;
    }

    // ATRIBUTOS DE TRANSAÇÃO - definem o que fazer com a transação do CHAMADOR:
    //  REQUIRED (padrão) - usa a existente ou cria uma
    //  REQUIRES_NEW      - suspende a do chamador e abre outra; usada para log/auditoria que deve
    //                      sobreviver ao rollback da transação principal
    //  MANDATORY         - exige uma transação em curso, senão lança exceção
    //  SUPPORTS          - participa se houver, roda sem transação se não houver
    //  NOT_SUPPORTED     - suspende a transação existente
    //  NEVER             - lança exceção se houver transação
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrarTentativa(Long pedidoId, String motivo) {
        // Grava mesmo que a transação que chamou este método sofra rollback.
        log.warning("tentativa em " + pedidoId + ": " + motivo);
    }

    public List<Pedido> pendentes() {
        return em.createQuery("SELECT p FROM Pedido p WHERE p.status = :s", Pedido.class)
                 .setParameter("s", StatusPedido.NOVO)
                 .getResultList();
    }
}

// Evento e observador: o emissor não conhece nenhum observador, e um observador novo não faz o
// emissor mudar. Aberto para extensão, fechado para modificação.
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

    // @Observes registra este método como observador. Nada precisa ser cadastrado em lugar nenhum.
    void aoConfirmar(@Observes PedidoConfirmado evento) {
        log.info("enviando e-mail de confirmação do pedido " + evento.getPedidoId());
    }
}

@ApplicationScoped
class IntegracaoContabil {

    // Segundo observador do MESMO evento. O ServicoPedido não ficou sabendo que ele existe.
    void aoConfirmar(@Observes PedidoConfirmado evento) {
        Logger.getLogger("contabil").info("lançando receita de " + evento.getValor());
    }
}

// @Stateful - mantém estado de conversa entre chamadas do MESMO cliente. É o caso do carrinho de
// compras. Custa memória no servidor e complica o balanceamento de carga; hoje se prefere manter
// esse estado no cliente ou em um cache distribuído.

// @Singleton (do EJB, não confundir com @ApplicationScoped do CDI) - uma única instância na
// aplicação, com controle de CONCORRÊNCIA pelo contêiner.
@Singleton
@Startup   // instanciado na subida da aplicação, e não na primeira chamada
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

    // Temporizador declarativo: substitui Thread, Timer e agendadores externos. Em cluster,
    // atenção para não rodar em todos os nós - existe @Schedule(persistent = ...) e configuração
    // específica de cada servidor para isso.
    @Schedule(hour = "*", minute = "*/15", persistent = false)
    void verificarPendencias() {
        execucoes++;
        log.info("verificação periódica #" + execucoes);
    }
}

// ---------------------------------------------------------------------------
// BEAN GERENCIADO PARA A APRESENTAÇÃO
// @Named expõe o bean por nome para a camada de visão (JSF, por exemplo).
// ---------------------------------------------------------------------------

@Named("pedidoBean")
@RequestScoped
class PedidoBean {

    @Inject
    private ServicoPedido servicoPedido;

    private Long pedidoId;
    private String mensagem;

    // O bean de apresentação NÃO tem regra de negócio: ele coleta a entrada, delega e prepara a
    // saída. É o Controller do GRASP.
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

    // @SessionScoped e @ConversationScoped EXIGEM Serializable: o contêiner pode passivar a
    // sessão para disco ou replicá-la no cluster.
    private static final long serialVersionUID = 1L;

    private final List<String> skus = new java.util.ArrayList<>();

    public void adicionar(String sku) {
        skus.add(sku);
    }

    public List<String> getSkus() {
        return skus;
    }
}

//MAPA MENTAL DA CAMADA DE NEGÓCIO
//CDI      - injeção, escopos, produtores, eventos, interceptadores, decoradores
//EJB      - transação declarativa, pool, concorrência, temporizador, chamada assíncrona
//JTA      - a transação em si; @Transactional é a forma declarativa de usá-la
//JPA      - persistência (ver a pasta JPA)
//Bean Validation - @NotNull, @Positive, @Size validados na fronteira dos métodos
//
//OS PADRÕES POR TRÁS DAS ANOTAÇÕES
//@Inject                    - Dependency Injection, que é Indirection + Protected Variations
//@ApplicationScoped         - Singleton gerenciado, sem os problemas de estado global do estático
//@Produces                  - Factory Method
//@Observes / Event          - Observer
//@Interceptor / @Decorator  - Decorator e Proxy aplicados por proxy dinâmico
//@Transactional             - Template Method: o contêiner executa begin/commit/rollback em volta
//Qualificadores             - Strategy escolhida por metadado em vez de por if

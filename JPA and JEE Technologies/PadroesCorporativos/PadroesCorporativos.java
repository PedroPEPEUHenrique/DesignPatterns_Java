//PADRÕES CORPORATIVOS - DAO, REPOSITORY, DTO, SERVICE LAYER E SESSION FACADE

//Este arquivo é o único da pasta que COMPILA E RODA: os padrões corporativos estão escritos em
//Java puro, sem depender das APIs do Jakarta EE. A intenção é mostrar a ARQUITETURA EM CAMADAS
//que a plataforma pressupõe, e a qual padrão do GoF/GRASP cada peça corresponde.

//As camadas, de fora para dentro:
//  APRESENTAÇÃO -> SERVIÇO (fachada + regra de caso de uso) -> DOMÍNIO -> PERSISTÊNCIA
//A regra é que a dependência aponte sempre para dentro. A apresentação conhece o serviço; o
//serviço conhece o domínio e as INTERFACES de persistência; o domínio não conhece ninguém.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ===========================================================================
// CAMADA DE DOMÍNIO
// Entidades com comportamento. Sem anotação, sem SQL, sem HTTP - só negócio.
// ===========================================================================

class Cliente {

    private Long id;
    private final String cpf;
    private final String nome;
    private boolean vip;

    Cliente(String cpf, String nome) {
        this.cpf = cpf;
        this.nome = nome;
    }

    void promoverParaVip() {
        this.vip = true;
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    String getCpf() {
        return cpf;
    }

    String getNome() {
        return nome;
    }

    boolean isVip() {
        return vip;
    }
}

class ItemPedido {

    private final String sku;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemPedido(String sku, int quantidade, int precoUnitarioEmCentavos) {
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    // Information Expert: quem tem quantidade e preço calcula o subtotal.
    int subtotalEmCentavos() {
        return quantidade * precoUnitarioEmCentavos;
    }

    String getSku() {
        return sku;
    }

    int getQuantidade() {
        return quantidade;
    }
}

class Pedido {

    private Long id;
    private final String codigo;
    private final Cliente cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private String status = "NOVO";

    Pedido(String codigo, Cliente cliente) {
        this.codigo = codigo;
        this.cliente = cliente;
    }

    // Creator: quem agrega os itens é quem os cria, e por isso consegue validar.
    void adicionarItem(String sku, int quantidade, int precoUnitarioEmCentavos) {
        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("quantidade inválida: " + quantidade);
        }
        itens.add(new ItemPedido(sku, quantidade, precoUnitarioEmCentavos));
    }

    int totalEmCentavos() {
        int total = 0;
        for (ItemPedido item : itens) {
            total += item.subtotalEmCentavos();
        }
        return cliente.isVip() ? (int) (total * 0.95) : total;
    }

    void pagar() {
        if (!"NOVO".equals(status)) {
            throw new IllegalStateException("pedido não está aberto para pagamento");
        }
        status = "PAGO";
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    String getCodigo() {
        return codigo;
    }

    Cliente getCliente() {
        return cliente;
    }

    List<ItemPedido> getItens() {
        return itens;
    }

    String getStatus() {
        return status;
    }
}

// ===========================================================================
// CAMADA DE PERSISTÊNCIA - DAO / REPOSITORY
//
// DAO (Data Access Object) - padrão dos Core J2EE Patterns. Encapsula o acesso a uma FONTE DE
//   DADOS. Fala em termos de tabela/registro e costuma ter CRUD.
// REPOSITORY - padrão do DDD. Também esconde a persistência, mas apresenta-se como uma COLEÇÃO
//   de objetos de domínio, com métodos na linguagem do negócio.
// Na prática os dois se confundem; o que importa é o efeito: o domínio e o serviço não sabem se
// há JDBC, JPA, arquivo ou memória do outro lado.
//
// Padrões envolvidos: Pure Fabrication (a classe não existe no negócio), Protected Variations
// (interface estável sobre a tecnologia volátil) e Low Coupling.
// ===========================================================================

interface PedidoRepository {

    void salvar(Pedido pedido);

    Optional<Pedido> porCodigo(String codigo);

    List<Pedido> porCliente(String cpf);

    List<Pedido> porStatus(String status);
}

interface ClienteRepository {

    void salvar(Cliente cliente);

    Optional<Cliente> porCpf(String cpf);
}

// Implementação em memória - a que roda no main e nos testes.
class PedidoRepositoryEmMemoria implements PedidoRepository {

    private final Map<String, Pedido> base = new HashMap<>();
    private long sequencia;

    @Override
    public void salvar(Pedido pedido) {
        if (pedido.getId() == null) {
            pedido.setId(++sequencia);
        }
        base.put(pedido.getCodigo(), pedido);
    }

    @Override
    public Optional<Pedido> porCodigo(String codigo) {
        return Optional.ofNullable(base.get(codigo));
    }

    @Override
    public List<Pedido> porCliente(String cpf) {
        List<Pedido> encontrados = new ArrayList<>();
        for (Pedido pedido : base.values()) {
            if (pedido.getCliente().getCpf().equals(cpf)) {
                encontrados.add(pedido);
            }
        }
        return encontrados;
    }

    @Override
    public List<Pedido> porStatus(String status) {
        List<Pedido> encontrados = new ArrayList<>();
        for (Pedido pedido : base.values()) {
            if (pedido.getStatus().equals(status)) {
                encontrados.add(pedido);
            }
        }
        return encontrados;
    }
}

class ClienteRepositoryEmMemoria implements ClienteRepository {

    private final Map<String, Cliente> base = new HashMap<>();

    @Override
    public void salvar(Cliente cliente) {
        base.put(cliente.getCpf(), cliente);
    }

    @Override
    public Optional<Cliente> porCpf(String cpf) {
        return Optional.ofNullable(base.get(cpf));
    }
}

// A implementação JPA teria EXATAMENTE a mesma interface. Trocar uma pela outra não faz o serviço
// mudar - é esse o teste de que a proteção funcionou.
//
//   class PedidoRepositoryJpa implements PedidoRepository {
//       @PersistenceContext private EntityManager em;
//       public void salvar(Pedido p) { em.merge(p); }
//       public Optional<Pedido> porCodigo(String codigo) { ... createQuery ... }
//   }

// ===========================================================================
// DTO - DATA TRANSFER OBJECT
//
// Objeto sem comportamento que atravessa a fronteira. Existe para:
//  - não expor a entidade (e o modelo interno) ao mundo externo;
//  - levar em UMA chamada tudo que a tela precisa, em vez de várias idas ao servidor;
//  - evitar problemas de serialização e de carregamento preguiçoso.
// ===========================================================================

class NovoPedidoDTO {

    private final String cpfCliente;
    private final List<ItemDTO> itens;

    NovoPedidoDTO(String cpfCliente, List<ItemDTO> itens) {
        this.cpfCliente = cpfCliente;
        this.itens = itens;
    }

    String getCpfCliente() {
        return cpfCliente;
    }

    List<ItemDTO> getItens() {
        return itens;
    }
}

class ItemDTO {

    private final String sku;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemDTO(String sku, int quantidade, int precoUnitarioEmCentavos) {
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    String getSku() {
        return sku;
    }

    int getQuantidade() {
        return quantidade;
    }

    int getPrecoUnitarioEmCentavos() {
        return precoUnitarioEmCentavos;
    }
}

class PedidoResumoDTO {

    private final String codigo;
    private final String nomeCliente;
    private final String status;
    private final int totalEmCentavos;
    private final int quantidadeDeItens;

    PedidoResumoDTO(String codigo, String nomeCliente, String status,
                    int totalEmCentavos, int quantidadeDeItens) {
        this.codigo = codigo;
        this.nomeCliente = nomeCliente;
        this.status = status;
        this.totalEmCentavos = totalEmCentavos;
        this.quantidadeDeItens = quantidadeDeItens;
    }

    @Override
    public String toString() {
        return codigo + " | " + nomeCliente + " | " + status
               + " | " + totalEmCentavos + " centavos | " + quantidadeDeItens + " itens";
    }
}

// ASSEMBLER / MAPPER - converte entidade em DTO e vice-versa.
// Outra Fabricação Pura: sem ela, ou a entidade ganharia um "paraDTO()" (baixa coesão), ou o DTO
// conheceria a entidade (acoplamento na direção errada).
class PedidoAssembler {

    PedidoResumoDTO paraResumo(Pedido pedido) {
        return new PedidoResumoDTO(pedido.getCodigo(),
                                   pedido.getCliente().getNome(),
                                   pedido.getStatus(),
                                   pedido.totalEmCentavos(),
                                   pedido.getItens().size());
    }

    List<PedidoResumoDTO> paraResumos(List<Pedido> pedidos) {
        List<PedidoResumoDTO> resumos = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            resumos.add(paraResumo(pedido));
        }
        return resumos;
    }
}

// ===========================================================================
// SERVIÇOS DE APOIO - as variações protegidas do caso de uso
// ===========================================================================

interface GatewayPagamento {
    String cobrar(String cpf, int valorEmCentavos);
}

class GatewayPagamentoSimulado implements GatewayPagamento {

    @Override
    public String cobrar(String cpf, int valorEmCentavos) {
        System.out.println("    [gateway] cobrando " + valorEmCentavos + " centavos de " + cpf);
        return "PAG-" + Math.abs((cpf + valorEmCentavos).hashCode() % 100000);
    }
}

interface Notificador {
    void notificar(String cpf, String mensagem);
}

class NotificadorConsole implements Notificador {

    @Override
    public void notificar(String cpf, String mensagem) {
        System.out.println("    [notificação] " + cpf + ": " + mensagem);
    }
}

// ===========================================================================
// SERVICE LAYER / SESSION FACADE
//
// Session Facade (Core J2EE Patterns): uma fachada de granularidade GROSSA sobre um conjunto de
// objetos de negócio de granularidade fina. Nasceu para evitar que o cliente remoto fizesse
// dezenas de chamadas de rede - hoje o motivo principal é outro: é aqui que fica a FRONTEIRA
// TRANSACIONAL e a coordenação do caso de uso.
//
// Em Jakarta EE esta classe seria um @Stateless com @Transactional em cada método.
// Padrões: Facade (GoF), Controller (GRASP), Low Coupling (recebe tudo por injeção).
// ===========================================================================

class ServicoPedido {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final GatewayPagamento gateway;
    private final Notificador notificador;
    private final PedidoAssembler assembler;

    // Injeção por construtor: as dependências ficam explícitas e o objeto nasce completo.
    // É o que o @Inject do CDI faz por baixo.
    ServicoPedido(PedidoRepository pedidos, ClienteRepository clientes,
                  GatewayPagamento gateway, Notificador notificador, PedidoAssembler assembler) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.gateway = gateway;
        this.notificador = notificador;
        this.assembler = assembler;
    }

    // UM método público = UM caso de uso = UMA transação.
    // O serviço COORDENA; quem decide a regra é o domínio (Pedido calcula o seu total, valida a
    // quantidade e controla o seu status). Um serviço que calcula no lugar do domínio produz o
    // modelo anêmico.
    public PedidoResumoDTO criarPedido(NovoPedidoDTO dto) {
        // ---- início da transação (@Transactional faria isso) ----

        Cliente cliente = clientes.porCpf(dto.getCpfCliente())
                .orElseThrow(() -> new IllegalArgumentException(
                        "cliente não cadastrado: " + dto.getCpfCliente()));

        Pedido pedido = new Pedido(proximoCodigo(), cliente);

        for (ItemDTO item : dto.getItens()) {
            pedido.adicionarItem(item.getSku(), item.getQuantidade(),
                                 item.getPrecoUnitarioEmCentavos());
        }

        pedidos.salvar(pedido);
        notificador.notificar(cliente.getCpf(), "pedido " + pedido.getCodigo() + " criado");

        // Devolve DTO, e não a entidade: a camada de cima não fica acoplada ao domínio.
        return assembler.paraResumo(pedido);

        // ---- commit ----
    }

    public PedidoResumoDTO pagarPedido(String codigo) {
        Pedido pedido = pedidos.porCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("pedido inexistente: " + codigo));

        String comprovante = gateway.cobrar(pedido.getCliente().getCpf(), pedido.totalEmCentavos());

        pedido.pagar();          // a regra de transição está na entidade, não aqui
        pedidos.salvar(pedido);

        notificador.notificar(pedido.getCliente().getCpf(), "pagamento " + comprovante + " aprovado");

        return assembler.paraResumo(pedido);
    }

    public List<PedidoResumoDTO> listarPorCliente(String cpf) {
        return assembler.paraResumos(pedidos.porCliente(cpf));
    }

    public List<PedidoResumoDTO> listarPagos() {
        return assembler.paraResumos(pedidos.porStatus("PAGO"));
    }

    private String proximoCodigo() {
        return "PED-" + (System.nanoTime() % 10000);
    }
}

// ===========================================================================
// CAMADA DE APRESENTAÇÃO
// Aqui estaria o @RestController, o Servlet ou o bean JSF. Ele só traduz entrada e saída.
// ===========================================================================

class PedidoController {

    private final ServicoPedido servico;

    PedidoController(ServicoPedido servico) {
        this.servico = servico;
    }

    void postPedido(String cpf) {
        System.out.println("POST /api/pedidos");
        try {
            PedidoResumoDTO resumo = servico.criarPedido(new NovoPedidoDTO(cpf, List.of(
                    new ItemDTO("TEC-001", 2, 25000),
                    new ItemDTO("MOU-002", 1, 8000))));
            System.out.println("  201 Created -> " + resumo);
        } catch (IllegalArgumentException e) {
            System.out.println("  400 Bad Request -> " + e.getMessage());
        }
    }

    void putPagamento(String codigo) {
        System.out.println("PUT /api/pedidos/" + codigo + "/pagamento");
        try {
            System.out.println("  200 OK -> " + servico.pagarPedido(codigo));
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  409 Conflict -> " + e.getMessage());
        }
    }

    void getPorCliente(String cpf) {
        System.out.println("GET /api/pedidos?cpf=" + cpf);
        for (PedidoResumoDTO resumo : servico.listarPorCliente(cpf)) {
            System.out.println("  " + resumo);
        }
    }
}

// Classe Cliente
class PadroesCorporativos {

    public static void main(String[] args) {
        // MONTAGEM DA APLICAÇÃO
        // Este é o único ponto que conhece as implementações concretas. Em Jakarta EE, quem faz
        // esta montagem é o contêiner CDI a partir das anotações - o efeito é o mesmo.
        ClienteRepository clientes = new ClienteRepositoryEmMemoria();
        PedidoRepository pedidos = new PedidoRepositoryEmMemoria();

        ServicoPedido servico = new ServicoPedido(pedidos, clientes,
                                                  new GatewayPagamentoSimulado(),
                                                  new NotificadorConsole(),
                                                  new PedidoAssembler());

        Cliente ana = new Cliente("11122233344", "Ana Souza");
        ana.promoverParaVip();
        clientes.salvar(ana);

        PedidoController controller = new PedidoController(servico);

        controller.postPedido("11122233344");
        controller.postPedido("99999999999");   // cliente inexistente: erro tratado na fronteira

        controller.getPorCliente("11122233344");

        String codigo = pedidos.porCliente("11122233344").get(0).getCodigo();
        controller.putPagamento(codigo);
        controller.putPagamento(codigo);        // segunda vez: o domínio recusa

        System.out.println("\npedidos pagos: " + servico.listarPagos().size());
    }
}

//QUADRO DE CORRESPONDÊNCIA
//Camada / peça          Padrão corporativo      Padrão GoF / GRASP
//-------------------------------------------------------------------------------
//Resource, Servlet      Front Controller        Controller (GRASP), Facade
//DTO                    Data Transfer Object    Pure Fabrication
//Assembler              Transfer Object Assembler  Pure Fabrication, Builder
//ServicoPedido          Session Facade,         Facade (GoF), Controller (GRASP),
//                       Service Layer           Low Coupling
//PedidoRepository       DAO / Repository        Pure Fabrication, Protected Variations
//Entidades              Domain Model            Information Expert, Creator, Polymorphism
//EntityManager          Unit of Work,           Facade sobre JDBC
//                       Identity Map
//Proxy LAZY da JPA      -                       Proxy (GoF)
//Filtro de servlet      Intercepting Filter     Chain of Responsibility
//Interceptador CDI      -                       Decorator / Proxy
//@Inject                Dependency Injection    Indirection, Protected Variations
//@Produces              -                       Factory Method
//@Observes / Event      -                       Observer
//
//A conclusão do estudo: os frameworks corporativos não substituem os padrões - eles são feitos
//DE padrões. Reconhecer qual padrão está por trás de cada anotação é o que permite prever o
//comportamento do contêiner em vez de decorá-lo.

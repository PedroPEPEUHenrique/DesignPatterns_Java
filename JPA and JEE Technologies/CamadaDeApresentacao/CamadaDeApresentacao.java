//JAKARTA EE - CAMADA DE APRESENTAÇÃO: SERVLET, JAX-RS E BEAN VALIDATION
//NÃO compila sem as APIs do Jakarta EE (servlet-api, ws.rs-api, validation-api, json.bind-api).
//A camada de apresentação é a fronteira: recebe o mundo externo (HTTP, JSON, formulário) e o
//traduz para o vocabulário do domínio. Duas regras valem para tudo aqui:
//1. NENHUMA regra de negócio - ela delega ao serviço (Controller, do GRASP).
//2. NENHUMA entidade JPA cruzando a fronteira - senão o modelo interno vira contrato público e
//   qualquer refatoração quebra o cliente da API.

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// DTO - objeto sem comportamento cuja função é atravessar a fronteira. Desacopla o contrato
// externo do modelo interno, evita LazyInitializationException ao serializar entidades e não expõe
// campos internos. É uma Fabricação Pura.
class NovoPedidoDTO {

    // Bean Validation: as restrições ficam junto do dado, em vez de espalhadas em ifs.
    @NotBlank(message = "o CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
    private String cpfCliente;

    @NotNull
    @Email(message = "e-mail inválido")
    private String email;

    @NotNull(message = "informe ao menos um item")
    @Size(min = 1, message = "o pedido precisa de pelo menos um item")
    private List<ItemDTO> itens;

    public String getCpfCliente() {
        return cpfCliente;
    }

    public void setCpfCliente(String cpfCliente) {
        this.cpfCliente = cpfCliente;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<ItemDTO> getItens() {
        return itens;
    }

    public void setItens(List<ItemDTO> itens) {
        this.itens = itens;
    }
}

class ItemDTO {

    @NotBlank
    private String sku;

    @Positive(message = "a quantidade deve ser maior que zero")
    private int quantidade;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}

class PedidoResumoDTO {

    private String codigo;
    private String status;
    private BigDecimal total;

    PedidoResumoDTO(String codigo, String status, BigDecimal total) {
        this.codigo = codigo;
        this.status = status;
        this.total = total;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }
}

// Prefixo de todos os recursos: as URLs ficam em /api/...
@ApplicationPath("/api")
class ConfiguracaoRest extends Application {
}

// RECURSO REST: não abre transação, não toca em EntityManager e não calcula. Traduz HTTP para
// chamada de serviço e o resultado de volta para HTTP.
@Path("/pedidos")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PedidoResource {

    @Inject
    private ServicoPedidoRest servico;

    // @QueryParam lê a query string; @DefaultValue evita nulo quando o parâmetro não vem.
    @GET
    public List<PedidoResumoDTO> listar(@QueryParam("status") String status,
                                        @QueryParam("pagina") @DefaultValue("0") int pagina,
                                        @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return servico.listar(status, pagina, tamanho);
    }

    // Devolver 404 faz parte do contrato REST - devolver 200 com corpo vazio é erro comum.
    @GET
    @Path("/{codigo}")
    public Response porCodigo(@PathParam("codigo") String codigo) {
        PedidoResumoDTO pedido = servico.porCodigo(codigo);

        if (pedido == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErroDTO("pedido não encontrado: " + codigo))
                           .build();
        }
        return Response.ok(pedido).build();
    }

    // @Valid dispara o Bean Validation ANTES do método executar: a validação vira um
    // interceptador, não um bloco de ifs no início do corpo.
    // A resposta correta para criação é 201 com o cabeçalho Location.
    @POST
    public Response criar(@Valid NovoPedidoDTO novoPedido) {
        String codigo = servico.criar(novoPedido);

        return Response.created(URI.create("/api/pedidos/" + codigo))
                       .entity(new PedidoResumoDTO(codigo, "NOVO", BigDecimal.ZERO))
                       .build();
    }

    @PUT
    @Path("/{codigo}/pagamento")
    public Response pagar(@PathParam("codigo") String codigo) {
        servico.pagar(codigo);
        return Response.noContent().build();   // 204
    }

    @DELETE
    @Path("/{codigo}")
    public Response cancelar(@PathParam("codigo") String codigo) {
        servico.cancelar(codigo);
        return Response.noContent().build();
    }
}

class ErroDTO {

    private final String mensagem;

    ErroDTO(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}

// Concentra a tradução de exceção para resposta HTTP num lugar só, em vez de um try/catch repetido
// em cada método do recurso. É Indirection aplicada ao erro.
@Provider
class RegraNegocioExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException excecao) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(new ErroDTO(excecao.getMessage()))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}

@RequestScoped
class ServicoPedidoRest {

    List<PedidoResumoDTO> listar(String status, int pagina, int tamanho) {
        return List.of(new PedidoResumoDTO("PED-1", "PAGO", new BigDecimal("330.00")));
    }

    PedidoResumoDTO porCodigo(String codigo) {
        return "PED-1".equals(codigo)
                ? new PedidoResumoDTO("PED-1", "PAGO", new BigDecimal("330.00"))
                : null;
    }

    String criar(NovoPedidoDTO novoPedido) {
        return "PED-" + Math.abs(novoPedido.getCpfCliente().hashCode() % 1000);
    }

    void pagar(String codigo) {
    }

    void cancelar(String codigo) {
    }
}

// SERVLET - JSF, JAX-RS e os frameworks MVC rodam SOBRE ele.
@WebServlet(urlPatterns = "/pedidos")
class PedidoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private ServicoPedidoRest servico;

    // CONCORRÊNCIA: o contêiner cria UMA instância do servlet e a compartilha entre todas as
    // requisições, cada uma em sua thread. Atributo de instância com estado de requisição é bug
    // garantido - o estado vai em variável local, no request ou na sessão.
    @Override
    protected void doGet(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {

        String status = requisicao.getParameter("status");
        List<PedidoResumoDTO> pedidos = servico.listar(status, 0, 20);

        // Escopos de atributo: request (uma requisição), session (a sessão do usuário),
        // application (enquanto a aplicação estiver no ar).
        requisicao.setAttribute("pedidos", pedidos);

        HttpSession sessao = requisicao.getSession();
        sessao.setAttribute("ultimaConsulta", System.currentTimeMillis());

        // forward mantém a mesma requisição; sendRedirect faz o navegador emitir uma NOVA - é o
        // que se usa depois de um POST, para evitar reenvio do formulário ao atualizar a página.
        requisicao.getRequestDispatcher("/WEB-INF/jsp/pedidos.jsp").forward(requisicao, resposta);
    }

    @Override
    protected void doPost(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws ServletException, IOException {

        NovoPedidoDTO dto = new NovoPedidoDTO();
        dto.setCpfCliente(requisicao.getParameter("cpf"));

        String codigo = servico.criar(dto);

        resposta.sendRedirect(requisicao.getContextPath() + "/pedidos?criado=" + codigo);
    }
}

// FILTRO - Chain of Responsibility na especificação: cada filtro decide se chama o próximo
// (doFilter) ou se interrompe a corrente ali mesmo.
@WebFilter(urlPatterns = "/api/*")
class FiltroAutenticacao implements jakarta.servlet.Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest requisicao,
                         jakarta.servlet.ServletResponse resposta,
                         jakarta.servlet.FilterChain corrente)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) requisicao;
        String token = http.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            ((HttpServletResponse) resposta).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;   // NÃO chama doFilter: a corrente para aqui e o servlet nunca executa
        }

        corrente.doFilter(requisicao, resposta);
    }
}

//RESUMO
//Servlet   - base de tudo; uma instância compartilhada, cuidado com estado
//Filter    - Chain of Responsibility para requisições transversais (auth, CORS, log, encoding)
//JSF       - MVC por componentes, com @Named + escopos CDI
//JAX-RS    - REST por anotações
//JSON-B    - serialização automática entre objeto e JSON
//Bean Validation - restrições disparadas por @Valid
//
//Padrões: DTO, Front Controller (o servlet despachante do JAX-RS/JSF), Controller do GRASP,
//Chain of Responsibility (filtros), Facade (o serviço de fronteira) e Indirection (ExceptionMapper).

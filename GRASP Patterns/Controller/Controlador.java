//GRASP - CONTROLLER
//Problema: qual é o primeiro objeto, além da interface, que recebe e coordena uma operação do
//sistema?
//Solução: uma classe que represente o SISTEMA como um todo (controlador de fachada) ou um CASO DE
//USO específico (controlador de sessão). Sem isso, a regra fica presa aos eventos de clique da
//tela e não dá para reaproveitá-la na API nem testá-la sem interface.

import java.util.ArrayList;
import java.util.List;

class Produto {
    private final String sku;
    private final String nome;
    private final int precoEmCentavos;

    Produto(String sku, String nome, int precoEmCentavos) {
        this.sku = sku;
        this.nome = nome;
        this.precoEmCentavos = precoEmCentavos;
    }

    String getSku() {
        return sku;
    }

    String getNome() {
        return nome;
    }

    int getPrecoEmCentavos() {
        return precoEmCentavos;
    }
}

class CatalogoProdutos {

    private final List<Produto> produtos = List.of(
            new Produto("TEC-001", "Teclado", 25000),
            new Produto("MOU-002", "Mouse", 8000),
            new Produto("MON-003", "Monitor", 90000));

    Produto porSku(String sku) {
        return produtos.stream()
                       .filter(p -> p.getSku().equals(sku))
                       .findFirst()
                       .orElseThrow(() -> new IllegalArgumentException("sku inexistente: " + sku));
    }
}

class LinhaVenda {
    private final Produto produto;
    private final int quantidade;

    LinhaVenda(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    int subtotalEmCentavos() {
        return produto.getPrecoEmCentavos() * quantidade;
    }

    Produto getProduto() {
        return produto;
    }

    int getQuantidade() {
        return quantidade;
    }
}

class Venda {
    private final List<LinhaVenda> linhas = new ArrayList<>();
    private boolean encerrada;
    private int descontoEmCentavos;

    void adicionar(Produto produto, int quantidade) {
        if (encerrada) {
            throw new IllegalStateException("venda já encerrada");
        }
        linhas.add(new LinhaVenda(produto, quantidade));
    }

    void aplicarDesconto(int descontoEmCentavos) {
        this.descontoEmCentavos = descontoEmCentavos;
    }

    void encerrar() {
        encerrada = true;
    }

    int totalEmCentavos() {
        int total = 0;
        for (LinhaVenda linha : linhas) {
            total += linha.subtotalEmCentavos();
        }
        return total - descontoEmCentavos;
    }

    boolean isEncerrada() {
        return encerrada;
    }

    List<LinhaVenda> getLinhas() {
        return linhas;
    }
}

class ProgramaFidelidade {

    int descontoEmCentavos(String cpf, int totalEmCentavos) {
        return cpf.isEmpty() ? 0 : (int) (totalEmCentavos * 0.05);
    }
}

class ServicoPagamento {

    String cobrar(int valorEmCentavos, String meio) {
        System.out.println("  [pagamento] " + valorEmCentavos + " centavos via " + meio);
        return "PAG-" + System.nanoTime() % 10000;
    }
}

class RegistrarVendaController {

    private final CatalogoProdutos catalogo;
    private final ProgramaFidelidade fidelidade;
    private final ServicoPagamento pagamento;
    private Venda vendaAtual;

    RegistrarVendaController(CatalogoProdutos catalogo, ProgramaFidelidade fidelidade,
                             ServicoPagamento pagamento) {
        this.catalogo = catalogo;
        this.fidelidade = fidelidade;
        this.pagamento = pagamento;
    }

    public void iniciarNovaVenda() {
        vendaAtual = new Venda();
        System.out.println("venda iniciada");
    }

    public void informarItem(String sku, int quantidade) {
        exigirVendaAberta();
        Produto produto = catalogo.porSku(sku);
        vendaAtual.adicionar(produto, quantidade);
        System.out.println("  + " + quantidade + "x " + produto.getNome());
    }

    public void encerrarVenda(String cpf) {
        exigirVendaAberta();
        int desconto = fidelidade.descontoEmCentavos(cpf, vendaAtual.totalEmCentavos());
        vendaAtual.aplicarDesconto(desconto);
        vendaAtual.encerrar();
        System.out.println("  desconto de fidelidade: " + desconto);
    }

    public String efetuarPagamento(String meio) {
        if (vendaAtual == null || !vendaAtual.isEncerrada()) {
            throw new IllegalStateException("venda não encerrada");
        }
        return pagamento.cobrar(vendaAtual.totalEmCentavos(), meio);
    }

    public String resumoParaTela() {
        StringBuilder resumo = new StringBuilder();
        for (LinhaVenda linha : vendaAtual.getLinhas()) {
            resumo.append("    ").append(linha.getQuantidade()).append("x ")
                  .append(linha.getProduto().getNome()).append(" = ")
                  .append(linha.subtotalEmCentavos()).append("\n");
        }
        resumo.append("    TOTAL: ").append(vendaAtual.totalEmCentavos());
        return resumo.toString();
    }

    private void exigirVendaAberta() {
        if (vendaAtual == null || vendaAtual.isEncerrada()) {
            throw new IllegalStateException("não há venda aberta");
        }
    }
}

class TelaCaixa {

    private final RegistrarVendaController controller;

    TelaCaixa(RegistrarVendaController controller) {
        this.controller = controller;
    }

    void simularOperacao() {
        System.out.println("== tela do caixa ==");
        controller.iniciarNovaVenda();
        controller.informarItem("TEC-001", 1);
        controller.informarItem("MOU-002", 2);
        controller.encerrarVenda("111.222.333-44");
        System.out.println(controller.resumoParaTela());
        System.out.println("  comprovante: " + controller.efetuarPagamento("cartão"));
    }
}

class ApiVendas {

    private final RegistrarVendaController controller;

    ApiVendas(RegistrarVendaController controller) {
        this.controller = controller;
    }

    void postVenda() {
        System.out.println("== API REST ==");
        controller.iniciarNovaVenda();
        controller.informarItem("MON-003", 1);
        controller.encerrarVenda("");
        System.out.println(controller.resumoParaTela());
        System.out.println("  comprovante: " + controller.efetuarPagamento("pix"));
    }
}

class Controlador {

    public static void main(String[] args) {
        CatalogoProdutos catalogo = new CatalogoProdutos();
        ProgramaFidelidade fidelidade = new ProgramaFidelidade();
        ServicoPagamento pagamento = new ServicoPagamento();

        new TelaCaixa(new RegistrarVendaController(catalogo, fidelidade, pagamento))
                .simularOperacao();

        System.out.println();

        new ApiVendas(new RegistrarVendaController(catalogo, fidelidade, pagamento))
                .postVenda();
    }
}

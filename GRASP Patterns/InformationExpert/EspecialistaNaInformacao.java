//GRASP - EXPERT / INFORMATION EXPERT
//Problema: a qual classe atribuir uma responsabilidade?
//Solução: à classe que TEM A INFORMAÇÃO necessária para cumpri-la.

import java.util.ArrayList;
import java.util.List;

class Produto {
    private final String nome;

    Produto(String nome) {
        this.nome = nome;
    }

    String getNome() {
        return nome;
    }
}

class ItemPedidoAnemico {
    private Produto produto;
    private int quantidade;
    private int precoUnitarioEmCentavos;

    ItemPedidoAnemico(Produto produto, int quantidade, int precoUnitarioEmCentavos) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getPrecoUnitarioEmCentavos() {
        return precoUnitarioEmCentavos;
    }

    public Produto getProduto() {
        return produto;
    }
}

class PedidoAnemico {
    private final List<ItemPedidoAnemico> itens = new ArrayList<>();

    public List<ItemPedidoAnemico> getItens() {
        return itens;
    }

    public void adicionar(ItemPedidoAnemico item) {
        itens.add(item);
    }
}

class ServicoCalculoPedido {

    public int calcularTotal(PedidoAnemico pedido) {
        int total = 0;
        for (ItemPedidoAnemico item : pedido.getItens()) {
            total += item.getQuantidade() * item.getPrecoUnitarioEmCentavos();
        }
        return total;
    }
}

class ItemPedido {
    private final Produto produto;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemPedido(Produto produto, int quantidade, int precoUnitarioEmCentavos) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    public int subtotalEmCentavos() {
        return quantidade * precoUnitarioEmCentavos;
    }

    public int pesoEmGramas() {
        return quantidade * 250;
    }

    public Produto getProduto() {
        return produto;
    }
}

class Pedido {
    private final List<ItemPedido> itens = new ArrayList<>();
    private int descontoEmCentavos;

    public void adicionar(ItemPedido item) {
        itens.add(item);
    }

    public void aplicarDesconto(int descontoEmCentavos) {
        this.descontoEmCentavos = descontoEmCentavos;
    }

    public int totalEmCentavos() {
        int total = 0;
        for (ItemPedido item : itens) {
            total += item.subtotalEmCentavos();
        }
        return total - descontoEmCentavos;
    }

    public int pesoTotalEmGramas() {
        int peso = 0;
        for (ItemPedido item : itens) {
            peso += item.pesoEmGramas();
        }
        return peso;
    }

    public int quantidadeDeItens() {
        return itens.size();
    }
}

class EspecialistaNaInformacao {

    public static void main(String[] args) {
        PedidoAnemico anemico = new PedidoAnemico();
        anemico.adicionar(new ItemPedidoAnemico(new Produto("Teclado"), 2, 25000));
        anemico.adicionar(new ItemPedidoAnemico(new Produto("Mouse"), 1, 8000));
        System.out.println("total (anêmico): " + new ServicoCalculoPedido().calcularTotal(anemico));

        Pedido pedido = new Pedido();
        pedido.adicionar(new ItemPedido(new Produto("Teclado"), 2, 25000));
        pedido.adicionar(new ItemPedido(new Produto("Mouse"), 1, 8000));
        pedido.aplicarDesconto(3000);

        System.out.println("total: " + pedido.totalEmCentavos());
        System.out.println("peso: " + pedido.pesoTotalEmGramas() + "g");
        System.out.println("itens: " + pedido.quantidadeDeItens());
    }
}

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

// COMO NÃO FAZER - objetos anêmicos: só dados, nenhum comportamento.
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

// A responsabilidade foi parar longe do dado. Cada getter de outra classe é um acoplamento: se
// ItemPedido mudar a forma de guardar o preço, ESTA classe quebra.
class ServicoCalculoPedido {

    public int calcularTotal(PedidoAnemico pedido) {
        int total = 0;
        for (ItemPedidoAnemico item : pedido.getItens()) {
            total += item.getQuantidade() * item.getPrecoUnitarioEmCentavos();
        }
        return total;
    }
}

// COMO FAZER - cada classe calcula o que consegue com o que ela mesma tem.

class ItemPedido {
    private final Produto produto;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemPedido(Produto produto, int quantidade, int precoUnitarioEmCentavos) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    // Quem tem quantidade e preço é o item: o subtotal é dele. E agora nem precisamos expor os dois.
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

    // Quem tem a lista é o pedido: o total é dele. Mas ele NÃO calcula o subtotal de cada item -
    // a responsabilidade se distribui pela cadeia de quem tem cada informação.
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

//Como o cálculo acontece onde o dado está, o dado não precisa vazar - o Expert reforça Low
//Coupling e High Cohesion, e por isso é o padrão GRASP básico.
//Quando NÃO seguir: quando atribuir a responsabilidade ao especialista criar acoplamento indevido.
//O caso clássico é persistência - dar ao Pedido a responsabilidade de gravar acoplaria o domínio
//ao banco. A saída é uma classe que não existe no domínio, o que é Pure Fabrication.

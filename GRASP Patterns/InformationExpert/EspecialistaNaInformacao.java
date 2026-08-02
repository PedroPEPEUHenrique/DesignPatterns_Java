//GRASP - EXPERT / INFORMATION EXPERT (Especialista na Informação)

//Problema: a qual classe atribuir uma responsabilidade?
//Solução: atribua a responsabilidade à classe que TEM A INFORMAÇÃO necessária para cumpri-la.

//Suponha que a sua tarefa seja calcular o total de um pedido. O pedido tem itens; cada item tem
//quantidade e preço unitário.
//Imagine a solução em que um "ServicoCalculoPedido" pega a lista de itens do pedido, pega a
//quantidade e o preço de cada item e faz a conta. Para isso, Pedido e ItemPedido precisam expor
//tudo o que têm. O cálculo fica LONGE do dado, e a classe que tem o dado vira um saco de getters.

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

// COMO NÃO FAZER
// Objetos anêmicos: só dados, nenhum comportamento. É o "Anemic Domain Model".
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

// A responsabilidade foi parar aqui, longe do dado. Repare em quantos getters de outras classes
// este método precisa: cada um deles é um acoplamento. Se ItemPedido mudar a forma de guardar o
// preço, ESTA classe quebra - e ela nem deveria saber que existe preço unitário.
class ServicoCalculoPedido {

    public int calcularTotal(PedidoAnemico pedido) {
        int total = 0;
        for (ItemPedidoAnemico item : pedido.getItens()) {
            total += item.getQuantidade() * item.getPrecoUnitarioEmCentavos();
        }
        return total;
    }
}

// COMO FAZER
// Cada classe calcula o que consegue calcular com o que ela mesma tem.

class ItemPedido {
    private final Produto produto;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemPedido(Produto produto, int quantidade, int precoUnitarioEmCentavos) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    // ESPECIALISTA: quem tem quantidade e preço unitário é o item, então o subtotal é dele.
    // Note que agora nem precisamos expor quantidade e preço para fora.
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

    // ESPECIALISTA PARCIAL: quem tem a lista de itens é o pedido, então o total é dele. Mas ele
    // NÃO calcula o subtotal de cada item - delega a quem sabe. A responsabilidade se distribui
    // pela cadeia de quem tem cada informação.
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

// Classe Cliente
class EspecialistaNaInformacao {

    public static void main(String[] args) {
        // Versão anêmica: o cliente e o serviço precisam conhecer a estrutura interna.
        PedidoAnemico anemico = new PedidoAnemico();
        anemico.adicionar(new ItemPedidoAnemico(new Produto("Teclado"), 2, 25000));
        anemico.adicionar(new ItemPedidoAnemico(new Produto("Mouse"), 1, 8000));
        System.out.println("total (anêmico): " + new ServicoCalculoPedido().calcularTotal(anemico));

        // Versão com especialista: o cliente só pergunta, e cada objeto responde pelo que sabe.
        Pedido pedido = new Pedido();
        pedido.adicionar(new ItemPedido(new Produto("Teclado"), 2, 25000));
        pedido.adicionar(new ItemPedido(new Produto("Mouse"), 1, 8000));
        pedido.aplicarDesconto(3000);

        System.out.println("total: " + pedido.totalEmCentavos());
        System.out.println("peso: " + pedido.pesoTotalEmGramas() + "g");
        System.out.println("itens: " + pedido.quantidadeDeItens());
    }
}

//Consequências do Expert:
//Mantém o encapsulamento: como o cálculo acontece onde o dado está, o dado não precisa vazar.
//Distribui o comportamento: em vez de poucas classes gordas e muitas classes de dados, o sistema
//  fica com classes que fazem o que lhes cabe.
//Reforça Low Coupling e High Cohesion - por isso o Expert é considerado o padrão GRASP básico.
//
//Quando NÃO seguir: se atribuir a responsabilidade ao especialista criar um acoplamento indevido.
//O caso clássico é persistência - o Pedido tem os dados a serem gravados, mas dar a ele a
//responsabilidade de gravar acoplaria o domínio ao banco. A saída é criar uma classe que não
//existe no domínio (um repositório), o que é o padrão Pure Fabrication.

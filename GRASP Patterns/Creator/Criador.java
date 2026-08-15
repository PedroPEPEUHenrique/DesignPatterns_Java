//GRASP - CREATOR
//Problema: quem deve criar uma instância de A?
//Solução: a classe B que AGREGA, CONTÉM, REGISTRA ou USA A de perto, ou que TEM OS DADOS de
//inicialização de A.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Produto {
    private final String nome;
    private final int precoVigenteEmCentavos;

    Produto(String nome, int precoVigenteEmCentavos) {
        this.nome = nome;
        this.precoVigenteEmCentavos = precoVigenteEmCentavos;
    }

    String getNome() {
        return nome;
    }

    int getPrecoVigenteEmCentavos() {
        return precoVigenteEmCentavos;
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

    int subtotalEmCentavos() {
        return quantidade * precoUnitarioEmCentavos;
    }

    Produto getProduto() {
        return produto;
    }

    int getQuantidade() {
        return quantidade;
    }

    int getPrecoUnitarioEmCentavos() {
        return precoUnitarioEmCentavos;
    }
}

class Pedido {

    private final String codigo;
    private final List<ItemPedido> itens = new ArrayList<>();

    Pedido(String codigo) {
        this.codigo = codigo;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("quantidade fora da faixa permitida: " + quantidade);
        }

        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).getProduto() == produto) {
                ItemPedido existente = itens.get(i);
                itens.set(i, new ItemPedido(produto,
                                            existente.getQuantidade() + quantidade,
                                            existente.getPrecoUnitarioEmCentavos()));
                return;
            }
        }

        itens.add(new ItemPedido(produto, quantidade, produto.getPrecoVigenteEmCentavos()));
    }

    public int totalEmCentavos() {
        int total = 0;
        for (ItemPedido item : itens) {
            total += item.subtotalEmCentavos();
        }
        return total;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public String getCodigo() {
        return codigo;
    }
}

class Cliente {

    private final String nome;
    private final List<Pedido> pedidos = new ArrayList<>();

    Cliente(String nome) {
        this.nome = nome;
    }

    public Pedido abrirPedido() {
        Pedido pedido = new Pedido(nome.substring(0, 3).toUpperCase() + "-" + (pedidos.size() + 1));
        pedidos.add(pedido);
        return pedido;
    }

    public int quantidadeDePedidos() {
        return pedidos.size();
    }

    public String getNome() {
        return nome;
    }
}

class Criador {

    public static void main(String[] args) {
        Cliente ana = new Cliente("Ana Souza");

        Pedido pedido = ana.abrirPedido();

        Produto teclado = new Produto("Teclado", 25000);
        Produto mouse = new Produto("Mouse", 8000);

        pedido.adicionarItem(teclado, 2);
        pedido.adicionarItem(mouse, 1);
        pedido.adicionarItem(teclado, 3);

        System.out.println("pedido " + pedido.getCodigo() + " de " + ana.getNome());
        System.out.println("  linhas: " + pedido.getItens().size());
        System.out.println("  total: " + pedido.totalEmCentavos() + " centavos");

        try {
            pedido.adicionarItem(mouse, 500);
        } catch (IllegalArgumentException e) {
            System.out.println("  recusado: " + e.getMessage());
        }

        ana.abrirPedido();
        System.out.println("pedidos de " + ana.getNome() + ": " + ana.quantidadeDePedidos());
    }
}

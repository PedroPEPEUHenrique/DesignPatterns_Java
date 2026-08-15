//POO - ENCAPSULAMENTO
//Encapsular é esconder a REPRESENTAÇÃO e expor apenas OPERAÇÕES. O ganho não é sigilo: é poder
//trocar o interior sem quebrar quem usa e garantir que o objeto nunca chegue a um estado inválido
//- a sua INVARIANTE.
//Visibilidade em Java, da mais aberta à mais fechada:
//  public    - qualquer classe
//  protected - o pacote e as subclasses
//  (padrão)  - só o pacote
//  private   - só a própria classe
//A regra prática é declarar tudo private e abrir apenas o que o cliente realmente precisa.
//Encapsular NÃO é gerar getter e setter para cada atributo: um setter público devolve o campo ao
//mundo e a invariante vai junto. Peça ao objeto que FAÇA algo, em vez de perguntar os dados e
//decidir no lugar dele.
//Devolver a coleção interna é o furo mais comum: quem recebe a referência altera o interior do
//objeto pelas costas dele. Devolva uma cópia ou uma visão não modificável.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ItemCarrinho {

    private final String sku;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemCarrinho(String sku, int quantidade, int precoUnitarioEmCentavos) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
        if (precoUnitarioEmCentavos <= 0) {
            throw new IllegalArgumentException("preço deve ser positivo");
        }
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    int subtotalEmCentavos() {
        return quantidade * precoUnitarioEmCentavos;
    }

    String getSku() {
        return sku;
    }

    @Override
    public String toString() {
        return sku + " x" + quantidade;
    }
}

class CarrinhoExposto {

    public List<ItemCarrinho> itens = new ArrayList<>();
    public int descontoEmCentavos;

    public int totalEmCentavos() {
        int bruto = 0;
        for (ItemCarrinho item : itens) {
            bruto += item.subtotalEmCentavos();
        }
        return bruto - descontoEmCentavos;
    }
}

class Carrinho {

    private final List<ItemCarrinho> itens = new ArrayList<>();
    private int descontoEmCentavos;

    void adicionar(ItemCarrinho item) {
        if (item == null) {
            throw new IllegalArgumentException("item nulo");
        }
        itens.add(item);
    }

    void aplicarDesconto(int valorEmCentavos) {
        if (valorEmCentavos < 0 || valorEmCentavos > totalBrutoEmCentavos()) {
            throw new IllegalArgumentException("desconto fora da faixa permitida");
        }
        this.descontoEmCentavos = valorEmCentavos;
    }

    int totalEmCentavos() {
        return totalBrutoEmCentavos() - descontoEmCentavos;
    }

    boolean estaVazio() {
        return itens.isEmpty();
    }

    List<ItemCarrinho> getItens() {
        return Collections.unmodifiableList(itens);
    }

    private int totalBrutoEmCentavos() {
        int bruto = 0;
        for (ItemCarrinho item : itens) {
            bruto += item.subtotalEmCentavos();
        }
        return bruto;
    }
}

class Encapsulamento {

    public static void main(String[] args) {
        CarrinhoExposto exposto = new CarrinhoExposto();
        exposto.itens.add(new ItemCarrinho("TEC-001", 1, 25000));
        exposto.descontoEmCentavos = 900000;
        System.out.println("carrinho exposto, total: " + exposto.totalEmCentavos());

        exposto.itens.clear();
        System.out.println("qualquer um esvaziou a lista interna: " + exposto.itens);

        Carrinho carrinho = new Carrinho();
        carrinho.adicionar(new ItemCarrinho("TEC-001", 1, 25000));
        carrinho.adicionar(new ItemCarrinho("MOU-002", 2, 8000));
        carrinho.aplicarDesconto(5000);
        System.out.println("carrinho encapsulado, total: " + carrinho.totalEmCentavos());

        try {
            carrinho.aplicarDesconto(900000);
        } catch (IllegalArgumentException e) {
            System.out.println("invariante preservada: " + e.getMessage());
        }

        try {
            carrinho.getItens().clear();
        } catch (UnsupportedOperationException e) {
            System.out.println("a coleção interna não é alterável de fora");
        }

        System.out.println("itens: " + carrinho.getItens() + ", vazio? " + carrinho.estaVazio());
    }
}

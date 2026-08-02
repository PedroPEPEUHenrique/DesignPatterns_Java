//GRASP - CREATOR (Criador)

//Problema: quem deve ser responsável por criar uma instância de uma classe A?
//Solução: atribua à classe B a responsabilidade de criar A se ao menos uma destas for verdadeira:
//B AGREGA objetos de A
//B CONTÉM objetos de A
//B REGISTRA instâncias de A
//B USA A de perto
//B TEM OS DADOS de inicialização de A

//Suponha que a sua tarefa seja acrescentar um item a um pedido. O item precisa do produto, da
//quantidade e do preço unitário vigente.
//Imagine a solução em que a camada de aplicação cria o ItemPedido "na mão" e depois manda o
//pedido guardá-lo. Quem cria passa a precisar saber tudo sobre a construção do item - e o pedido,
//que é quem CONTÉM os itens, perde o controle sobre o que entra na sua própria lista.

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

    // O construtor tem visibilidade de PACOTE, não pública: só quem está no mesmo pacote - o
    // Pedido - consegue criar um item. Isso torna a regra do Creator verificável pelo compilador,
    // em vez de ser só uma convenção que alguém vai furar.
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

// O CRIADOR
// Pedido AGREGA ItemPedido e TEM os dados de inicialização (o preço vigente vem do produto que
// ele recebe). Pelas duas razões, é ele quem cria.
class Pedido {

    private final String codigo;
    private final List<ItemPedido> itens = new ArrayList<>();

    Pedido(String codigo) {
        this.codigo = codigo;
    }

    // O cliente diz O QUE quer ("adicione 2 teclados"), não COMO construir o item. Como o pedido
    // controla a criação, ele pode aplicar regras que ninguém consegue burlar.
    public void adicionarItem(Produto produto, int quantidade) {
        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("quantidade fora da faixa permitida: " + quantidade);
        }

        // Item já existente: em vez de duplicar, soma a quantidade. Esta regra só é possível
        // porque a criação passa por aqui.
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).getProduto() == produto) {
                ItemPedido existente = itens.get(i);
                itens.set(i, new ItemPedido(produto,
                                            existente.getQuantidade() + quantidade,
                                            existente.getPrecoUnitarioEmCentavos()));
                return;
            }
        }

        // O preço é congelado no momento da criação, e é o pedido que sabe disso.
        itens.add(new ItemPedido(produto, quantidade, produto.getPrecoVigenteEmCentavos()));
    }

    public int totalEmCentavos() {
        int total = 0;
        for (ItemPedido item : itens) {
            total += item.subtotalEmCentavos();
        }
        return total;
    }

    // Devolve cópia imutável: ninguém adiciona item por fora, driblando adicionarItem().
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public String getCodigo() {
        return codigo;
    }
}

// Quem REGISTRA instâncias também é candidato a criador. O cliente registra seus pedidos, então
// é ele quem os cria - e já os deixa vinculados a si.
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

// Classe Cliente
class Criador {

    public static void main(String[] args) {
        Cliente ana = new Cliente("Ana Souza");

        // A camada de aplicação NUNCA faz "new ItemPedido(...)". Ela nem conseguiria, se estas
        // classes estivessem em pacotes diferentes.
        Pedido pedido = ana.abrirPedido();

        Produto teclado = new Produto("Teclado", 25000);
        Produto mouse = new Produto("Mouse", 8000);

        pedido.adicionarItem(teclado, 2);
        pedido.adicionarItem(mouse, 1);
        pedido.adicionarItem(teclado, 3);   // consolida com o item existente, em vez de duplicar

        System.out.println("pedido " + pedido.getCodigo() + " de " + ana.getNome());
        System.out.println("  linhas: " + pedido.getItens().size());
        System.out.println("  total: " + pedido.totalEmCentavos() + " centavos");

        // A regra de quantidade é imposta no ponto de criação: não há como criar um item inválido.
        try {
            pedido.adicionarItem(mouse, 500);
        } catch (IllegalArgumentException e) {
            System.out.println("  recusado: " + e.getMessage());
        }

        ana.abrirPedido();
        System.out.println("pedidos de " + ana.getNome() + ": " + ana.quantidadeDePedidos());
    }
}

//Por que o Creator importa: quem cria fica ACOPLADO ao que cria. Escolher como criador uma classe
//que já estava acoplada ao objeto criado não acrescenta acoplamento nenhum ao sistema - por isso
//o Creator é uma aplicação direta do Low Coupling.
//
//Quando NÃO seguir: se a criação for complexa (muitas variantes, muitos parâmetros opcionais,
//escolha de implementação concreta), delegue a um objeto especializado. É aí que entram os padrões
//criacionais do GoF - Factory Method, Abstract Factory, Builder e Prototype - que são exatamente
//a resposta para os casos em que o criador "natural" não deve carregar essa responsabilidade.

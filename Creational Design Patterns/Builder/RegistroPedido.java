//Suponha que a sua tarefa seja construir um objeto Pedido que possui muitos atributos, sendo
//poucos obrigatórios e vários opcionais:
//Cliente e itens (obrigatórios)
//Endereço de entrega, cupom de desconto, observação, embalagem para presente (opcionais)

//Imagine a construção feita por construtor. Como os opcionais podem aparecer em qualquer
//combinação, acaba-se com um construtor por combinação - o chamado "construtor telescópico".
//Além de explodir em número, ele produz chamadas ilegíveis:
//  new Pedido(cliente, itens, null, null, "sem cebola", false);
//E se dois parâmetros vizinhos tiverem o mesmo tipo, trocá-los de posição compila sem erro.

//O Builder resolve o problema de separar a CONSTRUÇÃO de um objeto complexo da sua
//REPRESENTAÇÃO, permitindo montar o objeto passo a passo e só então validá-lo por inteiro.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Cliente {
    private final String nome;

    Cliente(String nome) {
        this.nome = nome;
    }

    String getNome() {
        return nome;
    }
}

class Item {
    private final String descricao;
    private final int valorEmCentavos;

    Item(String descricao, int valorEmCentavos) {
        this.descricao = descricao;
        this.valorEmCentavos = valorEmCentavos;
    }

    String getDescricao() {
        return descricao;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }
}

// Padrão Builder - o produto
// Repare que o Pedido é IMUTÁVEL: todos os campos são final e não há setter. Isso só é viável
// porque o builder junta os dados antes de chamar o construtor, que é privado.
class Pedido {
    private final Cliente cliente;
    private final List<Item> itens;
    private final String enderecoEntrega;
    private final String cupomDesconto;
    private final String observacao;
    private final boolean embalagemPresente;

    // Construtor privado: o único caminho de criação é o builder.
    private Pedido(PedidoBuilder builder) {
        this.cliente = builder.cliente;
        this.itens = Collections.unmodifiableList(new ArrayList<>(builder.itens));
        this.enderecoEntrega = builder.enderecoEntrega;
        this.cupomDesconto = builder.cupomDesconto;
        this.observacao = builder.observacao;
        this.embalagemPresente = builder.embalagemPresente;
    }

    public int valorTotalEmCentavos() {
        int total = 0;
        for (Item item : itens) {
            total += item.getValorEmCentavos();
        }
        return total;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<Item> getItens() {
        return itens;
    }

    public String getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public String getCupomDesconto() {
        return cupomDesconto;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isEmbalagemPresente() {
        return embalagemPresente;
    }

    // Ponto de entrada do padrão. Os obrigatórios entram aqui e não têm método "with":
    // fica impossível esquecê-los, o compilador cobra.
    public static PedidoBuilder para(Cliente cliente) {
        return new PedidoBuilder(cliente);
    }

    // Padrão Builder - o construtor propriamente dito
    // É uma classe estática aninhada porque só existe em função do produto e precisa enxergar
    // o construtor privado dele.
    static class PedidoBuilder {
        private final Cliente cliente;
        private final List<Item> itens = new ArrayList<>();
        private String enderecoEntrega;
        private String cupomDesconto;
        private String observacao;
        private boolean embalagemPresente;

        private PedidoBuilder(Cliente cliente) {
            this.cliente = cliente;
        }

        // Cada método devolve "this": é isso que permite encadear as chamadas (interface fluente).
        public PedidoBuilder comItem(String descricao, int valorEmCentavos) {
            itens.add(new Item(descricao, valorEmCentavos));
            return this;
        }

        public PedidoBuilder entregarEm(String enderecoEntrega) {
            this.enderecoEntrega = enderecoEntrega;
            return this;
        }

        public PedidoBuilder comCupom(String cupomDesconto) {
            this.cupomDesconto = cupomDesconto;
            return this;
        }

        public PedidoBuilder comObservacao(String observacao) {
            this.observacao = observacao;
            return this;
        }

        public PedidoBuilder embrulharParaPresente() {
            this.embalagemPresente = true;
            return this;
        }

        // A validação vive no build(), não nos setters. Só aqui o objeto está completo, então só
        // aqui dá para checar regras que envolvem mais de um campo.
        public Pedido build() {
            if (cliente == null) {
                throw new IllegalStateException("pedido exige cliente");
            }
            if (itens.isEmpty()) {
                throw new IllegalStateException("pedido exige ao menos um item");
            }
            if (embalagemPresente && enderecoEntrega == null) {
                throw new IllegalStateException("embalagem para presente exige endereço de entrega");
            }
            return new Pedido(this);
        }
    }
}

// Classe Cliente
class RegistroPedido {

    public void registrar() {
        // Compare com "new Pedido(cliente, itens, null, null, "sem cebola", false)":
        // aqui cada valor está rotulado pelo nome do método e a ordem não importa.
        Pedido pedido = Pedido.para(new Cliente("Ana"))
                              .comItem("Teclado", 25000)
                              .comItem("Mouse", 8000)
                              .entregarEm("Rua A, 100")
                              .comObservacao("entregar após as 18h")
                              .embrulharParaPresente()
                              .build();

        System.out.println("pedido de " + pedido.getCliente().getNome()
                           + " com " + pedido.getItens().size() + " itens"
                           + " totalizando " + pedido.valorTotalEmCentavos() + " centavos");

        // Pedido sem os opcionais: o mesmo builder atende, sem sobrecarga nova.
        Pedido simples = Pedido.para(new Cliente("Bruno"))
                               .comItem("Cabo HDMI", 3000)
                               .build();

        System.out.println("pedido simples de " + simples.getCliente().getNome()
                           + " valendo " + simples.valorTotalEmCentavos() + " centavos");

        // A validação combinada só é possível porque acontece no build().
        try {
            Pedido.para(new Cliente("Carla"))
                  .comItem("Livro", 5000)
                  .embrulharParaPresente()
                  .build();
        } catch (IllegalStateException e) {
            System.out.println("recusado: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new RegistroPedido().registrar();
    }
}

//Observação: o GoF descreve o Builder com um Director, uma classe que conhece a SEQUÊNCIA de
//passos e a executa sobre um builder abstrato, permitindo trocar a representação final sem mudar
//o algoritmo de montagem. A variação acima, popularizada por Joshua Bloch, dispensa o Director e
//é a que se vê no dia a dia em Java - é o mesmo padrão com foco em legibilidade e imutabilidade
//em vez de troca de representação.

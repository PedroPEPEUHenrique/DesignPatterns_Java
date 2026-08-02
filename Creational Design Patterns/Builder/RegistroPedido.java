//Construir um Pedido com poucos atributos obrigatórios (cliente, itens) e vários opcionais
//(entrega, cupom, observação, presente). Por construtor, cada combinação de opcionais vira uma
//sobrecarga - o "construtor telescópico".
//O Builder separa a CONSTRUÇÃO de um objeto complexo da sua REPRESENTAÇÃO, montando-o passo a
//passo e validando-o só no fim.

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

// Produto: imutável, sem setter. Só é viável porque o builder junta os dados antes do construtor.
class Pedido {
    private final Cliente cliente;
    private final List<Item> itens;
    private final String enderecoEntrega;
    private final String cupomDesconto;
    private final String observacao;
    private final boolean embalagemPresente;

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

    // Os obrigatórios entram aqui e não têm método "with": o compilador cobra.
    public static PedidoBuilder para(Cliente cliente) {
        return new PedidoBuilder(cliente);
    }

    // Builder
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

        // Devolver "this" é o que permite encadear as chamadas (interface fluente).
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

        // A validação vive no build(): só aqui o objeto está completo e regras que envolvem mais
        // de um campo podem ser checadas.
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

// Cliente
class RegistroPedido {

    public void registrar() {
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

        Pedido simples = Pedido.para(new Cliente("Bruno"))
                               .comItem("Cabo HDMI", 3000)
                               .build();

        System.out.println("pedido simples de " + simples.getCliente().getNome()
                           + " valendo " + simples.valorTotalEmCentavos() + " centavos");

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

//O GoF descreve o Builder com um Director, que conhece a SEQUÊNCIA de passos e permite trocar a
//representação final. A variação acima, de Joshua Bloch, dispensa o Director e foca em
//legibilidade e imutabilidade - é a que se vê no dia a dia em Java.

//Suponha que a sua tarefa seja implementar um método que remova os itens inválidos de uma coleção de itens de um pedido. Um item de pedido possui os seguintes atributos:
//Quantidade
//Preço unitário
//Produto solicitado
 
//Imagine que um item válido é aquele que tenha uma quantidade de 1 a 100. Veja a implementação da classe ItemPedido a seguir.

//Além dos atributos, das operações de acesso e do construtor, essa classe define a operação valor, que retorna o valor do item resultante da multiplicação da quantidade pelo preço unitário.

import java.util.Collection;
import java.util.Iterator;

class Produto { }   // stub: só para as anotações fecharem

class ItemPedido{
    private Produto produto;
    private int quantidade;
    private int precoUnitarioEmCentavos;


    public ItemPedido(Produto produto, int quantidade, int valorEmCentavos){
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = valorEmCentavos;
    }
    public int valor(){
        return quantidade * precoUnitarioEmCentavos;
    }
    public Produto getProduto(){
        return produto;
    }
    public void setProduto(Produto produto){
        this.produto = produto;
    }
    public int getQuantidade(){
        return quantidade;
    }
    // ERRO 1: estava "public int" sem return -> não compila. Setter é void.
    public void setQuantidade(int quantidade){
        this.quantidade=quantidade;
    }
    public int getPrecoUnitarioEmCentavos(){
        return precoUnitarioEmCentavos;
    }
    public void setPrecoUnitarioEmCentavos(int precoUnitarioEmCentavos){
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }
}

// ERRO 2: métodos soltos fora de classe. Em Java todo método vive dentro de um tipo.
class ValidadorPedido {

    // ERRO 3: os sinais ‹ › (aspas angulares) não são generics. O correto é < >.
    public void removerItensInvalidos(Collection<ItemPedido> itens) {
        Iterator<ItemPedido> iterator = itens.iterator();
        while (iterator.hasNext()) {
            ItemPedido item = iterator.next();
            if (!isValido(item)) {
                // ERRO 4: itens.remove(item) durante a iteração lança
                // ConcurrentModificationException. Remove-se pelo próprio iterator.
                iterator.remove();
            }
        }
    }

    // ERRO 5: o enunciado diz "de 1 a 100" (inclusive), mas < 100 rejeitava o item de 100.
    public boolean isValido(ItemPedido item){
        return item.getQuantidade() >= 1 && item.getQuantidade() <= 100;
    }
}

//O Factory Method resolve o problema mais geral de evitar que o módulo cliente precise instanciar diretamente uma dentre várias implementações de uma abstração, fazendo com que ele dependa apenas da abstração e não das implementações concretas.

// Padrão Strategy
interface PagamentoStrategy {
    void pagar(double valor);
}

class PagamentoCartao implements PagamentoStrategy {
    @Override
    public void pagar(double valor) {
        System.out.println("Pagamento de " + valor + " com Cartão de Crédito.");
    }
}
class PagamentoPix implements PagamentoStrategy {
    @Override
    public void pagar(double valor)  {
        System.out.println("Pagamento de " + valor + " com Pix.");
    }
}

// Padrão Factory Method
abstract class PagamentoFactory {
    public abstract PagamentoStrategy criarMetodoPagamento();
}

class CartaoFactory extends PagamentoFactory {
    @Override
    public PagamentoStrategy criarMetodoPagamento() {
        return new PagamentoCartao();
    }
}
class PixFactory extends PagamentoFactory {
    @Override
    public PagamentoStrategy criarMetodoPagamento() {
        return new PagamentoPix();
    }
}

// Classe Cliente
class ServicoPagamento {
    private PagamentoStrategy pagamento;

    public ServicoPagamento (PagamentoFactory factory) {
        this. pagamento = factory.criarMetodoPagamento();
    }
    public void executarPagamento(double valor) {
        pagamento.pagar(valor);
    }

    public static void main(String[] args) {
        new ServicoPagamento(new CartaoFactory()).executarPagamento(100);
    }
}
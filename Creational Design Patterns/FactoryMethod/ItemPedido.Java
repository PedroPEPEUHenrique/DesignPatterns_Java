//Implementar um método que remova de uma coleção os itens inválidos de um pedido. Um item tem
//produto, quantidade e preço unitário, e a operação valor() devolve quantidade x preço unitário.
//É válido o item com quantidade de 1 a 100, inclusive nos dois extremos. A implementação original
//trazia cinco erros: setter declarado com tipo de retorno, métodos soltos fora de classe, aspas
//angulares no lugar de < >, remoção pela coleção durante a iteração (o que lança
//ConcurrentModificationException) e o limite superior tratado como exclusivo.
//A segunda parte do arquivo é o Factory Method: ele evita que o cliente instancie diretamente uma
//dentre várias implementações de uma abstração, delegando a criação a subclasses de uma fábrica -
//o cliente passa a depender só da abstração. Aqui a fábrica devolve uma estratégia de pagamento,
//com Factory Method e Strategy trabalhando juntos.

import java.util.Collection;
import java.util.Iterator;

class Produto { }

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

class ValidadorPedido {

    public void removerItensInvalidos(Collection<ItemPedido> itens) {
        Iterator<ItemPedido> iterator = itens.iterator();
        while (iterator.hasNext()) {
            ItemPedido item = iterator.next();
            if (!isValido(item)) {

                iterator.remove();
            }
        }
    }

    public boolean isValido(ItemPedido item){
        return item.getQuantidade() >= 1 && item.getQuantidade() <= 100;
    }
}

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

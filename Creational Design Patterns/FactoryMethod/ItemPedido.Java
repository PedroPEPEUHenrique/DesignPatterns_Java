//Suponha que a sua tarefa seja implementar um método que remova os itens inválidos de uma coleção de itens de um pedido. Um item de pedido possui os seguintes atributos:
//Quantidade
//Preço unitário
//Produto solicitado
 
//Imagine que um item válido é aquele que tenha uma quantidade de 1 a 100. Veja a implementação da classe ItemPedido a seguir.

//Além dos atributos, das operações de acesso e do construtor, essa classe define a operação valor, que retorna o valor do item resultante da multiplicação da quantidade pelo preço unitário.


public class ItemPedido{
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
    public int setQuantidade(int quantidade){
        this.quantidade=quantidade;
    }
    public int getPrecoUnitarioEmCentavos(){
        return precoUnitarioEmCentavos;
    }
    public void setPrecoUnitarioEmCentavos(int precoUnitarioEmCentavos){
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }
}


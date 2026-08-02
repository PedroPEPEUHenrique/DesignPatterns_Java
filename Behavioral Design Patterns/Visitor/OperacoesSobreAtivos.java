//Calcular imposto, valor de mercado, relatório e exportação sobre uma carteira de ativos (ação,
//fundo imobiliário, título público). Colocar cada operação dentro de cada classe de ativo infla o
//domínio com regras fiscais e de relatório, e cada operação nova reabre TODAS as classes.
//O Visitor representa uma operação a ser realizada sobre os elementos de uma estrutura de objetos,
//permitindo definir uma operação NOVA sem alterar as classes dos elementos.

import java.util.ArrayList;
import java.util.List;

// Visitor: um método por tipo concreto de elemento.
interface VisitanteAtivo<R> {

    R visitarAcao(Acao acao);

    R visitarFundoImobiliario(FundoImobiliario fundo);

    R visitarTituloPublico(TituloPublico titulo);
}

// Element: a ÚNICA coisa que o domínio precisa oferecer é o accept.
interface Ativo {
    <R> R aceitar(VisitanteAtivo<R> visitante);
}

class Acao implements Ativo {
    private final String ticker;
    private final int quantidade;
    private final int precoMedioEmCentavos;
    private final int precoAtualEmCentavos;

    Acao(String ticker, int quantidade, int precoMedioEmCentavos, int precoAtualEmCentavos) {
        this.ticker = ticker;
        this.quantidade = quantidade;
        this.precoMedioEmCentavos = precoMedioEmCentavos;
        this.precoAtualEmCentavos = precoAtualEmCentavos;
    }

    String getTicker() {
        return ticker;
    }

    int getQuantidade() {
        return quantidade;
    }

    int getPrecoMedioEmCentavos() {
        return precoMedioEmCentavos;
    }

    int getPrecoAtualEmCentavos() {
        return precoAtualEmCentavos;
    }

    int lucroEmCentavos() {
        return (precoAtualEmCentavos - precoMedioEmCentavos) * quantidade;
    }

    // DOUBLE DISPATCH: a 1ª chamada resolve o TIPO DO ELEMENTO pelo polimorfismo normal; a 2ª
    // resolve a OPERAÇÃO pelo tipo do visitante. Java só despacha sobre o receptor, nunca sobre o
    // argumento - por isso este "vai e volta" é necessário.
    @Override
    public <R> R aceitar(VisitanteAtivo<R> visitante) {
        return visitante.visitarAcao(this);
    }
}

class FundoImobiliario implements Ativo {
    private final String codigo;
    private final int cotas;
    private final int valorCotaEmCentavos;
    private final int rendimentoMensalEmCentavos;

    FundoImobiliario(String codigo, int cotas, int valorCotaEmCentavos, int rendimentoMensalEmCentavos) {
        this.codigo = codigo;
        this.cotas = cotas;
        this.valorCotaEmCentavos = valorCotaEmCentavos;
        this.rendimentoMensalEmCentavos = rendimentoMensalEmCentavos;
    }

    String getCodigo() {
        return codigo;
    }

    int getCotas() {
        return cotas;
    }

    int getValorCotaEmCentavos() {
        return valorCotaEmCentavos;
    }

    int getRendimentoMensalEmCentavos() {
        return rendimentoMensalEmCentavos;
    }

    @Override
    public <R> R aceitar(VisitanteAtivo<R> visitante) {
        return visitante.visitarFundoImobiliario(this);
    }
}

class TituloPublico implements Ativo {
    private final String nome;
    private final int valorAplicadoEmCentavos;
    private final int rendimentoEmCentavos;
    private final int diasAplicado;

    TituloPublico(String nome, int valorAplicadoEmCentavos, int rendimentoEmCentavos, int diasAplicado) {
        this.nome = nome;
        this.valorAplicadoEmCentavos = valorAplicadoEmCentavos;
        this.rendimentoEmCentavos = rendimentoEmCentavos;
        this.diasAplicado = diasAplicado;
    }

    String getNome() {
        return nome;
    }

    int getValorAplicadoEmCentavos() {
        return valorAplicadoEmCentavos;
    }

    int getRendimentoEmCentavos() {
        return rendimentoEmCentavos;
    }

    int getDiasAplicado() {
        return diasAplicado;
    }

    @Override
    public <R> R aceitar(VisitanteAtivo<R> visitante) {
        return visitante.visitarTituloPublico(this);
    }
}

// Toda a regra fiscal reunida em UMA classe, em vez de espalhada por três.
class CalculoImposto implements VisitanteAtivo<Integer> {

    @Override
    public Integer visitarAcao(Acao acao) {
        int lucro = acao.lucroEmCentavos();
        return lucro > 0 ? (int) (lucro * 0.15) : 0;
    }

    @Override
    public Integer visitarFundoImobiliario(FundoImobiliario fundo) {
        return 0;   // rendimento de FII é isento para pessoa física
    }

    @Override
    public Integer visitarTituloPublico(TituloPublico titulo) {
        double aliquota = titulo.getDiasAplicado() <= 180 ? 0.225
                        : titulo.getDiasAplicado() <= 360 ? 0.20
                        : titulo.getDiasAplicado() <= 720 ? 0.175
                        : 0.15;
        return (int) (titulo.getRendimentoEmCentavos() * aliquota);
    }
}

// Operação NOVA sem tocar em nenhuma classe de ativo: é esse o ganho do padrão.
class ValorDeMercado implements VisitanteAtivo<Integer> {

    @Override
    public Integer visitarAcao(Acao acao) {
        return acao.getPrecoAtualEmCentavos() * acao.getQuantidade();
    }

    @Override
    public Integer visitarFundoImobiliario(FundoImobiliario fundo) {
        return fundo.getValorCotaEmCentavos() * fundo.getCotas();
    }

    @Override
    public Integer visitarTituloPublico(TituloPublico titulo) {
        return titulo.getValorAplicadoEmCentavos() + titulo.getRendimentoEmCentavos();
    }
}

// O tipo de retorno pode ser qualquer um, graças ao parâmetro genérico da interface.
class DescricaoAtivo implements VisitanteAtivo<String> {

    @Override
    public String visitarAcao(Acao acao) {
        return acao.getQuantidade() + " ações de " + acao.getTicker();
    }

    @Override
    public String visitarFundoImobiliario(FundoImobiliario fundo) {
        return fundo.getCotas() + " cotas do FII " + fundo.getCodigo();
    }

    @Override
    public String visitarTituloPublico(TituloPublico titulo) {
        return "título " + titulo.getNome() + " há " + titulo.getDiasAplicado() + " dias";
    }
}

// Um visitante pode acumular resultado parcial entre as visitas.
class RendaMensalProjetada implements VisitanteAtivo<Void> {

    private int totalEmCentavos;

    @Override
    public Void visitarAcao(Acao acao) {
        return null;
    }

    @Override
    public Void visitarFundoImobiliario(FundoImobiliario fundo) {
        totalEmCentavos += fundo.getRendimentoMensalEmCentavos() * fundo.getCotas();
        return null;
    }

    @Override
    public Void visitarTituloPublico(TituloPublico titulo) {
        totalEmCentavos += titulo.getRendimentoEmCentavos() / Math.max(1, titulo.getDiasAplicado() / 30);
        return null;
    }

    int getTotalEmCentavos() {
        return totalEmCentavos;
    }
}

// Cliente: sem instanceof, sem cast, sem switch por tipo.
class OperacoesSobreAtivos {

    private final List<Ativo> carteira = new ArrayList<>();

    public void adicionar(Ativo ativo) {
        carteira.add(ativo);
    }

    public int somar(VisitanteAtivo<Integer> visitante) {
        int total = 0;
        for (Ativo ativo : carteira) {
            total += ativo.aceitar(visitante);
        }
        return total;
    }

    public void aplicar(VisitanteAtivo<Void> visitante) {
        for (Ativo ativo : carteira) {
            ativo.aceitar(visitante);
        }
    }

    public void listar() {
        DescricaoAtivo descricao = new DescricaoAtivo();
        ValorDeMercado valor = new ValorDeMercado();
        for (Ativo ativo : carteira) {
            System.out.println("  " + ativo.aceitar(descricao)
                               + " | mercado: " + ativo.aceitar(valor)
                               + " | imposto: " + ativo.aceitar(new CalculoImposto()));
        }
    }

    public static void main(String[] args) {
        OperacoesSobreAtivos carteira = new OperacoesSobreAtivos();
        carteira.adicionar(new Acao("PETR4", 100, 3200, 3900));
        carteira.adicionar(new Acao("VALE3", 50, 7000, 6500));
        carteira.adicionar(new FundoImobiliario("HGLG11", 30, 16000, 110));
        carteira.adicionar(new TituloPublico("Tesouro Selic 2029", 500000, 62000, 400));

        System.out.println("carteira:");
        carteira.listar();

        System.out.println("valor de mercado total: " + carteira.somar(new ValorDeMercado()));
        System.out.println("imposto devido total: " + carteira.somar(new CalculoImposto()));

        RendaMensalProjetada renda = new RendaMensalProjetada();
        carteira.aplicar(renda);
        System.out.println("renda mensal projetada: " + renda.getTotalEmCentavos());
    }
}

//O trade-off: é FÁCIL acrescentar uma OPERAÇÃO nova (uma classe) e DIFÍCIL acrescentar um TIPO
//novo de elemento (altera a interface do visitante e todos os visitantes existentes). Use quando a
//hierarquia for estável e as operações variarem muito; no caso contrário, polimorfismo comum.
//Outro custo: o visitante precisa do estado do elemento, o que empurra a classe de domínio a
//expor getters.
//Onde aparece: compiladores percorrendo a árvore sintática e o FileVisitor de Files.walkFileTree.

//Suponha que a sua tarefa seja calcular impostos sobre a carteira de ativos de um cliente. Os
//tipos de ativo são estáveis e bem definidos:
//Ação
//Fundo imobiliário
//Título público

//Imagine que, além do imposto, pedem o valor de mercado, depois um relatório em texto, depois a
//exportação para o formato do contador. Colocar cada operação como um método dentro de cada classe
//de ativo faz as classes de domínio incharem com regras fiscais, de relatório e de exportação -
//responsabilidades que não são delas. E cada operação nova reabre TODAS as classes de ativo.

//O Visitor resolve o problema de representar uma operação a ser realizada sobre os elementos de
//uma estrutura de objetos, permitindo definir uma operação NOVA sem alterar as classes dos
//elementos sobre os quais ela opera.

import java.util.ArrayList;
import java.util.List;

// Padrão Visitor - a interface Visitor
// Um método por tipo concreto de elemento. É essa lista que torna o padrão caro quando os tipos
// mudam - e barato quando eles são estáveis.
interface VisitanteAtivo<R> {

    R visitarAcao(Acao acao);

    R visitarFundoImobiliario(FundoImobiliario fundo);

    R visitarTituloPublico(TituloPublico titulo);
}

// Padrão Visitor - a interface Element
// A ÚNICA coisa que o domínio precisa oferecer é o accept. Nenhuma regra fiscal, de relatório ou
// de exportação entra aqui.
interface Ativo {
    <R> R aceitar(VisitanteAtivo<R> visitante);
}

// ELEMENTOS CONCRETOS - classes de domínio limpas, com dados e nada mais.

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

    // DOUBLE DISPATCH - o mecanismo central do padrão.
    // A 1ª chamada (aceitar) resolve o TIPO DO ELEMENTO pelo polimorfismo normal do Java.
    // A 2ª chamada (visitarAcao) resolve a OPERAÇÃO pelo tipo do visitante.
    // Java só faz despacho dinâmico sobre o receptor, nunca sobre o argumento - é por isso que
    // este "vai e volta" é necessário.
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

// VISITANTE 1 - imposto. Toda a regra fiscal reunida em UMA classe, em vez de espalhada por três.
class CalculoImposto implements VisitanteAtivo<Integer> {

    @Override
    public Integer visitarAcao(Acao acao) {
        // 15% sobre o lucro, e só sobre lucro.
        int lucro = acao.lucroEmCentavos();
        return lucro > 0 ? (int) (lucro * 0.15) : 0;
    }

    @Override
    public Integer visitarFundoImobiliario(FundoImobiliario fundo) {
        // Rendimento de FII é isento para pessoa física.
        return 0;
    }

    @Override
    public Integer visitarTituloPublico(TituloPublico titulo) {
        // Tabela regressiva por prazo.
        double aliquota = titulo.getDiasAplicado() <= 180 ? 0.225
                        : titulo.getDiasAplicado() <= 360 ? 0.20
                        : titulo.getDiasAplicado() <= 720 ? 0.175
                        : 0.15;
        return (int) (titulo.getRendimentoEmCentavos() * aliquota);
    }
}

// VISITANTE 2 - valor de mercado. Operação NOVA sem tocar em Acao, FundoImobiliario ou
// TituloPublico. Esse é exatamente o ganho do padrão.
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

// VISITANTE 3 - descrição em texto. Mostra que o tipo de retorno pode ser qualquer um, graças ao
// parâmetro genérico da interface.
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

// VISITANTE 4 - acumula estado enquanto percorre. Um visitante pode guardar resultado parcial
// entre as visitas, algo difícil de fazer se a operação estivesse dentro de cada elemento.
class RendaMensalProjetada implements VisitanteAtivo<Void> {

    private int totalEmCentavos;

    @Override
    public Void visitarAcao(Acao acao) {
        return null;   // dividendos ficam fora desta projeção
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

// Classe Cliente
class OperacoesSobreAtivos {

    private final List<Ativo> carteira = new ArrayList<>();

    public void adicionar(Ativo ativo) {
        carteira.add(ativo);
    }

    // Sem instanceof, sem cast, sem switch por tipo. O visitante é aplicado uniformemente.
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

//O trade-off do Visitor, e a razão de ele ser usado com parcimônia:
//É FÁCIL acrescentar uma OPERAÇÃO nova - basta uma classe de visitante.
//É DIFÍCIL acrescentar um TIPO novo de elemento - obriga a alterar a interface do visitante e
//  todos os visitantes existentes.
//Ou seja: use quando a hierarquia de elementos for estável e as operações variarem muito. Se for
//o contrário, o polimorfismo comum (métodos nas próprias classes) é a resposta certa.
//
//Outro custo: o visitante costuma precisar de acesso ao estado do elemento, o que empurra a classe
//de domínio a expor getters e enfraquece o encapsulamento.
//Onde aparece: compiladores e analisadores estáticos percorrendo a árvore sintática, e o
//FileVisitor de java.nio.file.Files.walkFileTree.

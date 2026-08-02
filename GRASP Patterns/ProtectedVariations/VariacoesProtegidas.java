//GRASP - PROTECTED VARIATIONS
//Problema: como projetar para que a variação de um ponto não impacte os outros?
//Solução: identificar os pontos de VARIAÇÃO PREVISTA e criar uma INTERFACE ESTÁVEL em volta deles.
//No cálculo de preço: o imposto muda por decisão do governo, a tabela de descontos muda a cada
//campanha e o provedor de câmbio já foi trocado duas vezes - mas o conceito
//"preço = base + imposto - desconto" não muda. É o princípio mais geral do GRASP.

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Produto {
    private final String nome;
    private final String ncm;
    private final int precoBaseEmCentavos;
    private final String origem;

    Produto(String nome, String ncm, int precoBaseEmCentavos, String origem) {
        this.nome = nome;
        this.ncm = ncm;
        this.precoBaseEmCentavos = precoBaseEmCentavos;
        this.origem = origem;
    }

    String getNome() {
        return nome;
    }

    String getNcm() {
        return ncm;
    }

    int getPrecoBaseEmCentavos() {
        return precoBaseEmCentavos;
    }

    String getOrigem() {
        return origem;
    }
}

// COMO NÃO FAZER - o volátil misturado ao estável
// Cada mudança de imposto, campanha ou provedor faz alguém editar exatamente a linha onde mora a
// fórmula, que é a parte que não deveria ser tocada.
class PrecificadorSemProtecao {

    public int precoFinal(Produto produto, int quantidade) {
        int base = produto.getPrecoBaseEmCentavos() * quantidade;

        int imposto = produto.getNcm().startsWith("30") ? 0 : (int) (base * 0.18);

        int desconto = quantidade >= 10 ? (int) (base * 0.05) : 0;

        double cotacao = "IMPORTADO".equals(produto.getOrigem()) ? 5.40 : 1.0;

        return (int) ((base + imposto - desconto) * cotacao);
    }
}

// Ponto de variação 1 - regime tributário
interface PoliticaTributaria {
    int impostoEmCentavos(Produto produto, int baseEmCentavos);

    String nome();
}

class RegimeAtual implements PoliticaTributaria {

    private static final Map<String, Double> ALIQUOTA_POR_NCM = Map.of(
            "3004", 0.00,
            "1006", 0.07,
            "8471", 0.20);

    @Override
    public int impostoEmCentavos(Produto produto, int baseEmCentavos) {
        double aliquota = ALIQUOTA_POR_NCM.getOrDefault(produto.getNcm(), 0.18);
        return (int) (baseEmCentavos * aliquota);
    }

    @Override
    public String nome() {
        return "regime atual";
    }
}

// A reforma tributária entra como classe NOVA. A fórmula do preço não é tocada.
class RegimeReformaTributaria implements PoliticaTributaria {

    @Override
    public int impostoEmCentavos(Produto produto, int baseEmCentavos) {
        boolean cestaBasica = produto.getNcm().startsWith("10");
        return (int) (baseEmCentavos * (cestaBasica ? 0.09 : 0.265));
    }

    @Override
    public String nome() {
        return "IVA dual";
    }
}

// Ponto de variação 2 - campanhas de desconto
interface PoliticaDesconto {
    int descontoEmCentavos(Produto produto, int quantidade, int baseEmCentavos);

    String nome();
}

class SemDesconto implements PoliticaDesconto {

    @Override
    public int descontoEmCentavos(Produto produto, int quantidade, int baseEmCentavos) {
        return 0;
    }

    @Override
    public String nome() {
        return "sem campanha";
    }
}

class DescontoPorVolume implements PoliticaDesconto {

    @Override
    public int descontoEmCentavos(Produto produto, int quantidade, int baseEmCentavos) {
        if (quantidade >= 50) {
            return (int) (baseEmCentavos * 0.12);
        }
        if (quantidade >= 10) {
            return (int) (baseEmCentavos * 0.05);
        }
        return 0;
    }

    @Override
    public String nome() {
        return "desconto por volume";
    }
}

// Ponto de variação 3 - provedor de câmbio (instabilidade externa)
interface FonteCotacao {
    double cotacao(String moeda);
}

class CotacaoBancoCentral implements FonteCotacao {

    private final Map<String, Double> cotacoes = new HashMap<>();

    CotacaoBancoCentral() {
        cotacoes.put("USD", 5.40);
        cotacoes.put("BRL", 1.0);
    }

    @Override
    public double cotacao(String moeda) {
        return cotacoes.getOrDefault(moeda, 1.0);
    }
}

class CotacaoFixaParaTeste implements FonteCotacao {

    @Override
    public double cotacao(String moeda) {
        return "USD".equals(moeda) ? 5.0 : 1.0;
    }
}

// O NÚCLEO ESTÁVEL: só o conceito que não muda. Nenhum if aqui dentro.
class Precificador {

    private final PoliticaTributaria tributacao;
    private final PoliticaDesconto campanha;
    private final FonteCotacao cambio;

    Precificador(PoliticaTributaria tributacao, PoliticaDesconto campanha, FonteCotacao cambio) {
        this.tributacao = tributacao;
        this.campanha = campanha;
        this.cambio = cambio;
    }

    public int precoFinalEmCentavos(Produto produto, int quantidade) {
        int base = produto.getPrecoBaseEmCentavos() * quantidade;
        int imposto = tributacao.impostoEmCentavos(produto, base);
        int desconto = campanha.descontoEmCentavos(produto, quantidade, base);
        double taxa = cambio.cotacao("IMPORTADO".equals(produto.getOrigem()) ? "USD" : "BRL");

        return (int) ((base + imposto - desconto) * taxa);
    }

    public String configuracao() {
        return tributacao.nome() + " + " + campanha.nome();
    }
}

class VariacoesProtegidas {

    public static void main(String[] args) {
        Produto notebook = new Produto("Notebook", "8471", 350000, "IMPORTADO");
        Produto arroz = new Produto("Arroz 5kg", "1006", 3200, "NACIONAL");

        System.out.println("== sem proteção ==");
        PrecificadorSemProtecao antigo = new PrecificadorSemProtecao();
        System.out.println("  notebook x1: " + antigo.precoFinal(notebook, 1));

        System.out.println("== com variações protegidas ==");
        Precificador hoje = new Precificador(new RegimeAtual(), new DescontoPorVolume(),
                                             new CotacaoBancoCentral());
        System.out.println("  [" + hoje.configuracao() + "]");
        System.out.println("  notebook x1:  " + hoje.precoFinalEmCentavos(notebook, 1));
        System.out.println("  notebook x50: " + hoje.precoFinalEmCentavos(notebook, 50));
        System.out.println("  arroz x1:     " + hoje.precoFinalEmCentavos(arroz, 1));

        Precificador depoisDaReforma = new Precificador(new RegimeReformaTributaria(),
                                                        new DescontoPorVolume(),
                                                        new CotacaoBancoCentral());
        System.out.println("  [" + depoisDaReforma.configuracao() + "]");
        System.out.println("  notebook x1:  " + depoisDaReforma.precoFinalEmCentavos(notebook, 1));
        System.out.println("  arroz x1:     " + depoisDaReforma.precoFinalEmCentavos(arroz, 1));

        // O teste roda com cotação previsível, sem depender do provedor externo.
        Precificador emTeste = new Precificador(new RegimeAtual(), new SemDesconto(),
                                                new CotacaoFixaParaTeste());
        System.out.println("  [teste] notebook x1: " + emTeste.precoFinalEmCentavos(notebook, 1));

        Precificador blackFriday = new Precificador(
                new RegimeAtual(),
                new PoliticaDesconto() {
                    @Override
                    public int descontoEmCentavos(Produto produto, int quantidade, int base) {
                        return (int) (base * 0.30);
                    }

                    @Override
                    public String nome() {
                        return "black friday";
                    }
                },
                new CotacaoBancoCentral());
        System.out.println("  [" + blackFriday.configuracao() + "] notebook x1: "
                           + blackFriday.precoFinalEmCentavos(notebook, 1));

        System.out.println("\npontos de variação exercitados: "
                           + List.of("tributação", "campanha", "câmbio"));
    }
}

//Mecanismos, do mais simples ao mais elaborado: encapsulamento e interfaces, polimorfismo,
//indireção, dados em vez de código (configuração, regras em banco) e metadados - frameworks como
//JPA e CDI protegem o seu código lendo anotações em tempo de execução.
//O limite: só protege quem PREVÊ a variação certa. Proteger tudo é especulação e gera abstrações
//que nunca serão usadas. Proteja onde a variação já aconteceu ou há evidência de que vai acontecer.
//É a formulação GRASP do Open/Closed Principle e do "programe para uma interface".

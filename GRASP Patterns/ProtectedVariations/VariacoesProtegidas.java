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

class PrecificadorSemProtecao {

    public int precoFinal(Produto produto, int quantidade) {
        int base = produto.getPrecoBaseEmCentavos() * quantidade;

        int imposto = produto.getNcm().startsWith("30") ? 0 : (int) (base * 0.18);

        int desconto = quantidade >= 10 ? (int) (base * 0.05) : 0;

        double cotacao = "IMPORTADO".equals(produto.getOrigem()) ? 5.40 : 1.0;

        return (int) ((base + imposto - desconto) * cotacao);
    }
}

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

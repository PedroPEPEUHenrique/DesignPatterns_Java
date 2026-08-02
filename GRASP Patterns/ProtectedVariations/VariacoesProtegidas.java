//GRASP - PROTECTED VARIATIONS (Variações Protegidas)

//Problema: como projetar objetos e sistemas de modo que a variação ou instabilidade de um ponto
//não cause impacto indesejado em outros?
//Solução: identifique os pontos de VARIAÇÃO PREVISTA ou de instabilidade e crie uma INTERFACE
//ESTÁVEL em volta deles.

//Suponha que a sua tarefa seja calcular o preço final de um produto. Você sabe que:
//A regra de imposto muda por decisão do governo (variação prevista)
//A tabela de descontos muda a cada campanha (variação prevista)
//A cotação vem de um provedor externo que a empresa já trocou duas vezes (instabilidade)
//Mas o CONCEITO "preço final = base + imposto - desconto" não muda.

//A ideia é blindar o que é estável do que é volátil, colocando uma interface entre os dois.
//Este é o princípio mais geral do GRASP - vários dos outros são casos particulares dele.

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
// A fórmula (estável) e as três fontes de variação estão no mesmo método. Cada mudança de imposto,
// de campanha ou de provedor de câmbio faz alguém editar exatamente a linha onde mora a fórmula -
// que é justamente a parte que não deveria ser tocada.
class PrecificadorSemProtecao {

    public int precoFinal(Produto produto, int quantidade) {
        int base = produto.getPrecoBaseEmCentavos() * quantidade;

        int imposto = produto.getNcm().startsWith("30") ? 0 : (int) (base * 0.18);

        int desconto = quantidade >= 10 ? (int) (base * 0.05) : 0;

        double cotacao = "IMPORTADO".equals(produto.getOrigem()) ? 5.40 : 1.0;

        return (int) ((base + imposto - desconto) * cotacao);
    }
}

// COMO FAZER - uma interface estável em volta de cada ponto de variação.

// PONTO DE VARIAÇÃO 1 - regime tributário
interface PoliticaTributaria {
    int impostoEmCentavos(Produto produto, int baseEmCentavos);

    String nome();
}

class RegimeAtual implements PoliticaTributaria {

    private static final Map<String, Double> ALIQUOTA_POR_NCM = Map.of(
            "3004", 0.00,      // medicamentos
            "1006", 0.07,      // arroz
            "8471", 0.20);     // informática

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

// A reforma tributária entra como uma classe NOVA. A fórmula do preço não é tocada.
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

// PONTO DE VARIAÇÃO 2 - campanhas de desconto
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

// PONTO DE VARIAÇÃO 3 - provedor de câmbio (instabilidade externa)
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

// O NÚCLEO ESTÁVEL
// Esta classe expressa apenas o conceito que não muda: preço = base + imposto - desconto, em
// moeda. As três variações entram por interface. Repare que não há um único if aqui dentro.
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

// Classe Cliente
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

        // A reforma tributária entra sem que a classe Precificador seja alterada.
        Precificador depoisDaReforma = new Precificador(new RegimeReformaTributaria(),
                                                        new DescontoPorVolume(),
                                                        new CotacaoBancoCentral());
        System.out.println("  [" + depoisDaReforma.configuracao() + "]");
        System.out.println("  notebook x1:  " + depoisDaReforma.precoFinalEmCentavos(notebook, 1));
        System.out.println("  arroz x1:     " + depoisDaReforma.precoFinalEmCentavos(arroz, 1));

        // E o teste roda com cotação previsível, sem depender do provedor externo.
        Precificador emTeste = new Precificador(new RegimeAtual(), new SemDesconto(),
                                                new CotacaoFixaParaTeste());
        System.out.println("  [teste] notebook x1: " + emTeste.precoFinalEmCentavos(notebook, 1));

        // Uma campanha pontual pode ser uma lambda: nada além da montagem precisa mudar.
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

//Mecanismos de Protected Variations, do mais simples ao mais elaborado:
//Encapsulamento e interfaces - o caso acima.
//Polimorfismo - cada variação vira um tipo.
//Indireção - um intermediário absorve a mudança.
//Dados em vez de código - tabelas de configuração, arquivos de propriedades, regras em banco.
//Reflexão e metadados - frameworks como JPA e CDI protegem o seu código da variação da
//  infraestrutura lendo anotações em tempo de execução.
//
//O limite: só protege quem PREVÊ a variação certa. Proteger tudo é especulação - gera camadas de
//abstração que nunca serão usadas e um sistema mais difícil de entender do que a mudança que se
//queria evitar. O conselho prático é proteger onde a variação já aconteceu antes ou onde há
//evidência concreta de que vai acontecer; nos demais pontos, prefira o código simples e refatore
//quando a variação aparecer de verdade.
//
//Este princípio é a formulação GRASP do Open/Closed Principle e do "programe para uma interface,
//não para uma implementação" do GoF.

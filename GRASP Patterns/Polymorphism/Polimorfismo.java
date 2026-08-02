//GRASP - POLYMORPHISM (Polimorfismo)

//Problema: como tratar alternativas que variam conforme o TIPO? Como criar componentes de software
//plugáveis?
//Solução: quando o comportamento varia por tipo, atribua a responsabilidade - usando operações
//polimórficas - às classes para as quais o comportamento varia. NÃO teste o tipo com condicionais.

//Suponha que a sua tarefa seja calcular o imposto de itens de uma nota. A alíquota depende da
//categoria: medicamento, alimento, eletrônico, serviço.
//Imagine a solução com um switch sobre a categoria. Toda categoria nova reabre esse método - e
//normalmente ele não está sozinho: o mesmo switch reaparece no cálculo de desconto, na regra de
//devolução e na emissão do documento. Um esquecimento em qualquer um deles é um bug silencioso.

import java.util.ArrayList;
import java.util.List;

// COMO NÃO FAZER - condicional por tipo
// O sinal de alerta é o switch/if-else sobre um "tipo" representado como String ou enum.
class CalculadoraImpostoComSwitch {

    public int calcular(String categoria, int valorEmCentavos) {
        switch (categoria) {
            case "MEDICAMENTO":
                return 0;
            case "ALIMENTO":
                return (int) (valorEmCentavos * 0.07);
            case "ELETRONICO":
                return (int) (valorEmCentavos * 0.20);
            case "SERVICO":
                return (int) (valorEmCentavos * 0.05);
            default:
                // Este default é o problema: uma categoria nova cai aqui silenciosamente e o
                // sistema fatura errado, sem erro de compilação e sem exceção.
                return (int) (valorEmCentavos * 0.18);
        }
    }

    // E o mesmo switch se repete a cada nova regra que dependa da categoria.
    public boolean permiteDevolucao(String categoria) {
        switch (categoria) {
            case "MEDICAMENTO":
                return false;
            case "SERVICO":
                return false;
            default:
                return true;
        }
    }
}

// COMO FAZER - polimorfismo
// Cada variação vira um TIPO, e cada tipo responde por si. O condicional some.

interface ItemTributavel {

    int valorEmCentavos();

    int impostoEmCentavos();

    boolean permiteDevolucao();

    String descricao();

    // Comportamento comum pode ficar num default, evitando repetição entre as implementações.
    default int totalComImpostoEmCentavos() {
        return valorEmCentavos() + impostoEmCentavos();
    }
}

abstract class ItemBase implements ItemTributavel {

    private final String nome;
    private final int valorEmCentavos;

    protected ItemBase(String nome, int valorEmCentavos) {
        this.nome = nome;
        this.valorEmCentavos = valorEmCentavos;
    }

    @Override
    public int valorEmCentavos() {
        return valorEmCentavos;
    }

    @Override
    public String descricao() {
        return nome;
    }

    @Override
    public boolean permiteDevolucao() {
        return true;
    }
}

class Medicamento extends ItemBase {

    private final boolean tarjaPreta;

    Medicamento(String nome, int valorEmCentavos, boolean tarjaPreta) {
        super(nome, valorEmCentavos);
        this.tarjaPreta = tarjaPreta;
    }

    @Override
    public int impostoEmCentavos() {
        return 0;   // isento
    }

    @Override
    public boolean permiteDevolucao() {
        return false;
    }

    // Um dado específico deste tipo fica DENTRO deste tipo. Com o switch, "tarjaPreta" teria que
    // virar mais um campo genérico na classe única de item, nulo para todas as outras categorias.
    @Override
    public String descricao() {
        return super.descricao() + (tarjaPreta ? " (tarja preta)" : "");
    }
}

class Alimento extends ItemBase {

    Alimento(String nome, int valorEmCentavos) {
        super(nome, valorEmCentavos);
    }

    @Override
    public int impostoEmCentavos() {
        return (int) (valorEmCentavos() * 0.07);
    }
}

class Eletronico extends ItemBase {

    private final int garantiaEmMeses;

    Eletronico(String nome, int valorEmCentavos, int garantiaEmMeses) {
        super(nome, valorEmCentavos);
        this.garantiaEmMeses = garantiaEmMeses;
    }

    @Override
    public int impostoEmCentavos() {
        return (int) (valorEmCentavos() * 0.20);
    }

    @Override
    public String descricao() {
        return super.descricao() + " (garantia de " + garantiaEmMeses + " meses)";
    }
}

class Servico extends ItemBase {

    Servico(String nome, int valorEmCentavos) {
        super(nome, valorEmCentavos);
    }

    @Override
    public int impostoEmCentavos() {
        return (int) (valorEmCentavos() * 0.05);
    }

    @Override
    public boolean permiteDevolucao() {
        return false;
    }
}

// TIPO NOVO - acrescentado sem alterar nenhuma linha das classes existentes nem da nota fiscal.
// É esse o teste prático do princípio: se a regra nova obrigou a mexer em código antigo, não era
// polimorfismo, era condicional disfarçada.
class LivroDidatico extends ItemBase {

    LivroDidatico(String nome, int valorEmCentavos) {
        super(nome, valorEmCentavos);
    }

    @Override
    public int impostoEmCentavos() {
        return 0;   // imunidade constitucional
    }
}

// O CLIENTE do polimorfismo
// Nenhum switch, nenhum instanceof, nenhum cast. Ele só conhece a abstração.
class NotaFiscal {

    private final List<ItemTributavel> itens = new ArrayList<>();

    public void adicionar(ItemTributavel item) {
        itens.add(item);
    }

    public int impostoTotalEmCentavos() {
        int total = 0;
        for (ItemTributavel item : itens) {
            total += item.impostoEmCentavos();
        }
        return total;
    }

    public int totalEmCentavos() {
        int total = 0;
        for (ItemTributavel item : itens) {
            total += item.totalComImpostoEmCentavos();
        }
        return total;
    }

    public void imprimir() {
        for (ItemTributavel item : itens) {
            System.out.println("  " + item.descricao()
                               + " | valor " + item.valorEmCentavos()
                               + " | imposto " + item.impostoEmCentavos()
                               + " | devolução " + (item.permiteDevolucao() ? "sim" : "não"));
        }
    }
}

// Classe Cliente
class Polimorfismo {

    public static void main(String[] args) {
        System.out.println("com switch (frágil): "
                           + new CalculadoraImpostoComSwitch().calcular("ELETRONICO", 100000));
        System.out.println("categoria desconhecida cai no default e fatura errado: "
                           + new CalculadoraImpostoComSwitch().calcular("LIVRO", 100000));

        System.out.println("\ncom polimorfismo:");
        NotaFiscal nota = new NotaFiscal();
        nota.adicionar(new Medicamento("Dipirona", 1500, false));
        nota.adicionar(new Alimento("Arroz 5kg", 3200));
        nota.adicionar(new Eletronico("Fone bluetooth", 25000, 12));
        nota.adicionar(new Servico("Instalação", 12000));
        nota.adicionar(new LivroDidatico("Padrões de Projeto", 18000));   // tipo novo, zero mudança

        nota.imprimir();
        System.out.println("imposto total: " + nota.impostoTotalEmCentavos());
        System.out.println("total da nota: " + nota.totalEmCentavos());
    }
}

//Como reconhecer o problema no código: procure por switch/if-else sobre um campo "tipo",
//"categoria" ou "status", especialmente quando o MESMO encadeamento aparece em mais de um método.
//A refatoração correspondente chama-se "Replace Conditional with Polymorphism".
//
//Quando o condicional é aceitável: quando a variação é sobre um VALOR e não sobre um tipo (faixas
//de valor, por exemplo), quando existe um único ponto de decisão e ele é estável, e nos pontos de
//fronteira - alguém precisa converter a String vinda do banco ou do JSON no objeto certo. O objetivo
//é ter esse "if" em UM lugar só (tipicamente uma fábrica), e não espalhado pelo sistema.
//
//Este princípio é a base do padrão Strategy, do State e do Command, e é a leitura GRASP do
//Open/Closed Principle: aberto para extensão, fechado para modificação.

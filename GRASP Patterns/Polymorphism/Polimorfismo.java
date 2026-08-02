//GRASP - POLYMORPHISM
//Problema: como tratar alternativas que variam conforme o TIPO?
//Solução: atribuir a responsabilidade, com operações polimórficas, às classes para as quais o
//comportamento varia. NÃO testar o tipo com condicionais - o mesmo switch acaba reaparecendo no
//cálculo de desconto, na regra de devolução e na emissão do documento.

import java.util.ArrayList;
import java.util.List;

// COMO NÃO FAZER - condicional por tipo
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
                // Uma categoria nova cai aqui silenciosamente e o sistema fatura errado, sem erro
                // de compilação e sem exceção.
                return (int) (valorEmCentavos * 0.18);
        }
    }

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

// COMO FAZER - cada variação vira um TIPO, e cada tipo responde por si.

interface ItemTributavel {

    int valorEmCentavos();

    int impostoEmCentavos();

    boolean permiteDevolucao();

    String descricao();

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
        return 0;
    }

    @Override
    public boolean permiteDevolucao() {
        return false;
    }

    // Um dado específico deste tipo fica DENTRO dele. Com o switch, "tarjaPreta" viraria mais um
    // campo genérico na classe única, nulo para todas as outras categorias.
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

// Tipo NOVO, sem alterar nenhuma classe existente nem a nota fiscal. Se a regra nova obrigou a
// mexer em código antigo, não era polimorfismo, era condicional disfarçada.
class LivroDidatico extends ItemBase {

    LivroDidatico(String nome, int valorEmCentavos) {
        super(nome, valorEmCentavos);
    }

    @Override
    public int impostoEmCentavos() {
        return 0;
    }
}

// Cliente: nenhum switch, nenhum instanceof, nenhum cast.
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
        nota.adicionar(new LivroDidatico("Padrões de Projeto", 18000));

        nota.imprimir();
        System.out.println("imposto total: " + nota.impostoTotalEmCentavos());
        System.out.println("total da nota: " + nota.totalEmCentavos());
    }
}

//Como reconhecer o problema: switch/if-else sobre um campo "tipo", "categoria" ou "status",
//especialmente quando o MESMO encadeamento aparece em mais de um método. A refatoração chama-se
//"Replace Conditional with Polymorphism".
//O condicional é aceitável quando a variação é sobre um VALOR (faixas), e nos pontos de fronteira -
//alguém precisa converter a String do banco no objeto certo. O objetivo é ter esse if em UM lugar
//só, tipicamente uma fábrica.
//É a base do Strategy, do State e do Command, e a leitura GRASP do Open/Closed Principle.

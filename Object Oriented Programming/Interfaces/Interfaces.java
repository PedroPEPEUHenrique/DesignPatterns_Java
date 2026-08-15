//POO - INTERFACES
//A interface é um CONTRATO: diz o que um tipo sabe fazer, sem dizer como. Uma classe implementa
//quantas interfaces precisar - é assim que Java, sem herança múltipla, deixa um objeto
//desempenhar mais de um papel.
//Programe para a interface, não para a implementação: quem depende do contrato aceita qualquer
//implementação, inclusive uma falsa, escrita só para o teste.
//A interface pode ter método DEFAULT, com implementação padrão, o que permite acrescentar
//comportamento sem quebrar quem já a implementava, e método STATIC, utilitário do próprio
//contrato. O que ela não tem é estado: seus campos são sempre public static final.
//Interface com UM único método abstrato é uma INTERFACE FUNCIONAL e pode ser satisfeita por um
//lambda. Comparator, Runnable e Predicate, da biblioteca padrão, são exatamente isso.
//Classe abstrata ou interface? A classe abstrata compartilha ESTADO e implementação, e é herança
//("é um"); a interface compartilha CAPACIDADE ("é capaz de") e não amarra a hierarquia.

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

interface Exportavel {

    String exportar();

    default String exportarComRodape() {
        return exportar() + System.lineSeparator() + "-- gerado automaticamente --";
    }

    static String cabecalho(String titulo) {
        return "===== " + titulo.toUpperCase() + " =====";
    }
}

interface Assinavel {

    void assinar(String responsavel);

    boolean estaAssinado();
}

@FunctionalInterface
interface RegraDeFrete {

    int calcularEmCentavos(int pesoEmGramas);
}

class NotaFiscal implements Exportavel, Assinavel {

    private final String numero;
    private final int valorEmCentavos;
    private String responsavel;

    NotaFiscal(String numero, int valorEmCentavos) {
        this.numero = numero;
        this.valorEmCentavos = valorEmCentavos;
    }

    @Override
    public String exportar() {
        return "NF " + numero + " no valor de " + valorEmCentavos + " centavos";
    }

    @Override
    public void assinar(String responsavel) {
        if (estaAssinado()) {
            throw new IllegalStateException("nota já assinada por " + this.responsavel);
        }
        this.responsavel = responsavel;
    }

    @Override
    public boolean estaAssinado() {
        return responsavel != null;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }

    String getNumero() {
        return numero;
    }
}

class RelatorioDeVendas implements Exportavel {

    private final List<String> linhas = new ArrayList<>();

    void registrar(String linha) {
        linhas.add(linha);
    }

    @Override
    public String exportar() {
        return String.join("; ", linhas);
    }

    @Override
    public String exportarComRodape() {
        return exportar() + System.lineSeparator() + "-- " + linhas.size() + " registro(s) --";
    }
}

class ServicoDeArquivamento {

    void arquivar(Exportavel documento) {
        System.out.println("arquivando: " + documento.exportarComRodape());
    }
}

class Interfaces {

    public static void main(String[] args) {
        NotaFiscal nota = new NotaFiscal("000123", 45900);
        RelatorioDeVendas relatorio = new RelatorioDeVendas();
        relatorio.registrar("teclado 250,00");
        relatorio.registrar("monitor 900,00");

        System.out.println(Exportavel.cabecalho("documentos"));

        ServicoDeArquivamento servico = new ServicoDeArquivamento();
        servico.arquivar(nota);
        servico.arquivar(relatorio);

        nota.assinar("Ana");
        System.out.println("nota assinada? " + nota.estaAssinado());

        RegraDeFrete porPeso = peso -> 1500 + (peso / 500) * 300;
        RegraDeFrete gratis = peso -> 0;
        System.out.println("frete por peso: " + porPeso.calcularEmCentavos(2000));
        System.out.println("frete promocional: " + gratis.calcularEmCentavos(2000));

        List<NotaFiscal> notas = new ArrayList<>();
        notas.add(new NotaFiscal("000124", 12000));
        notas.add(nota);
        notas.add(new NotaFiscal("000125", 8900));
        notas.sort(Comparator.comparingInt(NotaFiscal::getValorEmCentavos));

        for (NotaFiscal item : notas) {
            System.out.println("  " + item.getNumero() + ": " + item.getValorEmCentavos());
        }
    }
}

//Implementar desfazer/refazer em um editor. Se a classe de histórico ler os campos do documento
//para copiá-los, o documento precisa expor tudo o que tem - o encapsulamento morre para que o
//histórico funcione.
//O Memento captura e externaliza o estado interno de um objeto SEM violar o seu encapsulamento,
//de modo que ele possa ser restaurado depois.

import java.util.ArrayDeque;
import java.util.Deque;

class Documento {

    private String conteudo = "";
    private int posicaoCursor;
    private String selecao = "";

    public void digitar(String texto) {
        conteudo = conteudo.substring(0, posicaoCursor) + texto + conteudo.substring(posicaoCursor);
        posicaoCursor += texto.length();
        selecao = "";
    }

    public void selecionar(int inicio, int fim) {
        selecao = conteudo.substring(inicio, Math.min(fim, conteudo.length()));
        posicaoCursor = fim;
    }

    public void apagarSelecao() {
        if (selecao.isEmpty()) {
            return;
        }
        conteudo = conteudo.replace(selecao, "");
        posicaoCursor = Math.min(posicaoCursor, conteudo.length());
        selecao = "";
    }

    public Instantaneo salvar() {
        return new Instantaneo(conteudo, posicaoCursor, selecao);
    }

    public void restaurar(Instantaneo instantaneo) {
        this.conteudo = instantaneo.conteudo;
        this.posicaoCursor = instantaneo.posicaoCursor;
        this.selecao = instantaneo.selecao;
    }

    public void imprimir() {
        System.out.println("  texto: \"" + conteudo + "\" | cursor: " + posicaoCursor
                           + " | seleção: \"" + selecao + "\"");
    }

    public static final class Instantaneo {
        private final String conteudo;
        private final int posicaoCursor;
        private final String selecao;

        private Instantaneo(String conteudo, int posicaoCursor, String selecao) {
            this.conteudo = conteudo;
            this.posicaoCursor = posicaoCursor;
            this.selecao = selecao;
        }

        public String descricao() {
            return "\"" + resumo() + "\"";
        }

        private String resumo() {
            return conteudo.length() <= 20 ? conteudo : conteudo.substring(0, 20) + "...";
        }
    }
}

class HistoricoEdicao {

    private final Documento documento;
    private final Deque<Documento.Instantaneo> pilhaDesfazer = new ArrayDeque<>();
    private final Deque<Documento.Instantaneo> pilhaRefazer = new ArrayDeque<>();
    private final int limite;

    HistoricoEdicao(Documento documento, int limite) {
        this.documento = documento;
        this.limite = limite;
    }

    public void registrar() {
        pilhaDesfazer.push(documento.salvar());
        pilhaRefazer.clear();

        if (pilhaDesfazer.size() > limite) {
            pilhaDesfazer.removeLast();
        }
    }

    public void desfazer() {
        if (pilhaDesfazer.isEmpty()) {
            System.out.println("  nada a desfazer");
            return;
        }
        pilhaRefazer.push(documento.salvar());
        documento.restaurar(pilhaDesfazer.pop());
        System.out.println("  desfeito");
    }

    public void refazer() {
        if (pilhaRefazer.isEmpty()) {
            System.out.println("  nada a refazer");
            return;
        }
        pilhaDesfazer.push(documento.salvar());
        documento.restaurar(pilhaRefazer.pop());
        System.out.println("  refeito");
    }

    public void listar() {
        System.out.println("  histórico (" + pilhaDesfazer.size() + " estados):");
        for (Documento.Instantaneo instantaneo : pilhaDesfazer) {

            System.out.println("    " + instantaneo.descricao());
        }
    }
}

class EditorComHistorico {

    public static void main(String[] args) {
        Documento documento = new Documento();
        HistoricoEdicao historico = new HistoricoEdicao(documento, 10);

        historico.registrar();
        documento.digitar("Padrões de projeto");
        documento.imprimir();

        historico.registrar();
        documento.digitar(" são soluções recorrentes");
        documento.imprimir();

        historico.registrar();
        documento.selecionar(0, 8);
        documento.apagarSelecao();
        documento.imprimir();

        historico.listar();

        System.out.println("desfazendo duas vezes:");
        historico.desfazer();
        documento.imprimir();
        historico.desfazer();
        documento.imprimir();

        System.out.println("refazendo uma vez:");
        historico.refazer();
        documento.imprimir();
    }
}

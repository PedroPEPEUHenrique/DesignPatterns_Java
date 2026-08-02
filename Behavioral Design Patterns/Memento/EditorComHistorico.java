//Suponha que a sua tarefa seja implementar desfazer/refazer em um editor de textos. O documento
//possui:
//Conteúdo
//Posição do cursor
//Trecho selecionado

//Imagine a solução em que a classe de histórico lê os campos do documento para guardar cópias.
//Para isso o documento precisaria expor getters e setters de tudo, inclusive do que é detalhe
//interno - o encapsulamento morre para que o histórico funcione. E se um campo novo for criado e
//alguém esquecer de incluí-lo no histórico, o desfazer passa a restaurar um estado inconsistente.

//O Memento resolve o problema de capturar e externalizar o estado interno de um objeto, SEM violar
//o seu encapsulamento, de modo que ele possa ser restaurado a esse estado depois.

import java.util.ArrayDeque;
import java.util.Deque;

// Padrão Memento - o Originator
// É quem tem o estado que interessa. Só ele cria mementos e só ele sabe lê-los.
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

    // Padrão Memento - a criação do memento
    // O originator entrega um instantâneo completo. Note que ele não precisa expor NENHUM getter
    // de conteudo, posicaoCursor ou selecao para isso funcionar.
    public Instantaneo salvar() {
        return new Instantaneo(conteudo, posicaoCursor, selecao);
    }

    // Padrão Memento - a restauração
    // Só o originator consegue ler os campos do memento, porque a classe é interna a ele.
    public void restaurar(Instantaneo instantaneo) {
        this.conteudo = instantaneo.conteudo;
        this.posicaoCursor = instantaneo.posicaoCursor;
        this.selecao = instantaneo.selecao;
    }

    public void imprimir() {
        System.out.println("  texto: \"" + conteudo + "\" | cursor: " + posicaoCursor
                           + " | seleção: \"" + selecao + "\"");
    }

    // Padrão Memento - o Memento
    // É IMUTÁVEL (todos os campos final, nenhum setter): um instantâneo que muda depois de tirado
    // não é instantâneo. E é uma classe aninhada com campos privados, de modo que o cuidador
    // consegue guardá-lo e devolvê-lo, mas não consegue inspecioná-lo nem alterá-lo. Essa
    // assimetria de acesso é exatamente o que preserva o encapsulamento.
    public static final class Instantaneo {
        private final String conteudo;
        private final int posicaoCursor;
        private final String selecao;

        private Instantaneo(String conteudo, int posicaoCursor, String selecao) {
            this.conteudo = conteudo;
            this.posicaoCursor = posicaoCursor;
            this.selecao = selecao;
        }

        // Interface ESTREITA: o único acesso público é um rótulo para a tela de histórico.
        // Nada aqui revela a estrutura interna do documento.
        public String descricao() {
            return "\"" + resumo() + "\"";
        }

        private String resumo() {
            return conteudo.length() <= 20 ? conteudo : conteudo.substring(0, 20) + "...";
        }
    }
}

// Padrão Memento - o Caretaker (cuidador)
// Guarda os mementos e decide QUANDO restaurar. Não sabe - e não pode saber - o que há dentro
// deles. Para o cuidador, um memento é uma caixa opaca.
class HistoricoEdicao {

    private final Documento documento;
    private final Deque<Documento.Instantaneo> pilhaDesfazer = new ArrayDeque<>();
    private final Deque<Documento.Instantaneo> pilhaRefazer = new ArrayDeque<>();
    private final int limite;

    HistoricoEdicao(Documento documento, int limite) {
        this.documento = documento;
        this.limite = limite;
    }

    // Deve ser chamado ANTES de cada alteração: o que se guarda é o estado que se quer voltar.
    public void registrar() {
        pilhaDesfazer.push(documento.salvar());
        pilhaRefazer.clear();   // um caminho novo invalida o "refazer" anterior

        // Mementos ocupam memória. Um editor real limita a profundidade do histórico ou grava
        // apenas o DELTA entre estados, em vez do estado completo.
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
            // O cuidador só consegue chamar a interface estreita. Tentar acessar
            // instantaneo.conteudo aqui nem compila - o campo é privado da classe aninhada.
            System.out.println("    " + instantaneo.descricao());
        }
    }
}

// Classe Cliente
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

//Memento x Command: os dois viabilizam desfazer, por caminhos opostos. O Memento guarda o ESTADO
//e volta a ele; o Command guarda a OPERAÇÃO e a reverte. Guardar estado é mais simples e sempre
//correto, mas custa memória; reverter operação é barato, porém exige que toda operação tenha
//inversa - o que nem sempre é verdade.
//
//Onde isso aparece: pontos de salvamento (savepoints) de transações de banco, snapshots de
//máquinas virtuais e o estado de sessão serializado de aplicações web.

//Suponha que a sua tarefa seja representar em memória cada caractere de um documento de texto,
//para que o editor possa renderizá-lo. Cada caractere na tela precisa de:
//O símbolo em si
//A fonte, o tamanho e a cor
//A posição (linha, coluna)

//Imagine um documento de 500 mil caracteres. Se cada caractere for um objeto com todos esses
//campos, são 500 mil cópias da mesma String "Times New Roman" e do mesmo tamanho 12. O consumo de
//memória é dominado por informação REPETIDA.

//O Flyweight resolve o problema de suportar uma quantidade muito grande de objetos de granularidade
//fina, COMPARTILHANDO as partes que se repetem entre eles.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Padrão Flyweight - o estado INTRÍNSECO
// É a parte que não depende do contexto e por isso pode ser compartilhada: o símbolo e a
// formatação. A classe é IMUTÁVEL - isso não é detalhe, é requisito. Um flyweight mutável
// compartilhado por milhares de posições seria um bug garantido.
final class CaractereFlyweight {
    private final char simbolo;
    private final String fonte;
    private final int tamanho;
    private final String cor;

    // Construtor de visibilidade restrita: quem cria é a fábrica, não o cliente.
    private CaractereFlyweight(char simbolo, String fonte, int tamanho, String cor) {
        this.simbolo = simbolo;
        this.fonte = fonte;
        this.tamanho = tamanho;
        this.cor = cor;
    }

    // A operação recebe o estado EXTRÍNSECO como parâmetro. Esse é o outro lado do padrão:
    // o que não pode ser compartilhado é passado na chamada, não guardado no objeto.
    void desenhar(int linha, int coluna) {
        System.out.println("  '" + simbolo + "' em (" + linha + "," + coluna + ") "
                           + fonte + " " + tamanho + " " + cor);
    }

    char getSimbolo() {
        return simbolo;
    }

    // Padrão Flyweight - a FlyweightFactory
    // Garante o compartilhamento: dada a mesma combinação intrínseca, devolve sempre a MESMA
    // instância. Sem essa fábrica não existe padrão, só uma classe imutável qualquer.
    static class Fabrica {
        private final Map<String, CaractereFlyweight> cache = new HashMap<>();

        CaractereFlyweight obter(char simbolo, String fonte, int tamanho, String cor) {
            String chave = simbolo + "|" + fonte + "|" + tamanho + "|" + cor;

            // computeIfAbsent: cria só na primeira vez, devolve a instância existente nas demais.
            return cache.computeIfAbsent(chave,
                    k -> new CaractereFlyweight(simbolo, fonte, tamanho, cor));
        }

        int quantidadeDeInstancias() {
            return cache.size();
        }
    }
}

// O estado EXTRÍNSECO, mantido pelo cliente. É leve: dois inteiros e uma referência compartilhada.
// Multiplique por 500 mil e compare com 500 mil cópias de fonte, tamanho e cor.
class PosicaoCaractere {
    private final CaractereFlyweight glifo;
    private final int linha;
    private final int coluna;

    PosicaoCaractere(CaractereFlyweight glifo, int linha, int coluna) {
        this.glifo = glifo;
        this.linha = linha;
        this.coluna = coluna;
    }

    void desenhar() {
        glifo.desenhar(linha, coluna);
    }

    CaractereFlyweight getGlifo() {
        return glifo;
    }
}

// Classe Cliente
class EditorTexto {

    private final CaractereFlyweight.Fabrica fabrica = new CaractereFlyweight.Fabrica();
    private final List<PosicaoCaractere> documento = new ArrayList<>();

    public void digitar(String texto, String fonte, int tamanho, String cor, int linha) {
        for (int coluna = 0; coluna < texto.length(); coluna++) {
            // Sempre pela fábrica. Um "new" direto aqui destruiria o compartilhamento.
            CaractereFlyweight glifo = fabrica.obter(texto.charAt(coluna), fonte, tamanho, cor);
            documento.add(new PosicaoCaractere(glifo, linha, coluna));
        }
    }

    public void renderizar() {
        for (PosicaoCaractere posicao : documento) {
            posicao.desenhar();
        }
    }

    public void estatisticas() {
        System.out.println("caracteres no documento: " + documento.size());
        System.out.println("objetos flyweight criados: " + fabrica.quantidadeDeInstancias());

        // Prova do compartilhamento: duas posições com o mesmo símbolo e formatação apontam para
        // o MESMO objeto em memória.
        if (documento.size() > 3) {
            System.out.println("primeiro 'a' == segundo 'a'? " + comparaOcorrencias('a'));
        }
    }

    private boolean comparaOcorrencias(char alvo) {
        CaractereFlyweight primeiro = null;
        for (PosicaoCaractere posicao : documento) {
            if (posicao.getGlifo().getSimbolo() == alvo) {
                if (primeiro == null) {
                    primeiro = posicao.getGlifo();
                } else {
                    return primeiro == posicao.getGlifo();   // == compara REFERÊNCIA, de propósito
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        EditorTexto editor = new EditorTexto();

        editor.digitar("banana", "Times", 12, "preto", 0);
        editor.digitar("cabana", "Times", 12, "preto", 1);
        editor.digitar("aba", "Arial", 12, "vermelho", 2);   // formatação diferente = flyweight novo

        editor.renderizar();
        editor.estatisticas();
    }
}

//Onde isso aparece na prática:
//Integer.valueOf() mantém um cache de -128 a 127 - é por isso que Integer.valueOf(100) ==
//Integer.valueOf(100) dá true e com 1000 dá false. É um Flyweight na biblioteca padrão.
//String.intern() e o pool de literais de String seguem a mesma ideia.
//
//Quando NÃO usar: o padrão troca memória por indireção e por complexidade. Se a quantidade de
//objetos não é grande o suficiente para pesar, o cache só atrapalha a leitura do código. E o
//estado intrínseco precisa mesmo se repetir muito - se cada objeto tem formatação única, o mapa
//fica do mesmo tamanho da lista e não se ganha nada.

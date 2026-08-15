//Representar em memória cada caractere de um documento. Num texto de 500 mil caracteres, cada
//objeto guardaria sua própria cópia de fonte, tamanho e cor - o consumo é dominado por informação
//REPETIDA.
//O Flyweight suporta uma quantidade grande de objetos de granularidade fina COMPARTILHANDO as
//partes que se repetem entre eles.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CaractereFlyweight {
    private final char simbolo;
    private final String fonte;
    private final int tamanho;
    private final String cor;

    private CaractereFlyweight(char simbolo, String fonte, int tamanho, String cor) {
        this.simbolo = simbolo;
        this.fonte = fonte;
        this.tamanho = tamanho;
        this.cor = cor;
    }

    void desenhar(int linha, int coluna) {
        System.out.println("  '" + simbolo + "' em (" + linha + "," + coluna + ") "
                           + fonte + " " + tamanho + " " + cor);
    }

    char getSimbolo() {
        return simbolo;
    }

    static class Fabrica {
        private final Map<String, CaractereFlyweight> cache = new HashMap<>();

        CaractereFlyweight obter(char simbolo, String fonte, int tamanho, String cor) {
            String chave = simbolo + "|" + fonte + "|" + tamanho + "|" + cor;

            return cache.computeIfAbsent(chave,
                    k -> new CaractereFlyweight(simbolo, fonte, tamanho, cor));
        }

        int quantidadeDeInstancias() {
            return cache.size();
        }
    }
}

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

class EditorTexto {

    private final CaractereFlyweight.Fabrica fabrica = new CaractereFlyweight.Fabrica();
    private final List<PosicaoCaractere> documento = new ArrayList<>();

    public void digitar(String texto, String fonte, int tamanho, String cor, int linha) {
        for (int coluna = 0; coluna < texto.length(); coluna++) {

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
                    return primeiro == posicao.getGlifo();
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        EditorTexto editor = new EditorTexto();

        editor.digitar("banana", "Times", 12, "preto", 0);
        editor.digitar("cabana", "Times", 12, "preto", 1);
        editor.digitar("aba", "Arial", 12, "vermelho", 2);

        editor.renderizar();
        editor.estatisticas();
    }
}

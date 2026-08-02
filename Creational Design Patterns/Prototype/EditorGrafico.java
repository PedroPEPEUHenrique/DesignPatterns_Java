import java.util.ArrayList;
import java.util.List;

interface Forma {
    Forma copiar();

    void mover(int dx, int dy);

    void desenhar();
}

class Estilo {
    private String corPreenchimento;
    private int espessuraBorda;

    Estilo(String corPreenchimento, int espessuraBorda) {
        this.corPreenchimento = corPreenchimento;
        this.espessuraBorda = espessuraBorda;
    }

    Estilo copiar() {
        return new Estilo(corPreenchimento, espessuraBorda);
    }

    String getCorPreenchimento() {
        return corPreenchimento;
    }

    void setCorPreenchimento(String corPreenchimento) {
        this.corPreenchimento = corPreenchimento;
    }

    int getEspessuraBorda() {
        return espessuraBorda;
    }
}

class Circulo implements Forma {
    private int x;
    private int y;
    private int raio;
    private Estilo estilo;

    Circulo(int x, int y, int raio, Estilo estilo) {
        this.x = x;
        this.y = y;
        this.raio = raio;
        this.estilo = estilo;
    }

    @Override
    public Forma copiar() {
        return new Circulo(x, y, raio, estilo.copiar());
    }

    @Override
    public void mover(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public void desenhar() {
        System.out.println("círculo em (" + x + "," + y + ") raio " + raio
                           + " cor " + estilo.getCorPreenchimento());
    }

    public Estilo getEstilo() {
        return estilo;
    }
}

class Retangulo implements Forma {
    private int x;
    private int y;
    private int largura;
    private int altura;
    private Estilo estilo;

    Retangulo(int x, int y, int largura, int altura, Estilo estilo) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.estilo = estilo;
    }

    @Override
    public Forma copiar() {
        return new Retangulo(x, y, largura, altura, estilo.copiar());
    }

    @Override
    public void mover(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public void desenhar() {
        System.out.println("retângulo em (" + x + "," + y + ") " + largura + "x" + altura
                           + " cor " + estilo.getCorPreenchimento());
    }
}

class Grupo implements Forma {
    private int x;
    private int y;
    private final List<Forma> filhos = new ArrayList<>();

    Grupo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void adicionar(Forma forma) {
        filhos.add(forma);
    }

    @Override
    public Forma copiar() {
        Grupo copia = new Grupo(x, y);
        for (Forma filho : filhos) {
            copia.adicionar(filho.copiar());
        }
        return copia;
    }

    @Override
    public void mover(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        for (Forma filho : filhos) {
            filho.mover(dx, dy);
        }
    }

    @Override
    public void desenhar() {
        System.out.println("grupo em (" + x + "," + y + ") com " + filhos.size() + " formas:");
        for (Forma filho : filhos) {
            System.out.print("  ");
            filho.desenhar();
        }
    }
}

class EditorGrafico {
    private final List<Forma> formas = new ArrayList<>();

    public void adicionar(Forma forma) {
        formas.add(forma);
    }

    public void duplicar(int indice, int deslocamento) {
        Forma copia = formas.get(indice).copiar();
        copia.mover(deslocamento, deslocamento);
        formas.add(copia);
    }

    public void desenharTudo() {
        for (Forma forma : formas) {
            forma.desenhar();
        }
    }

    public Forma get(int indice) {
        return formas.get(indice);
    }

    public static void main(String[] args) {
        EditorGrafico editor = new EditorGrafico();

        Circulo original = new Circulo(10, 10, 5, new Estilo("azul", 1));
        editor.adicionar(original);

        Grupo grupo = new Grupo(0, 0);
        grupo.adicionar(new Retangulo(0, 0, 100, 50, new Estilo("verde", 2)));
        grupo.adicionar(new Circulo(50, 25, 8, new Estilo("verde", 2)));
        editor.adicionar(grupo);

        editor.duplicar(0, 30);
        editor.duplicar(1, 200);

        editor.desenharTudo();

        Circulo copia = (Circulo) editor.get(2);
        copia.getEstilo().setCorPreenchimento("vermelho");

        System.out.println("--- após pintar apenas a cópia ---");
        original.desenhar();
        copia.desenhar();
    }
}

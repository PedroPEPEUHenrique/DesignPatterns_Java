//Suponha que a sua tarefa seja implementar a função "duplicar" de um editor gráfico. Uma forma
//possui:
//Posição (x, y)
//Estilo (cor de preenchimento, espessura da borda)
//Atributos próprios de cada tipo (raio, largura/altura, pontos, ...)

//Imagine que o editor trabalha com a abstração Forma e nem sabe quais tipos concretos existem -
//eles podem até vir de plugins. Duplicar com "new" exigiria um if por tipo concreto dentro do
//editor, e cada tipo novo obrigaria a alterá-lo.

//O Prototype resolve o problema de criar novos objetos COPIANDO uma instância existente, deixando
//que cada objeto saiba se clonar. O cliente pede a cópia à própria abstração.

import java.util.ArrayList;
import java.util.List;

// Padrão Prototype - a interface do protótipo
// Não uso a Cloneable/Object.clone() da linguagem de propósito: aquele mecanismo devolve Object,
// depende de um método protegido e tem semântica confusa de cópia rasa. Uma operação de cópia
// declarada explicitamente é mais clara e devolve o tipo certo.
interface Forma {
    Forma copiar();

    void mover(int dx, int dy);

    void desenhar();
}

// Estilo é um objeto MUTÁVEL compartilhado. É aqui que mora a diferença entre cópia rasa e
// profunda - o ponto que mais gera erro em provas e em produção.
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

    // CÓPIA PROFUNDA: o estilo também é copiado. Sem o estilo.copiar(), mudar a cor da cópia
    // mudaria a cor do original, porque os dois apontariam para o mesmo Estilo.
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

// Composto: um grupo de formas também é uma forma, e copiá-lo exige copiar cada filho.
// Aqui o ganho do Prototype fica evidente - o grupo não sabe que tipos guarda.
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
            copia.adicionar(filho.copiar());   // recursão: cada filho sabe se copiar
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

// Classe Cliente
class EditorGrafico {
    private final List<Forma> formas = new ArrayList<>();

    public void adicionar(Forma forma) {
        formas.add(forma);
    }

    // Repare: nenhum "new", nenhum if por tipo, nenhuma menção a Circulo/Retangulo/Grupo.
    // Um tipo novo de forma passa a ser duplicável sem tocar nesta classe.
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

        // Prova da cópia profunda: mudar a cor da cópia não afeta o original.
        Circulo copia = (Circulo) editor.get(2);
        copia.getEstilo().setCorPreenchimento("vermelho");

        System.out.println("--- após pintar apenas a cópia ---");
        original.desenhar();
        copia.desenhar();
    }
}

//POO - COMPOSIÇÃO, AGREGAÇÃO E DELEGAÇÃO
//Composição é "TEM UM": o objeto guarda outro dentro de si e DELEGA a ele parte do trabalho.
//Herança é "É UM" e amarra: a subclasse herda tudo, inclusive o que não faz sentido para ela, e
//passa a depender de detalhes internos da superclasse - se a superclasse mudar por dentro, a
//subclasse quebra sem ter sido tocada.
//Pilha estendendo ArrayList é o exemplo clássico do erro: a pilha ganha de graça add(indice, item),
//remove(indice) e get(indice), e com isso qualquer cliente fura a disciplina LIFO que ela deveria
//garantir. Com composição, a lista fica escondida e só empilhar e desempilhar aparecem.
//A diferença entre COMPOSIÇÃO e AGREGAÇÃO é o ciclo de vida: o motor é criado e destruído com o
//carro (composição); o aluno já existia antes da turma e continua existindo depois dela
//(agregação).
//Regra prática: prefira composição. Herde apenas quando houver substituição verdadeira e a
//superclasse tiver sido projetada para ser estendida.

import java.util.ArrayList;
import java.util.List;

class PilhaHerdada extends ArrayList<String> {

    private static final long serialVersionUID = 1L;

    void empilhar(String item) {
        add(item);
    }

    String desempilhar() {
        if (isEmpty()) {
            throw new IllegalStateException("pilha vazia");
        }
        return remove(size() - 1);
    }
}

class Pilha {

    private final List<String> itens = new ArrayList<>();

    void empilhar(String item) {
        itens.add(item);
    }

    String desempilhar() {
        if (estaVazia()) {
            throw new IllegalStateException("pilha vazia");
        }
        return itens.remove(itens.size() - 1);
    }

    String espiar() {
        if (estaVazia()) {
            throw new IllegalStateException("pilha vazia");
        }
        return itens.get(itens.size() - 1);
    }

    boolean estaVazia() {
        return itens.isEmpty();
    }

    int tamanho() {
        return itens.size();
    }

    @Override
    public String toString() {
        return itens.toString();
    }
}

class Motor {

    private final int cilindradas;
    private boolean ligado;

    Motor(int cilindradas) {
        this.cilindradas = cilindradas;
    }

    void ligar() {
        ligado = true;
    }

    void desligar() {
        ligado = false;
    }

    boolean estaLigado() {
        return ligado;
    }

    int getCilindradas() {
        return cilindradas;
    }
}

class Carro {

    private final String placa;
    private final Motor motor;

    Carro(String placa, int cilindradas) {
        this.placa = placa;
        this.motor = new Motor(cilindradas);
    }

    void darPartida() {
        motor.ligar();
        System.out.println(placa + " ligado, motor " + motor.getCilindradas() + " cc");
    }

    void estacionar() {
        motor.desligar();
        System.out.println(placa + " desligado");
    }

    boolean estaEmFuncionamento() {
        return motor.estaLigado();
    }
}

class Aluno {

    private final String nome;

    Aluno(String nome) {
        this.nome = nome;
    }

    String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}

class Turma {

    private final String disciplina;
    private final List<Aluno> matriculados = new ArrayList<>();

    Turma(String disciplina) {
        this.disciplina = disciplina;
    }

    void matricular(Aluno aluno) {
        matriculados.add(aluno);
    }

    int quantidadeDeMatriculados() {
        return matriculados.size();
    }

    @Override
    public String toString() {
        return disciplina + " " + matriculados;
    }
}

class ComposicaoEDelegacao {

    public static void main(String[] args) {
        PilhaHerdada herdada = new PilhaHerdada();
        herdada.empilhar("primeiro");
        herdada.empilhar("segundo");
        herdada.add(0, "furou a fila");
        System.out.println("pilha por herança: " + herdada);
        System.out.println("desempilhou: " + herdada.desempilhar()
                           + ", e o get(0) exposto devolve: " + herdada.get(0));

        Pilha pilha = new Pilha();
        pilha.empilhar("primeiro");
        pilha.empilhar("segundo");
        System.out.println("pilha por composição: " + pilha + ", topo " + pilha.espiar());
        System.out.println("desempilhou: " + pilha.desempilhar()
                           + ", restam " + pilha.tamanho());

        Carro carro = new Carro("ABC1D23", 1600);
        carro.darPartida();
        System.out.println("em funcionamento? " + carro.estaEmFuncionamento());
        carro.estacionar();

        Aluno ana = new Aluno("Ana");
        Aluno bruno = new Aluno("Bruno");
        Turma poo = new Turma("POO");
        poo.matricular(ana);
        poo.matricular(bruno);
        System.out.println(poo + " com " + poo.quantidadeDeMatriculados() + " matriculados");
        System.out.println("a turma acaba e " + ana.getNome() + " continua existindo");
    }
}

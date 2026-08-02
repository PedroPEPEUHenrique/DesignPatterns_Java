//Suponha que a sua tarefa seja implementar o controle remoto de uma automação residencial. O
//controle tem botões configuráveis e cada botão pode acionar qualquer aparelho:
//Ligar/desligar a luz
//Abrir/fechar a persiana
//Ajustar o volume do som

//Imagine o controle com um switch sobre o botão pressionado, chamando o método certo de cada
//aparelho. O controle passaria a depender de TODOS os aparelhos, e permitir que o usuário
//reconfigure um botão em tempo de execução ficaria impossível. Some o requisito de DESFAZER a
//última ação e o switch vira inviável.

//O Command resolve o problema de encapsular uma requisição como um OBJETO, permitindo
//parametrizar clientes com diferentes requisições, enfileirá-las, registrá-las em log e - por
//serem objetos - desfazê-las.

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Padrão Command - a interface Command
// Uma requisição vira um objeto com uma operação sem parâmetros. Tudo de que ela precisa foi
// guardado no seu estado quando ela foi criada.
interface Comando {
    void executar();

    void desfazer();

    String descricao();
}

// RECEIVERS - os aparelhos. Repare que eles não sabem que existem comandos: continuam sendo
// classes de domínio comuns. O padrão não os contamina.

class Luz {
    private final String comodo;
    private boolean ligada;

    Luz(String comodo) {
        this.comodo = comodo;
    }

    void ligar() {
        ligada = true;
        System.out.println("  luz do " + comodo + " ligada");
    }

    void desligar() {
        ligada = false;
        System.out.println("  luz do " + comodo + " desligada");
    }

    boolean isLigada() {
        return ligada;
    }
}

class Persiana {
    private int aberturaEmPorcento;

    void ajustar(int aberturaEmPorcento) {
        this.aberturaEmPorcento = aberturaEmPorcento;
        System.out.println("  persiana em " + aberturaEmPorcento + "%");
    }

    int getAberturaEmPorcento() {
        return aberturaEmPorcento;
    }
}

class Som {
    private int volume;

    void setVolume(int volume) {
        this.volume = volume;
        System.out.println("  volume em " + volume);
    }

    int getVolume() {
        return volume;
    }
}

// Padrão Command - ConcreteCommand
// Guarda o RECEIVER e os argumentos. Para desfazer, guarda também o estado anterior - é o
// comando que sabe reverter a si mesmo, e não o aparelho.
class ComandoLigarLuz implements Comando {
    private final Luz luz;
    private boolean estadoAnterior;

    ComandoLigarLuz(Luz luz) {
        this.luz = luz;
    }

    @Override
    public void executar() {
        estadoAnterior = luz.isLigada();
        luz.ligar();
    }

    @Override
    public void desfazer() {
        if (estadoAnterior) {
            luz.ligar();
        } else {
            luz.desligar();
        }
    }

    @Override
    public String descricao() {
        return "ligar luz";
    }
}

class ComandoDesligarLuz implements Comando {
    private final Luz luz;
    private boolean estadoAnterior;

    ComandoDesligarLuz(Luz luz) {
        this.luz = luz;
    }

    @Override
    public void executar() {
        estadoAnterior = luz.isLigada();
        luz.desligar();
    }

    @Override
    public void desfazer() {
        if (estadoAnterior) {
            luz.ligar();
        } else {
            luz.desligar();
        }
    }

    @Override
    public String descricao() {
        return "desligar luz";
    }
}

class ComandoAjustarPersiana implements Comando {
    private final Persiana persiana;
    private final int novaAbertura;
    private int aberturaAnterior;

    ComandoAjustarPersiana(Persiana persiana, int novaAbertura) {
        this.persiana = persiana;
        this.novaAbertura = novaAbertura;
    }

    @Override
    public void executar() {
        aberturaAnterior = persiana.getAberturaEmPorcento();
        persiana.ajustar(novaAbertura);
    }

    @Override
    public void desfazer() {
        persiana.ajustar(aberturaAnterior);
    }

    @Override
    public String descricao() {
        return "persiana para " + novaAbertura + "%";
    }
}

class ComandoAjustarVolume implements Comando {
    private final Som som;
    private final int novoVolume;
    private int volumeAnterior;

    ComandoAjustarVolume(Som som, int novoVolume) {
        this.som = som;
        this.novoVolume = novoVolume;
    }

    @Override
    public void executar() {
        volumeAnterior = som.getVolume();
        som.setVolume(novoVolume);
    }

    @Override
    public void desfazer() {
        som.setVolume(volumeAnterior);
    }

    @Override
    public String descricao() {
        return "volume " + novoVolume;
    }
}

// MacroComando - um comando composto por outros. Como ele também implementa Comando, o invoker
// não distingue um comando simples de uma cena inteira: é um Composite de comandos.
class Cena implements Comando {
    private final String nome;
    private final List<Comando> passos = new ArrayList<>();

    Cena(String nome) {
        this.nome = nome;
    }

    Cena com(Comando comando) {
        passos.add(comando);
        return this;
    }

    @Override
    public void executar() {
        System.out.println("  [cena " + nome + "]");
        for (Comando passo : passos) {
            passo.executar();
        }
    }

    @Override
    public void desfazer() {
        // Desfazer é na ORDEM INVERSA. Esquecer isso é o erro mais comum em macro comandos.
        for (int i = passos.size() - 1; i >= 0; i--) {
            passos.get(i).desfazer();
        }
    }

    @Override
    public String descricao() {
        return "cena " + nome;
    }
}

// Comando nulo (Null Object): evita o if (comando != null) espalhado pelo invoker.
class ComandoVazio implements Comando {

    @Override
    public void executar() {
        System.out.println("  botão sem função");
    }

    @Override
    public void desfazer() {
    }

    @Override
    public String descricao() {
        return "vazio";
    }
}

// Padrão Command - o Invoker
// Sabe DISPARAR comandos e guardar o histórico. Não sabe o que cada comando faz, nem quais
// aparelhos existem. Um aparelho novo não muda uma linha desta classe.
class ControleRemoto {

    private final Comando[] botoes = new Comando[6];
    private final Deque<Comando> historico = new ArrayDeque<>();

    public ControleRemoto() {
        for (int i = 0; i < botoes.length; i++) {
            botoes[i] = new ComandoVazio();
        }
    }

    // É aqui que o botão é PARAMETRIZADO com uma requisição - em tempo de execução.
    public void configurar(int botao, Comando comando) {
        botoes[botao] = comando;
    }

    public void pressionar(int botao) {
        Comando comando = botoes[botao];
        System.out.println("botão " + botao + " -> " + comando.descricao());
        comando.executar();
        historico.push(comando);
    }

    // O histórico só é possível porque a requisição é um objeto que sobrevive à chamada.
    public void desfazerUltimo() {
        if (historico.isEmpty()) {
            System.out.println("nada a desfazer");
            return;
        }
        Comando comando = historico.pop();
        System.out.println("desfazendo -> " + comando.descricao());
        comando.desfazer();
    }

    public static void main(String[] args) {
        Luz luzSala = new Luz("sala");
        Persiana persiana = new Persiana();
        Som som = new Som();

        ControleRemoto controle = new ControleRemoto();
        controle.configurar(0, new ComandoLigarLuz(luzSala));
        controle.configurar(1, new ComandoDesligarLuz(luzSala));
        controle.configurar(2, new ComandoAjustarPersiana(persiana, 70));
        controle.configurar(3, new ComandoAjustarVolume(som, 15));
        controle.configurar(4, new Cena("cinema")
                                   .com(new ComandoDesligarLuz(luzSala))
                                   .com(new ComandoAjustarPersiana(persiana, 0))
                                   .com(new ComandoAjustarVolume(som, 30)));

        controle.pressionar(0);
        controle.pressionar(2);
        controle.pressionar(3);
        controle.pressionar(4);
        controle.pressionar(5);   // botão não configurado

        System.out.println("--- desfazendo ---");
        controle.desfazerUltimo();   // botão vazio
        controle.desfazerUltimo();   // a cena inteira, na ordem inversa
        controle.desfazerUltimo();
    }
}

//O que o Command habilita, e que um método direto não habilita:
//Desfazer/refazer, porque a operação virou um objeto com estado.
//Fila e agendamento: comandos podem ser enfileirados, executados em outra thread ou mais tarde.
//Log e replay: gravando os comandos, o sistema pode reconstruir seu estado reexecutando-os -
//  é a base de event sourcing e do log de transações de um banco de dados.
//Em Java, Runnable é a interface Command da biblioteca padrão, e é por isso que um ExecutorService
//aceita qualquer tarefa sem saber o que ela faz.

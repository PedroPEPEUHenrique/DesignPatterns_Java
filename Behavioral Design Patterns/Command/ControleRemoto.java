//Implementar um controle remoto com botões configuráveis, capaz de acionar qualquer aparelho e de
//DESFAZER a última ação. Com um switch sobre o botão, o controle dependeria de todos os aparelhos
//e reconfigurar um botão em tempo de execução seria impossível.
//O Command encapsula uma requisição como um OBJETO, o que permite parametrizar clientes com
//requisições, enfileirá-las, registrá-las em log e desfazê-las.

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Command
interface Comando {
    void executar();

    void desfazer();

    String descricao();
}

// Receivers: continuam classes de domínio comuns, não sabem que existem comandos.

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

// ConcreteCommand
// Guarda o receiver, os argumentos e o estado anterior: é o comando que sabe reverter a si mesmo.
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

// MacroComando: também implementa Comando, então o invoker não distingue um comando de uma cena.
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
        // Ordem INVERSA: esquecer isso é o erro mais comum em macro comandos.
        for (int i = passos.size() - 1; i >= 0; i--) {
            passos.get(i).desfazer();
        }
    }

    @Override
    public String descricao() {
        return "cena " + nome;
    }
}

// Null Object: evita o if (comando != null) espalhado pelo invoker.
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

// Invoker: sabe disparar comandos e guardar o histórico, sem saber o que cada um faz.
class ControleRemoto {

    private final Comando[] botoes = new Comando[6];
    private final Deque<Comando> historico = new ArrayDeque<>();

    public ControleRemoto() {
        for (int i = 0; i < botoes.length; i++) {
            botoes[i] = new ComandoVazio();
        }
    }

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
        controle.pressionar(5);

        System.out.println("--- desfazendo ---");
        controle.desfazerUltimo();
        controle.desfazerUltimo();
        controle.desfazerUltimo();
    }
}

//O que o Command habilita e um método direto não: desfazer/refazer, fila e agendamento, log e
//replay (base de event sourcing e do log de transações de um banco).
//Em Java, Runnable é a interface Command da biblioteca padrão - por isso um ExecutorService
//aceita qualquer tarefa sem saber o que ela faz.

//Suponha que a sua tarefa seja acrescentar recursos ao envio de notificações do sistema. O envio
//básico já existe e a abstração é Notificador, com a operação:
//enviar(destinatario, mensagem)

//Imagine que agora se pede: registrar em log todo envio, limitar a taxa de envios, tentar de novo
//em caso de falha e criptografar a mensagem. Por herança seriam NotificadorComLog,
//NotificadorComLogERetentativa, NotificadorComLogRetentativaELimite... uma classe por COMBINAÇÃO,
//e a combinação é escolhida em tempo de execução, o que a herança nem permite.

//O Decorator resolve o problema de acrescentar responsabilidades a um objeto DINAMICAMENTE,
//mantendo a mesma interface - o que o torna uma alternativa flexível à herança para estender
//comportamento.

import java.util.ArrayList;
import java.util.List;

// Padrão Decorator - o Component
interface Notificador {
    void enviar(String destinatario, String mensagem);
}

// Padrão Decorator - o ConcreteComponent
// O objeto "de verdade", que faz o trabalho real. É o único que não delega para ninguém.
class NotificadorEmail implements Notificador {

    @Override
    public void enviar(String destinatario, String mensagem) {
        System.out.println("[email] para " + destinatario + ": " + mensagem);
    }
}

class NotificadorSms implements Notificador {

    @Override
    public void enviar(String destinatario, String mensagem) {
        System.out.println("[sms] para " + destinatario + ": " + mensagem);
    }
}

// Padrão Decorator - o Decorator abstrato
// Ele IMPLEMENTA a interface (para poder substituir o componente) e ao mesmo tempo CONTÉM um
// componente (para poder delegar). Essa dupla natureza é o coração do padrão e é o que permite
// empilhar decoradores indefinidamente.
abstract class NotificadorDecorator implements Notificador {

    protected final Notificador delegado;

    protected NotificadorDecorator(Notificador delegado) {
        this.delegado = delegado;
    }

    // Comportamento padrão: repassar sem alterar nada. As subclasses sobrescrevem e chamam super.
    @Override
    public void enviar(String destinatario, String mensagem) {
        delegado.enviar(destinatario, mensagem);
    }
}

// Decorador 1 - acrescenta comportamento ANTES e DEPOIS da chamada real.
class NotificadorComLog extends NotificadorDecorator {

    NotificadorComLog(Notificador delegado) {
        super(delegado);
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        long inicio = System.nanoTime();
        System.out.println("  <log> iniciando envio para " + destinatario);

        super.enviar(destinatario, mensagem);

        System.out.println("  <log> concluído em " + (System.nanoTime() - inicio) / 1000 + "us");
    }
}

// Decorador 2 - TRANSFORMA o argumento antes de repassar.
class NotificadorCriptografado extends NotificadorDecorator {

    NotificadorCriptografado(Notificador delegado) {
        super(delegado);
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        super.enviar(destinatario, cifrar(mensagem));
    }

    // Cifra de César, só para o exemplo. Não é criptografia de verdade.
    private String cifrar(String texto) {
        StringBuilder cifrado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            cifrado.append(Character.isLetter(c) ? (char) (c + 1) : c);
        }
        return cifrado.toString();
    }
}

// Decorador 3 - controla SE a chamada acontece.
class NotificadorComLimite extends NotificadorDecorator {
    private final int limite;
    private int enviados;

    NotificadorComLimite(Notificador delegado, int limite) {
        super(delegado);
        this.limite = limite;
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        if (enviados >= limite) {
            System.out.println("  <limite> envio bloqueado, cota de " + limite + " esgotada");
            return;
        }
        enviados++;
        super.enviar(destinatario, mensagem);
    }
}

// Decorador 4 - controla QUANTAS VEZES a chamada acontece.
class NotificadorComRetentativa extends NotificadorDecorator {
    private final int tentativas;

    NotificadorComRetentativa(Notificador delegado, int tentativas) {
        super(delegado);
        this.tentativas = tentativas;
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                super.enviar(destinatario, mensagem);
                return;
            } catch (RuntimeException e) {
                System.out.println("  <retry> tentativa " + tentativa + " falhou: " + e.getMessage());
                if (tentativa == tentativas) {
                    throw e;
                }
            }
        }
    }
}

// Componente instável, só para exercitar a retentativa.
class NotificadorInstavel implements Notificador {
    private int chamadas;

    @Override
    public void enviar(String destinatario, String mensagem) {
        chamadas++;
        if (chamadas < 3) {
            throw new RuntimeException("conexão recusada");
        }
        System.out.println("[push] para " + destinatario + ": " + mensagem);
    }
}

// Classe Cliente
class ServicoNotificacao {
    private final Notificador notificador;

    // O cliente recebe um Notificador e não distingue um componente puro de uma pilha de 4
    // decoradores. Para ele, todos têm exatamente a mesma interface.
    public ServicoNotificacao(Notificador notificador) {
        this.notificador = notificador;
    }

    public void avisar(String destinatario, String mensagem) {
        notificador.enviar(destinatario, mensagem);
    }

    public static void main(String[] args) {
        // A montagem é lida DE FORA PARA DENTRO: log envolve limite, que envolve criptografia,
        // que envolve o email. A ordem importa - inverter log e criptografia muda o que é logado.
        Notificador email = new NotificadorComLog(
                                new NotificadorComLimite(
                                    new NotificadorCriptografado(
                                        new NotificadorEmail()), 2));

        ServicoNotificacao servico = new ServicoNotificacao(email);
        servico.avisar("ana@exemplo.com", "pedido aprovado");
        servico.avisar("bruno@exemplo.com", "pedido aprovado");
        servico.avisar("carla@exemplo.com", "pedido aprovado");   // barrado pela cota

        System.out.println("---");

        // Outra combinação, escolhida em tempo de execução, sem nenhuma classe nova.
        new ServicoNotificacao(new NotificadorComRetentativa(new NotificadorInstavel(), 5))
                .avisar("diego@exemplo.com", "código de acesso");

        System.out.println("---");

        // O SMS aproveita os mesmos decoradores: eles não conhecem nenhum componente concreto.
        new ServicoNotificacao(new NotificadorComLog(new NotificadorSms()))
                .avisar("+5511999990000", "seu pedido saiu para entrega");

        // As classes de java.io são o exemplo canônico de Decorator na biblioteca padrão:
        // new BufferedReader(new InputStreamReader(new FileInputStream(arquivo)))
    }
}

//Decorator x Proxy: os dois embrulham um objeto mantendo a interface. O Decorator ACRESCENTA
//comportamento e é montado pelo cliente, que escolhe a pilha; o Proxy CONTROLA o acesso e
//normalmente gerencia o ciclo de vida do objeto real, escondendo-o do cliente.
//Decorator x Chain of Responsibility: a estrutura de delegação é parecida, mas no Decorator todos
//os elos participam do resultado, enquanto na corrente um elo trata e interrompe a passagem.

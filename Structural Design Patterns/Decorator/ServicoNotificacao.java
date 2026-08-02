//Acrescentar log, limite de envio, retentativa e criptografia ao envio de notificações. Por
//herança seria uma classe por COMBINAÇÃO, e a combinação é escolhida em tempo de execução - o que
//a herança nem permite.
//O Decorator acrescenta responsabilidades a um objeto DINAMICAMENTE, mantendo a mesma interface.

// Component
interface Notificador {
    void enviar(String destinatario, String mensagem);
}

// ConcreteComponent: o único que faz o trabalho real e não delega para ninguém.
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

// Decorator abstrato
// IMPLEMENTA a interface (para poder substituir o componente) e CONTÉM um componente (para poder
// delegar). Essa dupla natureza é o que permite empilhar decoradores indefinidamente.
abstract class NotificadorDecorator implements Notificador {

    protected final Notificador delegado;

    protected NotificadorDecorator(Notificador delegado) {
        this.delegado = delegado;
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        delegado.enviar(destinatario, mensagem);
    }
}

// Acrescenta comportamento antes e depois da chamada real.
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

// Transforma o argumento antes de repassar.
class NotificadorCriptografado extends NotificadorDecorator {

    NotificadorCriptografado(Notificador delegado) {
        super(delegado);
    }

    @Override
    public void enviar(String destinatario, String mensagem) {
        super.enviar(destinatario, cifrar(mensagem));
    }

    private String cifrar(String texto) {
        StringBuilder cifrado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            cifrado.append(Character.isLetter(c) ? (char) (c + 1) : c);
        }
        return cifrado.toString();
    }
}

// Controla SE a chamada acontece.
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

// Controla QUANTAS VEZES a chamada acontece.
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

// Cliente
class ServicoNotificacao {
    private final Notificador notificador;

    public ServicoNotificacao(Notificador notificador) {
        this.notificador = notificador;
    }

    public void avisar(String destinatario, String mensagem) {
        notificador.enviar(destinatario, mensagem);
    }

    public static void main(String[] args) {
        // Lê-se DE FORA PARA DENTRO. A ordem importa: inverter log e criptografia muda o que é
        // registrado no log.
        Notificador email = new NotificadorComLog(
                                new NotificadorComLimite(
                                    new NotificadorCriptografado(
                                        new NotificadorEmail()), 2));

        ServicoNotificacao servico = new ServicoNotificacao(email);
        servico.avisar("ana@exemplo.com", "pedido aprovado");
        servico.avisar("bruno@exemplo.com", "pedido aprovado");
        servico.avisar("carla@exemplo.com", "pedido aprovado");

        System.out.println("---");

        new ServicoNotificacao(new NotificadorComRetentativa(new NotificadorInstavel(), 5))
                .avisar("diego@exemplo.com", "código de acesso");

        System.out.println("---");

        new ServicoNotificacao(new NotificadorComLog(new NotificadorSms()))
                .avisar("+5511999990000", "seu pedido saiu para entrega");

        // java.io é o exemplo canônico na biblioteca padrão:
        // new BufferedReader(new InputStreamReader(new FileInputStream(arquivo)))
    }
}

//Decorator x Proxy: o Decorator ACRESCENTA comportamento e é montado pelo cliente; o Proxy
//CONTROLA o acesso e normalmente esconde o objeto real do cliente.
//Decorator x Chain of Responsibility: no Decorator todos os elos participam do resultado; na
//corrente, um elo trata e interrompe a passagem.

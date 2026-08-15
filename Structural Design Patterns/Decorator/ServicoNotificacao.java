//Acrescentar log, limite de envio, retentativa e criptografia ao envio de notificações. Por
//herança seria uma classe por COMBINAÇÃO, e a combinação é escolhida em tempo de execução - o que
//a herança nem permite.
//O Decorator acrescenta responsabilidades a um objeto DINAMICAMENTE, mantendo a mesma interface.
// Component

interface Notificador {
    void enviar(String destinatario, String mensagem);
}

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

class ServicoNotificacao {
    private final Notificador notificador;

    public ServicoNotificacao(Notificador notificador) {
        this.notificador = notificador;
    }

    public void avisar(String destinatario, String mensagem) {
        notificador.enviar(destinatario, mensagem);
    }

    public static void main(String[] args) {

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

    }
}

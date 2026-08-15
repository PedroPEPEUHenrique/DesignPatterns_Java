//POO - EXCEÇÕES COMO OBJETOS
//Exceção é objeto: tem tipo, hierarquia, estado e comportamento. Throwable se divide em Error, que
//é problema da máquina virtual e não se trata, e Exception. Dentro de Exception, RuntimeException e
//suas filhas são as NÃO VERIFICADAS (unchecked) e todas as demais são VERIFICADAS (checked), que o
//compilador obriga a tratar ou a declarar com throws.
//O catch é POLIMÓRFICO: catch (Exception e) captura qualquer subtipo. Por isso o bloco mais
//específico vem sempre antes do mais genérico - invertido, o específico fica inalcançável e o
//código nem compila.
//Use exceção não verificada para erro de programação e para violação de regra do domínio; use
//verificada quando o chamador tem uma recuperação plausível a fazer.
//A exceção do domínio carrega DADO, não apenas mensagem: "faltam 32,00" é mais útil do que um
//texto solto, porque quem trata consegue decidir com aquele número.
//Ao converter uma falha técnica em falha de domínio, passe a original como CAUSA. Perder a causa é
//perder a pista do que realmente aconteceu.
//finally roda sempre, com ou sem exceção; para liberar recurso, porém, o que se usa é
//try-with-resources, que fecha sozinho tudo que for AutoCloseable, na ordem inversa da abertura.
//Catch vazio é o pior desfecho possível: o erro desaparece e o defeito reaparece longe da origem.

class SaldoInsuficienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int faltaEmCentavos;

    SaldoInsuficienteException(int faltaEmCentavos) {
        super("faltam " + faltaEmCentavos + " centavos");
        this.faltaEmCentavos = faltaEmCentavos;
    }

    int getFaltaEmCentavos() {
        return faltaEmCentavos;
    }
}

class ContaBloqueadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ContaBloqueadaException(String motivo) {
        super(motivo);
    }
}

class RepositorioIndisponivelException extends Exception {

    private static final long serialVersionUID = 1L;

    RepositorioIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

class ConexaoSimulada implements AutoCloseable {

    private final String nome;

    ConexaoSimulada(String nome) {
        this.nome = nome;
        System.out.println("  abriu " + nome);
    }

    void gravar(String dado) {
        System.out.println("  gravou " + dado + " em " + nome);
    }

    @Override
    public void close() {
        System.out.println("  fechou " + nome);
    }
}

class ContaBancaria {

    private final String numero;
    private int saldoEmCentavos;
    private boolean bloqueada;

    ContaBancaria(String numero, int saldoInicialEmCentavos) {
        this.numero = numero;
        this.saldoEmCentavos = saldoInicialEmCentavos;
    }

    void sacar(int valorEmCentavos) {
        if (bloqueada) {
            throw new ContaBloqueadaException("conta " + numero + " está bloqueada");
        }
        if (valorEmCentavos <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        if (valorEmCentavos > saldoEmCentavos) {
            throw new SaldoInsuficienteException(valorEmCentavos - saldoEmCentavos);
        }
        saldoEmCentavos -= valorEmCentavos;
    }

    void bloquear() {
        bloqueada = true;
    }

    int getSaldoEmCentavos() {
        return saldoEmCentavos;
    }

    String getNumero() {
        return numero;
    }
}

class RepositorioDeContas {

    private final boolean bancoNoAr;

    RepositorioDeContas(boolean bancoNoAr) {
        this.bancoNoAr = bancoNoAr;
    }

    void salvar(ContaBancaria conta) throws RepositorioIndisponivelException {
        try {
            if (!bancoNoAr) {
                throw new IllegalStateException("connection refused: 127.0.0.1:5432");
            }
            System.out.println("  conta " + conta.getNumero() + " gravada");
        } catch (IllegalStateException falhaTecnica) {
            throw new RepositorioIndisponivelException(
                    "não foi possível gravar a conta " + conta.getNumero(), falhaTecnica);
        }
    }
}

class Excecoes {

    public static void main(String[] args) {
        ContaBancaria conta = new ContaBancaria("12345-6", 50000);

        try {
            conta.sacar(80000);
        } catch (SaldoInsuficienteException e) {
            System.out.println("a exceção carrega dado: faltam " + e.getFaltaEmCentavos()
                               + " centavos, ofereça um empréstimo desse valor");
        }

        try {
            conta.sacar(-1);
        } catch (SaldoInsuficienteException e) {
            System.out.println("não passa por aqui");
        } catch (IllegalArgumentException e) {
            System.out.println("do mais específico para o mais genérico: " + e.getMessage());
        } finally {
            System.out.println("finally executa em qualquer caso, saldo em "
                               + conta.getSaldoEmCentavos());
        }

        conta.bloquear();
        try {
            conta.sacar(1000);
        } catch (RuntimeException e) {
            System.out.println("catch polimórfico pegou " + e.getClass().getSimpleName()
                               + ": " + e.getMessage());
        }

        System.out.println("try-with-resources:");
        try (ConexaoSimulada principal = new ConexaoSimulada("banco");
             ConexaoSimulada auditoria = new ConexaoSimulada("auditoria")) {
            principal.gravar("movimento");
            auditoria.gravar("trilha");
        }

        System.out.println("exceção verificada e causa preservada:");
        try {
            new RepositorioDeContas(false).salvar(conta);
        } catch (RepositorioIndisponivelException e) {
            System.out.println("  domínio: " + e.getMessage());
            System.out.println("  causa técnica: " + e.getCause());
        }

        try {
            new RepositorioDeContas(true).salvar(conta);
        } catch (RepositorioIndisponivelException e) {
            System.out.println("  não deveria falhar: " + e.getMessage());
        }
    }
}

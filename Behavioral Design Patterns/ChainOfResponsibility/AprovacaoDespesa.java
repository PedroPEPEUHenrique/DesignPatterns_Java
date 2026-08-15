//Implementar a alçada de aprovação de despesas: supervisor até R$ 1.000, gerente até R$ 10.000,
//diretor até R$ 100.000, acima disso o conselho. Com if/else encadeado, cada mudança de alçada
//altera a classe e a ordem das faixas fica implícita na ordem dos ifs.
//O Chain of Responsibility evita o acoplamento entre quem envia a requisição e quem a trata,
//dando a MAIS DE UM objeto a chance de tratá-la.

class Despesa {
    private final String descricao;
    private final int valorEmCentavos;
    private final boolean urgente;

    Despesa(String descricao, int valorEmCentavos, boolean urgente) {
        this.descricao = descricao;
        this.valorEmCentavos = valorEmCentavos;
        this.urgente = urgente;
    }

    String getDescricao() {
        return descricao;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }

    boolean isUrgente() {
        return urgente;
    }
}

abstract class Aprovador {

    private Aprovador proximo;

    public Aprovador encadear(Aprovador proximo) {
        this.proximo = proximo;
        return proximo;
    }

    public final void aprovar(Despesa despesa) {
        if (podeAprovar(despesa)) {
            registrarAprovacao(despesa);
            return;
        }
        if (proximo != null) {
            System.out.println("  " + cargo() + " repassa adiante");
            proximo.aprovar(despesa);
            return;
        }

        throw new IllegalStateException("nenhum aprovador para " + despesa.getDescricao());
    }

    protected abstract boolean podeAprovar(Despesa despesa);

    protected abstract String cargo();

    protected void registrarAprovacao(Despesa despesa) {
        System.out.println("  " + cargo() + " APROVOU " + despesa.getDescricao()
                           + " (" + despesa.getValorEmCentavos() + " centavos)");
    }
}

class Supervisor extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        return despesa.getValorEmCentavos() <= 100000;
    }

    @Override
    protected String cargo() {
        return "supervisor";
    }
}

class Gerente extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        return despesa.getValorEmCentavos() <= 1000000;
    }

    @Override
    protected String cargo() {
        return "gerente";
    }
}

class Diretor extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        return despesa.getValorEmCentavos() <= 10000000;
    }

    @Override
    protected String cargo() {
        return "diretor";
    }
}

class Conselho extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        return true;
    }

    @Override
    protected String cargo() {
        return "conselho";
    }

    @Override
    protected void registrarAprovacao(Despesa despesa) {
        System.out.println("  conselho aprovou em reunião extraordinária: " + despesa.getDescricao());
    }
}

class RegistroAuditoria extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        System.out.println("  <auditoria> registrando pedido de " + despesa.getValorEmCentavos()
                           + " centavos");
        return false;
    }

    @Override
    protected String cargo() {
        return "auditoria";
    }
}

class BloqueioOrcamentario extends Aprovador {
    private int saldoDisponivelEmCentavos;

    BloqueioOrcamentario(int saldoDisponivelEmCentavos) {
        this.saldoDisponivelEmCentavos = saldoDisponivelEmCentavos;
    }

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        return despesa.getValorEmCentavos() > saldoDisponivelEmCentavos && !despesa.isUrgente();
    }

    @Override
    protected String cargo() {
        return "controle orçamentário";
    }

    @Override
    protected void registrarAprovacao(Despesa despesa) {
        System.out.println("  controle orçamentário RECUSOU " + despesa.getDescricao()
                           + ": saldo de " + saldoDisponivelEmCentavos + " centavos");
    }
}

class AprovacaoDespesa {

    private final Aprovador inicioDaCorrente;

    public AprovacaoDespesa(Aprovador inicioDaCorrente) {
        this.inicioDaCorrente = inicioDaCorrente;
    }

    public void solicitar(Despesa despesa) {
        System.out.println("pedido: " + despesa.getDescricao());
        inicioDaCorrente.aprovar(despesa);
    }

    public static void main(String[] args) {

        Aprovador inicio = new RegistroAuditoria();
        inicio.encadear(new BloqueioOrcamentario(5000000))
              .encadear(new Supervisor())
              .encadear(new Gerente())
              .encadear(new Diretor())
              .encadear(new Conselho());

        AprovacaoDespesa fluxo = new AprovacaoDespesa(inicio);

        fluxo.solicitar(new Despesa("café para a copa", 50000, false));
        fluxo.solicitar(new Despesa("notebooks para o time", 900000, false));
        fluxo.solicitar(new Despesa("reforma do andar", 8000000, false));
        fluxo.solicitar(new Despesa("aquisição de concorrente", 900000000, false));
        fluxo.solicitar(new Despesa("servidor emergencial", 6000000, true));

        System.out.println("--- filial com alçada reduzida ---");
        Aprovador filial = new Supervisor();
        filial.encadear(new Gerente());

        try {
            new AprovacaoDespesa(filial).solicitar(new Despesa("frota de veículos", 50000000, false));
        } catch (IllegalStateException e) {
            System.out.println("  " + e.getMessage());
        }
    }
}

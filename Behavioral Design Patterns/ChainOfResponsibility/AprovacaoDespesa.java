//Suponha que a sua tarefa seja implementar a alçada de aprovação de despesas. As regras são:
//Até R$ 1.000,00 o supervisor aprova
//Até R$ 10.000,00 o gerente aprova
//Até R$ 100.000,00 o diretor aprova
//Acima disso vai para o conselho

//Imagine a solução com um if/else encadeado dentro do serviço de despesas. Cada mudança de alçada
//- e elas mudam - obriga a alterar essa classe. Pior: a ordem das faixas fica implícita na ordem
//dos ifs, e não há como montar uma alçada diferente por filial em tempo de execução.

//O Chain of Responsibility resolve o problema de evitar o acoplamento entre quem envia uma
//requisição e quem a trata, dando a MAIS DE UM objeto a chance de tratá-la. A requisição percorre
//a corrente até que alguém a resolva.

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

// Padrão Chain of Responsibility - o Handler
// Guarda a referência para o PRÓXIMO da corrente. É essa referência que substitui o if/else:
// a estrutura de decisão vira a estrutura de ligação entre objetos.
abstract class Aprovador {

    private Aprovador proximo;

    // Devolve o parâmetro para permitir encadear a montagem de forma legível.
    public Aprovador encadear(Aprovador proximo) {
        this.proximo = proximo;
        return proximo;
    }

    // Template do fluxo: tenta tratar, senão repassa. As subclasses só dizem SE tratam e COMO.
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
        // Fim da corrente sem tratamento. É uma decisão de projeto: falhar ou ignorar em silêncio.
        // Ignorar em silêncio é a origem de bugs difíceis - aqui a corrente falha explicitamente.
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

    // Elo final que aceita tudo. Sem ele, valores muito altos chegariam ao fim da corrente e
    // lançariam exceção.
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

// Um elo que NÃO aprova, apenas observa e repassa. A corrente aceita responsabilidades de tipos
// diferentes - não precisa ser só alçada de valor.
class RegistroAuditoria extends Aprovador {

    @Override
    protected boolean podeAprovar(Despesa despesa) {
        System.out.println("  <auditoria> registrando pedido de " + despesa.getValorEmCentavos()
                           + " centavos");
        return false;   // nunca trata: sempre deixa seguir
    }

    @Override
    protected String cargo() {
        return "auditoria";
    }
}

// Elo que INTERROMPE a corrente por uma razão diferente da alçada.
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

// Classe Cliente
class AprovacaoDespesa {

    private final Aprovador inicioDaCorrente;

    // O cliente conhece apenas o PRIMEIRO elo e a abstração Aprovador. Não sabe quantos elos
    // existem, nem quais, nem qual deles vai tratar.
    public AprovacaoDespesa(Aprovador inicioDaCorrente) {
        this.inicioDaCorrente = inicioDaCorrente;
    }

    public void solicitar(Despesa despesa) {
        System.out.println("pedido: " + despesa.getDescricao());
        inicioDaCorrente.aprovar(despesa);
    }

    public static void main(String[] args) {
        // Montagem da corrente. A ordem é o que define a política - e ela é dado, não código.
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

        // Outra política, sem alterar nenhuma classe: filial pequena, sem diretoria local.
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

//Onde isso aparece na prática: filtros de Servlet (FilterChain), interceptadores de CDI/EJB,
//middlewares de frameworks web, e o tratamento de exceções da própria linguagem - um throw sobe a
//pilha até encontrar um catch que trate aquele tipo, que é a mesma ideia.

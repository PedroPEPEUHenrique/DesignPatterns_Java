//POO - CLASSE, OBJETO E CONSTRUTOR
//A CLASSE descreve um tipo: que dados cada exemplar guarda e o que ele sabe fazer. O OBJETO é o
//exemplar concreto, criado com new, com estado e identidade próprios - dois objetos com os mesmos
//valores continuam sendo dois objetos distintos.
//O CONSTRUTOR é o único caminho por onde um objeto nasce, e é por isso que as regras de criação
//moram nele: um objeto mal formado não deveria chegar a existir. Um construtor pode delegar a
//outro com this(...), em vez de repetir a validação.
//O que é marcado com static pertence à CLASSE, não ao objeto: existe uma vez só e é compartilhado
//por todos os exemplares. O que não é static existe uma vez por objeto.
//this é a referência ao objeto que recebeu a chamada; serve para distinguir o atributo do
//parâmetro de mesmo nome e para o objeto passar a si próprio adiante.

class ContaCorrente {

    private static final int LIMITE_PADRAO_EM_CENTAVOS = 50000;
    private static int totalDeContasAbertas;

    private final String agencia;
    private final String numero;
    private final String titular;
    private final int limiteEmCentavos;
    private int saldoEmCentavos;

    ContaCorrente(String agencia, String numero, String titular) {
        this(agencia, numero, titular, LIMITE_PADRAO_EM_CENTAVOS);
    }

    ContaCorrente(String agencia, String numero, String titular, int limiteEmCentavos) {
        exigirPreenchido(agencia, "agência");
        exigirPreenchido(numero, "número");
        exigirPreenchido(titular, "titular");
        if (limiteEmCentavos < 0) {
            throw new IllegalArgumentException("limite não pode ser negativo");
        }
        this.agencia = agencia;
        this.numero = numero;
        this.titular = titular;
        this.limiteEmCentavos = limiteEmCentavos;
        totalDeContasAbertas++;
    }

    void depositar(int valorEmCentavos) {
        exigirValorPositivo(valorEmCentavos);
        saldoEmCentavos += valorEmCentavos;
    }

    void sacar(int valorEmCentavos) {
        exigirValorPositivo(valorEmCentavos);
        if (valorEmCentavos > saldoEmCentavos + limiteEmCentavos) {
            throw new IllegalStateException("saldo e limite insuficientes");
        }
        saldoEmCentavos -= valorEmCentavos;
    }

    void transferirPara(ContaCorrente destino, int valorEmCentavos) {
        this.sacar(valorEmCentavos);
        destino.depositar(valorEmCentavos);
    }

    String identificacao() {
        return agencia + "/" + numero + " (" + titular + ")";
    }

    int getSaldoEmCentavos() {
        return saldoEmCentavos;
    }

    static int getTotalDeContasAbertas() {
        return totalDeContasAbertas;
    }

    private static void exigirPreenchido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
    }

    private static void exigirValorPositivo(int valorEmCentavos) {
        if (valorEmCentavos <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }
}

class ObjetosEClasses {

    public static void main(String[] args) {
        ContaCorrente daAna = new ContaCorrente("0001", "12345-6", "Ana");
        ContaCorrente doBruno = new ContaCorrente("0001", "98765-4", "Bruno", 200000);
        ContaCorrente outraDaAna = new ContaCorrente("0001", "12345-6", "Ana");

        System.out.println("mesmos dados, mesmo objeto? " + (daAna == outraDaAna));
        System.out.println("contas abertas (dado da classe): "
                           + ContaCorrente.getTotalDeContasAbertas());

        daAna.depositar(150000);
        daAna.transferirPara(doBruno, 40000);

        System.out.println(daAna.identificacao() + " saldo " + daAna.getSaldoEmCentavos());
        System.out.println(doBruno.identificacao() + " saldo " + doBruno.getSaldoEmCentavos());

        try {
            new ContaCorrente("0001", "  ", "Carla");
        } catch (IllegalArgumentException e) {
            System.out.println("o construtor impediu o objeto inválido: " + e.getMessage());
        }

        try {
            doBruno.sacar(9000000);
        } catch (IllegalStateException e) {
            System.out.println("a regra mora no objeto: " + e.getMessage());
        }

        System.out.println("contas abertas ao final: "
                           + ContaCorrente.getTotalDeContasAbertas());
    }
}

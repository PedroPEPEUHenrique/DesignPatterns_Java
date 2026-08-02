//GRASP - HIGH COHESION (Alta Coesão)

//Problema: como manter as classes focadas, compreensíveis e fáceis de manter?
//Solução: atribua responsabilidades de forma que a COESÃO - o grau em que as responsabilidades de
//uma classe são funcionalmente relacionadas entre si - permaneça alta.

//Suponha que a sua tarefa seja evoluir a classe que representa uma conta bancária.
//Imagine que, ao longo do tempo, foram parando nela: o cálculo de saldo, a formatação do extrato
//em PDF, a validação de CPF, o envio de e-mail ao cliente, a conexão com o banco de dados e a
//conversão de moeda. A classe tem 900 linhas, cinco motivos diferentes para mudar e ninguém
//consegue alterá-la sem medo. É a "classe baixa coesão", também chamada de God Object.

import java.util.ArrayList;
import java.util.List;

// COMO NÃO FAZER - baixa coesão
// Conte os assuntos: movimentação, formatação, validação de documento, e-mail, persistência e
// câmbio. Seis responsabilidades sem relação funcional entre si dentro de um mesmo tipo.
class ContaBancariaFazTudo {

    private final String titular;
    private final String cpf;
    private int saldoEmCentavos;
    private final List<String> movimentos = new ArrayList<>();

    ContaBancariaFazTudo(String titular, String cpf) {
        this.titular = titular;
        this.cpf = cpf;
    }

    // (1) movimentação - esta É responsabilidade da conta
    void depositar(int valorEmCentavos) {
        saldoEmCentavos += valorEmCentavos;
        movimentos.add("crédito " + valorEmCentavos);
    }

    // (2) formatação de relatório - responsabilidade de apresentação
    String extratoEmHtml() {
        StringBuilder html = new StringBuilder("<table>");
        for (String movimento : movimentos) {
            html.append("<tr><td>").append(movimento).append("</td></tr>");
        }
        return html.append("</table>").toString();
    }

    // (3) validação de documento - regra de outro domínio
    boolean cpfValido() {
        return cpf.replaceAll("\\D", "").length() == 11;
    }

    // (4) comunicação - infraestrutura
    void enviarEmailDeExtrato(String servidorSmtp) {
        System.out.println("conectando em " + servidorSmtp + " e enviando extrato...");
    }

    // (5) persistência - infraestrutura
    void salvarNoBanco(String urlJdbc) {
        System.out.println("abrindo conexão " + urlJdbc + " e gravando...");
    }

    // (6) câmbio - outro domínio inteiro
    double saldoEmDolar(double cotacao) {
        return (saldoEmCentavos / 100.0) / cotacao;
    }
}

// COMO FAZER - alta coesão
// Cada classe passa a ter UM assunto e UM motivo para mudar.

// Objeto de valor coeso: só sabe ser um CPF e dizer se é válido.
final class Cpf {

    private final String numero;

    Cpf(String numero) {
        String limpo = numero.replaceAll("\\D", "");
        if (limpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido: " + numero);
        }
        this.numero = limpo;
    }

    // Como a validação acontece no construtor, um Cpf que existe é necessariamente válido - o
    // resto do sistema não precisa mais checar isso em lugar nenhum.
    String formatado() {
        return numero.substring(0, 3) + "." + numero.substring(3, 6) + "."
               + numero.substring(6, 9) + "-" + numero.substring(9);
    }
}

class Movimento {
    private final String tipo;
    private final int valorEmCentavos;

    Movimento(String tipo, int valorEmCentavos) {
        this.tipo = tipo;
        this.valorEmCentavos = valorEmCentavos;
    }

    String getTipo() {
        return tipo;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }

    int efeitoNoSaldo() {
        return "crédito".equals(tipo) ? valorEmCentavos : -valorEmCentavos;
    }
}

// A conta cuida APENAS de saldo e movimentação. Um motivo para mudar: regra de movimentação.
class ContaBancaria {

    private final String titular;
    private final Cpf cpf;
    private int saldoEmCentavos;
    private final List<Movimento> movimentos = new ArrayList<>();

    ContaBancaria(String titular, Cpf cpf) {
        this.titular = titular;
        this.cpf = cpf;
    }

    public void depositar(int valorEmCentavos) {
        registrar(new Movimento("crédito", valorEmCentavos));
    }

    public void sacar(int valorEmCentavos) {
        if (valorEmCentavos > saldoEmCentavos) {
            throw new IllegalStateException("saldo insuficiente");
        }
        registrar(new Movimento("débito", valorEmCentavos));
    }

    private void registrar(Movimento movimento) {
        movimentos.add(movimento);
        saldoEmCentavos += movimento.efeitoNoSaldo();
    }

    public int getSaldoEmCentavos() {
        return saldoEmCentavos;
    }

    public String getTitular() {
        return titular;
    }

    public Cpf getCpf() {
        return cpf;
    }

    public List<Movimento> getMovimentos() {
        return List.copyOf(movimentos);
    }
}

// Um motivo para mudar: formato do extrato.
class ExtratoHtml {

    String gerar(ContaBancaria conta) {
        StringBuilder html = new StringBuilder("<h2>" + conta.getTitular() + " ("
                                               + conta.getCpf().formatado() + ")</h2><table>");
        for (Movimento movimento : conta.getMovimentos()) {
            html.append("<tr><td>").append(movimento.getTipo())
                .append("</td><td>").append(movimento.getValorEmCentavos()).append("</td></tr>");
        }
        return html.append("</table>").toString();
    }
}

// Um motivo para mudar: tecnologia de persistência.
class RepositorioConta {

    void salvar(ContaBancaria conta) {
        System.out.println("  [repositório] gravando conta de " + conta.getTitular()
                           + " com saldo " + conta.getSaldoEmCentavos());
    }
}

// Um motivo para mudar: canal de comunicação.
class EnvioDeExtrato {

    void enviar(ContaBancaria conta, ExtratoHtml formatador) {
        System.out.println("  [envio] extrato de " + conta.getTitular()
                           + " (" + formatador.gerar(conta).length() + " bytes)");
    }
}

// Um motivo para mudar: regra de câmbio.
class ConversorMoeda {

    double paraDolar(int valorEmCentavos, double cotacao) {
        return (valorEmCentavos / 100.0) / cotacao;
    }
}

// Classe Cliente
class AltaCoesao {

    public static void main(String[] args) {
        ContaBancaria conta = new ContaBancaria("Ana Souza", new Cpf("111.222.333-44"));
        conta.depositar(500000);
        conta.sacar(120000);

        System.out.println("saldo: " + conta.getSaldoEmCentavos());

        new RepositorioConta().salvar(conta);
        new EnvioDeExtrato().enviar(conta, new ExtratoHtml());

        System.out.println("em dólar: " + new ConversorMoeda().paraDolar(conta.getSaldoEmCentavos(), 5.4));

        // A validação de CPF deixou de ser um método perdido na conta: virou um tipo. Não existe
        // mais como circular pelo sistema um CPF inválido.
        try {
            new ContaBancaria("Bruno", new Cpf("123"));
        } catch (IllegalArgumentException e) {
            System.out.println("recusado: " + e.getMessage());
        }
    }
}

//Sintomas de baixa coesão, na ordem em que costumam aparecer:
//A classe tem mais de um "motivo para mudar" - é o mesmo que diz o Single Responsibility
//  Principle do SOLID, sob outro nome.
//Fica difícil dar um nome à classe sem usar "Gerenciador", "Utilitário" ou "Helper".
//Os métodos se dividem em grupos que usam atributos diferentes, sem interseção entre os grupos.
//O teste da classe precisa de infraestrutura que não tem nada a ver com o assunto principal.
//
//Coesão e acoplamento andam juntos e em tensão: quebrar uma classe grande em várias aumenta a
//coesão de cada uma, mas cria dependências entre elas. Bom desenho é encontrar o ponto de
//equilíbrio, e não maximizar uma das duas isoladamente.

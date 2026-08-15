//GRASP - HIGH COHESION
//Problema: como manter cada classe focada, compreensível e barata de manter?
//Solução: atribuir responsabilidades de modo que cada classe fique com um conjunto PEQUENO e
//FORTEMENTE RELACIONADO delas. A conta que valida CPF, gera HTML, envia e-mail, grava no banco e
//converte moeda muda por cinco motivos diferentes; separada, cada peça muda por um só. Alta coesão
//e baixo acoplamento andam juntos - buscar um sozinho degenera o outro.

import java.util.ArrayList;
import java.util.List;

class ContaBancariaFazTudo {

    private final String titular;
    private final String cpf;
    private int saldoEmCentavos;
    private final List<String> movimentos = new ArrayList<>();

    ContaBancariaFazTudo(String titular, String cpf) {
        this.titular = titular;
        this.cpf = cpf;
    }

    void depositar(int valorEmCentavos) {
        saldoEmCentavos += valorEmCentavos;
        movimentos.add("crédito " + valorEmCentavos);
    }

    String extratoEmHtml() {
        StringBuilder html = new StringBuilder("<table>");
        for (String movimento : movimentos) {
            html.append("<tr><td>").append(movimento).append("</td></tr>");
        }
        return html.append("</table>").toString();
    }

    boolean cpfValido() {
        return cpf.replaceAll("\\D", "").length() == 11;
    }

    void enviarEmailDeExtrato(String servidorSmtp) {
        System.out.println("conectando em " + servidorSmtp + " e enviando extrato...");
    }

    void salvarNoBanco(String urlJdbc) {
        System.out.println("abrindo conexão " + urlJdbc + " e gravando...");
    }

    double saldoEmDolar(double cotacao) {
        return (saldoEmCentavos / 100.0) / cotacao;
    }
}

final class Cpf {

    private final String numero;

    Cpf(String numero) {
        String limpo = numero.replaceAll("\\D", "");
        if (limpo.length() != 11) {
            throw new IllegalArgumentException("CPF inválido: " + numero);
        }
        this.numero = limpo;
    }

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

class RepositorioConta {

    void salvar(ContaBancaria conta) {
        System.out.println("  [repositório] gravando conta de " + conta.getTitular()
                           + " com saldo " + conta.getSaldoEmCentavos());
    }
}

class EnvioDeExtrato {

    void enviar(ContaBancaria conta, ExtratoHtml formatador) {
        System.out.println("  [envio] extrato de " + conta.getTitular()
                           + " (" + formatador.gerar(conta).length() + " bytes)");
    }
}

class ConversorMoeda {

    double paraDolar(int valorEmCentavos, double cotacao) {
        return (valorEmCentavos / 100.0) / cotacao;
    }
}

class AltaCoesao {

    public static void main(String[] args) {
        ContaBancaria conta = new ContaBancaria("Ana Souza", new Cpf("111.222.333-44"));
        conta.depositar(500000);
        conta.sacar(120000);

        System.out.println("saldo: " + conta.getSaldoEmCentavos());

        new RepositorioConta().salvar(conta);
        new EnvioDeExtrato().enviar(conta, new ExtratoHtml());

        System.out.println("em dólar: " + new ConversorMoeda().paraDolar(conta.getSaldoEmCentavos(), 5.4));

        try {
            new ContaBancaria("Bruno", new Cpf("123"));
        } catch (IllegalArgumentException e) {
            System.out.println("recusado: " + e.getMessage());
        }
    }
}

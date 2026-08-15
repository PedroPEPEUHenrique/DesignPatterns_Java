//POO - IMUTABILIDADE E OBJETOS DE VALOR
//Objeto imutável é aquele cujo estado não muda depois da construção. Toda "alteração" devolve um
//objeto NOVO - é assim que String, LocalDate e BigDecimal funcionam.
//A receita: classe final, campos private final, nenhum setter e CÓPIA DEFENSIVA de tudo que for
//mutável, tanto na entrada, no construtor, quanto na saída, nos getters. Sem a cópia, quem passou
//a lista continua com a referência em mãos e altera o objeto por fora.
//O que se ganha: validado uma vez no construtor, o objeto nunca mais fica inválido; pode ser
//compartilhado entre threads sem sincronização; serve de chave de mapa sem surpresa; e a leitura
//do código fica simples, porque ninguém altera nada às escondidas.
//O que se paga: um objeto novo por operação. Quase sempre é troca vantajosa; quando não for,
//isole a parte mutável em vez de abrir a classe inteira.
//OBJETO DE VALOR é o par natural da imutabilidade: um tipo pequeno definido pelo seu valor, e não
//pela sua identidade - dinheiro, CPF, período, coordenada. Trocar int e String soltos por objetos
//de valor põe a validação e as operações no lugar certo. record é a forma curta de escrevê-los,
//mas a cópia defensiva de campos mutáveis continua sendo responsabilidade sua.

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class Dinheiro {

    private final BigDecimal valor;
    private final String moeda;

    Dinheiro(BigDecimal valor, String moeda) {
        if (valor == null || moeda == null) {
            throw new IllegalArgumentException("valor e moeda são obrigatórios");
        }
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
        this.moeda = moeda;
    }

    Dinheiro mais(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return new Dinheiro(valor.add(outro.valor), moeda);
    }

    Dinheiro menos(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return new Dinheiro(valor.subtract(outro.valor), moeda);
    }

    Dinheiro vezes(int quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)), moeda);
    }

    boolean maiorQue(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return valor.compareTo(outro.valor) > 0;
    }

    private void exigirMesmaMoeda(Dinheiro outro) {
        if (!moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException("moedas diferentes: " + moeda + " e " + outro.moeda);
        }
    }

    @Override
    public String toString() {
        return moeda + " " + valor;
    }
}

class PeriodoDescuidado {

    private final LocalDate inicio;
    private final List<LocalDate> feriados;

    PeriodoDescuidado(LocalDate inicio, List<LocalDate> feriados) {
        this.inicio = inicio;
        this.feriados = feriados;
    }

    List<LocalDate> getFeriados() {
        return feriados;
    }

    LocalDate getInicio() {
        return inicio;
    }
}

final class Periodo {

    private final LocalDate inicio;
    private final LocalDate fim;
    private final List<LocalDate> feriados;

    Periodo(LocalDate inicio, LocalDate fim, List<LocalDate> feriados) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("período inválido");
        }
        this.inicio = inicio;
        this.fim = fim;
        this.feriados = List.copyOf(feriados);
    }

    Periodo prorrogarPorDias(int dias) {
        return new Periodo(inicio, fim.plusDays(dias), feriados);
    }

    long diasCorridos() {
        return java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    long diasUteisAproximados() {
        return diasCorridos() - feriados.size();
    }

    List<LocalDate> getFeriados() {
        return feriados;
    }

    @Override
    public String toString() {
        return inicio + " a " + fim;
    }
}

record Coordenada(double latitude, double longitude) {

    Coordenada {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude fora da faixa");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude fora da faixa");
        }
    }

    Coordenada deslocar(double deltaLatitude, double deltaLongitude) {
        return new Coordenada(latitude + deltaLatitude, longitude + deltaLongitude);
    }
}

class Imutabilidade {

    public static void main(String[] args) {
        Dinheiro preco = new Dinheiro(new BigDecimal("199.90"), "BRL");
        Dinheiro frete = new Dinheiro(new BigDecimal("34.50"), "BRL");
        Dinheiro total = preco.vezes(3).mais(frete);

        System.out.println("preço original permanece " + preco);
        System.out.println("total calculado: " + total);
        System.out.println("total maior que o preço? " + total.maiorQue(preco));

        try {
            preco.mais(new Dinheiro(new BigDecimal("10.00"), "USD"));
        } catch (IllegalArgumentException e) {
            System.out.println("objeto de valor barra a operação sem sentido: " + e.getMessage());
        }

        List<LocalDate> feriados = new ArrayList<>();
        feriados.add(LocalDate.of(2026, 9, 7));

        PeriodoDescuidado descuidado = new PeriodoDescuidado(LocalDate.of(2026, 9, 1), feriados);
        feriados.add(LocalDate.of(2026, 12, 25));
        descuidado.getFeriados().add(LocalDate.of(2026, 1, 1));
        System.out.println("sem cópia defensiva, o interior mudou de fora: "
                           + descuidado.getFeriados().size() + " feriados");

        Periodo periodo = new Periodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                                      List.of(LocalDate.of(2026, 9, 7)));
        System.out.println(periodo + ": " + periodo.diasCorridos() + " dias corridos, "
                           + periodo.diasUteisAproximados() + " descontando feriado");

        Periodo prorrogado = periodo.prorrogarPorDias(15);
        System.out.println("original intacto: " + periodo + " | novo objeto: " + prorrogado);

        try {
            periodo.getFeriados().add(LocalDate.of(2026, 12, 25));
        } catch (UnsupportedOperationException e) {
            System.out.println("a lista devolvida é imutável");
        }

        Coordenada saoPaulo = new Coordenada(-23.55, -46.63);
        System.out.println("record de valor: " + saoPaulo
                           + " deslocado para " + saoPaulo.deslocar(0.1, 0.1));

        try {
            new Coordenada(120, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("validação no construtor compacto: " + e.getMessage());
        }
    }
}

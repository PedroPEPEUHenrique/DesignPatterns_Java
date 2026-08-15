//POO - ABSTRAÇÃO
//Abstrair é ficar apenas com o que IMPORTA PARA O PROBLEMA. A praça de pedágio não precisa saber a
//cor do veículo nem o nome do dono: precisa de eixos, peso e tarifa. O mesmo veículo, num sistema
//de oficina, seria abstraído de outro jeito - não existe modelo certo fora de um problema.
//Em Java a abstração aparece como CLASSE ABSTRATA: um tipo que nomeia o conceito, não pode ser
//instanciado com new e deixa métodos ABSTRATOS, sem corpo, para as subclasses completarem. Ela
//pode ter estado, construtor e métodos prontos - é isso que a distingue de uma interface.
//O cliente conversa com o TIPO ABSTRATO e não sabe qual subclasse tem em mãos. Uma categoria nova
//de veículo entra no sistema sem que a praça de pedágio seja alterada.

abstract class Veiculo {

    private final String placa;
    private final int pesoEmKg;

    protected Veiculo(String placa, int pesoEmKg) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("placa é obrigatória");
        }
        if (pesoEmKg <= 0) {
            throw new IllegalArgumentException("peso deve ser positivo");
        }
        this.placa = placa;
        this.pesoEmKg = pesoEmKg;
    }

    abstract int eixos();

    abstract int tarifaPorEixoEmCentavos();

    abstract String categoria();

    int pedagioEmCentavos() {
        return tarifaPorEixoEmCentavos() * eixos() + adicionalPorPesoEmCentavos();
    }

    protected int adicionalPorPesoEmCentavos() {
        if (pesoEmKg <= 3500) {
            return 0;
        }
        return ((pesoEmKg - 3500) / 1000) * 150;
    }

    String getPlaca() {
        return placa;
    }

    int getPesoEmKg() {
        return pesoEmKg;
    }
}

class Motocicleta extends Veiculo {

    Motocicleta(String placa, int pesoEmKg) {
        super(placa, pesoEmKg);
    }

    @Override
    int eixos() {
        return 2;
    }

    @Override
    int tarifaPorEixoEmCentavos() {
        return 250;
    }

    @Override
    String categoria() {
        return "motocicleta";
    }
}

class Automovel extends Veiculo {

    Automovel(String placa, int pesoEmKg) {
        super(placa, pesoEmKg);
    }

    @Override
    int eixos() {
        return 2;
    }

    @Override
    int tarifaPorEixoEmCentavos() {
        return 500;
    }

    @Override
    String categoria() {
        return "automóvel";
    }
}

class Caminhao extends Veiculo {

    private final int eixos;

    Caminhao(String placa, int pesoEmKg, int eixos) {
        super(placa, pesoEmKg);
        if (eixos < 2) {
            throw new IllegalArgumentException("caminhão tem pelo menos 2 eixos");
        }
        this.eixos = eixos;
    }

    @Override
    int eixos() {
        return eixos;
    }

    @Override
    int tarifaPorEixoEmCentavos() {
        return 500;
    }

    @Override
    String categoria() {
        return "caminhão";
    }
}

class VeiculoEletrico extends Veiculo {

    VeiculoEletrico(String placa, int pesoEmKg) {
        super(placa, pesoEmKg);
    }

    @Override
    int eixos() {
        return 2;
    }

    @Override
    int tarifaPorEixoEmCentavos() {
        return 400;
    }

    @Override
    String categoria() {
        return "elétrico";
    }

    @Override
    protected int adicionalPorPesoEmCentavos() {
        return 0;
    }
}

class PracaDePedagio {

    private int arrecadadoEmCentavos;
    private int veiculosAtendidos;

    void passar(Veiculo veiculo) {
        int valor = veiculo.pedagioEmCentavos();
        arrecadadoEmCentavos += valor;
        veiculosAtendidos++;
        System.out.println(veiculo.getPlaca() + " (" + veiculo.categoria() + ", "
                           + veiculo.eixos() + " eixos): " + valor + " centavos");
    }

    int getArrecadadoEmCentavos() {
        return arrecadadoEmCentavos;
    }

    int getVeiculosAtendidos() {
        return veiculosAtendidos;
    }
}

class Abstracao {

    public static void main(String[] args) {
        PracaDePedagio praca = new PracaDePedagio();

        Veiculo[] fila = {
                new Motocicleta("MOT1A23", 180),
                new Automovel("CAR2B34", 1400),
                new Caminhao("CAM3C45", 12000, 5),
                new VeiculoEletrico("ELE4D56", 2100)
        };

        for (Veiculo veiculo : fila) {
            praca.passar(veiculo);
        }

        System.out.println("atendidos: " + praca.getVeiculosAtendidos()
                           + ", arrecadado: " + praca.getArrecadadoEmCentavos() + " centavos");
    }
}

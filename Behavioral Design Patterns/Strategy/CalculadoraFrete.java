//Calcular o frete de um pedido em quatro modalidades: econômico, expresso, retirada na loja e
//transportadora parceira. Num único método com switch, quatro regras diferentes se misturam no
//mesmo corpo e cada modalidade nova reabre a mesma classe.
//O Strategy define uma FAMÍLIA DE ALGORITMOS, encapsula cada um e os torna intercambiáveis,
//permitindo que o algoritmo varie independentemente dos clientes que o utilizam.

import java.util.HashMap;
import java.util.Map;

class Encomenda {
    private final int pesoEmGramas;
    private final int distanciaEmKm;
    private final String regiao;
    private final int valorDeclaradoEmCentavos;

    Encomenda(int pesoEmGramas, int distanciaEmKm, String regiao, int valorDeclaradoEmCentavos) {
        this.pesoEmGramas = pesoEmGramas;
        this.distanciaEmKm = distanciaEmKm;
        this.regiao = regiao;
        this.valorDeclaradoEmCentavos = valorDeclaradoEmCentavos;
    }

    int getPesoEmGramas() {
        return pesoEmGramas;
    }

    int getDistanciaEmKm() {
        return distanciaEmKm;
    }

    String getRegiao() {
        return regiao;
    }

    int getValorDeclaradoEmCentavos() {
        return valorDeclaradoEmCentavos;
    }
}

// Strategy
// A assinatura é a mesma para todos e recebe tudo de que qualquer estratégia possa precisar -
// definir bem esse contrato é a parte mais delicada do padrão.
interface EstrategiaFrete {

    int calcularEmCentavos(Encomenda encomenda);

    String nome();

    default int prazoEmDias() {
        return 10;
    }
}

// Estratégias concretas: cada uma isolada, testável sozinha e sem saber que as outras existem.

class FreteEconomico implements EstrategiaFrete {

    @Override
    public int calcularEmCentavos(Encomenda encomenda) {
        int peso = encomenda.getPesoEmGramas();
        if (peso <= 500) {
            return 1500;
        }
        if (peso <= 2000) {
            return 2500;
        }
        return 2500 + ((peso - 2000) / 1000) * 800;
    }

    @Override
    public String nome() {
        return "econômico";
    }

    @Override
    public int prazoEmDias() {
        return 12;
    }
}

class FreteExpresso implements EstrategiaFrete {

    private static final int TAXA_FIXA = 900;

    @Override
    public int calcularEmCentavos(Encomenda encomenda) {
        int porPeso = (encomenda.getPesoEmGramas() / 500) * 700;
        int porDistancia = encomenda.getDistanciaEmKm() * 12;
        return TAXA_FIXA + porPeso + porDistancia;
    }

    @Override
    public String nome() {
        return "expresso";
    }

    @Override
    public int prazoEmDias() {
        return 2;
    }
}

class RetiradaNaLoja implements EstrategiaFrete {

    @Override
    public int calcularEmCentavos(Encomenda encomenda) {
        return 0;
    }

    @Override
    public String nome() {
        return "retirada na loja";
    }

    @Override
    public int prazoEmDias() {
        return 0;
    }
}

class FreteTransportadoraParceira implements EstrategiaFrete {

    private final Map<String, Integer> tabelaPorRegiao = new HashMap<>();

    FreteTransportadoraParceira() {
        tabelaPorRegiao.put("SUDESTE", 1800);
        tabelaPorRegiao.put("SUL", 2200);
        tabelaPorRegiao.put("NORDESTE", 3400);
        tabelaPorRegiao.put("NORTE", 4900);
        tabelaPorRegiao.put("CENTRO-OESTE", 3100);
    }

    @Override
    public int calcularEmCentavos(Encomenda encomenda) {
        int base = tabelaPorRegiao.getOrDefault(encomenda.getRegiao(), 5000);
        int seguro = (int) (encomenda.getValorDeclaradoEmCentavos() * 0.01);
        return base + seguro;
    }

    @Override
    public String nome() {
        return "transportadora parceira";
    }

    @Override
    public int prazoEmDias() {
        return 6;
    }
}

// Estratégia que COMPÕE outras: o contexto não a distingue de uma estratégia simples.
class FreteMaisBarato implements EstrategiaFrete {

    private final EstrategiaFrete[] candidatas;

    FreteMaisBarato(EstrategiaFrete... candidatas) {
        this.candidatas = candidatas;
    }

    @Override
    public int calcularEmCentavos(Encomenda encomenda) {
        return escolher(encomenda).calcularEmCentavos(encomenda);
    }

    @Override
    public String nome() {
        return "mais barato";
    }

    private EstrategiaFrete escolher(Encomenda encomenda) {
        EstrategiaFrete melhor = candidatas[0];
        for (EstrategiaFrete candidata : candidatas) {
            if (candidata.calcularEmCentavos(encomenda) < melhor.calcularEmCentavos(encomenda)) {
                melhor = candidata;
            }
        }
        return melhor;
    }
}

// Context: nenhum if, nenhuma regra de cálculo. Uma modalidade nova não muda esta classe.
class CalculadoraFrete {

    private EstrategiaFrete estrategia;

    public CalculadoraFrete(EstrategiaFrete estrategia) {
        this.estrategia = estrategia;
    }

    // Trocar o algoritmo em tempo de execução é o que a herança não permitiria.
    public void setEstrategia(EstrategiaFrete estrategia) {
        this.estrategia = estrategia;
    }

    public void cotar(Encomenda encomenda) {
        System.out.println("  " + estrategia.nome() + ": "
                           + estrategia.calcularEmCentavos(encomenda) + " centavos, "
                           + estrategia.prazoEmDias() + " dia(s)");
    }

    public static void main(String[] args) {
        Encomenda encomenda = new Encomenda(3200, 480, "NORDESTE", 250000);

        CalculadoraFrete calculadora = new CalculadoraFrete(new FreteEconomico());
        System.out.println("cotações para a encomenda:");
        calculadora.cotar(encomenda);

        calculadora.setEstrategia(new FreteExpresso());
        calculadora.cotar(encomenda);

        calculadora.setEstrategia(new FreteTransportadoraParceira());
        calculadora.cotar(encomenda);

        calculadora.setEstrategia(new RetiradaNaLoja());
        calculadora.cotar(encomenda);

        calculadora.setEstrategia(new FreteMaisBarato(
                new FreteEconomico(), new FreteExpresso(), new FreteTransportadoraParceira()));
        calculadora.cotar(encomenda);

        // Não cabe lambda aqui porque a interface declara dois métodos abstratos - exemplo de como
        // o desenho da interface afeta a ergonomia do padrão.
        System.out.println("promoção pontual:");
        calculadora.setEstrategia(new EstrategiaFrete() {
            @Override
            public int calcularEmCentavos(Encomenda e) {
                return e.getValorDeclaradoEmCentavos() > 200000 ? 0 : 1200;
            }

            @Override
            public String nome() {
                return "frete grátis acima de R$ 2.000";
            }
        });
        calculadora.cotar(encomenda);
    }
}

//Strategy x State: no Strategy quem escolhe é o cliente, de fora, e as estratégias se ignoram.
//Strategy x Template Method: o Template Method varia por HERANÇA, com o esqueleto fixo na
//superclasse; o Strategy varia por COMPOSIÇÃO, trocável em tempo de execução.
//Na biblioteca padrão: o Comparator passado a Collections.sort() é uma estratégia de ordenação.

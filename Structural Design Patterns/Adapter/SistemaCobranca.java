//Suponha que a sua tarefa seja fazer o sistema de cobrança aceitar um segundo gateway de
//pagamento. O sistema já trabalha com a abstração ProcessadorPagamento, que expõe:
//cobrar(valorEmCentavos, numeroCartao) devolvendo um Recibo

//Imagine que o novo gateway é uma biblioteca de terceiros que você NÃO pode alterar. A interface
//dela é incompatível: recebe o valor em reais como double, o cartão dentro de um objeto próprio,
//devolve um código numérico em vez de um recibo e sinaliza falha com exceção checada.

//O Adapter resolve o problema de fazer duas interfaces incompatíveis trabalharem juntas,
//convertendo a interface de uma classe naquela que o cliente espera - sem alterar nenhuma das duas.

// A abstração que o nosso sistema já usa. Todo o código de cobrança depende só dela.
interface ProcessadorPagamento {
    Recibo cobrar(int valorEmCentavos, String numeroCartao);
}

class Recibo {
    private final boolean aprovado;
    private final String identificador;

    Recibo(boolean aprovado, String identificador) {
        this.aprovado = aprovado;
        this.identificador = identificador;
    }

    boolean isAprovado() {
        return aprovado;
    }

    String getIdentificador() {
        return identificador;
    }
}

// Implementação nossa, já existente. Serve de referência do formato esperado.
class ProcessadorInterno implements ProcessadorPagamento {

    @Override
    public Recibo cobrar(int valorEmCentavos, String numeroCartao) {
        System.out.println("[interno] cobrando " + valorEmCentavos + " centavos");
        return new Recibo(true, "INT-" + numeroCartao.substring(numeroCartao.length() - 4));
    }
}

// O ADAPTEE - código de terceiro, fora do nosso controle.
// Repare em tudo que é incompatível: unidade (reais x centavos), tipo (double x int),
// parâmetro (objeto próprio x String), retorno (int x Recibo) e erro (exceção x flag).
class GatewayTerceiroCartao {
    private final String chaveApi;

    GatewayTerceiroCartao(String chaveApi) {
        this.chaveApi = chaveApi;
    }

    public int executeCharge(double amountInDollars, CardData card) throws ChargeException {
        if (amountInDollars <= 0) {
            throw new ChargeException("invalid amount");
        }
        System.out.println("[terceiro] charging " + amountInDollars + " with key " + chaveApi);
        return Math.abs(card.getPan().hashCode());
    }
}

class CardData {
    private final String pan;

    CardData(String pan) {
        this.pan = pan;
    }

    String getPan() {
        return pan;
    }
}

class ChargeException extends Exception {
    ChargeException(String mensagem) {
        super(mensagem);
    }
}

// Padrão Adapter - adaptador por COMPOSIÇÃO (object adapter)
// Implementa a interface que o cliente espera e guarda uma referência ao adaptee, delegando e
// traduzindo. É a forma preferida: funciona mesmo que o adaptee seja final e permite adaptar
// mais de um objeto.
class AdaptadorGatewayTerceiro implements ProcessadorPagamento {

    private final GatewayTerceiroCartao gateway;

    AdaptadorGatewayTerceiro(GatewayTerceiroCartao gateway) {
        this.gateway = gateway;
    }

    @Override
    public Recibo cobrar(int valorEmCentavos, String numeroCartao) {
        // 1. converte a unidade e o tipo do valor
        double valorEmDolares = valorEmCentavos / 100.0;

        // 2. embrulha o parâmetro no tipo que o adaptee entende
        CardData cartao = new CardData(numeroCartao);

        try {
            // 3. delega
            int codigo = gateway.executeCharge(valorEmDolares, cartao);

            // 4. converte o retorno para o formato do cliente
            return new Recibo(true, "EXT-" + codigo);
        } catch (ChargeException e) {
            // 5. converte o modelo de erro: exceção checada vira recibo reprovado
            return new Recibo(false, "");
        }
    }
}

// Padrão Adapter - adaptador por HERANÇA (class adapter)
// Estende o adaptee e implementa a interface alvo ao mesmo tempo. Em Java só é possível quando o
// adaptee é uma classe não-final e sobra a única herança disponível - por isso é menos usado.
class AdaptadorPorHeranca extends GatewayTerceiroCartao implements ProcessadorPagamento {

    AdaptadorPorHeranca(String chaveApi) {
        super(chaveApi);
    }

    @Override
    public Recibo cobrar(int valorEmCentavos, String numeroCartao) {
        try {
            int codigo = executeCharge(valorEmCentavos / 100.0, new CardData(numeroCartao));
            return new Recibo(true, "EXT-" + codigo);
        } catch (ChargeException e) {
            return new Recibo(false, "");
        }
    }
}

// Classe Cliente
class SistemaCobranca {
    private final ProcessadorPagamento processador;

    // O cliente recebe a abstração e não faz ideia se por trás está o processador interno ou um
    // gateway de terceiro embrulhado num adaptador. Esse é o ganho do padrão.
    public SistemaCobranca(ProcessadorPagamento processador) {
        this.processador = processador;
    }

    public void cobrarAssinatura(String numeroCartao) {
        Recibo recibo = processador.cobrar(4990, numeroCartao);

        if (recibo.isAprovado()) {
            System.out.println("assinatura paga, recibo " + recibo.getIdentificador());
        } else {
            System.out.println("pagamento recusado");
        }
    }

    public static void main(String[] args) {
        new SistemaCobranca(new ProcessadorInterno())
                .cobrarAssinatura("4111111111111234");

        new SistemaCobranca(new AdaptadorGatewayTerceiro(new GatewayTerceiroCartao("chave-abc")))
                .cobrarAssinatura("5222222222225678");

        new SistemaCobranca(new AdaptadorPorHeranca("chave-xyz"))
                .cobrarAssinatura("6333333333339012");
    }
}

//Adapter x Facade: os dois embrulham código existente, mas com intenções diferentes. O Adapter
//converte para uma interface que JÁ EXISTE e que o cliente exige; a Facade inventa uma interface
//NOVA e mais simples para um subsistema inteiro.
//Adapter x Decorator: o Decorator preserva a interface e acrescenta comportamento; o Adapter troca
//a interface e não acrescenta comportamento nenhum.

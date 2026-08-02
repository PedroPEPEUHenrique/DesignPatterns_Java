//Fazer o sistema de cobrança aceitar um gateway de terceiro que você não pode alterar e cuja
//interface é incompatível: valor em reais como double, cartão em objeto próprio, retorno numérico
//e falha por exceção checada.
//O Adapter converte a interface de uma classe naquela que o cliente espera, sem alterar nenhuma
//das duas.

// Interface alvo: o que o nosso sistema já usa.
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

class ProcessadorInterno implements ProcessadorPagamento {

    @Override
    public Recibo cobrar(int valorEmCentavos, String numeroCartao) {
        System.out.println("[interno] cobrando " + valorEmCentavos + " centavos");
        return new Recibo(true, "INT-" + numeroCartao.substring(numeroCartao.length() - 4));
    }
}

// Adaptee: código de terceiro, fora do nosso controle.
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

// Adapter por COMPOSIÇÃO (object adapter)
// Forma preferida: funciona mesmo que o adaptee seja final e permite adaptar mais de um objeto.
class AdaptadorGatewayTerceiro implements ProcessadorPagamento {

    private final GatewayTerceiroCartao gateway;

    AdaptadorGatewayTerceiro(GatewayTerceiroCartao gateway) {
        this.gateway = gateway;
    }

    @Override
    public Recibo cobrar(int valorEmCentavos, String numeroCartao) {
        double valorEmDolares = valorEmCentavos / 100.0;
        CardData cartao = new CardData(numeroCartao);

        try {
            int codigo = gateway.executeCharge(valorEmDolares, cartao);
            return new Recibo(true, "EXT-" + codigo);
        } catch (ChargeException e) {
            // Converte o modelo de erro: exceção checada vira recibo reprovado.
            return new Recibo(false, "");
        }
    }
}

// Adapter por HERANÇA (class adapter)
// Só é possível quando o adaptee é uma classe não-final e sobra a única herança disponível.
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

// Cliente
class SistemaCobranca {
    private final ProcessadorPagamento processador;

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

//Adapter x Facade: o Adapter converte para uma interface que JÁ EXISTE e que o cliente exige; a
//Facade inventa uma interface NOVA e mais simples para um subsistema inteiro.
//Adapter x Decorator: o Decorator preserva a interface e acrescenta comportamento; o Adapter troca
//a interface e não acrescenta comportamento nenhum.

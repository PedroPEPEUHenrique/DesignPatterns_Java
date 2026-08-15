//Finalizar uma compra exige orquestrar estoque, precificação, antifraude, pagamento, nota fiscal,
//logística e e-mail, na ordem certa e com desfazimento quando um passo falha. Cada tela ou API que
//repetisse essa sequência duplicaria a ordem dos passos, o estorno e a liberação da reserva.
//O Facade fornece uma interface ÚNICA e de mais alto nível para um conjunto de interfaces de um
//subsistema, tornando-o mais fácil de usar. Ele não veda o subsistema: quem precisar de um caso
//particular continua podendo chamar os serviços diretamente.

import java.util.List;

class ServicoEstoque {

    boolean reservar(List<String> skus) {
        System.out.println("[estoque] reservando " + skus);
        return !skus.contains("ESGOTADO");
    }

    void liberarReserva(List<String> skus) {
        System.out.println("[estoque] liberando reserva de " + skus);
    }
}

class ServicoPrecificacao {

    int calcularTotalEmCentavos(List<String> skus, String cupom) {
        int total = skus.size() * 5000;
        if ("PRIMEIRACOMPRA".equals(cupom)) {
            total = (int) (total * 0.9);
        }
        System.out.println("[preço] total calculado: " + total + " centavos");
        return total;
    }
}

class ServicoAntifraude {

    boolean aprovar(String cpf, int valorEmCentavos) {
        boolean aprovado = valorEmCentavos < 1000000;
        System.out.println("[antifraude] análise para " + cpf + ": "
                           + (aprovado ? "aprovada" : "recusada"));
        return aprovado;
    }
}

class ServicoPagamento {

    String cobrar(String cartao, int valorEmCentavos) {
        System.out.println("[pagamento] cobrando " + valorEmCentavos + " centavos");
        return "PAG-" + cartao.substring(cartao.length() - 4);
    }

    void estornar(String idPagamento) {
        System.out.println("[pagamento] estornando " + idPagamento);
    }
}

class ServicoNotaFiscal {

    String emitir(String cpf, int valorEmCentavos) {
        System.out.println("[fiscal] emitindo NF-e para " + cpf);
        return "NFE-" + System.nanoTime() % 100000;
    }
}

class ServicoLogistica {

    String agendarEntrega(String endereco, List<String> skus) {
        System.out.println("[logística] agendando entrega em " + endereco);
        return "RASTREIO-" + Math.abs(endereco.hashCode() % 10000);
    }
}

class ServicoEmail {

    void enviarConfirmacao(String email, String rastreio) {
        System.out.println("[email] confirmação para " + email + ", rastreio " + rastreio);
    }
}

class ResultadoCompra {
    private final boolean concluida;
    private final String motivoRecusa;
    private final String notaFiscal;
    private final String rastreio;

    private ResultadoCompra(boolean concluida, String motivoRecusa, String notaFiscal, String rastreio) {
        this.concluida = concluida;
        this.motivoRecusa = motivoRecusa;
        this.notaFiscal = notaFiscal;
        this.rastreio = rastreio;
    }

    static ResultadoCompra sucesso(String notaFiscal, String rastreio) {
        return new ResultadoCompra(true, "", notaFiscal, rastreio);
    }

    static ResultadoCompra recusa(String motivo) {
        return new ResultadoCompra(false, motivo, "", "");
    }

    boolean isConcluida() {
        return concluida;
    }

    String getMotivoRecusa() {
        return motivoRecusa;
    }

    String getNotaFiscal() {
        return notaFiscal;
    }

    String getRastreio() {
        return rastreio;
    }
}

class CheckoutFacade {

    private final ServicoEstoque estoque = new ServicoEstoque();
    private final ServicoPrecificacao precificacao = new ServicoPrecificacao();
    private final ServicoAntifraude antifraude = new ServicoAntifraude();
    private final ServicoPagamento pagamento = new ServicoPagamento();
    private final ServicoNotaFiscal notaFiscal = new ServicoNotaFiscal();
    private final ServicoLogistica logistica = new ServicoLogistica();
    private final ServicoEmail email = new ServicoEmail();

    public ResultadoCompra finalizarCompra(String cpf, String emailCliente, String cartao,
                                           String endereco, List<String> skus, String cupom) {

        if (!estoque.reservar(skus)) {
            return ResultadoCompra.recusa("item sem estoque");
        }

        int total = precificacao.calcularTotalEmCentavos(skus, cupom);

        if (!antifraude.aprovar(cpf, total)) {
            estoque.liberarReserva(skus);
            return ResultadoCompra.recusa("reprovado no antifraude");
        }

        String idPagamento = pagamento.cobrar(cartao, total);

        try {
            String nfe = notaFiscal.emitir(cpf, total);
            String rastreio = logistica.agendarEntrega(endereco, skus);
            email.enviarConfirmacao(emailCliente, rastreio);
            return ResultadoCompra.sucesso(nfe, rastreio);
        } catch (RuntimeException e) {
            pagamento.estornar(idPagamento);
            estoque.liberarReserva(skus);
            return ResultadoCompra.recusa("falha ao concluir: " + e.getMessage());
        }
    }
}

class LojaVirtual {

    private final CheckoutFacade checkout = new CheckoutFacade();

    public void comprar() {
        ResultadoCompra resultado = checkout.finalizarCompra(
                "111.222.333-44",
                "ana@exemplo.com",
                "4111111111111234",
                "Rua A, 100",
                List.of("TEC-001", "MOU-002"),
                "PRIMEIRACOMPRA");

        if (resultado.isConcluida()) {
            System.out.println("compra concluída, NF " + resultado.getNotaFiscal()
                               + ", rastreio " + resultado.getRastreio());
        } else {
            System.out.println("compra não concluída: " + resultado.getMotivoRecusa());
        }
    }

    public static void main(String[] args) {
        new LojaVirtual().comprar();

        System.out.println("---");

        ResultadoCompra semEstoque = new CheckoutFacade().finalizarCompra(
                "555.666.777-88", "bruno@exemplo.com", "5222222222225678",
                "Rua B, 200", List.of("ESGOTADO"), "");

        System.out.println("resultado: " + semEstoque.getMotivoRecusa());
    }
}

//Suponha que a sua tarefa seja implementar a finalização de compra de uma loja virtual. O
//subsistema envolvido é grande: estoque, precificação, antifraude, pagamento, nota fiscal,
//logística e notificação. Cada um tem a sua própria classe, com regras de ordem e de erro.

//Imagine o controlador da tela de checkout chamando os sete serviços na ordem certa, tratando
//falha de cada um e desfazendo o que já foi feito. Esse código apareceria repetido no checkout do
//site, no do aplicativo e no da API - e qualquer mudança na ordem exigiria alterar os três.

//A Facade resolve o problema de fornecer uma interface UNIFICADA e de alto nível para um conjunto
//de interfaces de um subsistema, tornando-o mais fácil de usar.

import java.util.List;

// SUBSISTEMA - classes de baixo nível, cada uma com a sua responsabilidade.
// Elas continuam públicas e utilizáveis diretamente: a fachada não as esconde nem as substitui,
// só oferece um caminho mais curto para o caso de uso comum.

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

// Resultado de alto nível devolvido pela fachada. Repare que o cliente recebe só o que interessa
// ao caso de uso, não os objetos internos de cada serviço.
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

// Padrão Facade
// Ela não implementa regra de negócio nova: apenas conhece a ORDEM das chamadas, o tratamento de
// falha e a compensação. Toda a inteligência continua nos serviços do subsistema.
class CheckoutFacade {

    private final ServicoEstoque estoque = new ServicoEstoque();
    private final ServicoPrecificacao precificacao = new ServicoPrecificacao();
    private final ServicoAntifraude antifraude = new ServicoAntifraude();
    private final ServicoPagamento pagamento = new ServicoPagamento();
    private final ServicoNotaFiscal notaFiscal = new ServicoNotaFiscal();
    private final ServicoLogistica logistica = new ServicoLogistica();
    private final ServicoEmail email = new ServicoEmail();

    // Uma única operação de alto nível no lugar de sete chamadas coordenadas.
    public ResultadoCompra finalizarCompra(String cpf, String emailCliente, String cartao,
                                           String endereco, List<String> skus, String cupom) {

        if (!estoque.reservar(skus)) {
            return ResultadoCompra.recusa("item sem estoque");
        }

        int total = precificacao.calcularTotalEmCentavos(skus, cupom);

        if (!antifraude.aprovar(cpf, total)) {
            estoque.liberarReserva(skus);                 // compensação
            return ResultadoCompra.recusa("reprovado no antifraude");
        }

        String idPagamento = pagamento.cobrar(cartao, total);

        try {
            String nfe = notaFiscal.emitir(cpf, total);
            String rastreio = logistica.agendarEntrega(endereco, skus);
            email.enviarConfirmacao(emailCliente, rastreio);
            return ResultadoCompra.sucesso(nfe, rastreio);
        } catch (RuntimeException e) {
            // Se algo falhar depois da cobrança, é a fachada que sabe desfazer.
            pagamento.estornar(idPagamento);
            estoque.liberarReserva(skus);
            return ResultadoCompra.recusa("falha ao concluir: " + e.getMessage());
        }
    }
}

// Classe Cliente
class LojaVirtual {

    private final CheckoutFacade checkout = new CheckoutFacade();

    public void comprar() {
        // O cliente conhece UMA classe e UMA operação. Não precisa saber que existe antifraude,
        // nem em que ordem chamar, nem o que estornar quando dá errado.
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

//Pontos de atenção:
//A Facade não pode virar um "God Object". Se ela começa a decidir regra de negócio em vez de
//apenas orquestrar, o subsistema está com responsabilidades no lugar errado.
//A Facade não proíbe o acesso direto ao subsistema - quem precisa de um caso de uso incomum
//continua podendo chamar os serviços um a um. Ela é uma conveniência, não uma barreira.
//No GRASP, essa mesma ideia aparece como Controller e como Indirection: um objeto intermediário
//que reduz o acoplamento entre o cliente e muitas classes internas.

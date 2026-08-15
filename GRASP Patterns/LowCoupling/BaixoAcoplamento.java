//GRASP - LOW COUPLING
//Problema: como reduzir o impacto de uma mudança e favorecer o reúso?
//Solução: atribuir responsabilidades de forma que o ACOPLAMENTO - o quanto uma classe depende de
//outras - permaneça baixo.

import java.util.ArrayList;
import java.util.List;

class Pedido {
    private final String codigo;
    private final String cpfCliente;
    private final int totalEmCentavos;

    Pedido(String codigo, String cpfCliente, int totalEmCentavos) {
        this.codigo = codigo;
        this.cpfCliente = cpfCliente;
        this.totalEmCentavos = totalEmCentavos;
    }

    String getCodigo() {
        return codigo;
    }

    String getCpfCliente() {
        return cpfCliente;
    }

    int getTotalEmCentavos() {
        return totalEmCentavos;
    }
}

class FaturamentoAcoplado {

    public void faturar(Pedido pedido) {
        ClienteHttpSefaz sefaz = new ClienteHttpSefaz("https://sefaz.exemplo.gov.br", 3000);
        String chaveNfe = sefaz.transmitir(pedido.getCpfCliente(), pedido.getTotalEmCentavos());

        GeradorPdfItext pdf = new GeradorPdfItext();
        String arquivo = pdf.gerar(chaveNfe);

        BancoMySql banco = new BancoMySql();
        banco.executar("INSERT INTO nota_fiscal (pedido, chave) VALUES ('"
                       + pedido.getCodigo() + "','" + chaveNfe + "')");

        new ServidorSmtp("smtp.exemplo.com").enviar(pedido.getCpfCliente(), arquivo);
    }
}

class ClienteHttpSefaz {
    ClienteHttpSefaz(String url, int timeout) {
    }

    String transmitir(String cpf, int valor) {
        return "NFE-" + Math.abs((cpf + valor).hashCode());
    }
}

class GeradorPdfItext {
    String gerar(String chave) {
        return chave + ".pdf";
    }
}

class BancoMySql {
    void executar(String sql) {
    }
}

class ServidorSmtp {
    ServidorSmtp(String host) {
    }

    void enviar(String destinatario, String anexo) {
    }
}

interface TransmissorFiscal {
    String transmitir(String cpf, int valorEmCentavos);
}

interface GeradorDocumento {
    String gerar(String chaveNfe);
}

interface RepositorioNotaFiscal {
    void salvar(String codigoPedido, String chaveNfe);
}

interface Notificador {
    void notificar(String destinatario, String documento);
}

class Faturamento {

    private final TransmissorFiscal transmissor;
    private final GeradorDocumento gerador;
    private final RepositorioNotaFiscal repositorio;
    private final Notificador notificador;

    Faturamento(TransmissorFiscal transmissor, GeradorDocumento gerador,
                RepositorioNotaFiscal repositorio, Notificador notificador) {
        this.transmissor = transmissor;
        this.gerador = gerador;
        this.repositorio = repositorio;
        this.notificador = notificador;
    }

    public String faturar(Pedido pedido) {
        String chaveNfe = transmissor.transmitir(pedido.getCpfCliente(), pedido.getTotalEmCentavos());
        String documento = gerador.gerar(chaveNfe);

        repositorio.salvar(pedido.getCodigo(), chaveNfe);
        notificador.notificar(pedido.getCpfCliente(), documento);

        return chaveNfe;
    }
}

class TransmissorSefaz implements TransmissorFiscal {

    @Override
    public String transmitir(String cpf, int valorEmCentavos) {
        System.out.println("  [sefaz] transmitindo nota de " + valorEmCentavos + " centavos");
        return "NFE-" + Math.abs((cpf + valorEmCentavos).hashCode() % 100000);
    }
}

class GeradorPdf implements GeradorDocumento {

    @Override
    public String gerar(String chaveNfe) {
        System.out.println("  [pdf] gerando " + chaveNfe + ".pdf");
        return chaveNfe + ".pdf";
    }
}

class RepositorioNotaFiscalBanco implements RepositorioNotaFiscal {

    @Override
    public void salvar(String codigoPedido, String chaveNfe) {
        System.out.println("  [banco] gravando " + chaveNfe + " para o pedido " + codigoPedido);
    }
}

class NotificadorEmail implements Notificador {

    @Override
    public void notificar(String destinatario, String documento) {
        System.out.println("  [email] enviando " + documento + " para " + destinatario);
    }
}

class TransmissorFalso implements TransmissorFiscal {

    @Override
    public String transmitir(String cpf, int valorEmCentavos) {
        return "NFE-TESTE";
    }
}

class RepositorioEmMemoria implements RepositorioNotaFiscal {

    private final List<String> gravados = new ArrayList<>();

    @Override
    public void salvar(String codigoPedido, String chaveNfe) {
        gravados.add(codigoPedido + "=" + chaveNfe);
    }

    List<String> getGravados() {
        return gravados;
    }
}

class BaixoAcoplamento {

    public static void main(String[] args) {
        Pedido pedido = new Pedido("PED-1", "111.222.333-44", 330000);

        System.out.println("== produção ==");
        Faturamento producao = new Faturamento(new TransmissorSefaz(), new GeradorPdf(),
                                               new RepositorioNotaFiscalBanco(), new NotificadorEmail());
        System.out.println("  chave: " + producao.faturar(pedido));

        System.out.println("== teste, sem rede e sem banco ==");
        RepositorioEmMemoria repositorio = new RepositorioEmMemoria();
        Faturamento emTeste = new Faturamento(new TransmissorFalso(),
                                              chave -> chave + ".txt",
                                              repositorio,
                                              (destinatario, documento) -> { });
        String chave = emTeste.faturar(pedido);

        System.out.println("  chave: " + chave);
        System.out.println("  gravado: " + repositorio.getGravados());
        System.out.println("  a MESMA classe Faturamento rodou nos dois cenários");
    }
}

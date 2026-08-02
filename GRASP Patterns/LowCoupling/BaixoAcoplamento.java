//GRASP - LOW COUPLING (Baixo Acoplamento)

//Problema: como reduzir o impacto de uma mudança e favorecer o reúso?
//Solução: atribua responsabilidades de forma que o ACOPLAMENTO - o quanto uma classe conhece,
//depende de ou está ligada a outras - permaneça baixo.

//Suponha que a sua tarefa seja emitir a nota fiscal de um pedido aprovado.
//Imagine a solução em que o serviço de faturamento cria por dentro o cliente HTTP da SEFAZ,
//instancia o gerador de PDF, abre a conexão com o banco e chama o servidor SMTP. Essa classe fica
//impossível de testar sem rede e sem banco, não roda em outro contexto e quebra sempre que
//qualquer um desses quatro detalhes mudar.

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

// COMO NÃO FAZER - alto acoplamento
// Esta classe depende de QUATRO implementações concretas, e ainda as CRIA por dentro.
// Duas consequências práticas: não há como substituir nenhuma delas em teste, e a classe precisa
// ser recompilada e reavaliada sempre que qualquer uma das quatro mudar.
class FaturamentoAcoplado {

    public void faturar(Pedido pedido) {
        // acoplamento 1: sabe qual cliente HTTP usar e como configurá-lo
        ClienteHttpSefaz sefaz = new ClienteHttpSefaz("https://sefaz.exemplo.gov.br", 3000);
        String chaveNfe = sefaz.transmitir(pedido.getCpfCliente(), pedido.getTotalEmCentavos());

        // acoplamento 2: sabe qual biblioteca de PDF usar
        GeradorPdfItext pdf = new GeradorPdfItext();
        String arquivo = pdf.gerar(chaveNfe);

        // acoplamento 3: sabe SQL e a estrutura da tabela
        BancoMySql banco = new BancoMySql();
        banco.executar("INSERT INTO nota_fiscal (pedido, chave) VALUES ('"
                       + pedido.getCodigo() + "','" + chaveNfe + "')");

        // acoplamento 4: sabe do servidor de e-mail
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

// COMO FAZER - baixo acoplamento
// A classe passa a depender de ABSTRAÇÕES e a RECEBER as dependências, em vez de criá-las.

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

// Agora esta classe conhece apenas quatro interfaces - e nenhuma tecnologia. Ela não sabe se a
// SEFAZ é HTTP ou fila, se o banco é MySQL ou memória, se a notificação é e-mail ou push.
// Trocar qualquer uma dessas decisões não faz esta classe ser sequer recompilada.
class Faturamento {

    private final TransmissorFiscal transmissor;
    private final GeradorDocumento gerador;
    private final RepositorioNotaFiscal repositorio;
    private final Notificador notificador;

    // Injeção de dependência: quem monta o objeto decide as implementações, e essa decisão fica
    // concentrada em um ponto só do sistema.
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

// IMPLEMENTAÇÕES DE PRODUÇÃO
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

// IMPLEMENTAÇÕES DE TESTE (dublês)
// Só existem porque a classe depende de abstrações. Com o desenho acoplado, escrever este teste
// exigiria rede, banco e servidor SMTP no ar.
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

// Classe Cliente
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

//Formas de acoplamento que contam:
//A tem um atributo do tipo B; A chama um método de B; A recebe ou devolve B; A herda de B.
//A herança é a forma MAIS forte de acoplamento - a subclasse depende até de detalhes protegidos
//da superclasse. É a razão do conselho "prefira composição a herança".
//
//Acoplamento não é para ser eliminado, e sim controlado. Zero acoplamento significa objetos que
//não colaboram, ou seja, nenhum sistema. Depender de classes estáveis (String, List, interfaces
//do próprio domínio) é barato; depender de classes voláteis e concretas é o que custa caro.
//
//Este é um princípio AVALIATIVO: não existe métrica que diga "acoplado demais". Ele é usado para
//comparar dois desenhos possíveis e escolher o de menor dependência.

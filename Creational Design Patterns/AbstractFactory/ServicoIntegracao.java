//Um serviço de integração recebe mensagens de vários parceiros, cada um com o seu formato de
//serialização (XML, JSON). Para cada origem existe uma FAMÍLIA inteira de decodificadores - um por
//tipo de mensagem - que precisam ser usados de forma coerente: não faz sentido decodificar o
//cliente com o decodificador XML e a conta com o de JSON.
//O Abstract Factory fornece uma interface para criar FAMÍLIAS de objetos relacionados sem
//especificar as suas classes concretas. Essa garantia de coerência da família é o que o distingue
//do Factory Method, que produz um único produto.

class MsgRegistrarCliente {
    private final String nome;
    private final String cpf;

    MsgRegistrarCliente(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    String getNome() {
        return nome;
    }

    String getCpf() {
        return cpf;
    }
}

class MsgRegistrarConta {
    private final String agencia;
    private final String numero;

    MsgRegistrarConta(String agencia, String numero) {
        this.agencia = agencia;
        this.numero = numero;
    }

    String getAgencia() {
        return agencia;
    }

    String getNumero() {
        return numero;
    }
}

interface RegistrarClienteDecoder {
    MsgRegistrarCliente decode(String textoMsg);
}

interface RegistrarContaDecoder {
    MsgRegistrarConta decode(String textoMsg);
}

abstract class DecoderFactory {

    abstract RegistrarClienteDecoder createRegistrarClienteDecoder();

    abstract RegistrarContaDecoder createRegistrarContaDecoder();

    static DecoderFactory fabricaParaOrigem(String origem) {
        switch (origem.toUpperCase()) {
            case "XML":
                return new XmlDecoderFactory();
            case "JSON":
                return new JsonDecoderFactory();
            default:
                throw new IllegalArgumentException("Origem sem decoders: " + origem);
        }
    }
}

class XmlDecoderFactory extends DecoderFactory {

    @Override
    RegistrarClienteDecoder createRegistrarClienteDecoder() {
        return new XmlRegistrarClienteDecoder();
    }

    @Override
    RegistrarContaDecoder createRegistrarContaDecoder() {
        return new XmlRegistrarContaDecoder();
    }
}

class XmlRegistrarClienteDecoder implements RegistrarClienteDecoder {

    @Override
    public MsgRegistrarCliente decode(String textoMsg) {

        return new MsgRegistrarCliente(extrairTag(textoMsg, "nome"),
                                       extrairTag(textoMsg, "cpf"));
    }

    private String extrairTag(String xml, String tag) {
        String abertura = "<" + tag + ">";
        String fechamento = "</" + tag + ">";
        int inicio = xml.indexOf(abertura);
        int fim = xml.indexOf(fechamento);
        if (inicio < 0 || fim < 0) {
            return "";
        }
        return xml.substring(inicio + abertura.length(), fim);
    }
}

class XmlRegistrarContaDecoder implements RegistrarContaDecoder {

    @Override
    public MsgRegistrarConta decode(String textoMsg) {
        return new MsgRegistrarConta(extrairTag(textoMsg, "agencia"),
                                     extrairTag(textoMsg, "numero"));
    }

    private String extrairTag(String xml, String tag) {
        String abertura = "<" + tag + ">";
        String fechamento = "</" + tag + ">";
        int inicio = xml.indexOf(abertura);
        int fim = xml.indexOf(fechamento);
        if (inicio < 0 || fim < 0) {
            return "";
        }
        return xml.substring(inicio + abertura.length(), fim);
    }
}

class JsonDecoderFactory extends DecoderFactory {

    @Override
    RegistrarClienteDecoder createRegistrarClienteDecoder() {
        return new JsonRegistrarClienteDecoder();
    }

    @Override
    RegistrarContaDecoder createRegistrarContaDecoder() {
        return new JsonRegistrarContaDecoder();
    }
}

class JsonRegistrarClienteDecoder implements RegistrarClienteDecoder {

    @Override
    public MsgRegistrarCliente decode(String textoMsg) {
        return new MsgRegistrarCliente(extrairCampo(textoMsg, "nome"),
                                       extrairCampo(textoMsg, "cpf"));
    }

    private String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int inicio = json.indexOf(chave);
        if (inicio < 0) {
            return "";
        }
        int aspaValor = json.indexOf('"', json.indexOf(':', inicio) + 1);
        int fimValor = json.indexOf('"', aspaValor + 1);
        if (aspaValor < 0 || fimValor < 0) {
            return "";
        }
        return json.substring(aspaValor + 1, fimValor);
    }
}

class JsonRegistrarContaDecoder implements RegistrarContaDecoder {

    @Override
    public MsgRegistrarConta decode(String textoMsg) {
        return new MsgRegistrarConta(extrairCampo(textoMsg, "agencia"),
                                     extrairCampo(textoMsg, "numero"));
    }

    private String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int inicio = json.indexOf(chave);
        if (inicio < 0) {
            return "";
        }
        int aspaValor = json.indexOf('"', json.indexOf(':', inicio) + 1);
        int fimValor = json.indexOf('"', aspaValor + 1);
        if (aspaValor < 0 || fimValor < 0) {
            return "";
        }
        return json.substring(aspaValor + 1, fimValor);
    }
}

public class ServicoIntegracao {

    public void registrarCliente(String textoMsg, String origem) {

        DecoderFactory decoderFactory = DecoderFactory.fabricaParaOrigem(origem);

        RegistrarClienteDecoder msgDecoder = decoderFactory.createRegistrarClienteDecoder();

        MsgRegistrarCliente msg = msgDecoder.decode(textoMsg);

        System.out.println("Cliente registrado: " + msg.getNome() + " / " + msg.getCpf());
    }

    public void registrarConta(String textoMsg, String origem) {
        DecoderFactory decoderFactory = DecoderFactory.fabricaParaOrigem(origem);
        RegistrarContaDecoder msgDecoder = decoderFactory.createRegistrarContaDecoder();
        MsgRegistrarConta msg = msgDecoder.decode(textoMsg);

        System.out.println("Conta registrada: " + msg.getAgencia() + " / " + msg.getNumero());
    }

    public static void main(String[] args) {
        ServicoIntegracao servico = new ServicoIntegracao();

        servico.registrarCliente("<cliente><nome>Ana</nome><cpf>111</cpf></cliente>", "XML");
        servico.registrarCliente("{\"nome\": \"Bruno\", \"cpf\": \"222\"}", "JSON");

        servico.registrarConta("<conta><agencia>0001</agencia><numero>12345</numero></conta>", "XML");
        servico.registrarConta("{\"agencia\": \"0002\", \"numero\": \"67890\"}", "JSON");
    }
}

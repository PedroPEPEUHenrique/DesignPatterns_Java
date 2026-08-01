// ============================================================================
// ABSTRACT FACTORY (GoF - Criacional)
//
// Intencao: fornecer uma interface para criar FAMILIAS de objetos relacionados
// sem especificar suas classes concretas.
//
// Cenario: um servico recebe mensagens de varias origens (parceiros), cada uma
// com seu proprio formato de serializacao (XML, JSON, ...). Para cada origem
// existe uma familia inteira de decoders - um por tipo de mensagem - e eles
// precisam ser usados de forma COERENTE: nao faz sentido decodificar o cliente
// com o decoder XML e a conta com o decoder JSON.
//
// Essa garantia de coerencia da familia e o que distingue o Abstract Factory
// do Factory Method, que produz um unico produto.
// ============================================================================


// ---------------------------------------------------------------------------
// PRODUTOS FINAIS - as mensagens ja decodificadas.
// Note que sao independentes do formato de origem: e justamente esse o ganho,
// o restante do sistema trabalha so com elas.
// ---------------------------------------------------------------------------

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


// ---------------------------------------------------------------------------
// PRODUTOS ABSTRATOS - um tipo de decoder para cada tipo de mensagem.
// Sao esses os "produtos" que a fabrica abstrata sabe criar.
// ---------------------------------------------------------------------------

interface RegistrarClienteDecoder {
    MsgRegistrarCliente decode(String textoMsg);
}

interface RegistrarContaDecoder {
    MsgRegistrarConta decode(String textoMsg);
}


// ---------------------------------------------------------------------------
// FABRICA ABSTRATA
//
// Declara uma operacao de criacao para CADA produto da familia. Acrescentar um
// novo tipo de mensagem significa acrescentar um metodo aqui - e essa e a
// principal desvantagem do padrao: a interface da fabrica nao e aberta a
// extensao, todas as fabricas concretas precisam ser alteradas junto.
// ---------------------------------------------------------------------------

abstract class DecoderFactory {

    abstract RegistrarClienteDecoder createRegistrarClienteDecoder();

    abstract RegistrarContaDecoder createRegistrarContaDecoder();

    // Ponto unico do sistema que conhece as fabricas concretas. E aqui que a
    // dependencia fica confinada; num sistema real isso viria de configuracao,
    // de um Map registrado na inicializacao ou de um ServiceLoader, e nao de um
    // switch como este.
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


// ---------------------------------------------------------------------------
// FAMILIA 1 - origem XML (parceiro legado)
// ---------------------------------------------------------------------------

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
        // Parsing real de XML omitido - o que importa no padrao e que este
        // decoder so existe para a familia XML.
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


// ---------------------------------------------------------------------------
// FAMILIA 2 - origem JSON (parceiro novo)
//
// Acrescentar uma origem inteira nao exige tocar em ServicoIntegracao: basta
// uma nova fabrica concreta + seus decoders. Esse e o eixo em que o padrao e
// aberto a extensao.
// ---------------------------------------------------------------------------

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


// ---------------------------------------------------------------------------
// CLIENTE
//
// Codigo original do exemplo, agora compilavel. Repare no que ele NAO tem:
// nenhuma mencao a XML ou JSON, nenhum `new` de decoder, nenhum if por origem.
// Ele depende so das abstracoes DecoderFactory / *Decoder / Msg*.
// ---------------------------------------------------------------------------

public class ServicoIntegracao {

    public void registrarCliente(String textoMsg, String origem) {
        // 1. obtem a familia de decoders coerente com a origem
        DecoderFactory decoderFactory = DecoderFactory.fabricaParaOrigem(origem);

        // 2. pede a fabrica o produto especifico desta operacao
        RegistrarClienteDecoder msgDecoder = decoderFactory.createRegistrarClienteDecoder();

        // 3. daqui pra frente o formato de origem ja nao existe mais
        MsgRegistrarCliente msg = msgDecoder.decode(textoMsg);

        // codigo para tratamento da mensagem MsgRegistrarCliente
        System.out.println("Cliente registrado: " + msg.getNome() + " / " + msg.getCpf());
    }

    public void registrarConta(String textoMsg, String origem) {
        DecoderFactory decoderFactory = DecoderFactory.fabricaParaOrigem(origem);
        RegistrarContaDecoder msgDecoder = decoderFactory.createRegistrarContaDecoder();
        MsgRegistrarConta msg = msgDecoder.decode(textoMsg);

        // codigo para tratamento da mensagem MsgRegistrarConta
        System.out.println("Conta registrada: " + msg.getAgencia() + " / " + msg.getNumero());
    }

    // codigo para demais mensagens

    public static void main(String[] args) {
        ServicoIntegracao servico = new ServicoIntegracao();

        servico.registrarCliente("<cliente><nome>Ana</nome><cpf>111</cpf></cliente>", "XML");
        servico.registrarCliente("{\"nome\": \"Bruno\", \"cpf\": \"222\"}", "JSON");

        servico.registrarConta("<conta><agencia>0001</agencia><numero>12345</numero></conta>", "XML");
        servico.registrarConta("{\"agencia\": \"0002\", \"numero\": \"67890\"}", "JSON");
    }
}

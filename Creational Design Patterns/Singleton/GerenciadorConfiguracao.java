//Dar acesso, em todo o sistema, a parâmetros de configuração lidos uma única vez na subida da
//aplicação. A leitura da origem é cara e os parâmetros não mudam em tempo de execução.
//O Singleton garante que uma classe tenha UMA ÚNICA instância e fornece um ponto global de
//acesso a ela.

import java.util.HashMap;
import java.util.Map;

class ConfiguracaoInsegura {
    private static ConfiguracaoInsegura instancia;

    private ConfiguracaoInsegura() { }

    public static ConfiguracaoInsegura getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracaoInsegura();
        }
        return instancia;
    }
}

class ConfiguracaoEager {
    private static final ConfiguracaoEager INSTANCIA = new ConfiguracaoEager();

    private ConfiguracaoEager() { }

    public static ConfiguracaoEager getInstancia() {
        return INSTANCIA;
    }
}

class GerenciadorConfiguracao {

    private GerenciadorConfiguracao() {
        carregarDaOrigem();
    }

    private static class Holder {
        private static final GerenciadorConfiguracao INSTANCIA = new GerenciadorConfiguracao();
    }

    public static GerenciadorConfiguracao getInstancia() {
        return Holder.INSTANCIA;
    }

    private final Map<String, String> parametros = new HashMap<>();

    private void carregarDaOrigem() {
        System.out.println("[lendo configuração da origem... isso deve aparecer UMA vez]");
        parametros.put("ambiente", "producao");
        parametros.put("timeoutEmMs", "3000");
        parametros.put("taxaJurosEmPontosBase", "150");
    }

    public String get(String chave) {
        return parametros.getOrDefault(chave, "");
    }

    public int getInt(String chave) {
        String valor = get(chave);
        return valor.isEmpty() ? 0 : Integer.parseInt(valor);
    }
}

enum ConfiguracaoEnum {
    INSTANCIA;

    private final Map<String, String> parametros = new HashMap<>();

    ConfiguracaoEnum() {
        parametros.put("ambiente", "producao");
    }

    public String get(String chave) {
        return parametros.getOrDefault(chave, "");
    }
}

class ServicoIntegracaoExterna {

    public void chamar() {
        GerenciadorConfiguracao config = GerenciadorConfiguracao.getInstancia();

        System.out.println("chamando integração com timeout de " + config.getInt("timeoutEmMs") + "ms");
    }
}

class DemoSingleton {

    public static void main(String[] args) {
        new ServicoIntegracaoExterna().chamar();
        new ServicoIntegracaoExterna().chamar();

        GerenciadorConfiguracao a = GerenciadorConfiguracao.getInstancia();
        GerenciadorConfiguracao b = GerenciadorConfiguracao.getInstancia();

        System.out.println("mesma instância? " + (a == b));
        System.out.println("ambiente = " + ConfiguracaoEnum.INSTANCIA.get("ambiente"));
    }
}

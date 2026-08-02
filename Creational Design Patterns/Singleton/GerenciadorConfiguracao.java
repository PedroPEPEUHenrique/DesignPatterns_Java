//Suponha que a sua tarefa seja dar acesso, a todo o sistema, a um conjunto de parâmetros de
//configuração lidos uma única vez na subida da aplicação. Um parâmetro possui:
//Chave
//Valor
//Origem (arquivo, variável de ambiente, ...)

//Imagine que a leitura da origem é cara e que os parâmetros não mudam em tempo de execução.
//Se cada classe cliente criar a sua própria instância do gerenciador, cada uma paga o custo da
//leitura de novo e - pior - duas classes podem enxergar valores diferentes.

//O Singleton resolve o problema de garantir que uma classe tenha UMA ÚNICA instância e de fornecer
//um ponto global de acesso a ela.

import java.util.HashMap;
import java.util.Map;

// Padrão Singleton - versão ingênua (NÃO usar)
// Serve só para mostrar o erro clássico: dois threads podem passar pelo if ao mesmo tempo,
// e o sistema termina com duas instâncias - exatamente o que o padrão deveria impedir.
class ConfiguracaoInsegura {
    private static ConfiguracaoInsegura instancia;

    private ConfiguracaoInsegura() { }

    public static ConfiguracaoInsegura getInstancia() {
        if (instancia == null) {                    // thread A e thread B podem entrar juntas aqui
            instancia = new ConfiguracaoInsegura();
        }
        return instancia;
    }
}

// Padrão Singleton - inicialização adiantada (eager)
// A JVM garante que o inicializador estático roda uma única vez, de forma thread-safe.
// Custo: a instância é criada mesmo que ninguém use.
class ConfiguracaoEager {
    private static final ConfiguracaoEager INSTANCIA = new ConfiguracaoEager();

    private ConfiguracaoEager() { }

    public static ConfiguracaoEager getInstancia() {
        return INSTANCIA;
    }
}

// Padrão Singleton - inicialização sob demanda (lazy holder)
// A classe interna só é carregada na primeira chamada a getInstancia(). Junta o melhor dos dois
// mundos: preguiçoso e thread-safe sem synchronized, porque quem garante é o class loader.
class GerenciadorConfiguracao {

    // O construtor é privado: essa é a parte do padrão que realmente impede o "new" no cliente.
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
        // Leitura cara simulada: em um sistema real seria arquivo, banco ou variável de ambiente.
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

// Padrão Singleton - via enum
// É a forma mais robusta em Java: a JVM garante instância única inclusive contra reflexão e
// serialização, que quebram as versões acima. A limitação é não poder estender outra classe.
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

// Classe Cliente
class ServicoIntegracaoExterna {

    public void chamar() {
        // O cliente não instancia nada: pede a instância ao próprio tipo.
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

        // Mesma referência: é esse o contrato do padrão.
        System.out.println("mesma instância? " + (a == b));
        System.out.println("ambiente = " + ConfiguracaoEnum.INSTANCIA.get("ambiente"));
    }
}

//Cuidado: Singleton é o padrão mais criticado do catálogo. Ele introduz estado global e uma
//dependência escondida - quem chama getInstancia() não declara essa dependência na sua interface,
//o que dificulta substituir a configuração em teste. Em sistemas com injeção de dependência
//(CDI, Spring), o escopo singleton do contêiner resolve o mesmo problema sem estado global.

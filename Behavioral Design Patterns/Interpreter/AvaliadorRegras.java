//Suponha que a sua tarefa seja permitir que a área de negócio configure as regras de desconto sem
//pedir alteração de código. As regras são expressões como:
//  valorPedido > 10000 E clienteVip
//  primeiraCompra OU (valorPedido > 5000 E NAO clienteInadimplente)

//Imagine cada regra virando um if dentro do serviço de pedidos. Uma regra nova é um deploy novo,
//e a lógica combinada fica espalhada em condicionais que ninguém consegue mais ler.

//O Interpreter resolve o problema de, dada uma linguagem, definir uma representação para a sua
//gramática junto com um interpretador que usa essa representação para interpretar sentenças da
//linguagem. Cada regra da gramática vira uma CLASSE, e uma expressão vira uma ÁRVORE de objetos.

import java.util.HashMap;
import java.util.Map;

// O CONTEXTO - guarda o estado sobre o qual a expressão é avaliada. É passado a toda a árvore.
class ContextoRegra {
    private final Map<String, Boolean> fatos = new HashMap<>();
    private final Map<String, Integer> numeros = new HashMap<>();

    ContextoRegra defineFato(String nome, boolean valor) {
        fatos.put(nome, valor);
        return this;
    }

    ContextoRegra defineNumero(String nome, int valor) {
        numeros.put(nome, valor);
        return this;
    }

    boolean fato(String nome) {
        return fatos.getOrDefault(nome, false);
    }

    int numero(String nome) {
        return numeros.getOrDefault(nome, 0);
    }
}

// Padrão Interpreter - a AbstractExpression
// Uma única operação: interpretar-se dentro de um contexto. Note que a estrutura é um Composite -
// expressões terminais são folhas e as não terminais são nós internos.
interface Expressao {
    boolean interpretar(ContextoRegra contexto);

    String emTexto();
}

// EXPRESSÕES TERMINAIS - folhas da árvore. Não contêm outras expressões e resolvem consultando
// diretamente o contexto.

class Fato implements Expressao {
    private final String nome;

    Fato(String nome) {
        this.nome = nome;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        return contexto.fato(nome);
    }

    @Override
    public String emTexto() {
        return nome;
    }
}

class MaiorQue implements Expressao {
    private final String variavel;
    private final int limite;

    MaiorQue(String variavel, int limite) {
        this.variavel = variavel;
        this.limite = limite;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        return contexto.numero(variavel) > limite;
    }

    @Override
    public String emTexto() {
        return variavel + " > " + limite;
    }
}

class Literal implements Expressao {
    private final boolean valor;

    Literal(boolean valor) {
        this.valor = valor;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        return valor;
    }

    @Override
    public String emTexto() {
        return String.valueOf(valor);
    }
}

// EXPRESSÕES NÃO TERMINAIS - nós internos. Contêm outras expressões e interpretam a si mesmas
// combinando o resultado dos filhos. A recursão é o próprio mecanismo de avaliação.

class E implements Expressao {
    private final Expressao esquerda;
    private final Expressao direita;

    E(Expressao esquerda, Expressao direita) {
        this.esquerda = esquerda;
        this.direita = direita;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        // Avaliação em curto-circuito, como no operador && da linguagem.
        return esquerda.interpretar(contexto) && direita.interpretar(contexto);
    }

    @Override
    public String emTexto() {
        return "(" + esquerda.emTexto() + " E " + direita.emTexto() + ")";
    }
}

class Ou implements Expressao {
    private final Expressao esquerda;
    private final Expressao direita;

    Ou(Expressao esquerda, Expressao direita) {
        this.esquerda = esquerda;
        this.direita = direita;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        return esquerda.interpretar(contexto) || direita.interpretar(contexto);
    }

    @Override
    public String emTexto() {
        return "(" + esquerda.emTexto() + " OU " + direita.emTexto() + ")";
    }
}

class Nao implements Expressao {
    private final Expressao expressao;

    Nao(Expressao expressao) {
        this.expressao = expressao;
    }

    @Override
    public boolean interpretar(ContextoRegra contexto) {
        return !expressao.interpretar(contexto);
    }

    @Override
    public String emTexto() {
        return "NAO " + expressao.emTexto();
    }
}

// O PARSER não faz parte do padrão - o Interpreter cuida da avaliação, não da análise sintática.
// Este é um parser mínimo, em notação polonesa (prefixada), só para mostrar de onde a árvore vem
// no mundo real: de um arquivo de configuração, de uma tela de administração ou de um banco.
class ParserRegra {

    private final String[] tokens;
    private int posicao;

    ParserRegra(String expressao) {
        this.tokens = expressao.trim().split("\\s+");
    }

    Expressao analisar() {
        String token = tokens[posicao++];

        switch (token) {
            case "E":
                return new E(analisar(), analisar());
            case "OU":
                return new Ou(analisar(), analisar());
            case "NAO":
                return new Nao(analisar());
            case "true":
                return new Literal(true);
            case "false":
                return new Literal(false);
            default:
                if (token.contains(">")) {
                    String[] partes = token.split(">");
                    return new MaiorQue(partes[0], Integer.parseInt(partes[1]));
                }
                return new Fato(token);
        }
    }
}

// Classe Cliente
class AvaliadorRegras {

    public void avaliar(String nomeRegra, Expressao regra, ContextoRegra contexto) {
        System.out.println(nomeRegra + ": " + regra.emTexto()
                           + " -> " + regra.interpretar(contexto));
    }

    public static void main(String[] args) {
        ContextoRegra pedido = new ContextoRegra()
                .defineNumero("valorPedido", 12000)
                .defineFato("clienteVip", true)
                .defineFato("primeiraCompra", false)
                .defineFato("clienteInadimplente", false);

        AvaliadorRegras avaliador = new AvaliadorRegras();

        // Árvore montada em código: valorPedido > 10000 E clienteVip
        Expressao descontoVip = new E(new MaiorQue("valorPedido", 10000), new Fato("clienteVip"));
        avaliador.avaliar("desconto vip", descontoVip, pedido);

        // Árvore mais profunda: primeiraCompra OU (valorPedido > 5000 E NAO clienteInadimplente)
        Expressao freteGratis = new Ou(
                new Fato("primeiraCompra"),
                new E(new MaiorQue("valorPedido", 5000), new Nao(new Fato("clienteInadimplente"))));
        avaliador.avaliar("frete grátis", freteGratis, pedido);

        // A MESMA árvore vinda de texto: é assim que a regra deixa de ser código.
        Expressao daConfiguracao = new ParserRegra("OU primeiraCompra E valorPedido>5000 NAO clienteInadimplente")
                .analisar();
        avaliador.avaliar("frete grátis (do arquivo)", daConfiguracao, pedido);

        // Trocar a regra é trocar o texto, sem recompilar nada.
        Expressao outraPolitica = new ParserRegra("E clienteVip NAO clienteInadimplente").analisar();
        avaliador.avaliar("atendimento prioritário", outraPolitica, pedido);

        // A mesma regra sobre outro pedido: a árvore é reutilizável, o contexto é que muda.
        ContextoRegra outroPedido = new ContextoRegra()
                .defineNumero("valorPedido", 300)
                .defineFato("clienteVip", false);
        avaliador.avaliar("desconto vip (outro pedido)", descontoVip, outroPedido);
    }
}

//Limites do padrão: ele só se paga quando a gramática é PEQUENA e ESTÁVEL. Com muitas regras
//gramaticais, o número de classes explode e um gerador de parser (ANTLR, JavaCC) passa a valer
//mais a pena. Por isso o Interpreter é o padrão do GoF menos usado no dia a dia.
//Onde ele aparece: java.util.regex.Pattern, expressões de JPQL/Criteria API, e as especificações
//de regra de negócio compostas (Specification pattern) são interpretadores.

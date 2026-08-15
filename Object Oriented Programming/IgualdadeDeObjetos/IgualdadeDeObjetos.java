//POO - IDENTIDADE, IGUALDADE E hashCode
//== compara REFERÊNCIAS: pergunta se são o mesmo objeto na memória. equals compara VALOR: pergunta
//se dois objetos representam a mesma coisa. Object.equals, herdado sem sobrescrita, faz exatamente
//o mesmo que ==, e é por isso que dois objetos com dados idênticos aparecem como diferentes.
//Quem sobrescreve equals é obrigado a sobrescrever hashCode: o contrato exige que objetos iguais
//tenham o mesmo hashCode. HashSet e HashMap procuram primeiro pelo hashCode e só então comparam
//com equals - com hashCode inconsistente, o mesmo valor entra duas vezes no conjunto.
//equals precisa ser reflexivo, simétrico, transitivo, consistente e devolver false para null.
//Objeto MUTÁVEL usado como chave é armadilha: se um campo que entra no hashCode muda depois da
//inserção, a chave passa a ser procurada em outro balde e o mapa deixa de encontrá-la.
//toString não muda a semântica, mas é o que aparece no log e no depurador - sem ele sobra
//NomeDaClasse@1b6d3586.
//record gera equals, hashCode e toString a partir dos componentes declarados.

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class CodigoSemIgualdade {

    private final String valor;

    CodigoSemIgualdade(String valor) {
        this.valor = valor;
    }

    String getValor() {
        return valor;
    }
}

final class Sku {

    private final String codigo;
    private final String deposito;

    Sku(String codigo, String deposito) {
        this.codigo = Objects.requireNonNull(codigo, "código é obrigatório");
        this.deposito = Objects.requireNonNull(deposito, "depósito é obrigatório");
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Sku)) {
            return false;
        }
        Sku sku = (Sku) outro;
        return codigo.equals(sku.codigo) && deposito.equals(sku.deposito);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo, deposito);
    }

    @Override
    public String toString() {
        return codigo + "@" + deposito;
    }
}

class ChaveMutavel {

    private String valor;

    ChaveMutavel(String valor) {
        this.valor = valor;
    }

    void setValor(String valor) {
        this.valor = valor;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof ChaveMutavel && valor.equals(((ChaveMutavel) outro).valor);
    }

    @Override
    public int hashCode() {
        return valor.hashCode();
    }

    @Override
    public String toString() {
        return "chave " + valor;
    }
}

record Coordenada(int x, int y) {
}

class IgualdadeDeObjetos {

    public static void main(String[] args) {
        CodigoSemIgualdade a = new CodigoSemIgualdade("ABC");
        CodigoSemIgualdade b = new CodigoSemIgualdade("ABC");
        System.out.println("sem equals: a == b? " + (a == b)
                           + ", a.equals(b)? " + a.equals(b)
                           + ", impresso: " + a);

        Set<CodigoSemIgualdade> conjuntoIngenuo = new HashSet<>();
        conjuntoIngenuo.add(a);
        conjuntoIngenuo.add(b);
        System.out.println("o mesmo código entrou " + conjuntoIngenuo.size() + " vezes no HashSet");

        Sku primeiro = new Sku("TEC-001", "SP");
        Sku segundo = new Sku("TEC-001", "SP");
        Sku deOutroDeposito = new Sku("TEC-001", "RJ");

        System.out.println("com equals: primeiro == segundo? " + (primeiro == segundo)
                           + ", equals? " + primeiro.equals(segundo));
        System.out.println("depósitos diferentes são iguais? " + primeiro.equals(deOutroDeposito));

        Set<Sku> estoque = new HashSet<>();
        estoque.add(primeiro);
        estoque.add(segundo);
        estoque.add(deOutroDeposito);
        System.out.println("HashSet de Sku: " + estoque.size() + " elementos -> " + estoque);

        Map<Sku, Integer> quantidades = new HashMap<>();
        quantidades.put(primeiro, 12);
        System.out.println("consulta por outro objeto de mesmo valor: "
                           + quantidades.get(new Sku("TEC-001", "SP")));

        Map<ChaveMutavel, String> mapa = new HashMap<>();
        ChaveMutavel chave = new ChaveMutavel("original");
        mapa.put(chave, "valor guardado");
        chave.setValor("alterada");
        System.out.println("chave mutável após a alteração: " + mapa.get(chave)
                           + " (o mapa ainda contém " + mapa.size() + " entrada)");

        Coordenada origem = new Coordenada(0, 0);
        Coordenada outraOrigem = new Coordenada(0, 0);
        System.out.println("record: " + origem + ", iguais? " + origem.equals(outraOrigem)
                           + ", mesmo hashCode? " + (origem.hashCode() == outraOrigem.hashCode()));
    }
}

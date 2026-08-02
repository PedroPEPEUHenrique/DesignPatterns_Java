//Suponha que a sua tarefa seja calcular o custo mensal de folha de um setor da empresa. A
//estrutura é uma árvore: um setor contém outros setores e funcionários, em qualquer profundidade.
//Um funcionário possui:
//Nome
//Cargo
//Salário

//Imagine a solução ingênua: um método que recebe um Object e faz "if (obj instanceof Funcionario)
//... else if (obj instanceof Setor) ...", com recursão manual e um cast em cada ramo. Cada novo
//tipo de nó obriga a mexer em todos esses ifs, espalhados pelo sistema.

//O Composite resolve o problema de compor objetos em estruturas de árvore e permitir que o cliente
//trate objetos individuais (folhas) e composições de objetos de maneira UNIFORME.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Padrão Composite - o Component
// É a chave do padrão: folha e composto implementam ESTA MESMA interface, então o cliente nunca
// precisa saber com qual dos dois está falando.
interface UnidadeOrganizacional {
    String getNome();

    int custoMensalEmCentavos();

    int quantidadeDePessoas();

    void imprimir(String recuo);
}

// Padrão Composite - a Leaf (folha)
// Não tem filhos. Implementa as operações de forma direta, encerrando a recursão.
class Funcionario implements UnidadeOrganizacional {
    private final String nome;
    private final String cargo;
    private final int salarioEmCentavos;

    Funcionario(String nome, String cargo, int salarioEmCentavos) {
        this.nome = nome;
        this.cargo = cargo;
        this.salarioEmCentavos = salarioEmCentavos;
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public int custoMensalEmCentavos() {
        return salarioEmCentavos;
    }

    @Override
    public int quantidadeDePessoas() {
        return 1;
    }

    @Override
    public void imprimir(String recuo) {
        System.out.println(recuo + "- " + nome + " (" + cargo + ") "
                           + salarioEmCentavos + " centavos");
    }
}

// Padrão Composite - o Composite
// Guarda filhos do tipo Component e implementa cada operação DELEGANDO aos filhos e combinando
// os resultados. Repare que ele não sabe se cada filho é folha ou composto - e não precisa saber.
class Setor implements UnidadeOrganizacional {
    private final String nome;
    private final List<UnidadeOrganizacional> membros = new ArrayList<>();

    Setor(String nome) {
        this.nome = nome;
    }

    // As operações de gestão de filhos ficam SÓ no composto. O GoF discute colocá-las no
    // Component para uniformidade total, mas aí a folha precisaria lançar exceção em adicionar() -
    // troca-se segurança de tipos por transparência. Aqui optei pela segurança.
    public Setor adicionar(UnidadeOrganizacional unidade) {
        membros.add(unidade);
        return this;
    }

    public void remover(UnidadeOrganizacional unidade) {
        membros.remove(unidade);
    }

    public List<UnidadeOrganizacional> getMembros() {
        return Collections.unmodifiableList(membros);
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public int custoMensalEmCentavos() {
        int total = 0;
        for (UnidadeOrganizacional membro : membros) {
            total += membro.custoMensalEmCentavos();   // recursão implícita pelo polimorfismo
        }
        return total;
    }

    @Override
    public int quantidadeDePessoas() {
        int total = 0;
        for (UnidadeOrganizacional membro : membros) {
            total += membro.quantidadeDePessoas();
        }
        return total;
    }

    @Override
    public void imprimir(String recuo) {
        System.out.println(recuo + "+ " + nome + " [" + quantidadeDePessoas() + " pessoas, "
                           + custoMensalEmCentavos() + " centavos]");
        for (UnidadeOrganizacional membro : membros) {
            membro.imprimir(recuo + "   ");
        }
    }
}

// Classe Cliente
class EstruturaOrganizacional {

    // Nenhum instanceof, nenhum cast, nenhuma recursão escrita à mão: o parâmetro é o Component e
    // a árvore inteira responde com uma única chamada.
    public void relatorioDeCusto(UnidadeOrganizacional unidade) {
        System.out.println("custo de " + unidade.getNome() + ": "
                           + unidade.custoMensalEmCentavos() + " centavos");
    }

    public static void main(String[] args) {
        Setor engenharia = new Setor("Engenharia")
                .adicionar(new Funcionario("Ana", "Tech Lead", 1800000))
                .adicionar(new Funcionario("Bruno", "Dev Pleno", 1100000));

        Setor qualidade = new Setor("Qualidade")
                .adicionar(new Funcionario("Carla", "QA", 900000));

        Setor tecnologia = new Setor("Tecnologia")
                .adicionar(engenharia)
                .adicionar(qualidade)
                .adicionar(new Funcionario("Diego", "Diretor de TI", 3000000));

        Setor empresa = new Setor("Empresa")
                .adicionar(tecnologia)
                .adicionar(new Setor("Comercial")
                        .adicionar(new Funcionario("Elisa", "Vendedora", 700000)));

        empresa.imprimir("");

        EstruturaOrganizacional cliente = new EstruturaOrganizacional();

        // A MESMA operação vale para a empresa inteira, para um setor e para uma única pessoa.
        cliente.relatorioDeCusto(empresa);
        cliente.relatorioDeCusto(engenharia);
        cliente.relatorioDeCusto(new Funcionario("Ana", "Tech Lead", 1800000));
    }
}

//O Composite aparece em toda estrutura recursiva: sistema de arquivos (arquivo x diretório),
//DOM de uma página (texto x elemento), menus (item x submenu) e expressões aritméticas (literal x
//operação) - este último é a base do padrão Interpreter, que é um Composite especializado.

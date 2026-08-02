//Calcular o custo de folha de um setor. A estrutura é uma árvore: um setor contém outros setores
//e funcionários, em qualquer profundidade. A solução ingênua percorre a árvore com instanceof e
//cast em cada ramo.
//O Composite compõe objetos em árvore e permite tratar objetos individuais (folhas) e composições
//de maneira UNIFORME.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Component: folha e composto implementam ESTA MESMA interface.
interface UnidadeOrganizacional {
    String getNome();

    int custoMensalEmCentavos();

    int quantidadeDePessoas();

    void imprimir(String recuo);
}

// Leaf: não tem filhos e encerra a recursão.
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

// Composite: guarda filhos do tipo Component e delega, sem saber se cada um é folha ou composto.
class Setor implements UnidadeOrganizacional {
    private final String nome;
    private final List<UnidadeOrganizacional> membros = new ArrayList<>();

    Setor(String nome) {
        this.nome = nome;
    }

    // A gestão de filhos fica só no composto. Colocá-la no Component daria transparência total,
    // mas a folha teria que lançar exceção em adicionar() - troca-se segurança por uniformidade.
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

// Cliente
class EstruturaOrganizacional {

    // Sem instanceof, sem cast, sem recursão manual: a árvore inteira responde a uma chamada.
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

//O Composite aparece em toda estrutura recursiva: sistema de arquivos, DOM, menus e expressões
//aritméticas - esta última é a base do Interpreter, que é um Composite especializado.

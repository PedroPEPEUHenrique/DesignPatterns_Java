//Calcular o custo de folha de um setor. A estrutura é uma árvore: um setor contém outros setores
//e funcionários, em qualquer profundidade. A solução ingênua percorre a árvore com instanceof e
//cast em cada ramo.
//O Composite compõe objetos em árvore e permite tratar objetos individuais (folhas) e composições
//de maneira UNIFORME.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

interface UnidadeOrganizacional {
    String getNome();

    int custoMensalEmCentavos();

    int quantidadeDePessoas();

    void imprimir(String recuo);
}

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

class Setor implements UnidadeOrganizacional {
    private final String nome;
    private final List<UnidadeOrganizacional> membros = new ArrayList<>();

    Setor(String nome) {
        this.nome = nome;
    }

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
            total += membro.custoMensalEmCentavos();
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

class EstruturaOrganizacional {

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

        cliente.relatorioDeCusto(empresa);
        cliente.relatorioDeCusto(engenharia);
        cliente.relatorioDeCusto(new Funcionario("Ana", "Tech Lead", 1800000));
    }
}

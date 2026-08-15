//POO - HERANÇA
//Herdar é afirmar "É UM": Gerente É UM Funcionário. A subclasse recebe os membros da superclasse e
//pode ACRESCENTAR o que é seu ou SOBRESCREVER o que herdou. super(...) chama o construtor de cima,
//que roda sempre primeiro, e super.metodo() reaproveita a versão da superclasse em vez de copiá-la.
//@Override não é enfeite: sem ela, um erro na assinatura cria silenciosamente um método NOVO e a
//sobrescrita que você acha que fez nunca acontece.
//O limite é o Princípio da Substituição de Liskov: onde o código espera a superclasse, qualquer
//subclasse tem de servir SEM surpresa. Quadrado herdando de Retângulo passa na prova do "é um" da
//geometria e reprova na do software - largura e altura ajustáveis de forma independente fazem
//parte do contrato herdado, e o quadrado não tem como honrá-lo.
//Java tem herança simples: uma classe estende UMA classe (mas implementa várias interfaces). Antes
//de herdar, pergunte se não é caso de COMPOSIÇÃO: herdar prende a subclasse à implementação da
//superclasse para sempre.

class Funcionario {

    private final String nome;
    private final String matricula;
    private final int salarioBaseEmCentavos;

    Funcionario(String nome, String matricula, int salarioBaseEmCentavos) {
        this.nome = nome;
        this.matricula = matricula;
        this.salarioBaseEmCentavos = salarioBaseEmCentavos;
    }

    int salarioEmCentavos() {
        return salarioBaseEmCentavos;
    }

    String cargo() {
        return "funcionário";
    }

    final String cracha() {
        return matricula + " - " + nome + " (" + cargo() + ")";
    }

    String getNome() {
        return nome;
    }

    protected int getSalarioBaseEmCentavos() {
        return salarioBaseEmCentavos;
    }
}

class Vendedor extends Funcionario {

    private final int vendasEmCentavos;
    private final double comissao;

    Vendedor(String nome, String matricula, int salarioBaseEmCentavos,
             int vendasEmCentavos, double comissao) {
        super(nome, matricula, salarioBaseEmCentavos);
        this.vendasEmCentavos = vendasEmCentavos;
        this.comissao = comissao;
    }

    @Override
    int salarioEmCentavos() {
        return super.salarioEmCentavos() + (int) (vendasEmCentavos * comissao);
    }

    @Override
    String cargo() {
        return "vendedor";
    }
}

class Gerente extends Funcionario {

    private final int quantidadeDeLiderados;

    Gerente(String nome, String matricula, int salarioBaseEmCentavos, int quantidadeDeLiderados) {
        super(nome, matricula, salarioBaseEmCentavos);
        this.quantidadeDeLiderados = quantidadeDeLiderados;
    }

    @Override
    int salarioEmCentavos() {
        return super.salarioEmCentavos() + quantidadeDeLiderados * 20000;
    }

    @Override
    String cargo() {
        return "gerente";
    }

    void aprovarFerias(Funcionario liderado) {
        System.out.println(getNome() + " aprovou as férias de " + liderado.getNome());
    }
}

class Estagiario extends Funcionario {

    Estagiario(String nome, String matricula, int bolsaEmCentavos) {
        super(nome, matricula, bolsaEmCentavos);
    }

    @Override
    String cargo() {
        return "estagiário";
    }
}

class Retangulo {

    private int largura;
    private int altura;

    Retangulo(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
    }

    void setLargura(int largura) {
        this.largura = largura;
    }

    void setAltura(int altura) {
        this.altura = altura;
    }

    int area() {
        return largura * altura;
    }
}

class Quadrado extends Retangulo {

    Quadrado(int lado) {
        super(lado, lado);
    }

    @Override
    void setLargura(int largura) {
        super.setLargura(largura);
        super.setAltura(largura);
    }

    @Override
    void setAltura(int altura) {
        super.setLargura(altura);
        super.setAltura(altura);
    }
}

class Heranca {

    private static void redimensionarPara5x4(Retangulo retangulo) {
        retangulo.setLargura(5);
        retangulo.setAltura(4);
        System.out.println("  esperava área 20 e obteve " + retangulo.area());
    }

    public static void main(String[] args) {
        Funcionario[] equipe = {
                new Estagiario("Ana", "E-01", 150000),
                new Vendedor("Bruno", "V-07", 200000, 5000000, 0.03),
                new Gerente("Carla", "G-02", 900000, 6)
        };

        for (Funcionario funcionario : equipe) {
            System.out.println(funcionario.cracha() + ": "
                               + funcionario.salarioEmCentavos() + " centavos");
        }

        Gerente carla = (Gerente) equipe[2];
        carla.aprovarFerias(equipe[0]);

        System.out.println("substituição de Liskov:");
        redimensionarPara5x4(new Retangulo(1, 1));
        redimensionarPara5x4(new Quadrado(1));
    }
}

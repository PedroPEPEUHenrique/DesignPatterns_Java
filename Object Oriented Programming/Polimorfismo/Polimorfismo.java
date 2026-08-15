//POO - POLIMORFISMO
//Polimorfismo é a mesma mensagem produzindo respostas diferentes conforme o objeto que a recebe.
//Toda variável tem um TIPO ESTÁTICO, o declarado no código, e um TIPO DINÂMICO, o da instância que
//está ali em tempo de execução. Quem decide qual corpo de método roda é o tipo DINÂMICO: é a
//ligação tardia, e é ela que permite escrever um laço que não sabe com quais classes vai lidar.
//SOBRESCRITA (override) é o polimorfismo propriamente dito: mesma assinatura, classes diferentes,
//escolha em tempo de execução.
//SOBRECARGA (overload) é outra coisa: mesmo nome, assinaturas diferentes, escolha feita pelo
//COMPILADOR a partir do tipo ESTÁTICO. Por isso uma variável Object que guarda uma String cai na
//versão que recebe Object.
//Subir para o supertipo (upcast) é sempre seguro; descer (downcast) exige verificação, e o
//instanceof com padrão faz o teste e a conversão de uma vez. Uma cadeia de instanceof, porém,
//costuma ser sinal de que falta um método na hierarquia.

class Midia {

    private final String titulo;
    private final int ano;

    Midia(String titulo, int ano) {
        this.titulo = titulo;
        this.ano = ano;
    }

    int duracaoEmMinutos() {
        return 0;
    }

    String descricao() {
        return titulo + " (" + ano + ")";
    }

    String getTitulo() {
        return titulo;
    }

    @Override
    public String toString() {
        return descricao() + ", " + duracaoEmMinutos() + " min";
    }
}

class Livro extends Midia {

    private final int paginas;
    private final String autor;

    Livro(String titulo, int ano, int paginas, String autor) {
        super(titulo, ano);
        this.paginas = paginas;
        this.autor = autor;
    }

    @Override
    int duracaoEmMinutos() {
        return paginas * 2;
    }

    @Override
    String descricao() {
        return "livro " + super.descricao() + " de " + autor;
    }

    int getPaginas() {
        return paginas;
    }
}

class Filme extends Midia {

    private final int minutos;
    private final String diretor;

    Filme(String titulo, int ano, int minutos, String diretor) {
        super(titulo, ano);
        this.minutos = minutos;
        this.diretor = diretor;
    }

    @Override
    int duracaoEmMinutos() {
        return minutos;
    }

    @Override
    String descricao() {
        return "filme " + super.descricao() + " dirigido por " + diretor;
    }
}

class Episodio extends Midia {

    private final int minutos;
    private final int temporada;

    Episodio(String titulo, int ano, int minutos, int temporada) {
        super(titulo, ano);
        this.minutos = minutos;
        this.temporada = temporada;
    }

    @Override
    int duracaoEmMinutos() {
        return minutos;
    }

    @Override
    String descricao() {
        return "episódio " + super.descricao() + ", temporada " + temporada;
    }
}

class Polimorfismo {

    private static String identificar(Object objeto) {
        return "recebi um Object";
    }

    private static String identificar(String texto) {
        return "recebi uma String";
    }

    private static String identificar(Midia midia) {
        return "recebi uma Mídia";
    }

    public static void main(String[] args) {
        Midia[] acervo = {
                new Livro("O Cortiço", 1890, 320, "Aluísio Azevedo"),
                new Filme("Cidade de Deus", 2002, 130, "Fernando Meirelles"),
                new Episodio("Piloto", 2019, 48, 1)
        };

        int total = 0;
        for (Midia midia : acervo) {
            System.out.println(midia);
            total += midia.duracaoEmMinutos();
        }
        System.out.println("acervo com " + total + " minutos");

        Midia comoMidia = new Livro("Dom Casmurro", 1899, 256, "Machado de Assis");
        System.out.println("tipo estático Midia, tipo dinâmico "
                           + comoMidia.getClass().getSimpleName()
                           + " -> " + comoMidia.descricao());

        Object texto = "sobrecarga é resolvida na compilação";
        System.out.println(identificar(texto));
        System.out.println(identificar("sobrecarga é resolvida na compilação"));
        System.out.println(identificar(comoMidia));

        for (Midia midia : acervo) {
            if (midia instanceof Livro livro) {
                System.out.println(livro.getTitulo() + " tem " + livro.getPaginas() + " páginas");
            }
        }
    }
}

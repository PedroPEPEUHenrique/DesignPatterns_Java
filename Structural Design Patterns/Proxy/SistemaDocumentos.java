//Suponha que a sua tarefa seja abrir documentos digitalizados de um acervo. A abstração é
//Documento, com as operações:
//exibir()
//getTitulo()

//Imagine que o conteúdo de cada documento tem dezenas de megabytes e vem de um serviço remoto.
//Carregar todos ao abrir a listagem trava a aplicação, sendo que o usuário vai abrir um ou dois.
//Some a isso a exigência de que documentos sigilosos só possam ser abertos por certos perfis e de
//que toda abertura fique registrada em auditoria.

//O Proxy resolve o problema de fornecer um SUBSTITUTO para outro objeto, controlando o acesso a
//ele. O substituto tem a mesma interface do objeto real, então o cliente não percebe a diferença.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Padrão Proxy - o Subject
// Proxy e objeto real implementam a MESMA interface. É isso que permite trocar um pelo outro sem
// o cliente saber.
interface Documento {
    String getTitulo();

    void exibir();
}

// Padrão Proxy - o RealSubject
// O objeto caro de verdade. Repare que o custo está no CONSTRUTOR: só existir já é pesado.
class DocumentoDigitalizado implements Documento {
    private final String titulo;
    private final String conteudo;

    DocumentoDigitalizado(String titulo) {
        this.titulo = titulo;
        this.conteudo = carregarDoServidor(titulo);
    }

    private String carregarDoServidor(String titulo) {
        System.out.println("    ...baixando " + titulo + " do servidor (operação cara)...");
        return "conteúdo digitalizado de " + titulo;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    @Override
    public void exibir() {
        System.out.println("    " + conteudo);
    }
}

// Padrão Proxy VIRTUAL
// Adia a criação do objeto real até o primeiro uso que realmente precise dele. Note que
// getTitulo() é respondido pelo próprio proxy, SEM disparar o carregamento - é aí que está o ganho.
class ProxyVirtualDocumento implements Documento {
    private final String titulo;
    private DocumentoDigitalizado real;   // só é criado quando (e se) for necessário

    ProxyVirtualDocumento(String titulo) {
        this.titulo = titulo;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    @Override
    public void exibir() {
        if (real == null) {
            real = new DocumentoDigitalizado(titulo);   // inicialização preguiçosa
        }
        real.exibir();
    }
}

class Usuario {
    private final String nome;
    private final String perfil;

    Usuario(String nome, String perfil) {
        this.nome = nome;
        this.perfil = perfil;
    }

    String getNome() {
        return nome;
    }

    String getPerfil() {
        return perfil;
    }
}

// Padrão Proxy de PROTEÇÃO
// Verifica permissão antes de delegar. A regra de acesso fica FORA do objeto real, que continua
// só sabendo exibir conteúdo.
class ProxyProtecaoDocumento implements Documento {
    private final Documento real;
    private final Usuario usuario;
    private final boolean sigiloso;

    ProxyProtecaoDocumento(Documento real, Usuario usuario, boolean sigiloso) {
        this.real = real;
        this.usuario = usuario;
        this.sigiloso = sigiloso;
    }

    @Override
    public String getTitulo() {
        return real.getTitulo();
    }

    @Override
    public void exibir() {
        if (sigiloso && !"AUDITOR".equals(usuario.getPerfil())) {
            System.out.println("    acesso negado a " + usuario.getNome()
                               + " (perfil " + usuario.getPerfil() + ")");
            return;
        }
        real.exibir();
    }
}

// Padrão Proxy de REGISTRO (logging / auditoria)
// Proxies são empilháveis como decoradores: aqui um proxy embrulha outro.
class ProxyAuditoriaDocumento implements Documento {
    private final Documento real;
    private final Usuario usuario;
    private static final List<String> TRILHA = new ArrayList<>();

    ProxyAuditoriaDocumento(Documento real, Usuario usuario) {
        this.real = real;
        this.usuario = usuario;
    }

    @Override
    public String getTitulo() {
        return real.getTitulo();
    }

    @Override
    public void exibir() {
        TRILHA.add(usuario.getNome() + " abriu " + real.getTitulo());
        real.exibir();
    }

    static void imprimirTrilha() {
        System.out.println("trilha de auditoria:");
        for (String registro : TRILHA) {
            System.out.println("  " + registro);
        }
    }
}

// Padrão Proxy de CACHE
// Guarda o resultado de operações caras para não repeti-las. A diferença para o Flyweight é a
// intenção: aqui o objetivo é evitar a chamada, não economizar memória.
class ProxyCacheDocumento implements Documento {
    private static final Map<String, DocumentoDigitalizado> CACHE = new HashMap<>();
    private final String titulo;

    ProxyCacheDocumento(String titulo) {
        this.titulo = titulo;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    @Override
    public void exibir() {
        CACHE.computeIfAbsent(titulo, DocumentoDigitalizado::new).exibir();
    }
}

// Classe Cliente
class SistemaDocumentos {

    // O tipo do parâmetro é Documento. O cliente não distingue o objeto real de nenhum dos proxies.
    public void listar(List<Documento> documentos) {
        System.out.println("índice do acervo:");
        for (Documento documento : documentos) {
            // Só o título: com o proxy virtual, montar esta listagem não baixa nada.
            System.out.println("  " + documento.getTitulo());
        }
    }

    public static void main(String[] args) {
        Usuario comum = new Usuario("Ana", "OPERADOR");
        Usuario auditor = new Usuario("Bruno", "AUDITOR");

        List<Documento> acervo = List.of(
                new ProxyVirtualDocumento("contrato-2019.tif"),
                new ProxyVirtualDocumento("ata-assembleia.tif"),
                new ProxyVirtualDocumento("balanco-2020.tif"));

        SistemaDocumentos sistema = new SistemaDocumentos();
        sistema.listar(acervo);   // nenhuma linha de "baixando" aparece: nada foi carregado

        System.out.println("\nabrindo um documento:");
        acervo.get(0).exibir();   // agora sim o carregamento acontece
        System.out.println("abrindo o mesmo de novo:");
        acervo.get(0).exibir();   // já carregado, não baixa outra vez

        System.out.println("\ncom proteção e auditoria empilhadas:");
        Documento sigiloso = new ProxyAuditoriaDocumento(
                                 new ProxyProtecaoDocumento(
                                     new ProxyVirtualDocumento("folha-salarial.tif"),
                                     comum, true),
                                 comum);
        sigiloso.exibir();

        Documento sigilosoAuditor = new ProxyAuditoriaDocumento(
                                        new ProxyProtecaoDocumento(
                                            new ProxyVirtualDocumento("folha-salarial.tif"),
                                            auditor, true),
                                        auditor);
        sigilosoAuditor.exibir();

        System.out.println("\ncom cache compartilhado:");
        new ProxyCacheDocumento("manual.tif").exibir();
        new ProxyCacheDocumento("manual.tif").exibir();   // instância nova de proxy, mesmo real

        System.out.println();
        ProxyAuditoriaDocumento.imprimirTrilha();
    }
}

//Os quatro tipos clássicos do GoF:
//Virtual - adia a criação de um objeto caro (o exemplo acima).
//De proteção - controla quem pode acessar.
//Remoto - representa localmente um objeto que vive em outro processo ou máquina; foi a base de
//  RMI, EJB remoto e ainda hoje dos stubs de gRPC.
//Smart reference - executa tarefas extras no acesso: contagem de referências, carga de atributos
//  sob demanda, travamento. É exatamente o que a JPA faz com relacionamentos LAZY, devolvendo um
//  proxy no lugar da entidade até que algum getter seja chamado.
//
//Em Java, java.lang.reflect.Proxy permite criar proxies dinamicamente, sem escrever uma classe por
//interface. É o mecanismo por trás dos interceptadores de Spring, CDI e EJB.

//Abrir documentos digitalizados de dezenas de megabytes vindos de um serviço remoto. Carregar
//todos ao montar a listagem trava a aplicação; além disso, documentos sigilosos exigem controle
//de acesso e registro de auditoria.
//O Proxy fornece um SUBSTITUTO para outro objeto, controlando o acesso a ele. Como tem a mesma
//interface, o cliente não percebe a diferença.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface Documento {
    String getTitulo();

    void exibir();
}

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

class ProxyVirtualDocumento implements Documento {
    private final String titulo;
    private DocumentoDigitalizado real;

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
            real = new DocumentoDigitalizado(titulo);
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

class SistemaDocumentos {

    public void listar(List<Documento> documentos) {
        System.out.println("índice do acervo:");
        for (Documento documento : documentos) {
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
        sistema.listar(acervo);

        System.out.println("\nabrindo um documento:");
        acervo.get(0).exibir();
        System.out.println("abrindo o mesmo de novo:");
        acervo.get(0).exibir();

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
        new ProxyCacheDocumento("manual.tif").exibir();

        System.out.println();
        ProxyAuditoriaDocumento.imprimirTrilha();
    }
}

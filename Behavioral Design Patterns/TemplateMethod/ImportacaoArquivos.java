//Importar arquivos de parceiros. O processo é sempre o mesmo (abrir, validar cabeçalho, ler,
//converter, gravar, fechar, resumir) e só dois ou três passos mudam por formato. Uma classe por
//formato copia os sete passos em todas, e corrigir um bug vira três correções.
//O Template Method define o ESQUELETO de um algoritmo em uma operação, adiando alguns passos para
//as subclasses, que os redefinem sem alterar a estrutura do algoritmo.

import java.util.ArrayList;
import java.util.List;

class RegistroImportado {
    private final String documento;
    private final String nome;
    private final int valorEmCentavos;

    RegistroImportado(String documento, String nome, int valorEmCentavos) {
        this.documento = documento;
        this.nome = nome;
        this.valorEmCentavos = valorEmCentavos;
    }

    String getDocumento() {
        return documento;
    }

    String getNome() {
        return nome;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }

    @Override
    public String toString() {
        return documento + " | " + nome + " | " + valorEmCentavos;
    }
}

// AbstractClass
abstract class ImportadorArquivo {

    // O template method é FINAL de propósito: a subclasse muda os passos, não a ordem nem a
    // estrutura. Tirar o final devolve a ela o poder de quebrar o fluxo.
    public final ResumoImportacao importar(List<String> linhas) {
        System.out.println("importando via " + formato());

        abrir();
        try {
            if (!cabecalhoValido(linhas.get(0))) {
                throw new IllegalArgumentException("cabeçalho inválido para " + formato());
            }

            List<RegistroImportado> registros = new ArrayList<>();
            int rejeitados = 0;

            for (String linha : linhas.subList(1, linhas.size())) {
                RegistroImportado registro = converter(linha);
                if (registro == null) {
                    rejeitados++;
                    continue;
                }
                gravar(registro);
                registros.add(registro);
            }

            aoTerminar(registros);

            return new ResumoImportacao(formato(), registros.size(), rejeitados);
        } finally {
            fechar();   // a base garante isso para todas as subclasses, inclusive em caso de erro
        }
    }

    // Passos ABSTRATOS: a subclasse é obrigada a fornecer. É o que varia entre os formatos.
    protected abstract String formato();

    protected abstract boolean cabecalhoValido(String primeiraLinha);

    protected abstract RegistroImportado converter(String linha);

    // Passos CONCRETOS: iguais para todos, não se repetem em lugar nenhum.
    protected void abrir() {
        System.out.println("  abrindo origem");
    }

    protected void gravar(RegistroImportado registro) {
        System.out.println("  gravando " + registro);
    }

    protected void fechar() {
        System.out.println("  fechando origem");
    }

    // GANCHO (hook): passo opcional, vazio na base. É o que o diferencia de um método abstrato.
    protected void aoTerminar(List<RegistroImportado> registros) {
    }
}

class ResumoImportacao {
    private final String formato;
    private final int importados;
    private final int rejeitados;

    ResumoImportacao(String formato, int importados, int rejeitados) {
        this.formato = formato;
        this.importados = importados;
        this.rejeitados = rejeitados;
    }

    @Override
    public String toString() {
        return "resumo " + formato + ": " + importados + " importados, " + rejeitados + " rejeitados";
    }
}

class ImportadorCsv extends ImportadorArquivo {

    @Override
    protected String formato() {
        return "CSV";
    }

    @Override
    protected boolean cabecalhoValido(String primeiraLinha) {
        return primeiraLinha.startsWith("documento;nome;valor");
    }

    @Override
    protected RegistroImportado converter(String linha) {
        String[] campos = linha.split(";");
        if (campos.length != 3) {
            return null;
        }
        return new RegistroImportado(campos[0], campos[1], Integer.parseInt(campos[2]));
    }
}

class ImportadorPosicional extends ImportadorArquivo {

    @Override
    protected String formato() {
        return "posicional";
    }

    @Override
    protected boolean cabecalhoValido(String primeiraLinha) {
        return primeiraLinha.startsWith("HDR");
    }

    @Override
    protected RegistroImportado converter(String linha) {
        if (linha.length() < 30) {
            return null;
        }
        return new RegistroImportado(linha.substring(0, 11).trim(),
                                     linha.substring(11, 26).trim(),
                                     Integer.parseInt(linha.substring(26, 30).trim()));
    }

    // Só este formato precisa conferir o total do rodapé - é para isso que serve o gancho.
    @Override
    protected void aoTerminar(List<RegistroImportado> registros) {
        int total = 0;
        for (RegistroImportado registro : registros) {
            total += registro.getValorEmCentavos();
        }
        System.out.println("  <gancho> total conferido: " + total + " centavos");
    }
}

class ImportadorJsonSimplificado extends ImportadorArquivo {

    @Override
    protected String formato() {
        return "JSON";
    }

    @Override
    protected boolean cabecalhoValido(String primeiraLinha) {
        return primeiraLinha.trim().startsWith("[");
    }

    @Override
    protected RegistroImportado converter(String linha) {
        String limpa = linha.replaceAll("[\\[\\]{}\"]", "").trim();
        if (limpa.isEmpty()) {
            return null;
        }
        String[] campos = limpa.split(",");
        if (campos.length != 3) {
            return null;
        }
        return new RegistroImportado(valorDe(campos[0]), valorDe(campos[1]),
                                     Integer.parseInt(valorDe(campos[2])));
    }

    private String valorDe(String par) {
        return par.substring(par.indexOf(':') + 1).trim();
    }

    // Sobrescrever um passo CONCRETO é permitido; quebrar a cadeia do super é o risco conhecido
    // da variação por herança.
    @Override
    protected void gravar(RegistroImportado registro) {
        System.out.print("  [json] ");
        super.gravar(registro);
    }
}

// Cliente
class ImportacaoArquivos {

    public void executar(ImportadorArquivo importador, List<String> linhas) {
        System.out.println(importador.importar(linhas));
        System.out.println();
    }

    public static void main(String[] args) {
        ImportacaoArquivos importacao = new ImportacaoArquivos();

        importacao.executar(new ImportadorCsv(), List.of(
                "documento;nome;valor",
                "11122233344;Ana Souza;15000",
                "linha corrompida",
                "55566677788;Bruno Lima;24500"));

        importacao.executar(new ImportadorPosicional(), List.of(
                "HDR20260802",
                "11122233344Ana Souza     1500",
                "55566677788Bruno Lima    2450"));

        importacao.executar(new ImportadorJsonSimplificado(), List.of(
                "[",
                "{\"documento\":11122233344,\"nome\":Ana,\"valor\":15000}"));
    }
}

//O princípio por trás é o "Hollywood Principle": não nos chame, nós chamamos você. É a superclasse
//que controla o fluxo e chama os passos da subclasse - inversão de controle.
//Se a subclasse precisa implementar oito métodos, há mais de uma responsabilidade sendo variada
//ao mesmo tempo.

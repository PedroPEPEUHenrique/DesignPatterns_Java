//Suponha que a sua tarefa seja importar arquivos de parceiros para dentro do sistema. O processo
//é sempre o mesmo:
//Abrir a origem -> validar o cabeçalho -> ler os registros -> converter cada um -> gravar ->
//fechar a origem -> emitir o resumo

//Imagine uma classe de importação por formato: ImportadorCsv, ImportadorPosicional,
//ImportadorJson. Os sete passos aparecem copiados nas três, e só dois ou três deles realmente
//mudam. Corrigir um bug no fechamento da origem vira três correções - e alguém vai esquecer uma.

//O Template Method resolve o problema de definir o ESQUELETO de um algoritmo em uma operação,
//adiando alguns passos para as subclasses. Elas redefinem certos passos sem alterar a estrutura
//do algoritmo.

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

// Padrão Template Method - a AbstractClass
abstract class ImportadorArquivo {

    // ESTE é o template method. Ele é FINAL de propósito: a subclasse pode mudar os passos, mas
    // não a ordem nem a estrutura do algoritmo. Tirar o final devolve à subclasse o poder de
    // quebrar o fluxo, que é justamente o que o padrão quer impedir.
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
                RegistroImportado registro = converter(linha);   // passo obrigatório da subclasse
                if (registro == null) {
                    rejeitados++;
                    continue;
                }
                gravar(registro);
                registros.add(registro);
            }

            // GANCHO (hook): passo OPCIONAL, com implementação vazia na base. A subclasse
            // sobrescreve só se quiser. É o que diferencia um hook de um método abstrato.
            aoTerminar(registros);

            return new ResumoImportacao(formato(), registros.size(), rejeitados);
        } finally {
            fechar();   // roda mesmo em caso de erro: a base garante isso para todas as subclasses
        }
    }

    // PASSOS ABSTRATOS - a subclasse é OBRIGADA a fornecer. É o que varia entre os formatos.
    protected abstract String formato();

    protected abstract boolean cabecalhoValido(String primeiraLinha);

    protected abstract RegistroImportado converter(String linha);

    // PASSOS CONCRETOS - iguais para todos. Ficam aqui e não se repetem em lugar nenhum.
    protected void abrir() {
        System.out.println("  abrindo origem");
    }

    protected void gravar(RegistroImportado registro) {
        System.out.println("  gravando " + registro);
    }

    protected void fechar() {
        System.out.println("  fechando origem");
    }

    // GANCHO - vazio na base, opcional na subclasse.
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

// SUBCLASSE 1 - só implementa o que é específico do CSV. Não há nenhum passo de abrir/fechar
// duplicado aqui, e ela não tem como alterar a ordem do processo.
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

// SUBCLASSE 2 - arquivo posicional, e usa o gancho para uma etapa extra.
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

    // Aproveitando o gancho: só este formato precisa conferir o total do rodapé.
    @Override
    protected void aoTerminar(List<RegistroImportado> registros) {
        int total = 0;
        for (RegistroImportado registro : registros) {
            total += registro.getValorEmCentavos();
        }
        System.out.println("  <gancho> total conferido: " + total + " centavos");
    }
}

// SUBCLASSE 3 - sobrescreve também um passo CONCRETO, o que o padrão permite. Note que ela chama
// super.gravar(): quebrar essa cadeia por engano é um risco conhecido da variação por herança.
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

    @Override
    protected void gravar(RegistroImportado registro) {
        System.out.print("  [json] ");
        super.gravar(registro);
    }
}

// Classe Cliente
class ImportacaoArquivos {

    // O cliente depende da abstração e chama sempre a mesma operação, qualquer que seja o formato.
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

//O princípio por trás do padrão é o "Hollywood Principle": não nos chame, nós chamamos você. É a
//superclasse que controla o fluxo e chama os passos da subclasse - inversão de controle, e não o
//contrário.
//
//Template Method x Strategy: os dois variam parte de um algoritmo. Aqui a variação é por HERANÇA,
//decidida em tempo de compilação e limitada a uma superclasse; no Strategy é por COMPOSIÇÃO,
//trocável em tempo de execução. A herança acopla mais, mas evita ter que passar objetos de
//estratégia adiante quando os passos são muitos.
//Cuidado clássico: um template method com muitos passos abstratos vira um contrato pesado demais.
//Se a subclasse precisa implementar oito métodos, provavelmente há mais de uma responsabilidade
//sendo variada ao mesmo tempo.

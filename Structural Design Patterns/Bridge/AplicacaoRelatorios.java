//Gerar relatórios que variam em duas dimensões: o tipo (vendas, estoque) e o formato de saída
//(HTML, CSV, texto). Por herança seriam 2 x 3 classes, e um formato novo passaria para 2 x 4 -
//explosão combinatória de subclasses.
//O Bridge desacopla uma abstração da sua implementação para que as duas variem de forma
//INDEPENDENTE: em vez de multiplicar (2 x 3), passa-se a somar (2 + 3).

import java.util.ArrayList;
import java.util.List;

interface RenderizadorRelatorio {
    void iniciarDocumento(String titulo);

    void escreverCabecalho(List<String> colunas);

    void escreverLinha(List<String> valores);

    String finalizarDocumento();
}

class RenderizadorHtml implements RenderizadorRelatorio {
    private final StringBuilder saida = new StringBuilder();

    @Override
    public void iniciarDocumento(String titulo) {
        saida.append("<html><h1>").append(titulo).append("</h1><table>");
    }

    @Override
    public void escreverCabecalho(List<String> colunas) {
        saida.append("<tr>");
        for (String coluna : colunas) {
            saida.append("<th>").append(coluna).append("</th>");
        }
        saida.append("</tr>");
    }

    @Override
    public void escreverLinha(List<String> valores) {
        saida.append("<tr>");
        for (String valor : valores) {
            saida.append("<td>").append(valor).append("</td>");
        }
        saida.append("</tr>");
    }

    @Override
    public String finalizarDocumento() {
        return saida.append("</table></html>").toString();
    }
}

class RenderizadorCsv implements RenderizadorRelatorio {
    private final StringBuilder saida = new StringBuilder();

    @Override
    public void iniciarDocumento(String titulo) {
        saida.append("# ").append(titulo).append("\n");
    }

    @Override
    public void escreverCabecalho(List<String> colunas) {
        escreverLinha(colunas);
    }

    @Override
    public void escreverLinha(List<String> valores) {
        saida.append(String.join(";", valores)).append("\n");
    }

    @Override
    public String finalizarDocumento() {
        return saida.toString();
    }
}

class RenderizadorTextoPlano implements RenderizadorRelatorio {
    private final StringBuilder saida = new StringBuilder();

    @Override
    public void iniciarDocumento(String titulo) {
        saida.append(titulo.toUpperCase()).append("\n");
    }

    @Override
    public void escreverCabecalho(List<String> colunas) {
        for (String coluna : colunas) {
            saida.append(String.format("%-15s", coluna));
        }
        saida.append("\n");
    }

    @Override
    public void escreverLinha(List<String> valores) {
        for (String valor : valores) {
            saida.append(String.format("%-15s", valor));
        }
        saida.append("\n");
    }

    @Override
    public String finalizarDocumento() {
        return saida.toString();
    }
}

abstract class Relatorio {
    protected final RenderizadorRelatorio renderizador;

    protected Relatorio(RenderizadorRelatorio renderizador) {
        this.renderizador = renderizador;
    }

    public String gerar() {
        renderizador.iniciarDocumento(titulo());
        renderizador.escreverCabecalho(colunas());
        for (List<String> linha : linhas()) {
            renderizador.escreverLinha(linha);
        }
        return renderizador.finalizarDocumento();
    }

    protected abstract String titulo();

    protected abstract List<String> colunas();

    protected abstract List<List<String>> linhas();
}

class RelatorioVendas extends Relatorio {

    RelatorioVendas(RenderizadorRelatorio renderizador) {
        super(renderizador);
    }

    @Override
    protected String titulo() {
        return "Vendas do mês";
    }

    @Override
    protected List<String> colunas() {
        return List.of("Produto", "Quantidade", "Total");
    }

    @Override
    protected List<List<String>> linhas() {
        List<List<String>> linhas = new ArrayList<>();
        linhas.add(List.of("Teclado", "12", "3000,00"));
        linhas.add(List.of("Mouse", "40", "3200,00"));
        return linhas;
    }
}

class RelatorioEstoque extends Relatorio {

    RelatorioEstoque(RenderizadorRelatorio renderizador) {
        super(renderizador);
    }

    @Override
    protected String titulo() {
        return "Posição de estoque";
    }

    @Override
    protected List<String> colunas() {
        return List.of("SKU", "Disponível");
    }

    @Override
    protected List<List<String>> linhas() {
        List<List<String>> linhas = new ArrayList<>();
        linhas.add(List.of("TEC-001", "5"));
        linhas.add(List.of("MOU-002", "0"));
        return linhas;
    }
}

class AplicacaoRelatorios {

    public static void main(String[] args) {

        System.out.println(new RelatorioVendas(new RenderizadorHtml()).gerar());
        System.out.println(new RelatorioVendas(new RenderizadorCsv()).gerar());
        System.out.println(new RelatorioEstoque(new RenderizadorTextoPlano()).gerar());
    }
}

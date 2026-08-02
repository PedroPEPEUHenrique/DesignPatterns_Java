//Suponha que a sua tarefa seja gerar relatórios do sistema. Existem tipos de relatório:
//Relatório de vendas
//Relatório de estoque
//E existem formatos de saída:
//PDF, HTML, CSV

//Imagine a solução por herança: RelatorioVendasPdf, RelatorioVendasHtml, RelatorioVendasCsv,
//RelatorioEstoquePdf, RelatorioEstoqueHtml, RelatorioEstoqueCsv. São 2 x 3 = 6 classes, e um
//quarto formato passaria para 8. A hierarquia cresce pelo PRODUTO das duas dimensões - é a
//explosão combinatória de subclasses.

//O Bridge resolve o problema de desacoplar uma abstração da sua implementação, para que as duas
//variem de forma INDEPENDENTE. Em vez de multiplicar (2 x 3), passamos a somar (2 + 3).

import java.util.ArrayList;
import java.util.List;

// Padrão Bridge - o lado da IMPLEMENTAÇÃO (Implementor)
// Note que não é a interface do cliente: é um vocabulário primitivo de desenho, pensado para ser
// implementado, não para ser chamado por quem pede um relatório.
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

// Padrão Bridge - o lado da ABSTRAÇÃO
// A PONTE é este campo: a abstração COMPÕE um renderizador em vez de herdar dele. É por aqui que
// as duas hierarquias se conectam, e é o que permite trocar o formato em tempo de execução.
abstract class Relatorio {
    protected final RenderizadorRelatorio renderizador;

    protected Relatorio(RenderizadorRelatorio renderizador) {
        this.renderizador = renderizador;
    }

    // Operação de alto nível, escrita uma única vez em termos das primitivas do renderizador.
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

// Abstração refinada 1
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

// Abstração refinada 2
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

// Classe Cliente
class AplicacaoRelatorios {

    public static void main(String[] args) {
        // Qualquer relatório combina com qualquer formato, sem nenhuma classe nova.
        // Um formato novo é 1 classe; um relatório novo é 1 classe. Nunca 1 por combinação.
        System.out.println(new RelatorioVendas(new RenderizadorHtml()).gerar());
        System.out.println(new RelatorioVendas(new RenderizadorCsv()).gerar());
        System.out.println(new RelatorioEstoque(new RenderizadorTextoPlano()).gerar());
    }
}

//Bridge x Strategy: estruturalmente são iguais - um objeto delega a outro por composição. A
//diferença é de INTENÇÃO e de escala. O Strategy troca um algoritmo dentro de uma classe; o Bridge
//separa duas HIERARQUIAS inteiras que evoluem em ritmos diferentes, e essa separação é decidida
//no início do projeto, não como refatoração pontual.
//Bridge x Abstract Factory: os dois costumam aparecer juntos - uma fábrica abstrata é um bom lugar
//para escolher qual implementação concreta será ligada à abstração.

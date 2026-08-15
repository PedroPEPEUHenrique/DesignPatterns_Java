//Ao mudar a quantidade em estoque é preciso alertar o comprador, atualizar o painel e registrar
//em auditoria. Chamando os três diretamente, a classe de estoque passa a depender de e-mail,
//painel e auditoria - e o quarto interessado obriga a alterá-la.
//O Observer define uma dependência UM-PARA-MUITOS: quando um objeto muda de estado, todos os seus
//dependentes são notificados automaticamente.

import java.util.ArrayList;
import java.util.List;

class EventoEstoque {
    private final String sku;
    private final int quantidadeAnterior;
    private final int quantidadeAtual;
    private final int pontoReposicao;

    EventoEstoque(String sku, int quantidadeAnterior, int quantidadeAtual, int pontoReposicao) {
        this.sku = sku;
        this.quantidadeAnterior = quantidadeAnterior;
        this.quantidadeAtual = quantidadeAtual;
        this.pontoReposicao = pontoReposicao;
    }

    String getSku() {
        return sku;
    }

    int getQuantidadeAnterior() {
        return quantidadeAnterior;
    }

    int getQuantidadeAtual() {
        return quantidadeAtual;
    }

    int getPontoReposicao() {
        return pontoReposicao;
    }

    boolean cruzouPontoDeReposicao() {
        return quantidadeAnterior > pontoReposicao && quantidadeAtual <= pontoReposicao;
    }
}

interface ObservadorEstoque {
    void aoMudarEstoque(EventoEstoque evento);
}

class Estoque {

    private final String sku;
    private int quantidade;
    private final int pontoReposicao;
    private final List<ObservadorEstoque> observadores = new ArrayList<>();

    Estoque(String sku, int quantidade, int pontoReposicao) {
        this.sku = sku;
        this.quantidade = quantidade;
        this.pontoReposicao = pontoReposicao;
    }

    public void registrar(ObservadorEstoque observador) {
        observadores.add(observador);
    }

    public void remover(ObservadorEstoque observador) {
        observadores.remove(observador);
    }

    public void baixar(int unidades) {
        int anterior = quantidade;
        quantidade -= unidades;
        System.out.println("baixa de " + unidades + " em " + sku + ": " + anterior + " -> " + quantidade);

        notificar(new EventoEstoque(sku, anterior, quantidade, pontoReposicao));
    }

    public void repor(int unidades) {
        int anterior = quantidade;
        quantidade += unidades;
        System.out.println("reposição de " + unidades + " em " + sku + ": " + anterior + " -> " + quantidade);

        notificar(new EventoEstoque(sku, anterior, quantidade, pontoReposicao));
    }

    private void notificar(EventoEstoque evento) {

        for (ObservadorEstoque observador : new ArrayList<>(observadores)) {
            observador.aoMudarEstoque(evento);
        }
    }
}

class AlertaComprador implements ObservadorEstoque {

    @Override
    public void aoMudarEstoque(EventoEstoque evento) {
        if (evento.cruzouPontoDeReposicao()) {
            System.out.println("  [comprador] atenção: " + evento.getSku()
                               + " chegou a " + evento.getQuantidadeAtual()
                               + " (ponto de reposição " + evento.getPontoReposicao() + ")");
        }
    }
}

class PainelLoja implements ObservadorEstoque {

    @Override
    public void aoMudarEstoque(EventoEstoque evento) {
        String rotulo = evento.getQuantidadeAtual() <= 0 ? "ESGOTADO"
                      : evento.getQuantidadeAtual() < 5 ? "ÚLTIMAS UNIDADES"
                      : "DISPONÍVEL";
        System.out.println("  [painel] " + evento.getSku() + ": " + rotulo);
    }
}

class AuditoriaMovimentacao implements ObservadorEstoque {

    private int movimentacoes;

    @Override
    public void aoMudarEstoque(EventoEstoque evento) {
        movimentacoes++;
        System.out.println("  [auditoria] movimentação #" + movimentacoes + " em " + evento.getSku());
    }
}

class AvisoUnicoDeEsgotamento implements ObservadorEstoque {

    private final Estoque estoque;

    AvisoUnicoDeEsgotamento(Estoque estoque) {
        this.estoque = estoque;
    }

    @Override
    public void aoMudarEstoque(EventoEstoque evento) {
        if (evento.getQuantidadeAtual() <= 0) {
            System.out.println("  [aviso único] " + evento.getSku() + " esgotou, encerrando escuta");
            estoque.remover(this);
        }
    }
}

class MonitorEstoque {

    public static void main(String[] args) {
        Estoque teclado = new Estoque("TEC-001", 12, 5);

        teclado.registrar(new AlertaComprador());
        teclado.registrar(new PainelLoja());
        teclado.registrar(new AuditoriaMovimentacao());
        teclado.registrar(new AvisoUnicoDeEsgotamento(teclado));

        teclado.registrar(evento -> {
            if (evento.getQuantidadeAtual() < 0) {
                System.out.println("  [consistência] ERRO: estoque negativo em " + evento.getSku());
            }
        });

        teclado.baixar(5);
        System.out.println("---");
        teclado.baixar(3);
        System.out.println("---");
        teclado.baixar(4);
        System.out.println("---");
        teclado.baixar(1);
        System.out.println("---");
        teclado.repor(20);
    }
}

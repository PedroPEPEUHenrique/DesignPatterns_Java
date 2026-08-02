//Ao mudar a quantidade em estoque é preciso alertar o comprador, atualizar o painel e registrar
//em auditoria. Chamando os três diretamente, a classe de estoque passa a depender de e-mail,
//painel e auditoria - e o quarto interessado obriga a alterá-la.
//O Observer define uma dependência UM-PARA-MUITOS: quando um objeto muda de estado, todos os seus
//dependentes são notificados automaticamente.

import java.util.ArrayList;
import java.util.List;

// Modelo "push": passar um objeto de evento evita que cada observador consulte o sujeito de volta.
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

// Observer
interface ObservadorEstoque {
    void aoMudarEstoque(EventoEstoque evento);
}

// Subject: conhece apenas a interface. Essa ignorância é o que o mantém fechado para alteração.
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
        // Iterar sobre uma CÓPIA: um observador pode se descadastrar durante a notificação, e
        // remover da lista original em plena iteração lança ConcurrentModificationException.
        for (ObservadorEstoque observador : new ArrayList<>(observadores)) {
            observador.aoMudarEstoque(evento);
        }
    }
}

// Observadores concretos: cada um com sua responsabilidade, sem saber dos outros.

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

// Observador que se descadastra durante a notificação: o caso que justifica a cópia da lista.
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

// Cliente
class MonitorEstoque {

    public static void main(String[] args) {
        Estoque teclado = new Estoque("TEC-001", 12, 5);

        // A composição de observadores é decidida AQUI, fora da classe Estoque.
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
        teclado.baixar(3);   // cruza o ponto de reposição
        System.out.println("---");
        teclado.baixar(4);   // esgota: o aviso único dispara e sai da lista
        System.out.println("---");
        teclado.baixar(1);
        System.out.println("---");
        teclado.repor(20);
    }
}

//Pontos de atenção: o padrão não garante ORDEM de notificação; o sujeito guarda referência forte
//para cada observador, então quem não se descadastra vaza memória ("lapsed listener"); e um
//observador que altera o sujeito dispara nova notificação, podendo gerar recursão infinita.
//java.util.Observer/Observable foram DEPRECIADOS no Java 9. Hoje se usa interface própria,
//PropertyChangeListener, Flow.Publisher/Subscriber ou eventos CDI com @Observes.

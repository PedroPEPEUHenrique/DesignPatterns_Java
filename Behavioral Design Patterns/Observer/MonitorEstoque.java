//Suponha que a sua tarefa seja reagir à baixa de estoque de um produto. Quando a quantidade muda,
//é preciso:
//Alertar o comprador quando cruzar o ponto de reposição
//Atualizar o painel da loja
//Registrar a movimentação em auditoria

//Imagine o método baixarEstoque() chamando os três serviços diretamente. A classe de estoque, que
//deveria cuidar só de quantidade, passa a depender de e-mail, de painel e de auditoria. E o
//quarto interessado que aparecer amanhã obriga a alterá-la de novo - violação direta do princípio
//aberto/fechado.

//O Observer resolve o problema de definir uma dependência UM-PARA-MUITOS entre objetos, de modo
//que quando um objeto muda de estado todos os seus dependentes são notificados automaticamente.

import java.util.ArrayList;
import java.util.List;

// O evento. Passar um objeto de evento em vez de chamar getters no sujeito é o modelo "push":
// evita que cada observador tenha que consultar o sujeito de volta, e deixa explícito o que mudou.
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

// Padrão Observer - a interface Observer
interface ObservadorEstoque {
    void aoMudarEstoque(EventoEstoque evento);
}

// Padrão Observer - o Subject
// Conhece apenas a interface ObservadorEstoque. Não sabe quantos observadores existem, quem são,
// nem o que fazem - e é justamente essa ignorância que o mantém fechado para alteração.
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

// OBSERVADORES CONCRETOS - cada um com sua responsabilidade, sem saber da existência dos outros.

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

// Observador que se DESCADASTRA durante a notificação - o caso que justifica a cópia da lista.
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

// Classe Cliente
class MonitorEstoque {

    public static void main(String[] args) {
        Estoque teclado = new Estoque("TEC-001", 12, 5);

        // A composição de observadores é decidida AQUI, fora da classe Estoque.
        teclado.registrar(new AlertaComprador());
        teclado.registrar(new PainelLoja());
        teclado.registrar(new AuditoriaMovimentacao());
        teclado.registrar(new AvisoUnicoDeEsgotamento(teclado));

        // Um observador novo pode ser uma lambda: a interface tem um único método abstrato.
        teclado.registrar(evento -> {
            if (evento.getQuantidadeAtual() < 0) {
                System.out.println("  [consistência] ERRO: estoque negativo em " + evento.getSku());
            }
        });

        teclado.baixar(5);
        System.out.println("---");
        teclado.baixar(3);   // cruza o ponto de reposição: o comprador é alertado
        System.out.println("---");
        teclado.baixar(4);   // esgota: o aviso único dispara e sai da lista
        System.out.println("---");
        teclado.baixar(1);   // o aviso único já não aparece
        System.out.println("---");
        teclado.repor(20);
    }
}

//Pontos de atenção:
//Ordem de notificação - o padrão não garante nenhuma. Se um observador depende de outro ter
//  rodado antes, a modelagem está errada.
//Vazamento de memória - o sujeito guarda referência forte para cada observador. Observador que
//  não se descadastra impede a coleta de lixo; é o "lapsed listener problem".
//Notificação em cascata - um observador que altera o sujeito dispara nova notificação e pode
//  gerar recursão infinita.
//
//Na plataforma Java: java.util.Observer/Observable existem desde a versão 1.0, mas foram
//DEPRECIADOS no Java 9 por serem pouco flexíveis e não serializáveis com segurança. Hoje se usa
//uma interface própria (como acima), java.beans.PropertyChangeListener, Flow.Publisher/Subscriber
//(fluxos reativos, Java 9+) ou eventos CDI com @Observes.

//Suponha que a sua tarefa seja implementar a tela de cadastro de um pedido. Ela tem:
//Um campo de cliente
//Uma lista de itens
//Uma caixa "aplicar cupom"
//Um botão "finalizar"

//Imagine as regras de interação: escolher o cliente habilita a lista de itens; a lista vazia
//desabilita o botão; o cupom só aparece para cliente VIP. Se cada componente chamar diretamente os
//outros, todos passam a se conhecer - são n x n ligações. Nenhum componente pode ser reaproveitado
//em outra tela, porque cada um carrega junto o conhecimento dos demais.

//O Mediator resolve o problema de definir um objeto que ENCAPSULA como um conjunto de objetos
//interage. Os componentes deixam de se referenciar entre si e passam a falar só com o mediador,
//trocando um grafo de ligações por uma estrela.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Padrão Mediator - a interface do mediador
// A operação é genérica de propósito: "componente X sofreu o evento Y". Quem decide o que isso
// significa é o mediador concreto, não o componente.
interface MediadorTela {
    void notificar(Componente origem, String evento);
}

// Padrão Mediator - o Colleague
// Conhece SÓ o mediador. Não tem nenhuma referência aos outros componentes - esse é o ponto.
abstract class Componente {

    protected MediadorTela mediador;
    protected boolean habilitado = true;
    private final String nome;

    protected Componente(String nome) {
        this.nome = nome;
    }

    void setMediador(MediadorTela mediador) {
        this.mediador = mediador;
    }

    String getNome() {
        return nome;
    }

    void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
        System.out.println("  [" + nome + "] " + (habilitado ? "habilitado" : "desabilitado"));
    }

    boolean isHabilitado() {
        return habilitado;
    }
}

class CampoCliente extends Componente {

    private String clienteSelecionado = "";
    private boolean vip;

    CampoCliente() {
        super("campo cliente");
    }

    void selecionar(String cliente, boolean vip) {
        this.clienteSelecionado = cliente;
        this.vip = vip;
        System.out.println("usuário selecionou o cliente " + cliente + (vip ? " (VIP)" : ""));

        // O componente só anuncia o que aconteceu com ele. Não sabe quem se importa nem o que
        // deve mudar na tela por causa disso.
        mediador.notificar(this, "clienteSelecionado");
    }

    String getClienteSelecionado() {
        return clienteSelecionado;
    }

    boolean isVip() {
        return vip;
    }
}

class ListaItens extends Componente {

    private final List<String> itens = new ArrayList<>();

    ListaItens() {
        super("lista de itens");
    }

    void adicionar(String item) {
        if (!habilitado) {
            System.out.println("  [lista de itens] ignorado, componente desabilitado");
            return;
        }
        itens.add(item);
        System.out.println("usuário adicionou o item " + item);
        mediador.notificar(this, "itemAdicionado");
    }

    void limpar() {
        itens.clear();
        mediador.notificar(this, "listaLimpa");
    }

    int quantidade() {
        return itens.size();
    }
}

class CaixaCupom extends Componente {

    private boolean visivel;

    CaixaCupom() {
        super("caixa de cupom");
    }

    void setVisivel(boolean visivel) {
        this.visivel = visivel;
        System.out.println("  [caixa de cupom] " + (visivel ? "exibida" : "oculta"));
    }

    boolean isVisivel() {
        return visivel;
    }
}

class BotaoFinalizar extends Componente {

    BotaoFinalizar() {
        super("botão finalizar");
    }

    void clicar() {
        if (!habilitado) {
            System.out.println("  [botão finalizar] clique ignorado, botão desabilitado");
            return;
        }
        System.out.println("usuário clicou em finalizar");
        mediador.notificar(this, "finalizarClicado");
    }
}

// Padrão Mediator - o ConcreteMediator
// TODA a lógica de interação da tela mora aqui, em um lugar só e legível de cima a baixo.
// Antes ela estava espalhada em cada componente, na forma de chamadas cruzadas.
class MediadorTelaPedido implements MediadorTela {

    private final CampoCliente campoCliente;
    private final ListaItens listaItens;
    private final CaixaCupom caixaCupom;
    private final BotaoFinalizar botaoFinalizar;

    MediadorTelaPedido(CampoCliente campoCliente, ListaItens listaItens,
                       CaixaCupom caixaCupom, BotaoFinalizar botaoFinalizar) {
        this.campoCliente = campoCliente;
        this.listaItens = listaItens;
        this.caixaCupom = caixaCupom;
        this.botaoFinalizar = botaoFinalizar;

        // O mediador se registra em cada componente: é a única ligação que existe.
        for (Componente componente : List.of(campoCliente, listaItens, caixaCupom, botaoFinalizar)) {
            componente.setMediador(this);
        }

        estadoInicial();
    }

    private void estadoInicial() {
        System.out.println("estado inicial da tela:");
        listaItens.setHabilitado(false);
        botaoFinalizar.setHabilitado(false);
        caixaCupom.setVisivel(false);
    }

    @Override
    public void notificar(Componente origem, String evento) {
        if (origem == campoCliente && "clienteSelecionado".equals(evento)) {
            listaItens.setHabilitado(true);
            caixaCupom.setVisivel(campoCliente.isVip());
            botaoFinalizar.setHabilitado(listaItens.quantidade() > 0);
            return;
        }

        if (origem == listaItens) {
            botaoFinalizar.setHabilitado(listaItens.quantidade() > 0
                                         && !campoCliente.getClienteSelecionado().isEmpty());
            return;
        }

        if (origem == botaoFinalizar && "finalizarClicado".equals(evento)) {
            System.out.println("  >> pedido de " + campoCliente.getClienteSelecionado()
                               + " com " + listaItens.quantidade() + " itens enviado");
            listaItens.limpar();
        }
    }
}

// Um segundo mediador concreto: MESMOS componentes, política de tela diferente. Isso só é
// possível porque a regra de interação não está dentro dos componentes.
class MediadorTelaSimplificada implements MediadorTela {

    private final Map<String, Componente> componentes = new HashMap<>();

    MediadorTelaSimplificada(Componente... todos) {
        for (Componente componente : todos) {
            componente.setMediador(this);
            componentes.put(componente.getNome(), componente);
            componente.setHabilitado(true);   // nesta tela nada fica bloqueado
        }
    }

    @Override
    public void notificar(Componente origem, String evento) {
        System.out.println("  (tela simplificada ignora o evento " + evento
                           + " de " + origem.getNome() + ")");
    }
}

// Classe Cliente
class TelaCadastroPedido {

    public static void main(String[] args) {
        CampoCliente campoCliente = new CampoCliente();
        ListaItens listaItens = new ListaItens();
        CaixaCupom caixaCupom = new CaixaCupom();
        BotaoFinalizar botaoFinalizar = new BotaoFinalizar();

        new MediadorTelaPedido(campoCliente, listaItens, caixaCupom, botaoFinalizar);

        System.out.println("--- interação do usuário ---");
        listaItens.adicionar("Teclado");          // bloqueado: nenhum cliente escolhido ainda
        botaoFinalizar.clicar();                  // bloqueado pelo mesmo motivo

        campoCliente.selecionar("Ana", true);     // habilita a lista e revela o cupom
        listaItens.adicionar("Teclado");          // habilita o botão
        listaItens.adicionar("Mouse");
        botaoFinalizar.clicar();

        System.out.println("--- mesmos componentes, outro mediador ---");
        new MediadorTelaSimplificada(new CampoCliente(), new ListaItens(), new BotaoFinalizar());
    }
}

//Mediator x Observer: os dois reduzem acoplamento entre objetos, mas de formas diferentes. No
//Observer a comunicação é unidirecional e o emissor não sabe quem escuta - bom para notificação
//em difusão. No Mediator a comunicação é centralizada e o mediador CONHECE todos os colegas -
//bom quando existe uma lógica de coordenação com regras entre eles. É comum implementar o
//mediador usando Observer por baixo.
//
//O risco do padrão: o mediador tende a crescer. Se ele vira o único lugar com lógica do sistema,
//virou um God Object - a coordenação foi centralizada, mas a complexidade não diminuiu.
//No GRASP, essa mesma ideia aparece como Indirection e Low Coupling.

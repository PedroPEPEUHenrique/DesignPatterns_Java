//Numa tela de cadastro de pedido, escolher o cliente habilita a lista de itens, a lista vazia
//desabilita o botão e o cupom só aparece para cliente VIP. Se cada componente chamar os outros
//diretamente, são n x n ligações e nenhum componente é reaproveitável em outra tela.
//O Mediator encapsula em um objeto como um conjunto de objetos interage, trocando um grafo de
//ligações por uma estrela.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface MediadorTela {
    void notificar(Componente origem, String evento);
}

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

class MediadorTelaSimplificada implements MediadorTela {

    private final Map<String, Componente> componentes = new HashMap<>();

    MediadorTelaSimplificada(Componente... todos) {
        for (Componente componente : todos) {
            componente.setMediador(this);
            componentes.put(componente.getNome(), componente);
            componente.setHabilitado(true);
        }
    }

    @Override
    public void notificar(Componente origem, String evento) {
        System.out.println("  (tela simplificada ignora o evento " + evento
                           + " de " + origem.getNome() + ")");
    }
}

class TelaCadastroPedido {

    public static void main(String[] args) {
        CampoCliente campoCliente = new CampoCliente();
        ListaItens listaItens = new ListaItens();
        CaixaCupom caixaCupom = new CaixaCupom();
        BotaoFinalizar botaoFinalizar = new BotaoFinalizar();

        new MediadorTelaPedido(campoCliente, listaItens, caixaCupom, botaoFinalizar);

        System.out.println("--- interação do usuário ---");
        listaItens.adicionar("Teclado");
        botaoFinalizar.clicar();

        campoCliente.selecionar("Ana", true);
        listaItens.adicionar("Teclado");
        listaItens.adicionar("Mouse");
        botaoFinalizar.clicar();

        System.out.println("--- mesmos componentes, outro mediador ---");
        new MediadorTelaSimplificada(new CampoCliente(), new ListaItens(), new BotaoFinalizar());
    }
}

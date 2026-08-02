//Suponha que a sua tarefa seja controlar o ciclo de vida de um pedido. Os estados são:
//Novo -> Pago -> Enviado -> Entregue
//E de Novo ou Pago é possível ir para Cancelado.

//Imagine a solução com um campo "status" do tipo String e um if em cada operação:
//  if ("NOVO".equals(status)) { ... } else if ("PAGO".equals(status)) { ... }
//O mesmo encadeamento se repete em pagar(), enviar(), entregar() e cancelar(). Acrescentar o
//estado "Em separação" obriga a revisitar todos eles, e nada impede que um trecho esqueça um caso.

//O State resolve o problema de permitir que um objeto altere o seu comportamento quando o seu
//estado interno muda - o objeto parece mudar de classe. Cada estado vira uma CLASSE, e a
//transição vira a troca da referência para o objeto de estado.

import java.util.ArrayList;
import java.util.List;

// Padrão State - a interface State
// Declara toda operação cujo comportamento depende do estado. Cada estado concreto responde a
// todas elas - inclusive dizendo que não pode.
interface EstadoPedido {

    void pagar(Pedido pedido);

    void enviar(Pedido pedido);

    void entregar(Pedido pedido);

    void cancelar(Pedido pedido);

    String nome();
}

// Base com o comportamento padrão: recusar. Assim cada estado concreto sobrescreve só o que ele
// realmente PERMITE, e a lista de transições válidas fica evidente na leitura de cada classe.
abstract class EstadoBase implements EstadoPedido {

    @Override
    public void pagar(Pedido pedido) {
        recusar("pagar", pedido);
    }

    @Override
    public void enviar(Pedido pedido) {
        recusar("enviar", pedido);
    }

    @Override
    public void entregar(Pedido pedido) {
        recusar("entregar", pedido);
    }

    @Override
    public void cancelar(Pedido pedido) {
        recusar("cancelar", pedido);
    }

    protected void recusar(String operacao, Pedido pedido) {
        System.out.println("  não é possível " + operacao + " um pedido " + nome());
    }
}

class EstadoNovo extends EstadoBase {

    @Override
    public void pagar(Pedido pedido) {
        System.out.println("  pagamento capturado");
        pedido.mudarPara(new EstadoPago());   // é o estado que decide a transição
    }

    @Override
    public void cancelar(Pedido pedido) {
        System.out.println("  pedido cancelado antes do pagamento");
        pedido.mudarPara(new EstadoCancelado());
    }

    @Override
    public String nome() {
        return "novo";
    }
}

class EstadoPago extends EstadoBase {

    @Override
    public void enviar(Pedido pedido) {
        System.out.println("  etiqueta emitida, pedido despachado");
        pedido.mudarPara(new EstadoEnviado());
    }

    @Override
    public void cancelar(Pedido pedido) {
        // O comportamento de cancelar MUDA conforme o estado: aqui há estorno, no estado novo não.
        // Com if/else isso viraria um if dentro de outro if.
        System.out.println("  cancelamento com estorno do pagamento");
        pedido.mudarPara(new EstadoCancelado());
    }

    @Override
    public String nome() {
        return "pago";
    }
}

class EstadoEnviado extends EstadoBase {

    @Override
    public void entregar(Pedido pedido) {
        System.out.println("  entrega confirmada pelo destinatário");
        pedido.mudarPara(new EstadoEntregue());
    }

    @Override
    public String nome() {
        return "enviado";
    }
}

// Estados terminais: herdam tudo da base, ou seja, recusam qualquer transição.
class EstadoEntregue extends EstadoBase {

    @Override
    public String nome() {
        return "entregue";
    }
}

class EstadoCancelado extends EstadoBase {

    @Override
    public String nome() {
        return "cancelado";
    }
}

// Padrão State - o Context
// Repare no que sumiu: não há um único if de status nesta classe. Ela apenas DELEGA ao objeto de
// estado atual. Acrescentar um estado novo não muda uma linha aqui.
class Pedido {

    private final String codigo;
    private EstadoPedido estado = new EstadoNovo();
    private final List<String> trilha = new ArrayList<>();

    Pedido(String codigo) {
        this.codigo = codigo;
        trilha.add("novo");
    }

    // Chamado pelos estados. Em um sistema real seria de visibilidade restrita ao pacote, para
    // que só os estados pudessem provocar transições.
    void mudarPara(EstadoPedido novoEstado) {
        System.out.println("  [" + estado.nome() + " -> " + novoEstado.nome() + "]");
        this.estado = novoEstado;
        trilha.add(novoEstado.nome());
    }

    public void pagar() {
        System.out.println(codigo + ": pagar");
        estado.pagar(this);
    }

    public void enviar() {
        System.out.println(codigo + ": enviar");
        estado.enviar(this);
    }

    public void entregar() {
        System.out.println(codigo + ": entregar");
        estado.entregar(this);
    }

    public void cancelar() {
        System.out.println(codigo + ": cancelar");
        estado.cancelar(this);
    }

    public String getEstadoAtual() {
        return estado.nome();
    }

    public List<String> getTrilha() {
        return trilha;
    }
}

// Classe Cliente
class CicloDeVidaPedido {

    public static void main(String[] args) {
        Pedido feliz = new Pedido("PED-1");
        feliz.pagar();
        feliz.enviar();
        feliz.entregar();
        feliz.cancelar();     // recusado: entregue é terminal
        System.out.println("trilha: " + feliz.getTrilha());

        System.out.println("---");

        Pedido foraDeOrdem = new Pedido("PED-2");
        foraDeOrdem.enviar();    // recusado: ainda não foi pago
        foraDeOrdem.entregar();  // recusado
        foraDeOrdem.pagar();
        foraDeOrdem.cancelar();  // permitido, e com estorno
        foraDeOrdem.pagar();     // recusado: cancelado é terminal
        System.out.println("trilha: " + foraDeOrdem.getTrilha());
        System.out.println("estado final: " + foraDeOrdem.getEstadoAtual());
    }
}

//State x Strategy: estruturalmente idênticos - um contexto que delega a um objeto trocável. As
//diferenças são de intenção e de quem troca:
//No Strategy, quem escolhe o algoritmo é o CLIENTE, de fora, e as estratégias não se conhecem.
//No State, quem escolhe o próximo estado é o PRÓPRIO ESTADO, de dentro, e por isso os estados
//conhecem uns aos outros e formam um grafo de transições.
//
//Variação comum: estados sem campos podem ser singletons ou constantes de enum, evitando criar um
//objeto a cada transição. Em Java, um enum que implementa a interface de estado, com cada
//constante sobrescrevendo os métodos, é uma forma bastante idiomática de escrever este padrão.

//Controlar o ciclo de vida de um pedido: Novo -> Pago -> Enviado -> Entregue, com cancelamento a
//partir de Novo ou Pago. Com um campo status e um if em cada operação, o mesmo encadeamento se
//repete em pagar(), enviar(), entregar() e cancelar().
//O State permite que um objeto altere o seu comportamento quando o estado interno muda - o objeto
//parece mudar de classe. Cada estado vira uma CLASSE e a transição vira a troca da referência.

import java.util.ArrayList;
import java.util.List;

interface EstadoPedido {

    void pagar(Pedido pedido);

    void enviar(Pedido pedido);

    void entregar(Pedido pedido);

    void cancelar(Pedido pedido);

    String nome();
}

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
        pedido.mudarPara(new EstadoPago());
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

class Pedido {

    private final String codigo;
    private EstadoPedido estado = new EstadoNovo();
    private final List<String> trilha = new ArrayList<>();

    Pedido(String codigo) {
        this.codigo = codigo;
        trilha.add("novo");
    }

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

class CicloDeVidaPedido {

    public static void main(String[] args) {
        Pedido feliz = new Pedido("PED-1");
        feliz.pagar();
        feliz.enviar();
        feliz.entregar();
        feliz.cancelar();
        System.out.println("trilha: " + feliz.getTrilha());

        System.out.println("---");

        Pedido foraDeOrdem = new Pedido("PED-2");
        foraDeOrdem.enviar();
        foraDeOrdem.entregar();
        foraDeOrdem.pagar();
        foraDeOrdem.cancelar();
        foraDeOrdem.pagar();
        System.out.println("trilha: " + foraDeOrdem.getTrilha());
        System.out.println("estado final: " + foraDeOrdem.getEstadoAtual());
    }
}

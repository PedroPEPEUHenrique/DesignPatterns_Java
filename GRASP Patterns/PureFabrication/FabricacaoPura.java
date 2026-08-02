//GRASP - PURE FABRICATION
//Problema: e quando seguir o Expert leva a um desenho ruim?
//Solução: atribuir um conjunto coeso de responsabilidades a uma classe ARTIFICIAL, que não
//representa nada do domínio.
//Exemplo: pelo Expert, gravar o pedido seria responsabilidade do próprio Pedido, que tem os dados.
//Mas isso o faria conhecer JDBC e transação, misturando negócio com persistência. É o caso em que
//se DESOBEDECE o Expert conscientemente.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ItemPedido {
    private final String sku;
    private final int quantidade;
    private final int precoUnitarioEmCentavos;

    ItemPedido(String sku, int quantidade, int precoUnitarioEmCentavos) {
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitarioEmCentavos = precoUnitarioEmCentavos;
    }

    int subtotalEmCentavos() {
        return quantidade * precoUnitarioEmCentavos;
    }

    String getSku() {
        return sku;
    }

    int getQuantidade() {
        return quantidade;
    }
}

// Pedido continua sendo APENAS pedido: nenhuma linha de SQL, e-mail ou arquivo aqui dentro.
class Pedido {

    private final String codigo;
    private final String cpfCliente;
    private final List<ItemPedido> itens = new ArrayList<>();

    Pedido(String codigo, String cpfCliente) {
        this.codigo = codigo;
        this.cpfCliente = cpfCliente;
    }

    public void adicionar(String sku, int quantidade, int precoUnitarioEmCentavos) {
        itens.add(new ItemPedido(sku, quantidade, precoUnitarioEmCentavos));
    }

    // Expert aplicado corretamente: quem tem os itens calcula o total.
    public int totalEmCentavos() {
        int total = 0;
        for (ItemPedido item : itens) {
            total += item.subtotalEmCentavos();
        }
        return total;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCpfCliente() {
        return cpfCliente;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}

// Fabricação pura 1 - Repositório
// Nenhum usuário fala em "repositório": foi inventado para concentrar a persistência num lugar
// coeso, mantendo Pedido limpo.
interface PedidoRepository {

    void salvar(Pedido pedido);

    Pedido porCodigo(String codigo);

    List<Pedido> porCliente(String cpf);
}

class PedidoRepositoryEmMemoria implements PedidoRepository {

    private final Map<String, Pedido> base = new HashMap<>();

    @Override
    public void salvar(Pedido pedido) {
        base.put(pedido.getCodigo(), pedido);
        System.out.println("  [repo] pedido " + pedido.getCodigo() + " gravado");
    }

    @Override
    public Pedido porCodigo(String codigo) {
        return base.get(codigo);
    }

    @Override
    public List<Pedido> porCliente(String cpf) {
        List<Pedido> encontrados = new ArrayList<>();
        for (Pedido pedido : base.values()) {
            if (pedido.getCpfCliente().equals(cpf)) {
                encontrados.add(pedido);
            }
        }
        return encontrados;
    }
}

// Fabricação pura 2 - Serviço de domínio
// Regra que envolve MAIS DE UMA entidade e não pertence a nenhuma: o frete em Pedido o acoplaria à
// tabela de transportadoras, e na transportadora o acoplaria a pedido.
class CalculoFreteService {

    private final Map<String, Integer> tabelaPorRegiao = Map.of(
            "SUDESTE", 1800, "SUL", 2200, "NORDESTE", 3400);

    public int calcularEmCentavos(Pedido pedido, String regiao) {
        int base = tabelaPorRegiao.getOrDefault(regiao, 5000);
        int porItem = 0;
        for (ItemPedido item : pedido.getItens()) {
            porItem += item.getQuantidade() * 150;
        }
        return pedido.totalEmCentavos() > 30000 ? 0 : base + porItem;
    }
}

// Fabricação pura 3 - Assembler
// Sem ela, Pedido teria um "paraJson()" e passaria a conhecer o formato do canal.
class PedidoJsonAssembler {

    public String paraJson(Pedido pedido) {
        StringBuilder json = new StringBuilder("{\"codigo\":\"" + pedido.getCodigo()
                                               + "\",\"total\":" + pedido.totalEmCentavos()
                                               + ",\"itens\":[");
        for (int i = 0; i < pedido.getItens().size(); i++) {
            ItemPedido item = pedido.getItens().get(i);
            json.append(i > 0 ? "," : "")
                .append("{\"sku\":\"").append(item.getSku())
                .append("\",\"qtd\":").append(item.getQuantidade()).append("}");
        }
        return json.append("]}").toString();
    }
}

// Fabricação pura 4 - utilitário sem estado. java.lang.Math é exatamente isso.
final class Dinheiro {

    private Dinheiro() {
    }

    static String formatar(int valorEmCentavos) {
        return String.format("R$ %d,%02d", valorEmCentavos / 100, Math.abs(valorEmCentavos % 100));
    }
}

class FabricacaoPura {

    public static void main(String[] args) {
        Pedido pedido = new Pedido("PED-1", "111.222.333-44");
        pedido.adicionar("TEC-001", 1, 25000);
        pedido.adicionar("MOU-002", 2, 8000);

        System.out.println("total: " + Dinheiro.formatar(pedido.totalEmCentavos()));

        PedidoRepository repositorio = new PedidoRepositoryEmMemoria();
        repositorio.salvar(pedido);

        int frete = new CalculoFreteService().calcularEmCentavos(pedido, "NORDESTE");
        System.out.println("frete: " + Dinheiro.formatar(frete));

        System.out.println("json: " + new PedidoJsonAssembler().paraJson(pedido));

        System.out.println("pedidos do cliente: "
                           + repositorio.porCliente("111.222.333-44").size());

        Pedido pequeno = new Pedido("PED-2", "555.666.777-88");
        pequeno.adicionar("MOU-002", 1, 8000);
        repositorio.salvar(pequeno);
        System.out.println("frete do pedido pequeno: "
                           + Dinheiro.formatar(new CalculoFreteService()
                                   .calcularEmCentavos(pequeno, "NORDESTE")));
    }
}

//Nomes que indicam fabricação pura: Repository, DAO, Service, Factory, Assembler, Validator,
//Mapper, Adapter, Controller - nenhum existe no vocabulário do usuário.
//O risco: usá-la em excesso esvazia o domínio. Se TODA regra vira um "Service" e as entidades
//ficam só com getters, voltamos ao modelo anêmico. Tente o Expert primeiro; fabrique só quando
//segui-lo prejudicar de fato a coesão ou o acoplamento.
//Praticamente todo padrão do GoF é uma fabricação pura - nenhum representa conceito do domínio.

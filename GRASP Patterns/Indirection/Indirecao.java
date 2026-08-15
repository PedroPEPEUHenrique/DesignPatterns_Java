//GRASP - INDIRECTION
//Problema: onde atribuir uma responsabilidade para evitar o acoplamento direto entre dois
//elementos?
//Solução: a um objeto INTERMEDIÁRIO, que faz a mediação entre eles e absorve a mudança.
//"Todo problema em computação pode ser resolvido com mais um nível de indireção" - e o custo é
//sempre o mesmo: mais um nível de indireção para entender e manter.

import java.util.ArrayList;
import java.util.List;

class Venda {
    private final String codigo;
    private final int valorEmCentavos;
    private final String centroCusto;

    Venda(String codigo, int valorEmCentavos, String centroCusto) {
        this.codigo = codigo;
        this.valorEmCentavos = valorEmCentavos;
        this.centroCusto = centroCusto;
    }

    String getCodigo() {
        return codigo;
    }

    int getValorEmCentavos() {
        return valorEmCentavos;
    }

    String getCentroCusto() {
        return centroCusto;
    }
}

class ContabilidadeLegada {

    public void postEntry(String accountCode, long amountCents, String costCenterId, char debitCredit) {
        System.out.println("  [legado] conta=" + accountCode + " valor=" + amountCents
                           + " cc=" + costCenterId + " dc=" + debitCredit);
    }
}

class ServicoVendasAcoplado {

    private final ContabilidadeLegada contabilidade = new ContabilidadeLegada();

    public void registrar(Venda venda) {
        contabilidade.postEntry("4.1.01", venda.getValorEmCentavos(),
                                "CC-" + venda.getCentroCusto(), 'C');
    }
}

interface LancamentoContabil {
    void lancarReceita(Venda venda);
}

class AdaptadorContabilidadeLegada implements LancamentoContabil {

    private final ContabilidadeLegada legado = new ContabilidadeLegada();

    @Override
    public void lancarReceita(Venda venda) {
        legado.postEntry("4.1.01", venda.getValorEmCentavos(),
                         "CC-" + venda.getCentroCusto(), 'C');
    }
}

class ContabilidadeNuvem {

    public String createJournalEntry(String json) {
        System.out.println("  [nuvem] POST /journal-entries " + json);
        return "JE-" + Math.abs(json.hashCode() % 10000);
    }
}

class AdaptadorContabilidadeNuvem implements LancamentoContabil {

    private final ContabilidadeNuvem nuvem = new ContabilidadeNuvem();

    @Override
    public void lancarReceita(Venda venda) {
        nuvem.createJournalEntry("{\"account\":\"REVENUE\",\"amount\":"
                                 + venda.getValorEmCentavos() / 100.0
                                 + ",\"costCenter\":\"" + venda.getCentroCusto() + "\"}");
    }
}

class LancamentoEmParalelo implements LancamentoContabil {

    private final List<LancamentoContabil> destinos = new ArrayList<>();

    LancamentoEmParalelo(LancamentoContabil... destinos) {
        for (LancamentoContabil destino : destinos) {
            this.destinos.add(destino);
        }
    }

    @Override
    public void lancarReceita(Venda venda) {
        for (LancamentoContabil destino : destinos) {
            destino.lancarReceita(venda);
        }
    }
}

class LancamentoComRegistro implements LancamentoContabil {

    private final LancamentoContabil delegado;
    private final List<String> trilha = new ArrayList<>();

    LancamentoComRegistro(LancamentoContabil delegado) {
        this.delegado = delegado;
    }

    @Override
    public void lancarReceita(Venda venda) {
        trilha.add(venda.getCodigo());
        delegado.lancarReceita(venda);
    }

    List<String> getTrilha() {
        return trilha;
    }
}

class ServicoVendas {

    private final LancamentoContabil contabilidade;

    ServicoVendas(LancamentoContabil contabilidade) {
        this.contabilidade = contabilidade;
    }

    public void concluir(Venda venda) {
        System.out.println("venda " + venda.getCodigo() + " concluída");
        contabilidade.lancarReceita(venda);
    }
}

class Indirecao {

    public static void main(String[] args) {
        Venda venda = new Venda("VND-1", 450000, "10023");

        System.out.println("== acoplado ao legado ==");
        new ServicoVendasAcoplado().registrar(venda);

        System.out.println("== com indireção, apontando para o legado ==");
        new ServicoVendas(new AdaptadorContabilidadeLegada()).concluir(venda);

        System.out.println("== mesma classe de vendas, outro sistema contábil ==");
        new ServicoVendas(new AdaptadorContabilidadeNuvem()).concluir(venda);

        System.out.println("== durante a migração, os dois ao mesmo tempo ==");
        LancamentoComRegistro comRegistro = new LancamentoComRegistro(
                new LancamentoEmParalelo(new AdaptadorContabilidadeLegada(),
                                         new AdaptadorContabilidadeNuvem()));

        ServicoVendas servico = new ServicoVendas(comRegistro);
        servico.concluir(venda);
        servico.concluir(new Venda("VND-2", 120000, "10023"));

        System.out.println("trilha do intermediário: " + comRegistro.getTrilha());
        System.out.println("nenhuma dessas variações alterou a classe ServicoVendas");
    }
}

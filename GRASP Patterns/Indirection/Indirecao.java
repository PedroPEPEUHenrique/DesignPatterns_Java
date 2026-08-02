//GRASP - INDIRECTION (Indireção)

//Problema: onde atribuir uma responsabilidade para evitar o acoplamento direto entre dois (ou
//muitos) elementos?
//Solução: atribua a responsabilidade a um objeto INTERMEDIÁRIO, que faça a mediação entre eles.
//Assim os dois deixam de se conhecer, e o intermediário absorve a mudança.

//Suponha que a sua tarefa seja registrar as vendas na contabilidade. Hoje o serviço de vendas
//chama diretamente a API do sistema contábil legado.
//Imagine que a empresa troque o sistema contábil, que passe a existir um segundo sistema para a
//filial no exterior, e que durante a migração os dois precisem receber o mesmo lançamento. Com a
//chamada direta, cada uma dessas mudanças invade o serviço de vendas.

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

// O SISTEMA EXTERNO - fora do nosso controle, com uma API esquisita e instável.
class ContabilidadeLegada {

    public void postEntry(String accountCode, long amountCents, String costCenterId, char debitCredit) {
        System.out.println("  [legado] conta=" + accountCode + " valor=" + amountCents
                           + " cc=" + costCenterId + " dc=" + debitCredit);
    }
}

// COMO NÃO FAZER - acoplamento direto
// Repare no que vazou para cá: o código contábil "4.1.01", a unidade (a API usa long), o caractere
// 'C' de crédito e o formato do centro de custo. Nada disso é assunto de vendas.
class ServicoVendasAcoplado {

    private final ContabilidadeLegada contabilidade = new ContabilidadeLegada();

    public void registrar(Venda venda) {
        contabilidade.postEntry("4.1.01", venda.getValorEmCentavos(),
                                "CC-" + venda.getCentroCusto(), 'C');
    }
}

// COMO FAZER - um intermediário
// A abstração fala a linguagem do NOSSO domínio, não a do sistema externo.
interface LancamentoContabil {
    void lancarReceita(Venda venda);
}

// O INTERMEDIÁRIO (aqui no papel de Adapter): é o único ponto do sistema que conhece a API legada.
// Se a API mudar, muda só esta classe.
class AdaptadorContabilidadeLegada implements LancamentoContabil {

    private final ContabilidadeLegada legado = new ContabilidadeLegada();

    @Override
    public void lancarReceita(Venda venda) {
        legado.postEntry("4.1.01", venda.getValorEmCentavos(),
                         "CC-" + venda.getCentroCusto(), 'C');
    }
}

// Segundo sistema contábil, com API totalmente diferente. O serviço de vendas não fica sabendo.
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

// Outro uso da indireção: um intermediário que fala com VÁRIOS destinos. Durante a migração, os
// dois sistemas recebem o mesmo lançamento - e nem o serviço de vendas nem os adaptadores mudam.
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

// E outro: um intermediário que acrescenta comportamento sem que nenhuma das pontas saiba.
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

// O cliente, agora desacoplado. Ele conhece UMA interface do próprio domínio e mais nada.
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

// Classe Cliente
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

//Indireção é o mecanismo por trás de muitos padrões do GoF, com intenções diferentes:
//Adapter - o intermediário TRADUZ uma interface incompatível (o caso acima).
//Facade - o intermediário SIMPLIFICA o acesso a um subsistema inteiro.
//Proxy - o intermediário CONTROLA o acesso ao objeto real.
//Mediator - o intermediário COORDENA a interação entre vários objetos.
//Observer - o registro de observadores é a indireção entre quem emite e quem escuta.
//
//O custo: cada camada de indireção é mais uma classe para navegar ao ler o código e mais um salto
//para depurar. Indireção resolve acoplamento, mas não é de graça - use quando houver uma variação
//real a proteger, não "por precaução".

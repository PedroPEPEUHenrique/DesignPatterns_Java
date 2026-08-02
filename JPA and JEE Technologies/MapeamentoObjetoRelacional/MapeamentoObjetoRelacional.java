//JPA - MAPEAMENTO OBJETO-RELACIONAL
//NÃO compila sem jakarta.persistence-api no classpath. Em Java EE 8 e anteriores o pacote é
//javax.persistence.
//O modelo orientado a objetos e o relacional não se encaixam ("impedance mismatch"). A JPA é uma
//especificação (Hibernate, EclipseLink) que resolve isso por METADADOS: você anota as classes e o
//provedor gera o SQL.

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// Requisitos que a especificação impõe a uma entidade:
//  1. @Entity  2. construtor SEM ARGUMENTOS  3. um @Id
//  4. a classe não pode ser final - o provedor gera uma subclasse proxy para o carregamento LAZY
@Entity
@Table(name = "cliente")
class Cliente {

    // Estratégias de @GeneratedValue:
    //  IDENTITY - auto-incremento; simples, mas impede batch de inserts
    //  SEQUENCE - preferida quando o banco suporta (PostgreSQL, Oracle)
    //  TABLE    - portável, porém lenta e com contenção
    //  AUTO     - o provedor escolhe
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nome;

    // unique = true gera a constraint no DDL, mas NÃO é validado em memória: a violação só aparece
    // como exceção do banco no flush.
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    // SEMPRE EnumType.STRING. O padrão é ORDINAL, que grava a POSIÇÃO - inserir um valor no meio do
    // enum corrompe silenciosamente todos os registros gravados.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCliente categoria = CategoriaCliente.COMUM;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // @Embedded incorpora um objeto de valor NAS COLUNAS DA PRÓPRIA TABELA.
    @Embedded
    private Endereco endereco;

    // mappedBy diz que o DONO é o outro lado (o campo "cliente" em Pedido) - o dono tem a chave
    // estrangeira e é o único lado que o provedor observa para gravar.
    // orphanRemoval apaga o filho ao sair da coleção; CascadeType.REMOVE só age quando o PAI é
    // removido.
    @OneToMany(mappedBy = "cliente",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PerfilCredito perfilCredito;

    // Bloqueio OTIMISTA: o provedor acrescenta "AND versao = ?" ao UPDATE. Se outra transação já
    // alterou a linha, lança OptimisticLockException - proteção contra "lost update" sem travar.
    @Version
    private Integer versao;

    protected Cliente() {
    }

    public Cliente(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    // A JPA NÃO sincroniza os dois lados sozinha: só cliente.getPedidos().add(pedido) deixa
    // pedido.cliente nulo e a FK vai nula para o banco.
    public void adicionarPedido(Pedido pedido) {
        pedidos.add(pedido);
        pedido.setCliente(this);
    }

    public void removerPedido(Pedido pedido) {
        pedidos.remove(pedido);
        pedido.setCliente(null);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public CategoriaCliente getCategoria() {
        return categoria;
    }

    public void promoverParaVip() {
        this.categoria = CategoriaCliente.VIP;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    // equals/hashCode com o id gerado quebra (nulo antes do persist, muda de hash depois); com
    // todos os campos também quebra. Use chave de NEGÓCIO imutável ou UUID atribuído pela aplicação.
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Cliente)) {
            return false;
        }
        return cpf != null && cpf.equals(((Cliente) outro).cpf);
    }

    @Override
    public int hashCode() {
        return cpf == null ? 0 : cpf.hashCode();
    }
}

enum CategoriaCliente {
    COMUM, VIP, CORPORATIVO
}

// @Embeddable: sem identidade própria, identificado pelo conteúdo e gravado na tabela do dono.
// É o Value Object do DDD - sem ele, os cinco campos de endereço ficariam soltos em Cliente.
@Embeddable
class Endereco {

    @Column(name = "logradouro", length = 150)
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "cidade", length = 80)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep", length = 8)
    private String cep;

    protected Endereco() {
    }

    Endereco(String logradouro, String numero, String cidade, String uf, String cep) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.cidade = cidade;
        this.uf = uf;
        this.cep = cep;
    }

    String resumo() {
        return logradouro + ", " + numero + " - " + cidade + "/" + uf;
    }
}

@Entity
@Table(name = "pedido")
class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    // Lado DONO: aqui fica a coluna de chave estrangeira.
    // ATENÇÃO: em @ManyToOne e @OneToOne o padrão da especificação é EAGER, uma das principais
    // causas de lentidão. Declare LAZY e traga o necessário com JOIN FETCH.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Em @OneToMany e @ManyToMany o padrão já é LAZY.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    // Na prática é quase sempre melhor transformar a junção em ENTIDADE própria: assim que o
    // negócio pedir um atributo no relacionamento, o @ManyToMany não serve mais.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "pedido_cupom",
               joinColumns = @JoinColumn(name = "pedido_id"),
               inverseJoinColumns = @JoinColumn(name = "cupom_id"))
    private List<Cupom> cupons = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status = StatusPedido.NOVO;

    @Version
    private Integer versao;

    protected Pedido() {
    }

    Pedido(String codigo) {
        this.codigo = codigo;
    }

    // A JPA não obriga a esvaziar a entidade em getters e setters: a regra de negócio permanece.
    BigDecimal totalEmReais() {
        return itens.stream()
                    .map(ItemPedido::subtotalEmReais)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    void adicionarItem(ItemPedido item) {
        itens.add(item);
        item.setPedido(this);
    }

    void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    Cliente getCliente() {
        return cliente;
    }

    String getCodigo() {
        return codigo;
    }

    List<ItemPedido> getItens() {
        return itens;
    }

    StatusPedido getStatus() {
        return status;
    }

    void avancarPara(StatusPedido novoStatus) {
        this.status = novoStatus;
    }
}

enum StatusPedido {
    NOVO, PAGO, ENVIADO, ENTREGUE, CANCELADO
}

@Entity
@Table(name = "item_pedido")
class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    // Valor monetário: BigDecimal com precision/scale, NUNCA double - ponto flutuante binário não
    // representa 0,10 exatamente e o erro se acumula.
    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {
    }

    ItemPedido(Produto produto, int quantidade, BigDecimal precoUnitario) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    BigDecimal subtotalEmReais() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }
}

@Entity
@Table(name = "produto")
class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    protected Produto() {
    }

    Produto(String sku, String nome, BigDecimal preco) {
        this.sku = sku;
        this.nome = nome;
        this.preco = preco;
    }

    BigDecimal getPreco() {
        return preco;
    }

    String getNome() {
        return nome;
    }
}

@Entity
@Table(name = "cupom")
class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    protected Cupom() {
    }

    Cupom(String codigo) {
        this.codigo = codigo;
    }
}

@Entity
@Table(name = "perfil_credito")
class PerfilCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lado DONO do @OneToOne: a FK fica nesta tabela.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "limite", precision = 19, scale = 2)
    private BigDecimal limite;

    protected PerfilCredito() {
    }
}

// HERANÇA - tabelas não têm herança. As três estratégias:
//  SINGLE_TABLE (padrão) - uma tabela e uma coluna discriminadora; a mais rápida, mas as colunas
//    das subclasses precisam aceitar NULL, o que impede constraints NOT NULL
//  JOINED - uma tabela por classe, ligadas pela PK; normalizado, ao custo de uma junção por nível
//  TABLE_PER_CLASS - uma tabela por classe concreta; consultas polimórficas viram UNION e a
//    geração de id fica limitada (IDENTITY não serve)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "pagamento")
abstract class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    protected Pagamento() {
    }

    // Polimorfismo preservado no modelo persistente: nenhum switch por tipo.
    abstract BigDecimal taxaEmReais();

    BigDecimal getValor() {
        return valor;
    }
}

@Entity
@Table(name = "pagamento_cartao")
class PagamentoCartao extends Pagamento {

    @Column(name = "bandeira", length = 20)
    private String bandeira;

    @Column(name = "parcelas")
    private Integer parcelas;

    protected PagamentoCartao() {
    }

    @Override
    BigDecimal taxaEmReais() {
        return getValor().multiply(new BigDecimal("0.0299"));
    }
}

@Entity
@Table(name = "pagamento_pix")
class PagamentoPix extends Pagamento {

    @Column(name = "chave", length = 80)
    private String chave;

    protected PagamentoPix() {
    }

    @Override
    BigDecimal taxaEmReais() {
        return BigDecimal.ZERO;
    }
}

//Outras anotações: @Transient (não persistir), @Lob (textos e binários grandes),
//@AttributeOverride (renomear colunas de um @Embeddable), @DiscriminatorColumn, @NamedQuery.
//
//As três armadilhas mais comuns:
//1. EnumType.ORDINAL (o padrão) corrompendo dados quando o enum muda.
//2. @ManyToOne EAGER por padrão, trazendo meia base junto em cada consulta.
//3. Relacionamento bidirecional sem método de sincronização, gravando FK nula.

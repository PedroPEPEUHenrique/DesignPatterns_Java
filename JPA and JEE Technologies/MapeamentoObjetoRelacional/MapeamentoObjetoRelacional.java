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

@Entity
@Table(name = "cliente")
class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCliente categoria = CategoriaCliente.COMUM;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Embedded
    private Endereco endereco;

    @OneToMany(mappedBy = "cliente",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PerfilCredito perfilCredito;

    @Version
    private Integer versao;

    protected Cliente() {
    }

    public Cliente(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "limite", precision = 19, scale = 2)
    private BigDecimal limite;

    protected PerfilCredito() {
    }
}

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

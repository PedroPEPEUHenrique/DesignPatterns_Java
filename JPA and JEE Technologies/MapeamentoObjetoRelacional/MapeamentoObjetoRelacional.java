//JPA - MAPEAMENTO OBJETO-RELACIONAL

//ATENÇÃO: este arquivo é material de estudo e NÃO compila sozinho. Ele depende da API
//jakarta.persistence-api no classpath. Em projetos antigos (Java EE 8 e anteriores) o pacote
//chama-se javax.persistence; a partir do Jakarta EE 9 passou a ser jakarta.persistence, e essa é
//a única diferença na maior parte do código.

//O problema que a JPA resolve: o modelo ORIENTADO A OBJETOS e o modelo RELACIONAL não se
//encaixam. Objetos têm herança, referências, identidade e navegação; tabelas têm colunas, chaves
//estrangeiras e junções. Essa distância é chamada de "impedance mismatch", e escrevê-la à mão em
//JDBC significa repetir INSERT, UPDATE, SELECT e a conversão de cada campo em cada classe.

//A JPA é uma ESPECIFICAÇÃO (Hibernate, EclipseLink e OpenJPA são implementações dela) que resolve
//isso por METADADOS: você anota as classes e o provedor gera o SQL. É Protected Variations
//aplicado à camada de dados - o código de domínio fica protegido do dialeto SQL de cada banco.

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

// ENTIDADE - a unidade básica do mapeamento.
// Requisitos que a especificação impõe e que costumam cair em prova:
//  1. anotada com @Entity (ou declarada no orm.xml)
//  2. construtor SEM ARGUMENTOS, público ou protegido - o provedor instancia por reflexão
//  3. um identificador (@Id)
//  4. a classe não pode ser final, nem podem ser final os métodos/atributos persistentes -
//     o provedor precisa gerar uma subclasse proxy para o carregamento preguiçoso
@Entity
@Table(name = "cliente")
class Cliente {

    // @Id define a chave primária, que é a IDENTIDADE da entidade para o banco.
    // @GeneratedValue delega a geração ao banco. As estratégias:
    //  IDENTITY - coluna auto-incremento; simples, mas impede o batch de inserts, porque o
    //             provedor precisa do INSERT imediato para saber o id.
    //  SEQUENCE - sequence do banco; é a preferida quando o banco suporta (PostgreSQL, Oracle).
    //  TABLE    - tabela de controle; portável, porém lenta e com contenção.
    //  AUTO     - o provedor escolhe.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nome;

    // unique = true gera a constraint no DDL, mas NÃO é validado pela JPA em memória: a violação
    // só aparece como exceção do banco no momento do flush.
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    // Enum: SEMPRE use EnumType.STRING. O padrão é ORDINAL, que grava a POSIÇÃO da constante -
    // inserir um valor novo no meio do enum corrompe silenciosamente todos os registros gravados.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCliente categoria = CategoriaCliente.COMUM;

    // Tipos de data e hora do java.time são mapeados nativamente desde a JPA 2.2. Em código legado
    // aparecem java.util.Date com @Temporal(TemporalType.DATE|TIME|TIMESTAMP).
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // @Embedded incorpora um objeto de valor NAS COLUNAS DA PRÓPRIA TABELA. Não há tabela nem id
    // para Endereco - é a forma de ter alta coesão no modelo de objetos sem criar uma tabela a mais.
    @Embedded
    private Endereco endereco;

    // Um cliente tem muitos pedidos.
    // mappedBy diz que o DONO do relacionamento é o outro lado (o campo "cliente" em Pedido). O
    // dono é quem tem a chave estrangeira e é o único lado que o provedor observa para gravar.
    // Esquecer o mappedBy faz a JPA criar uma tabela de junção desnecessária.
    // cascade = ALL propaga persist/merge/remove aos filhos.
    // orphanRemoval = true apaga o filho ao ser removido da coleção - diferente de CascadeType.REMOVE,
    // que só age quando o PAI é removido.
    @OneToMany(mappedBy = "cliente",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Pedido> pedidos = new ArrayList<>();

    // Relacionamento um-para-um. optional = false gera NOT NULL na FK.
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PerfilCredito perfilCredito;

    // Bloqueio OTIMISTA: o provedor acrescenta "AND versao = ?" ao UPDATE e incrementa o campo.
    // Se outra transação já alterou a linha, nenhuma linha é afetada e o provedor lança
    // OptimisticLockException. É a proteção contra o "lost update" sem travar registros.
    @Version
    private Integer versao;

    // Construtor sem argumentos, exigido pela especificação.
    protected Cliente() {
    }

    public Cliente(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    // MÉTODO DE SINCRONIZAÇÃO DE RELACIONAMENTO BIDIRECIONAL.
    // A JPA não mantém os dois lados sincronizados sozinha. Se você só fizer
    // cliente.getPedidos().add(pedido), o campo pedido.cliente fica nulo e a FK vai nula para o
    // banco. Concentrar os dois lados em um método é a forma de não esquecer - e é o Creator do
    // GRASP: quem agrega é quem gerencia.
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

    // equals/hashCode em entidades é um assunto delicado:
    //  - usar o id gerado quebra, porque ele é nulo antes do persist e o objeto muda de hash
    //    depois de entrar num HashSet;
    //  - usar todos os campos quebra quando qualquer um deles muda.
    // A recomendação usual é usar uma chave de NEGÓCIO imutável (aqui o CPF) ou um UUID atribuído
    // pela aplicação antes do persist.
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

// OBJETO DE VALOR (@Embeddable)
// Não tem identidade própria: é identificado pelo seu conteúdo e vive dentro da tabela do dono.
// É o mesmo conceito de Value Object do DDD e ajuda muito a coesão - sem ele, os cinco campos de
// endereço ficariam soltos dentro de Cliente.
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

    // LADO DONO do relacionamento: é aqui que fica a coluna de chave estrangeira.
    // ATENÇÃO ao fetch: em @ManyToOne e @OneToOne o padrão da especificação é EAGER, e isso é uma
    // das principais causas de lentidão - carregar um pedido traz o cliente junto, sempre.
    // A prática recomendada é declarar LAZY em tudo e trazer o que precisa com JOIN FETCH.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Em @OneToMany e @ManyToMany o padrão já é LAZY.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    // MUITOS-PARA-MUITOS com tabela de junção.
    // Na prática, quase sempre é melhor transformar a junção em uma ENTIDADE própria: assim que o
    // negócio pedir um atributo no relacionamento (data de aplicação, valor concedido), o
    // @ManyToMany não serve mais e a migração é dolorosa.
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

    // Information Expert: quem tem os itens calcula o total. A regra de negócio permanece na
    // entidade - a JPA não obriga a esvaziá-la em getters e setters.
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

    // Valor monetário: use BigDecimal com precision/scale, NUNCA double. Ponto flutuante binário
    // não representa 0,10 exatamente e o erro se acumula a cada soma.
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

// HERANÇA - o ponto em que o descompasso objeto-relacional fica mais evidente, porque tabelas não
// têm herança. As três estratégias e seus custos:
//
// SINGLE_TABLE (padrão) - uma tabela para toda a hierarquia, com uma coluna discriminadora.
//   É a mais rápida (nenhuma junção), mas as colunas das subclasses precisam aceitar NULL, o que
//   impede constraints NOT NULL nos campos específicos.
// JOINED - uma tabela por classe, ligadas pela PK. Modelo normalizado e com integridade completa,
//   ao custo de uma junção por nível de herança.
// TABLE_PER_CLASS - uma tabela completa por classe concreta. Evita junções na leitura de um tipo,
//   mas consultas polimórficas viram UNION e a geração de id fica limitada (IDENTITY não serve).
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

    // Polimorfismo (GRASP) preservado no modelo persistente: cada forma de pagamento sabe a sua
    // própria taxa, sem nenhum switch por tipo.
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

//RESUMO DAS ANOTAÇÕES DE MAPEAMENTO
//@Entity, @Table                        - a classe vira tabela
//@Id, @GeneratedValue                   - chave primária e sua geração
//@Column                                - nome, tamanho, nulidade, precisão
//@Embeddable, @Embedded, @AttributeOverride - objetos de valor nas colunas do dono
//@Enumerated(EnumType.STRING)           - enum como texto (nunca ORDINAL)
//@Temporal                              - datas do java.util (desnecessário com java.time)
//@Transient                             - campo que NÃO deve ser persistido
//@Lob                                   - textos e binários grandes
//@OneToOne, @ManyToOne, @OneToMany, @ManyToMany - relacionamentos
//@JoinColumn, @JoinTable, mappedBy      - onde fica a FK e quem é o dono
//@Inheritance, @DiscriminatorColumn     - estratégia de herança
//@Version                               - bloqueio otimista
//@NamedQuery                            - JPQL nomeada, validada na subida da aplicação
//
//As três armadilhas mais comuns em prova e em produção:
//1. EnumType.ORDINAL (o padrão) corrompendo dados quando o enum muda.
//2. @ManyToOne EAGER por padrão, trazendo meia base junto em cada consulta.
//3. Relacionamento bidirecional sem método de sincronização, gravando FK nula.

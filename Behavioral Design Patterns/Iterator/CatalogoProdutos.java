//Suponha que a sua tarefa seja percorrer os produtos de um catálogo para gerar uma listagem. O
//catálogo guarda os produtos internamente em um array, mas a área de compras mantém os dela em
//uma lista ligada e a loja física em uma árvore por categoria.

//Imagine o código de listagem escrito para cada estrutura: um for com índice para o array, um
//while com getProximo() para a lista, uma recursão para a árvore. Trocar a estrutura interna do
//catálogo quebraria todos os clientes - a representação vazou.

//O Iterator resolve o problema de fornecer uma maneira de acessar sequencialmente os elementos de
//um agregado SEM expor a sua representação interna.

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class Produto {
    private final String nome;
    private final String categoria;
    private final int precoEmCentavos;

    Produto(String nome, String categoria, int precoEmCentavos) {
        this.nome = nome;
        this.categoria = categoria;
        this.precoEmCentavos = precoEmCentavos;
    }

    String getNome() {
        return nome;
    }

    String getCategoria() {
        return categoria;
    }

    int getPrecoEmCentavos() {
        return precoEmCentavos;
    }

    @Override
    public String toString() {
        return nome + " (" + categoria + ") " + precoEmCentavos;
    }
}

// Padrão Iterator - o Aggregate
// A única coisa que o agregado promete é saber produzir um iterador. Em Java isso já é padrão da
// linguagem: implementar Iterable habilita o for-each.
class CatalogoArray implements Iterable<Produto> {

    private final Produto[] produtos = new Produto[10];
    private int quantidade;

    void adicionar(Produto produto) {
        produtos[quantidade++] = produto;
    }

    // Padrão Iterator - o iterador concreto, como classe interna
    // Sendo interna, ele enxerga a representação (o array e o contador) sem que ninguém de fora
    // precise enxergá-la. É essa combinação que faz o padrão funcionar.
    @Override
    public Iterator<Produto> iterator() {
        return new Iterator<Produto>() {
            private int indice;

            @Override
            public boolean hasNext() {
                return indice < quantidade;
            }

            @Override
            public Produto next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return produtos[indice++];
            }
        };
    }
}

// Outra estrutura interna, MESMA interface para o cliente. Aqui a representação é uma lista
// encadeada escrita à mão - e o cliente continua sem saber disso.
class CatalogoListaLigada implements Iterable<Produto> {

    private No primeiro;

    private static class No {
        private final Produto produto;
        private No proximo;

        No(Produto produto) {
            this.produto = produto;
        }
    }

    void adicionar(Produto produto) {
        No novo = new No(produto);
        if (primeiro == null) {
            primeiro = novo;
            return;
        }
        No atual = primeiro;
        while (atual.proximo != null) {
            atual = atual.proximo;
        }
        atual.proximo = novo;
    }

    @Override
    public Iterator<Produto> iterator() {
        return new Iterator<Produto>() {
            private No atual = primeiro;

            @Override
            public boolean hasNext() {
                return atual != null;
            }

            @Override
            public Produto next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Produto produto = atual.produto;
                atual = atual.proximo;
                return produto;
            }
        };
    }
}

// Iterador com FILTRO. Como iterar é responsabilidade de um objeto separado, dá para ter vários
// percursos diferentes sobre o mesmo agregado - sem inchar a classe do agregado.
class IteradorPorCategoria implements Iterator<Produto> {

    private final Iterator<Produto> origem;
    private final String categoria;
    private Produto proximo;

    IteradorPorCategoria(Iterable<Produto> agregado, String categoria) {
        this.origem = agregado.iterator();
        this.categoria = categoria;
        avancar();
    }

    // O elemento precisa ser buscado ANTES de hasNext() poder responder: um filtro só sabe se
    // ainda há elementos depois de procurar o próximo que passa no critério.
    private void avancar() {
        proximo = null;
        while (origem.hasNext()) {
            Produto candidato = origem.next();
            if (candidato.getCategoria().equals(categoria)) {
                proximo = candidato;
                return;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return proximo != null;
    }

    @Override
    public Produto next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Produto atual = proximo;
        avancar();
        return atual;
    }
}

// Classe Cliente
class CatalogoProdutos {

    // O parâmetro é Iterable: este método serve para o catálogo em array, para o em lista ligada
    // e para qualquer coleção da biblioteca padrão. Nenhum deles precisa expor sua estrutura.
    public void listar(String titulo, Iterable<Produto> agregado) {
        System.out.println(titulo);
        for (Produto produto : agregado) {   // o for-each usa iterator() por baixo
            System.out.println("  " + produto);
        }
    }

    public void listar(String titulo, Iterator<Produto> iterador) {
        System.out.println(titulo);
        while (iterador.hasNext()) {
            System.out.println("  " + iterador.next());
        }
    }

    public static void main(String[] args) {
        CatalogoArray porArray = new CatalogoArray();
        porArray.adicionar(new Produto("Teclado", "periférico", 25000));
        porArray.adicionar(new Produto("Mouse", "periférico", 8000));
        porArray.adicionar(new Produto("Monitor", "vídeo", 90000));

        CatalogoListaLigada porLista = new CatalogoListaLigada();
        porLista.adicionar(new Produto("Cadeira", "mobiliário", 120000));
        porLista.adicionar(new Produto("Mesa", "mobiliário", 180000));

        List<Produto> daBibliotecaPadrao = new ArrayList<>();
        daBibliotecaPadrao.add(new Produto("Headset", "periférico", 35000));

        CatalogoProdutos cliente = new CatalogoProdutos();

        // Três representações internas completamente diferentes, um único código de listagem.
        cliente.listar("catálogo em array:", porArray);
        cliente.listar("catálogo em lista ligada:", porLista);
        cliente.listar("catálogo em ArrayList:", daBibliotecaPadrao);

        // Percurso alternativo sobre o mesmo agregado.
        cliente.listar("só periféricos:", new IteradorPorCategoria(porArray, "periférico"));

        // Dois iteradores independentes sobre o MESMO agregado, cada um com sua posição.
        // Manter a posição no iterador, e não no agregado, é o que torna isso possível.
        Iterator<Produto> a = porArray.iterator();
        Iterator<Produto> b = porArray.iterator();
        a.next();
        System.out.println("iterador A está em: " + a.next());
        System.out.println("iterador B está em: " + b.next());
    }
}

//Iterador EXTERNO x INTERNO:
//Externo é o do exemplo - o cliente controla o avanço com hasNext()/next(), podendo parar no meio.
//Interno é quando o agregado controla o percurso e o cliente só fornece a operação a aplicar; em
//Java é o forEach(Consumer) e toda a API de Stream.
//
//Cuidado clássico: modificar a coleção durante a iteração. Os iteradores de java.util são
//fail-fast e lançam ConcurrentModificationException - a remoção correta é pelo próprio iterador,
//com iterator.remove(), exatamente como no exemplo de Factory Method deste repositório.

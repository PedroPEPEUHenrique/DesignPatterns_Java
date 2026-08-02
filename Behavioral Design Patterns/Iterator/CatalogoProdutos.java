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

class CatalogoArray implements Iterable<Produto> {

    private final Produto[] produtos = new Produto[10];
    private int quantidade;

    void adicionar(Produto produto) {
        produtos[quantidade++] = produto;
    }

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

class IteradorPorCategoria implements Iterator<Produto> {

    private final Iterator<Produto> origem;
    private final String categoria;
    private Produto proximo;

    IteradorPorCategoria(Iterable<Produto> agregado, String categoria) {
        this.origem = agregado.iterator();
        this.categoria = categoria;
        avancar();
    }

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

class CatalogoProdutos {

    public void listar(String titulo, Iterable<Produto> agregado) {
        System.out.println(titulo);
        for (Produto produto : agregado) {
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

        cliente.listar("catálogo em array:", porArray);
        cliente.listar("catálogo em lista ligada:", porLista);
        cliente.listar("catálogo em ArrayList:", daBibliotecaPadrao);

        cliente.listar("só periféricos:", new IteradorPorCategoria(porArray, "periférico"));

        Iterator<Produto> a = porArray.iterator();
        Iterator<Produto> b = porArray.iterator();
        a.next();
        System.out.println("iterador A está em: " + a.next());
        System.out.println("iterador B está em: " + b.next());
    }
}

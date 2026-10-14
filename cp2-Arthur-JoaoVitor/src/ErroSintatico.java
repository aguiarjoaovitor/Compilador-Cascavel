package src;

public class ErroSintatico {
    public final int linha;
    public final int coluna;
    public final String mensagem;

    public ErroSintatico(int linha, int coluna, String mensagem) {
        this.linha = linha;
        this.coluna = coluna;
        this.mensagem = mensagem;
    }

    @Override
    public String toString() {
        return "Erro sintático na linha " + linha + ", coluna " + coluna + ": " + mensagem;
    }
}

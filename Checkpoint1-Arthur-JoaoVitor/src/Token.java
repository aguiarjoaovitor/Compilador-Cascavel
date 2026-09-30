package src;

public class Token {
    public final TokenType tipo;
    public final String lexema;
    public final int linha;
    public final int coluna;

    public Token(TokenType tipo, String lexema, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    @Override
    public String toString() {
        return tipo.name() + (lexema.isEmpty() || tipo.name().equals(lexema) ? "" : "(" + lexema + ")");
    }
}

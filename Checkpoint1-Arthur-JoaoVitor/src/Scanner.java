package src;

import java.util.*;

public class Scanner {
    private final String fonte;
    private int atual = 0;
    private int inicio = 0;
    
    private int linha = 1;
    private int coluna = 1;
    private int colunaInicio = 1;
    
    private boolean inicioDeLinha = true;
    private boolean primeiraLinhaComConteudo = true;
    private Stack<Integer> pilhaIndentacao = new Stack<>();
    private Queue<Token> tokensPendentes = new LinkedList<>();
    private boolean eofEmitido = false;
    
    public List<String> erros = new ArrayList<>();

    private static final Map<String, TokenType> palavrasReservadas;
    static {
        palavrasReservadas = new HashMap<>();
        palavrasReservadas.put("int", TokenType.INT);
        palavrasReservadas.put("double", TokenType.DOUBLE);
        palavrasReservadas.put("bool", TokenType.BOOL);
        palavrasReservadas.put("char", TokenType.CHAR);
        palavrasReservadas.put("string", TokenType.STRING);
        palavrasReservadas.put("se", TokenType.SE);
        palavrasReservadas.put("senaose", TokenType.SENAOSE);
        palavrasReservadas.put("senao", TokenType.SENAO);
        palavrasReservadas.put("enquanto", TokenType.ENQUANTO);
        palavrasReservadas.put("def", TokenType.DEF);
        palavrasReservadas.put("retorne", TokenType.RETORNE);
        palavrasReservadas.put("passe", TokenType.PASSE);
        palavrasReservadas.put("None", TokenType.NONE);
        palavrasReservadas.put("e", TokenType.E);
        palavrasReservadas.put("ou", TokenType.OU);
        palavrasReservadas.put("nao", TokenType.NAO);
        palavrasReservadas.put("True", TokenType.BOOL_LIT);
        palavrasReservadas.put("False", TokenType.BOOL_LIT);
    }

    public Scanner(String fonte) {
        this.fonte = fonte;
        pilhaIndentacao.push(0);
    }

    private void reportarErro(String mensagem, int erroLinha, int erroCol) {
        String err = "Erro na linha " + erroLinha + ", coluna " + erroCol + ": " + mensagem;
        erros.add(err);
        System.err.println(err);
    }

    public boolean temProximo() {
        return !eofEmitido;
    }

    public Token proximoToken() {
        while (tokensPendentes.isEmpty() && atual < fonte.length()) {
            if (inicioDeLinha) {
                processarIndentacao();
                if (!tokensPendentes.isEmpty()) break;
                if (atual >= fonte.length()) break;
            }
            
            inicio = atual;
            colunaInicio = coluna;
            
            char c = avancar();
            
            if (c == ' ' || c == '\t' || c == '\r') {
                continue;
            }
            
            if (c == '\n') {
                inicioDeLinha = true;
                tokensPendentes.offer(new Token(TokenType.NEWLINE, "\\n", linha, colunaInicio));
                linha++;
                coluna = 1;
                continue;
            }
            
            if (c == '#') {
                if (espiar() == '*') {
                    comentarioBloco();
                } else {
                    comentarioLinha();
                }
                continue;
            }
            
            escanearToken(c);
        }
        
        if (!tokensPendentes.isEmpty()) {
            return tokensPendentes.poll();
        }
        
        if (!eofEmitido) {
            if (!inicioDeLinha) {
                tokensPendentes.offer(new Token(TokenType.NEWLINE, "\\n", linha, coluna));
                inicioDeLinha = true;
            }
            
            while (pilhaIndentacao.size() > 1) {
                pilhaIndentacao.pop();
                tokensPendentes.offer(new Token(TokenType.DEDENT, "", linha, coluna));
            }
            
            tokensPendentes.offer(new Token(TokenType.EOF, "EOF", linha, coluna));
            eofEmitido = true;
            return tokensPendentes.poll();
        }
        
        return null;
    }

    private void processarIndentacao() {
        int espacos = 0;
        boolean temErroTab = false;
        int colunaErro = coluna;
        
        while (atual < fonte.length() && (espiar() == ' ' || espiar() == '\t')) {
            char c = avancar();
            if (c == '\t' && !temErroTab) {
                temErroTab = true;
                reportarErro("Tab na indentação", linha, colunaErro);
            }
            if (c == ' ') espacos++;
        }
        
        if (atual >= fonte.length() || espiar() == '\n' || espiar() == '\r') {
            return;
        }
        
        if (espiar() == '#' && espiarProximo() != '*') {
            return;
        }
        
        inicioDeLinha = false;
        
        if (temErroTab) {
            espacos = pilhaIndentacao.peek();
        }
        
        if (primeiraLinhaComConteudo) {
            primeiraLinhaComConteudo = false;
            if (espacos > 0 && !temErroTab) {
                reportarErro("Primeira linha indentada", linha, 1);
                espacos = 0;
            }
        }
        
        int topo = pilhaIndentacao.peek();
        if (espacos > topo) {
            pilhaIndentacao.push(espacos);
            tokensPendentes.offer(new Token(TokenType.INDENT, "", linha, coluna));
        } else if (espacos < topo) {
            while (pilhaIndentacao.size() > 1 && pilhaIndentacao.peek() > espacos) {
                pilhaIndentacao.pop();
                tokensPendentes.offer(new Token(TokenType.DEDENT, "", linha, coluna));
            }
            if (pilhaIndentacao.peek() != espacos) {
                reportarErro("Indentação inconsistente", linha, 1);
            }
        }
    }

    private void escanearToken(char c) {
        if (ehLetra(c)) {
            identificador(c);
        } else if (ehDigito(c)) {
            numero(c);
        } else {
            switch (c) {
                case '"': literalString(); break;
                case '\'': literalChar(); break;
                case '+': adicionarToken(TokenType.PLUS); break;
                case '-': 
                    if (combina('>')) adicionarToken(TokenType.ARROW);
                    else adicionarToken(TokenType.MINUS);
                    break;
                case '*': adicionarToken(TokenType.STAR); break;
                case '/': adicionarToken(TokenType.SLASH); break;
                case '%': adicionarToken(TokenType.PERCENT); break;
                case '=':
                    if (combina('=')) adicionarToken(TokenType.EQ);
                    else adicionarToken(TokenType.ASSIGN);
                    break;
                case '!':
                    if (combina('=')) adicionarToken(TokenType.NE);
                    else reportarErro("'!' não seguido de '='", linha, colunaInicio);
                    break;
                case '<':
                    if (combina('=')) adicionarToken(TokenType.LE);
                    else adicionarToken(TokenType.LT);
                    break;
                case '>':
                    if (combina('=')) adicionarToken(TokenType.GE);
                    else adicionarToken(TokenType.GT);
                    break;
                case '(': adicionarToken(TokenType.LPAREN); break;
                case ')': adicionarToken(TokenType.RPAREN); break;
                case ':': adicionarToken(TokenType.COLON); break;
                case ',': adicionarToken(TokenType.COMMA); break;
                default:
                    reportarErro("Caractere fora do alfabeto", linha, colunaInicio);
                    break;
            }
        }
    }

    private void identificador(char primeiro) {
        while (atual < fonte.length() && (ehLetra(espiar()) || ehDigito(espiar()) || espiar() == '_')) {
            avancar();
        }
        
        String texto = fonte.substring(inicio, atual);
        TokenType tipo = palavrasReservadas.getOrDefault(texto, TokenType.ID);
        tokensPendentes.offer(new Token(tipo, texto, linha, colunaInicio));
    }

    private void numero(char primeiro) {
        while (atual < fonte.length() && ehDigito(espiar())) {
            avancar();
        }
        
        if (espiar() == '.') {
            avancar(); // Consome '.'
            
            if (!ehDigito(espiar())) {
                reportarErro("Real sem dígito após o ponto", linha, colunaInicio);
                adicionarToken(TokenType.DOUBLE_LIT);
            } else {
                while (atual < fonte.length() && ehDigito(espiar())) {
                    avancar();
                }
                adicionarToken(TokenType.DOUBLE_LIT);
            }
        } else {
            adicionarToken(TokenType.INT_LIT);
        }
        
        if (atual < fonte.length() && (ehLetra(espiar()) || espiar() == '_')) {
            reportarErro("Número seguido de letra ou '_'", linha, colunaInicio);
            while (atual < fonte.length() && (ehLetra(espiar()) || ehDigito(espiar()) || espiar() == '_')) {
                avancar(); 
            }
        }
    }

    private void literalString() {
        while (atual < fonte.length() && espiar() != '"' && espiar() != '\n') {
            if (espiar() == '\\') {
                avancar();
                char proximo = espiar();
                if (proximo == '"' || proximo == '\'' || proximo == '\\' || proximo == 'n' || proximo == 't') {
                    avancar();
                } else {
                    reportarErro("Escape inválido", linha, coluna - 1);
                }
            } else {
                avancar();
            }
        }
        
        if (atual >= fonte.length()) {
            reportarErro("String não fechada até o fim do arquivo", linha, colunaInicio);
            return;
        }
        if (espiar() == '\n') {
            reportarErro("String não fechada até o fim da linha", linha, colunaInicio);
            return;
        }
        
        avancar(); // Consome '"'
        String texto = fonte.substring(inicio, atual);
        tokensPendentes.offer(new Token(TokenType.STRING_LIT, texto, linha, colunaInicio));
    }

    private void literalChar() {
        int tamanhoConteudo = 0;
        
        while (atual < fonte.length() && espiar() != '\'' && espiar() != '\n') {
            if (espiar() == '\\') {
                avancar();
                char proximo = espiar();
                if (proximo == '"' || proximo == '\'' || proximo == '\\' || proximo == 'n' || proximo == 't') {
                    avancar();
                    tamanhoConteudo++;
                } else {
                    reportarErro("Escape inválido", linha, coluna - 1);
                    if (atual < fonte.length() && espiar() != '\n' && espiar() != '\'') {
                        avancar();
                        tamanhoConteudo++;
                    }
                }
            } else {
                avancar();
                tamanhoConteudo++;
            }
        }
        
        if (atual >= fonte.length()) {
            reportarErro("Char não fechado até o fim do arquivo", linha, colunaInicio);
            return;
        }
        if (espiar() == '\n') {
            reportarErro("Char não fechado até o fim da linha", linha, colunaInicio);
            return;
        }
        
        avancar(); // Consome '\''
        
        if (tamanhoConteudo != 1) {
            reportarErro("Char vazio ou com mais de um caractere", linha, colunaInicio);
        } else {
            String texto = fonte.substring(inicio, atual);
            tokensPendentes.offer(new Token(TokenType.CHAR_LIT, texto, linha, colunaInicio));
        }
    }

    private void comentarioBloco() {
        int erroLinha = linha;
        int erroCol = colunaInicio;
        avancar(); // '*'
        
        while (!chegouAoFim()) {
            char c = espiar();
            if (c == '\n') {
                atual++;
                linha++;
                coluna = 1;
            } else if (c == '*' && espiarProximo() == '#') {
                avancar(); // '*'
                avancar(); // '#'
                return;
            } else {
                avancar();
            }
        }
        
        reportarErro("Fim de arquivo dentro de comentário de bloco", erroLinha, erroCol);
    }

    private void comentarioLinha() {
        while (atual < fonte.length() && espiar() != '\n') {
            avancar();
        }
    }

    private boolean ehLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean ehDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean combina(char esperado) {
        if (chegouAoFim()) return false;
        if (fonte.charAt(atual) != esperado) return false;
        atual++;
        coluna++;
        return true;
    }

    private char espiar() {
        if (chegouAoFim()) return '\0';
        return fonte.charAt(atual);
    }

    private char espiarProximo() {
        if (atual + 1 >= fonte.length()) return '\0';
        return fonte.charAt(atual + 1);
    }

    private char avancar() {
        char c = fonte.charAt(atual);
        atual++;
        coluna++;
        return c;
    }

    private boolean chegouAoFim() {
        return atual >= fonte.length();
    }

    private void adicionarToken(TokenType tipo) {
        String texto = fonte.substring(inicio, atual);
        tokensPendentes.offer(new Token(tipo, texto, linha, colunaInicio));
    }
}

package src;

public enum TokenType {
    // Tipos
    INT, DOUBLE, BOOL, CHAR, STRING,
    
    // Controle
    SE, SENAOSE, SENAO, ENQUANTO,
    
    // Funções
    DEF, RETORNE, PASSE, NONE,
    
    // Lógicos
    E, OU, NAO,
    
    // Booleanos
    BOOL_LIT,
    
    // Identificador e Literais
    ID, INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT,
    
    // Operadores
    PLUS, MINUS, STAR, SLASH, PERCENT, ASSIGN, ARROW,
    EQ, NE, LT, LE, GT, GE,
    
    // Delimitadores
    LPAREN, RPAREN, COLON, COMMA,
    
    // Especiais (Indentação e Fim de arquivo)
    NEWLINE, INDENT, DEDENT, EOF
}

package test;

import src.Scanner;
import src.Token;
import src.TokenType;

public class ScannerTest {
    public static void main(String[] args) {
        System.out.println("=== TESTES VÁLIDOS BÁSICOS ===");
        testarToken("Identificador", "variavel_1", TokenType.ID);
        testarToken("Palavra Reservada", "enquanto", TokenType.ENQUANTO);
        testarToken("Inteiro", "42", TokenType.INT_LIT);
        testarToken("Real", "3.14", TokenType.DOUBLE_LIT);
        testarToken("String", "\"olá mundo\"", TokenType.STRING_LIT);
        testarToken("Char", "'a'", TokenType.CHAR_LIT);
        testarToken("Operador Composto", "==", TokenType.EQ);

        System.out.println("\n=== TESTES DE ERROS (Tratamento e Recuperação) ===");
        testarErros("String não fechada (Fim de Linha)", "\"teste\n", 1);
        testarErros("String não fechada (EOF)", "\"teste", 1);
        testarErros("Caractere fora do alfabeto", "@", 1);
        testarErros("Número seguido de letra", "123x", 1);
        testarErros("Char vazio", "''", 1);
        testarErros("Escape inválido", "\"teste \\q\"", 1);

        System.out.println("\n=== TESTE DE CÓDIGO REALISTA ===");
        String codigo = 
            "def dobro(n: int) -> int:  # comentario\n" +
            "    retorne n * 2\n" +
            "\n" +
            "se True:\n" +
            "    passe\n" +
            "#* bloco\n" +
            "ignorado *#\n" +
            "    senao:\n" +
            "        passe\n";
        
        System.out.println("Código-fonte analisado:\n" + codigo + "------");
        Scanner analisador = new Scanner(codigo);
        while (analisador.temProximo()) {
            Token token = analisador.proximoToken();
            if (token != null) {
                System.out.println(token.tipo + (token.lexema.isEmpty() || token.tipo.name().equals(token.lexema) ? "" : "(" + token.lexema + ")") + " - [L" + token.linha + ", C" + token.coluna + "]");
            }
        }
        
        if (!analisador.erros.isEmpty()) {
            System.out.println("Erros detectados no código realista:");
            for (String erro : analisador.erros) {
                System.out.println(erro);
            }
        }
        
        System.out.println("\n=== SUCESSO: TESTES FINALIZADOS ===");
    }

    private static void testarToken(String nome, String codigoFonte, TokenType esperado) {
        Scanner analisador = new Scanner(codigoFonte);
        Token token = analisador.proximoToken();
        boolean passou = token != null && token.tipo == esperado;
        System.out.println("[" + (passou ? "PASS" : "FAIL") + "] " + nome + " -> '" + codigoFonte + "' gerou o token correto.");
    }

    private static void testarErros(String nome, String codigoFonte, int qtdErrosEsperados) {
        Scanner analisador = new Scanner(codigoFonte);
        while (analisador.temProximo()) {
            Token token = analisador.proximoToken();
            if (token != null && token.tipo == TokenType.EOF) break;
        }
        boolean passou = analisador.erros.size() == qtdErrosEsperados;
        System.out.println("[" + (passou ? "PASS" : "FAIL") + "] " + nome);
        for (String erro : analisador.erros) {
            System.out.println("    " + erro);
        }
    }
}

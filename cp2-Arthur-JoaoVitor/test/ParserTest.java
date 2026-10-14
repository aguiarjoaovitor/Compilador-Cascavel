package test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import src.ErroSintatico;
import src.Parser;
import src.Scanner;

public class ParserTest {
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("=== PROGRAMAS VÁLIDOS ===");

        testar("Funções, declarações dos cinco tipos, enquanto e chamada", linhas(
            "def soma(a: int, b: int) -> int:",
            "    retorne a + b",
            "",
            "def mostrar(x: int) -> None:",
            "    se x > 10 e nao (x == 15):",
            "        passe",
            "    senaose x < 0:",
            "        retorne",
            "    senao:",
            "        x = x * 2",
            "    retorne",
            "",
            "total: int = 0",
            "i: int = 1",
            "enquanto i <= 10:",
            "    total = soma(total, i)",
            "    i = i + 1",
            "mostrar(total)",
            "media: double = 5.5",
            "nome: string = \"Cascavel\"",
            "letra: char = 'c'",
            "ok: bool = True"
        ));

        testar("Expressões: precedência, operadores unários e chamadas aninhadas", linhas(
            "def vazio() -> None:",
            "    passe",
            "x: int = -(3 + 4) * 2 % 5 - -1",
            "y: bool = nao x == 1 ou x != 2 e True",
            "vazio()",
            "f(g(1, 2), (x))"
        ));

        testar("Controle aninhado: se/senaose/senao dentro de enquanto dentro de def", linhas(
            "def classificar(n: int) -> string:",
            "    enquanto n > 0:",
            "        se n % 2 == 0:",
            "            retorne \"par\"",
            "        senaose n == 1:",
            "            retorne \"um\"",
            "        senao:",
            "            n = n - 2",
            "    retorne \"zero\"",
            "resultado: string = classificar(7)"
        ));

        testar("Programa vazio, apenas com comentários", linhas(
            "# comentário de linha",
            "#* comentário",
            "   de bloco *#"
        ));

        System.out.println("\n=== PROGRAMAS INVÁLIDOS (UM ERRO CADA) ===");

        testar("Declaração sem inicialização", linhas(
            "x: int"
        ), 1);

        testar("Operadores relacionais encadeados", linhas(
            "ok: bool = 1 < 2 < 3"
        ), 1);

        testar("Expressão usada como comando", linhas(
            "x: int = 1",
            "x + 1"
        ), 2);

        testar("'def' dentro de um bloco", linhas(
            "se x:",
            "    def f() -> None:",
            "        passe",
            "    y = 1"
        ), 2);

        testar("Falta ':' no cabeçalho do enquanto", linhas(
            "enquanto x",
            "    passe",
            "z = 1"
        ), 1);

        testar("Parêntese não fechado", linhas(
            "x = (1 + 2",
            "y = 3"
        ), 1);

        testar("Bloco na mesma linha do cabeçalho", linhas(
            "se x: passe",
            "y = 1"
        ), 1);

        testar("Indentação inesperada", linhas(
            "x = 1",
            "    y = 2",
            "z = 3"
        ), 2);

        testar("Bloco vazio (só comentário)", linhas(
            "se x:",
            "    # nada aqui",
            "y = 1"
        ), 3);

        testar("'senao' sem 'se'", linhas(
            "senao:",
            "    passe",
            "x = 1"
        ), 1);

        System.out.println("\n=== VÁRIOS ERROS NA MESMA RODADA (MODO PÂNICO) ===");

        testar("Quatro erros em linhas diferentes", linhas(
            "x: int =",
            "y: int = 2 +",
            "z = 3",
            "w = 4 4",
            "f(1,)",
            "ok = True"
        ), 1, 2, 4, 5);

        testar("Três erros dentro de uma função", linhas(
            "def f(a int, b: int) -> int:",
            "    retorne a +",
            "    x = 1",
            "    enquanto x < :",
            "        passe"
        ), 1, 2, 4);

        System.out.println("\n=== ERRO LÉXICO E SINTÁTICO JUNTOS ===");

        testarComErroLexico("String não fechada", linhas(
            "x = \"abc",
            "y = 1"
        ), 1);

        System.out.println("\n=== ROBUSTEZ ===");
        testarRobustez(2000);

        System.out.println();
        if (falhas == 0) {
            System.out.println("=== SUCESSO: TODOS OS TESTES PASSARAM ===");
        } else {
            System.out.println("=== FALHA: " + falhas + " TESTE(S) FALHARAM ===");
        }
    }

    private static String linhas(String... linhas) {
        return String.join("\n", linhas) + "\n";
    }

    private static void testar(String nome, String codigo, int... linhasEsperadas) {
        Parser parser = new Parser(new Scanner(codigo));
        boolean aceito = parser.parse();
        List<Integer> obtidas = linhasDosErros(parser.getErros());
        List<Integer> esperadas = Arrays.stream(linhasEsperadas).boxed().collect(Collectors.toList());
        boolean aceitoEsperado = linhasEsperadas.length == 0;

        boolean passou = aceito == aceitoEsperado
            && obtidas.equals(esperadas)
            && parser.getErrosLexicos().isEmpty();
        registrar(passou, nome);
        imprimirErros(parser);
        if (!passou) {
            System.out.println("    esperado: " + (aceitoEsperado ? "aceito" : "erros nas linhas " + esperadas));
            System.out.println("    obtido:   " + (aceito ? "aceito" : "erros nas linhas " + obtidas));
        }
    }

    private static void testarComErroLexico(String nome, String codigo, int... linhasSintaticas) {
        Parser parser = new Parser(new Scanner(codigo));
        boolean aceito = parser.parse();
        List<Integer> obtidas = linhasDosErros(parser.getErros());
        List<Integer> esperadas = Arrays.stream(linhasSintaticas).boxed().collect(Collectors.toList());

        boolean passou = !aceito
            && !parser.getErrosLexicos().isEmpty()
            && obtidas.equals(esperadas);
        registrar(passou, nome);
        imprimirErros(parser);
    }

    private static void testarRobustez(int quantidade) {
        String base = linhas(
            "def f(a: int, b: int) -> int:",
            "    se a > b e nao (a == 0):",
            "        retorne a % b",
            "    senaose a < 0:",
            "        retorne -a",
            "    senao:",
            "        enquanto b > 0:",
            "            b = b - 1",
            "    retorne f(a, b) * (2 + 3)",
            "x: int = f(1, 2)"
        );
        String[] pedacos = {"def", "se", "senao", "enquanto", "retorne", "passe", "(", ")", ":", ",",
            "=", "==", "->", "+", "x", "1", "\n", "\n    ", "@", "\""};
        Random aleatorio = new Random(2026);
        List<String> problemas = new ArrayList<>();

        Thread execucao = new Thread(() -> {
            for (int i = 0; i < quantidade; i++) {
                StringBuilder codigo = new StringBuilder(base);
                int mutacoes = 1 + aleatorio.nextInt(5);
                for (int m = 0; m < mutacoes; m++) {
                    int posicao = aleatorio.nextInt(codigo.length());
                    if (aleatorio.nextBoolean()) {
                        codigo.deleteCharAt(posicao);
                    } else {
                        codigo.insert(posicao, pedacos[aleatorio.nextInt(pedacos.length)]);
                    }
                }
                try {
                    new Parser(new Scanner(codigo.toString())).parse();
                } catch (RuntimeException e) {
                    problemas.add(e + " em:\n" + codigo);
                }
            }
        });
        PrintStream errOriginal = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        execucao.setDaemon(true);
        execucao.start();
        try {
            execucao.join(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.setErr(errOriginal);
        }

        boolean terminou = !execucao.isAlive();
        boolean passou = terminou && problemas.isEmpty();
        registrar(passou, quantidade + " programas com mutações aleatórias: sem exceção e sem loop infinito");
        if (!terminou) {
            System.out.println("    o parser não terminou em 30 segundos (possível loop infinito)");
        }
        problemas.stream().limit(3).forEach(p -> System.out.println("    " + p));
    }

    private static List<Integer> linhasDosErros(List<ErroSintatico> erros) {
        List<Integer> linhas = new ArrayList<>();
        for (ErroSintatico erro : erros) {
            linhas.add(erro.linha);
        }
        return linhas;
    }

    private static void imprimirErros(Parser parser) {
        parser.getErrosLexicos().forEach(e -> System.out.println("    [léxico] " + e));
        parser.getErros().forEach(e -> System.out.println("    " + e));
    }

    private static void registrar(boolean passou, String nome) {
        if (!passou) {
            falhas++;
        }
        System.out.println("[" + (passou ? "PASS" : "FAIL") + "] " + nome);
    }
}

# Cascavel

Compilador para a linguagem **Cascavel**, desenvolvido ao longo da disciplina de Construção de Compiladores da Universidade Federal de Uberlândia (UFU), semestre 2026.2.

**Dupla:** Arthur Marques e João Vitor

## A linguagem

Cascavel é uma linguagem imperativa com sintaxe inspirada em Python:

- os blocos são delimitados por **indentação**;
- cada comando termina na quebra de linha, sem `;`;
- as palavras-chave são em **português, sem acento** (`se`, `senaose`, `senao`, `enquanto`, `retorne`, `passe`, `e`, `ou`, `nao`);
- a tipagem é **explícita**, com anotações no estilo Python.

Tipos básicos: `int`, `double`, `bool`, `char` e `string`.

```
def fatorial(n: int) -> int:
    se n <= 1:
        retorne 1
    retorne n * fatorial(n - 1)

#* Soma dos números de 1 a 10 *#
total: int = 0
i: int = 1
enquanto i <= 10:
    total = total + i
    i = i + 1
```

## Entregas

| Checkpoint | Conteúdo | Pasta |
|---|---|---|
| 1 | Especificação léxica e analisador léxico escrito à mão em Java | [`Checkpoint1-Arthur-JoaoVitor`](Checkpoint1-Arthur-JoaoVitor/) |
| 2 | — | — |
| 3 | — | — |
| 4 | — | — |
| 5 | — | — |

## Checkpoint 1: analisador léxico

### Estrutura

```
Checkpoint1-Arthur-JoaoVitor/
├── especificacao-lexica.md   especificação dos tokens da linguagem
├── log-uso-ia.md             registro do uso de IA no desenvolvimento
├── automatos/                diagramas dos AFDs e mapeamento estado → código
├── src/
│   ├── Scanner.java          analisador léxico
│   ├── Token.java
│   └── TokenType.java
└── test/
    └── ScannerTest.java      casos de teste, inclusive de erro
```

### Documentação

- [Especificação léxica](Checkpoint1-Arthur-JoaoVitor/especificacao-lexica.md): alfabeto, palavras reservadas, categorias de token, regras de indentação e política de erros.
- [Autômatos](Checkpoint1-Arthur-JoaoVitor/automatos/README.md): os AFDs de identificador, número, string, char e comentários, com a correspondência entre cada estado e as linhas do `Scanner.java`.
- [Log de uso de IA](Checkpoint1-Arthur-JoaoVitor/log-uso-ia.md)

### Como compilar e executar os testes

Requer Java 17 ou superior. A partir da pasta do checkpoint:

```bash
cd Checkpoint1-Arthur-JoaoVitor
javac -encoding UTF-8 -d out src/*.java test/*.java
java -cp out test.ScannerTest
```

A saída mostra o resultado de cada caso de teste e, ao final, se todos passaram. Também é impressa a tokenização completa de um trecho de código realista.

### Destaques da implementação

- **Indentação significativa:** o scanner mantém uma pilha de níveis de indentação e emite os tokens `INDENT`, `DEDENT` e `NEWLINE`, que o parser trata como delimitadores de bloco.
- **Maximal munch:** operadores compostos (`==`, `!=`, `<=`, `>=`, `->`) são reconhecidos com um caractere de lookahead.
- **Recuperação de erros:** todo erro léxico é registrado com linha e coluna, e a tokenização continua. Nenhum erro interrompe a execução.

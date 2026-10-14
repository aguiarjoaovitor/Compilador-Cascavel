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
| 2 | Gramática LL(1) (FIRST/FOLLOW) e parser descendente recursivo | [`cp2-Arthur-JoaoVitor`](cp2-Arthur-JoaoVitor/) |
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

## Checkpoint 2: gramática e parser

### Estrutura

```
cp2-Arthur-JoaoVitor/
├── gramatica-first-follow.md   gramática EBNF/BNF, FIRST/FOLLOW e verificação LL(1)
├── log-uso-ia.md                registro do uso de IA no desenvolvimento
├── src/
│   ├── Parser.java              parser descendente recursivo
│   ├── ErroSintatico.java       erro sintático (linha, coluna, mensagem)
│   ├── Scanner.java             analisador léxico (revisado a partir do Checkpoint 1)
│   ├── Token.java
│   └── TokenType.java
└── test/
    └── ParserTest.java          casos de teste, inclusive de erro e um teste de robustez
```

### Documentação

- [Gramática, FIRST/FOLLOW e verificação LL(1)](cp2-Arthur-JoaoVitor/gramatica-first-follow.md): gramática inicial em EBNF, eliminação de recursão à esquerda, fatoração à esquerda, gramática final em BNF (38 não-terminais), tabela de FIRST/FOLLOW e a verificação das três cláusulas da condição LL(1).
- [Log de uso de IA](cp2-Arthur-JoaoVitor/log-uso-ia.md)

### Como compilar e executar os testes

Requer Java 17 ou superior. A partir da pasta do checkpoint:

```bash
cd cp2-Arthur-JoaoVitor
javac -encoding UTF-8 -d out src/*.java test/*.java
java -cp out test.ParserTest
```

A saída mostra o resultado de cada caso de teste (programas válidos, inválidos com um erro, múltiplos erros em modo pânico e erro léxico combinado com sintático), seguido de um teste de robustez com 2000 programas gerados por mutação aleatória, verificando que o parser nunca lança exceção nem entra em loop infinito.

### Destaques da implementação

- **Parser LL(1) descendente recursivo:** cada não-terminal da gramática tem um método `parseX()` correspondente, e a escolha de alternativa é feita olhando apenas o token atual (FIRST) e, para as alternativas vazias, o conjunto FOLLOW.
- **Recuperação em modo pânico:** ao encontrar um token inesperado, o parser registra um único erro por ponto de falha e sincroniza avançando até um token do conjunto de sincronização (normalmente o FOLLOW do não-terminal ou as âncoras `NEWLINE`/`DEDENT`/`EOF`), evitando uma cascata de erros espúrios.
- **Scanner revisado:** durante a escrita do parser, bugs do scanner do Checkpoint 1 foram corrigidos (tratamento de comentários e indentação, detecção de identificador iniciado por `_`), mantendo os dois componentes consistentes.
- **Sem ambiguidade de dangling else:** a gramática evita a ambiguidade clássica porque todo bloco da Cascavel termina com um `DEDENT` explícito (ver Seção 6.4 da documentação da gramática).

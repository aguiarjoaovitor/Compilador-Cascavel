# Especificação Léxica — Linguagem: Cascavel

**Construção de Compiladores · UFU · Checkpoint 1 · Grupo: Arthur Marques - 12321BCC027, João Vitor Aguiar - 12321BCC009**

## 1. Visão geral

A linguagem tem estilo inspirado em Python: os blocos são delimitados por indentação e cada comando termina na quebra de linha. As palavras-chave são em português, sem acento. Mantivemos em inglês os termos consagrados do Python (`def`, `None`, `True`, `False`) e os nomes dos tipos básicos definidos no escopo da disciplina (`int`, `double`, `bool`, `char`, `string`).

Classes auxiliares usadas na notação deste documento:

```
letra   = [a-zA-Z]
dígito  = [0-9]
escape  = "\" ( '"' | "'" | "\" | "n" | "t" )
```

## 2. Alfabeto de entrada

- **Fora de strings, chars e comentários**, o alfabeto é formado por: letras ASCII (`a–z`, `A–Z`), dígitos (`0–9`), `_`, os símbolos `+ - * / % = ! < > ( ) , : . " ' #`, espaço, tab, quebra de linha (`\n`) e retorno de carro (`\r`).
- **Dentro de strings, chars e comentários**, qualquer caractere é aceito, inclusive letras acentuadas. A única exceção é que strings e chars não podem conter quebra de linha.
- **Qualquer outro caractere fora de strings, chars e comentários é erro léxico.** Exemplos: `@`, `$`, `?`, `;`, `[`, `{`, `&`, `|` e letras acentuadas.

## 3. Maiúsculas e minúsculas

A linguagem é **case-sensitive**, e identificadores e palavras reservadas seguem a mesma regra. As palavras reservadas só são reconhecidas com a grafia exata da Seção 4. Por exemplo, `Se`, `ENQUANTO` e `true` são identificadores comuns.

## 4. Palavras reservadas (lista fechada)

| Grupo | Palavras | Token |
|---|---|---|
| Tipos | `int` `double` `bool` `char` `string` | `INT` `DOUBLE` `BOOL` `CHAR` `STRING` |
| Controle | `se` `senaose` `senao` `enquanto` | `SE` `SENAOSE` `SENAO` `ENQUANTO` |
| Funções | `def` `retorne` `passe` `None` | `DEF` `RETORNE` `PASSE` `NONE` |
| Lógicos | `e` `ou` `nao` | `E` `OU` `NAO` |
| Booleanos | `True` `False` | `BOOL_LIT` |

São 18 palavras, todas reconhecidas pelo mesmo AFD dos identificadores e depois classificadas por consulta a uma tabela.

`passe` é o comando vazio, necessário porque todo bloco indentado precisa conter ao menos um comando. `None` é usado como tipo de retorno de funções sem valor (`-> None`).

## 5. Categorias de token

| Categoria | Token | Notação | Válidos | Inválidos |
|---|---|---|---|---|
| Identificador | `ID` | `letra ( letra \| dígito \| "_" )*` | `total`, `conta_itens`, `x1` | `_x`, `1x`, `ação` |
| Palavra reservada | ver Seção 4 | mesmo padrão + tabela de busca | `se`, `def`, `True` | — |
| Inteiro | `INT_LIT` | `dígito+` | `0`, `42`, `007` | `123abc` |
| Real | `DOUBLE_LIT` | `dígito+ "." dígito+` | `3.14`, `0.5` | `3.`, `.5`, `3.14abc` |
| String | `STRING_LIT` | `'"' ( qualquer ≠ '"' '\' \n \| escape )* '"'` | `"ok"`, `""`, `"diz \"oi\""` | `"sem fim`, `"a\qb"` |
| Char | `CHAR_LIT` | `"'" ( qualquer ≠ "'" '\' \n \| escape ) "'"` | `'a'`, `'\n'`, `'"'` | `''`, `'ab'`, `'x` |

Regras complementares:

- **Identificadores** não têm tamanho máximo e não podem começar com `_`.
- **Números colados em letras** são erro: um literal numérico imediatamente seguido de letra ou `_` (como em `123abc`) é um erro léxico, e não um `INT_LIT` seguido de um `ID`.
- **Literais numéricos são sempre não negativos.** O sinal de `-5` é o operador `MINUS`, tratado pela análise sintática.
- **Lexema de strings e chars:** o lexema de `STRING_LIT` e `CHAR_LIT` é o texto original, incluindo as aspas e os escapes sem processamento.
- **Escapes:** os mesmos cinco escapes valem em strings e chars. Não há notação científica nem literais hexadecimais.

## 6. Operadores e delimitadores

| Símbolo | Token | | Símbolo | Token |
|---|---|---|---|---|
| `+` | `PLUS` | | `==` | `EQ` |
| `-` | `MINUS` | | `!=` | `NE` |
| `*` | `STAR` | | `<` | `LT` |
| `/` | `SLASH` | | `<=` | `LE` |
| `%` | `PERCENT` | | `>` | `GT` |
| `=` | `ASSIGN` | | `>=` | `GE` |
| `->` | `ARROW` | | `(` `)` | `LPAREN` `RPAREN` |
| `:` | `COLON` | | `,` | `COMMA` |

Os operadores com forma composta são `==`, `!=`, `<=`, `>=` e `->`. O `!` sozinho não é operador, porque a negação lógica é `nao`: um `!` que não seja seguido de `=` é erro léxico.

## 7. Regras de desambiguação

1. **Maximal munch:** o scanner sempre consome o prefixo válido mais longo do texto restante. Assim, `==` é um único `EQ`, e não dois `ASSIGN`; `->` é `ARROW`, e não `MINUS` seguido de `GT`; e `enquanto1` é um único `ID`.
2. **Prioridade das palavras reservadas:** quando um lexema reconhecido pelo AFD de identificador consta da tabela da Seção 4, ele é classificado como palavra reservada, e não como `ID`.

As decisões entre formas simples e compostas usam um caractere de lookahead: o scanner espia o próximo caractere com `peek()` e só o consome se ele estender o token.

## 8. Espaços em branco e comentários

- No meio da linha, espaços e tabs separam tokens e são descartados. O caractere `\r` é ignorado em qualquer posição.
- **Comentário de linha:** vai de `#` até o fim da linha.
- **Comentário de bloco:** vai de `#*` até o primeiro `*#`. Comentários de bloco **não podem ser aninhados**. Qualquer `#` seguido imediatamente de `*` abre um comentário de bloco, inclusive em linhas decorativas como `#*****`.
- Quebras de linha dentro de um comentário de bloco não geram `NEWLINE`.
- O fim de arquivo dentro de um comentário de bloco é erro léxico.

## 9. Indentação e fim de linha

O scanner emite três tokens que não correspondem a texto visível: `NEWLINE` (fim de comando), `INDENT` (abertura de bloco) e `DEDENT` (fechamento de bloco). Para isso, ele mantém uma pilha de níveis de indentação que começa com `[0]`.

1. Apenas espaços contam para a indentação. Um tab no início de linha é erro léxico.
2. A indentação de uma linha é a coluna do seu primeiro token menos 1. Comentários não contam como token.
3. A primeira linha com conteúdo deve ter indentação 0.
4. Se a indentação for maior que o topo da pilha, o novo nível é empilhado e é emitido um `INDENT`. Qualquer aumento é aceito.
5. Se for igual ao topo, nada é emitido.
6. Se for menor, são desempilhados os níveis maiores que ela, com um `DEDENT` para cada um. Se o nível resultante não for exatamente igual à indentação da linha, ocorre um erro de indentação inconsistente.
7. Linhas vazias ou compostas apenas de comentários são ignoradas: não geram tokens nem alteram a pilha.
8. `NEWLINE` é emitido ao fim de cada linha com conteúdo, e nunca são emitidos dois `NEWLINE` seguidos.
9. No fim do arquivo, o scanner emite `NEWLINE` (se a última linha não tiver terminado com quebra de linha), depois um `DEDENT` para cada nível restante acima de 0 e, por fim, `EOF`.
10. Uma quebra de linha dentro de parênteses encerra o comando normalmente, ou seja, não há continuação implícita de linha.

O scanner verifica apenas a consistência da indentação. Um `INDENT` em posição indevida (por exemplo, após um comando que não termina em `:`) é aceito pelo léxico e rejeitado pela análise sintática.

## 10. Erros léxicos e recuperação

Todo erro é registrado com mensagem, linha e coluna, e o scanner continua a tokenização. Nenhum erro léxico interrompe a execução.

| Erro | Posição reportada | Recuperação |
|---|---|---|
| Caractere fora do alfabeto | o caractere | descarta o caractere |
| `!` não seguido de `=` | o `!` | descarta o `!` |
| Número seguido de letra ou `_` | início do número | consome toda a sequência alfanumérica |
| Real sem dígito após o ponto (`3.`) | início do número | consome o número e o ponto |
| String ou char não fechado até o fim da linha | aspa de abertura | retoma na linha seguinte, emitindo o `NEWLINE` normalmente |
| String ou char não fechado até o fim do arquivo | aspa de abertura | emite `EOF` |
| Escape inválido | a `\` | ignora o escape e continua a string ou o char |
| Char vazio ou com mais de um caractere | aspa de abertura | consome até a `'` de fechamento ou o fim da linha |
| Fim de arquivo dentro de comentário de bloco | o `#*` de abertura | emite `EOF` |
| Tab na indentação | início da linha | usa a indentação do topo da pilha |
| Primeira linha indentada | início da linha | trata a linha como nível 0 |
| Indentação inconsistente | início da linha | usa o nível resultante após desempilhar |

## 11. Exemplo

```
def dobro(n: int) -> int:  # comentário
    retorne n * 2
```

```
DEF ID(dobro) LPAREN ID(n) COLON INT RPAREN ARROW INT COLON NEWLINE
INDENT RETORNE ID(n) STAR INT_LIT(2) NEWLINE
DEDENT EOF
```

## 12. USO de IA

Após decidirmos toda a especificação do léxica da nossa linguagem, pedimos ajuda ao Claude para redigir o texto acima.

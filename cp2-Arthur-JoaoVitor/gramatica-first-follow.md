# Gramática da Linguagem Cascavel — FIRST, FOLLOW e verificação LL(1)

**Construção de Compiladores · UFU · Checkpoint 2 · Grupo: Arthur Marques, João Vitor Aguiar**

## 1. Convenções de notação

- **MAIÚSCULAS** são terminais, com o mesmo nome do `TokenType` do scanner do Checkpoint 1 (por exemplo, `SE`, `COLON`, `INT_LIT`). Cada terminal corresponde a um `match(TokenType.X)` no parser.
- **CamelCase** são não-terminais. Cada não-terminal corresponde a um método `parseX()`. Não-terminais com apóstrofo (`Soma'`) viram métodos com o sufixo `Linha` (`parseSomaLinha()`).
- `::=` separa o lado esquerdo do lado direito, `|` separa alternativas e `;` encerra a regra. O `;` é apenas notação: a Cascavel não tem ponto e vírgula.
- Na versão EBNF, `{ x }` indica zero ou mais repetições e `[ x ]` indica opcional.
- `ε` é a produção vazia.
- `EOF` é o token de fim de arquivo emitido pelo scanner. Ele cumpre o papel do `$` visto em aula.
- `NEWLINE`, `INDENT` e `DEDENT` são os tokens de estrutura emitidos pelo scanner (Seção 9 da especificação léxica). Para o parser, `INDENT` e `DEDENT` funcionam como abre e fecha bloco, e `NEWLINE` como terminador de comando simples.

## 2. Gramática inicial (EBNF)

Primeira versão, escrita a partir dos programas de exemplo do grupo. Ela ainda tem recursão à esquerda e prefixos comuns, tratados na Seção 3.

```
Programa    ::= { Def | Comando } EOF ;

Def         ::= DEF ID LPAREN Params RPAREN ARROW TipoRetorno COLON Bloco ;
Params      ::= [ Param { COMMA Param } ] ;
Param       ::= ID COLON Tipo ;
TipoRetorno ::= Tipo | NONE ;
Tipo        ::= INT | DOUBLE | BOOL | CHAR | STRING ;

Bloco       ::= NEWLINE INDENT Comando { Comando } DEDENT ;

Comando     ::= Declaracao | Atribuicao | Chamada | Se | Enquanto | Retorne | Passe ;
Declaracao  ::= ID COLON Tipo ASSIGN Expr NEWLINE ;
Atribuicao  ::= ID ASSIGN Expr NEWLINE ;
Chamada     ::= ID LPAREN Args RPAREN NEWLINE ;
Se          ::= SE Expr COLON Bloco { SENAOSE Expr COLON Bloco } [ SENAO COLON Bloco ] ;
Enquanto    ::= ENQUANTO Expr COLON Bloco ;
Retorne     ::= RETORNE [ Expr ] NEWLINE ;
Passe       ::= PASSE NEWLINE ;

Args        ::= [ Expr { COMMA Expr } ] ;

Expr        ::= Disjuncao ;
Disjuncao   ::= Disjuncao OU Conjuncao | Conjuncao ;
Conjuncao   ::= Conjuncao E Negacao | Negacao ;
Negacao     ::= NAO Negacao | Relacional ;
Relacional  ::= Soma | Soma OpRel Soma ;
OpRel       ::= EQ | NE | LT | LE | GT | GE ;
Soma        ::= Soma PLUS Termo | Soma MINUS Termo | Termo ;
Termo       ::= Termo STAR Fator | Termo SLASH Fator | Termo PERCENT Fator | Fator ;
Fator       ::= MINUS Fator | Primario ;
Primario    ::= INT_LIT | DOUBLE_LIT | STRING_LIT | CHAR_LIT | BOOL_LIT
              | ID | ID LPAREN Args RPAREN | LPAREN Expr RPAREN ;
```

### 2.1 Decisões sintáticas refletidas na gramática

- **Topo do programa:** definições de função (`def`) e comandos podem aparecer misturados no topo. Funções aninhadas não são permitidas, porque `Bloco` só contém `Comando`.
- **Declaração:** toda declaração tem inicialização (`x: int = 0`). `x: int` sozinho é erro sintático.
- **Atribuição:** a atribuição é um comando, nunca uma expressão.
- **Chamada como comando:** uma chamada de função pode ser usada como comando. Nenhuma outra expressão pode aparecer sozinha numa linha.
- **Funções:** o tipo de retorno é sempre explícito (`-> None` para procedimentos), e os parâmetros são sempre tipados.
- **`retorne`:** é aceito com ou sem valor, em qualquer bloco. A coerência com o tipo da função e a proibição de `retorne` fora de função ficam para a análise semântica.
- **Relacionais:** não são associativos (`a < b < c` é erro sintático).
- **Precedência**, do nível mais fraco para o mais forte: `ou` → `e` → `nao` → relacionais → `+ -` → `* / %` → `-` unário → primário.

## 3. Transformações

### 3.1 EBNF → BNF: repetições e opcionais

Cada `{ x }` e cada `[ x ]` vira um não-terminal novo com uma alternativa vazia. A escolha entre repetir (ou incluir) e parar passa a ser uma decisão entre alternativas, que a condição LL(1) controla (Seção 6).

| Construção EBNF | Não-terminal BNF |
|---|---|
| `{ Def \| Comando }` em `Programa` | `ListaItens ::= Item ListaItens \| ε` e `Item ::= Def \| Comando` |
| `[ Param { COMMA Param } ]` | `Params ::= Param ParamsResto \| ε` e `ParamsResto ::= COMMA Param ParamsResto \| ε` |
| `{ Comando }` em `Bloco` | `ListaComandos ::= Comando ListaComandos \| ε` |
| `{ SENAOSE Expr COLON Bloco }` | `ListaSenaose ::= SENAOSE Expr COLON Bloco ListaSenaose \| ε` |
| `[ SENAO COLON Bloco ]` | `SenaoOpc ::= SENAO COLON Bloco \| ε` |
| `[ Expr ]` em `Retorne` | `ExprOpc ::= Expr \| ε` |
| `[ Expr { COMMA Expr } ]` | `Args ::= Expr ArgsResto \| ε` e `ArgsResto ::= COMMA Expr ArgsResto \| ε` |

### 3.2 Eliminação de recursão à esquerda

Regra geral (Aula 4): `A ::= A α | β` vira `A ::= β A'` e `A' ::= α A' | ε`. Quando há várias alternativas recursivas (`A ::= A α1 | A α2 | β`), cada αi vira uma alternativa de `A'`.

Quatro não-terminais têm recursão à esquerda direta. Não há recursão à esquerda indireta: seguindo o primeiro símbolo de cada produção (Expr → Disjuncao → Conjuncao → Negacao → Relacional → Soma → Termo → Fator → Primario), nenhuma cadeia volta a um não-terminal já visitado. O único caminho de volta a `Expr` passa por `LPAREN Expr RPAREN`, em que um terminal é consumido antes da recursão.

**Disjuncao** (α = `OU Conjuncao`, β = `Conjuncao`):

```
antes:   Disjuncao  ::= Disjuncao OU Conjuncao | Conjuncao ;
depois:  Disjuncao  ::= Conjuncao Disjuncao' ;
         Disjuncao' ::= OU Conjuncao Disjuncao' | ε ;
```

**Conjuncao** (α = `E Negacao`, β = `Negacao`):

```
antes:   Conjuncao  ::= Conjuncao E Negacao | Negacao ;
depois:  Conjuncao  ::= Negacao Conjuncao' ;
         Conjuncao' ::= E Negacao Conjuncao' | ε ;
```

**Soma** (α1 = `PLUS Termo`, α2 = `MINUS Termo`, β = `Termo`):

```
antes:   Soma  ::= Soma PLUS Termo | Soma MINUS Termo | Termo ;
depois:  Soma  ::= Termo Soma' ;
         Soma' ::= PLUS Termo Soma' | MINUS Termo Soma' | ε ;
```

**Termo** (α1 = `STAR Fator`, α2 = `SLASH Fator`, α3 = `PERCENT Fator`, β = `Fator`):

```
antes:   Termo  ::= Termo STAR Fator | Termo SLASH Fator | Termo PERCENT Fator | Fator ;
depois:  Termo  ::= Fator Termo' ;
         Termo' ::= STAR Fator Termo' | SLASH Fator Termo' | PERCENT Fator Termo' | ε ;
```

`Negacao ::= NAO Negacao` e `Fator ::= MINUS Fator` são recursivos **à direita**, porque o terminal vem antes da recursão. Eles não precisam de transformação.

### 3.3 Fatoração à esquerda

Regra geral (Aula 5): `A ::= α β1 | α β2` vira `A ::= α A'` e `A' ::= β1 | β2`.

**Comando:** declaração, atribuição e chamada começam com `ID`. As três alternativas foram fundidas em uma, e o token seguinte (`COLON`, `ASSIGN` ou `LPAREN`) decide.

```
antes:   Comando    ::= Declaracao | Atribuicao | Chamada | Se | Enquanto | Retorne | Passe ;
         Declaracao ::= ID COLON Tipo ASSIGN Expr NEWLINE ;
         Atribuicao ::= ID ASSIGN Expr NEWLINE ;
         Chamada    ::= ID LPAREN Args RPAREN NEWLINE ;
depois:  Comando    ::= ID ComandoID | Se | Enquanto | Retorne | Passe ;
         ComandoID  ::= COLON Tipo ASSIGN Expr NEWLINE
                      | ASSIGN Expr NEWLINE
                      | LPAREN Args RPAREN NEWLINE ;
```

**Primario:** variável e chamada de função começam com `ID`.

```
antes:   Primario  ::= … | ID | ID LPAREN Args RPAREN | … ;
depois:  Primario  ::= … | ID Primario' | … ;
         Primario' ::= LPAREN Args RPAREN | ε ;
```

**Relacional:** as duas alternativas começam com `Soma`.

```
antes:   Relacional  ::= Soma | Soma OpRel Soma ;
depois:  Relacional  ::= Soma Relacional' ;
         Relacional' ::= OpRel Soma | ε ;
```

Repare que `Relacional'` não é recursivo. É isso que impede o encadeamento `a < b < c`: depois de um operador relacional e da `Soma` seguinte, não há como reconhecer outro operador relacional no mesmo nível.

## 4. Gramática final (BNF, LL(1))

```
P1   Programa      ::= ListaItens EOF ;
P2   ListaItens    ::= Item ListaItens | ε ;
P3   Item          ::= Def | Comando ;
P4   Def           ::= DEF ID LPAREN Params RPAREN ARROW TipoRetorno COLON Bloco ;
P5   Params        ::= Param ParamsResto | ε ;
P6   ParamsResto   ::= COMMA Param ParamsResto | ε ;
P7   Param         ::= ID COLON Tipo ;
P8   TipoRetorno   ::= Tipo | NONE ;
P9   Tipo          ::= INT | DOUBLE | BOOL | CHAR | STRING ;
P10  Bloco         ::= NEWLINE INDENT Comando ListaComandos DEDENT ;
P11  ListaComandos ::= Comando ListaComandos | ε ;
P12  Comando       ::= ID ComandoID | Se | Enquanto | Retorne | Passe ;
P13  ComandoID     ::= COLON Tipo ASSIGN Expr NEWLINE
                     | ASSIGN Expr NEWLINE
                     | LPAREN Args RPAREN NEWLINE ;
P14  Se            ::= SE Expr COLON Bloco ListaSenaose SenaoOpc ;
P15  ListaSenaose  ::= SENAOSE Expr COLON Bloco ListaSenaose | ε ;
P16  SenaoOpc      ::= SENAO COLON Bloco | ε ;
P17  Enquanto      ::= ENQUANTO Expr COLON Bloco ;
P18  Retorne       ::= RETORNE ExprOpc NEWLINE ;
P19  ExprOpc       ::= Expr | ε ;
P20  Passe         ::= PASSE NEWLINE ;
P21  Args          ::= Expr ArgsResto | ε ;
P22  ArgsResto     ::= COMMA Expr ArgsResto | ε ;
P23  Expr          ::= Disjuncao ;
P24  Disjuncao     ::= Conjuncao Disjuncao' ;
P25  Disjuncao'    ::= OU Conjuncao Disjuncao' | ε ;
P26  Conjuncao     ::= Negacao Conjuncao' ;
P27  Conjuncao'    ::= E Negacao Conjuncao' | ε ;
P28  Negacao       ::= NAO Negacao | Relacional ;
P29  Relacional    ::= Soma Relacional' ;
P30  Relacional'   ::= OpRel Soma | ε ;
P31  OpRel         ::= EQ | NE | LT | LE | GT | GE ;
P32  Soma          ::= Termo Soma' ;
P33  Soma'         ::= PLUS Termo Soma' | MINUS Termo Soma' | ε ;
P34  Termo         ::= Fator Termo' ;
P35  Termo'        ::= STAR Fator Termo' | SLASH Fator Termo' | PERCENT Fator Termo' | ε ;
P36  Fator         ::= MINUS Fator | Primario ;
P37  Primario      ::= INT_LIT | DOUBLE_LIT | STRING_LIT | CHAR_LIT | BOOL_LIT
                     | ID Primario' | LPAREN Expr RPAREN ;
P38  Primario'     ::= LPAREN Args RPAREN | ε ;
```

São 38 não-terminais, cada um correspondendo a um método `parseX()` no parser.

## 5. FIRST e FOLLOW

Para manter a tabela legível, os conjuntos que se repetem recebem um nome:

| Nome | Conjunto | Significado |
|---|---|---|
| **Lit** | { INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT, BOOL_LIT } | literais |
| **IniPrim** | Lit ∪ { ID, LPAREN } | início de um primário |
| **IniExpr** | IniPrim ∪ { MINUS, NAO } | início de qualquer expressão |
| **OpsRel** | { EQ, NE, LT, LE, GT, GE } | operadores relacionais |
| **IniCmd** | { ID, SE, ENQUANTO, RETORNE, PASSE } | início de um comando |
| **FimItem** | IniCmd ∪ { DEF, EOF } | o que pode vir depois de um item do topo |
| **FimCmd** | FimItem ∪ { DEDENT } | o que pode vir depois de um comando |
| **FimExpr** | { COLON, COMMA, NEWLINE, RPAREN } | o que pode vir depois de uma expressão completa |

Na coluna "Origem do FOLLOW", "fim de X (Pn)" significa que o não-terminal aparece no fim da produção Pn de X, então herda FOLLOW(X). "Antes de Y (Pn)" significa que ele é seguido por Y na produção Pn.

| Não-terminal | FIRST | FOLLOW | Origem do FOLLOW |
|---|---|---|---|
| Programa | IniCmd ∪ { DEF, EOF } | { $ } | símbolo inicial |
| ListaItens | IniCmd ∪ { DEF, ε } | { EOF } | antes de EOF (P1); fim de ListaItens (P2) |
| Item | IniCmd ∪ { DEF } | FimItem | antes de ListaItens (P2): FIRST(ListaItens) − ε ∪ FOLLOW(ListaItens) |
| Def | { DEF } | FimItem | fim de Item (P3) |
| Params | { ID, ε } | { RPAREN } | antes de RPAREN (P4) |
| ParamsResto | { COMMA, ε } | { RPAREN } | fim de Params (P5) e de ParamsResto (P6) |
| Param | { ID } | { COMMA, RPAREN } | antes de ParamsResto (P5, P6): { COMMA } ∪ FOLLOW(ParamsResto) |
| TipoRetorno | { INT, DOUBLE, BOOL, CHAR, STRING, NONE } | { COLON } | antes de COLON (P4) |
| Tipo | { INT, DOUBLE, BOOL, CHAR, STRING } | { ASSIGN, COLON, COMMA, RPAREN } | fim de Param (P7); fim de TipoRetorno (P8); antes de ASSIGN (P13) |
| Bloco | { NEWLINE } | FimCmd ∪ { SENAOSE, SENAO } | fim de Def (P4), Enquanto (P17) e SenaoOpc (P16); antes de ListaSenaose e SenaoOpc (P14, P15) |
| ListaComandos | IniCmd ∪ { ε } | { DEDENT } | antes de DEDENT (P10) |
| Comando | IniCmd | FimCmd | fim de Item (P3); antes de ListaComandos (P10, P11): IniCmd ∪ { DEDENT } |
| ComandoID | { COLON, ASSIGN, LPAREN } | FimCmd | fim de Comando (P12) |
| Se | { SE } | FimCmd | fim de Comando (P12) |
| ListaSenaose | { SENAOSE, ε } | FimCmd ∪ { SENAO } | antes de SenaoOpc (P14): { SENAO } ∪ FOLLOW(Se), pois SenaoOpc é anulável |
| SenaoOpc | { SENAO, ε } | FimCmd | fim de Se (P14) |
| Enquanto | { ENQUANTO } | FimCmd | fim de Comando (P12) |
| Retorne | { RETORNE } | FimCmd | fim de Comando (P12) |
| ExprOpc | IniExpr ∪ { ε } | { NEWLINE } | antes de NEWLINE (P18) |
| Passe | { PASSE } | FimCmd | fim de Comando (P12) |
| Args | IniExpr ∪ { ε } | { RPAREN } | antes de RPAREN (P13, P38) |
| ArgsResto | { COMMA, ε } | { RPAREN } | fim de Args (P21) e de ArgsResto (P22) |
| Expr | IniExpr | FimExpr | antes de NEWLINE (P13); antes de COLON (P14, P15, P17); fim de ExprOpc (P19); antes de ArgsResto (P21, P22); antes de RPAREN (P37) |
| Disjuncao | IniExpr | FimExpr | fim de Expr (P23) |
| Disjuncao' | { OU, ε } | FimExpr | fim de Disjuncao (P24) |
| Conjuncao | IniExpr | FimExpr ∪ { OU } | antes de Disjuncao' (P24, P25): { OU } ∪ FOLLOW(Disjuncao) |
| Conjuncao' | { E, ε } | FimExpr ∪ { OU } | fim de Conjuncao (P26) |
| Negacao | IniExpr | FimExpr ∪ { OU, E } | antes de Conjuncao' (P26, P27): { E } ∪ FOLLOW(Conjuncao); fim de Negacao (P28) |
| Relacional | IniPrim ∪ { MINUS } | FimExpr ∪ { OU, E } | fim de Negacao (P28) |
| Relacional' | OpsRel ∪ { ε } | FimExpr ∪ { OU, E } | fim de Relacional (P29) |
| OpRel | OpsRel | IniPrim ∪ { MINUS } | antes de Soma (P30): FIRST(Soma) |
| Soma | IniPrim ∪ { MINUS } | FimExpr ∪ { OU, E } ∪ OpsRel | antes de Relacional' (P29): OpsRel ∪ FOLLOW(Relacional); fim de Relacional' (P30) |
| Soma' | { PLUS, MINUS, ε } | FimExpr ∪ { OU, E } ∪ OpsRel | fim de Soma (P32) |
| Termo | IniPrim ∪ { MINUS } | FOLLOW(Soma) ∪ { PLUS, MINUS } | antes de Soma' (P32, P33): { PLUS, MINUS } ∪ FOLLOW(Soma) |
| Termo' | { STAR, SLASH, PERCENT, ε } | FOLLOW(Soma) ∪ { PLUS, MINUS } | fim de Termo (P34) |
| Fator | IniPrim ∪ { MINUS } | FOLLOW(Termo) ∪ { STAR, SLASH, PERCENT } | antes de Termo' (P34, P35): { STAR, SLASH, PERCENT } ∪ FOLLOW(Termo); fim de Fator (P36) |
| Primario | IniPrim | FOLLOW(Fator) | fim de Fator (P36) |
| Primario' | { LPAREN, ε } | FOLLOW(Fator) | fim de Primario (P37) |

Expandidos, os três FOLLOW mais longos são:

- FOLLOW(Soma) = { COLON, COMMA, NEWLINE, RPAREN, OU, E, EQ, NE, LT, LE, GT, GE }
- FOLLOW(Termo) = FOLLOW(Soma) ∪ { PLUS, MINUS }
- FOLLOW(Fator) = FOLLOW(Primario) = FOLLOW(Termo) ∪ { STAR, SLASH, PERCENT }

## 6. Verificação da condição LL(1)

Para cada não-terminal `A ::= α1 | … | αn`, verificamos as três cláusulas:

- **(a)** os FIRST das alternativas são disjuntos dois a dois;
- **(b)** no máximo uma alternativa é anulável;
- **(c)** se uma alternativa é anulável, o FIRST das demais é disjunto de FOLLOW(A).

### 6.1 Não-terminais com uma única alternativa

`Programa`, `Def`, `Param`, `Bloco`, `Se`, `Enquanto`, `Retorne`, `Passe`, `Expr`, `Disjuncao`, `Conjuncao`, `Relacional`, `Soma` e `Termo` têm uma única produção. Não há escolha a fazer, e as três cláusulas são satisfeitas trivialmente.

### 6.2 Não-terminais com várias alternativas, sem ε

| Não-terminal | FIRST de cada alternativa | (a) | (b) e (c) |
|---|---|---|---|
| Item | Def: { DEF } · Comando: IniCmd | DEF ∉ IniCmd ✔ | nenhuma alternativa anulável ✔ |
| TipoRetorno | Tipo: { INT, DOUBLE, BOOL, CHAR, STRING } · { NONE } | ✔ | ✔ |
| Tipo | cinco terminais distintos | ✔ | ✔ |
| Comando | { ID } · { SE } · { ENQUANTO } · { RETORNE } · { PASSE } | ✔ | ✔ |
| ComandoID | { COLON } · { ASSIGN } · { LPAREN } | ✔ (resultado da fatoração da Seção 3.3) | ✔ |
| Negacao | { NAO } · Relacional: IniPrim ∪ { MINUS } | NAO ∉ FIRST(Relacional) ✔ | ✔ |
| OpRel | seis terminais distintos | ✔ | ✔ |
| Fator | { MINUS } · Primario: IniPrim | MINUS ∉ IniPrim ✔ | ✔ |
| Primario | cinco literais · { ID } · { LPAREN } | ✔ (resultado da fatoração da Seção 3.3) | ✔ |

### 6.3 Não-terminais com alternativa ε

Em todos eles, há exatamente uma alternativa anulável, a própria ε, e a cláusula (b) é satisfeita. As cláusulas (a) e (c) são verificadas abaixo.

| Não-terminal | FIRST das alternativas não vazias | FOLLOW | (a) | (c): FIRST ∩ FOLLOW |
|---|---|---|---|---|
| ListaItens | IniCmd ∪ { DEF } | { EOF } | uma alternativa não vazia ✔ | ∅ ✔ |
| Params | { ID } | { RPAREN } | ✔ | ∅ ✔ |
| ParamsResto | { COMMA } | { RPAREN } | ✔ | ∅ ✔ |
| ListaComandos | IniCmd | { DEDENT } | ✔ | ∅ ✔ |
| ListaSenaose | { SENAOSE } | FimCmd ∪ { SENAO } | ✔ | ∅ ✔ |
| SenaoOpc | { SENAO } | FimCmd | ✔ | ∅ ✔ |
| ExprOpc | IniExpr | { NEWLINE } | ✔ | ∅ ✔ |
| Args | IniExpr | { RPAREN } | ✔ | ∅ ✔ |
| ArgsResto | { COMMA } | { RPAREN } | ✔ | ∅ ✔ |
| Disjuncao' | { OU } | FimExpr | ✔ | ∅ ✔ |
| Conjuncao' | { E } | FimExpr ∪ { OU } | ✔ | ∅ ✔ |
| Relacional' | OpsRel | FimExpr ∪ { OU, E } | ✔ | ∅ ✔ |
| Soma' | { PLUS } · { MINUS } | FOLLOW(Soma) | disjuntos ✔ | ∅ ✔ |
| Termo' | { STAR } · { SLASH } · { PERCENT } | FOLLOW(Termo) | disjuntos ✔ | ∅ ✔ |
| Primario' | { LPAREN } | FOLLOW(Fator) | ✔ | ∅ ✔ |

**Conclusão:** as três cláusulas valem para todos os 38 não-terminais. A gramática é LL(1).

### 6.4 Pontos que merecem atenção

- **SenaoOpc e ausência de dangling else.** A cláusula (c) só vale porque `SENAO ∉ FOLLOW(SenaoOpc)`. Na gramática clássica do dangling else, o `else` pertence ao FOLLOW do próprio comando condicional, e a cláusula falha. Na Cascavel, todo bloco termina com um `DEDENT` explícito. Por isso, depois do bloco de um `se` interno, o próximo token é um `DEDENT`, nunca o `senao` do `se` externo, e a ambiguidade não existe.
- **Soma' e o menos unário.** `MINUS` aparece tanto como operador binário (em `Soma'`) quanto como unário (em `Fator`). Não há conflito, porque `MINUS ∉ FOLLOW(Soma')`: uma expressão nunca é seguida diretamente por outra, e o `-` depois de uma expressão completa só pode ser binário.
- **Primario' e chamadas.** A cláusula (c) vale porque `LPAREN ∉ FOLLOW(Primario)`: na Cascavel, uma expressão nunca é seguida imediatamente de `(`. Por isso, um `ID` seguido de `(` é sempre uma chamada.
- **Relacionais encadeados.** Em `a < b < c`, o primeiro `<` é consumido por `Relacional'`. Depois de `b`, o `<` seguinte está em FOLLOW(Soma') (por isso `Soma'` escolhe ε), mas não está no FIRST nem no FOLLOW de `Conjuncao'`. O erro é detectado ali.

## 7. Observação para o Checkpoint 3

A eliminação da recursão à esquerda preserva a linguagem reconhecida, mas muda o formato da árvore de derivação: `a - b - c` passa a ser derivado com a recursão pendurada à direita (`Soma'` dentro de `Soma'`). Para um reconhecedor, isso é indiferente. Quando a AST for construída (Aula 7), a associatividade à esquerda (`(a - b) - c`) precisa ser restaurada na construção dos nós dos métodos `parseSomaLinha()`, `parseTermoLinha()`, `parseConjuncaoLinha()` e `parseDisjuncaoLinha()`.

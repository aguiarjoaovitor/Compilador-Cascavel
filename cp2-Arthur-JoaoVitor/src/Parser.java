package src;

import static src.TokenType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Parser {

    private static final Set<TokenType> ANCORAS = EnumSet.of(NEWLINE, DEDENT, EOF);
    private static final Set<TokenType> SYNC_COMANDO = ANCORAS;

    private static final Set<TokenType> INI_CMD = EnumSet.of(ID, SE, ENQUANTO, RETORNE, PASSE);
    private static final Set<TokenType> INI_ITEM = uniao(INI_CMD, EnumSet.of(DEF));
    private static final Set<TokenType> TIPOS = EnumSet.of(INT, DOUBLE, BOOL, CHAR, STRING);
    private static final Set<TokenType> LITERAIS = EnumSet.of(INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT, BOOL_LIT);
    private static final Set<TokenType> INI_PRIM = uniao(LITERAIS, EnumSet.of(ID, LPAREN));
    private static final Set<TokenType> INI_RELACIONAL = uniao(INI_PRIM, EnumSet.of(MINUS));
    private static final Set<TokenType> INI_EXPR = uniao(INI_RELACIONAL, EnumSet.of(NAO));
    private static final Set<TokenType> OPS_REL = EnumSet.of(EQ, NE, LT, LE, GT, GE);

    private static final Set<TokenType> FIM_ITEM = uniao(INI_CMD, EnumSet.of(DEF, EOF));
    private static final Set<TokenType> FIM_CMD = uniao(FIM_ITEM, EnumSet.of(DEDENT));
    private static final Set<TokenType> FIM_EXPR = EnumSet.of(COLON, COMMA, NEWLINE, RPAREN);

    private static final Set<TokenType> FOLLOW_PROGRAMA = EnumSet.of(EOF);
    private static final Set<TokenType> FOLLOW_LISTA_ITENS = EnumSet.of(EOF);
    private static final Set<TokenType> FOLLOW_ITEM = FIM_ITEM;
    private static final Set<TokenType> FOLLOW_PARAMS = EnumSet.of(RPAREN);
    private static final Set<TokenType> FOLLOW_PARAM = EnumSet.of(COMMA, RPAREN);
    private static final Set<TokenType> FOLLOW_TIPO_RETORNO = EnumSet.of(COLON);
    private static final Set<TokenType> FOLLOW_TIPO = EnumSet.of(ASSIGN, COLON, COMMA, RPAREN);
    private static final Set<TokenType> FOLLOW_BLOCO = uniao(FIM_CMD, EnumSet.of(SENAOSE, SENAO));
    private static final Set<TokenType> FOLLOW_LISTA_COMANDOS = EnumSet.of(DEDENT);
    private static final Set<TokenType> FOLLOW_LISTA_SENAOSE = uniao(FIM_CMD, EnumSet.of(SENAO));
    private static final Set<TokenType> FOLLOW_SENAO_OPC = FIM_CMD;
    private static final Set<TokenType> FOLLOW_EXPR_OPC = EnumSet.of(NEWLINE);
    private static final Set<TokenType> FOLLOW_ARGS = EnumSet.of(RPAREN);
    private static final Set<TokenType> FOLLOW_EXPR = FIM_EXPR;
    private static final Set<TokenType> FOLLOW_CONJUNCAO = uniao(FIM_EXPR, EnumSet.of(OU));
    private static final Set<TokenType> FOLLOW_NEGACAO = uniao(FOLLOW_CONJUNCAO, EnumSet.of(E));
    private static final Set<TokenType> FOLLOW_OP_REL = INI_RELACIONAL;
    private static final Set<TokenType> FOLLOW_SOMA = uniao(FOLLOW_NEGACAO, OPS_REL);
    private static final Set<TokenType> FOLLOW_TERMO = uniao(FOLLOW_SOMA, EnumSet.of(PLUS, MINUS));
    private static final Set<TokenType> FOLLOW_FATOR = uniao(FOLLOW_TERMO, EnumSet.of(STAR, SLASH, PERCENT));

    private final Scanner scanner;
    private final List<ErroSintatico> erros = new ArrayList<>();
    private Token atual;
    private boolean emPanico = false;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        avancar();
    }

    public boolean parse() {
        parsePrograma();
        return erros.isEmpty() && scanner.erros.isEmpty();
    }

    public List<ErroSintatico> getErros() {
        return Collections.unmodifiableList(erros);
    }

    public List<String> getErrosLexicos() {
        return Collections.unmodifiableList(scanner.erros);
    }


    private void avancar() {
        if (atual != null && atual.tipo == EOF) {
            return;
        }
        atual = scanner.proximoToken();
    }

    private boolean match(TokenType esperado, Set<TokenType> follow) {
        if (atual.tipo == esperado) {
            avancar();
            emPanico = false;
            return true;
        }
        erro("esperava " + nome(esperado) + ", encontrou " + descrever(atual));
        sincronizar(follow);
        return false;
    }

    private boolean matchDoisPontos() {
        if (match(COLON, EnumSet.of(COLON))) {
            return true;
        }
        if (atual.tipo == COLON) {
            avancar();
            return true;
        }
        return atual.tipo == NEWLINE;
    }

    private void erro(String mensagem) {
        if (!emPanico) {
            erros.add(new ErroSintatico(atual.linha, atual.coluna, mensagem));
        }
        emPanico = true;
    }

    private void sincronizar(Set<TokenType> follow) {
        while (!follow.contains(atual.tipo) && !ANCORAS.contains(atual.tipo)) {
            avancar();
        }
    }

    private void recuperarLinhaInvalida() {
        if (atual.tipo == INDENT) {
            erro("indentação inesperada");
            avancar();
            parseListaComandos();
            match(DEDENT, SYNC_COMANDO);
            return;
        }
        if (atual.tipo == DEDENT) {
            erro("recuo inesperado");
            avancar();
            return;
        }
        if (atual.tipo == DEF) {
            erro(mensagemComandoInvalido());
            parseDef();
            return;
        }
        erro(mensagemComandoInvalido());
        while (!ANCORAS.contains(atual.tipo)) {
            avancar();
        }
        if (atual.tipo == NEWLINE) {
            avancar();
            if (atual.tipo != INDENT) {
                emPanico = false;
            }
        }
    }

    private String mensagemComandoInvalido() {
        if (atual.tipo == DEF) {
            return "'def' só é permitido no nível de topo, fora de blocos";
        }
        return "esperava um comando, encontrou " + descrever(atual);
    }


    private void parsePrograma() {
        parseListaItens();
        match(EOF, FOLLOW_PROGRAMA);
    }

    private void parseListaItens() {
        if (INI_ITEM.contains(atual.tipo)) {
            parseItem();
            parseListaItens();
        } else if (FOLLOW_LISTA_ITENS.contains(atual.tipo)) {
            return;
        } else {
            recuperarLinhaInvalida();
            parseListaItens();
        }
    }

    private void parseItem() {
        if (atual.tipo == DEF) {
            parseDef();
        } else if (INI_CMD.contains(atual.tipo)) {
            parseComando();
        } else {
            erro(mensagemComandoInvalido());
            sincronizar(FOLLOW_ITEM);
        }
    }

    private void parseDef() {
        if (!match(DEF, SYNC_COMANDO)) return;
        if (!match(ID, SYNC_COMANDO)) return;
        if (!match(LPAREN, SYNC_COMANDO)) return;
        parseParams();
        if (!match(RPAREN, SYNC_COMANDO)) return;
        if (!match(ARROW, SYNC_COMANDO)) return;
        parseTipoRetorno();
        if (!matchDoisPontos()) return;
        parseBloco();
    }

    private void parseParams() {
        if (atual.tipo == ID) {
            parseParam();
            parseParamsResto();
        } else if (FOLLOW_PARAMS.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava um parâmetro ou ')', encontrou " + descrever(atual));
            sincronizar(FOLLOW_PARAMS);
        }
    }

    private void parseParamsResto() {
        if (atual.tipo == COMMA) {
            match(COMMA, FOLLOW_PARAMS);
            parseParam();
            parseParamsResto();
        } else if (FOLLOW_PARAMS.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava ',' ou ')', encontrou " + descrever(atual));
            sincronizar(FOLLOW_PARAMS);
        }
    }

    private void parseParam() {
        if (!match(ID, FOLLOW_PARAM)) return;
        if (!match(COLON, FOLLOW_PARAM)) return;
        parseTipo();
    }

    private void parseTipoRetorno() {
        if (TIPOS.contains(atual.tipo)) {
            parseTipo();
        } else if (atual.tipo == NONE) {
            match(NONE, FOLLOW_TIPO_RETORNO);
        } else {
            erro("esperava um tipo de retorno ou 'None', encontrou " + descrever(atual));
            sincronizar(FOLLOW_TIPO_RETORNO);
        }
    }

    private void parseTipo() {
        if (TIPOS.contains(atual.tipo)) {
            match(atual.tipo, FOLLOW_TIPO);
        } else {
            erro("esperava um tipo (int, double, bool, char ou string), encontrou " + descrever(atual));
            sincronizar(FOLLOW_TIPO);
        }
    }


    private void parseBloco() {
        if (!match(NEWLINE, SYNC_COMANDO)) return;
        if (!match(INDENT, FOLLOW_BLOCO)) return;
        parseComando();
        parseListaComandos();
        match(DEDENT, FOLLOW_BLOCO);
    }

    private void parseListaComandos() {
        if (INI_CMD.contains(atual.tipo)) {
            parseComando();
            parseListaComandos();
        } else if (FOLLOW_LISTA_COMANDOS.contains(atual.tipo)) {
            return;
        } else if (atual.tipo != EOF) {
            recuperarLinhaInvalida();
            parseListaComandos();
        }
    }

    private void parseComando() {
        if (atual.tipo == ID) {
            match(ID, SYNC_COMANDO);
            parseComandoID();
        } else if (atual.tipo == SE) {
            parseSe();
        } else if (atual.tipo == ENQUANTO) {
            parseEnquanto();
        } else if (atual.tipo == RETORNE) {
            parseRetorne();
        } else if (atual.tipo == PASSE) {
            parsePasse();
        } else {
            recuperarLinhaInvalida();
        }
    }

    private void parseComandoID() {
        if (atual.tipo == COLON) {
            match(COLON, SYNC_COMANDO);
            parseTipo();
            if (!match(ASSIGN, SYNC_COMANDO)) return;
            parseExpr();
            match(NEWLINE, SYNC_COMANDO);
        } else if (atual.tipo == ASSIGN) {
            match(ASSIGN, SYNC_COMANDO);
            parseExpr();
            match(NEWLINE, SYNC_COMANDO);
        } else if (atual.tipo == LPAREN) {
            match(LPAREN, SYNC_COMANDO);
            parseArgs();
            if (!match(RPAREN, SYNC_COMANDO)) return;
            match(NEWLINE, SYNC_COMANDO);
        } else {
            erro("esperava ':', '=' ou '(' depois do identificador, encontrou " + descrever(atual));
            sincronizar(SYNC_COMANDO);
        }
    }

    private void parseSe() {
        match(SE, SYNC_COMANDO);
        parseExpr();
        if (!matchDoisPontos()) return;
        parseBloco();
        parseListaSenaose();
        parseSenaoOpc();
    }

    private void parseListaSenaose() {
        if (atual.tipo == SENAOSE) {
            match(SENAOSE, FOLLOW_LISTA_SENAOSE);
            parseExpr();
            if (!matchDoisPontos()) return;
            parseBloco();
            parseListaSenaose();
        } else if (FOLLOW_LISTA_SENAOSE.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava 'senaose', 'senao' ou um novo comando, encontrou " + descrever(atual));
            sincronizar(SYNC_COMANDO);
        }
    }

    private void parseSenaoOpc() {
        if (atual.tipo == SENAO) {
            match(SENAO, FOLLOW_SENAO_OPC);
            if (!matchDoisPontos()) return;
            parseBloco();
        } else if (FOLLOW_SENAO_OPC.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava 'senao' ou um novo comando, encontrou " + descrever(atual));
            sincronizar(SYNC_COMANDO);
        }
    }

    private void parseEnquanto() {
        match(ENQUANTO, SYNC_COMANDO);
        parseExpr();
        if (!matchDoisPontos()) return;
        parseBloco();
    }

    private void parseRetorne() {
        match(RETORNE, SYNC_COMANDO);
        parseExprOpc();
        match(NEWLINE, SYNC_COMANDO);
    }

    private void parseExprOpc() {
        if (INI_EXPR.contains(atual.tipo)) {
            parseExpr();
        } else if (FOLLOW_EXPR_OPC.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava uma expressão ou fim de linha depois de 'retorne', encontrou " + descrever(atual));
            sincronizar(FOLLOW_EXPR_OPC);
        }
    }

    private void parsePasse() {
        match(PASSE, SYNC_COMANDO);
        match(NEWLINE, SYNC_COMANDO);
    }

    private void parseArgs() {
        if (INI_EXPR.contains(atual.tipo)) {
            parseExpr();
            parseArgsResto();
        } else if (FOLLOW_ARGS.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava um argumento ou ')', encontrou " + descrever(atual));
            sincronizar(FOLLOW_ARGS);
        }
    }

    private void parseArgsResto() {
        if (atual.tipo == COMMA) {
            match(COMMA, FOLLOW_ARGS);
            parseExpr();
            parseArgsResto();
        } else if (FOLLOW_ARGS.contains(atual.tipo)) {
            return;
        } else {
            erro("esperava ',' ou ')', encontrou " + descrever(atual));
            sincronizar(FOLLOW_ARGS);
        }
    }


    private void parseExpr() {
        parseDisjuncao();
    }

    private void parseDisjuncao() {
        parseConjuncao();
        parseDisjuncaoLinha();
    }

    private void parseDisjuncaoLinha() {
        if (atual.tipo == OU) {
            match(OU, FOLLOW_EXPR);
            parseConjuncao();
            parseDisjuncaoLinha();
        } else if (FOLLOW_EXPR.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_EXPR);
        }
    }

    private void parseConjuncao() {
        parseNegacao();
        parseConjuncaoLinha();
    }

    private void parseConjuncaoLinha() {
        if (atual.tipo == E) {
            match(E, FOLLOW_CONJUNCAO);
            parseNegacao();
            parseConjuncaoLinha();
        } else if (FOLLOW_CONJUNCAO.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_CONJUNCAO);
        }
    }

    private void parseNegacao() {
        if (atual.tipo == NAO) {
            match(NAO, FOLLOW_NEGACAO);
            parseNegacao();
        } else if (INI_RELACIONAL.contains(atual.tipo)) {
            parseRelacional();
        } else {
            erro("esperava uma expressão, encontrou " + descrever(atual));
            sincronizar(FOLLOW_NEGACAO);
        }
    }

    private void parseRelacional() {
        parseSoma();
        parseRelacionalLinha();
    }

    private void parseRelacionalLinha() {
        if (OPS_REL.contains(atual.tipo)) {
            parseOpRel();
            parseSoma();
        } else if (FOLLOW_NEGACAO.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_NEGACAO);
        }
    }

    private void parseOpRel() {
        if (OPS_REL.contains(atual.tipo)) {
            match(atual.tipo, FOLLOW_OP_REL);
        } else {
            erro("esperava um operador relacional, encontrou " + descrever(atual));
            sincronizar(FOLLOW_OP_REL);
        }
    }

    private void parseSoma() {
        parseTermo();
        parseSomaLinha();
    }

    private void parseSomaLinha() {
        if (atual.tipo == PLUS || atual.tipo == MINUS) {
            match(atual.tipo, FOLLOW_SOMA);
            parseTermo();
            parseSomaLinha();
        } else if (FOLLOW_SOMA.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_SOMA);
        }
    }

    private void parseTermo() {
        parseFator();
        parseTermoLinha();
    }

    private void parseTermoLinha() {
        if (atual.tipo == STAR || atual.tipo == SLASH || atual.tipo == PERCENT) {
            match(atual.tipo, FOLLOW_TERMO);
            parseFator();
            parseTermoLinha();
        } else if (FOLLOW_TERMO.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_TERMO);
        }
    }

    private void parseFator() {
        if (atual.tipo == MINUS) {
            match(MINUS, FOLLOW_FATOR);
            parseFator();
        } else if (INI_PRIM.contains(atual.tipo)) {
            parsePrimario();
        } else {
            erro("esperava uma expressão, encontrou " + descrever(atual));
            sincronizar(FOLLOW_FATOR);
        }
    }

    private void parsePrimario() {
        if (LITERAIS.contains(atual.tipo)) {
            match(atual.tipo, FOLLOW_FATOR);
        } else if (atual.tipo == ID) {
            match(ID, FOLLOW_FATOR);
            parsePrimarioLinha();
        } else if (atual.tipo == LPAREN) {
            match(LPAREN, FOLLOW_FATOR);
            parseExpr();
            match(RPAREN, FOLLOW_FATOR);
        } else {
            erro("esperava uma expressão, encontrou " + descrever(atual));
            sincronizar(FOLLOW_FATOR);
        }
    }

    private void parsePrimarioLinha() {
        if (atual.tipo == LPAREN) {
            match(LPAREN, FOLLOW_FATOR);
            parseArgs();
            match(RPAREN, FOLLOW_FATOR);
        } else if (FOLLOW_FATOR.contains(atual.tipo)) {
            return;
        } else {
            erro("token inesperado na expressão: " + descrever(atual));
            sincronizar(FOLLOW_FATOR);
        }
    }


    private static String descrever(Token token) {
        switch (token.tipo) {
            case NEWLINE:
            case INDENT:
            case DEDENT:
            case EOF:
                return nome(token.tipo);
            default:
                return "'" + token.lexema + "'";
        }
    }

    private static String nome(TokenType tipo) {
        switch (tipo) {
            case NEWLINE: return "fim de linha";
            case INDENT: return "bloco indentado";
            case DEDENT: return "fim de bloco";
            case EOF: return "fim do arquivo";
            case ID: return "identificador";
            case INT_LIT: return "literal inteiro";
            case DOUBLE_LIT: return "literal real";
            case STRING_LIT: return "literal string";
            case CHAR_LIT: return "literal char";
            case BOOL_LIT: return "literal booleano";
            case NONE: return "'None'";
            case PLUS: return "'+'";
            case MINUS: return "'-'";
            case STAR: return "'*'";
            case SLASH: return "'/'";
            case PERCENT: return "'%'";
            case ASSIGN: return "'='";
            case ARROW: return "'->'";
            case EQ: return "'=='";
            case NE: return "'!='";
            case LT: return "'<'";
            case LE: return "'<='";
            case GT: return "'>'";
            case GE: return "'>='";
            case LPAREN: return "'('";
            case RPAREN: return "')'";
            case COLON: return "':'";
            case COMMA: return "','";
            default: return "'" + tipo.name().toLowerCase() + "'";
        }
    }

    private static Set<TokenType> uniao(Set<TokenType> a, Set<TokenType> b) {
        Set<TokenType> resultado = EnumSet.noneOf(TokenType.class);
        resultado.addAll(a);
        resultado.addAll(b);
        return resultado;
    }
}

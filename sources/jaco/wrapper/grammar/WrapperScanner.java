//      /   _ _      JaCo
//  \  //\ / / \     - an extended Java scanner
//   \//  \\_\_/     
//         \         Matthias Zenger, 30/05/99

package jaco.wrapper.grammar;

import jaco.framework.*;
import jaco.wrapper.context.*;


public class Scanner extends jaco.java.grammar.Scanner {

    public Scanner(WrapperSyntacticContext context, Sourcefile source) {
        super(context, source);
        // initialize scanner constants
        DOUBLELITERAL = Symbols.DOUBLELITERAL;
        FLOATLITERAL = Symbols.FLOATLITERAL;
        LONGLITERAL = Symbols.LONGLITERAL;
        INTLITERAL = Symbols.INTLITERAL;
        CHARLITERAL = Symbols.CHARLITERAL;
        STRINGLITERAL = Symbols.STRINGLITERAL;
        IDENTIFIER = Symbols.IDENTIFIER;
        DOT = Symbols.DOT;
        COMMA = Symbols.COMMA;
        SEMI = Symbols.SEMI;
        LPAREN = Symbols.LPAREN;
        RPAREN = Symbols.RPAREN;
        LBRACKET = Symbols.LBRACKET;
        RBRACKET = Symbols.RBRACKET;
        LBRACE = Symbols.LBRACE;
        RBRACE = Symbols.RBRACE;
        SLASHEQ = Symbols.SLASHEQ;
        SLASH = Symbols.SLASH;
        USUB = Symbols.USUB;
        EOF = Symbols.EOF;
        error = Symbols.error;
    }
    
    protected void initKeywords() {
        // initialize keywords
        enterKeyword("+", Symbols.PLUS);
        enterKeyword("-", Symbols.SUB);
        enterKeyword("!", Symbols.BANG);
        enterKeyword("%", Symbols.PERCENT);
        enterKeyword("^", Symbols.CARET);
        enterKeyword("&", Symbols.AMP);
        enterKeyword("*", Symbols.STAR);
        enterKeyword("|", Symbols.BAR);
        enterKeyword("~", Symbols.TILDE);
        enterKeyword("/", Symbols.SLASH);
        enterKeyword(">", Symbols.GT);
        enterKeyword("<", Symbols.LT);
        enterKeyword("?", Symbols.QUES);
        enterKeyword(":", Symbols.COLON);
        enterKeyword("=", Symbols.EQ);
        enterKeyword("++", Symbols.PLUSPLUS);
        enterKeyword("--", Symbols.SUBSUB);
        enterKeyword("==", Symbols.EQEQ);
        enterKeyword("<=", Symbols.LTEQ);
        enterKeyword(">=", Symbols.GTEQ);
        enterKeyword("!=", Symbols.BANGEQ);
        enterKeyword("<<", Symbols.LTLT);
        enterKeyword(">>", Symbols.GTGT);
        enterKeyword(">>>", Symbols.GTGTGT);
        enterKeyword("+=", Symbols.PLUSEQ);
        enterKeyword("-=", Symbols.SUBEQ);
        enterKeyword("*=", Symbols.STAREQ);
        enterKeyword("/=", Symbols.SLASHEQ);
        enterKeyword("&=", Symbols.AMPEQ);
        enterKeyword("|=", Symbols.BAREQ);
        enterKeyword("^=", Symbols.CARETEQ);
        enterKeyword("%=", Symbols.PERCENTEQ);
        enterKeyword("<<=", Symbols.LTLTEQ);
        enterKeyword(">>=", Symbols.GTGTEQ);
        enterKeyword(">>>=", Symbols.GTGTGTEQ);
        enterKeyword("||", Symbols.BARBAR);
        enterKeyword("&&", Symbols.AMPAMP);
        enterKeyword("abstract", Symbols.ABSTRACT);
        enterKeyword("break", Symbols.BREAK);
        enterKeyword("case", Symbols.CASE);
        enterKeyword("catch", Symbols.CATCH);
        enterKeyword("class", Symbols.CLASS);
        enterKeyword("const", Symbols.CONST);
        enterKeyword("continue", Symbols.CONTINUE);
        enterKeyword("default", Symbols.DEFAULT);
        enterKeyword("do", Symbols.DO);
        enterKeyword("else", Symbols.ELSE);
        enterKeyword("extends", Symbols.EXTENDS);
        enterKeyword("final", Symbols.FINAL);
        enterKeyword("finally", Symbols.FINALLY);
        enterKeyword("for", Symbols.FOR);
        enterKeyword("goto", Symbols.GOTO);
        enterKeyword("if", Symbols.IF);
        enterKeyword("implements", Symbols.IMPLEMENTS);
        enterKeyword("import", Symbols.IMPORT);
        enterKeyword("interface", Symbols.INTERFACE);
        enterKeyword("native", Symbols.NATIVE);
        enterKeyword("new", Symbols.NEW);
        enterKeyword("package", Symbols.PACKAGE);
        enterKeyword("private", Symbols.PRIVATE);
        enterKeyword("protected", Symbols.PROTECTED);
        enterKeyword("public", Symbols.PUBLIC);
        enterKeyword("return", Symbols.RETURN);
        enterKeyword("static", Symbols.STATIC);
        enterKeyword("strictfp", Symbols.STRICTFP);
        enterKeyword("super", Symbols.SUPER);
        enterKeyword("switch", Symbols.SWITCH);
        enterKeyword("synchronized", Symbols.SYNCHRONIZED);
        enterKeyword("this", Symbols.THIS);
        enterKeyword("volatile", Symbols.VOLATILE);
        enterKeyword("throw", Symbols.THROW);
        enterKeyword("throws", Symbols.THROWS);
        enterKeyword("transient", Symbols.TRANSIENT);
        enterKeyword("try", Symbols.TRY);
        enterKeyword("while", Symbols.WHILE);
        enterKeyword("instanceof", Symbols.INSTANCEOF);
        enterKeyword("boolean", Symbols.BOOLEAN);
        enterKeyword("byte", Symbols.BYTE);
        enterKeyword("char", Symbols.CHAR);
        enterKeyword("double", Symbols.DOUBLE);
        enterKeyword("float", Symbols.FLOAT);
        enterKeyword("int", Symbols.INT);
        enterKeyword("long", Symbols.LONG);
        enterKeyword("short", Symbols.SHORT);
        enterKeyword("void", Symbols.VOID);
        if (asserts)
            enterKeyword("assert", Symbols.ASSERT);
    }
}

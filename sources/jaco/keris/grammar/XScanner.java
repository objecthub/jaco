//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               Keris scanner

package jaco.keris.grammar;

import jaco.framework.*;
import jaco.java.grammar.*;
import jaco.java.context.*;


public class XScanner extends Scanner {
    
    public XScanner(SyntacticContext context, Sourcefile source) {
        super(context, source);
    }
    
    protected void initKeywords() {
        // initialize scanner constants
        DOUBLELITERAL = XSymbols.DOUBLELITERAL;
        FLOATLITERAL = XSymbols.FLOATLITERAL;
        LONGLITERAL = XSymbols.LONGLITERAL;
        INTLITERAL = XSymbols.INTLITERAL;
        CHARLITERAL = XSymbols.CHARLITERAL;
        STRINGLITERAL = XSymbols.STRINGLITERAL;
        IDENTIFIER = XSymbols.IDENTIFIER;
        DOT = XSymbols.DOT;
        COMMA = XSymbols.COMMA;
        SEMI = XSymbols.SEMI;
        LPAREN = XSymbols.LPAREN;
        RPAREN = XSymbols.RPAREN;
        LBRACKET = XSymbols.LBRACKET;
        RBRACKET = XSymbols.RBRACKET;
        LBRACE = XSymbols.LBRACE;
        RBRACE = XSymbols.RBRACE;
        SLASHEQ = XSymbols.SLASHEQ;
        SLASH = XSymbols.SLASH;
        USUB = XSymbols.USUB;
        EOF = XSymbols.EOF;
        error = XSymbols.error;
        
        // initialize keywords
        enterKeyword("+", XSymbols.PLUS);
        enterKeyword("-", XSymbols.SUB);
        enterKeyword("!", XSymbols.BANG);
        enterKeyword("%", XSymbols.PERCENT);
        enterKeyword("^", XSymbols.CARET);
        enterKeyword("&", XSymbols.AMP);
        enterKeyword("*", XSymbols.STAR);
        enterKeyword("|", XSymbols.BAR);
        enterKeyword("~", XSymbols.TILDE);
        enterKeyword("/", XSymbols.SLASH);
        enterKeyword(">", XSymbols.GT);
        enterKeyword("<", XSymbols.LT);
        enterKeyword("?", XSymbols.QUES);
        enterKeyword(":", XSymbols.COLON);
        enterKeyword("=", XSymbols.EQ);
        enterKeyword("++", XSymbols.PLUSPLUS);
        enterKeyword("--", XSymbols.SUBSUB);
        enterKeyword("==", XSymbols.EQEQ);
        enterKeyword("<=", XSymbols.LTEQ);
        enterKeyword(">=", XSymbols.GTEQ);
        enterKeyword("!=", XSymbols.BANGEQ);
        enterKeyword("<<", XSymbols.LTLT);
        enterKeyword(">>", XSymbols.GTGT);
        enterKeyword(">>>", XSymbols.GTGTGT);
        enterKeyword("+=", XSymbols.PLUSEQ);
        enterKeyword("-=", XSymbols.SUBEQ);
        enterKeyword("*=", XSymbols.STAREQ);
        enterKeyword("/=", XSymbols.SLASHEQ);
        enterKeyword("&=", XSymbols.AMPEQ);
        enterKeyword("|=", XSymbols.BAREQ);
        enterKeyword("^=", XSymbols.CARETEQ);
        enterKeyword("%=", XSymbols.PERCENTEQ);
        enterKeyword("<<=", XSymbols.LTLTEQ);
        enterKeyword(">>=", XSymbols.GTGTEQ);
        enterKeyword(">>>=", XSymbols.GTGTGTEQ);
        enterKeyword("||", XSymbols.BARBAR);
        enterKeyword("&&", XSymbols.AMPAMP);
        enterKeyword("abstract", XSymbols.ABSTRACT);
        enterKeyword("break", XSymbols.BREAK);
        enterKeyword("case", XSymbols.CASE);
        enterKeyword("catch", XSymbols.CATCH);
        enterKeyword("class", XSymbols.CLASS);
        enterKeyword("const", XSymbols.CONST);
        enterKeyword("continue", XSymbols.CONTINUE);
        enterKeyword("default", XSymbols.DEFAULT);
        enterKeyword("do", XSymbols.DO);
        enterKeyword("else", XSymbols.ELSE);
        enterKeyword("extends", XSymbols.EXTENDS);
        enterKeyword("final", XSymbols.FINAL);
        enterKeyword("finally", XSymbols.FINALLY);
        enterKeyword("for", XSymbols.FOR);
        enterKeyword("goto", XSymbols.GOTO);
        enterKeyword("if", XSymbols.IF);
        enterKeyword("implements", XSymbols.IMPLEMENTS);
        enterKeyword("import", XSymbols.IMPORT);
        enterKeyword("interface", XSymbols.INTERFACE);
        enterKeyword("native", XSymbols.NATIVE);
        enterKeyword("new", XSymbols.NEW);
        enterKeyword("package", XSymbols.PACKAGE);
        enterKeyword("private", XSymbols.PRIVATE);
        enterKeyword("protected", XSymbols.PROTECTED);
        enterKeyword("public", XSymbols.PUBLIC);
        enterKeyword("return", XSymbols.RETURN);
        enterKeyword("static", XSymbols.STATIC);
        enterKeyword("strictfp", XSymbols.STRICTFP);
        enterKeyword("super", XSymbols.SUPER);
        enterKeyword("switch", XSymbols.SWITCH);
        enterKeyword("synchronized", XSymbols.SYNCHRONIZED);
        enterKeyword("this", XSymbols.THIS);
        enterKeyword("volatile", XSymbols.VOLATILE);
        enterKeyword("throw", XSymbols.THROW);
        enterKeyword("throws", XSymbols.THROWS);
        enterKeyword("transient", XSymbols.TRANSIENT);
        enterKeyword("try", XSymbols.TRY);
        enterKeyword("while", XSymbols.WHILE);
        enterKeyword("instanceof", XSymbols.INSTANCEOF);
        enterKeyword("boolean", XSymbols.BOOLEAN);
        enterKeyword("byte", XSymbols.BYTE);
        enterKeyword("char", XSymbols.CHAR);
        enterKeyword("double", XSymbols.DOUBLE);
        enterKeyword("float", XSymbols.FLOAT);
        enterKeyword("int", XSymbols.INT);
        enterKeyword("long", XSymbols.LONG);
        enterKeyword("short", XSymbols.SHORT);
        enterKeyword("void", XSymbols.VOID);
        enterKeyword("assert", XSymbols.ASSERT);
        enterKeyword("_", XSymbols.SUBSCR);
        enterKeyword("module", XSymbols.MODULE);
        enterKeyword("requires", XSymbols.REQUIRES);
        enterKeyword("refines", XSymbols.REFINES);
        enterKeyword("specializes", XSymbols.SPECIALIZES);
        enterKeyword("::", XSymbols.COLONCOLON);
        enterKeyword("as", XSymbols.AS);
    }
}

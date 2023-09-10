//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.jjava.grammar;

import jaco.framework.*;
import jaco.java.grammar.*;
import jaco.java.context.*;


public class JScanner extends Scanner
{
    public JScanner(SyntacticContext context, Sourcefile source)
    {
        super(context, source);
    }
    
    protected void initKeywords()
    {
        // initialize scanner constants
        DOUBLELITERAL = JSymbols.DOUBLELITERAL;
        FLOATLITERAL = JSymbols.FLOATLITERAL;
        LONGLITERAL = JSymbols.LONGLITERAL;
        INTLITERAL = JSymbols.INTLITERAL;
        CHARLITERAL = JSymbols.CHARLITERAL;
        STRINGLITERAL = JSymbols.STRINGLITERAL;
        IDENTIFIER = JSymbols.IDENTIFIER;
        DOT = JSymbols.DOT;
        COMMA = JSymbols.COMMA;
        SEMI = JSymbols.SEMI;
        LPAREN = JSymbols.LPAREN;
        RPAREN = JSymbols.RPAREN;
        LBRACKET = JSymbols.LBRACKET;
        RBRACKET = JSymbols.RBRACKET;
        LBRACE = JSymbols.LBRACE;
        RBRACE = JSymbols.RBRACE;
        SLASHEQ = JSymbols.SLASHEQ;
        SLASH = JSymbols.SLASH;
        USUB = JSymbols.USUB;
        EOF = JSymbols.EOF;
        error = JSymbols.error;
        
        // initialize keywords
        enterKeyword("+", JSymbols.PLUS);
        enterKeyword("-", JSymbols.SUB);
        enterKeyword("!", JSymbols.BANG);
        enterKeyword("%", JSymbols.PERCENT);
        enterKeyword("^", JSymbols.CARET);
        enterKeyword("&", JSymbols.AMP);
        enterKeyword("*", JSymbols.STAR);
        enterKeyword("|", JSymbols.BAR);
        enterKeyword("~", JSymbols.TILDE);
        enterKeyword("/", JSymbols.SLASH);
        enterKeyword(">", JSymbols.GT);
        enterKeyword("<", JSymbols.LT);
        enterKeyword("?", JSymbols.QUES);
        enterKeyword(":", JSymbols.COLON);
        enterKeyword("=", JSymbols.EQ);
        enterKeyword("++", JSymbols.PLUSPLUS);
        enterKeyword("--", JSymbols.SUBSUB);
        enterKeyword("==", JSymbols.EQEQ);
        enterKeyword("<=", JSymbols.LTEQ);
        enterKeyword(">=", JSymbols.GTEQ);
        enterKeyword("!=", JSymbols.BANGEQ);
        enterKeyword("<<", JSymbols.LTLT);
        enterKeyword(">>", JSymbols.GTGT);
        enterKeyword(">>>", JSymbols.GTGTGT);
        enterKeyword("+=", JSymbols.PLUSEQ);
        enterKeyword("-=", JSymbols.SUBEQ);
        enterKeyword("*=", JSymbols.STAREQ);
        enterKeyword("/=", JSymbols.SLASHEQ);
        enterKeyword("&=", JSymbols.AMPEQ);
        enterKeyword("|=", JSymbols.BAREQ);
        enterKeyword("^=", JSymbols.CARETEQ);
        enterKeyword("%=", JSymbols.PERCENTEQ);
        enterKeyword("<<=", JSymbols.LTLTEQ);
        enterKeyword(">>=", JSymbols.GTGTEQ);
        enterKeyword(">>>=", JSymbols.GTGTGTEQ);
        enterKeyword("||", JSymbols.BARBAR);
        enterKeyword("&&", JSymbols.AMPAMP);
        enterKeyword("abstract", JSymbols.ABSTRACT);
        enterKeyword("break", JSymbols.BREAK);
        enterKeyword("case", JSymbols.CASE);
        enterKeyword("catch", JSymbols.CATCH);
        enterKeyword("class", JSymbols.CLASS);
        enterKeyword("const", JSymbols.CONST);
        enterKeyword("continue", JSymbols.CONTINUE);
        enterKeyword("default", JSymbols.DEFAULT);
        enterKeyword("do", JSymbols.DO);
        enterKeyword("else", JSymbols.ELSE);
        enterKeyword("extends", JSymbols.EXTENDS);
        enterKeyword("final", JSymbols.FINAL);
        enterKeyword("finally", JSymbols.FINALLY);
        enterKeyword("for", JSymbols.FOR);
        enterKeyword("goto", JSymbols.GOTO);
        enterKeyword("if", JSymbols.IF);
        enterKeyword("implements", JSymbols.IMPLEMENTS);
        enterKeyword("import", JSymbols.IMPORT);
        enterKeyword("interface", JSymbols.INTERFACE);
        enterKeyword("native", JSymbols.NATIVE);
        enterKeyword("new", JSymbols.NEW);
        enterKeyword("package", JSymbols.PACKAGE);
        enterKeyword("private", JSymbols.PRIVATE);
        enterKeyword("protected", JSymbols.PROTECTED);
        enterKeyword("public", JSymbols.PUBLIC);
        enterKeyword("return", JSymbols.RETURN);
        enterKeyword("static", JSymbols.STATIC);
        enterKeyword("super", JSymbols.SUPER);
        enterKeyword("switch", JSymbols.SWITCH);
        enterKeyword("synchronized", JSymbols.SYNCHRONIZED);
        enterKeyword("this", JSymbols.THIS);
        enterKeyword("volatile", JSymbols.VOLATILE);
        enterKeyword("throw", JSymbols.THROW);
        enterKeyword("throws", JSymbols.THROWS);
        enterKeyword("transient", JSymbols.TRANSIENT);
        enterKeyword("try", JSymbols.TRY);
        enterKeyword("while", JSymbols.WHILE);
        enterKeyword("instanceof", JSymbols.INSTANCEOF);
        enterKeyword("boolean", JSymbols.BOOLEAN);
        enterKeyword("byte", JSymbols.BYTE);
        enterKeyword("char", JSymbols.CHAR);
        enterKeyword("double", JSymbols.DOUBLE);
        enterKeyword("float", JSymbols.FLOAT);
        enterKeyword("int", JSymbols.INT);
        enterKeyword("long", JSymbols.LONG);
        enterKeyword("short", JSymbols.SHORT);
        enterKeyword("void", JSymbols.VOID);
    enterKeyword("signal", JSymbols.SIGNAL);
    }
}

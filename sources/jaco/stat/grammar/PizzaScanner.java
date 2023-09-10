//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.grammar;

import jaco.framework.*;
import jaco.java.grammar.*;
import jaco.java.context.*;


public class PizzaScanner extends Scanner
{
    public PizzaScanner(SyntacticContext context, Sourcefile source)
    {
        super(context, source);
    }
    
    protected void initKeywords()
    {
        // initialize scanner constants
        DOUBLELITERAL = PizzaSymbols.DOUBLELITERAL;
        FLOATLITERAL = PizzaSymbols.FLOATLITERAL;
        LONGLITERAL = PizzaSymbols.LONGLITERAL;
        INTLITERAL = PizzaSymbols.INTLITERAL;
        CHARLITERAL = PizzaSymbols.CHARLITERAL;
        STRINGLITERAL = PizzaSymbols.STRINGLITERAL;
        IDENTIFIER = PizzaSymbols.IDENTIFIER;
        DOT = PizzaSymbols.DOT;
        COMMA = PizzaSymbols.COMMA;
        SEMI = PizzaSymbols.SEMI;
        LPAREN = PizzaSymbols.LPAREN;
        RPAREN = PizzaSymbols.RPAREN;
        LBRACKET = PizzaSymbols.LBRACKET;
        RBRACKET = PizzaSymbols.RBRACKET;
        LBRACE = PizzaSymbols.LBRACE;
        RBRACE = PizzaSymbols.RBRACE;
        SLASHEQ = PizzaSymbols.SLASHEQ;
        SLASH = PizzaSymbols.SLASH;
        USUB = PizzaSymbols.USUB;
        EOF = PizzaSymbols.EOF;
        error = PizzaSymbols.error;
        
        // initialize keywords
        enterKeyword("+", PizzaSymbols.PLUS);
        enterKeyword("-", PizzaSymbols.SUB);
        enterKeyword("!", PizzaSymbols.BANG);
        enterKeyword("%", PizzaSymbols.PERCENT);
        enterKeyword("^", PizzaSymbols.CARET);
        enterKeyword("&", PizzaSymbols.AMP);
        enterKeyword("*", PizzaSymbols.STAR);
        enterKeyword("|", PizzaSymbols.BAR);
        enterKeyword("~", PizzaSymbols.TILDE);
        enterKeyword("/", PizzaSymbols.SLASH);
        enterKeyword(">", PizzaSymbols.GT);
        enterKeyword("<", PizzaSymbols.LT);
        enterKeyword("?", PizzaSymbols.QUES);
        enterKeyword(":", PizzaSymbols.COLON);
        enterKeyword("=", PizzaSymbols.EQ);
        enterKeyword("++", PizzaSymbols.PLUSPLUS);
        enterKeyword("--", PizzaSymbols.SUBSUB);
        enterKeyword("==", PizzaSymbols.EQEQ);
        enterKeyword("<=", PizzaSymbols.LTEQ);
        enterKeyword(">=", PizzaSymbols.GTEQ);
        enterKeyword("!=", PizzaSymbols.BANGEQ);
        enterKeyword("<<", PizzaSymbols.LTLT);
        enterKeyword(">>", PizzaSymbols.GTGT);
        enterKeyword(">>>", PizzaSymbols.GTGTGT);
        enterKeyword("+=", PizzaSymbols.PLUSEQ);
        enterKeyword("-=", PizzaSymbols.SUBEQ);
        enterKeyword("*=", PizzaSymbols.STAREQ);
        enterKeyword("/=", PizzaSymbols.SLASHEQ);
        enterKeyword("&=", PizzaSymbols.AMPEQ);
        enterKeyword("|=", PizzaSymbols.BAREQ);
        enterKeyword("^=", PizzaSymbols.CARETEQ);
        enterKeyword("%=", PizzaSymbols.PERCENTEQ);
        enterKeyword("<<=", PizzaSymbols.LTLTEQ);
        enterKeyword(">>=", PizzaSymbols.GTGTEQ);
        enterKeyword(">>>=", PizzaSymbols.GTGTGTEQ);
        enterKeyword("||", PizzaSymbols.BARBAR);
        enterKeyword("&&", PizzaSymbols.AMPAMP);
        enterKeyword("abstract", PizzaSymbols.ABSTRACT);
        enterKeyword("break", PizzaSymbols.BREAK);
        enterKeyword("case", PizzaSymbols.CASE);
        enterKeyword("catch", PizzaSymbols.CATCH);
        enterKeyword("class", PizzaSymbols.CLASS);
        enterKeyword("const", PizzaSymbols.CONST);
        enterKeyword("continue", PizzaSymbols.CONTINUE);
        enterKeyword("default", PizzaSymbols.DEFAULT);
        enterKeyword("do", PizzaSymbols.DO);
        enterKeyword("else", PizzaSymbols.ELSE);
        enterKeyword("extends", PizzaSymbols.EXTENDS);
        enterKeyword("final", PizzaSymbols.FINAL);
        enterKeyword("finally", PizzaSymbols.FINALLY);
        enterKeyword("for", PizzaSymbols.FOR);
        enterKeyword("goto", PizzaSymbols.GOTO);
        enterKeyword("if", PizzaSymbols.IF);
        enterKeyword("implements", PizzaSymbols.IMPLEMENTS);
        enterKeyword("import", PizzaSymbols.IMPORT);
        enterKeyword("interface", PizzaSymbols.INTERFACE);
        enterKeyword("native", PizzaSymbols.NATIVE);
        enterKeyword("new", PizzaSymbols.NEW);
        enterKeyword("package", PizzaSymbols.PACKAGE);
        enterKeyword("private", PizzaSymbols.PRIVATE);
        enterKeyword("protected", PizzaSymbols.PROTECTED);
        enterKeyword("public", PizzaSymbols.PUBLIC);
        enterKeyword("return", PizzaSymbols.RETURN);
        enterKeyword("static", PizzaSymbols.STATIC);
        enterKeyword("strictfp", PizzaSymbols.STRICTFP);
        enterKeyword("super", PizzaSymbols.SUPER);
        enterKeyword("switch", PizzaSymbols.SWITCH);
        enterKeyword("synchronized", PizzaSymbols.SYNCHRONIZED);
        enterKeyword("this", PizzaSymbols.THIS);
        enterKeyword("volatile", PizzaSymbols.VOLATILE);
        enterKeyword("throw", PizzaSymbols.THROW);
        enterKeyword("throws", PizzaSymbols.THROWS);
        enterKeyword("transient", PizzaSymbols.TRANSIENT);
        enterKeyword("try", PizzaSymbols.TRY);
        enterKeyword("while", PizzaSymbols.WHILE);
        enterKeyword("instanceof", PizzaSymbols.INSTANCEOF);
        enterKeyword("boolean", PizzaSymbols.BOOLEAN);
        enterKeyword("byte", PizzaSymbols.BYTE);
        enterKeyword("char", PizzaSymbols.CHAR);
        enterKeyword("double", PizzaSymbols.DOUBLE);
        enterKeyword("float", PizzaSymbols.FLOAT);
        enterKeyword("int", PizzaSymbols.INT);
        enterKeyword("long", PizzaSymbols.LONG);
        enterKeyword("short", PizzaSymbols.SHORT);
        enterKeyword("void", PizzaSymbols.VOID);
        enterKeyword("_", PizzaSymbols.SUBSCR);
        if (asserts)
            enterKeyword("assert", PizzaSymbols.ASSERT);
    }
}

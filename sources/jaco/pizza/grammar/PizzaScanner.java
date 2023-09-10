//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.grammar;

import jaco.framework.*;
import jaco.java.grammar.*;
import jaco.java.context.*;


public class PizzaScanner extends Scanner
{
    public PizzaScanner(SyntacticContext context, Sourcefile source)
    {
        super(context, source);
    }
    
/**
 * Scans the buffer that is supposed to be a doccomment for
 * a deprecated tag.
 */
    protected boolean scan4deprecated(byte[] buf, int offset, int len)
    {
        int bp = offset+1;
        // the current character
        byte ch = buf[bp];
        boolean linestart = true;
        while (bp < offset + len - 1 && (ch == ' ' || ch == '\t' || ch == '*'))
            ch = buf[++bp];
        while (bp < offset + len - 1)
        {
            switch (ch)
            {
                case CR:
                case LF:
                    ch = buf[++bp];
                    if (ch == LF)
                    {
                        ch = buf[++bp];
                    }
                    // kill heading whitspaces and stars, but keep whitespaces whithout stars
                    while (ch == ' ' || ch == '\t')
                        ch = buf[++bp];
                    if (ch == '*')
                    {
                        while (ch == '*')
                            ch = buf[++bp];
                        while (ch == ' ' || ch == '\t')
                            ch = buf[++bp]; }
                        linestart = true;
                        if (ch != '@')
                            break;
                case '@':
                    // a tag starts in a new line after whitespaces or stars
                    ch = buf[++bp];
                    if (linestart)
                    {
                        linestart = false;
                        if (ch != 'd') {
                            if (ch != 'm') break;
                            ch = buf[++bp];
                            if (ch != 'e') break;
                            ch = buf[++bp];
                            if (ch != 't') break;
                            ch = buf[++bp];
                            if (ch != 'a') break;
                            ch = buf[++bp];
                            if (ch == CR || ch == LF) {
                                if (metaContent == null)
                                    metaContent = "";
                                else
                                    metaContent += "\n";
                            } else if (ch == ' ' || ch == '\t') {
                                ch = buf[++bp];
                                int start = bp;
                                int end = -1;
                                while ((ch != CR) && (ch != LF) && (bp < offset + len - 1))
                                    if (ch == '*') {
                                        ch = buf[++bp];
                                        if (ch == '/') {
                                            end = bp - 1;
                                            break;
                                        }
                                    } else
                                        ch = buf[++bp];
                                if (end < 0)
                                    end = bp;
                                if (metaContent == null)
                                    metaContent = Name.fromSource(buf, start, end - start).toString();
                                else
                                    metaContent += "\n" + Name.fromSource(buf, start, end - start).toString();
                            }
                            break;
                        }
                        ch = buf[++bp];
                        if (ch != 'e') break;
                        ch = buf[++bp];
                        if (ch != 'p') break;
                        ch = buf[++bp];
                        if (ch != 'r') break;
                        ch = buf[++bp];
                        if (ch != 'e') break;
                        ch = buf[++bp];
                        if (ch != 'c') break;
                        ch = buf[++bp];
                        if (ch != 'a') break;
                        ch = buf[++bp];
                        if (ch != 't') break;
                        ch = buf[++bp];
                        if (ch != 'e') break;
                        ch = buf[++bp];
                        if (ch != 'd') break;
                        ch = buf[++bp];
                        if (ch == ' ' || ch == '\t' || ch == CR || ch == LF)
                            return true;
                    }
                    break;
    
                case ' ':
                case '\t':
                    ch = buf[++bp];
                    break;
                
                default:
                    ch = buf[++bp];
                    linestart = false;
            }
        }
        return false;
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

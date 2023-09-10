package jaco.join.grammar;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.join.struct.*;


public class Scanner extends jaco.java.grammar.Scanner
{
    JTree.Factory   jmake = new JTreeFactory();

    
    public Scanner(SyntacticContext context, Sourcefile source)
    {
        super(context, source);
    }
    
    protected void initKeywords()
    {
        DOUBLELITERAL = Symbols.DOUBLELIT;
        FLOATLITERAL = Symbols.FLOATLIT;
        LONGLITERAL = Symbols.LONGLIT;
        INTLITERAL = Symbols.INTLIT;
        CHARLITERAL = Symbols.CHARLIT;
        STRINGLITERAL = Symbols.STRLIT;
        IDENTIFIER = Symbols.IDENTIFIER;
        DOT = Symbols.DOT;
        COMMA = Symbols.COMMA;
        SEMI = Symbols.SEMICOLON;
        LPAREN = Symbols.LPAREN;
        RPAREN = Symbols.RPAREN;
        LBRACKET = Symbols.error;
        RBRACKET = Symbols.error;
        LBRACE = Symbols.LBRACE;
        RBRACE = Symbols.RBRACE;
        SLASH = Symbols.DIV;
        USUB = Symbols.USUB;
        EOF = Symbols.EOF;
        error = Symbols.error;
        
        enterKeyword("+", Symbols.PLUS);
        enterKeyword("-", Symbols.MINUS);
        enterKeyword("!", Symbols.NOT);
        enterKeyword("&", Symbols.AMPERSAND);
        enterKeyword("*", Symbols.TIMES);
        enterKeyword("|", Symbols.BAR);
        enterKeyword("/", Symbols.DIV);
        enterKeyword(">", Symbols.GT);
        enterKeyword("<", Symbols.LT);
        enterKeyword("=", Symbols.EQUALS);
        enterKeyword("==", Symbols.EQ);
        enterKeyword("<=", Symbols.LTEQ);
        enterKeyword(">=", Symbols.GTEQ);
        enterKeyword("!=", Symbols.NOTEQ);
        enterKeyword("||", Symbols.OR);
        enterKeyword("&&", Symbols.AND);
        enterKeyword("_", Symbols.SUB);
        enterKeyword("if", Symbols.IF);
        enterKeyword("else", Symbols.ELSE);
        enterKeyword("true", Symbols.TRUE);
        enterKeyword("false", Symbols.FALSE);
        enterKeyword("error", Symbols.ERROR);
    }
    
    public void nextToken()
    {
        lastpos = Position.encode(cline, ccol);
        while (true)
        {
            boolean escaped = false;
            pos = Position.encode(cline, ccol);
            int index = bp;
            switch (ch)
            {
                case ' ':
                    ch = buf[++bp]; ccol++;
                    break;
                case '\t':
                    ccol = ((ccol - 1) / tabinc * tabinc) + tabinc;
                    ch = buf[++bp]; ccol++;
                    break;
                case CR:
                    cline++;
                    ccol = 0;
                    ch = buf[++bp]; ccol++;
                    if (ch == LF)
                    {
                        ccol = 0;
                        ch = buf[++bp]; ccol++;
                    }
                    break;
                case LF: case FF:
                    cline++;
                    ccol = 0;
                    ch = buf[++bp]; ccol++;
                    break;
                case '\\':
                    if (!isUnicode())
                    {
                        ch = buf[++bp]; ccol++;
                        lexError("illegal character");
                        return;
                    }
                    else
                    {
                        escaped = true;
                        bp += 5;
                        ccol += 5;
                        ch = (byte)'A';
                    }
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                case '$': case '_':
                    while (true)
                    {
                        ch = buf[++bp]; ccol++;
                        switch (ch)
                        {
                            case 'A': case 'B': case 'C': case 'D': case 'E':
                            case 'F': case 'G': case 'H': case 'I': case 'J':
                            case 'K': case 'L': case 'M': case 'N': case 'O':
                            case 'P': case 'Q': case 'R': case 'S': case 'T':
                            case 'U': case 'V': case 'W': case 'X': case 'Y':
                            case 'Z':
                            case 'a': case 'b': case 'c': case 'd': case 'e':
                            case 'f': case 'g': case 'h': case 'i': case 'j':
                            case 'k': case 'l': case 'm': case 'n': case 'o':
                            case 'p': case 'q': case 'r': case 's': case 't':
                            case 'u': case 'v': case 'w': case 'x': case 'y':
                            case 'z':
                            case '$': case '_':
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9':
                                break;
                            
                            case '\\':
                                if (!isUnicode())
                                {
                                    ch = buf[++bp]; ccol++;
                                    lexError("illegal character");
                                    return;
                                }
                                else
                                {
                                    escaped = true;
                                    bp += 5;
                                    ccol += 5;
                                    ch = buf[bp];
                                    break;
                                }
                                
                            default:
                                if (ch < 0)
                                {
                                    escaped = true;
                                    break;
                                }
                                if (escaped)
                                    name = Name.fromSource(buf, index, bp - index);
                                else
                                    name = Name.fromAscii(buf, index, bp - index);
                                if (name.index <= maxKey)
                                    token = key[name.index];
                                else
                                    token = IDENTIFIER;
                                return;
                        }
                    }
                case '0':
                    ch = buf[++bp]; ccol++;
                    if (ch == 'x' || ch == 'X')
                    {
                        ch = buf[++bp]; ccol++;
                        getnumber(index + 2, 16);
                    }
                    else
                        getnumber(index, 8);
                    return;
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    getnumber(index, 10);
                    return;
                case '.':
                    ch = buf[++bp]; ccol++;
                    if ('0' <= ch && ch <= '9')
                        getfraction(index);
                    else
                        token = DOT;
                    return;
                case ',':
                    ch = buf[++bp]; ccol++; token = COMMA; return;
                case ';':
                    ch = buf[++bp]; ccol++; token = SEMI; return;
                case '(':
                    ch = buf[++bp]; ccol++; token = LPAREN; return;
                case ')':
                    ch = buf[++bp]; ccol++; token = RPAREN; return;
                case '{':
                    ch = buf[++bp]; ccol++; token = LBRACE; return;
                case '}':
                    ch = buf[++bp]; ccol++; token = RBRACE; return;
                case '/':
                    ch = buf[++bp]; ccol++;
                    if (ch == '/')
                    {
                        do
                        {
                            ch = buf[++bp]; ccol++;
                        }
                        while (ch != CR && ch != LF && ch != SU);
                        break;
                    }
                    else
                    if (ch == '*')
                    {
                        ch = buf[++bp]; ccol++;
                        if (ch == '*')
                            docComment = true;
                        do
                        {
                            do
                            {
                                if (ch == CR)
                                {
                                    cline++;
                                    ccol = 0;
                                    ch = buf[++bp]; ccol++;
                                    if (ch == LF)
                                    {
                                        ccol = 0;
                                        ch = buf[++bp]; ccol++;
                                    }
                                }
                                else
                                if (ch == LF)
                                {
                                    cline++;
                                    ccol = 0;
                                    ch = buf[++bp]; ccol++;
                                }
                                else
                                if (ch == '\t')
                                {
                                    ccol = ((ccol - 1) / tabinc * tabinc) + tabinc;
                                    ch = buf[++bp]; ccol++;
                                }
                                else
                                {
                                    ch = buf[++bp]; ccol++;
                                }
                            }
                            while (ch != '*' && ch != SU);
                            while (ch == '*')
                            {
                                ch = buf[++bp]; ccol++;
                            }
                        }
                        while (ch != '/' && ch != SU);
                        if (ch == '/')
                        {
                            if (docComment)
                            {
                                deprecatedFlag =
                                    scan4deprecated(buf, index, bp - index);
                                    docComment = false;
                            }
                            ch = buf[++bp]; ccol++;
                            break;
                        }
                        else
                        {
                            lexError("unclosed comment");
                            return;
                        }
                    }
                    else
                    {
                        name = slashS;
                        token = SLASH;
                        return;
                    }
                    
                case '\'':
                    ch = buf[++bp]; ccol++;
                    litlen = 0;
                    getlitch();
                    if (ch == '\'')
                    {
                        ch = buf[++bp]; ccol++;
                        token = CHARLITERAL;
                        byte[] ascii = new byte[litlen * 2];
                        int alen = SourceRepr.source2ascii(lit, 0, litlen, ascii);
                        if (alen > 0)
                            intVal = SourceRepr.ascii2string(ascii, 0, alen).charAt(0);
                        else
                            intVal = 0;
                    }
                    else
                        lexError("unclosed character literal");
                    return;
                    
                case '\"':
                    ch = buf[++bp]; ccol++;
                    litlen = 0;
                    while (ch != '\"' && ch != CR && ch != LF && ch != SU)
                        getlitch();
                    if (ch == '\"')
                    {
                        token = STRINGLITERAL;
                        name = Name.fromSource(lit, 0, litlen);
                        ch = buf[++bp]; ccol++;
                    }
                    else
                        lexError("unclosed character literal", Position.encode(cline, ccol));
                    return;
                    
                case SU:
                    token = EOF;
                    currentSource.lines = cline;
                    return;
                    
                default:
                    if (isspecial(ch))
                        getspecials();
                    else
                    {
                        ch = buf[++bp]; ccol++;
                        lexError("illegal character");
                    }
                    return;
            }
        }
    }
}

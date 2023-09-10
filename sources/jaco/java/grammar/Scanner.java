//      /   _ _      JaCo
//  \  //\ / / \     - lexical analyzer
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.grammar;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import java.io.*;


public class Scanner
{
/** components used by the scanner and parser
 */
    public ErrorHandler     report;
    public Trees            trees;
    public Constants        constants;
    
/** layout & character constants
 */
    public int  tabinc = 8;
    protected final static byte LF = 0xA;
    protected final static byte FF = 0xC;
    protected final static byte CR = 0xD;
    protected final static byte SU = 0x1A;
    
/** scanner constants
 */
    public byte DOUBLELITERAL = Symbols.DOUBLELITERAL;
    public byte FLOATLITERAL = Symbols.FLOATLITERAL;
    public byte LONGLITERAL = Symbols.LONGLITERAL;
    public byte INTLITERAL = Symbols.INTLITERAL;
    public byte CHARLITERAL = Symbols.CHARLITERAL;
    public byte STRINGLITERAL = Symbols.STRINGLITERAL;
    public byte IDENTIFIER = Symbols.IDENTIFIER;
    public byte DOT = Symbols.DOT;
    public byte COMMA = Symbols.COMMA;
    public byte SEMI = Symbols.SEMI;
    public byte LPAREN = Symbols.LPAREN;
    public byte RPAREN = Symbols.RPAREN;
    public byte LBRACKET = Symbols.LBRACKET;
    public byte RBRACKET = Symbols.RBRACKET;
    public byte LBRACE = Symbols.LBRACE;
    public byte RBRACE = Symbols.RBRACE;
    public byte SLASHEQ = Symbols.SLASHEQ;
    public byte SLASH = Symbols.SLASH;
    public byte USUB = Symbols.USUB;
    public byte EOF = Symbols.EOF;
    public byte error = Symbols.error;
    

/** the names of all tokens
 */
    public Name[]       tokenName = new Name[128];
    public int          numToken = 0;
    
/** keyword array; maps from name indices to tokens
 */
    protected byte[]    key;
    protected int       maxKey = 0;

/** the next token
 */
    public int          token;

/** the token's position. pos = line << Position.LINESHIFT + col
 */
    public int          pos = 0;

/** the first character position after the previous token
 */
    public int          lastpos = 0;

/** the name of an identifier or token
 */
    public Name         name;

/** the value of a number
 */
    public long         intVal;
    public double       floatVal;

/** has a deprecated been encountered in last doc comment?
 */
    public boolean deprecatedFlag = false;

/** the meta content
 */
    public String metaContent;

    public int          errPos = -1;

/** the input buffer:
 */
    protected byte[]    buf;
    protected int       bp;

/** the current character
 */
    protected byte      ch;

/** the line and column position of the current character
 */
    public int          cline;
    public int          ccol;
    
/** the current sourcefile
 */
    public Sourcefile   currentSource;
    
/** a buffer for character and string literals
 */
    protected byte[]    lit = new byte[64];
    protected int       litlen;
    
/** two name constants
 */
    protected static final Name slashS = Name.fromString("/");
    protected static final Name slashEqualsS = Name.fromString("/=");
    
/** support asserts?
 */
    protected boolean asserts = false;
    
/** Construct a scanner from a file input stream.
 */
    public Scanner(SyntacticContext context, Sourcefile source)
    {
        report = context.compilerContext.mainContext.ErrorHandler();
        trees = context.compilerContext.mainContext.Trees();
        constants = context.compilerContext.mainContext.Constants();
        buf = (currentSource = source).getBuffer();
        bp = 0;
        cline = 1;
        ccol = 1;
        ch = buf[0];
        asserts = ((JavaSettings)context.settings).sourceversion.equals("1.4");
    	++report.files;
        init();
        nextToken();
    }
    
/** for debugging purposes
 */
    protected void dch()
    {
        System.out.print((char)ch); System.out.flush();
    }

/** generate an error at the given position
 */
    protected void lexError(String msg, int pos)
    {
        report.error(pos, msg);
        token = error;
        errPos = pos;
    }

/** generate an error at the current token position
 */
    protected void lexError(String msg)
    {
        lexError(msg, pos);
    }

/** append characteter to "lit" buffer
 */
    protected void putch(byte c)
    {
        if (litlen == lit.length)
        {
            byte[] newlit = new byte[lit.length * 2];
            System.arraycopy(lit, 0, newlit, 0, lit.length);
            lit = newlit;
        }
        lit[litlen++] = c;
    }

/** return true iff next 6 characters are a valid unicode sequence:
 */
    protected boolean isUnicode()
    {
        return
            (bp + 6) < buf.length &&
            (buf[bp] == '\\') &&
            (buf[bp+1] == 'u') &&
            (SourceRepr.digit2int(buf[bp+2], 16) >= 0) &&
            (SourceRepr.digit2int(buf[bp+3], 16) >= 0) &&
            (SourceRepr.digit2int(buf[bp+4], 16) >= 0) &&
            (SourceRepr.digit2int(buf[bp+5], 16) >= 0);
    }

/** read next character in character or string literal:
 */
    protected void getlitch()
    {
        if (ch == '\\')
        {
            if (isUnicode())
            {
                putch(ch); ch = buf[++bp]; ccol++;
                putch(ch); ch = buf[++bp]; ccol++;
                putch(ch); ch = buf[++bp]; ccol++;
                putch(ch); ch = buf[++bp]; ccol++;
                putch(ch); ch = buf[++bp]; ccol++;
                putch(ch); ch = buf[++bp]; ccol++;
            }
            else
            {
                ch = buf[++bp]; ccol++;
                if ('0' <= ch && ch <= '7')
                {
                    byte leadch = ch;
                    int oct = SourceRepr.digit2int(ch, 8);
                    ch = buf[++bp]; ccol++;
                    if ('0' <= ch && ch <= '7')
                    {
                        oct = oct * 8 + SourceRepr.digit2int(ch, 8);
                        ch = buf[++bp]; ccol++;
                        if (leadch <= '3' && '0' <= ch && ch <= '7')
                        {
                            oct = oct * 8 + SourceRepr.digit2int(ch, 8);
                            ch = buf[++bp]; ccol++;
                        }
                    }
                    // treat '\' different
                    if (oct == 92)
                    	putch((byte)oct);
                    putch((byte)oct);
                }
                else
                if (ch != SU)
                {
                    switch (ch)
                    {
                        case 'b': case 't': case 'n':
                        case 'f': case 'r': case '\"':
                        case '\'': case '\\':
                            putch((byte)'\\');
                            putch(ch);
                            break;
                        
                        default:
                            lexError("invalid escape character", Position.encode(cline, ccol) - 1);
                            putch(ch);
                    }
                    ch = buf[++bp]; ccol++;
                }
            }
        }
        else
        if (ch != SU)
        {
            putch(ch);
            ch = buf[++bp]; ccol++;
        }
    }

/** read fractional part of floating point number;
 *  Then floatVal := buf[index..], converted to a floating point number.
 */
    protected void getfraction(int index)
    {
        while (SourceRepr.digit2int(ch, 10) >= 0)
        {
            ch = buf[++bp];
            ccol++;
        }
        token = DOUBLELITERAL;
        if ((ch == 'e') || (ch == 'E'))
        {
            ch = buf[++bp]; ccol++;
            if ((ch == '+') || (ch == '-'))
            {
                byte sign = ch;
                ch = buf[++bp]; ccol++;
                if (('0' > ch) || (ch > '9'))
                {
                    ch = sign;
                    bp--;
                    ccol--;
                }
            }
            while (SourceRepr.digit2int(ch, 10) >= 0)
            {
                ch = buf[++bp];
                ccol++;
            }
        }
        double limit = Double.MAX_VALUE;
        if ((ch == 'd') || (ch == 'D'))
        {
            ch = buf[++bp];
            ccol++;
        }
        else
        if ((ch == 'f') || (ch == 'F'))
        {
            token = FLOATLITERAL;
            limit = Float.MAX_VALUE;
            ch = buf[++bp];
            ccol++;
        }
        try
        {
            floatVal = Double.valueOf(new String(buf, index, bp - index)).doubleValue();
            if (floatVal > limit)
                lexError("floating point number too large");
        }
        catch (NumberFormatException e)
        {
            lexError("malformed floating point number");
        }
    }

/** intVal := buf[index..index+len-1], converted to an integer number.
 *  base = the base of the number; one of 8, 10, 16.
 *  max  = the maximal number before an overflow.
 */
    protected void makeint (int index, int len, int base, long max)
    {
        intVal = 0;
        int divider = (base == 10 ? 1 : 2);
        for (int i = 0; i < len; i++)
        {
            int d = SourceRepr.digit2int(buf[index + i], base);
            if (d < 0)
            {
                lexError("malformed integer number");
                return;
            }
            if (intVal < 0 ||
                max / (base / divider) < intVal ||
                max - (d / divider) < (intVal * (base / divider) - (token == USUB ? 1 : 0)))
            {
                lexError("integer number too large");
                return;
            }
            intVal = intVal * base + d;
        }
    }

/** read a number,
 *  and convert buf[index..], setting either intVal or floatVal.
 *  base = the base of the number; one of 8, 10, 16.
 */
    protected void getnumber(int index, int base)
    {
        while (SourceRepr.digit2int(ch, base == 8 ? 10 : base) >= 0)
        {
            ch = buf[++bp];
            ccol++;
        }
        if (base <= 10 && ch == '.')
        {
            ch = buf[++bp]; ccol++;
            getfraction(index);
        }
        else
        if (base <= 10 &&
           (ch == 'e' || ch == 'E' ||
            ch == 'f' || ch == 'F' ||
            ch == 'd' || ch == 'D'))
            getfraction(index);
        else
        {
            if (ch == 'l' || ch == 'L')
            {
                makeint(index, bp - index, base, Long.MAX_VALUE);
                ch = buf[++bp]; ccol++;
                token = LONGLITERAL;
            }
            else
            {
                makeint(index, bp - index, base, Integer.MAX_VALUE);
                intVal = (int)intVal;
                token = INTLITERAL;
            }
        }
    }

/** return true if ch can be part of an operator
 */
    protected boolean isspecial(byte ch)
    {
        switch(ch)
        {
            case '!': case '%': case '&': case '*': case '?':
            case '+': case '-': case ':': case '<': case '=':
            case '>': case '^': case '|': case '~':
                return true;
            default:
                return false;
        }
    }

/** read longest possible sequence of special characters and convert
 *  to token:
 */
    protected void getspecials()
    {
        int index = bp;
        name = Name.fromAscii(buf, index, 1);
        while (true)
        {
            token = key[name.index];
            ch = buf[++bp]; ccol++;
            if (!isspecial(ch))
                break;
            Name newname = Name.fromAscii(buf, index, bp + 1 - index);
            if (newname.index > maxKey || key[newname.index] == IDENTIFIER)
                break;
            name = newname;
        }
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
                        if (ch != 'd') break;
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

    protected boolean docComment = false;

/** read next token
 */
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
                        // fall-through intentional
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
                              	++report.tokens;
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
                    ++report.tokens;
                    return;
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    getnumber(index, 10);
                    ++report.tokens;
                    return;
                case '.':
                    ch = buf[++bp]; ccol++;
                    if ('0' <= ch && ch <= '9')
                        getfraction(index);
                    else
                        token = DOT;
                   	++report.tokens;
                    return;
                case ',':
                    ch = buf[++bp]; ccol++; token = COMMA; ++report.tokens; return;
                case ';':
                    ch = buf[++bp]; ccol++; token = SEMI; ++report.tokens; return;
                case '(':
                    ch = buf[++bp]; ccol++; token = LPAREN; ++report.tokens; return;
                case ')':
                    ch = buf[++bp]; ccol++; token = RPAREN; ++report.tokens; return;
                case '[':
                    ch = buf[++bp]; ccol++; token = LBRACKET; ++report.tokens; return;
                case ']':
                    ch = buf[++bp]; ccol++; token = RBRACKET; ++report.tokens; return;
                case '{':
                    ch = buf[++bp]; ccol++; token = LBRACE; ++report.tokens; return;
                case '}':
                    ch = buf[++bp]; ccol++; token = RBRACE; ++report.tokens; return;
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
                        if (ch == '=')
                        {
                            name = slashEqualsS;
                            token = SLASHEQ;
                            ch = buf[++bp]; ccol++;
                        }
                        else
                        {
                            name = slashS;
                            token = SLASH;
                        }
                        ++report.tokens;
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
                    ++report.tokens;
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
                        //System.out.println("len = " + litlen + ", " + name.toString().length());
                    }
                    else
                        lexError("unclosed character literal", Position.encode(cline, ccol));
                    ++report.tokens;
                    return;
                    
                case SU:
                    token = EOF;
                    if (currentSource.lines == 0) {
						currentSource.lines = cline;
						report.lines += cline;
					}
                    return;
                    
                default:
                    if (isspecial(ch))
                        getspecials();
                    else
                    {
                        ch = buf[++bp]; ccol++;
                        lexError("illegal character");
                    }
                    ++report.tokens;
                    return;
            }
        }
    }

    public int name2token(Name name)
    {
        if (name.index <= maxKey)
            return key[name.index];
        else
            return IDENTIFIER;
    }

    public String token2string(int token)
    {
        if (token == IDENTIFIER)
            return "<identifier>";
        if (token == CHARLITERAL)
            return "<character>";
        if (token == STRINGLITERAL)
            return "<string>";
        if (token == INTLITERAL)
            return "<integer>";
        if (token == LONGLITERAL)
            return "<long integer>";
        if (token == FLOATLITERAL)
            return "<float>";
        if (token == DOUBLELITERAL)
            return "<double>";
        if (token == DOT)
            return "'.'";
        if (token == COMMA)
            return "','";
        if (token == SEMI)
            return "';'";
        if (token == LPAREN)
            return "'('";
        if (token == RPAREN)
            return "')'";
        if (token == LBRACKET)
            return "'['";
        if (token == RBRACKET)
            return "']'";
        if (token == LBRACE)
            return "'{'";
        if (token == RBRACE)
            return "'}'";
        if (token == error)
            return "<bad symbol>";
        if (token == EOF)
            return "<end of input>";
        return tokenName[token].toString();
    }
    
    protected void enterKeyword(String s, int tokenId)
    {
        while (tokenId > tokenName.length)
        {
            Name[]  newTokName = new Name[tokenName.length * 2];
            System.arraycopy(tokenName, 0, newTokName, 0, newTokName.length);
            tokenName = newTokName;
        }
        Name n = Name.fromString(s);
        tokenName[tokenId] = n;
        if (n.index > maxKey)
            maxKey = n.index;
        if (tokenId >= numToken)
            numToken = tokenId + 1;
    }
    
    protected void init()
    {
        initKeywords();
        key = new byte[maxKey+1];
        for (int i = 0; i <= maxKey; i++)
            key[i] = IDENTIFIER;
        for (byte j = 0; j < numToken; j++)
            if (tokenName[j] != null)
                key[tokenName[j].index] = j;
    }
    
    protected void initKeywords()
    {
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

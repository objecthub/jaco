//      /   _ _      JaCo
//  \  //\ / / \     - representation of a single source file
//   \//  \\_\_/     
//         \         Matthias Zenger, 16/02/98

package jaco.framework;

import java.io.*;
import java.util.Hashtable;


public class Sourcefile
{
/** the filename
 */
    protected String        filename;
    public String           shortname;
    
/** the encoding of the file
 */
    protected String        encoding;
    
/** a log of all errors generated so far; used to avoid printing an
 *  error message more than once
 */
    protected Hashtable     recorded = new Hashtable();
    
/** the buffer containing the file that is currently translated
 */
    protected byte[]        buf = null;
    
/** the last error position
 */
    protected int           lastLine = 0;
    protected int           lastPos = 0;
    protected int           lineEnd = 0;
    protected int           newPos = 0;
    
/** constants used for source parsing
 */
    final static byte       LF = 0xA;
    final static byte       FF = 0xC;
    final static byte       CR = 0xD;
    final static byte       SU = 0x1A;

/** set col to NO_COLUMN, if the printLine method should not mark
 *  the column
 */
    final static int        NO_COLUMN = -1;
    
/** number of lines and bytes (not used internally)
 */
    public int              lines;  // set externally
    public int              bytes;
    
    
/** constructors
 */
    public Sourcefile(String filename) throws IOException
    {
        if (filename == null)
        {
            this.filename = "(sourcefile not available)";
            this.shortname = "?";
            buf = new byte[]{SU};
        }
        else
        {
            File f = new File(filename);
            this.filename = filename;
            this.shortname = f.getName();
            fillBuffer(new FileInputStream(f));
        }
    }
    
    public Sourcefile(VirtualFile file) throws IOException
    {
        if (file == null)
        {
            this.filename = "(sourcefile not available)";
            this.shortname = "?";
            buf = new byte[]{SU};
        }
        else
        {
            this.filename = file.getPath();
            this.shortname = file.getName();
            fillBuffer(file.getInputStream());
        }
    }
    
/** fill the buffer using the InputStream
 */
    private void fillBuffer(InputStream in) throws IOException
    {
        try
        {
            buf = new byte[(bytes = in.available()) + 1];
            if (in.read(buf) != (buf.length - 1))
                throw new IOException();
            in.close();
            buf[buf.length - 1] = SU;
        }
        catch (IOException e)
        {
            throw new IOException("cannot read '" + filename + "'");
        }
    }
    
/** return filename as a string
 */
    public String toString()
    {
        return filename;
    }
    
/** return filename as a name
 */
    public Name getName()
    {
        return Name.fromString(filename);
    }
    
/** return the source buffer of this file
 */
    public byte[] getBuffer()
    {
        return buf;
    }
    
/** number of logged entries
 */
    public int logged()
    {
        return recorded.size();
    }
    
/** is there already an entry at position 'pos'
 */
    public boolean isLogged(int pos)
    {
        return (recorded.get(new Integer(pos)) != null);
    }

/** enter entry into log table
 */
    public void log(int pos, String message)
    {
        recorded.put(new Integer(pos), message);
    }

/** set encoding of the file
 */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    
/** print message, if there is no entry for this position
 *  and enter message into log
 */
    public boolean printMessageIfNew(int pos, String message)
    {
        if (!isLogged(pos))
        {
            log(pos, message);
            printMessage(pos, message);
            return true;
        }
        return false;
    }

/** print message and line in sourcefile
 */
    public void printMessage(int pos, String message)
    {
        if (pos == Position.NOPOS)
            System.out.println(filename + ": " + message);
        else
        {
            int line = Position.line(pos);
            int col = Position.column(pos);
            System.out.print(filename + ":" + line + ": ");
            System.out.println(message);
            printLine(line, col);
        }
    }
    
/** print source line
 */
    public void printLine(int line, int col)
    {
        int pos = 0;
        if (lastLine > line)
            lastLine = 0;
        else
            pos = newPos;
        while ((pos < buf.length) && (lastLine < line))
        {
            lastPos = pos;
            while ((pos < buf.length) && (buf[pos] != CR) &&
                    (buf[pos] != LF) && (buf[pos] != FF))
                pos++;
            lineEnd = pos;
            if (pos < buf.length)
                pos++;
            if ((pos < buf.length) && (buf[pos-1] == CR) && (buf[pos] == LF))
                pos++;
            lastLine++;
        }
        newPos = pos;
        try
        {
            String  errline = (encoding != null) ?
                                new String(buf, lastPos, lineEnd - lastPos, encoding) :
                                new String(buf, lastPos, lineEnd - lastPos);
            System.out.println(errline);
            if (col != NO_COLUMN)
            {
                byte[]  ptr = new byte[col];
                for (int i = col - 2; i >= 0; i--)
                    ptr[i] = ' ';
                ptr[col - 1] = '^';
                System.out.println(new String(ptr));
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new InternalError(e.getMessage());
        }
    }
}

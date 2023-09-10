//      /   _ _      JaCo
//  \  //\ / / \     - handle classfile names
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import java.io.File;


public interface ClassfileConst
{
    int JAVA_MAGIC = 0xCAFEBABE;
    int JAVA_MAJOR_VERSION = 45;
    int JAVA_MINOR_VERSION = 3;

    int CONSTANT_UTF8 = 1;
    int CONSTANT_UNICODE = 2;
    int CONSTANT_INTEGER = 3;
    int CONSTANT_FLOAT = 4;
    int CONSTANT_LONG = 5;
    int CONSTANT_DOUBLE = 6;
    int CONSTANT_CLASS = 7;
    int CONSTANT_STRING = 8;
    int CONSTANT_FIELDREF = 9;
    int CONSTANT_METHODREF = 10;
    int CONSTANT_INTFMETHODREF = 11;
    int CONSTANT_NAMEANDTYPE = 12;
}

public class Classfiles extends Component
{
/** component name
 */
    public String getName()
    {
        return "JavaClassfiles";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
    }

    
//////////// String Translation Routines

/** return internal representation of buf[offset..offset+len-1],
 *  converting '/' to '.'
 */
    public byte[] internalize(byte[] buf, int offset, int len)
    {
        byte[] translated = new byte[len];
        for (int j = 0; j < len; j++)
        {
            byte b = buf[offset + j];
            if (b == '/')
                translated[j] = '.';
            else
                translated[j] = b;
        }
        return translated;
    }

/** return external representation of buf[offset..offset+len-1],
 *  converting '.' to '/'
 */
    public byte[] externalize(byte[] buf, int offset, int len)
    {
        byte[] translated = new byte[len];
        for (int j = 0; j < len; j++)
        {
            byte b = buf[offset + j];
            if (b == '.')
                translated[j] = '/';
            else
                translated[j] = b;
        }
        return translated;
    }

/** return internal representation of file name s,
 *  converting File.separatorChar to '.'
 */
    public Name internalizeFileName(String s)
    {
        byte[] ascii = SourceRepr.string2ascii(s.replace(File.separatorChar, '.'));
        return Name.fromAscii(ascii, 0, ascii.length);
    }

/** return external representation of file name s,
 *  converting '.' to File.separatorChar
 */
    public String externalizeFileName(Name n)
    {
        if ((n == null) || (n.length() == 0))
            return ".";
        byte[] ascii = n.toAscii();
        String s = SourceRepr.ascii2string(ascii, 0, ascii.length);
        return s.replace('.', File.separatorChar);
    }
}


/** the name and type signature of a method or field
 */
public final class NameAndType
{
    public Name name;
    public Name sig;

    public NameAndType(Name name, Name sig)
    {
        this.name = name;
        this.sig = sig;
    }
}

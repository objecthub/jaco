//      /   _ _      JaCo
//  \  //\ / / \     - classfile input buffer
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import java.io.*;


public class ClassfileInput
{
/** the buffer containing the class file
 */
    public byte[]   buf;

/** the current input pointer
 */
    public int      bp;
    
    
/** constructor
 */
    public ClassfileInput(VirtualFile f) throws IOException
    {
        buf = f.read();
        bp = 0;
    }
    

/** return byte at offset 'pos'
 */
    public byte byteAt(int pos)
    {
        return buf[pos];
    }

/** read a byte
 */
    public byte nextByte()
    {
        return buf[bp++];
    }

/** read a character
 */
    public char nextChar()
    {
        return
            (char)(((buf[bp++] & 0xff) << 8) + (buf[bp++] & 0xff));
    }

/** read an integer
 */
    public int nextInt()
    {
        return  ((buf[bp++] & 0xff) << 24) +
                ((buf[bp++] & 0xff) << 16) +
                ((buf[bp++] & 0xff) <<  8) +
                (buf[bp++] & 0xff);
    }

/** extract a character at position bp from buf
 */
    public char getChar(int bp)
    {
        return (char)(((buf[bp] & 0xff) << 8) + (buf[bp+1] & 0xff));
    }

/** extract an integer at position bp from buf
 */
    public int getInt(int bp)
    {
        return  ((buf[bp  ] & 0xff) << 24) +
                ((buf[bp+1] & 0xff) << 16) +
                ((buf[bp+2] & 0xff) << 8) +
                 (buf[bp+3] & 0xff);
    }

/** extract a long integer at position bp from buf
 */
    public long getLong(int bp)
    {
        DataInputStream bufin = new DataInputStream(
                                    new ByteArrayInputStream(buf, bp, 8));
        try
        {
            return bufin.readLong();
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

/** extract a float at position bp from buf
 */
    public strictfp float getFloat(int bp)
    {
        DataInputStream bufin = new DataInputStream(
                                    new ByteArrayInputStream(buf, bp, 4));
        try
        {
            return bufin.readFloat();
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

/** extract a double at position bp from buf
 */
    public strictfp double getDouble(int bp)
    {
        DataInputStream bufin = new DataInputStream(
                                        new ByteArrayInputStream(buf, bp, 8));
        try
        {
            return bufin.readDouble();
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

/** skip next 'n' bytes
 */
    public void skip(int n)
    {
        bp += n;
    }
}

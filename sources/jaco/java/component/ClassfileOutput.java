//      /   _ _      JaCo
//  \  //\ / / \     - classfile output buffer
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.java.struct.*;
import java.io.*;


public class ClassfileOutput
{
/** the initial sizes of the object and constant pool output buffers
 *  sizes are increased when buffers get full
 */
    protected static final int  DATA_BUF_SIZE = 0x10000;
    protected static final int  POOL_BUF_SIZE = 0x10000;

/** the object and constant pool output buffers
 */
    protected byte[]            databuf = new byte[DATA_BUF_SIZE];
    protected int               datasize = 0;
    protected byte[]            poolbuf = new byte[POOL_BUF_SIZE];
    protected int               poolsize = 0;

/** the currently active output buffer, and the number of bytes written to it
 */
    protected boolean           useDataBuf;
    protected byte[]            out;
    protected int               op;


/** constructor
 */
    public ClassfileOutput()
    {
        useDataBuf = true;
        out = databuf;
        op = 0;
    }

/** recycle output object
 */
    public void recycle()
    {
        datasize = 0;
        poolsize = 0;
        useDataBuf = true;
        out = databuf;
        op = 0;
    }
    
/** switch between buffers
 */
    public void useDataBuffer()
    {
        if (!useDataBuf)
        {
            poolsize = op;
            poolbuf = out;
            out = databuf;
            op = datasize;
            useDataBuf = true;
        }
    }

    public void usePoolBuffer()
    {
        if (useDataBuf)
        {
            datasize = op;
            databuf = out;
            out = poolbuf;
            op = poolsize;
            useDataBuf = false;
        }
    }

/** append other buffer to current buffer
 */
    public void appendOtherBuffer()
    {
        if (useDataBuf)
            writeBytes(poolbuf, 0, poolsize);
        else
            writeBytes(databuf, 0, datasize);
    }

/** get current position in output buffer
 */
    public int getPos()
    {
        return op;
    }

/** double the size of the active output buffer
 */
    protected void extend()
    {
        byte[] newout = new byte[out.length * 2];
        System.arraycopy(out, 0, newout, 0, out.length);
        out = newout;
    }

/** append a byte to the active output buffer
 */
    public void writeByte(int b)
    {
        if (op == out.length)
            extend();
        out[op++] = (byte)b;
    }
    
/** append xs[offset..offset+len-1] to the active output buffer
 */
    public void writeBytes(byte[] xs, int offset, int len)
    {
        while ((op + len) > out.length)
            extend();
        System.arraycopy(xs, offset, out, op, len);
        op += len;
    }

/** append a character to the active output buffer
 */
    public void writeChar(int x)
    {
        if (op + 2 > out.length)
            extend();
        out[op++] = (byte)((x >> 8) & 0xff);
        out[op++] = (byte)(x & 0xff);
    }
    
/** write a character at the given position
 */
    public void putChar(int op, int x)
    {
        out[op    ] = (byte)((x >> 8) & 0xff);
        out[op + 1] = (byte)(x & 0xff);
    }
    
/** append an integer to the active output buffer.
 */
    public void writeInt(int x)
    {
        if ((op + 4) > out.length)
            extend();
        putInt(op, x);
        op += 4;
    }

/** write an integer at the given position
 */
    public void putInt(int adr, int x)
    {
        out[adr    ] = (byte)((x >> 24) & 0xff);
        out[adr + 1] = (byte)((x >> 16) & 0xff);
        out[adr + 2] = (byte)((x >> 8) & 0xff);
        out[adr + 3] = (byte)(x & 0xff);
    }

/** extract a character at position bp from buf
 */
    public static char getChar(byte[] buf, int bp)
    {
        return (char)(((buf[bp] & 0xff) << 8) + (buf[bp+1] & 0xff));
    }

/** append a long to the active output buffer
 */
    public void writeLong(long x)
    {
        ByteArrayOutputStream   buffer = new ByteArrayOutputStream(8);
        DataOutputStream        bufout = new DataOutputStream(buffer);
        try
        {
            bufout.writeLong(x);
            writeBytes(buffer.toByteArray(), 0, 8);
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

/** append a float to the active output buffer
 */
    public strictfp void writeFloat(float x)
    {
        ByteArrayOutputStream   buffer = new ByteArrayOutputStream(4);
        DataOutputStream        bufout = new DataOutputStream(buffer);
        try
        {
            bufout.writeFloat(x);
            writeBytes(buffer.toByteArray(), 0, 4);
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

/** append a double to the active output buffer.
 */
    public strictfp void writeDouble(double x)
    {
        ByteArrayOutputStream   buffer = new ByteArrayOutputStream(8);
        DataOutputStream        bufout = new DataOutputStream(buffer);
        try
        {
            bufout.writeDouble(x);
            writeBytes(buffer.toByteArray(), 0, 8);
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }
    
    public static void putChar(byte[] buf, int op, int x)
    {
        buf[op    ] = (byte)((x >> 8) & 0xff);
        buf[op + 1] = (byte)(x & 0xff);
    }
    
/** write buffer to stream
 */
    public OutputStream writeBuffer(OutputStream dataOut) throws IOException
    {
        dataOut.write(out, 0, op);
        return dataOut;
    }
}

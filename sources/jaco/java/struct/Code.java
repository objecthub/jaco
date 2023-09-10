//      /   _ _      JaCo
//  \  //\ / / \     - data structure that represents the bytecode for a method
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.java.component.*;
import Definition.*;


public class Code implements ConstantConst
{
/** internal fields
 */
    public boolean          fatcode;
    public boolean          debuginfo;
    
/** classfile fields
 */
    public int              max_stack = 0;
    public int              max_locals = 0;
    public byte[]           code = new byte[64];
    public int              cp = 0;
    
    public char[]           exc_start_pc = new char[4];
    public char[]           exc_end_pc = new char[4];
    public char[]           exc_handler_pc = new char[4];
    public char[]           exc_catch_type = new char[4];
    public int              ncatches = 0;
    
    public char[]           line_start_pc = new char[32];
    public char[]           line_number = new char[32];
    public int              nlines = 0;
    
    public char[]           lvar_start_pc = new char[4];
    public char[]           lvar_length = new char[4];
    public char[]           lvar_reg = new char[4];
    public Definition[]     lvar = new Definition[4];
    public int              nvars = 0;
    
/** code pointer used by the decoder
 */
    public int              pc = 0;
    
    
/** constructor
 */
    public Code(boolean fatcode, boolean debuginfo)
    {
        this.fatcode = fatcode;
        this.debuginfo = debuginfo;
    }
    
/** add a byte to code.
 */
    public void addCode(int od)
    {
        if (cp == code.length)
        {
            byte[] newcode = new byte[cp * 2];
            System.arraycopy(code, 0, newcode, 0, cp);
            code = newcode;
        }
        code[cp++] = (byte)od;
    }
    
/** add a catch clause
 */
    public void addCatch(int start_pc, int end_pc, int handler_pc, int catch_type)
    {
        if (ncatches == exc_start_pc.length)
        {
            char[] new_exc_start_pc = new char[ncatches * 2];
            char[] new_exc_end_pc = new char[ncatches * 2];
            char[] new_exc_handler_pc = new char[ncatches * 2];
            char[] new_exc_catch_type = new char[ncatches * 2];
            for (int i = 0; i < ncatches; i++)
            {
                new_exc_start_pc[i] = exc_start_pc[i];
                new_exc_end_pc[i] = exc_end_pc[i];
                new_exc_handler_pc[i] = exc_handler_pc[i];
                new_exc_catch_type[i] = exc_catch_type[i];
            }
            exc_start_pc = new_exc_start_pc;
            exc_end_pc = new_exc_end_pc;
            exc_handler_pc = new_exc_handler_pc;
            exc_catch_type = new_exc_catch_type;
        }
        exc_start_pc[ncatches] = (char)start_pc;
        exc_end_pc[ncatches] = (char)end_pc;
        exc_handler_pc[ncatches] = (char)handler_pc;
        exc_catch_type[ncatches] = (char)catch_type;
        ncatches++;
    }
    
/** add a line number entry
 */
    public void addLineNumber(int start_pc, int lnum)
    {
        if (nlines > 0 && line_start_pc[nlines - 1] == start_pc)
            nlines--;
        if (nlines == line_start_pc.length)
        {
            char[] new_line_start_pc = new char[nlines * 2];
            char[] new_line_number = new char[nlines * 2];
            for (int i = 0; i < nlines; i++)
            {
                new_line_start_pc[i] = line_start_pc[i];
                new_line_number[i] = line_number[i];
            }
            line_start_pc = new_line_start_pc;
            line_number = new_line_number;
        }
        line_start_pc[nlines] = (char)start_pc;
        line_number[nlines] = (char)lnum;
        nlines++;
    }
    
/** add a local variable entry
 */
    public void addLocalVar(int reg, Definition v)
    {
        if (debuginfo)
        {
            int address = v.address();
            while (address >= lvar.length)
            {
                char[] new_lvar_start_pc = new char[lvar.length * 2];
                char[] new_lvar_length = new char[lvar.length * 2];
                char[] new_lvar_reg = new char[lvar.length * 2];
                Definition[] new_lvar = new Definition[lvar.length * 2];
                for (int i = 0; i < lvar.length; i++)
                {
                    new_lvar_start_pc[i] = lvar_start_pc[i];
                    new_lvar_length[i] = lvar_length[i];
                    new_lvar_reg[i] = lvar_reg[i];
                    new_lvar[i] = lvar[i];
                }
                lvar_start_pc = new_lvar_start_pc;
                lvar_length = new_lvar_length;
                lvar_reg = new_lvar_reg;
                lvar = new_lvar;
            }
            int adr = address;
            lvar_start_pc[adr] = MAX_CHAR;
            lvar_length[adr] = MAX_CHAR;
            lvar_reg[adr] = (char)reg;
            lvar[adr] = v;
            nvars++;
        }
    }
    
    public void setStartPc(int adr)
    {
        if (debuginfo && (cp < MAX_CHAR) &&
            (adr < lvar_start_pc.length) && (lvar_start_pc[adr] == MAX_CHAR))
            lvar_start_pc[adr] = (char)cp;
    }
    
    public void setEndPc(int adr)
    {
        if (debuginfo && (adr < lvar_start_pc.length) &&
            (lvar_start_pc[adr] != MAX_CHAR) &&
            (cp - lvar_start_pc[adr] < MAX_CHAR) &&
            (lvar_length[adr] == MAX_CHAR))
            lvar_length[adr] = (char)(cp - lvar_start_pc[adr]);
    }
}

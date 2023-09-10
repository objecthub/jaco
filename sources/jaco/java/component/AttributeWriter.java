//      /   _ _      JaCo
//  \  //\ / / \     - extensible component to write attributes to classfiles
//   \//  \\_\_/     
//         \         Matthias Zenger, 06/06/00

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.*;
import java.io.File;


public interface AttributeConst
{
    int     BAD_ATTR                = 0x0000;
    int     SOURCEFILE_ATTR         = 0x0001;
    int     SYNTHETIC_ATTR          = 0x0002;
    int     DEPRECATED_ATTR         = 0x0004;
    int     CODE_ATTR               = 0x0008;
    int     EXCEPTIONS_ATTR         = 0x0010;
    int     CONSTANT_VALUE_ATTR     = 0x0020;
    int     LINE_NUM_TABLE_ATTR     = 0x0040;
    int     LOCAL_VAR_TABLE_ATTR    = 0x0080;
    int     BRIDGE_ATTR             = 0x0100;
    int     INNERCLASSES_ATTR       = 0x8000;
    
    Name    SOURCEFILE_N = Name.fromString("SourceFile");
    Name    SYNTHETIC_N = Name.fromString("Synthetic");
    Name    BRIDGE_N = Name.fromString("Bridge");
    Name    DEPRECATED_N = Name.fromString("Deprecated");
    Name    CODE_N = Name.fromString("Code");
    Name    EXCEPTIONS_N = Name.fromString("Exceptions");
    Name    CONSTANT_VALUE_N = Name.fromString("ConstantValue");
    Name    LINE_NUM_TABLE_N = Name.fromString("LineNumberTable");
    Name    LOCAL_VAR_TABLE_N = Name.fromString("LocalVariableTable");
    Name    INNERCLASSES_N = Name.fromString("InnerClasses");
}


public class AttributeWriter implements AttributeConst, ModifierConst, DefinitionConst
{
/** the classfile output buffer
 */
    protected ClassfileOutput       out;

/** the current constant pool
 */
    protected Pool                  pool;
    
/** the class writer that instantiated this attribute writer
 */
    protected ClassWriter           parent;

    
/** constructor
 */
    public AttributeWriter(ClassWriter parent)
    {
        this.parent = parent;
        this.out = parent.out;
        this.pool = parent.pool;
    }
    
/** write header for an attribute to active output buffer
 */
    protected int writeAttr(Name attrName)
    {
        out.writeChar(pool.put(attrName));
        out.writeInt(0);
        return out.getPos();
    }
    
    protected boolean endAttr(int start)
    {
        out.putInt(start - 4, out.op - start);
        return true;
    }
    
    public int writeZeroChar()
    {
        int op = out.op;
        out.writeChar(0);
        return op;
    }
    
    public void fixupChar(int op, int nattrs)
    {
        out.putChar(op, nattrs);
    }
    
    public void writeAttributes(int attrs, Definition def)
    {
        fixupChar(writeZeroChar(), writeAttribSet(attrs, def));
    }
    
    public int writeAttribSet(int attrs, Definition def)
    {
        int i = 1;
        int nattrs = 0;
        
        while (attrs != 0)
        {
            if (((attrs & i) != 0) && writeAttribute(i, def))
                nattrs++;
            attrs &= ~i;
            i <<= 1;
        }
        return nattrs;
    }
    
    public boolean writeAttribute(int attr, Definition def)
    {
        switch (attr)
        {
        // class attributes
            case SOURCEFILE_ATTR:
                switch (def)
                {
                    case ClassDef(_, _, _, _, Name sourcename):
                        if (sourcename != null)
                        {
                            String str = sourcename.toString();
                            str = str.substring(str.lastIndexOf(File.separatorChar) + 1);
                            int adr = writeAttr(SOURCEFILE_N);
                            out.writeChar(pool.put(Name.fromString(str)));
                            return endAttr(adr);
                        }
                }
                break;
            
            case INNERCLASSES_ATTR:
                switch (def)
                {
                    case ClassDef(_, _, _, _, _):
                        return writeInnerClassesAttr(def.locals().elems);
                }
                break;
                
        // method attributes
            case CODE_ATTR:
                switch (def)
                {
                    case MethodDef(Code code):
                        if (code != null)
                            return writeCodeAttr(code, def);
                }
                break;
                
            case EXCEPTIONS_ATTR:
                switch (def)
                {
                    case MethodDef(_):
                        Type[]  thrown = def.type.thrown();
                        if ((thrown != null) && (thrown.length > 0))
                        {
                            int adr = writeAttr(EXCEPTIONS_N);
                            out.writeChar(thrown.length);
                            for (int i = 0; i < thrown.length; i++)
                                out.writeChar(pool.put(thrown[i].tdef()));
                            return endAttr(adr);
                        }
                }
                break;
                
            case LINE_NUM_TABLE_ATTR:
                switch (def)
                {
                    case MethodDef(Code code):
                        if (code.nlines > 0)
                        {
                            int adr = writeAttr(LINE_NUM_TABLE_N);
                            out.writeChar(code.nlines);
                            for (int i = 0; i < code.nlines; i++)
                            {
                                out.writeChar(code.line_start_pc[i]);
                                out.writeChar(code.line_number[i]);
                            }
                            return endAttr(adr);
                        }
                }
                break;
                
            case LOCAL_VAR_TABLE_ATTR:
                switch (def)
                {
                    case MethodDef(Code code):
                        if (code.nvars > 0)
                        {
                            int adr = writeAttr(LOCAL_VAR_TABLE_N);
                            out.writeChar(code.nvars);
                            int nvars = code.nvars;
                            int i = 0;
                            while (nvars > 0)
                            {
                                if (code.lvar[i] != null)
                                {
                                    out.writeChar(code.lvar_start_pc[i]);
                                    out.writeChar(code.lvar_length[i]);
                                    out.writeChar(pool.put(code.lvar[i].name));
                                    out.writeChar(pool.put(parent.signatures.typeToSig(
                                                                code.lvar[i].type)));
                                    out.writeChar(code.lvar_reg[i]);
                                    nvars--;
                                }
                                i++;
                            }
                            return endAttr(adr);
                        }
                }
                break;
                
        // general attributes
            case SYNTHETIC_ATTR:
                if ((def.modifiers & SYNTHETIC) != 0)
                    return endAttr(writeAttr(SYNTHETIC_N));
                break;
            
            case BRIDGE_ATTR:
                if ((def.modifiers & BRIDGE) != 0)
                    return endAttr(writeAttr(BRIDGE_N));
                break;
                
            case DEPRECATED_ATTR:
                if ((def.modifiers & DEPRECATED) != 0)
                    return endAttr(writeAttr(DEPRECATED_N));
                break;
            
            case CONSTANT_VALUE_ATTR:
                if (def.type.isTypeOfConstant())
                {
                    int adr = writeAttr(CONSTANT_VALUE_N);
                    out.writeChar(pool.put(def.type.tconst()));//TODO: coerce to type?
                    return endAttr(adr);
                }
                break;

            default:
                throw new InternalError("unknown attribute: " + attr);
        }
        return false;
    }
    
/** write code attribute, entering all definition references into given pool
 */
    protected boolean writeCodeAttr(Code code, Definition def)
    {
        int adr = writeAttr(CODE_N);
        out.writeChar(code.max_stack);
        out.writeChar(code.max_locals);
        out.writeInt(code.cp);
        out.writeBytes(code.code, 0, code.cp);
        out.writeChar(code.ncatches);
        for (int i = 0; i < code.ncatches; i++)
        {
            out.writeChar(code.exc_start_pc[i]);
            out.writeChar(code.exc_end_pc[i]);
            out.writeChar(code.exc_handler_pc[i]);
            out.writeChar(code.exc_catch_type[i]);
        }
        int attr = LINE_NUM_TABLE_ATTR;
        if (code.debuginfo)
            attr |= LOCAL_VAR_TABLE_ATTR;
        writeAttributes(attr, def);
        return endAttr(adr);
    }
    
/** write innerclasses attribute if necessary
 */
    protected boolean writeInnerClassesAttr(Definition root)
    {
        int         adr = -1;
        int         nadr = -1;
        int         ninner = 0;
        Hashtable   innerClasses = new Hashtable();
        Stack       worklist= new Stack();
        
        worklist.push(root);
        while (pool.classes != null)
        {
            root = (Definition)pool.classes.obj;
            Mangler.Mangle  info = parent.mangler.get(root.fullname);
            if ((info != null) &&
                (innerClasses.put(root.fullname, root) == null))
            {
                worklist.push(info.owner.locals().elems);
                if (ninner++ == 0)
                {
                    adr = writeAttr(INNERCLASSES_N);
                    nadr = writeZeroChar();
                }
                out.writeChar(pool.put(root.def));
                out.writeChar(pool.put(info.owner));
                out.writeChar(pool.put(info.name));
                out.writeChar((info.mods & LANGUAGE_MODS) & ~STRICTFP);
            }
            pool.classes = pool.classes.next;
        }
        while (!worklist.empty())
        {
            root = (Definition)worklist.pop();
            while (root != null)
            {
                if (root.def.kind == TYP)
                {
                    Mangler.Mangle  info = parent.mangler.get(root.def.fullname);
                    if ((info != null) &&
                        (innerClasses.put(root.def.fullname, root.def) == null))
                    {
                        worklist.push(info.owner.locals().elems);
                        if (ninner++ == 0)
                        {
                            adr = writeAttr(INNERCLASSES_N);
                            nadr = writeZeroChar();
                        }
                        out.writeChar(pool.put(root.def));
                        out.writeChar(pool.put(info.owner));
                        out.writeChar(pool.put(info.name));
                        out.writeChar(info.mods);
                    }
                }
                root = root.sibling;
            }
        }
        if (ninner > 0)
        {
            fixupChar(nadr, ninner);
            return endAttr(adr);
        }
        else
            return false;
    }
}

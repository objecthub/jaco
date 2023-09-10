//      /   _ _      JaCo
//  \  //\ / / \     - extended attribute writer
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.pizza.struct.*;
import java.util.*;


public interface PizzaAttributeConst
{
    int     JACO_EAC_ATTR           = 0x0100;
    int     JACO_INTERFACE_ATTR     = 0x4000;
    int     JACO_META_ATTR          = 0x10000;
    
    Name    JACO_EAC_N = Name.fromString("JacoEAC");
    Name    JACO_INTERFACE_N = Name.fromString("JacoInterface");
    Name    JACO_META_N = Name.fromString("JacoMeta");
}

public class PizzaAttributeWriter extends AttributeWriter
                                  implements PizzaAttributeConst, PizzaModifierConst
{
    public PizzaAttributeWriter(ClassWriter parent)
    {
        super(parent);
    }
    
    public boolean writeAttribute(int attr, Definition def)
    {
        switch (attr)
        {
            case JACO_EAC_ATTR:
                int         tag = -1;
                switch (def)
                {
                    case MethodDef(_):
                        if ((def.modifiers & CASEDEF) != 0)
                            tag = ((MDef)def).tag;
                        break;
                    
                    case VarDef(_, _, _):
                        if ((def.modifiers & CASEDEF) != 0)
                            tag = ((VDef)def).tag;
                        break;
                }
                if (tag >= 0)
                {
                    int adr = writeAttr(JACO_EAC_N);
                    out.writeChar(tag);
                    return endAttr(adr);
                }
                else
                    return false;
                
            case JACO_INTERFACE_ATTR:
                switch (def)
                {
                    case ClassDef(_, _, _, _, _):
                        return writeInterfaceAttr((CDef)def);
                }
                break;
                
            case JACO_META_ATTR:
                String metaData = null;
                switch (def) {
                    case ClassDef(_, _, _, _, _):
                        metaData = ((CDef)def).metaData;
                        break;
                    case MethodDef(_):
                        metaData = ((MDef)def).metaData;
                        break;
                    case VarDef(_, _, _):
                        metaData = ((VDef)def).metaData;
                        break;
                }
                if (metaData != null) {
                    int adr = writeAttr(JACO_META_N);
                    out.writeChar(pool.put(Name.fromString(metaData)));
                    return endAttr(adr);
                } else
                    return false;
        }
        return super.writeAttribute(attr, def);
    }
    
    public boolean writeInterfaceAttr(CDef c)
    {
        if (c.origSupertype == null)
            return false;
        int adr = writeAttr(JACO_INTERFACE_N);
        out.writeChar((c.origModifiers & LANGUAGE_MODS) | ACC_SUPER);
        out.writeChar(c.origModifiers >> 16);
        out.writeChar(c.tag);
        out.writeChar(c.pool.put(c.baseClass));
        out.writeChar(c.pool.put(c.origSupertype.tdef()));
        Type[] is = c.origInterfaces;
        out.writeChar(is.length);
        for (int i = 0; i < is.length; i++)
            out.writeChar(c.pool.put(is[i].tdef()));
        Definition  elems = c.origLocals.elems;
        int classAttr = parent.classAttr;
        int methodAttr = parent.methodAttr;
        int fieldAttr = parent.fieldAttr;
        parent.classAttr = DEPRECATED_ATTR | JACO_META_ATTR;
        parent.methodAttr = EXCEPTIONS_ATTR | DEPRECATED_ATTR | JACO_EAC_ATTR | JACO_META_ATTR;
        parent.fieldAttr = CONSTANT_VALUE_ATTR | DEPRECATED_ATTR | JACO_EAC_ATTR | JACO_META_ATTR;
        parent.writeDefinitions(elems, VAR);
        parent.writeDefinitions(elems, FUN);
        parent.writeAttributes(c);
        parent.classAttr = classAttr;
        parent.methodAttr = methodAttr;
        parent.fieldAttr = fieldAttr;
        return endAttr(adr);
    }
}

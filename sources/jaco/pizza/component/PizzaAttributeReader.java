//      /   _ _      JaCo
//  \  //\ / / \     - extended attribute reader
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.pizza.struct.*;
import Type.*;


public class PizzaAttributeReader extends AttributeReader
                                  implements PizzaAttributeConst, PizzaModifierConst
{
    public PizzaAttributeReader(ClassReader reader)
    {
        super(reader);
    }
    
    public int nameToId(Name name)
    {
        if (name == JACO_EAC_N)
            return JACO_EAC_ATTR;
        if (name == JACO_INTERFACE_N)
            return JACO_INTERFACE_ATTR;
        return super.nameToId(name);
    }
    
    public void readAttribute(Definition def, int attr, int attrLen)
    {
        switch (attr)
        {
            case INNERCLASSES_ATTR:
                int n = in.nextChar();
                for (int i = 0; i < n; i++)
                {
                    Definition  inner = (Definition)reader.readPool(in.nextChar());
                    Definition  outer = (Definition)reader.readPool(in.nextChar());
                    Name        name = (Name)reader.readPool(in.nextChar());
                    int         flags = in.nextChar();
                    if (name != null) {
                        mangler.put(inner.fullname, outer, name, flags);
                        if (outer == def)
                        {
                            def.locals().enterIfAbsent(inner.proxy(name));
                            if (((CDef)def).origLocals == null)
                                ((CDef)def).origLocals = new Scope(null, def);
                            ((CDef)def).origLocals.enterIfAbsent(inner.proxy(name));
                            inner.owner = def;
                            if (definitions.isInnerclass(inner))
                                ((ClassType)inner.type).outer = def.type;
                        }
                    }
                }
                return;
            
            case JACO_EAC_ATTR:
                int tag = in.nextChar();
                switch (def)
                {
                    case MethodDef(_):
                        ((MDef)def).tag = tag;
                        def.modifiers |= CASEDEF;
                        break;
                    
                    case VarDef(_, _, _):
                        ((VDef)def).tag = tag;
                        def.modifiers |= CASEDEF;
                        break;
                }
                break;
            
            case JACO_INTERFACE_ATTR:
                readInterfaceAttr((CDef)def);
                break;
            
            default:
                super.readAttribute(def, attr, attrLen);
        }
    }
    
    protected void readInterfaceAttr(CDef c)
    {
        if (c.origLocals == null)
            c.origLocals = new Scope(null, c);
        c.origModifiers = in.nextChar();
        c.origModifiers |= (in.nextChar() << 16);
        c.tag = in.nextChar();
        c.baseClass = (CDef)reader.definitions.defineClass(
                            reader.readClassName(in.nextChar()));
        c.origSupertype = reader.definitions.defineClass(
                            reader.readClassName(in.nextChar())).type;
        Type[] is = new Type[in.nextChar()];
        c.origInterfaces = is;
        for  (int i = 0; i < is.length; i++)
            is[i] = reader.definitions.defineClass(
                        reader.readClassName(in.nextChar())).type;
        reader.methodAttr |= JACO_EAC_ATTR;
        reader.fieldAttr |= JACO_EAC_ATTR;
        char    fieldCount = in.nextChar();
        for (int i = 0; i < fieldCount; i++)
            c.origLocals.enter(reader.readField());
        char    methodCount = in.nextChar();
        for (int i = 0; i < methodCount; i++)
            c.origLocals.enter(reader.readMethod());
        reader.methodAttr &= ~JACO_EAC_ATTR;
        reader.fieldAttr &= ~JACO_EAC_ATTR;
        readAttributes(c, reader.classAttr & ~JACO_INTERFACE_ATTR);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - extensible component to read attributes from classfiles
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.*;
import Type.*;
import Definition.*;


public class AttributeReader implements AttributeConst, ModifierConst, DefinitionConst
{
/** the classfile output buffer
 */
    protected ClassfileInput    in;

/** class reader and type component
 */
    protected ClassReader       reader;
    protected Types             types;
    protected Definitions       definitions;
    protected Mangler           mangler;
    
    
/** constructor
 */
    public AttributeReader(ClassReader reader)
    {
        this.in = reader.in;
        this.reader = reader;
        this.types = reader.types;
        this.definitions = reader.definitions;
        this.mangler = reader.mangler;
    }

    public int nameToId(Name name)
    {
        if (name == SOURCEFILE_N)
            return SOURCEFILE_ATTR;
        if (name == SYNTHETIC_N)
            return SYNTHETIC_ATTR;
        if (name == DEPRECATED_N)
            return DEPRECATED_ATTR;
        if (name == CODE_N)
            return CODE_ATTR;
        if (name == EXCEPTIONS_N)
            return EXCEPTIONS_ATTR;
        if (name == CONSTANT_VALUE_N)
            return CONSTANT_VALUE_ATTR;
        if (name == LINE_NUM_TABLE_N)
            return LINE_NUM_TABLE_ATTR;
        if (name == LOCAL_VAR_TABLE_N)
            return LOCAL_VAR_TABLE_ATTR;
        if (name == INNERCLASSES_N)
            return INNERCLASSES_ATTR;
        return BAD_ATTR;
    }
    
    public Definition readAttributes(Definition def, int attrs)
    {
        Definition res = def;
        char    nattr = in.nextChar();
        for (int i = 0; i < nattr; i++)
        {
            Name attrName = (Name)reader.readPool(in.nextChar());
            if ((def.kind == FUN) && attrName.toString().equals("Bridge"))
            	res = null;
            int attr = nameToId(attrName);
            int attrLen = in.nextInt();
            if ((attrs & attr) == 0)    // unknown attribute
                in.skip(attrLen);
            else
                readAttribute(def, attr, attrLen);
        }
        return res;
    }
    
    public void readAttribute(Definition def, int attr, int attrLen)
    {
        switch (attr)
        {
        // class attributes
            case SOURCEFILE_ATTR:
                ((ClassDef)def).sourcefile = (Name)reader.readPool(in.nextChar());
                return;
            
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
                            inner.owner = def;
                            if (definitions.isInnerclass(inner))
                                ((ClassType)inner.type).outer = def.type;
                        }
                    }
                }
                return;
                
        // method attributes
            case CODE_ATTR:
                in.skip(attrLen);
                return;
                
            case EXCEPTIONS_ATTR:
                int     nexceptions = in.nextChar();
                Type[]  thrown = new Type[nexceptions];
                for (int j = 0; j < nexceptions; j++)
                    thrown[j] = definitions.defineClass(
                                    reader.readClassName(in.nextChar())).type;
                ((MethodType)def.type).thrown = thrown;
                return;
                
            case LINE_NUM_TABLE_ATTR:
                in.skip(attrLen);
                return;
                
            case LOCAL_VAR_TABLE_ATTR:
                in.skip(attrLen);
                return;
                
        // general attributes
            case SYNTHETIC_ATTR:
                def.modifiers |= SYNTHETIC;
                return;
                
            case DEPRECATED_ATTR:
                def.modifiers |= DEPRECATED;
                return;
                
            case CONSTANT_VALUE_ATTR:
                Type    ctype = (Type)reader.readPool(in.nextChar());
                def.type = types.coerce(ctype, def.type);
                return;
        }
        throw new InternalError();
    }
}

package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.cjava.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.context.*;
import java.io.*;
import CJType.*;
import CJDefinition.*;
import Constant.*;
import CJTree.*;
import java.util.Hashtable;

/**
 * Write extended cjava attribute understood by CJAttributeWriter.
 */
public class CJClassWriter extends ClassWriter implements CJAttributeConst
{
    CJCompounds cjcompounds;
    Types types;

    public CJClassWriter()
    {
    classAttr |= ALIAS_ATTR;
    }

    public void init(CompilerContext context)
    {
    super.init(context);
    cjcompounds = ((CJMainContext)context.mainContext).CJCompounds();
    types = context.mainContext.Types();
    }
    
    public AttributeWriter AttributeWriter()
    {
    return new CJAttributeWriter(this);
    }

    public String getName()
    {
    return "CJClassWriter";
    }

    public void writeAttributes(Definition def)
    {

    switch(def)
        {
        case MethodDef(_):
        if(def.owner instanceof CJClassDef)
            {
            CJClassDef cjc = (CJClassDef)def.owner;
            boolean specialdb = def.name.equals(Name.fromString("createLabel")) && cjc.name.equals(Name.fromString("Named"));
            TwoFacedMethodDef m = (TwoFacedMethodDef)cjc.matching(def.name, def.type, types);
            if(specialdb&&m==null)
                {
                cjc.dump(System.err, 1);
                }
            if(m!=null)
                {
                m.standard();
                m.code = ((Definition.MethodDef)def).code;
                attrib.writeAttributes(methodAttr|CJAVA_REALSIG_ATTR, (Definition)m);
                return;
                }
            }
        super.writeAttributes(def);
        break;

        case VarDef(_, _, _):
        if(def.owner instanceof CJClassDef)
            {
            CJClassDef cjc = (CJClassDef)def.owner;
            TwoFacedVarDef m = (TwoFacedVarDef)cjc.matching(def.name, null, null);
            if(m!=null)
                {
                m.standard();
                attrib.writeAttributes(fieldAttr|CJAVA_REALVARTYPE_ATTR, (Definition)m);
                return;
                }
            }
        super.writeAttributes(def);
        break;

        default:
        super.writeAttributes(def);
        }
    }
}

package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.cjava.context.*;
import jaco.java.struct.*;
import jaco.cjava.struct.*;
import java.util.*;

/**
 * Additional attributes for CJava.
 *
 */
public interface CJAttributeConst
{
    int CJAVA_REALSIG_ATTR = 0x0200; // ? 
    Name CJAVA_REALSIG_N = Name.fromString("CJRealSig");
    int CJAVA_REALVARTYPE_ATTR = 0x0400;
    Name CJAVA_REALVARTYPE_N = Name.fromString("CJRealVar");
    int ALIAS_ATTR = 0x800;
    Name ALIAS_N = Name.fromString("CJAlias");
}

/**
 * Extended attribute writer that writes attributes defined in CJAttributeConst
 */
public class CJAttributeWriter extends AttributeWriter implements CJAttributeConst
{
    /**
     * Extended signatures containing compounds.
     */
    CJSignatures cjsignatures;

    public CJAttributeWriter(ClassWriter parent)
    {
    super(parent);
    cjsignatures = ((CJMainContext)parent.context).CJSignatures();
    }

    /**
     * Write attributes, including ALIAS_ATTR, CJAVA_REALSIG_ATTR and CJAVA_REALVARTYPE_ATTR.
     *
     * <b>Class attribute: ALIAS_ATTR</b>
     * 
     *   It marks the class as being an alias and contains the real type
     *   of the alias.
     * 
     *   byte 0: a pointer to a CJSignature in the pool, from CJClassDef.getAliasSignature()
     *
     * <b>Field attribute: CJAVA_REALVARTYPE_ATTR</b>
     *
     *   If the field type is a compound, a fake standard type is set
     *   to it and the real type is written in this attribute.
     * 
     *   byte 0: a pointer to a CJSignature in the pool. 
     *
     * <b>Class attribute: CJAVA_REALSIG_ATTR</b>
     *
     *   If the class signature or exception list contains compound types,
     *   or if it was renamed, a signature, exception list and name valid
     *   only for CJava is written in this attribute.
     *
     *   <p>byte 0: a pointer to the real name of the class in the pool
     *   <p>byte 1: a pointer to the CJSignature of the method in the pool
     *   <p>byte 2: N the number of exceptions thrown by the class 
     *   <p>byte 3-N+1: a CJSignature of an exception 
     */
    public boolean writeAttribute(int attr, Definition def)
    {
    switch (attr)
        {
        /* class attribute; see comment in method */
        case ALIAS_ATTR:
        CJClassDef cjc = (CJClassDef)def;
        if(cjc.isAlias())
            {
            int adr = writeAttr(ALIAS_N);
            out.writeChar(pool.put(cjc.getAliasSignature() ) );
            return endAttr(adr);
            }
        return false;

        /* field attribute; see comment in method */
        case CJAVA_REALVARTYPE_ATTR:
        if(def instanceof TwoFacedVarDef)
            {
            TwoFacedVarDef v = (TwoFacedVarDef)def;
            int adr = writeAttr(CJAVA_REALVARTYPE_N);
            out.writeChar(pool.put(cjsignatures.typeToSig(v.getSpecialType())));
            return endAttr(adr);
            }
        return false;

        /* class attribute; see comment in method */
        case CJAVA_REALSIG_ATTR:
        if(def instanceof TwoFacedMethodDef)
            {
            TwoFacedMethodDef m = (TwoFacedMethodDef)def;
            Type t = m.getSpecialType();
            switch(t)
                {
                case MethodType(Type[] args, Type result, Type[] thrown):
                int adr = writeAttr(CJAVA_REALSIG_N);
                out.writeChar(pool.put(m.getSpecialName()));
                out.writeChar(pool.put(cjsignatures.typeToSig(t)));
                if(thrown!=null)
                    {
                    out.writeChar(thrown.length);
                    for(int i=0; i<thrown.length; i++)
                        out.writeChar(pool.put(cjsignatures.typeToSig(thrown[i])));
                    }
                else
                    out.writeChar(0);
                return endAttr(adr);
                default:
                throw new InternalError(def.name + " type=" + t.toString());
                }
            }
        return false;

        default:
        return super.writeAttribute(attr, def);
        }

    }
}

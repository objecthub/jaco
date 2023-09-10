package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.cjava.context.*;
import jaco.cjava.struct.*;
import Type.*;
import java.util.*;

/**
 * Read special attributes for compounds and aliases
 */
public class CJAttributeReader extends AttributeReader implements CJAttributeConst
{
    /**
     * CJAliases component.
     */
    CJAliases cjaliases;

    /**
     * CJSignatures component
     */
    CJSignatures cjsignatures;
    
    /**
     * Main context.
     */
    CJMainContext maincontext;

    public CJAttributeReader(ClassReader parent)
    {
    super(parent);
    maincontext = ((CJMainContext)parent.context);
    cjsignatures = maincontext.CJSignatures();
    cjaliases = maincontext.CJAliases();

    }
    
    public int nameToId(Name name)
    {
    if (name == CJAVA_REALSIG_N)
        return CJAVA_REALSIG_ATTR;
    else if(name == CJAVA_REALVARTYPE_N)
        return CJAVA_REALVARTYPE_ATTR;
    else if(name == ALIAS_N)
        return ALIAS_ATTR;
    return super.nameToId(name);
    }
    
    /**
     * Read attributes, including ALIAS_ATTR, CJAVA_REALVARTYPE_ATTR & CJAVA_REALSIG_ATTR.
     */
    public void readAttribute(Definition def, int attr, int attrLen)
    {
    switch (attr)
        {
        /**
         * Class attribute - Read the real type of an alias type and
         * transform the class definition into an alias. 
         */
        case ALIAS_ATTR:
        CJClassDef cjc = (CJClassDef)def;
        Name sig = (Name)reader.readPool(in.nextChar());
        Type realtype = (Type)cjsignatures.sigToType(Name.names, sig.index, sig.length());
        cjc.aliasTo(realtype);
        if(maincontext.useCJ())
            cjc.special();
        else
            cjc.standard();
        /*      if(def.type.tdef()==def)
                throw new InternalError(def.fullname.toString());*/
        break;

        /**
         * Field attribute - Read the alternate type of an field.
         *
         * The type may be a compound type (and usually is).
         */
        case CJAVA_REALVARTYPE_ATTR:
        Name sig = (Name)reader.readPool(in.nextChar());
        Type realtype = (Type)cjsignatures.sigToType(Name.names, sig.index, sig.length());
        TwoFacedVarDef m = (TwoFacedVarDef)def;
        m.setSpecialType(realtype);
        if(maincontext.useCJ())
            m.special();
        else
            m.standard();
        break;

        /**
         * Method attribute - Read the alternate name, signature, exception
         * list of the method.
         *
         * The signature or the list of exception may contain compound
         * types. 
         */
        case CJAVA_REALSIG_ATTR:
        Name realname = (Name)reader.readPool(in.nextChar());
        Name sig = (Name)reader.readPool(in.nextChar());
        Type realtype = (Type)cjsignatures.sigToType(Name.names, sig.index, sig.length());
        if(realtype.tdef()==null)
            throw new InternalError();

        TwoFacedMethodDef m = (TwoFacedMethodDef)def;
        m.setSpecialName(realname);
        m.setSpecialType(realtype);
        if(maincontext.useCJ())
            m.special();
        else
            m.standard();

        int n_exceptions = in.nextChar();
        if(n_exceptions>0)
            {
            Type[] thrown = new Type[n_exceptions];
            for(int i=0; i<n_exceptions; i++)
                {
                Name thrownsig = (Name)reader.readPool(in.nextChar());
                thrown[i] = (Type)cjsignatures.sigToType(Name.names, thrownsig.index, thrownsig.length());
                }
            ((MethodType)realtype).thrown = thrown;
            }
        break;
    
        default:
        super.readAttribute(def, attr, attrLen);
        }
    }
}

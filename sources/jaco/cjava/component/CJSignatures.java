package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.cjava.context.*;
import jaco.cjava.struct.*;
import jaco.java.struct.*;
import java.util.*;
import Type.*;

/**
 * Extends standard java signature to support compounds.
 *
 * Format of a compound in the signature:
 *
 * compound-signature ::= "P" { type-signature } ";" 
 *
 * This component shouldn't be used instead of the standard
 * signatures component, because it would create non-standard 
 * signatures in java class files. Rather, the cjsignatures
 * are put into a special attribute written and read by
 * cjavac and ignored by the virtual machine. 
 *
 */
public class CJSignatures extends Signatures
{
    protected CJCompounds cjcompounds;

    public void init(MainContext context)
    {
    super.init(context);
    cjcompounds = ((CJMainContext)context).CJCompounds();
    }

    /**
     * Create a signature from a type.
     *
     * @param type the type for which a signature should be created
     * @result signature the signature (as a Name)
     */
    public Name typeToSig(Type type)
    {
        switch ((CJType)type.deref())
        {
        case CompoundType(_):
        {
            StringBuffer buf = new StringBuffer("P");
            Definition def = type.tdef();
            if(def.supertype()!=null)
            buf.append(typeToSig(def.supertype()).toString());
            Type[] tlist = def.interfaces();
            for(int i=tlist.length-1; i>=0; i--)
            {
                buf.append(typeToSig(tlist[i]).toString());
            }
            buf.append(';');
            return Name.fromString(buf.toString());
        }
        
        default:
        return super.typeToSig(type);
        }
    }

    /**
     * Read a type from the current signature.
     *
     * @result the type created from the signature
     */
    protected Type sigToType()
    {
        switch (signature[sigp])
        {
        case 'P': /* comPound */
        Vector tlist = new Vector();
        Type[] components;
        sigp++;
        while(signature[sigp] != ';')
            {
            Type newt = sigToType();
            tlist.addElement(newt);
            }
        sigp++;
        components = new Type[tlist.size()];
        Type t = ((CJType.Factory)types.make).CompoundType((Type[])tlist.toArray(components));
        //cjcompounds.fakeDefinitionNoChecks(t, components);
        return t;

        default:
        return super.sigToType();
        }
    }
}

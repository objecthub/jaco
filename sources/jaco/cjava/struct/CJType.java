package jaco.cjava.struct;

import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.component.*;
import jaco.cjava.context.*;
import CJDefinition.*;

/**
 * Type extensions.
 */
public class CJType extends Type
{
    /**
     * A compound type is the union of 
     * one or more interfaces and zero or one class.
     *
     * @param components the list of types 
     */
    public case CompoundType(Type[] components);

    /**
     * A temporary type used by CJEnterClasses.
     *
     * This type marks aliases definition whose
     * real type is not known yet. Using this
     * type causes CJEnterMembers.enter to be called
     * on the alias definition. 
     *
     * This way, aliases do not need to be created
     * in any order.
     *
     * @param aliascompleter the object responsible
     *  for getting the real type of the alias. Usually CJEnterMembers.
     * @see CJType.AliasCompleter
     */
    public case AliasType(AliasCompleter complete);

    /**
     * Complete an alias.
     *
     * Read and return the real type of the definition.
     * @see CJClassDef
     */
    public interface AliasCompleter
    {
    
    /**
     * Return the real type of the definition.
     *
     * @param the real type
     */
    public Type completeAlias(Definition def);
    }

    /**
     * Duplicate the type.
     * 
     * @result a copy
     */
    public Type dup()
    {
    Type    res = this;
    switch (this)
        {
        case CompoundType(Type[] components):
        {
            res = CompoundType(components);
            //  res.tdef = null;  not used for compound
            // res.tconst = null; 
            return res;
        }

        case AliasType(AliasCompleter completer):
        {
            res = AliasType(completer);
            res.setDef(tdef);
            //  res.tdef = null;  not used for compound
            // res.tconst = null; 
            return res;
        }
        
        default:
        return super.dup();
        }
    }

    /**
     * Convert the compound type to a string.
     *
     * WARNING: compounds are not converted the same
     * depending on whether their definition is set.
     *
     * If the definition is set, the output only
     * contains classes and interface names, and no
     * nested compounds and aliases names. The output
     * will also be ordered (class first, interfaces 
     * next in alphabetical order.)
     *
     * If the definition is no set, the compound type
     * is printed as it has been defined, with nested
     * compounds and alias names.
     */
    public String toString()
    {
    switch(this)
        {
        case CompoundType(Type[] components):
        {
            Definition d = super.tdef();
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            if(d!=null)
            {
                Type supertype = d.supertype();
                Type[] interfaces = d.interfaces();
                if(supertype!=null)
                buf.append(supertype.toString());
                for(int i=interfaces.length-1; i>=0; i--)
                {
                    if(i!=(interfaces.length-1) || supertype!=null)
                    buf.append(", ");
                    buf.append(interfaces[i].toString());
                }
            }
            else
            {
                for(int i=0; i<components.length; i++)
                {
                    if(i!=0)
                    buf.append(", ");
                    buf.append(components[i].toString());
                }
            }
            buf.append("]");
            return buf.toString();
        }
        
        case AliasType(AliasCompleter completer):
        System.out.println("ALIAS TOSTRING");
        return completer.completeAlias(super.tdef()).toString();
        
        default:
        return super.toString();
        }
    }

    /**
     * The tag.
     */
    public int tag()
    {
    switch (this)
        {
        case CompoundType(_):
        case AliasType(_):
        return TypeConst.REF;

        default:
        return super.tag();
        }
    }

    /**
     * Return the types of interfaces member of the compound.
     *
     * For compounds, it is a shortcut to tdef().interfaces().
     * A fake definition may be created by this call.
     *
     * @see CJDefinition.interfaces
     */
    public Type[] interfaces()
    {
        switch (this)
        {
            case CompoundType(_):
            return tdef().interfaces();

            case AliasType(AliasCompleter completer):
            completer.completeAlias(super.tdef());
            return tdef().interfaces();

            default:
            return super.interfaces();
        }
    }


    /** returns the supertype of this type.
     *
     * For compounds, it is a shortcut to tdef().supertype().
     * A fake definition may be created by this call.
     */
    public Type supertype()
    {
    switch (this)
        {
        case CompoundType(_):
        return tdef().supertype();

        case AliasType(AliasCompleter completer):
        completer.completeAlias(super.tdef());
        return tdef().supertype();
        

        default:
        return super.supertype();
        }
    }

    /**
     * Return the definition.
     *
     * WARNING: for compound, a definition will be CREATED
     * if none have been yet. I don't like this either, but
     * it seemed necessary...
     *
     * @result a definition, never null for CompoundDef
     */
    public Definition tdef()
    {
    switch(this)
        {
        case AliasType(AliasCompleter completer):
        Definition d = super.tdef();
        completer.completeAlias(d);
        return d;

        case CompoundType(Type[] tlist):
        Definition d = super.tdef();
        if(d==null)
            {
            /* DANGEROUS HACK ! 
             * 
             * I have to use this because I can't follow aliases in
             * CJSignature when called from CJAttributeReader.
             *
             * This means that I can't simply create the definition
             * in CJSignature or CJAttributeReader; I have to wait
             * until the definition is accessed.
             *
             * This also had to create the instance() method in CJCompound,
             * and that sucks, too.
             *
             * I need to find a better way of following aliases in
             * compounds loaded from CJAttributeReader/ALIAS_ATTR.
             */
            return CJCompounds.instance().fakeDefinitionNoChecks((CompoundType)this, tlist);
            }
        return d;

        default:
        return super.tdef();
        }
    }
 
   
    /**
     * Check the type.
     */
    public boolean erroneous()
    {
    switch (this)
        {
        case CompoundType(Type[] components):
        for(int i=0; i<components.length; i++)
            {
            if(components[i].erroneous())
                return true;
            }
        return false;

        case AliasType(AliasCompleter completer):
        return completer.completeAlias(super.tdef()).erroneous();


        default:
        return super.erroneous();
        }
    }
    
    /**
     * Return the 'real' type.
     */
    public Type deref()
    {
    switch(this)
        {
        case AliasType(AliasCompleter completer):
        return completer.completeAlias(super.tdef());

        default:
        return super.deref();
        }
    }

    public Type outer()
    {
    switch(this)
        {
        case AliasType(_):
        return this.deref().outer();
        
        default:
        return super.outer();
        }
    }

    
    /**
     * Extended type factory.
     *
     */
    public static interface Factory extends Type.Factory
    {
    /**
     * Create a compound type.
     *
     * If you need the list of types in the compounds, use
     * tdef().interfaces() and tdef().supertype() instead. These
     * calls will return only standard types, and no compounds or aliases.
     *
     * @param components the list of types in the compounds (may be compounds & aliases)
     */
    public Type CompoundType(Type[] components);

    /**
     * Create a temporary alias type
     *
     * @param completer the alias completer
     * @see CJType.AliasCompleter
     */
    public Type AliasType(AliasCompleter completer);
    }
}

package jaco.cjava.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.cjava.component.*;
import java.util.*;
import Definition.*;

/**
 * Special ClassDef object for aliases.
 *
 * These class definitions can be converted
 * to aliases using the aliasTo() method.
 * 
 * @see CJBasicClassDef
 */
public class CJClassDef extends CJBasicClassDef
{
    public CJClassDef(Type supertype, Type[] interfaces, Scope locals, Pool pool, Name sourcefile)
    {
    super(supertype, interfaces, locals, pool, sourcefile);
    }

    /**
     * If set, this classdef is really an alias.
     * (active unless def.type.tdef()==def)
     */
    private Type realtype;

    /**
     * Check whether the class definition is an alias.
     *
     * @param true if the class definition is an alias, in which case getRealType() will return
     *             a valid type.
     */
    public boolean isAlias()
    {
    return realtype!=null;
    }

    /**
     * Transform the class definition into an alias.
     *
     * It flags this definition as being an alias
     * and sets the real type as well as the special
     * type to the aliases type. 
     *
     * The definition is put into special representation.
     * (see TwoFacedClassDef#special)
     *
     * Remember that for an alias to be exported,
     * it is necessary to set its signature using
     * set AliasSignature.
     *
     * @see TwoFacedClassDef#special
     * @see CJClassDef#getRealType
     * @see CJClassDef#setSignature
     */
    public void aliasTo(Type alias)
    {
    realtype = alias;
    special();
    setSpecialType(alias);
    /*
    supertype = realtype.supertype();
    interfaces = realtype.interfaces();
    */
    }

    /**
     * The real signature, containing compounds and
     * unresolved aliases.
     *
     * Required whenever a alias is exported.
     */
    private Name aliasSignature;

    /**
     * Set the alias signature that will be put into
     * the class file.
     *
     * CJSignature should be able to parse this signature.
     * It can contain compounds and unresolved alias names.
     *
     * @param sig the signature
     * @see CJSignature
     */
    public void setAliasSignature(Name sig)
    {
    aliasSignature = sig;
    }

    /**
     * Get the alias signature.
     *
     * @result a signature, or null if it was not set
     * @see CJClassDef#setAliasSignature
     */
    public Name getAliasSignature()
    {
    return aliasSignature;
    }

    /**
     * Get the real type of the alias, if it is set.
     *
     * The type can contain compounds. 
     *
     * @param the real type or null, meaning the definition is not an alias.
     * @see CJClassDef#isAlias
     */
    public Type getRealType()
    {
    return realtype;
    }


    /**
     * Special version of supertype() for aliases.
     *
     * If it's an alias, return the real type's supertype
     */
    public Type supertype()
    {
    /* realtype==null || type.tdef()==this => it's working as a class, not an alias */
    if(realtype==null || type.tdef()==this) 
        return super.supertype();
    else
        return type.tdef().supertype();
    }

    /**
     * Special version of interfaces() for aliases.
     *
     * If it's an alias, return the real type's interface list.
     */
    public Type[] interfaces()
    {
    if(realtype==null || type.tdef()==this)
        return super.interfaces();
    else
        return type.tdef().interfaces();
    }
    
    /**
     * Special version of locals() for aliases.
     *
     * If it's an alias, return the real type's locals.
     */
    public Scope locals()
    {
    if(realtype==null || type.tdef()==this)
        return super.locals();
    else
        return type.tdef().locals();
    }
}

/**
 * Special ClassDef object for CJava.
 *
 * It saves two-faced methods and fields in
 * a hash table.
 *
 * This class is never used directly. It contains
 * only whatever was necessary to implement Compound Types.
 * CJClassDef contains extension to support aliases and
 * that is what the CJDefininionFactory class creates.
 *
 * It builds upon TwoFacedClassDef and adds a map that
 * can contain definitions for easy retrieval even
 * after the class definition scope has been modified
 * by the original jaco compiler. 
 *
 * TODO: use a scope instead of a Map 
 */
public class CJBasicClassDef extends TwoFacedClassDef
{
    public CJBasicClassDef(Type supertype, Type[] interfaces, Scope locals, Pool pool, Name sourcefile)
    {
    super(supertype, interfaces, locals, pool, sourcefile);
    }

    /**
     * Hash table of two-faced definitions.
     *
     * key = def.name
     * value = TwoFacedDefinition, or List of TwoFacedDefinition
     *         
     */
    protected Map map;

    /**
     * Add a new definition into the hash table.
     *
     * Warning: don't change the definition standard name after
     * this call ! You can change the standard type, though. 
     *
     * @param def two-faced definition to add
     */
    public void addDefinition(TwoFacedDefinition def)
    {
    /* DEBUG */
    if( !( def instanceof MethodDef || def instanceof VarDef) )
        throw new InternalError(def.getClass().toString());

    if(map==null)
        map = new HashMap();
    Name name = def.getStandardName();
    Object value = map.get(name);
    if(value==null)
        map.put(name, def);
    else 
        {
        if(!(value instanceof List))
        {
            value = new LinkedList();
            map.put(name, value);
        }
        ((List)value).add(def);
        }
    }

    /**
     * Dump some information about the class definition
     * and its registered classes (in the map.)
     *
     * @param out PrintStream, System.err or System.out, for example.
     * @param ident ident level 
     */
    public void dump(java.io.PrintStream w, int level)
    {
    super.dump(w, level);

    if(map!=null)
        {
        dump_ident(w, level+1); w.println("map:");
        Iterator iter = map.values().iterator();
        level++;
        while(iter.hasNext())
            {
            Object o = iter.next();
            if(o instanceof TwoFacedDefinition)
                ((TwoFacedDefinition)o).dump(w, level);
            else
                {
                Iterator li = ((List)o).iterator();
                while(li.hasNext())
                    {
                    ((TwoFacedDefinition)o).dump(w, level);
                    }
                }
            }
        }
    else
        {
        dump_ident(w, level+1); w.println("map: EMPTY");
        }
    }

    /**
     * Search for a two-faced definition, given its name and type.
     *
     * The type isn't necessary when looking for variable definitions.
     * Set it to null.
     *
     * @param name standard name
     * @param type standard type, or null
     * @param types the types component, necessary when looking for a method
     * @result a TwoFacedDefinition or null
     */
    public TwoFacedDefinition matching(Name stdname, Type stdtype, Types types)
    {
    if(map==null)
        return null;
    Object val = map.get(stdname);
    if(val instanceof List)
        {
        /* prerequisite: contains only MethodDef & ValDef */
        boolean non_method = (stdtype==null || types==null);
        Iterator iter = ((List)val).iterator();
        while(iter.hasNext())
            {
            TwoFacedDefinition d = (TwoFacedDefinition)iter.next();
            if( (non_method && !(d instanceof MethodDef) ) || 
                (!non_method && types.sametype(stdtype, d.getStandardType())) )
                {
                return d;
                }
            }
        return null;
        }
    else if(val!=null)
        {
        if(stdtype!=null && !types.sametype(stdtype, ((TwoFacedDefinition)val).getStandardType()))
            return null;
            
        }
    return (TwoFacedDefinition)val;
    }

    
}

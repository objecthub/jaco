package jaco.cjava.struct;

import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.framework.*;
import java.util.*;

/** 
 * Common values for TwoFaced.
 *
 * Should be used only by TwoFaced definitions.
 */
interface TwoFacedConst
{
    public final boolean STANDARD = false;
    public final boolean SPECIAL = true;
}

/**
 * This is a special method definition that 
 * has two representations. 
 * 
 * It is possible to switch from one representation
 * to the other at any time. It handles proxies
 * and is able to move definitions in a scope
 * if the name changes.
 *
 * This class, TwoFacedCommon, defines 
 * common operations for TwoFacedMethodDef and TwoFacedClassDef.
 *
 * Should be used only by TwoFaced definitions.
 *
 * I added a dump() and a dump_level() method, for 
 * debugging.
 */
class TwoFacedCommon implements TwoFacedConst
{
    /* DEBUG START */

    /**
     * Write a number of space for identation.
     *
     * @param stream printstream
     * @param level ident level
     */
    public void dump_ident(java.io.PrintStream w, int level)
    {
    for(int i=0; i<level; i++)
        w.print('\t');
    }

    /**
     * Dump information about the two-faced definition.
     *
     * @param stream printstream
     * @param level ident level
     */
    public void dump(java.io.PrintStream w, int level)
    {
    dump_ident(w, level); w.print(getClass().toString() + " " + getName(STANDARD));
    if(!hasAlternate())
        {
        w.println(" (simple)");
        dump_ident(w, level+1); w.println(" type=" + getType(STANDARD).toString());
        }
    else
        {
        w.println(" (two-faced. current face: " + ( getFace()==STANDARD ? "standard":"special" ) + " )");
        dump_ident(w, level+1); w.println("spc name=" + getName(SPECIAL));
        dump_ident(w, level+1); w.println("std type=" + getType(STANDARD));
        dump_ident(w, level+1); w.println("spc type=" + getType(SPECIAL));
        }
    }
    
    /* DEBUG ENT */

    /**
     * The definition.
     *
     * It contains the settings corresponding to the
     * current face.
     */
    protected Definition def;

    /**
     * The registery.
     *
     * Access it through register()/setRegistery()
     */
    protected TwoFacedRegistery registery;

    /**
     * Check whether the definition really has two different faces.
     *
     * @result true if there is a altname or altface
     */
    protected boolean hasAlternate()
    {
    return (has_altname && !altname.equals(def.name)) || 
        (has_alttype && alttype!=def.type);
    }

    /**
     * Set the registery
     */
    public void setRegistery(TwoFacedRegistery _registery)
    {
    registery = _registery;
    registered = false;
    register(); // check if it's already necessary
    }   

    /**
     * If true, the object has registered itself.
     */
    protected boolean registered; 

    /**
     * Register the definition.
     *
     * It is done as soon as an alternate definition exists.
     */
    public void register()
    {
    if(!registered && hasAlternate())
        registery.register((TwoFacedDefinition)def);
    }
    
    /**
     * the current face of the definition.  
     * 0=TwoFacedConst.STANDARD or 1=TwoFacedConst.SPECIAL [default=0].
     *
     */
    private boolean face;

    /**
     * If true, then altname is set
     */
    protected boolean has_altname;

    /**
     * The alternate name (the one for the alternate face).
     * If has_altname==false, the alternate name is the same
     * as the current name (in def.name/def.fullname)
     */
    protected Name altname;

    /**
     * If true, then alttype is set
     */
    protected boolean has_alttype;

    /**
     * The alternate type (the one for the alternate face).
     * If has_alttype==false, the alternate ype is the same
     * as the current name (in def.type)
     */
    protected Type alttype;

    /**
     * List of proxies.
     *
     * Proxies save the name, so changing the name will
     * change it in the proxies, too.
     */
    private List proxies = new LinkedList();

    /**
     * Get the content of the list of proxies, an a list iterator.
     *
     * (It is possible to remove proxies using this iterator.)
     */
    public ListIterator proxies()
    {
    return proxies.listIterator();
    }

    /**
     * Add a new proxy to the list.
     *
     * @param proxy the proxy 
     */
    public void addProxy(Definition proxy)
    {
    proxies.add(proxy);
    }
    
    /**
     * Create a new object and link it to a
     * definition.
     */
    public TwoFacedCommon(Definition _def)
    {
    def = _def;
    face = STANDARD;
    }

    /**
     * Change the name of the definition.
     *
     * It does not change or access anything in altname/has_altname.
     * 
     * It changes the name of the proxies, too.
     *
     * @param name the new name.
     */
    protected void changeName(Name name)
    {
    if(!name.equals(def.name))
        {
        int oldfulllen = def.fullname.length();
        int oldlen = def.name.length();
        Name fullname;
        if(def.fullname.endsWith(def.name))
            fullname = def.fullname.subName(0, oldfulllen-oldlen).append(name);
        else
            fullname = def.owner.fullname.append(Name.fromString(".")).append(name);


        renameSingleDef(def, name, fullname, null);
        
        ListIterator iter = proxies.listIterator();
        while(iter.hasNext())
            {
            Definition proxy = (Definition)iter.next();
            renameSingleDef(proxy, name, fullname, iter);
            }
        
        }
    }

    static private void renameSingleDef(Definition def, Name name, Name fullname, ListIterator iter)
    {
    Scope scope = def.scope;
    if(scope!=null)
        {
        /* check whether it's still in the scope */
        boolean found = false;
        Definition e = scope.lookup(def.name);
        while(e.scope!=null)
            {
            if(e==def)
                {
                found = true;
                break;
                }
            e = e.next();
            }
        if(!found)
            {
            scope = null;
                /* should I remove proxy from list ? */
            }
        else
            scope.remove(def);
        def.scope=null;
        }
    def.fullname = fullname;
    def.name = name;
    if(scope!=null)
        scope.enter(def);
    }

    /**
     * Get the current face of the definition.
     *
     * @result TwoFaced.SPECIAL or TwoFaced.STANDARD
     */
    public boolean getFace()
    {
    return face;
    }

    /**
     * Switch faces, if necessary.
     *
     * @param face the face we want to go to
     */
    public void setFace(boolean _face)
    {
    if(face==_face)
        return;

    if(has_alttype)
        {
        Type type = alttype;
        alttype = def.type;
        def.type = type;
        }

    if(has_altname)
        {
        Name name = altname;
        altname = def.name;
        changeName(name);
        }

    face=_face;
    }

    /**
     * Get the standard or special name.
     *
     * @param face STANDARD or SPECIAL
     */
    public Name getName(boolean _face)
    {
    if(face==_face || !has_altname)
        return def.name;
    else
        return altname;
    }

    /**
     * Get the standard or special type.
     *
     * @param face STANDARD or SPECIAL
     */
    public Type getType(boolean _face)
    {
    if(face==_face || !has_alttype)
        return def.type;
    else
        return alttype;
    }

    /**
     * Set the standard or special name.
     *
     * This function sets only one of the two names. Its effect
     * should be the same regardless of the current face.
     *
     * @param name new name
     * @param face STANDARD or SPECIAL
     */
    public void setName(Name name, boolean _face)
    {
    if(face==_face)
        {
        /* work on definition */
        if(!name.equals(def.name))
            {
            if(!has_altname)
                {
                /* save the old name in the other face */
                has_altname = true;
                altname = def.name;
                register();
                }
            changeName(name);
            }
        }
    else 
        {
        /* work on alternate definition */

        has_altname = !name.equals(def.name);
        if(has_altname)
            {
            altname = name;
            register();
            }
        }
    }

    /**
     * Set the standard or special type.
     *
     * This function sets only one of the two type. Its effect
     * should be the same regardless of the current face.
     *
     * @param type new type
     * @param face STANDARD or SPECIAL
     */
    public void setType(Type type, boolean _face)
    {
    if(face==_face)
        {
        /* work on definition */
        if(type!=def.type)
            {
            if(!has_alttype)
                {
                /* save the old type in the other face */
                has_alttype = true;
                alttype = def.type;
                register();
                }
            def.type = type;
            }
        }
    else 
        {
        /* work on alternate definition */

        has_alttype = type!=def.type;
        if(has_alttype)
            {
            alttype = type;
            register();
            }
        }
    }
}

/**
 * Common interface for two-faced definitions.
 */
public interface TwoFacedDefinition
{
    public void dump_ident(java.io.PrintStream w, int level);
    public void dump(java.io.PrintStream w, int level);

    /**
     * Tell the two-faced definition to register itself
     * the next time the special definition is modified.
     */
    public void init(TwoFacedRegistery registery);

    /**
     * Switch to standard representation, if it's not the current one.
     */
    public Definition standard();

    /**
     * Switch to special representation, if it's not the current one.
     */
    public Definition special();

    /**
     * Return true if the current representation is special
     */
    public boolean isSpecial();

    /**
     * Return true if there exists a special representation
     */
    public boolean hasSpecial();

    /**
     * Get the special name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getSpecialName();

    /**
     * Get the standard name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getStandardName();

    /**
     * Set the special name.
     *
     * @param name the new special name
     */
    public void setSpecialName(Name name);

    /**
     * Set the standard name.
     *
     * @param name the new standard name
     */
    public void setStandardName(Name name);

    /**
     * Get the special type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getSpecialType();

    /**
     * Get the standard type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getStandardType();

    /**
     * Set the special type.
     *
     * @param type the new special type
     */
    public void setSpecialType(Type type);

    /**
     * Set the standard type.
     *
     * @param type the new standard type
     */
    public void setStandardType(Type type);
}

/**
 * Two-faced registery.
 *
 * It can be used to keep track of modified two-faced definitions; 
 * when a two-faced definition is switched to special representation
 * or when the special representation is modified, the two-faced definition
 * registers itself.
 */
public class TwoFacedRegistery
{
    protected Set set = new HashSet();

    /**
     * For the definitions to register themselves.
     */
    public void register(TwoFacedDefinition def)
    {
    set.add(def);
    }

    /**
     * Switch all registered definitions to standard representation.
     */
    public void standard()
    {
    Iterator iter = set.iterator();
    while(iter.hasNext())
        {
        TwoFacedDefinition def = (TwoFacedDefinition)iter.next();
        def.standard();
        }
    }

    /**
     * Switch all registered definitions to special representation.
     */
    public void special()
    {
    Iterator iter = set.iterator();
    while(iter.hasNext())
        {
        TwoFacedDefinition def = (TwoFacedDefinition)iter.next();
        def.special();
        }
    }

    /**
     * Get an iterator
     */
    public Iterator iterator()
    {
    return set.iterator();
    }
}

/**
 * Two-faced method definition.
 *
 * It supports alternate definition for name & type.
 * 
 */
public class TwoFacedMethodDef extends Definition.MethodDef implements TwoFacedConst, TwoFacedDefinition
{
    public void dump_ident(java.io.PrintStream w, int level)
    {
    twofaced.dump_ident(w, level);
    }
    public void dump(java.io.PrintStream w, int level)
    {
    twofaced.dump(w, level);
    }

    protected TwoFacedCommon twofaced;

    public TwoFacedMethodDef(Code code)
    {
        super(code);
        twofaced = new TwoFacedCommon(this);
    }

    /**
     * Tell the two-faced definition to register itself
     * the next time the special definition is modified.
     */
    public void init(TwoFacedRegistery _registery)
    {
        twofaced.setRegistery(_registery);
    }


    /**
     * Switch to standard representation, if it's not the current one.
     */
    public Definition standard()
    {
        twofaced.setFace(STANDARD);
        return this;
    }

    /**
     * Switch to special representation, if it's not the current one.
     */
    public Definition special()
    {
        twofaced.setFace(SPECIAL);
        return this; 
    }

    /**
     * Return true if the current representation is special
     */
    public boolean isSpecial()
    {
        return twofaced.getFace()==SPECIAL;
    }

    /**
     * Return true if there exists a special representation,
     * different from the standard one.
     */
    public boolean hasSpecial()
    {
        return twofaced.hasAlternate();
    }

    /**
     * Get the special name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getSpecialName()
    {
    return twofaced.getName(SPECIAL);
    }

    /**
     * Get the standard name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getStandardName()
    {
    return twofaced.getName(STANDARD);
    }

    /**
     * Set the special name.
     *
     * @param name the new special name
     */
    public void setSpecialName(Name name)
    {
    twofaced.setName(name, SPECIAL);
    }

    /**
     * Set the standard name.
     *
     * @param name the new standard name
     */
    public void setStandardName(Name name)
    {
    twofaced.setName(name, STANDARD);
    }

    /**
     * Get the special type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getSpecialType()
    {
    return twofaced.getType(SPECIAL);
    }


    /**
     * Get the standard type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getStandardType()
    {
    return twofaced.getType(STANDARD);
    }

    /**
     * Set the special type.
     *
     * @param type the new special type
     */
    public void setSpecialType(Type type)
    {
    twofaced.setType(type, SPECIAL);
    }

    /**
     * Set the standard type.
     *
     * @param type the new standard type
     */
    public void setStandardType(Type type)
    {
    twofaced.setType(type, STANDARD);
    }
}

/**
 * Two-faced variable definition.
 *
 * It supports alternate definition for name & type.
 * 
 */
public class TwoFacedVarDef extends Definition.VarDef implements TwoFacedConst, TwoFacedDefinition
{
    public void dump_ident(java.io.PrintStream w, int level)
    {
    twofaced.dump_ident(w, level);
    }
    public void dump(java.io.PrintStream w, int level)
    {
    twofaced.dump(w, level);
    }

    protected TwoFacedCommon twofaced;

    public TwoFacedVarDef(ContextEnv i, int a, int p)
    {
        super(i, a, p);
        twofaced = new TwoFacedCommon(this);
    }

    /**
     * Tell the two-faced definition to register itself
     * the next time the special definition is modified.
     */
    public void init(TwoFacedRegistery _registery)
    {
        twofaced.setRegistery(_registery);
    }


    /**
     * Switch to standard representation, if it's not the current one.
     */
    public Definition standard()
    {
        twofaced.setFace(STANDARD);
        return this;
    }

    /**
     * Switch to special representation, if it's not the current one.
     */
    public Definition special()
    {
        twofaced.setFace(SPECIAL);
        return this; 
    }

    /**
     * Return true if the current representation is special
     */
    public boolean isSpecial()
    {
        return twofaced.getFace()==SPECIAL;
    }

    /**
     * Return true if there exists a special representation,
     * different from the standard one.
     */
    public boolean hasSpecial()
    {
        return twofaced.hasAlternate();
    }

    /**
     * Get the special name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getSpecialName()
    {
    return twofaced.getName(SPECIAL);
    }

    /**
     * Get the standard name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getStandardName()
    {
    return twofaced.getName(STANDARD);
    }

    /**
     * Set the special name.
     *
     * @param name the new special name
     */
    public void setSpecialName(Name name)
    {
    twofaced.setName(name, SPECIAL);
    }

    /**
     * Set the standard name.
     *
     * @param name the new standard name
     */
    public void setStandardName(Name name)
    {
    twofaced.setName(name, STANDARD);
    }

    /**
     * Get the special type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getSpecialType()
    {
    return twofaced.getType(SPECIAL);
    }


    /**
     * Get the standard type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getStandardType()
    {
    return twofaced.getType(STANDARD);
    }

    /**
     * Set the special type.
     *
     * @param type the new special type
     */
    public void setSpecialType(Type type)
    {
    twofaced.setType(type, SPECIAL);
    }

    /**
     * Set the standard type.
     *
     * @param type the new standard type
     */
    public void setStandardType(Type type)
    {
    twofaced.setType(type, STANDARD);
    }
}


/**
 * Two-faced class definition.
 *
 * It supports alternate definition for name & type.
 * 
 */
public class TwoFacedClassDef extends Definition.ClassDef implements TwoFacedConst, TwoFacedDefinition
{
    public void dump_ident(java.io.PrintStream w, int level)
    {
    twofaced.dump_ident(w, level);
    }
    public void dump(java.io.PrintStream w, int level)
    {
    twofaced.dump(w, level);
    }

    protected TwoFacedCommon twofaced;

    public TwoFacedClassDef(Type sc, Type[] is, Scope scope, Pool pl, Name source)
    {
        super(sc, is, scope, pl, source);
        twofaced = new TwoFacedCommon(this);
    }

    /**
     * Tell the two-faced definition to register itself
     * the next time the special definition is modified.
     */
    public void init(TwoFacedRegistery _registery)
    {
        twofaced.setRegistery(_registery);
    }


    /**
     * Switch to standard representation, if it's not the current one.
     */
    public Definition standard()
    {
        twofaced.setFace(STANDARD);
        return this;
    }

    /**
     * Switch to special representation, if it's not the current one.
     */
    public Definition special()
    {
        twofaced.setFace(SPECIAL);
        return this; 
    }

    /**
     * Return true if the current representation is special
     */
    public boolean isSpecial()
    {
        return twofaced.getFace()==SPECIAL;
    }

    /**
     * Return true if there exists a special representation,
     * different from the standard one.
     */
    public boolean hasSpecial()
    {
        return twofaced.hasAlternate();
    }

    /**
     * Get the special name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getSpecialName()
    {
    return twofaced.getName(SPECIAL);
    }

    /**
     * Get the standard name.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.name is accessed directly.
     */
    public Name getStandardName()
    {
    return twofaced.getName(STANDARD);
    }

    /**
     * Set the special name.
     *
     * @param name the new special name
     */
    public void setSpecialName(Name name)
    {
    twofaced.setName(name, SPECIAL);
    }

    /**
     * Set the standard name.
     *
     * @param name the new standard name
     */
    public void setStandardName(Name name)
    {
    twofaced.setName(name, STANDARD);
    }

    /**
     * Get the special type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getSpecialType()
    {
    return twofaced.getType(SPECIAL);
    }


    /**
     * Get the standard type.
     *
     * The result is independent of the current representation,
     * which isn't the case if def.type is accessed directly.
     */
    public Type getStandardType()
    {
    return twofaced.getType(STANDARD);
    }

    /**
     * Set the special type.
     *
     * @param type the new special type
     */
    public void setSpecialType(Type type)
    {
    twofaced.setType(type, SPECIAL);
    }

    /**
     * Set the standard type.
     *
     * @param type the new standard type
     */
    public void setStandardType(Type type)
    {
    twofaced.setType(type, STANDARD);
    }
}



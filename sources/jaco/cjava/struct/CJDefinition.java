package jaco.cjava.struct;

import java.io.*;
import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.cjava.component.*;


public class CJDefinition extends Definition
{
    /**
     * Interface for CJDefinitionFactory
     */
    public static interface Factory extends Definition.Factory
    {
    /**
     * Create and initialize a compound definition.
     *
     * @param name the name of the compound definition (mostly ignored)
     * @param type the type; its definition will be set to the new compounddef, it type is non-null
     * @param superclass the class that is part of the compound, or null
     * @param interfaces the list of interfaces that are part of the compound
     * @param owner the owner of the definition (class, package or method)
     */
    Definition CompoundDef(Name name, CJType.CompoundType type, Type superclass, Type[] interfaces, Definition owner);
    }


    /** CompoundDef 
     *
     * @param superclass the class that is part of the compound, or null
     * @param interfaces the list of interfaces that are part of the compound
     */
    public case CompoundDef(Type superclass, Type[] interfaces);

    /**
     * Return the local scope.
     *
     * For compounds, create a fake empty scope if necessary.
     *
     * @result a scope
     */
    public Scope locals()
    {
    switch(this)
        {
        case CompoundDef(Type superclass, _):
        if(scope==null)
            scope=new Scope(null, this);
        return scope;

        default:
        return super.locals();
        }
    }

    /**
     * Return the super type.
     *
     * For compounds, the supertype means the class that is
     * part of the compound, if there are any. null if there
     * are no classes in the compound.
     *
     * @see CJDefinition#interfaces
     */
    public Type supertype()
    {
    switch(this)
        {
        case CompoundDef(Type superclass, _):
        return superclass;

        default:
        return super.supertype();
        }
    }

    /**
     * Return the list of interfaces.
     *
     * For compounds, it means returning all the interfaces
     * that are part of the compound. If there's a class in
     * the compound, it is returned by supertype().
     *
     * @result a list of interfaces
     * @see CJDefinition#supertype
     */
    public Type[] interfaces()
    {
    switch(this)
    {
        case CompoundDef(_, Type[] interfaces):
        return interfaces;

        default:
        return super.interfaces();
    }
    }

}

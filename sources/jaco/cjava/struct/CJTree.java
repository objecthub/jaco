package jaco.cjava.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Definition.*;


/** abstract syntax trees for cjava
 */
public class CJTree extends Tree
{

    /**
     * Compound type
     * @param an array of type trees (interface or class; identifiers)
     */
    public case CompoundType(Tree[] types);

    /**
     * Alias declaration.
     *
     * @param name the name of the alias
     * @param mods modifiers (public, private ...)
     * @param type the type this alias is set to
     * @param def the Definition created for the alias
     */
    public case AliasDecl(Name name, int mods, Tree realtype, Definition def);
    
    public static interface Factory extends Tree.Factory
    {
    CJTree CompoundType(Tree[] types);
    CJTree AliasDecl(Name name, int mods, Tree type);
    }

    public Tree setDef(Definition def)
    {
    switch (this)
        {
        case AliasDecl(_, _, _, _):
        ((AliasDecl)this).def = def;
        if ((def != null) && (type == null))
            type = def.type;
        return this;
        
        default:
        return super.setDef(def);
        }
    }

    public Definition def()
    {
    switch (this)
        {
        case AliasDecl(_, _, _, Definition def):
        return def;
        
        default:
        return super.def();
        }
    }
    
    public int mods()
    {
    switch (this)
        {
        case AliasDecl(_, int mods, _, _):
        return mods;
        default:
        return super.mods();
        }
    }
}

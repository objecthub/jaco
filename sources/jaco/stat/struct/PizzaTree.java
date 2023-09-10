//      /   _ _      JaCo
//  \  //\ / / \     - pizza abstract syntax tree nodes
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Definition.*;


/** abstract syntax trees for Pizza
 */
public class PizzaTree extends Tree
{
    public case CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits, ClassDef def);
    
    
    public Tree setDef(Definition def)
    {
        switch (this)
        {
            case CaseDecl(_, _, _, _, _):
                ((CaseDecl)this).def = (ClassDef)def;
                break;
        }
        return this;
    }
    
    public Definition def()
    {
        switch (this)
        {
            case CaseDecl(_, _, _, _, ClassDef def):
                return def;
            
            default:
                return null;
        }
    }
    
    public int mods()
    {
        switch (this)
        {
            case CaseDecl(_, int mods, _, _, _):
                return mods;
            
            default:
                return 0;
        }
    }
    
    
    public static interface Factory extends Tree.Factory
    {
        PizzaTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits);
    }
}

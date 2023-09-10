package jaco.cjava.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.cjava.struct.*;
import jaco.java.struct.*;
/**
 * Extensions of Trees that can create CJTree
 */
public class CJTrees extends Trees
{

    public String getName()
    {
    return "CJTrees";
    }
    
    public void init(MainContext context)
    {
    super.init(context);
    make = new CJTreeFactory();
    newdef = new CJTreeCreate();
    redef = new CJTreeRedef();
    }

    public Tree toTree(Type t)
    {
        switch ((CJType)t)
        {
        case CompoundType(Type[] tl):
            return ((CJTree.Factory)newdef).CompoundType(toTree(tl)).setType(t);
        default:
            return super.toTree(t);
        }
    }

}

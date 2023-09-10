//      /   _ _      JaCo
//  \  //\ / / \     - factory for abstract syntax tree nodes
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;
import PizzaTree.*;
import Definition.*;


public class PizzaTreeFactory extends TreeFactory implements PizzaTree.Factory
{
    public PizzaTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits)
    {
        return new CaseDecl(name, mods, fields, inits, null);
    }
}

public class PizzaTreeCreate extends TreeCreate implements PizzaTree.Factory
{
    public PizzaTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits)
    {
        CaseDecl    t = new CaseDecl(name, mods, fields, inits, null);
        t.pos = protoPos;
        return t;
    }
}

public class PizzaTreeRedef extends TreeRedef implements PizzaTree.Factory
{
    public PizzaTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits)
    {
        CaseDecl    t = (CaseDecl)protoTree;
        t = new CaseDecl(name, mods, fields, inits, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
}

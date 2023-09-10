package jaco.sjava.component;

import jaco.java.context.*;
import jaco.java.component.*;
import jaco.sjava.struct.*;
import jaco.java.struct.*;
import Definition.*;
import jaco.framework.*;
import SJTree.*;


public class SJTrees extends Trees implements SJModifierConst {
/** component name
 */
    public String getName()
    {
        return "SJJavaTrees";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        make = new SJTreeFactory();
        newdef = new SJTreeCreate();
        redef = new SJTreeRedef();
    }
    
    public Tree MethodDecl(Definition f, Tree[] stats)
    {
        return newdef.MethodDecl(f.name, f.modifiers & ~ACTIVE,
                                toTree(f.type.restype()),
                                Params(f.type.argtypes()),
                                toTree(f.type.thrown()), stats).setDef(f);
    }
    
    public Tree copy(Tree tree) {
        if (tree == null)
            return null;
        PresetReDef pr = at(tree);
        Tree res = null;
        SJTree t = (SJTree)tree;
        
        switch (t) {
            case SelectStat(SelectCase[] cases, boolean b):
                res = ((SJTreeRedef)redef).SelectStat((SelectCase[])copy(cases), b);
                break;
            // DUY 03.10.01 This function is modified in order to apply the ForAll
            /*case SelectCase(Name index, Name maxNumber, Tree when, Tree synchStat, Tree[] stats):
                res = ((SJTreeRedef)redef).SelectCase(copy(when), copy(synchStat), copy(stats));
                break;
             */
            case AcceptStat(Name name, MethodDef[] defs, Tree fromObj, Name forallVal, boolean fromDefined):
                res = ((SJTreeRedef)redef).AcceptStat(name,defs,fromObj,forallVal, fromDefined);
                break;
            case SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats):
                res = ((SJTreeRedef)redef).SelectCase(index, maxNumber, when, syncCase, stats);
                break;
            case WaitUntilStat(Tree expr):
                res = ((SJTreeRedef)redef).WaitUntilStat((SelectCase)copy(expr));
                break;
            default:
                return super.copy(t);
        }
        return pr.make(res);
    }
}

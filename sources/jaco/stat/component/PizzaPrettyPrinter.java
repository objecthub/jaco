//      /   _ _      JaCo
//  \  //\ / / \     - extended pretty printer; supports extensible algebraic types
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.stat.struct.*;
import jaco.java.struct.*;
import PizzaTree.*;
import Tree.*;
import java.io.*;


public class PizzaPrettyPrinter extends PrettyPrinter
{
    public PizzaPrettyPrinter()
    {
        super();
    }
    
    public PizzaPrettyPrinter(PrintWriter out)
    {
        super(out);
    }
    
    public PizzaPrettyPrinter(String suffix)
    {
        super(suffix);
    }
    
    public String getName()
    {
        return "PizzaPrettyPrinter";
    }
    
    public PrettyPrinter printDecl(Tree tree)
    {
        switch ((PizzaTree)tree)
        {
            case CaseDecl(Name name, int mods, VarDecl[] fields,
                          Tree[] inits, _):
                print(modifiers.toString(mods)).print("case ").print(name);
                if (fields != null)
                    print("(").printExprs(fields).print(")");
                if (inits != null)
                    return print(" ").printBlock(inits);
                else
                    return print(";");
            
            default:
                return super.printDecl(tree);
        }
    }
    
    public PrettyPrinter printExpr(Tree tree, int prec)
    {
        switch (tree)
        {
            case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                if (name == null)
                    return print("_");
                break;
        }
        return super.printExpr(tree, prec);
    }
    
    public PrettyPrinter printStat(Tree tree)
    {
        switch (tree)
        {
            case Block(int mods, Tree[] stats):
                if ((mods & PizzaModifierConst.CASEBLOCK) != 0)
                    print("/* case */ ");
                return printBlock(stats);
            
            default:
                return super.printStat(tree);
        }
    }
}

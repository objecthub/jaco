//      /   _ _      JaCo
//  \  //\ / / \     - extended pretty printer; supports extensible algebraic types
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.pizza.struct.*;
import jaco.java.struct.*;
import PizzaTree.*;
import Tree.*;
import java.io.*;


public class PizzaPrettyPrinter extends PrettyPrinter
{
	PizzaTrees trees;

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
    
    public void init(MainContext context)
    {
        super.init(context);
        trees = (PizzaTrees)context.Trees();
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
            
            case ClassDecl(_, _, _, _, _, _):
            case MethodDecl(_, _, _, _, _, _, _):
            case VarDecl(_, _, _, _, _):
                Definition def = tree.def();
                if (def != null) {
                    String meta = null;
                    if (def instanceof MDef)
                        meta = ((MDef)def).metaData;
                    else if (def instanceof VDef)
                        meta = ((VDef)def).metaData;
                    else if (def instanceof CDef)
                        meta = ((CDef)def).metaData;
                    align();
                    if (meta != null) {
                        println("/** @meta " + meta + " */");
                        align();
                    } else {
                    	meta = ((PizzaTrees)trees).getMetaData(tree);
                    	if (meta != null) {
							println("/*** @meta " + meta + " */");
							align();
						}
                    }
                }
                return super.printDecl(tree);

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

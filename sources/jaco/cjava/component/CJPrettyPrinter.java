//  /   _ _      JaCo
//  \  //\ / / \     - pretty printer for abstract syntax trees
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;
import jaco.cjava.struct.*;

/**
 * Extension of the pretty printer to accept new cjava trees
 */
public class CJPrettyPrinter extends PrettyPrinter {

/** constructor
 */
    public CJPrettyPrinter() {
    super();
    }
    
    public CJPrettyPrinter(PrintWriter out) {
    super(out);
    }
    
    public CJPrettyPrinter(String suffix) {
    super(suffix);
    }

/** component name
 */
    public String getName() {
    return "CJPrettyPrinter";
    }

    public PrettyPrinter printDecl(Tree tree) {
    switch ((CJTree)tree) {
    case AliasDecl(Name name, int mods, Tree realtype, _):
        out.print(modifiers.toString(mods));
        out.print("class ");
        out.print(name.toString());
        out.print(" = ");
        printExpr(realtype);
        out.print(";");
        return this;

        default:
        return super.printDecl(tree);
    }
    }

    public PrettyPrinter printStat(Tree tree) {
    switch ((CJTree)tree) {
    case AliasDecl(_, _, _, _):
        return printDecl(tree);
        default:
        return super.printStat(tree);
    }
    }
    
    public PrettyPrinter printExpr(Tree tree, int prec) {
    switch ((CJTree)tree) {
        case CompoundType(Tree[] trees):
        out.print("[");
        for(int i=0; i<trees.length; i++)
            {
            if(i!=0)
                out.print(", ");
            printExpr(trees[i]);
            }
        out.print("]");
        return this;
        default:
        return super.printExpr(tree, prec);
    }
    }
}

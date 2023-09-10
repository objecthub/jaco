//      /   _ _      JaCo
//  \  //\ / / \     - pretty printer for abstract syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;


public class OPPrettyPrinter extends PrettyPrinter {

/** constructor
 */
    public OPPrettyPrinter() {
        super();
    }
    
    public OPPrettyPrinter(PrintWriter out) {
        super(out);
    }
    
    public OPPrettyPrinter(String suffix) {
        super(suffix);
    }

/** component name
 */
    public String getName() {
        return "OPPrettyPrinter";
    }

    public PrettyPrinter printDecl(Tree tree) {
        switch (tree) {

            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):

                    if ( 0 == (mods & OPModifierConst.OPERATOR) ) {
                  return super.printDecl(tree);
                    }
            
                    print(modifiers.toString(mods));
                    printExpr(restype);      
                
                OPOperators oper = (OPOperators)operators;

                    if ( 0 != (mods & OPModifierConst.LEFT_OPERAND) ) {
                  print(" this " + oper.demangleName(name));
                  print(" (").printExprs(params).print(")");              
                } else {
                  print(" (").printExprs(params).print(")");              
                  print(" " + oper.demangleName(name) + " this");             
                }
                
                if (thrown.length > 0)
                    print(" throws ").printExprs(thrown);

                if (stats != null)
                    return print(" ").printBlock(stats);
                else
                    return print(";");


            default:
                return super.printDecl(tree);
        }
    }

    public PrettyPrinter printStat(Tree tree) {
        switch (tree) {
            default:
                return super.printStat(tree);
        }
    }
    
    public PrettyPrinter printExpr(Tree tree, int prec) {
        switch (tree) {
            default:
                return super.printExpr(tree, prec);
        }
    }
}





//      /   _ _      JaCo
//  \  //\ / / \     - pretty printer for abstract syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;


public class WrapperPrettyPrinter extends PrettyPrinter {

/** constructor
 */
    public WrapperPrettyPrinter() {
        super();
    }
    
    public WrapperPrettyPrinter(PrintWriter out) {
        super(out);
    }
    
    public WrapperPrettyPrinter(String suffix) {
        super(suffix);
    }

/** component name
 */
    public String getName() {
        return "WrapperPrettyPrinter";
    }

    public PrettyPrinter printDecl(Tree tree) {
        switch (tree) {
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

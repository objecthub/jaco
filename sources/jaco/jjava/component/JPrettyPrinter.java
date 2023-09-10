package jaco.jjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.jjava.struct.*;
import jaco.java.context.*;
import jaco.jjava.context.*;
import jaco.java.component.*;
import jaco.jjava.component.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;
import java.util.*;


public class JPrettyPrinter extends PrettyPrinter 
{
/** component name
 */

    public JPrettyPrinter(PrintWriter out) {
        super(out);
    }


    public JPrettyPrinter(String suffix) {
        super(suffix);
    }


    public JPrettyPrinter() {
        super();
    }

    public String getName() {
        return "JJavaPrettyPrinter";
    }


        public void init(JMainContext context)
        {
                super.init(context);
        }


        public PrettyPrinter printDecl(Tree tree)
        {
                switch (tree)
                {
                        case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                                                        Tree[] thrown, Tree[] stats, _):
                                print(modifiers.toString(mods));
                                if (name == PredefConst.INIT_N)
                                        print(classname);
                                else
                                if (restype == null)
                                        print(name);
                                else
                                        printExpr(restype).print(" " + name);
                                print("(").printExprs(params).print(")");
                                /*JMethodDecl md = ((JMethodDecl)tree).join;
                                while (md != null) {
                                    print(" & " + md.name + "(").
                                    printExprs(md.params).print(")");
                                    md = md.join;
                                }*/
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
}

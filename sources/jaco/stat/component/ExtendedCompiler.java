//      /   _ _      JaCo
//  \  //\ / / \     - extension of the main compiler processor
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.stat.context.*;


public class ExtendedCompiler extends jaco.java.component.Compiler
{
/** the context
 */
    protected PizzaCompilerContext  context;

/** component name
 */
    public String getName()
    {
        return "PizzaCompiler";
    }

/** component initialization
 */
    public void init(CompilerContext context)
    {
        super.init(context);
        this.context = (PizzaCompilerContext)context;
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        try {
            return treelist.process(context.SyntacticAnalyzer())
                           .process(context.PizzaSemanticAnalyzer());
        } finally {
            Stat.printStatistics();
        }
    }
}

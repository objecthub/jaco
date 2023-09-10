//      /   _ _      JaCo
//  \  //\ / / \     - extension of the main compiler processor
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.pizza.context.*;


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
        return treelist.process(context.SyntacticAnalyzer())
                       .process(context.PizzaSemanticAnalyzer())
                       .process(context.TransEAC())
                       .process(context.SemanticAnalyzer())
                       .process(context.Backend())
                       .process(context.BytecodeOptimizer())
                       .process(context.ClassWriter());
    }
}

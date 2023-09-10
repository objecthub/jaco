//      /   _ _      JaCo
//  \  //\ / / \     - bytecode optimizer
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;


public class BytecodeOptimizer extends Processor
{
/** other components
 */
    
    
/** component name
 */
    public String getName()
    {
        return "BytecodeOptimizer";
    }

/** description of trees processor
 */
    public String getDescription()
    {
        return "optimizing bytecode";
    }

/** component initialization
 */
    public void init(OptimizerContext context)
    {
        super.init(context.compilerContext);
    }
    
/** the tree processor method
 */
    protected Tree process(Tree.CompilationUnit tree) throws AbortCompilation
    {
        return tree;
    }
}

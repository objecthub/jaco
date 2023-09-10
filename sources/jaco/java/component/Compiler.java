//      /   _ _      JaCo
//  \  //\ / / \     - composite processor that represents a complete compiler run
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import java.io.IOException;


public class Compiler extends CompositeProcessor
{
/** the context
 */
    protected CompilerContext   context;
    
/** component name
 */
    public String getName()
    {
        return "JavaCompiler";
    }
    
/** description of trees processor
 */
    public String getDescription()
    {
        return "main compiler";
    }
    
/** component initialization
 */
    public void init(CompilerContext context)
    {
        super.init(context);
        this.context = context;
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        return treelist.process(context.SyntacticAnalyzer())
                       .process(context.SemanticAnalyzer())
                       .process(context.Backend())
                       .process(context.BytecodeOptimizer())
                       .process(context.ClassWriter());
    }
}

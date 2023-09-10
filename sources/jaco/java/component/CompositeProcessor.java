//      /   _ _      JaCo
//  \  //\ / / \     - superclass of composite processor components
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;


public abstract class CompositeProcessor extends DebuggableComponent
                                         implements TreeListProcessor
{
/** the current compiler context
 */
    protected CompilerContext   compilerContext;
    
    
/** init code
 */
    public void init(CompilerContext context)
    {
        super.init(context.mainContext);
        compilerContext = context;
    }
    
/** enter code
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        compilerContext.compositeSteps++;
        report.note("[--" + getDescription() + "--]");
        compilerContext.nestingDepth++;
        debug(Debug.ENTER, treelist);
        return treelist;
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist)  throws AbortCompilation
    {
        debug(Debug.EXIT, treelist);
        compilerContext.nestingDepth--;
        return treelist;
    }
    
/** the default treelist processor
 */
    public abstract TreeList process(TreeList tree) throws AbortCompilation;
}

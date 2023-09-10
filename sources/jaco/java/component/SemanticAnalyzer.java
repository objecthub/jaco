//      /   _ _      JaCo
//  \  //\ / / \     - semantic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;


public class SemanticAnalyzer extends CompositeProcessor
{
/** the context
 */
    protected SemanticContext   context;
    
/** component name
 */
    public String getName()
    {
        return "JavaSemanticAnalyzer";
    }
    
/** description of trees processor
 */
    public String getDescription()
    {
        return "semantic analyzer";
    }
    
/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context.compilerContext);
        this.context = context;
    }
    
/** the debugging name
 */ 
    public String getDebugName()
    {
        return "semantic";
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        return treelist.process(context.EnterClasses())
                       .process(context.ImportClasses())
                       .process(context.EnterMembers())
                       .process(context.Attribute());
    }
}

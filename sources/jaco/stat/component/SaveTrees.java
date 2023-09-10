//      /   _ _      JaCo
//  \  //\ / / \     - writes structure trees to source files
//   \//  \\_\_/     
//         \         Matthias Zenger, 01/05/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.stat.context.*;
import Tree.*;


public class SaveTrees extends jaco.java.component.Processor
{
/** other components
 */
    PrettyPrinter   pretty;
    
/** component name
 */
    public String getName()
    {
        return "JavaSaveTrees";
    }
    
/** description
 */
    public String getDescription()
    {
        return "write generated sources";
    }
    
/** component initialization
 */
    public void init(MainPizzaContext context)
    {
        super.init(context);
        pretty = context.PrettyPrinter();
    }
    
/** the processor routine
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        pretty.outputClasses(tree);
        return tree;
    }
    
/** do we have to process this tree?
 */
    protected boolean needsProcessing(CompilationEnv info)
    {
        return ((PizzaSettings)context.settings).keepgenerated;
    }
}

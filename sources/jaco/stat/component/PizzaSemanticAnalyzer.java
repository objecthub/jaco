//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.stat.context.*;


public class PizzaSemanticAnalyzer extends SemanticAnalyzer
{
    public String getName()
    {
        return "PizzaSemanticAnalyzer";
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        // ((PizzaDefinitions)compilerContext.mainContext.Definitions()).swapScopes();
        return super.exit(treelist);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - the optimizer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.context;

import jaco.framework.*;
import jaco.java.component.*;


public class OptimizerContext extends Context
{
/** enclosing context
 */
    public CompilerContext  compilerContext;

/** tool components of the Java bytecode optimizer
 */
    

/** context constructor
 */
    public OptimizerContext(CompilerContext context)
    {
        super(context);
        compilerContext = context;
    }
}

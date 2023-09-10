//      /   _ _      JaCo
//  \  //\ / / \     - the optimizer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.context;

import jaco.framework.*;
import jaco.wrapper.component.*;


public class WrapperOptimizerContext extends jaco.java.context.OptimizerContext
{
/** context constructor
 */
    public WrapperOptimizerContext(WrapperCompilerContext context) {
        super(context);
    }
}

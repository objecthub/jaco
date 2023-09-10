//      /   _ _      JaCo
//  \  //\ / / \     - the initial compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.context;

import jaco.framework.*;
import jaco.wrapper.component.*;


public class WrapperContext extends jaco.java.context.JavaContext
{
/** context constructor
 */
    public WrapperContext(WrapperSettings settings)
    {
        super(settings);
    }

/** factory method for the compiler of this context
 */
    public jaco.java.component.JavaCompiler JavaCompiler()
    {
        WrapperCompiler compiler = new WrapperCompiler();
        compiler.init(MainContext());
        return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected jaco.java.context.MainContext MainContext()
    {
        if (((WrapperSettings)settings).java)
            return new jaco.java.context.MainContext(this);
        else
            return new WrapperMainContext(this);
    }
}

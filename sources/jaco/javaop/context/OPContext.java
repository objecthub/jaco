//      /   _ _      JaCo
//  \  //\ / / \     - the initial compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.context;

import jaco.framework.*;
import jaco.javaop.component.*;


public class OPContext extends jaco.java.context.JavaContext
{
/** context constructor
 */
    public OPContext(OPSettings settings)
    {
        super(settings);
    }

/** factory method for the compiler of this context
 */
    public jaco.java.component.JavaCompiler JavaCompiler()
    {
        OPCompiler  compiler = new OPCompiler();
        compiler.init(MainContext());
        return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected jaco.java.context.MainContext MainContext()
    {
        if (((OPSettings)settings).java)
            return new jaco.java.context.MainContext(this);
        else
            return new OPMainContext(this);
    }
}

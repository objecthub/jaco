//  /   _ _      JaCo
//  \  //\ / / \     - the initial compiler context
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.context;

import jaco.framework.*;
import jaco.cjava.component.*;


public class CJContext extends jaco.java.context.JavaContext
{
/** context constructor
 */
    public CJContext(CJSettings settings)
    {
    super(settings);
    }

/** factory method for the compiler of this context
 */
    public jaco.java.component.JavaCompiler JavaCompiler()
    {
    CJCompiler  compiler = new CJCompiler();
    compiler.init(MainContext());
    return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected jaco.java.context.MainContext MainContext()
    {
    if (((CJSettings)settings).java)
        return new jaco.java.context.MainContext(this);
    else
        return new CJMainContext(this);
    }
}

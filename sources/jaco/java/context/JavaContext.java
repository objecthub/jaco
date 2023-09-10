//      /   _ _      JaCo
//  \  //\ / / \     - the initial compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;


public class JavaContext extends Context
{
/** context constructor
 */
    public JavaContext(Settings settings)
    {
        super(settings);
    }
    
/** factory method for the compiler of this context
 */
    public JavaCompiler JavaCompiler()
    {
        JavaCompiler    compiler = new JavaCompiler();
        compiler.init(MainContext());
        return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected MainContext MainContext()
    {
        return new MainContext(this);
    }
}

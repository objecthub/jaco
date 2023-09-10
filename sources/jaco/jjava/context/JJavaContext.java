//      /   _ _      JaCo
//  \  //\ / / \     - context for the pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.jjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.jjava.component.*;


public class JJavaContext extends JavaContext
{
    public JJavaContext(Settings settings)
    {
        super(settings);
    }
    
/** factory method for the compiler of this context
 */
/*
    public JavaCompiler JavaCompiler()
    {
        JJavaCompiler   compiler = new JJavaCompiler();
        compiler.init(MainContext());
        return compiler;
    }
*/

/** factory method for the main compilation context
 */
    protected MainContext MainContext()
    {
        return new JMainContext(this);
    }
}

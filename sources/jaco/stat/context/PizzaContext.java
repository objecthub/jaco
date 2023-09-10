//      /   _ _      JaCo
//  \  //\ / / \     - context for the pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.stat.component.*;


public class PizzaContext extends JavaContext
{
    public PizzaContext(Settings settings)
    {
        super(settings);
    }
    
/** factory method for the compiler of this context
 */
    public JavaCompiler JavaCompiler()
    {
        PizzaCompiler   compiler = new PizzaCompiler();
        compiler.init(MainContext());
        return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected MainContext MainContext()
    {
        return new MainPizzaContext(this);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - an ant compiler adaptor for PiCo
//   \//  \\_\_/     
//         \         Matthias Zenger, 13/12/2001

package jaco.pizza;

import jaco.framework.ant.*;
import org.apache.tools.ant.types.Commandline;
import jaco.pizza.component.*;
import jaco.pizza.context.*;
import jaco.framework.*;


/** Ant compiler adapter to use the PiCo Java compiler
 *  for translating .java files to .class files.
 */
public class AntAdaptor extends AntCompilerAdaptor {
	
    public boolean runCompiler(String[] args) {
        PizzaSettings js = new PizzaSettings();
        try {
            js.parse(args);
            return js.JavaContext().JavaCompiler().compile();
        } catch (Throwable e) {
            return false;
        }
    }
    
    public String compilerName() {
        return "PiCo";
    }
}

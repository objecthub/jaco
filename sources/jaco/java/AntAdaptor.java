//      /   _ _      JaCo
//  \  //\ / / \     - an ant compiler adaptor for JaCo
//   \//  \\_\_/     
//         \         Matthias Zenger, 13/12/2001

package jaco.java;

import jaco.framework.ant.*;
import org.apache.tools.ant.types.Commandline;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.framework.*;


/**
 * Ant compiler adapter to use the Jaco Java compiler
 * for translating .java files to .class files.
 */
public class AntAdaptor extends AntCompilerAdaptor {

    public boolean runCompiler(String[] args) {
        JavaSettings js = new JavaSettings();
        try {
            js.parse(args);
            return js.JavaContext().JavaCompiler().compile();
        } catch (Throwable e) {
            return false;
        }
    }
    
    public String compilerName() {
        return "JaCo";
    }
}

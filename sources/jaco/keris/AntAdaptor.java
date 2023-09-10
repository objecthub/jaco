//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    ant compiler adaptor
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris;

import jaco.framework.ant.*;
import org.apache.tools.ant.types.Commandline;
import jaco.keris.component.*;
import jaco.keris.context.*;
import jaco.framework.*;


/** Ant compiler adapter to use the Keris compiler
 *  for translating .java files to .class files.
 */
public class AntAdaptor extends AntCompilerAdaptor {
    public boolean runCompiler(String[] args) {
        XSettings js = new XSettings();
        try {
            js.parse(args);
            return js.JavaContext().JavaCompiler().compile();
        } catch (Throwable e) {
            return false;
        }
    }
    
    public String compilerName() {
        return "Kerisc";
    }
}

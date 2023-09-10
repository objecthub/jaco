//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               initial compiler factory

package jaco.keris.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.keris.component.*;


public class XContext extends JavaContext {
    
    public XContext(XSettings settings) {
        super(settings);
    }
    
/** factory method for the compiler of this context
 */
    public JavaCompiler JavaCompiler() {
        XJavaCompiler compiler = new XJavaCompiler();
        compiler.init(MainContext());
        return compiler;
    }
    
/** factory method for the main compilation context
 */
    protected MainContext MainContext() {
        return new XMainContext(this);
    }
}

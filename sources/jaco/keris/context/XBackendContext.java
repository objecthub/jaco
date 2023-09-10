//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               backend context

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.keris.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class XBackendContext extends BackendContext {

    public XBackendContext(XCompilerContext context) {
        super(context);
    }
    
    public BytecodeGenerator BytecodeGenerator() {
        if (generator == null) {
            generator = new XBytecodeGenerator();
            generator.init(this);
        }
        return generator;
    }
    
    protected SemanticContext SemanticContext() {
        return new JSemanticContext(compilerContext);
    }
}

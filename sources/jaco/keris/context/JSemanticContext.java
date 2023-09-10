//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               semantic analyzer context

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.context.*;
import jaco.java.component.*;


public class JSemanticContext extends SemanticContext {
    
    public JSemanticContext(CompilerContext context) {
        super(context);
    }

    public TypeChecker TypeChecker() {
        if (checks == null) {
            checks = new JTypeChecker();
            checks.init(this);
        }
        return checks;
    }
}

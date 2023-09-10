//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.context;

import jaco.framework.*;
import jaco.pizza.component.*;
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

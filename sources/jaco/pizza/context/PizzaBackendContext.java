//      /   _ _      JaCo
//  \  //\ / / \     - backend context
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.context;

import jaco.framework.*;
import jaco.pizza.component.*;
import jaco.pizza.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class PizzaBackendContext extends BackendContext {

    public PizzaBackendContext(PizzaCompilerContext context) {
        super(context);
    }
    
    public BytecodeGenerator BytecodeGenerator() {
        if (generator == null) {
            generator = new PizzaBytecodeGenerator();
            generator.init(this);
        }
        return generator;
    }
    
    protected SemanticContext SemanticContext() {
        return new JSemanticContext(compilerContext);
    }
}

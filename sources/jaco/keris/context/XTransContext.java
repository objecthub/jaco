//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               semantic analyzer context

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.component.*;


public class XTransContext extends Context {
    
    public XCompilerContext compilerContext;
    
    public XTransContext(XCompilerContext context) {
        super(context);
        compilerContext = context;
    }

    protected AlgebraicSupport algebraic;
    protected Labeller labeller;
    
    public AlgebraicSupport AlgebraicSupport() {
        if (algebraic == null) {
            algebraic = new AlgebraicSupport();
            algebraic.init(this);
        }
        return algebraic;
    }
    
    public Labeller Labeller() {
        if (labeller == null) {
            labeller = new Labeller();
            labeller.init(this);
        }
        return labeller;
    }
}

//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               top-level compiler pass

package jaco.keris.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.keris.component.*;


public class XCompilerContext extends CompilerContext {
    
/** context constructor
 */
    public XCompilerContext(XMainContext context) {
        super(context);
    }
    
    public SemanticAnalyzer SemanticAnalyzer() {
        SemanticAnalyzer attributor = new SilentSemanticAnalyzer();
        attributor.init(SemanticContext());
        return attributor;
    }
    
    public XSemanticAnalyzer KerisSemanticAnalyzer() {
        XSemanticAnalyzer attributor = new XSemanticAnalyzer();
        attributor.init(KerisSemanticContext());
        return attributor;
    }

    public TransModules TransModules() {
        TransModules trans = new TransModules();
        trans.init(TransContext());
        return trans;
    }
    
    public ClassWriter ClassWriter() {
        if (writer == null) {
            writer = new XClassWriter();
            writer.init(this);
        }
        return writer;
    }
    
/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext() {
        return new XSyntacticContext(this);
    }
    
    protected SemanticContext SemanticContext() {
        return new JSemanticContext(this);
    }
    
    protected XSemanticContext KerisSemanticContext() {
        return new XSemanticContext(this);
    }
    
    protected XTransContext TransContext() {
        return new XTransContext(this);
    }
    
    protected BackendContext BackendContext() {
        return new XBackendContext(this);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.context;

import jaco.java.context.*;
import jaco.java.component.*;
import jaco.javaop.component.*;
import jaco.framework.*;


public class OPCompilerContext extends CompilerContext
{
/** context constructor
 */
    public OPCompilerContext(OPMainContext context)
    {
        super(context);
    }

/** factory methods for embedded components
 
    public ClassWriter ClassWriter()
    {
        if (writer == null)
        {
            writer = new ClassWriter();
            writer.init(this);
        }
        return writer;
    }
    
    public SyntacticAnalyzer SyntacticAnalyzer()
    {
        SyntacticAnalyzer   parser = new SyntacticAnalyzer();
        parser.init(SyntacticContext());
        return parser;
    }
    
    public Backend Backend()
    {
        Backend backend = new Backend();
        backend.init(BackendContext());
        return backend;
    }
    
    public BytecodeOptimizer BytecodeOptimizer()
    {
        BytecodeOptimizer   optimizer = new BytecodeOptimizer();
        optimizer.init(OptimizerContext());
        return optimizer;
    }
*/

    public SemanticAnalyzer OPSemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SemanticAnalyzer();
        attributor.init(OPSemanticContext());
        return attributor;
    }

    public TransOP TransOP()
    {
        TransOP trans = new TransOP();
        trans.init( this );
        return trans;
    }


/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
        return new OPSyntacticContext(this);
    }
    
    protected SemanticContext SemanticContext()
    {
        return new SemanticContext(this);
    }
    
    protected OPSemanticContext OPSemanticContext()
    {
        return new OPSemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
        return new OPBackendContext(this);
    }
    
    protected OptimizerContext OptimizerContext()
    {
        return new OPOptimizerContext(this);
    }
}

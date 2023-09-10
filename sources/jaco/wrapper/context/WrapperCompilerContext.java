//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.context;

import jaco.java.context.*;
import jaco.java.component.*;
import jaco.wrapper.component.*;
import jaco.framework.*;


public class WrapperCompilerContext extends CompilerContext
{
/** context constructor
 */
    public WrapperCompilerContext(WrapperMainContext context)
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

    public SemanticAnalyzer WrapperSemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SemanticAnalyzer();
        attributor.init(WrapperSemanticContext());
        return attributor;
    }

    public TransWrapper TransWrapper()
    {
        TransWrapper trans = new TransWrapper();
        trans.init(this);
        return trans;
    }


/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
        return new WrapperSyntacticContext(this);
    }
    
    protected SemanticContext SemanticContext()
    {
        return new SemanticContext(this);
    }
    
    protected WrapperSemanticContext WrapperSemanticContext()
    {
        return new WrapperSemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
        return new WrapperBackendContext(this);
    }
    
    protected OptimizerContext OptimizerContext()
    {
        return new WrapperOptimizerContext(this);
    }
}

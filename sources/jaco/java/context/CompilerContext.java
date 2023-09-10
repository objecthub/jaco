//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;


public class CompilerContext extends Context
{
/** enclosing context
 */
    public MainContext          mainContext;
    
/** singleton components
 */
    protected ClassWriter       writer;

/** global data (for statistics)
 */
    public int                  nestingDepth;
    public int                  compositeSteps;
    public int                  totalSteps;
    
    
/** context constructor
 */
    public CompilerContext(MainContext context)
    {
        super(context);
        mainContext = context;
    }
    
    
/** factory methods for embedded components
 */
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
    
    public SemanticAnalyzer SemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SemanticAnalyzer();
        attributor.init(SemanticContext());
        return attributor;
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

    
/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
        return new SyntacticContext(this);
    }
    
    protected SemanticContext SemanticContext()
    {
        return new SemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
        return new BackendContext(this);
    }
    
    protected OptimizerContext OptimizerContext()
    {
        return new OptimizerContext(this);
    }
}

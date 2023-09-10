//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.context;

import jaco.java.context.*;
import jaco.java.component.*;
import jaco.sjava.component.*;
import jaco.framework.*;


public class SJCompilerContext extends CompilerContext
{
/** context constructor
 */
    public SJCompilerContext(SJMainContext context)
    {
        super(context);
    }

/** factory methods for embedded components
 */
    public ClassWriter ClassWriter()
    {
        if (writer == null)
        {
            writer = new SJClassWriter();
            writer.init(this);
        }
        return writer;
    }
    
    public SyntacticAnalyzer SyntacticAnalyzer()
    {
        SyntacticAnalyzer   parser = new SyntacticAnalyzer();
        parser.init(SJSyntacticContext());
        return parser;
    }
    
    public SemanticAnalyzer SJSemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SemanticAnalyzer();
        attributor.init(SJSemanticContext());
        return attributor;
    }

    public TransSJ TransSJ()
    {
        TransSJ trans = new TransSJ();
        trans.init(this);
        return trans;
    }

    /** factory methods for contexts
     */
     
    protected SJSyntacticContext SJSyntacticContext()
    {
        return new SJSyntacticContext(this);
    }
    
    protected SJSemanticContext SJSemanticContext()
    {
        return new SJSemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
        return new SJBackendContext(this);
    }
    
    protected OptimizerContext OptimizerContext()
    {
        return new SJOptimizerContext(this);
    }
}

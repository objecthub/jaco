//  /   _ _      JaCo
//  \  //\ / / \     - the main compiler context
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.context;

import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.component.*;
import jaco.framework.*;


public class CJCompilerContext extends CompilerContext
{

    CJCompounds cjcompounds;
    
/** context constructor
 */
    public CJCompilerContext(CJMainContext context)
    {
    super(context);
    }

    public CJCompounds CJCompounds()
    {
    if(cjcompounds==null)
        {
        cjcompounds = new CJCompounds();
        cjcompounds.init(this);
        }
    return cjcompounds;
    }


    public ClassWriter ClassWriter()
    {
    if (writer == null)
    {
        writer = new CJClassWriter();
        writer.init(this);
    }
    return writer;
    }

    public SemanticAnalyzer CJSemanticAnalyzer()
    {
    SemanticAnalyzer attributor = new SemanticAnalyzer();
    attributor.init(CJSemanticContext());
    return attributor;
    }

    public TransCJ TransCJ()
    {
    TransCJ trans = new TransCJ();
    trans.init(this);
    return trans;
    }


/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
    return new CJSyntacticContext(this);
    }
    
    protected CJSemanticContext CJSemanticContext()
    {
    return new CJSemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
    return new CJBackendContext(this);
    }
    
    protected OptimizerContext OptimizerContext()
    {
    return new CJOptimizerContext(this);
    }
}

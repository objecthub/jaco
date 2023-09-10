//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.stat.component.*;


public class PizzaCompilerContext extends CompilerContext
{
/** context constructor
 */
    public PizzaCompilerContext(MainPizzaContext context)
    {
        super(context);
    }
    
/** factory methods for embedded components
 */
    public ClassWriter ClassWriter()
    {
        if (writer == null)
        {
            writer = new PizzaClassWriter();
            writer.init(this);
        }
        return writer;
    }
    
    public SemanticAnalyzer SemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SilentSemanticAnalyzer();
        attributor.init(SemanticContext());
        return attributor;
    }
    
    public PizzaSemanticAnalyzer PizzaSemanticAnalyzer()
    {
        PizzaSemanticAnalyzer   attributor = new PizzaSemanticAnalyzer();
        attributor.init(PizzaSemanticContext());
        return attributor;
    }
    
    public TransEAC TransEAC()
    {
        TransEAC    trans = new TransEAC();
        trans.init(this);
        return trans;
    }
    
/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
        return new PizzaSyntacticContext(this);
    }
    
    protected PizzaSemanticContext PizzaSemanticContext()
    {
        return new PizzaSemanticContext(this);
    }
}

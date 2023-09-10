package jaco.join.component;

import jaco.framework.*;
import jaco.join.context.*;


public class JoinCompiler extends Component
{
/** the context
 */
    protected JoinCompilerContext   context;
    
/** component name
 */
    public String getName()
    {
        return "JoinCompiler";
    }
    
/** description of trees processor
 */
    public String getDescription()
    {
        return "main join compiler";
    }
    
/** component initialization
 */
    public void init(JoinCompilerContext context)
    {
        super.init(context);
        this.context = context;
    }
    
}

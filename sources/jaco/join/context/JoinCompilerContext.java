package jaco.join.context;

import jaco.framework.*;


public class JoinCompilerContext extends Context
{
/** enclosing context
 */
    public JoinSyntacticContext joinContext;
    
/** context constructor
 */
    public JoinCompilerContext(JoinSyntacticContext context)
    {
        super(context.settings);
        joinContext = context;
    }
    
    /* 
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
    */

/** factory methods for contexts
 */

/*
    protected SemanticContext SemanticContext()
    {
        return new SemanticContext(this);
    }
    
    protected BackendContext BackendContext()
    {
        return new BackendContext(this);
    }
    */
}

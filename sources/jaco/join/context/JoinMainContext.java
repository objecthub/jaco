package jaco.join.context;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.join.component.*;


public class JoinMainContext extends MainContext
{
    public JoinMainContext(JoinContext context)
    {
        super(context);
    }
    
    protected CompilerContext CompilerContext()
    {
        return new JoinJavaCompilerContext(this);
    }
}

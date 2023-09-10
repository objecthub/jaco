package jaco.join.context;

import jaco.framework.*;
import jaco.join.component.*;
import jaco.java.context.*;


public class JoinJavaCompilerContext extends CompilerContext
{
    public JoinJavaCompilerContext(JoinMainContext context)
    {
        super(context);
    }
    
    public jaco.java.component.SyntacticAnalyzer SyntacticAnalyzer()
    {
        JoinSyntacticAnalyzer   parser = new JoinSyntacticAnalyzer();
        parser.init((JoinSyntacticContext)SyntacticContext());
        return parser;
    }
    
    protected SyntacticContext SyntacticContext()
    {
        return new JoinSyntacticContext(this);
    }
}

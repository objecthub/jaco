package jaco.join.context;

import jaco.framework.*;
import jaco.join.component.*;
import jaco.java.context.*;


public class JoinContext extends JavaContext
{
    public JoinContext(Settings settings)
    {
        super(settings);
    }
    
    public jaco.java.component.JavaCompiler JavaCompiler() {
        JoinJavaCompiler    compiler = new JoinJavaCompiler();
        compiler.init(JoinMainContext());
        return compiler;
    }
    
    protected JoinMainContext JoinMainContext()
    {
        return new JoinMainContext(this);
    }
}

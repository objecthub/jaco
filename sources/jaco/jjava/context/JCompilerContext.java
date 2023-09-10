// JCompilerContext.java
package jaco.jjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.jjava.component.*;


public class JCompilerContext extends CompilerContext
{
/** context constructor
 */
    public JCompilerContext(JMainContext context)
    {
        super(context);
    }

/*
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
*/
    public JTrans0 JTrans0()
    {
        JTrans0 trans = new JTrans0();
        trans.init(this);
        return trans;
    }


        public JTrans1 JTrans1()
        {
                JTrans1 trans = new JTrans1();
                trans.init(this);
                return trans;
        }

    public SemanticAnalyzer JSemanticAnalyzer()
    {
        SemanticAnalyzer    attributor = new SemanticAnalyzer();
        attributor.init(JSemanticContext());
        return attributor;
    }

    
/** factory methods for contexts
 */
    protected SyntacticContext SyntacticContext()
    {
        return new JSyntacticContext(this);
    }

    protected JSemanticContext JSemanticContext()
    {
        return new JSemanticContext(this);
    }
}

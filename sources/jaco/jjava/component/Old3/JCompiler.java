// JCompiler
// the compiler for the join java

package jaco.jjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.jjava.context.*;


public class JCompiler extends jaco.java.component.Compiler
{
/** the context
 */
    protected JCompilerContext  context;

    protected JJavaSettings settings; 

/** component name
 */
    public String getName()
    {
        return "JCompiler";
    }

/** component initialization
 */
    public void init(CompilerContext context)
    {
        super.init(context);
        this.context = (JCompilerContext)context;
        settings = (JJavaSettings)context.settings;
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        if (settings.translation == 0) {
                    return treelist.process(context.SyntacticAnalyzer())
                                           .process(context.JSemanticAnalyzer())
                                           //.process(context.JTrans0())
                                           .process(context.SemanticAnalyzer())
                                           .process(context.Backend())
                                           .process(context.BytecodeOptimizer())
                                           .process(context.ClassWriter());
        } else
        if (settings.translation == 1) {
            return treelist.process(context.SyntacticAnalyzer())
                       .process(context.JSemanticAnalyzer())
                       .process(context.JTrans1())
                       .process(context.SemanticAnalyzer())
                       .process(context.Backend())
                       .process(context.BytecodeOptimizer())
                       .process(context.ClassWriter());
        }
        else {
            System.out.println("invalid translation");
            return null;    
        }
    }
}

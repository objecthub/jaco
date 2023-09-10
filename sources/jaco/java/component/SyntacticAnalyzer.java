//      /   _ _      JaCo
//  \  //\ / / \     - syntactic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.java.grammar.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.framework.*;
import jaco.framework.parser.*;


public class SyntacticAnalyzer extends Processor
{
/** other components
 */
    protected Trees             trees;
    
/** the current context
 */
    protected SyntacticContext  context;

    
/** component name
 */
    public String getName()
    {
        return "JavaSyntacticAnalyzer";
    }

/** description of tree processor
 */
    public String getDescription()
    {
        return "syntactic analyzer";
    }

/** component initialization
 */
    public void init(SyntacticContext context)
    {
        super.init(context.compilerContext);
        this.context = context;
        trees = context.compilerContext.mainContext.Trees();
    }
    
/** the debugging name
 */ 
    public String getDebugName()
    {
        return "parser";
    }

/** the tree processor
 */
    protected Tree process(Tree.CompilationUnit tree) throws AbortCompilation
    {
        if (tree.decls != null)
            return tree;
        else
        {
            long            msec   = System.currentTimeMillis();
            Scanner         scanner = context.Scanner(tree.info.source);
            LRParser        parser = context.Parser(scanner);
            try
            {
                Symbol  result;
                if (context.debug.debugSwitchSet(getDebugName(), 4))
                    result = parser.debug_parse();
                else
                    result = parser.parse();
                if (result.value instanceof Tree)
                    return (Tree)result.value;
                else
                    return trees.errorTree;
            }
            catch (Exception e)
            {
                //e.printStackTrace();//DEBUG
                String msg = e.toString();
                if (!msg.startsWith("java."))
                    report.error(Position.NOPOS, e.toString());
                return trees.errorTree;
            }
            finally
            {
                report.operation("parsed " + tree.info.source, msec);
            }
        }
    }
}

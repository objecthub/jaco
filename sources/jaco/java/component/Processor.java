//      /   _ _      JaCo
//  \  //\ / / \     - superclass of primitive processors
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;


public abstract class Processor extends DebuggableComponent
                                implements TreeProcessor
{
/** the current compiler context
 */
    protected CompilerContext   compilerContext;
    
    
/** init code
 */
    public void init(CompilerContext context)
    {
        super.init(context.mainContext);
        compilerContext = context;
    }
    
/** enter code
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        report.note("[" + getDescription() + "]");
        compilerContext.totalSteps++;
        debug(Debug.ENTER, treelist);
        return treelist;
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist)  throws AbortCompilation
    {
        debug(Debug.EXIT, treelist);
        return treelist;
    }
    
/** the default tree processor
 */
    public Tree process(Tree tree) throws AbortCompilation
    {
        switch (tree)
        {
            case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                if (needsProcessing(info))
                {
                    report.useEnv(info);
                    try
                    {
                        tree = process((CompilationUnit)tree);
                        report.usePreviousEnv();
                    }
                    catch (AbortCompilation e)
                    {
                        report.usePreviousEnv();
                        if (!catchException(e))
                            throw e;
                    }
                }
                return tree;
            
            case Bad():
                return tree;
            
            default:
                throw new InternalError();
        }
    }

/** catch this abort exception?
 */
    protected boolean catchException(AbortCompilation e)
    {
        return false;
    }
    
/** do we have to process this tree?
 */
    protected boolean needsProcessing(CompilationEnv info)
    {
        return (info.errors == 0);
    }
    
/** the abstract tree processor for compilation units
 */
    protected abstract Tree process(CompilationUnit tree) throws AbortCompilation;
}

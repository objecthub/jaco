//      /   _ _      JaCo
//  \  //\ / / \     - composite processor to generate bytecode
//   \//  \\_\_/     
//         \         Matthias Zenger, 16/10/00

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;


public class Backend extends CompositeProcessor
{
/** the current context
 */
    BackendContext      context;
    
    
/** component name
 */
    public String getName()
    {
        return "JavaBackend";
    }
    
/** description of trees processor
 */
    public String getDescription()
    {
        return "translating classes";
    }

/** component initialization
 */
    public void init(BackendContext context)
    {
        super.init(context.compilerContext);
        this.context = context;
    }

/** the debugging name
 */ 
    public String getDebugName()
    {
        return "backend";
    }
    
/** the debug method
 */
    public boolean debug(int debugId, TreeList treelist) throws AbortCompilation
    {
        if (super.debug(debugId))
            switch (debugId)
            {
                case Debug.EXIT:
                    treelist.process(compilerContext.mainContext.Disassembler());
                    return true;
                
                default:
                    return true;
            }
        return false;
    }
    
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        return treelist.process(context.TransInner())
                       .process(context.SemanticAnalyzer())
                       .process(context.BytecodeGenerator());
    }
}

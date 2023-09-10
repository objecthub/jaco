package jaco.jjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.jjava.context.*;
import jaco.jjava.struct.*;
import Tree.*;
import JTree.*;
import Definition.*;


public interface JConst
{
}


public class JTrans0 extends Translator
                      implements JModifierConst, OperatorConst, JConst,
                                 TreeConst, TreeProcessor, JTypeConst
{
/** component name
 */
    public String getName()
    {
        return "JTrans0";
    }
    
/** return descrition of tree processor
 */
    public String getDescription()
    {
        return "translating join semantics to java semantics";
    }
    
/** default getDebugName method; returns an invalid debug name
 */
    public String getDebugName()
    {
        return "jtran0";
    }
    
/** component initialization
 */
    public void init(JCompilerContext context)
    {
        super.init(context);
    }
    
/** enter code
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        //definitions.swapScopes();
        return super.enter(treelist);
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        //mainContext.algebraicClasses = false;
        return super.exit(treelist);
    }
    
    
}

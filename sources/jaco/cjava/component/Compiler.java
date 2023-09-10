//  /   _ _      JaCo
//  \  //\ / / \     - processor that represents a complete compiler run
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.component;

import jaco.framework.*;
import jaco.cjava.context.*;
import jaco.java.struct.TreeList;


public class Compiler extends jaco.java.component.Compiler {

/** component name
 */
    public String getName() {
    return "CJCompiler";
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation {
    CJCompilerContext c = (CJCompilerContext)context;

    treelist = treelist.process(c.SyntacticAnalyzer())
        .process(c.CJSemanticAnalyzer())
        .process(c.TransCJ());
    
    ((CJMainContext)c.mainContext).disableCJ();

    treelist = treelist.process(c.SemanticAnalyzer())
        .process(c.Backend())
        .process(c.BytecodeOptimizer())
        .process(c.ClassWriter());

    return treelist;
    }
}

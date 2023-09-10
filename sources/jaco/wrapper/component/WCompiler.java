//      /   _ _      JaCo
//  \  //\ / / \     - processor that represents a complete compiler run
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.component;

import jaco.framework.*;
import jaco.wrapper.context.*;
import jaco.java.struct.TreeList;


public class Compiler extends jaco.java.component.Compiler {

/** component name
 */
    public String getName() {
        return "WrapperCompiler";
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation {
        WrapperCompilerContext c = (WrapperCompilerContext)context;
        return treelist.process(c.SyntacticAnalyzer())
                       .process(c.WrapperSemanticAnalyzer())
                       .process(c.TransWrapper())
                       .process(c.SemanticAnalyzer())
                       .process(c.Backend())
                       .process(c.BytecodeOptimizer())
                       .process(c.ClassWriter());
    }
}

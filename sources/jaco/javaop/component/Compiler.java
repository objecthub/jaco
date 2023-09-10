//      /   _ _      JaCo
//  \  //\ / / \     - processor that represents a complete compiler run
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.component;

import jaco.framework.*;
import jaco.javaop.context.*;
import jaco.java.struct.TreeList;


public class Compiler extends jaco.java.component.Compiler {

/** component name
 */
    public String getName() {
        return "OPCompiler";
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation {
        OPCompilerContext c = (OPCompilerContext)context;
        return treelist.process(c.SyntacticAnalyzer())
                       .process(c.OPSemanticAnalyzer())
                       .process(c.TransOP())
                       .process(c.SemanticAnalyzer())
                       .process(c.Backend())
                       .process(c.BytecodeOptimizer())
                       .process(c.ClassWriter());
    }
}

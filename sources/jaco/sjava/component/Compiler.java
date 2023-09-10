//      /   _ _      JaCo
//  \  //\ / / \     - processor that represents a complete compiler run
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.sjava.component;

import jaco.framework.*;
import jaco.sjava.context.*;
import jaco.java.struct.TreeList;


public class Compiler extends jaco.java.component.Compiler {

/** component name
 */
	public String getName() {
		return "SJCompiler";
	}
	
/** the tree processor
 */
	public TreeList process(TreeList treelist) throws AbortCompilation {
		SJCompilerContext c = (SJCompilerContext)context;		
		return treelist.process(c.SyntacticAnalyzer())
					   .process(c.SJSemanticAnalyzer())
					   .process(c.TransSJ())							   	   
					   .process(c.SemanticAnalyzer())		
					   .process(c.Backend())
					   .process(c.BytecodeOptimizer())
					   .process(c.ClassWriter());	
	}
}

//      /   _ _      JaCo
//  \  //\ / / \     - the extended syntactic analyzer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 30/05/99

package jaco.javaop.context;

import jaco.framework.*;
import jaco.framework.parser.*;
import jaco.javaop.grammar.*;


public class OPSyntacticContext extends jaco.java.context.SyntacticContext {

/** context constructor
 */
    public OPSyntacticContext(OPCompilerContext context) {
        super(context);
    }
    
/** factory methods
 */
    public LRParser Parser(jaco.java.grammar.Scanner scanner) {
        return new Parser(scanner);
    }
    
    public jaco.java.grammar.Scanner Scanner(Sourcefile source) {
        return new Scanner(this, source);
    }
}

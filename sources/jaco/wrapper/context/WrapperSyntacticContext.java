//      /   _ _      JaCo
//  \  //\ / / \     - the extended syntactic analyzer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 30/05/99

package jaco.wrapper.context;

import jaco.framework.*;
import jaco.framework.parser.*;
import jaco.wrapper.grammar.*;


public class WrapperSyntacticContext extends jaco.java.context.SyntacticContext {

/** context constructor
 */
    public WrapperSyntacticContext(WrapperCompilerContext context) {
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

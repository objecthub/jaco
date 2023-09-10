//  /   _ _      JaCo
//  \  //\ / / \     - the extended syntactic analyzer context
//   \//  \\_\_/     
//     \         Matthias Zenger, 30/05/99

package jaco.cjava.context;

import jaco.framework.*;
import jaco.framework.parser.*;
import jaco.cjava.grammar.*;


public class CJSyntacticContext extends jaco.java.context.SyntacticContext {

/** context constructor
 */
    public CJSyntacticContext(CJCompilerContext context) {
    super(context);
    }
    
/** factory methods
 */
    public LRParser Parser(jaco.java.grammar.Scanner scanner) {
    return new Parser(scanner);
    }
    
    public jaco.java.grammar.Scanner Scanner(Sourcefile source) {
    return new jaco.java.grammar.Scanner(this, source);
    }
}

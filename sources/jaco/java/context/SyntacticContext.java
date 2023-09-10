//      /   _ _      JaCo
//  \  //\ / / \     - the syntactic analyzer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.context;

import jaco.java.grammar.*;
import jaco.java.struct.*;
import jaco.framework.*;
import jaco.framework.parser.*;


public class SyntacticContext extends Context
{
/** enclosing context
 */
    public CompilerContext  compilerContext;


/** context constructor
 */
    public SyntacticContext(CompilerContext context)
    {
        super(context);
        compilerContext = context;
    }
    
/** factory methods
 */
    public LRParser Parser(Scanner scanner)
    {
        return new Parser(scanner);
    }
    
    public Scanner Scanner(Sourcefile source)
    {
        return new Scanner(this, source);
    }
}

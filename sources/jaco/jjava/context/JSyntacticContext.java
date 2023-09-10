//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.jjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.grammar.*;
import jaco.jjava.grammar.*;
import jaco.framework.parser.*;


public class JSyntacticContext extends SyntacticContext
{
    public JSyntacticContext(JCompilerContext context)
    {
        super(context);
    }
    
    public LRParser Parser(Scanner scanner)
    {
        return new JParser(scanner);
    }
    
    public Scanner Scanner(Sourcefile source)
    {
        return new JScanner(this, source);
    }
}

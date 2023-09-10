//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.grammar.*;
import jaco.pizza.grammar.*;
import jaco.framework.parser.*;


public class PizzaSyntacticContext extends SyntacticContext
{
    public PizzaSyntacticContext(PizzaCompilerContext context)
    {
        super(context);
    }
    
    public LRParser Parser(Scanner scanner)
    {
        return new PizzaParser(scanner);
    }
    
    public Scanner Scanner(Sourcefile source)
    {
        return new PizzaScanner(this, source);
    }
}

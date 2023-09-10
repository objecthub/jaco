//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               syntactic analyzer context

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.grammar.*;
import jaco.java.grammar.*;
import jaco.java.context.*;
import jaco.framework.parser.*;


public class XSyntacticContext extends SyntacticContext {
    
    public XSyntacticContext(XCompilerContext context) {
        super(context);
    }
    
    public LRParser Parser(Scanner scanner) {
        return new XParser(scanner);
    }
    
    public Scanner Scanner(Sourcefile source) {
        return new XScanner(this, source);
    }
}

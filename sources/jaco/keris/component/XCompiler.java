//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    main compiler component
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.keris.context.*;


public class XCompiler extends jaco.java.component.Compiler {

/** the context
 */
    protected XCompilerContext  context;

/** component name
 */
    public String getName() {
        return "KerisCompiler";
    }

/** component initialization
 */
    public void init(CompilerContext context) {
        super.init(context);
        this.context = (XCompilerContext)context;
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation {
        return treelist.process(context.SyntacticAnalyzer())
                       .process(context.KerisSemanticAnalyzer())
                       .process(context.TransModules())
                       .process(context.SemanticAnalyzer())
                       .process(context.Backend())
                       .process(context.ClassWriter());
               ;
    }
}

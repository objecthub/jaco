//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    semantic analyzer component
//                           
//  [XSemanticAnalyzer.java (1551) 2-Apr-01 00:54 -> 23-Jun-01 00:10]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.keris.context.*;
import jaco.java.component.*;


public class XSemanticAnalyzer extends SemanticAnalyzer {
/** the context
 */
    protected XSemanticContext context;
    
/** component name
 */
    public String getName() {
        return "XSemanticAnalyzer";
    }
    
/** description of trees processor
 */
    public String getDescription() {
        return "semantic analyzer";
    }
    
/** component initialization
 */
    public void init(SemanticContext context) {
        super.init(context.compilerContext);
        this.context = (XSemanticContext)context;
    }
    
/** the debugging name
 */ 
    public String getDebugName() {
        return "xsemantic";
    }
    
/** the tree processor
 */
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        return treelist.process(context.EnterClasses())
                       .process(context.ImportClasses())
                       .process(context.EnterModules())
                       .process(context.EnterMembers())
                       .process(context.Attribute());
    }
}

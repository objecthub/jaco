//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    semantic analyzer that does not report errors
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;


public class SilentSemanticAnalyzer extends SemanticAnalyzer {
    
    public TreeList enter(TreeList treelist) throws AbortCompilation {
        report.setErrorsPossible(false);
        return super.enter(treelist);
    }
    
    public TreeList exit(TreeList treelist) throws AbortCompilation {
        treelist = super.exit(treelist);
        report.setErrorsPossible(true);
        return treelist;
    }
}

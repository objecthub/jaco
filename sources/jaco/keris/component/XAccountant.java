//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    state encapsulation of semantic analysis
//                           
//  [XAccountant.java (804) 2-Apr-01 00:43 -> 2-Apr-01 23:20]

package jaco.keris.component;

import jaco.java.component.*;
import jaco.keris.struct.*;
import jaco.java.struct.*;


public class XAccountant extends Accountant {

    public String getName() {
        return "XAccountant";
    }
    
    public ContextEnv ContextEnv(Tree.CompilationUnit tree) {
        XContextEnv  env = new XContextEnv(tree);
        env.toplevel = tree;
        env.enclClass = rootClassDecl;
        return env;
    }
}

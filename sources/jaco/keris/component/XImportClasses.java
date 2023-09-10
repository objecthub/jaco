//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    import classes
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.keris.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class XImportClasses extends ImportClasses implements XModifierConst {
    
    protected XDefinitions definitions;
    
    
    public String getName() {
        return "XImportClasses";
    }
    
    public void init(SemanticContext context) {
        super.init(context);
        definitions = (XDefinitions)super.definitions;
    }
    
    public void includeAll(Scope inscope, ContextEnv env, Definition c, int kinds) {
        boolean initial = true;
        do {
            Scope s = c.locals();
            Definition  def;
            for (Definition e = s.elems; e != null; e = e.sibling) {
                def = e.def;
                if (((def.kind & ~kinds) == 0) &&
                    (initial || ((def.kind == TYP) &&
                        ((def.modifiers & CASEDEF) != 0))) &&
                    namer.accessible(env, c, def))
                    include(inscope, imported(def));
            }
            initial = false;
            if (definitions.isAlgebraicSubclass(c))
                c = c.supertype().tdef();
            else
                c = null;
        }
        while (c != null);
    }
}

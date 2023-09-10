//      /   _ _      JaCo
//  \  //\ / / \     - process import statements
//   \//  \\_\_/     
//         \         Matthias Zenger, 29/01/01

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.stat.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class PizzaImportClasses extends ImportClasses
                                implements PizzaModifierConst
{
/** helper components
 */
    protected PizzaDefinitions      definitions;
    
    
    public String getName()
    {
        return "PizzaImportClasses";
    }
    
    public void init(SemanticContext context)
    {
        super.init(context);
        definitions = (PizzaDefinitions)super.definitions;
    }
    
    public void includeAll(Scope inscope, ContextEnv env, Definition c, int kinds)
    {
        boolean initial = true;
        do
        {
            Scope       s = c.locals();
            Definition  def;
            for (Definition e = s.elems; e != null; e = e.sibling)
            {
                def = e.def;
                if (((def.kind & ~kinds) == 0) &&
                    (initial || ((def.kind == TYP) && ((def.modifiers & CASEDEF) != 0))) &&
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

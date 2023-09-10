//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.pizza.struct.*;
import jaco.pizza.context.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import Definition.*;
import Type.*;
import Tree.*;


public class JTypeChecker extends TypeChecker {

    protected PizzaClassWriter writer;
    
/** component name
 */
    public String getName() {
        return "JJavaTypeChecker";
    }
    
/** component initialization
 */
    public void init(SemanticContext context) {
        super.init(context);
        writer = (PizzaClassWriter)((PizzaCompilerContext)
            ((JSemanticContext)context).compilerContext).ClassWriter();
    }

/** check that method 'f' conforms with overridden method 'other'
 */
    public boolean checkOverridden(int pos, Definition f, Definition other) {
        if (f.isConstructor())
            return true;
        if ((f.modifiers & STATIC) != 0) {
            if ((other.modifiers & STATIC) == 0)
                return cannotOverride(pos, ".public", f, other, null);
        }
        else
        if ((other.modifiers & (FINAL | STATIC)) != 0)
            return cannotOverride(pos, ".mod", f, other,
                                  modifiers.toString(other.modifiers & (FINAL | STATIC)));
        else
        if (modifiers.protection(f.modifiers) > modifiers.protection(other.modifiers))
            return cannotOverride(pos, ".access", f, other, 
                                  modifiers.protectionString(other.modifiers));
        else
        if (!((PizzaSettings)context.settings).scalahack &&
            !types.sametype(f.type.restype(), other.type.restype())) {
            if (!types.subtype(f.type.restype(), other.type.restype())) {
                typeError(pos, f + " can't override " + other +
                                   " with an incompatible return type",
                               f.type.restype(), other.type.restype());
                return false;
            }
            // System.out.println("check that " + f + " of " + f.owner + " conforms with " + other + " of " + other.owner);
            BridgeMethodSet bmset = (BridgeMethodSet)writer.bridges.get(f);
            if (bmset == null)
                writer.bridges.put(f, new BridgeMethodSet(types, other.type));
            else
                bmset.add(other.type);
        }
        Type unhandled = unHandled(f.type.thrown(), other.type.thrown());
        if (unhandled != null)
            return cannotOverride(pos, ".throws", f, other, unhandled);
        return true;
    }
}

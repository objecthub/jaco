//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type checking utilities
//                           
//  [JTypeChecker.java (2738) 2-Apr-01 00:42 -> 2-Apr-01 00:14]

package jaco.keris.component;

import jaco.framework.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import Definition.*;
import Type.*;
import Tree.*;


public class JTypeChecker extends TypeChecker
                          implements XModifierConst {

    protected XClassWriter writer;
    
	/** component name
	 */
    public String getName() {
        return "JJavaTypeChecker";
    }
    
	/** component initialization
	 */
    public void init(SemanticContext context) {
        super.init(context);
        writer = (XClassWriter)((XCompilerContext)
            ((JSemanticContext)context).compilerContext).ClassWriter();
    }
    
	/** check that all abstract members of class 'c' have definitions
     *
	public void checkAllDefined(int pos, Definition c)
	{
		Definition  undef = firstUndef(c, c);
		if (undef != null)
		{
			Definition  undef1 = definitions.make.MethodDef(undef.modifiers,
									undef.name, undef.type, undef.owner);
			report.error(pos, "missing.abstract", c, undef1 + undef1.location());
		}
	} */

	
	/** check that this method conforms with any class member method it overrides
	 */
    public void checkOverride(int pos, Definition f) {
        Definition c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
                e.scope != null; e = e.next())
            if ((f != e.def) && definitions.overrides(e.def, f))
                checkOverridden(pos, f, e.def);
    }
    
    /** does function 'f' override function definition `other'?
	 *  PRE: f.name == other.name
	 */
    public boolean overrides(Definition f, Definition other) {
        return !f.isConstructor() &&
               !other.isConstructor() &&
               (other.kind == FUN) &&
               (f.kind == FUN) &&
               ((other.modifiers & PRIVATE) == 0) &&
               types.sametypes(f.type.argtypes(), other.type.argtypes());
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
        if (!types.sametype(f.type.restype(), other.type.restype())) {
            if (!types.subtype(f.type.restype(), other.type.restype())) {
                typeError(pos, f + " can't override " + other +
                                   " with a incompatible return type",
                               f.type.restype(), other.type.restype());
                return false;
            }
            //if (f.name.toString().startsWith("$access"))
            //	System.out.println("check that " + f + ": " + f.type.restype().tdef() + " of " + f.owner + " conforms with " +
            //		other + ": " + other.type.restype().tdef() + " of " + other.owner);
            BridgeMethodSet bmset = (BridgeMethodSet)writer.bridges.get(f);
            if (bmset == null)
                writer.bridges.put(f, new BridgeMethodSet(types, other.type));
            else
                bmset.add(other.type);
        } else
        	if (f.name.toString().startsWith("$access"))
        		System.out.println("skipped " + other + " of " + other.owner);
        Type unhandled = unHandled(f.type.thrown(), other.type.thrown());
        if (unhandled != null)
            return cannotOverride(pos, ".throws", f, other, unhandled);
        return true;
    }
}

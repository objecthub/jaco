//      /   _ _      JaCo
//  \  //\ / / \     - pizza type checking module
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.stat.struct.*;
import jaco.java.context.*;
import java.util.Hashtable;
import Definition.*;


public class PizzaTypeChecker extends TypeChecker
{
/** other components
 */
    protected PizzaTypes        types;
    protected PizzaDefinitions  definitions;
    
    public String getName()
    {
        return "PizzaTypeChecker";
    }
    
    public void init(SemanticContext context)
    {
        super.init(context);
        types = (PizzaTypes)super.types;
        definitions = (PizzaDefinitions)super.definitions;
    }



/** check that method 'f' conforms with overloaded method 'other'
 */
    public boolean checkOverloaded(int pos, Definition f, Definition other)
    {
        Type[]  fargs = f.type.argtypes();
        Type[]  oargs = other.type.argtypes();
        
        for (int i = 0; i < fargs.length; i++)
            if (types.isAlgebraicType(fargs[i]) &&
                types.isAlgebraicType(oargs[i]) &&
                (((CDef)fargs[i].tdef()).baseClass ==
                ((CDef)oargs[i].tdef()).baseClass) &&
                !types.sametype(fargs[i], oargs[i]))
            {
                report.error(pos, f + f.location() + " cannot overload " + other +
                             other.location() + "; overloading is restricted " +
                             "for methods with algebraic argument types");
                return false;
            }
        return true;
    }
    
/** check that this method conforms with any method it overloades
 */
    public void checkOverload(int pos, Definition f)
    {
        Definition  c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
                e.scope != null; e = e.next())
            if ((f != e.def) && definitions.overloads(f, e.def))
            {
                checkOverloaded(pos, f, e.def);
                return;
            }
    }

/** check, if a method 'f' of an algebraic superclass overrides another
 *  method
 */
    public void mustOverride(int pos, Definition f)
    {
        Definition  c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
                e.scope != null; e = e.next())
            if ((f != e.def) && definitions.overrides(f, e.def))
                return;
        report.error(pos, "method " + f + f.location() + " must override " +
                    "another method");
    }

/** return all interfaces implemented by root
 */
    protected Type[] collectInterfaces(Type root)
    {
        if (root == null)
            return null;
        Definition  def = root.tdef();
        if (def.kind == TYP)
            return types.append(def.interfaces(),
                                collectInterfaces(root.supertype()));
        else
            return null;
    }

/** check that an algebraic subclass does not define a new type by
 *  implementing a new interface
 */
    public void noNewTypeIntro(int pos, Type[] is, Type root)
    {
        Type[]  ts = collectInterfaces(root);
        if (ts == null)
            return;
        outer: for (int i = 0; i < is.length; i++)
        {
            for (int j = 0; j < ts.length; j++)
                if (types.subtype(ts[j], is[i]))
                    continue outer;
            report.error(pos, "algebraic subclasses cannot implement new interfaces");
        }
    }
        
/** check that method 'f' conforms with overridden method 'other'
 */
    public boolean checkOverridden(int pos, Definition f, Definition other)
    {
        boolean res = super.checkOverridden(pos, f, other);
        if (res && ((f.modifiers & STATIC) == 0)) {
            Stat.addOverridden(types, f, other);
        }
        return res;
    }
}

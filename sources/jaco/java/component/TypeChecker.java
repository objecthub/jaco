//      /   _ _      JaCo
//  \  //\ / / \     - support for java type checking
//   \//  \\_\_/     
//         \         Matthias Zenger, 16/07/2K

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Definition.*;
import Type.*;
import Tree.*;


public class TypeChecker extends Component
                         implements ModifierConst, DefinitionConst, TypeConst
{
/** other components
 */
    protected Definitions       definitions;
    protected Types             types;
    protected Modifiers         modifiers;
    protected ErrorHandler      report;
    
    
/** component name
 */
    public String getName()
    {
        return "JavaTypeChecker";
    }

/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context);
        definitions = context.compilerContext.mainContext.Definitions();
        types = context.compilerContext.mainContext.Types();
        modifiers = context.compilerContext.mainContext.Modifiers();
        report = context.compilerContext.mainContext.ErrorHandler();
    }
    
    
/** issue a type error
 */
    public void typeError(int pos, String problem, Type found, Type req)
    {
        report.error(pos, problem + "\n  found   : " + found +
                        "\n  required: " + req);
    }

    public void typeError(int pos, String problem, Type[] found, Type[] req)
    {
        report.error(pos, problem +
                        "\n  found   : (" + Tools.toString(found) +
                        ")\n  required: (" + Tools.toString(req) + ")");
    }


/** check that type 'foundt' is assignable to proto-type 'reqt'.
 *  if it is, return 'foundt', otherwise return Type.ErrType
 */
    public Type checkType(int pos, Type foundt, Type reqt)
    {
        switch (reqt.deref())
        {
            case ErrType:
                return reqt;
            
            case MethodType(Type[] args, Type res, Type[] throwna):
                if (res == null) // only arguments matter
                    return foundt;
                break;
        }
        if (types.assignable(foundt, reqt))
            return foundt;
        String problem =
                (foundt.tag() >= MIN_BASICTYPE_TAG) &&
                (reqt.tag() >= MIN_BASICTYPE_TAG) &&
                (foundt.tag() <= DOUBLE) && (reqt.tag() <= DOUBLE) ?
                    "possible loss of precision" :
                    "incompatible types";
        typeError(pos, problem, foundt, reqt);
        return Type.ErrType;
    }

/** return the least common supertype of type a and type b;
 *  report an error and return Type.ErrType if none exists.
 */
    public Type join(int pos, Type a, Type b)
    {
        a = types.deconst(a);
        b = types.deconst(b);
        if ((a == Type.AnyType) || (b == Type.ErrType))
            return b;
        if ((b == Type.AnyType) || (a == Type.ErrType))
            return a;
        if ((a.tag() == CHAR) || (b.tag() == CHAR))
        {
            if ((a.tag() == CHAR) && (b.tag() == CHAR))
                return a;
            else
                return types.intType;
        }
        if (types.subtype(a, b))
            return b;
        if (types.subtype(b, a))
            return a;
        Type c = a.supertype();
        while (c != null)
            if (types.subtype(b, c))
                return c;
            else
                c = c.supertype();
        report.error(pos, "no.common.supertype", a, b);
        return Type.ErrType;
    }
    
/** check that type t is different from 'void'.
 */
    public Type checkNonVoid(Tree tree)
    {
        if (tree.type == Type.VoidType)
        {
            report.error(tree.pos, "illegal.void.type");
            tree.type = Type.ErrType;
        }
        return tree.type;
    }

/** check that type t can be cast to type reqt
 */
    public Type checkCastable(int pos, Type foundt, Type reqt)
    {
        if (types.castable(foundt, reqt))
            return reqt;
        typeError(pos, "inconvertible types", foundt, reqt);
        return Type.ErrType;
    }

/** check types against bounds
 */
    public void checkBound(Tree type, Type bound)
    {
        checkType(type.pos, type.type, bound);
    }

    public void checkBound(Tree[] types, Type bound)
    {
        for (int i = 0; i < types.length; i++)
            checkBound(types[i], bound);
    }


/** is exc handled by given exception list?
 */
    public boolean isHandled(Type exc, Type[] handled)
    {
        return (exc == Type.ErrType) ||
                types.subtype(exc, types.errorType) ||
                types.subtype(exc, types.runtimeExceptionType) ||
                types.elem(exc, handled);
    }

/** is exc thrown in the given list of exceptions?
 */
    public boolean isThrown(Type exc, Type[] thrown)
    {
        if ((exc == Type.ErrType) ||
            types.subtype(types.runtimeExceptionType, exc) ||
            types.subtype(exc, types.runtimeExceptionType) ||
            types.subtype(exc, types.errorType))
            return true;
        else
        {
            for (int i = 0; i < thrown.length; i++)
                if (types.subtype(thrown[i], exc) ||
                    types.subtype(exc, thrown[i]))
                    return true;
            return false;
        }
    }
    
/** return an exception in 'thrown' that is not contained in 'handled',
 *  or null if none exists
 */
    public Type unHandled(Type[] thrown, Type[] handled)
    {
        if (thrown == null)
            return null;
        if (handled == null)
            return thrown[0];
        for (int i = 0; i < thrown.length; i++)
            if (!isHandled(thrown[i], handled))
                return thrown[i];
        return null;
    }

/** check that exception exc is in in reported
 */
    public void checkHandled(int pos, Type exc, Type[] reported)
    {
        if (!isHandled(exc, reported))
            report.error(pos, "unreported.exception", exc);
    }

/** if t is a method type, check that all exceptions thrown by t
 *  are in reported
 */
    public void checkHandled(int pos, Type[] excs, Type[] reported)
    {
        Type unhandled = unHandled(excs, reported);
        if (unhandled != null)
            checkHandled(pos, unhandled, reported);
    }
    
    public boolean cannotOverride(int pos, String add, Definition f, Definition other, Object last)
    {
        report.error(pos, "cannot.override" + add, f + f.location(), other + other.location(), last);
        return false;
    }
    
/** check that method 'f' conforms with overridden method 'other'
 */
    public boolean checkOverridden(int pos, Definition f, Definition other)
    {
        if ((f.modifiers & STATIC) != 0)
        {
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
        if (!types.sametype(f.type.restype(), other.type.restype()))
        {
            typeError(pos, f + " can't override " + other +
                            " with a different return type",
                            f.type.restype(), other.type.restype());
            return false;
        }
        else
        {
            Type unhandled = unHandled(f.type.thrown(), other.type.thrown());
            if (unhandled != null)
                return cannotOverride(pos, ".throws", f, other, unhandled);
        }
        return true;
    }

/** return first abstract object in class c that is not defined in impl,
 *  null if there is none
 */
    protected Definition firstUndef(Definition root, Definition c)
    {
        Definition  undef = null;
        if ((c == root) || ((c.modifiers & (ABSTRACT | INTERFACE)) != 0))
        {
            Scope   s = c.locals();
            for (Definition e = s.elems; e != null; e = e.sibling)
                if ((e.def.kind == FUN) && ((e.def.modifiers & ABSTRACT) != 0))
                {
                    Definition  absfun = e.def;
                    Definition  implfun = definitions.implementation(absfun, root);
                    if ((implfun == null) || (implfun == absfun))
                        return absfun;
                }
            if (c.supertype() != null)
            {
                undef = firstUndef(root, c.supertype().tdef());
                int i = 0;
                while ((undef == null) && (i < c.interfaces().length))
                    undef = firstUndef(root, c.interfaces()[i++].tdef());
            }
        }
        return undef;
    }

/** check that all abstract members of class 'c' have definitions
 */
    public void checkAllDefined(int pos, Definition c)
    {
        Definition  undef = firstUndef(c, c);
        if (undef != null)
        {
            Definition  undef1 = definitions.make.MethodDef(undef.modifiers,
                                    undef.name, undef.type, undef.owner);
            report.error(pos, "missing.abstract", c, undef1 + undef1.location());
        }
    }

/** check that this method conforms with any class member method it overrides
 */
    public void checkOverride(int pos, Definition f)
    {
        Definition  c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
                e.scope != null; e = e.next())
            if ((f != e.def) && definitions.overrides(e.def, f))
            {
                checkOverridden(pos, f, e.def);
                return;
            }
    }

/** check that all methods which implement some interface
 *  method of c conform to the method they implement. root is the
 *  definition of the class which implements the interface
 */
    public void checkImplementations(int pos, Definition root)
    {
        Type[]  is = root.interfaces();
        for (int i = 0; i < root.interfaces().length; i++)
            checkImplementations(pos, root, is[i].tdef());
    }

    public void checkImplementations(int pos, Definition root, Definition ic)
    {
        for (Definition e = ic.locals().elems; e != null; e = e.sibling)
        {
            if ((e.def.kind == FUN) && ((e.def.modifiers & STATIC) == 0))
            {
                Definition  absfun = e.def;
                Definition  implfun = definitions.implementation(absfun, root);
                if (implfun != null)
                    checkOverridden(pos, implfun, absfun);
            }
        }
        Type[]  is = ic.interfaces();
        int     icintflen = ic.interfaces().length;
        for (int i = 0; i < icintflen; i++)
            checkImplementations(pos, root, is[i].tdef());
    }
}

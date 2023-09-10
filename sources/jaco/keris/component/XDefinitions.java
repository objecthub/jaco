//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    definition module
//                           
//  [XDefinitions.java (4636) 10-Apr-01 15:49 -> 23-Jun-01 00:05]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.util.*;
import Definition.*;
import XDefinition.*;


public class XDefinitions extends Definitions
                          implements XModifierConst {
                          
    XMainContext mainContext;
    
/** other components
 */
    protected XTypes    types;
    
    
    public String getName() {
        return "XDefinitions";
    }
    
    public void initComponent(MainContext context) {
        if (make == null) {
            super.initComponent(context);
            mainContext = (XMainContext)context;
        }
    }
    
    protected DefinitionFactory DefinitionFactory(Types types) {
        return new XDefinitionFactory(this, this.types = (XTypes)types);
    }

    
    public Definition algebraicBase(Definition def) {
        Type    type;
        while (types.isAlgebraicType(type = def.supertype()))
            def = type.tdef();
        return def;
    }
    
    public Definition algebraicClass(Definition def) {
        Type    type = def.type;
        while (!types.isAlgebraicType(type))
            if (type == null)
                return null;
            else {
                def = type.tdef();
                type = def.supertype();
            }
        return def;
    }
    
    public boolean isAlgebraicClass(Definition def) {
        return (def.kind == TYP) && ((def.modifiers & ALGEBRAIC) != 0);
    }
    
    public boolean isAlgebraicSubclass(Definition def) {
        return (def.kind == TYP) && ((def.modifiers & ALGEBRAIC) != 0)
                && (((XClassDef)def).baseClass != def);
    }
    
    public boolean isCase(Definition def) {
        return (def.kind == TYP) && ((def.modifiers & CASEDEF) != 0);
    }
    
    public int getCaseTag(Definition def) {
        switch (def) {
            case ClassDef(_, _, _, _, _):
                return ((XClassDef)def).tag;
            case MethodDef(_):
                return ((XMethodDef)def).tag;
            case VarDef(_, _, _):
                return ((XVarDef)def).tag;
            default:
                throw new InternalError();
        }
    }
    
    public Definition getCaseDefFromConstr(Definition constr) {
        return constr.type.restype().tdef();
    }
    
    public Definition[] getCaseMembers(Definition def) {
        if ((def.modifiers & CASEDEF) == 0)
            throw new InternalError();
        Definition base = def;
        Definition[] vars = def.members(VAR);
        int start = 0;
        while ((start < vars.length) &&
               (((vars[start].modifiers & (STATIC | PRIVATE)) != 0) ||
                ((vars[start].modifiers & PUBLIC) == 0)))
            start++;
        int i = start;
        while ((i < vars.length) &&
               ((vars[i].modifiers & (STATIC | PRIVATE)) == 0) &&
               ((vars[i].modifiers & PUBLIC) != 0))
            i++;
        Definition[] fields = new Definition[i - start];
        for (int k = 1; k <= fields.length; k++) {
            fields[i - k - start] = vars[start + k - 1];
        }
        return fields;
    }
    
    public Definition getCaseConstr(Definition site, int tag) {
        Definition[] defs = site.members(FUN);
        for (int i = 0; i < defs.length; i++)
            if (((defs[i].modifiers & CASEDEF) != 0) &&
                (((XMethodDef)defs[i]).tag == tag))
                return defs[i];
        defs = site.members(VAR);
        for (int i = 0; i < defs.length; i++)
            if (((defs[i].modifiers & CASEDEF) != 0) &&
                (((XMethodDef)defs[i]).tag == tag))
                return defs[i];
        return null;
    }
    
    public int[] getCaseArgNums(Definition site) {
        int[]   args = new int[((XClassDef)site).tag];
        do {
            Definition[] caseDefs = site.members(TYP);
            int tag;
            for (int i = 0; i < caseDefs.length; i++)
                if (((caseDefs[i].modifiers & CASEDEF) != 0) &&
                    ((tag = ((XClassDef)caseDefs[i]).tag) >= 0))
                    args[tag] = getCaseMembers(caseDefs[i]).length;
        } while ((site != ((XClassDef)site).baseClass) &&
                ((site = site.supertype().tdef()) == site)); // don't think about that :-)
        return args;
    }
    
    public Definition[][] getCaseArgDefs(Definition site) {
        Definition[][] argdefs = new Definition[((XClassDef)site).tag][];
        do {
            Definition[] caseDefs = site.members(TYP);
            int tag;
            for (int i = 0; i < caseDefs.length; i++)
                if (((caseDefs[i].modifiers & CASEDEF) != 0) &&
                    ((tag = ((XClassDef)caseDefs[i]).tag) >= 0))
                    argdefs[tag] = getCaseMembers(caseDefs[i]);
        } while ((site != ((XClassDef)site).baseClass) &&
                ((site = site.supertype().tdef()) == site)); // don't think about that :-)
        for (int i = 0; i < argdefs.length; i++)
            if (argdefs[i] == null)
                argdefs[i] = new Definition[0];
        return argdefs;
    }

/** does method 'f' overload method definition 'other' with same
 *  number of arguments?
 *  PRE: f.name == other.name
 */
    public boolean overloads(Definition f, Definition other) {
        if (!f.isConstructor() &&
            (other.kind == FUN) &&
            (((other.modifiers & PRIVATE) == 0) ||
            (f.owner == other.owner))) {
            Type[]  fargs = f.type.argtypes();
            Type[]  oargs = other.type.argtypes();
            return (fargs.length == oargs.length) &&
                    !types.sametypes(fargs, oargs);
        }
        return false;
    }
    
/** swap the scopes of class definitions; used to toggle between
 *  java and keris scopes
 */
    public void swapScopes() {
        for (Enumeration e = classes.elements(); e.hasMoreElements();)
            ((XClassDef)e.nextElement()).swap();
    }
    
/** does class field c implement abstract class field other
 */
    public boolean implementsClassField(Definition c, Definition other) {
        return (c.kind == TYP) &&
               (c != other) &&
               ((c.modifiers & PRIVATE) == 0);
    }
    
/** does other override function f?
 */
    public boolean overrides(Definition f, Definition other) {
        if (!mainContext.algebraicClasses)
            return super.overrides(f, other);
        if ((other == f) ||
            (other.kind != FUN) ||
            (other.owner == f.owner) ||
            ((other.modifiers & PRIVATE) != 0))
            return false;
        XClassDef top = (XClassDef)f.topLevelClass();
        if ((top.modifiers & MODULE) != 0) {
            XClassDef mod = (XClassDef)other.topLevelClass();
            if ((mod.modifiers & MODULE) != 0) {
                Type ftype = mainContext.ModuleContexts().refineType(f.type, mod);
                Type otype = other.type; //modules.refineType(f.type, mod);
                //System.out.println("overriding from " + f + " over " + other + "; seen from module " + mod + "; other is " + top);
                //for (int i = 0; i < ftype.argtypes().length; i++)
                //  System.out.println("  " + otype.argtypes()[i] + " <=> " + ftype.argtypes()[i]);
                return types.sametypes(ftype.argtypes(), otype.argtypes());
            } else
                return false;
        } else
            return types.sametypes(f.type.argtypes(), other.type.argtypes());
    }
    
/** does constructor other override constructor with argtypes args?
 */
    public boolean overrides(Type[] args, Definition other) {
        if ((other.kind != FUN) ||
            ((other.modifiers & PRIVATE) != 0))
            return false;
        return types.sametypes(args, other.type.argtypes());
    }

/** the implementation of (abstract) definition 'f' in class 'root';
 *  null if none exists
 */
    public Definition implementation(Definition f, Definition root) {
        if (!mainContext.algebraicClasses)
            return super.implementation(f, root);
        if (f.kind == FUN) {
            if (f.isConstructor()) {
                Type[] args = f.type.argtypes();
                for (Definition e = root.locals().lookup(f.name);
                     e.scope != null;
                     e = e.next()) {
                    if ((e.def.owner == root) &&
                        (e.def != f) &&
                        (e.def.kind == FUN) &&
                        (e.def.owner != f.owner) &&
                        ((e.def.modifiers & PRIVATE) == 0)) {
                         Type[] oargs = e.def.type.argtypes();
                         if (((e.def.owner.modifiers & STATIC) == 0) &&
                              (e.def.owner.owner.kind != PCK)) {
                            Type[] newargs = new Type[oargs.length - 1];
                            System.arraycopy(oargs, 1, newargs, 0, newargs.length);
                            oargs = newargs;
                        }
                        if (types.sametypes(args, oargs))
                            return e.def;
                     }
                }
                return null;
            } else
                return super.implementation(f, root);
        } else if ((f.kind == TYP) && ((f.modifiers & CLASSFIELD) != 0)) {
            Name name = mangler.unmangleShort(f.name, f.fullname);
            Definition e = root.locals().lookup(name);
            for (; e.scope != null; e = e.next())
                if (implementsClassField(e.def, f))
                    return e.def;
            return null;
        } else
            return null;
    }
    
/** is this definition an innerclass?
 */
    public boolean isInnerclass(Definition that) {
        return (((MainContext)context).classesNest &&
                ((that.modifiers & CLASSFIELD) != 0)) ||
               super.isInnerclass(that);
    }

    
    /*
    public Definition defineClass(Name fullname) {
        Definition res = super.defineClass(fullname);
        if (fullname.toString().endsWith("SUBJECT")) {
            System.out.println(">>>>> define " + fullname + ", " + res.hashCode());
        }
        return res;
    } */
}

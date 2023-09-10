//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    name resolution support
//                           
//  [XNameResolver.java (5328) 16-May-01 15:12 -> 23-Jun-01 00:09]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.io.IOException;
import Definition.*;
import Type.*;
import Tree.*;


public class XNameResolver extends NameResolver
                           implements XModifierConst {

/** other components
 */
    protected ModuleContexts modules;
    protected XMainContext mainContext;
    
/** component name
 */
    public String getName() {
        return "XNameResolver";
    }
    
    
/** component initialization
 */
    public void init(MainContext context) {
        super.init(context);
        modules = (mainContext = (XMainContext)context).ModuleContexts();
    }
    
/** is module nested contained in the current environment env?
 */
    public boolean contained(XClassDef mod, ContextEnv env) {
        Type mt = ((XContextInfo)env.info).moduleType;
        // do we access module mod in an unqualified context?
        if (mt == null) {
            XClassDef from = (XClassDef)env.enclClass.def().topLevelClass();
            if ((from.modifiers & MODULE) == 0)
                return false;
            if (from.modIntf.required.contains(mod))
                return true;
            if (from.modIntf.contained.contains(mod))
                return true;
            if (from.refinementOf(mod))
                return true;
        // we access module mod in a module selection
        } else {
            XClassDef from = (XClassDef)mt.tdef();
            if (from.modIntf.contained.contains(mod))
                return true;
        }
        return false;
    }
    
/** find a variable
 */
    public Definition findVar(ContextEnv env, Name name) {
        Definition def = super.findVar(env, name);
        if (!((XMainContext)context).algebraicClasses)
            return def;
        else if (def.kind >= BAD) {
            Definition def1 = findType(env, name);
            if ((def1.kind < BAD) &&
                ((def1.modifiers & MODULE) != 0) &&
                contained((XClassDef)def1, env))
                return def1;
        }
        return def;
    }
    
    public Definition findClassField(Definition c, Name name) {
        Definition  def = typeNotFound;
        Definition  e = c.locals().lookup(name);
        while (e.scope != null) {
            if (e.def.kind == TYP)
                return e.def;
            e = e.next();
        }
        return def;
    }
    
    public Definition findCase(ContextEnv env, Type site, Name name) {
		Definition e = site.tdef().locals().lookup(name);
		while (e.scope != null) {
			if (((e.def.modifiers & CASEDEF) != 0) &&
			    (e.def.kind == FUN) &&
			    accessible(env, site.tdef(), e.def))
				return e.def;
			e = e.next();
		}
		return funNotFound;
    }
    
    public Definition findBestMethod(ContextEnv env, Type site,
                                    Definition e, Type[] argtypes) {
        //System.out.println("  findBestMethod " + e.def + " in " + site);
        return super.findBestMethod(env, site, e, argtypes);
    }
    
    public Definition findMethod(ContextEnv env, Type site,
                                 Name name, Type[] argtypes) {
        //System.out.println("*** FIND " + name + " IN " + site);
        //site.tdef().locals().print();
        //System.out.println("***");
        if ((site.tdef().modifiers & CLASSFIELD) != 0) {
            Definition def = findAbstractMethod(env, site.tdef(),
                                                funNotFound,
                                                name, argtypes, site);
            if (def.kind < BAD)
                def = checkBestAbstractMethod(env, site.tdef(), def, argtypes, site);
            return def;
        } else
            return super.findMethod(env, site, name, argtypes);
    }

/** is method definition 'a' at least as good as method Definition 'b'
 *  when used from class 'site'?
 *  used to get the best-matching overloaded instance during object lookup.
 */
    public boolean asGood(ContextEnv env, Type site,
                          Definition a, Definition b) {
        if (!accessible(env, site.tdef(), b))
            return true;
        if (!mainContext.algebraicClasses) {
            if (accessible(env, site.tdef(), a) &&
                ((((a.modifiers | b.modifiers) & STATIC) != 0) ||
                     (((a.owner.modifiers | b.owner.modifiers) & INTERFACE) != 0) ||
                     definitions.subclass(a.owner, b.owner)) &&
                instantiatable(b, a.type.argtypes(), site, env))
                return types.subtype(a.type.restype(), b.type.restype());
            return types.sametype(a.type, b.type);
        }
        if (accessible(env, site.tdef(), a) &&
            ((((a.modifiers | b.modifiers) & STATIC) != 0) ||
                 (((a.owner.modifiers | b.owner.modifiers) & INTERFACE) != 0) ||
                 definitions.subclass(a.owner, b.owner)) &&
            instantiatable(b, a.type.argtypes(), site, env))
            return types.subtype(
                modules.migrateType(a.type.restype(), a, site, env),
                modules.migrateType(b.type.restype(), b, site, env));
        return types.sametype(
                modules.migrateType(a.type, a, site, env),
                modules.migrateType(b.type, b, site, env));
        /*
        if (!super.asGood(env, site, a, b))
            return types.sametype(
                modules.updateType(a.type, site, mod),
                modules.updateType(b.type, site, mod));
        else
            return true; */
    }

/** instantiate a function `fn' to a vector of argument types
 *  `argtypes' with `site' as the site class.
 */
    public boolean instantiatable(Definition fn, Type[] argtypes, Type site, ContextEnv env) {
        if (!mainContext.algebraicClasses)
            return super.instantiatable(fn, argtypes, site, env);
        //System.out.println("instantiate " + fn + " of " + fn.owner +" with receiver " + site + " in module " + env.enclClass.def().topLevelClass() + ": ");
        switch (fn.type) {
            case MethodType(Type[] formals, _, _):
               return (argtypes == null) ||
                      types.subtypes(
                        argtypes,
                        modules.migrateType(formals, fn, site, env));
            default:
                return false;
        }
    }
    
    public Type[] printTypes(Type[] ts) {
        for (int i = 0; i < ts.length; i++)
            System.out.println("  " + ts[i] + " with supertype " + ts[i].tdef().supertype());
        return ts;
    }
    
    public Type[] printWithTypes(Type[] ts) {
        System.out.println("  <==>");
        for (int i = 0; i < ts.length; i++)
            System.out.println("  " + ts[i] + " with supertype " + ts[i].tdef().supertype());
        return ts;
    }
    
    boolean printBool(boolean b) {
        //System.out.println("=> " + b);
        return b;
    }

    public void fixupScope(int pos, Definition c) {
        if (c.locals() != null) {
            //System.out.println("!!! Fixup scope of " + c);
            if ((c.locals().next == null) && (c.supertype() != null)) {
                c.locals().next = c.locals(); // mark to detect cycles
                fixupScope(pos, c.supertype().tdef());
                for (int i = 0; i < c.interfaces().length; i++)
                    fixupScope(pos, c.interfaces()[i].tdef());
                if (c.owner.kind != PCK)
                    fixupScope(pos, c.owner.enclClass());
                if (c.locals().next == c.locals())
                    c.locals().baseOn(c.supertype().tdef().locals());
            } else if (c.locals().next == c.locals()) {
                report.error(pos, "cyclic.inheritance", c);
                c.locals().baseOn(types.objectType.tdef().locals());
                c.setSupertype(types.objectType);
                c.setInterfaces(new Type[0]);
            }
        }
    }
    
/******* new resolution methods
 */
 
    public Definition resolveIdentPartially(int pos, Name name,
                                            ContextEnv env, int kind, Type pt) {
        Definition def = typeNotFound;
        if ((kind & VAR) != 0)
            def = findVar(env, name);
        if ((def.kind >= BAD) && ((kind & FUN) != 0)) {
            Definition def1 = findFun(env, name, protoArgs(pt));
            if (def1.kind < def.kind)
                def = def1;
        }
        if ((def.kind >= BAD) && ((kind & TYP) != 0)) {
            Definition def1 = findType(env, name);
            if (def1.kind < def.kind)
                def = def1;
        }
        if ((def.kind >= BAD) && ((kind & PCK) != 0)) {
            Definition  def1 = loadPackage(env, name);
            if (def1.kind < BAD)
                def = def1;
            else if ((kind & TYP) != 0 && def.kind == ABSENT_TYPE &&
                def1.kind == LOADERROR)
                def = LoadError(((LoadError)def1).fault, TYP | PCK, name);
        }
        return def;
    }


    
}

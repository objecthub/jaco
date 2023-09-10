//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type checking support
//                           
//  [XTypeChecker.java (8999) 12-Apr-01 12:32 -> 6-Jun-01 00:12]

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


public class XTypeChecker extends TypeChecker implements XModifierConst {
    
/** component references
 */
    protected ModuleContexts modules;
    protected XTypes types;
    protected XDefinitions definitions;
    
/** component name
 */
    public String getName() {
        return "XTypeChecker";
    }
    
/** component initialization
 */
    public void init(SemanticContext context) {
        super.init(context);
        types = (XTypes)super.types;
        definitions = (XDefinitions)super.definitions;
        modules = ((XMainContext)context.compilerContext.mainContext).ModuleContexts();
    }
    
/** check that this method conforms with any class member method it overrides
 */
    public void checkOverride(int pos, Definition f) {
        Definition  c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
             e.scope != null; e = e.next())
            if ((f != e.def) &&
                definitions.overrides(f, e.def) &&
                (((e.def.owner.modifiers &  INTERFACE) != 0) ||
                 !f.isConstructor())) {
                checkOverridden(pos, f, e.def);
                //return;
            }
    }
    
/** check that method 'f' conforms with overridden method 'other'
 */
    public boolean checkOverridden(int pos, Definition f, Definition other) {
        //System.out.println("checkOverridden(" + f + "(" + f.owner + "), " + other + "(" + other.owner +"))");
        //System.out.println("  " + modifiers.toString(f.modifiers) + " / " + modifiers.toString(other.modifiers));
        if ((f.modifiers & STATIC) != 0) {
            if ((other.modifiers & STATIC) == 0)
                return cannotOverride(pos, ".public", f, other, null);
        } else if ((other.modifiers & (FINAL | STATIC)) != 0)
            return cannotOverride(pos, ".mod", f, other,
                modifiers.toString(other.modifiers & (FINAL | STATIC)));
        else if (modifiers.protection(f.modifiers) > modifiers.protection(other.modifiers))
            return cannotOverride(pos, ".access", f, other, 
                                  modifiers.protectionString(other.modifiers));
        else if (!modules.subtype(f.type.restype(), other.type.restype())) {
            typeError(pos, f + " can't override " + other +
                            " with an incompatible return type",
                            f.type.restype(), other.type.restype());
            return false;
        } else {
            Type otype = ((f.topLevelClass().modifiers & MODULE) != 0) ?
                modules.refineType(other.type, (XClassDef)f.topLevelClass()) :
                other.type;
            Type unhandled = unHandled(f.type.thrown(), otype.thrown());
            if (unhandled != null)
                return cannotOverride(pos, ".throws", f, other, unhandled);
        }
        return true;
    }

/** check that 'f' conforms with overridden class field 'other'
 */
    public boolean checkOverriddenClassField(int pos, Definition f, Definition other) {
        //System.out.println(f + " implements");
        //Type[] jf = f.interfaces();
        //for (int i = 0; i < jf.length; i++)
        //    System.out.println("  " + jf[i]);
        // check that f defines at least the same interfaces as other
        Type[] ifs = other.interfaces();
        for (int i = 0; i < ifs.length; i++)
            if (!modules.subtype(f.type, ifs[i])) {
                report.error(pos, f.type + " does not implement interface " + ifs[i]);
                return false;
            }
        // for final class field others do not allow overriding
        if ((other.modifiers & FINAL) != 0)
            report.error(pos, f + " cannot override final " + other);
        // check consistend use of abstract
        else if (((f.modifiers & ABSTRACT) != 0) && ((other.modifiers & ABSTRACT) == 0))
            report.error(pos, f + " cannot be declared abstract; it overrides a non-abstract class field");
        // check correct access modifiers
        else if (modifiers.protection(f.modifiers) > modifiers.protection(other.modifiers))
            return cannotOverride(pos, ".access", f, other, modifiers.protectionString(other.modifiers));
        else
            return true;
        return false;
    }
    
/** check that class field initializer 'f' conforms with class field 'other'
 */
    public boolean checkClassFieldInitializer(int pos, Definition f, Definition other) {
        // check that all interfaces are implemented
        Type[] ifs = other.interfaces();
        for (int i = 0; i < ifs.length; i++)
            if (!modules.subtype(f.type, ifs[i])) {
                report.error(pos, "class field initializer " + f +
                                  " does not implement interface " + ifs[i]);
                return false;
            }
        // abstract class fields have abstract initializers
        if ((f.modifiers & ABSTRACT) != 0) {
            if ((other.modifiers & ABSTRACT) == 0) {
                report.error(pos, "class field has to be declared abstract");
                return false;
            }
        }
        // class field initializers are never final
        if ((other.modifiers & FINAL) != 0) {
            report.error(pos, "class field initializers cannot be final");
            return false;
        }
        return true;
    }
    
/** return first abstract object in class c that is not defined in root;
 *  null if there is none
 */
    protected Definition firstUndef(Definition root, Definition c) {
        //System.out.println("firstUndef(" + root + ", " + c + ")");//DEBUG
        Definition undef = null;
        if ((c == root) || ((c.modifiers & (ABSTRACT | INTERFACE)) != 0)) {
            Scope s = c.locals();
            for (Definition e = s.elems; e != null; e = e.sibling)
                if ((e.def.kind == FUN) && ((e.def.modifiers & ABSTRACT) != 0)) {
                    Definition absfun = e.def;
                    Definition implfun = definitions.implementation(absfun, root);
                    /* System.out.println("firstUndef(" + absfun.name + ": " +
                                       absfun.type + " in " + absfun.owner + ", " +
                                       root + ") = " + ((implfun == null) ? "" +
                                       implfun : "" + implfun.type)); */
                    if ((implfun == null) || (implfun == absfun))
                        return absfun;
                } else if ((e.def.kind == TYP) &&
                           ((e.def.modifiers & ABSTRACT) != 0) &&
                           (c != root) &&
                           ((c.modifiers & INTERFACE) != 0) &&
                           ((e.def.modifiers & CLASSFIELD) != 0)) {
                    Definition absclass = e.def;
                    Definition implclass = definitions.implementation(absclass, root);
                    if ((implclass == null) || (implclass == absclass))
                        return absclass;
                }
            if ((c != root) &&
                ((root.modifiers & MODULE) != 0) &&
                ((c.modifiers & (MODULE | INTERFACE)) == (MODULE | INTERFACE))) {
                ModuleIntf mod = ((XClassDef)root).modIntf;
                XClassDef[] subs = ((XClassDef)c).modIntf.contained.keys();
                for (int i = 0; i < subs.length; i++) {
                    int mods = ((XClassDef)c).modIntf.modsOfContained(subs[i]);
                    if ((mods & PRIVATE) != 0)
                        continue;
                    else if (!mod.contained.contains(subs[i]))
                        return subs[i];
                    else {
                        int m = mod.modsOfContained(subs[i]);
                        if (((m & (PRIVATE | ABSTRACT)) != 0) ||
                            (((mods & PUBLIC) != 0) && ((m & PUBLIC) == 0)))
                            return subs[i];
                    }
                }
            }
            if (c.supertype() != null) {
                undef = firstUndef(root, c.supertype().tdef());
                int i = 0;
                while ((undef == null) && (i < c.interfaces().length))
                    undef = firstUndef(root, c.interfaces()[i++].tdef());
            }
        }
        return undef;
    }

/** check that all interface members of ic are implemented in root
 */
    public void checkImplementations(int pos, Definition root, Definition ic) {
        checkImplementations(pos, true, root, ic);
    }
    
    protected void checkImplementations(int pos, boolean checkMods,
                                        Definition root, Definition ic) {
        // check all definitions in ic
        for (Definition e = ic.locals().elems; e != null; e = e.sibling) {
            if ((e.def.kind == FUN) && ((e.def.modifiers & STATIC) == 0)) {
                Definition absfun = e.def;
                Definition implfun = definitions.implementation(absfun, root);
                if (implfun != null)
                    checkOverridden(pos, implfun, absfun);
            } else if ((e.def.kind == TYP) && ((e.def.modifiers & CLASSFIELD) != 0)) {
                Definition absclass = e.def;
                Definition implclass = definitions.implementation(absclass, root);
                if (implclass != null) {
                    checkOverriddenClassField(pos, implclass, absclass);
                    Type[] ifc = absclass.interfaces();
                    if (ifc != null) {
                        for (int i = 0; i < ifc.length; i++)
                            if (!((XTypes)types).javaSubtype(implclass.type, ifc[i]))
                                report.error(pos, implclass + " has to implement " +
                                             ifc[i] + " defined in " + ic);
                    }
                }
            }
        }
        // for modules we have to check that all submodules of ic are
        // implemented in root
        if ((ic.modifiers & MODULE) != 0) {
            ModuleIntf mod = ((XClassDef)root).modIntf;
            XClassDef[] subs = ((XClassDef)ic).modIntf.contained.keys();
            for (int i = 0; i < subs.length; i++) {
                int mods = ((XClassDef)ic).modIntf.modsOfContained(subs[i]);
                if ((mods & PRIVATE) != 0)
                    continue;
                else if (!mod.contained.contains(subs[i]))
                    report.error(pos, root + " does not provide submodule " + subs[i] +
                                 " defined in " + ic);
                else {
                    int m = mod.modsOfContained(subs[i]);
                    if ((m & (PRIVATE | ABSTRACT)) != 0)
                        report.error(pos, root + " does not provide accessible submodule " +
                                     subs[i] + " defined in " + ic);
                    else if (((mods & PUBLIC) != 0) && ((m & PUBLIC) == 0))
                        report.error(pos, root + " does not provide public submodule " +
                                     subs[i] + " defined in " + ic);
                }
            }
        }
        // check all definitions inherited to ic
        Type[] is = ic.interfaces();
        for (int i = 0; i < is.length; i++)
            checkImplementations(pos, true, root, is[i].tdef());
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
                (((XClassDef)fargs[i].tdef()).baseClass ==
                ((XClassDef)oargs[i].tdef()).baseClass) &&
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
    public void checkOverload(int pos, Definition f) {
        Definition  c = f.owner;
        for (Definition e = c.locals().lookup(f.name);
                e.scope != null; e = e.next())
            if ((f != e.def) && definitions.overloads(f, e.def)) {
                checkOverloaded(pos, f, e.def);
                return;
            }
    }

/** check, if a method 'f' of an algebraic superclass overrides another
 *  method
 */
    public void mustOverride(int pos, Definition f) {
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
    protected Type[] collectInterfaces(Type root) {
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
    public void noNewTypeIntro(int pos, Type[] is, Type root) {
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
}

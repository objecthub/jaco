//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type checking pass
//                           
//  [XAttribute.java (33216) 7-May-01 17:01 -> 22-Jun-01 23:19]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.util.*;
import Tree.*;
import XTree.*;
import Type.*;
import Definition.*;


public class XAttribute extends Attribute implements XModifierConst {
    
	/** global flags
	 */
    public boolean nocomplete;

	/** other components
	 */
    protected XTypes types;
    protected XTypeChecker checks;
    protected XDefinitions definitions;
    protected ModuleContexts modules;
    protected XTrees trees;
    
    public String getName() {
        return "XAttribute";
    }

    public String getDebugName() {
        return "xattrib";
    }
    
    public void init(SemanticContext context) {
        super.init(context);
        checks = (XTypeChecker)super.checks;
        definitions = (XDefinitions)super.definitions;
        types = (XTypes)super.types;
        trees = (XTrees)super.trees;
        modules = ((XMainContext)context.compilerContext.mainContext).ModuleContexts();
    }
    
    /** attribute tree as a declared module type (without completion)
     */
    public Type attribModuleType(Tree tree, ContextEnv env) {
        nocomplete = true;
        Type tpe = attribExpr(tree, env, TYP, Type.AnyType);
        //System.out.println(">>>>>>>>>>>>> attributed type = " + tpe);
        nocomplete = false;
        switch ((XType)tree.type.deref()) {
            case ClassType(_):
                if ((tree.type.tdef().modifiers & MODULE) != 0)
                    return tree.type;
                break;
            case ModuleType(_):
                return tree.type;
            case ErrType:
                return tree.type;
        }
        report.error(tree.pos, "module type expected, but " + tree.type + " found");
        return tree.type = Type.ErrType;
    }
    
    /** attribute tree as a module reference type (with completion)
     */
    public Type attribFullModuleType(Tree tree, ContextEnv env) {
        attribExpr(tree, env, TYP, Type.AnyType);
        switch ((XType)tree.type.deref()) {
            case ClassType(_):
                if ((tree.type.tdef().modifiers & MODULE) != 0)
                    return tree.type;
                break;
            case ModuleType(_):
                return  tree.type;
            case ErrType:
                return tree.type;
        }
        report.error(tree.pos, "module type expected, but " + tree.type + " found");
        return tree.type = Type.ErrType;
    }
    
    /** check that nested class c does not hide any other nested class with
     *  the same name in a superclass
     */
    public XClassDef[] checkClassOverriding(int pos, ClassDef c) {
        Set set = new HashSet();
        //System.out.println("check class " + c + " in " + c.owner);
        checkClassOverriding(pos,
            mangler.unmangleShort(c.name, c.fullname),
            c,
            (ClassDef)c.owner,
            set);
        return (XClassDef[])set.toArray(new XClassDef[set.size()]);
    }
    
    /** check that nested class c does not hide any other nested class with
     *  the same name in a superclass
     */
    public void checkClassOverriding(int pos, Name n, ClassDef c, ClassDef d, Set set) {
        while ((d.modifiers & MODULE) != 0) {
            if ((c.modifiers & CLASSFIELD) != 0) {
                Type[] intfs = d.interfaces();
                for (int i = 0; i < intfs.length; i++)
                    checkClassOverriding(pos, n, c, (ClassDef)intfs[i].tdef(), set);
            }
            Definition e = d.locals().lookup(n);
            while (e.scope != null) {
                if ((e.def != c) && (e.def.kind == TYP)) {
                    if ((e.def.modifiers & CLASSFIELD) == 0)
                        report.error(pos, n + " hides class " + e.def + " of module " + d);
                    else
                        ((XTypeChecker)checks).checkOverriddenClassField(pos, c, e.def);
                    set.add(e.def);
                    break;
                }
                e = e.next();
            }
            d = (ClassDef)d.supertype().tdef();
        }
    }

    /** attribute declarations
     */
    public void attribDecl(Tree tree, ContextEnv env) {
        trees.pushPos(tree.pos);
        switch (tree) {
            case ClassDecl(Name name, int mods, Tree ext, Tree[] implementing,
                           Tree[] members, ClassDef c):
                XClassDef xc = (XClassDef)c;
                XClassDecl xdecl = (XClassDecl)tree;
                report.info.nestedClasses = true;
                xc.ensureCache();
                // check class overriding
                if (((c.modifiers & STATIC) == 0) && ((c.owner.modifiers & MODULE) != 0)) {
                    if (xc.vcIntf == null)
                        checkClassOverriding(tree.pos, xc);
                    else
                        xc.vcIntf.overrides = checkClassOverriding(tree.pos, xc);
                }
                // check that we never extend classfields
                if ((c.supertype().tdef().modifiers & CLASSFIELD) != 0)
                    report.error(tree.pos, "class fields cannot be extended");
                // attribute class field
                if ((c.modifiers & CLASSFIELD) != 0) {
                    namer.fixupScope(tree.pos, c);
                    tree.type = c.type;
                    if (members == null) {
                        if (xdecl.superimpl == null) {
                            // we have an uninitialized class field
                            if ((c.modifiers & OPAQUE) == 0)
                                report.error(tree.pos, "definition of class field " + c + " must not be abstract");
                            //else if ((c.owner.modifiers & (ABSTRACT | INTERFACE)) == 0)
                            //    report.error(tree.pos, "module " + c.owner + " has to be declared abstract due to abstract class field " + c);
                        } else if (((c.owner.modifiers & INTERFACE) != 0) ||
                                   ((c.modifiers & OPAQUE) != 0))
                                report.error(tree.pos, "module field " + c + " is abstract");
                        else {
                            // we have a class alias
                            XClassDef cfi = xc.vcIntf.withClass;
                            ((XTypeChecker)checks).checkClassFieldInitializer(xdecl.superimpl.pos, cfi, xc);
                            Definition cfiOwner = cfi.owner.enclClass();
                            if (((cfi.modifiers & STATIC) == 0) &&
                                (cfiOwner.kind == TYP) &&
                                ((cfiOwner.modifiers & MODULE) == 0))
                                report.error(xdecl.superimpl.pos, "enclosing instance type of " + 
                                                      cfi + " is not in scope");
                        }
                    } else {
                        // we have an implicit class definition
                        XClassDef cfi = xc.vcIntf.withClass;
                        namer.fixupScope(tree.pos, cfi);
                        if ((cfi.modifiers & ABSTRACT) == 0)
                            checks.checkAllDefined(tree.pos, cfi);
                        ClassDecl t = (ClassDecl)trees.at(tree.pos).make(
                                trees.newdef.ClassDecl(name, cfi.modifiers, xdecl.superimpl,
                                                       implementing, members));
                        t.setDef(cfi);
                        ContextEnv localEnv = env.dup(t, env.info, new Scope(null, cfi));
                        localEnv.enclClass = t;
                        localEnv.outer = env;
                        localEnv.info.isStatic = false;
                        localEnv.info.scope.enter(
                            definitions.make.VarDef(FINAL, PredefConst.THIS_N, xc.type, cfi));
                        localEnv.info.scope.enter(
                            definitions.make.VarDef(FINAL, PredefConst.SUPER_N, cfi.supertype(), cfi));
                        // do we really need the next line?
                        //localEnv.info.scope.enter(cfi.proxy(name));
                        checks.checkImplementations(tree.pos, c);
                        for (int i = 0; i < members.length; i++)
                            attribDecl(members[i], localEnv);
                    }
                // attribute module
                } else if ((c.modifiers & MODULE) != 0) {
                    // check that required modules do not require anything that
                    // is contained in this module (consistency rule)
                    /* XClassDef[] reqs = xc.modIntf.required.keys();
                    for (int i = 0; i < reqs.length; i++)
                        if (reqs[i].modIntf != null) {
                            XClassDef incons = xc.modIntf.containsSome(reqs[i].modIntf.required);
                            if (incons != null)
                                report.error(tree.pos, "required module " + reqs[i] +
                                                       " requires " + incons +
                                                       " which is also nested in " + xc);
                        } */
                    super.attribDecl(tree, env);
                // attribute regular class
                } else
                    super.attribDecl(tree, env);
                break;
            case MethodDecl(Name name, _, Tree restype, _, _, Tree[] stats, MethodDef f):
                if (name == null) {
                    if (stats != null) {
                        ContextEnv initEnv = accountant.methodEnv((MethodDecl)tree, env);
                        attribExpr(stats[0], initEnv, VAL, restype.type);
                    }
                } else {
                    checks.checkOverload(tree.pos, f);
                    if (definitions.isAlgebraicSubclass(f.owner)) {
                        if (((f.modifiers & STATIC) == 0) && !f.isConstructor())
                            checks.mustOverride(tree.pos, f);
                    }
                    super.attribDecl(tree, env);
                }
                break;
            case ModuleFieldDecl(int mods, Tree impl, Tree[] intf):
                XClassDef mod;
                switch (impl) {
                    case Overrides(Tree m, _):
                        mod = (XClassDef)m.type.tdef();
                        break;
                    default:
                        mod = (XClassDef)impl.type.tdef();
                }
                // check that the context provides all required modules
                XClassDef encl = (XClassDef)env.enclClass.def().topLevelClass();
                if (mod.modIntf == null)
                    report.error(impl.pos, mod + " is not a module");
                else {
                    XClassDef missing = encl.modIntf.provides(mod.modIntf.required);
                    if (missing != null)
                        report.error(impl.pos, "module field initializer requires module " + missing);
                    // check consistency
                    else {
                        ModuleSet barrier = new ModuleSet(encl.modIntf.required);
                        barrier.add(encl);
                        ModuleSet outsideEncl =
                            mod.modIntf.required
                            .intersect(encl.modIntf.required)
                            .union(mod.modIntf.required
                                   .intersect(encl.modIntf.contained)
                                   .required()
                                   .intersect(encl.modIntf.required))
                            .closure();
                        ModuleSet insideEncl =
                            mod.modIntf.required
                            .intersect(encl.modIntf.contained)
                            .closureUntil(barrier);
                        insideEncl.add(mod);
                        ModuleSet res = outsideEncl.intersect(insideEncl);
                        //System.out.println(mod + " in " + encl + ": " + outsideEncl + " ==" + barrier + "=> " + insideEncl);
                        if (!res.isEmpty())
                            report.error(impl.pos, "illegal submodule definition of " + mod +
                                                   "; conflicting modules: " + res);
                    }
                }
                break;
            case Import(_, _):
                break;
            default:
                super.attribDecl(tree, env);
        }
        trees.popPos();
    }
    
    /** return the result type of a `new ctype(argtypes)' constructor expression
     *  where constrtype is the type of the constructor function. This requires
     *  work since a constructor's return type as recorded in the definition table
     *  is always `void'.
     */
    protected Type constrType(int pos, Type ctype, Type[] argtypes,
                        ContextEnv env, Type constrtype) {
        switch (constrtype) {
            case MethodType(Type[] formals, Type restype, Type[] thrown):
                checks.checkHandled(pos, thrown, env.info.reported);
                env.info.thrown = types.append(thrown, env.info.thrown);
                //if (!types.subtypes(argtypes, formals))
                //    throw new InternalError();
                //else
                    return ctype;
            case ErrType:
                    return Type.ErrType;
            default:
                throw new InternalError("constrtype " + constrtype);
        }
    }
    
    protected Type[] attribConstrCall(Tree tree, Type encltype, ContextEnv env,
                                      Definition c, Type[] argtypes) {
        if (definitions.isInnerclass(c)) {
            if (encltype == null) {
                //System.out.println("find outer instance for " + c);
                // enclosing class of c
                XClassDef cowner = (XClassDef)c.owner.enclClass();
                // current class environment
                XClassDef c1 = (XClassDef)env.enclClass.def;
                // if the enclosing class of c is a module...
                if ((cowner.modifiers & MODULE) != 0) {
                	// get the current module
                    c1 = (XClassDef)c1.topLevelClass();
                	// I do not understand the next line; therefore I commented it out
                    //cowner = (XClassDef)modules.inheritType(modules.moduleType(cowner, null), cowner, c1).tdef();
                    if (((c1.modifiers & MODULE) != 0) &&
                        c1.modIntf.provides(cowner)) {
                        Tree encl = trees.at(tree.pos).make(trees.ClassName(cowner));
                        //Tree encl = trees.at(tree.pos).make(
                        //    trees.newdef.Apply(
                        //        trees.newdef.Ident(moduleAccessorMethodName(cowner)),
                        //        trees.noTrees));
                        encltype = attribExpr(encl, env, TYP, Type.AnyType);
                        setEncl(tree, encl);
                    } else {
                        report.error(tree.pos, "outof.scope", cowner, c);
                        encltype = Type.ErrType;
                    }
                } else {
                    while ((c1.kind == TYP) && !definitions.subclass(c1, cowner))
                        c1 = (XClassDef)c1.owner.enclClass();
                    if (c1.kind == TYP) {
                        Tree encl = trees.at(tree.pos).make(
                                        trees.newdef.Self(trees.ClassName(c1), THIS));
                        encltype = attribExpr(encl, env, VAL, Type.AnyType);
                        setEncl(tree, encl);
                    } else {
                        report.error(tree.pos, "outof.scope", cowner, c);
                        encltype = Type.ErrType;
                    }
                }
            }
            //System.out.println(c + " has outer instance " + encltype);
            argtypes = types.prepend(encltype, argtypes);
        } else if ((c.modifiers & MODULE) != 0) {
            
        } else if (encltype != null)
            report.error(tree.pos, "no.innerclass", c);
        return argtypes;
    }
    
    /** attribute the signature of a method type
     */
    public Type attribSignature(Tree[] params, Tree res, Tree[] excs, ContextEnv env) {
        Type[] argtypes = new Type[params.length];
        for (int i = 0; i < params.length; i++) {
            Tree partype = params[i];
            switch (partype) {
                case VarDecl(_, _, Tree vartype, _, _):
                    partype = vartype;
            }
            argtypes[i] = attribNonVoidType(partype, env);
        }
        Type restype;
        if (res == null) {
            if (definitions.isInnerclass(env.enclClass.def))
                argtypes = types.prepend(modules.modularizeType(env.outer.enclClass.def.type), argtypes);
            restype = Type.VoidType;
        } else
            restype = attribType(res, env);
        Type[]  thrown = new Type[excs.length];
        for (int i = 0; i < excs.length; i++)
            thrown[i] = attribClassType(excs[i], env);
        return types.make.MethodType(argtypes, restype, thrown);
    }

    ////
    //// transform types and definitions
    ////
    
    /** this function gets called from attribExpr to normalize module
     *  definitions if the flag 'nocomplete' is set to false
     */
    public Definition normalizeDef(int pos, ContextEnv env, Definition def) {
        if (((def.modifiers & MODULE) == 0) || nocomplete)
            return def;
        XClassDef mod = (XClassDef)def;
        Type mt = ((XContextInfo)env.info).moduleType;
        // do we access module mod in an unqualified context?
        if (mt == null) {
            XClassDef from = (XClassDef)env.enclClass.def().topLevelClass();
            if ((from.modifiers & MODULE) == 0)
                report.error(pos, "cannot access module " + mod + " outside of a module");
            else if (from.refinementOf(mod))
                return from;
            else {
                XClassDef norm = from.modIntf.required.key(mod);
                if (norm != null)
                    return norm;
                norm = from.modIntf.contained.key(mod);
                if (norm != null)
                    return norm;
                norm = from.modIntf.imported.key(mod);
                if (norm != null)
                    return norm;
                report.error(pos, "cannot access module " + mod + " in " + from);
            }
        // we access module mod in a module selection
        } else {
            XClassDef from = (XClassDef)mt.tdef();
            if (from.modIntf == null)
                return def;
            XClassDef norm = from.modIntf.contained.key(mod);
            if (norm != null)
                return norm;
            else
                report.error(pos, "cannot select module " + mod + " from " + from);
        }
        return def;
    }
    
    /** given a definition 'def' and a module context 'senv', return a type for this
     *  definition in this context
     */
    public Type completeType(int pos, Definition def, Type senv, ContextEnv env) {
        //System.out.println("complete " + def + ": " + def.type + " with " +
        //                   senv + " in " + ((XContextInfo)env.info).moduleType + "/" + env.enclClass.def().topLevelClass());
        // for non-modules just return the plain type
        if (nocomplete || (def.kind >= BAD) || (senv == Type.ErrType))
            return def.type;
        if ((def.modifiers & MODULE) == 0) {
            if (senv == null)
                return modules.inheritType(def, env);
            else
                return modules.inheritType(def, senv);
        }
        XClassDef mod = (XClassDef)def;
        Type mt = ((XContextInfo)env.info).moduleType;
        // do we access module mod in an unqualified context?
        if (mt == null) {
            XClassDef from = (XClassDef)env.enclClass.def().topLevelClass();
            if ((from.modifiers & MODULE) == 0)
                report.error(pos, "cannot access module " + mod + " outside of a module");
            else if (from.modIntf.required.contains(mod))
                return modules.moduleType(mod, null);
            else if (from.modIntf.contained.contains(mod))
                return modules.moduleType(mod, modules.moduleType(from, null));
            else if (from.refinementOf(mod))
                return modules.moduleType(mod, null);
            else if (from.modIntf.imported.contains(mod))
                return (Type)from.modIntf.imported.get(mod);
            else
                report.error(pos, "cannot access module " + mod + " in " + from);
        // we access module mod in a module selection
        } else {
            XClassDef from = (XClassDef)mt.tdef();
            if (from.modIntf == null)
                return Type.ErrType;
            if (from.modIntf.contained.contains(mod))
                return modules.moduleType(mod, mt);
            report.error(pos, "cannot select module " + mod + " from " + from);
        }
        return Type.ErrType;
    }
    
    /*
    public Type typeForDef(int pos, Definition def, Type qualifier, ContextEnv env) {
        // catch errors
        if ((def.kind >= BAD) || (from == null) || (from == Type.ErrType))
            return Type.ErrType;
        // find the top-most construct in env
        XClassDef top = (XClassDef)env.enclClass.def().topLevelClass();
        // find innermost module of 'qualifier'
        Type qualmod = modules.innermostModule(qualifier);
        // find innermost module of 'def'
        Type defmod = modules.innermostModule(def.type);
        // leave if a proper type for def is not required
        if (nocomplete || (def.kind >= BAD) || (qualmod == Type.ErrType))
            return def.type;
        
        
        
        if ((def.modifiers & MODULE) == 0) {
            if (qualmod == null)
                return modules.modularizeType(def.type);
            else
                return modules.qualifyType(modules.modularizeType(def.type), qualmod);
        }
        XClassDef mod = (XClassDef)def;
        Type mt = ((XContextInfo)env.info).moduleType;
        // do we access module mod in an unqualified context?
        if (mt == null) {
            XClassDef from = (XClassDef)env.enclClass.def().topLevelClass();
            if ((from.modifiers & MODULE) == 0)
                report.error(pos, "cannot access module " + mod + " outside of a module");
            else if (from.modIntf.required.contains(mod))
                return modules.moduleType(mod, null);
            else if (from.modIntf.contained.contains(mod))
                return modules.moduleType(mod, modules.moduleType(from, null));
            else if (from.refinementOf(mod))
                return modules.moduleType(mod, null);
            else
                report.error(pos, "cannot access module " + mod + " in " + from);
        // we access module mod in a module selection
        } else {
            XClassDef from = (XClassDef)mt.tdef();
            if (from.modIntf == null)
                return Type.ErrType;
            if (from.modIntf.contained.contains(mod))
                return modules.moduleType(mod, mt);
            report.error(pos, "cannot select module " + mod + " from " + from);
        }
        return Type.ErrType;
        
        
        // are we currently within a module?
        if ((mod.modifiers & MODULE) != 0) {
            if (def.owner.kind == TYP) {
                // in what definition did we select?
                XClassDef in = (XClassDef)from.tdef();
                // are we in a class (or directly inside the module)?
                if ((in.modifiers & MODULE) == 0)
                    return createTypeForSelected(pos, def, in, mod, from, env);
            }
            return modules.refineType(completeType(pos, def, inMod, env), mod);
        } else
            return completeType(pos, def, inMod, env);
    }
    */
    
    /** create a type for definition 'def' selected from 'from' in environment 'env'
     */
    public Type createTypeForSelected(int pos, Definition def, Type from, ContextEnv env) {
        //System.out.println("create type for " + def + ": " + def.type + " selected from " + from);
        // catch errors
        if ((def.kind >= BAD) || (from == null) || (from == Type.ErrType))
            return Type.ErrType;
        // find the top-most construct
        XClassDef mod = (XClassDef)env.enclClass.def().topLevelClass();
        // find innermost module of 'from'
        Type inMod = modules.innermostModule(from);
        // are we currently within a module?
        if ((mod.modifiers & MODULE) != 0) {
            if (def.owner.kind == TYP) {
                // in what definition did we select?
                XClassDef in = (XClassDef)from.tdef();
                // are we in a class (or directly inside the module)?
                if ((in.modifiers & MODULE) == 0)
                    return createTypeForSelected(pos, def, in, mod, from, env);
            }
            return modules.refineType(completeType(pos, def, inMod, env), mod);
        } else
            return completeType(pos, def, inMod, env);
    }
    
    public Type createTypeForSelected(int pos, Definition def, XClassDef in, XClassDef mod,
                                      Type from, ContextEnv env) {
        // find superclass of 'in' where 'def' was found?
        Type inMod = modules.innermostModule(from);
        while (def.owner != in) {
            // try interfaces
            Type[] intfs = in.interfaces();
            for (int i = 0; i < intfs.length; i++) {
                XClassDef newin = (XClassDef)intfs[i].tdef();
                if (!modules.inModule(newin) && (def.owner == newin))
                    return completeType(pos, def, inMod, env);
                Type res = createTypeForSelected(pos, def, newin, mod,
                    modules.qualifyType(intfs[i], inMod), env);
                if (res != null)
                    return res;
            }
            // try supertype
            Type superIn = in.supertype();
            if (superIn == null)
                return null;
            in = (XClassDef)superIn.tdef();
            if (!modules.inModule(in) && (def.owner == in))
                return completeType(pos, def, inMod, env);
            from = modules.qualifyType(superIn, inMod);
            inMod = modules.innermostModule(from);
        }
        return modules.refineType(completeType(pos, def, inMod, env), mod);
    }

    
    /** create a type for definition 'def' in environment 'env'
     */
    public Type createType(int pos, Definition def, ContextEnv env) {
        //System.out.println("createType for " + def);
        // catch errors
        if (def.kind >= BAD)
            return Type.ErrType;
        // find the top-most construct of the current environment
        XClassDef mod = (XClassDef)env.enclClass.def().topLevelClass();
        // in what class did we lookup this definition?
        XClassDef in = (XClassDef)env.enclClass.def;
        // compute innermost class type
        Type from = thisType(in);
        // find innermost module type of type from
        Type inMod = modules.innermostModule(from);
        // are we currently within a module?
        if ((mod.modifiers & MODULE) != 0) {
            //System.out.println("  in module " + mod + " within class " + in);
            //System.out.println("  in innermost module " + mod + " and innermost class " + from);
            if (def.owner.kind == TYP) {
                // are we in a class (or directly inside the module)?
                if ((in.modifiers & MODULE) == 0)
                    return createType(pos, def, in, mod, from, env);
            }
            //System.out.println("  ==> " + def);
            return modules.refineType(completeType(pos, def, inMod, env), mod);
        } else
            return completeType(pos, def, inMod, env);
    }
    
    /** find superclass of 'in' where 'def' was found?
     */
    private Type createType(int pos, Definition def, XClassDef in,
                               XClassDef mod, Type from, ContextEnv env) {
        ContextEnv inEnv = env;
        Type inMod = modules.innermostModule(from);
        //System.out.println("createType for " + def + " with owner " + def.owner + " in " + in);
        while (def.owner != in) {
            // try interfaces
            //System.out.println("  try " + in);
            Type[] intfs = in.interfaces();
            for (int i = 0; i < intfs.length; i++) {
                XClassDef newin = (XClassDef)intfs[i].tdef();
                if (!modules.inModule(newin) && (def.owner == newin))
                    return completeType(pos, def, inMod, env);
                //System.out.println("    implements " + intfs[i]);
                Type res = createType(pos, def, newin, mod,
                    modules.qualifyType(intfs[i], inMod), inEnv);
                if (res != null)
                    return res;
            }
            // try supertype
            Type superIn = in.supertype();
            if (superIn == null) {
                if (inEnv.outer == null)
                    return null;
                // try outer scope
                while (inEnv.enclClass.def == inEnv.outer.enclClass.def)
                    if ((inEnv = (ContextEnv)inEnv.outer) == null)
                        return null;
                inEnv = (ContextEnv)inEnv.outer;
                in = (XClassDef)inEnv.enclClass.def;
                from = thisType(in);
                inMod = modules.innermostModule(from);
                continue;
            }
            in = (XClassDef)superIn.tdef();
            if (!modules.inModule(in) && (def.owner == in))
                return completeType(pos, def, inMod, env);
            from = modules.qualifyType(superIn, inMod);
            inMod = modules.innermostModule(from);
        }
        return modules.refineType(completeType(pos, def, inMod, env), mod);
    }
    
    ////
    //// attribute expressions
    ////
    
    /** return this type
     */
    public Type thisType(XClassDef def) {
        XClassDef mod = (XClassDef)def.topLevelClass();
        if (((mod.modifiers & MODULE) == 0) ||
            ((def.modifiers & MODULE) != 0))
            return def.type;
        else
            return modules.classType(def, modules.moduleType(mod, null));
    }
    
    protected Definition attribSelect(Select tree, Type stype, ContextEnv env, int kind, Type pt) {
        Name name = tree.selector;
        int pos = tree.pos;
        Definition c = stype.tdef();
        Definition def;
        switch ((XType)stype.deref()) {
            case PackageType():
                def = namer.resolveSelectFromPackage(c, name, env, kind);
                if (def.kind >= BAD)
                    def = namer.access(def, pos, definitions.formFullName(name, def));
                    break;
            case ArrayType(_):
            case ClassType(_):
            case ModuleType(_):
                namer.fixupScope(pos, c);
                def = namer.resolveSelectFromType(stype, name, env, kind & ~PCK, pt);
                if (def.kind >= BAD) {
                    if ((kind & TYP) != 0) {
                        Name pname = trees.fullName(tree.selected);
                        if (pname != PredefConst.ERROR_N) {
                            Definition p = namer.loadPackage(env, pname);
                            if (p.kind < BAD) {
                                Definition def1 = namer.resolveSelectFromPackage(
                                                    (PackageDef)p, name, env, kind);
                                if (def1.kind < BAD)
                                    def = def1;
                            }
                        }
                    }
                    def = namer.access(def, pos, stype, name, namer.protoArgs(pt));
                }
                break;
            case ErrType:
                def = Scope.errDef;
                break;
            default:
                report.error(pos, "deref", stype);
                def = Scope.errDef;
                break;
        }
        return def;
    }
    
    protected Type attribNewObj(NewObj tree, ContextEnv env,
                                Type encltype, Type ctype, Type[] argtypes) {
        //ctype = modules.modularizeType(ctype);
        //System.out.println("~~~~~~~~ " + ctype);
        Definition c = ctype.tdef();
        argtypes = attribConstrCall(tree, encltype, env, c, argtypes);
        if ((c.modifiers & (ABSTRACT | INTERFACE)) != 0) {
            report.error(tree.pos, "is.abstract", c);
            return Type.ErrType;
        } else {
            //System.out.println("argtypes = ");
            //for (int i = 0; i < argtypes.length; i++)
            //    System.out.println("  " + argtypes[i]);
            //System.out.println("resolve constructor " + ctype);
            Definition constr = resolveConstructor(tree.pos, ctype, env, argtypes);
            tree.constructor = constr;
            if ((constr.modifiers & DEPRECATED) != 0)
                warnDeprecated(tree.pos, constr);
            return constrType(tree.pos, ctype, argtypes, env, constr.type);
        }
    }
    
    public Type attribExpr(Tree tree, ContextEnv env, int kind, Type pt) {
        int ownkind = VAL;
        Type owntype = Type.ErrType;
        Type treetype = null;
        trees.pushPos(tree.pos);
        switch (tree) {
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def, _):
                owntype = super.attribExpr(tree, env, kind, pt);
                if ((owntype.tdef().modifiers & MODULE) != 0)
                    report.error(tree.pos, "modules cannot be instantiated");
                break;
            case Select(Tree selected, Name selector, _):
                //System.out.println("-->");
                if (selector == PredefConst.CLASS_N)
                   return super.attribExpr(tree, env, kind, pt);
                else {
                    trees.pushPos(tree.pos);
                    int skind = 0;
                    if ((kind & PCK) != 0)
                        skind |= PCK;
                    if ((kind & TYP) != 0)
                        skind |= TYP | PCK;
                    if ((kind & ~(TYP | PCK)) != 0)
                        skind |= VAL | TYP | PCK; // check this (new: PCK)
                    // attribute the qualifier
                    Type stype = attribExpr(selected, env, skind, Type.AnyType);
                    Definition sdef = selected.def();
                    // check if we are within an anonymous class field implementation
                    // TODO: allow this only for final class fields
                    // TODO: final class fields cannot be extended but refined
                    if (((stype.tdef().modifiers & CLASSFIELD) != 0) &&
                	    (env.enclClass.def == ((XClassDef)stype.tdef()).vcIntf.withClass)) {
                    	// correct the qualification
                    	stype = modules.modularizeType(env.enclClass.def.type);
                    }
                    env.info.selectSuper = (sdef != null) &&
                                                ((sdef.name == PredefConst.SUPER_N) ||
                                                (sdef.kind == TYP));
                    // the following line is a hack; it possibly determines the wrong
                    // method/field for super.<methodOrField> terms; for methods this
                    // gets corrected by a later re-attribution of the tree, but errors
                    // are also found later (too late).
                    if ((sdef != null) &&
                        (sdef.name == PredefConst.SUPER_N) &&
                        ((stype.tdef().modifiers & MODULE) != 0) &&
                        ((env.enclClass.def.modifiers & SPECIALIZES) != 0))
                        stype = createType(tree.pos, env.enclClass.def, env);
                    Definition def = attribSelect((Select)tree, stype, env, kind, pt);
                    def = normalizeDef(tree.pos, env, def);
                    tree.setDef(def);
                    ownkind = def.kind;
                    if (ownkind == VAR) {
                        checkInit(tree.pos, env, (VarDef)def);
                        if ((def.modifiers & FINAL) != 0 &&
                            ((sdef == null) ||
                             (sdef.name != PredefConst.THIS_N) && (sdef.kind != TYP) ||
                             frozen(def, env)))
                            ownkind = VAL;
                    }
                    if (env.info.selectSuper) {
                        if ((def.modifiers & STATIC) == 0) {
                            if (sdef.name == PredefConst.SUPER_N)
                                namer.checkNonAbstract(tree.pos, def);
                            else if ((def.owner != null) && ((def.owner.modifiers & MODULE) != 0)) {
                            } else
                                namer.checkStatic(tree.pos, def);
                            Type stype1 = types.mgb(env.enclClass.def.type, stype.tdef());
                            if (stype1 != null)
                                stype = stype1;
                        }
                        env.info.selectSuper = false;
                    }
                    if ((def.modifiers & DEPRECATED) != 0)
                        warnDeprecated(tree.pos, def);
                    owntype = createTypeForSelected(tree.pos, def, stype, env);
                }
                //System.out.println("<-- ?." + selector + ": " + owntype + " with owner " + owntype.tdef().owner);
                break;
            case ModuleSelect(Tree from, Name selector, _):
                //System.out.println("==>");
                trees.pushPos(tree.pos);
                Type mtype = attribFullModuleType(from, env);
                ContextEnv lenv = (ContextEnv)env.dup();
                lenv.tree = tree;
                ((XContextInfo)lenv.info).moduleType = mtype;
                owntype = attribExpr(new Tree.Ident(selector, null),
                                     lenv, PCK | TYP, Type.AnyType);
                ownkind = owntype.tdef().kind;
                //tree.setDef(owntype.tdef());
                //System.out.println("<== ?::" + selector + ": " + owntype + " with owner " + owntype.tdef().owner);
                break;
            case Ident(Name idname, _):
            	Type stype = null;
            	// first try to find it in the regular way
                Definition def = ((XNameResolver)namer).resolveIdentPartially(
                	tree.pos, idname, env, kind, pt);
               	// if we didn't find anything and we are inside of a module...
                if (def.kind >= BAD) {
                	// get the top-level module
                	XClassDef mod = (XClassDef)env.enclClass.def().topLevelClass();
                	if ((mod.modifiers & MODULE) != 0) {
                		// get the star imports from the top level module
                		Type[] starImports = (Type[])mod.modIntf.starImported.types();
                		// find first matching star import
                		for (int i = 0; i < starImports.length; i++) {
                			// check current star import scope
							Definition newdef =
								namer.resolveSelectFromType(starImports[i], idname,
								                            env, kind & ~PCK, pt);
							// if found...
							if (newdef.kind < BAD) {
							    def = newdef;
								stype = starImports[i];
								// save the reference to the corresponding module
								trees.replacements.put(tree, mod.modIntf.starImportedTrees.get(stype));
								// check for the absence of ambiguities
								for (int j = i + 1; j < starImports.length; j++)
									if (namer.resolveSelectFromType(starImports[j], idname,
											env, kind & ~PCK, pt).kind < BAD) {
										report.error(tree.pos, "ambiguous star import; both " +
											"modules " + starImports[i] + " and " +
											starImports[j] + " define " + idname);
										break;
									}
								break;
							}
                		}
                	}
                }
                if (def.kind >= BAD)
            		def = namer.access(def, tree.pos,
            			env.enclClass.def.type, idname, namer.protoArgs(pt));
                //System.out.println("~~ resolve ident " + idname + ": " + def + ", " + def.hashCode());
                def = normalizeDef(tree.pos, env, def);
                //System.out.println("~~     refined: " + def + ", " + def.hashCode());
                tree.setDef(def);
                ownkind = def.kind;
                if (ownkind == VAR) {
                    VarDef  v = (VarDef)def;
                    checkInit(tree.pos, env, v);
                    if (((v.modifiers & FINAL) != 0) && frozen(v, env))
                        ownkind = VAL;
                    if ((v.owner.kind != TYP) && (v.owner != env.info.scope.owner) &&
                        ((v.modifiers & CAPTURED) == 0)) {
                        v.modifiers |= CAPTURED;
                        if ((v.modifiers & FINAL) == 0)
                            report.error(tree.pos, "access.to.uplevel", v);
                    }
                    if (kind == VAR) {
                        if ((v.modifiers & CAPTURED) == 0)
                            v.adr = tree.pos;
                    } else
                        v.modifiers |= USED;
                }
                if ((def.modifiers & STATIC) == 0) {
                    if (namer.existsStatic(def.owner, env))
                        namer.checkStatic(tree.pos, def);
                    else if ((def.owner != null) &&
                        (def.owner.kind == TYP) && env.info.isSelfArgs)
                            report.error(tree.pos, "call.superconstr.first", def);
                }
                if ((def.modifiers & DEPRECATED) != 0)
                    warnDeprecated(tree.pos, def);
                if (stype == null)
                	owntype = createType(tree.pos, def, env);
                else
                	owntype = createTypeForSelected(tree.pos, def, stype, env);
                //System.out.println("*** " + idname + ": " + owntype + " with owner " + owntype.tdef().owner);
                break;
            case Self(Tree clazz, int tag, _):
                Definition def;
                Name name = (tag == THIS) ? PredefConst.THIS_N : PredefConst.SUPER_N;
                if (env.info.isSelfArgs) {
                    report.error(tree.pos, "call.superconstr.first", name);
                    def = Scope.errDef;
                } else {
                    Definition  cc = ((clazz == null) || env.info.isSelfCall) ?
                        env.enclClass.def : attribType(clazz, env).tdef();
                    def = namer.resolveSelf(tree.pos, tag, cc, env);
                    if (def.type != Type.ErrType) {
                        if (kind == FUN) {
                            if (env.info.isSelfCall) {
                                env.info.isSelfCall = false;
                                Type clazztype;
                                //System.out.println("========");
                                //System.out.println("== " + modules.modularizeType(def.type));
                                if (clazz == null)
                                    clazztype = null;
                                else if ((def.type.tdef().owner.modifiers & MODULE) != 0)
                                    clazztype = attribExpr(clazz, env, TYP, Type.AnyType);
                                else
                                    clazztype = attribExpr(clazz, env, VAL, Type.AnyType);
                                Type[] argtypes = attribConstrCall(
                                    tree, clazztype, env,
                                    def.type.tdef(), pt.argtypes());
                                def = resolveConstructor(tree.pos, modules.modularizeType(def.type), env, argtypes);
                                ownkind = FUN;
                                env.info.isSelfCall = true;
                            } else
                                report.error(tree.pos,"must.be.first.stat", name);
                        }
                        if (clazz != null)
                            report.info.nestedClasses = true;
                    }
                    if (namer.existsStatic(def.owner, env))
                        namer.checkStatic(tree.pos, def);
                    if (clazz != null)
                        owntype = createTypeForSelected(tree.pos, def, clazz.type, env);
                    else
                        owntype = createType(tree.pos, def, env);
                }
                tree.setDef(def);
                // owntype = modularizeType(owntype);
                // System.out.println("*** self: " + owntype + " with owner " + owntype.tdef().owner);
                break;
            case CompoundType(Tree[] fields):
                TypeSet ts = new TypeSet(types);
                for (int i = 0; i < fields.length; i++) {
                    Type tpe = attribClassType(fields[i], env);
                    if ((tpe != Type.ErrType) &&
                        ((tpe.tdef().modifiers & INTERFACE) == 0))
                        report.error(fields[i].pos, "interface type expected as compound type member");
                    else if (tpe != Type.ErrType)
                        ts.add(tpe);
                }
                ownkind = TYP;
                Type[] ct = ts.types();
                if (ct.length == 0)
                    owntype = types.objectType;
                else if (ct.length == 1)
                    owntype = ct[0];
                else {
                    owntype = ((XType.Factory)types.make).CompoundType(ct);
                    owntype.setDef(ct[0].tdef());
                }
                break;
            default:
                trees.popPos();
                return super.attribExpr(tree, env, kind, pt);
        }
        if ((owntype != Type.ErrType) &&
            definitions.checkKind(tree.pos, ownkind, kind))
            tree.type = checks.checkType(tree.pos, owntype, pt);
        else
            tree.type = Type.ErrType;
        trees.popPos();
        return tree.type;
    }
    
    ////
    //// attribute pattern-matching
    ////
    
    public Type attribStat(Tree tree, ContextEnv env, Type pt, Type sofar) {
        switch (tree) {
            case Switch(Tree selector, Case[] cases):
                trees.pushPos(tree.pos);
                tree.type = sofar;
                Type selpt = selectorType(selector.pos, null);
                Type seltype = selectorType(selector.pos,
                    attribExpr(selector, env, VAL, selpt));
                ContextEnv switchEnv = env.dup(tree);
                Hashtable tags = new Hashtable();
                boolean hasDefault = false;
                for (int i = 0; i < cases.length; i++) {
                    Case c = cases[i];
                    ContextEnv caseEnv = switchEnv.dup(c);
                    for (int j = 0; j < c.pat.length; j++) {
                        if (c.pat[j] != null) {
                            Type pattype = attribPattern(c.pat[j], caseEnv, seltype);
                            if (seltype != Type.ErrType) {
                                if (isDuplicateCase(pattype, tags))
                                    report.error(c.pos, "duplicate.label", "case");
                            }
                        } else if (hasDefault)
                            report.error(c.pos, "duplicate.label", "default");
                        else
                            hasDefault = true;
                    }
                    sofar = attribStats(c.stats, caseEnv, pt, sofar);
                    env.info.thrown = types.append(caseEnv.info.thrown, env.info.thrown);
                    Definition e = caseEnv.info.scope.elems;
                    caseEnv.info.scope.leave();
                    if ((seltype.tdef().modifiers & ALGEBRAIC) == 0) {
                        while (e != null) {
                            if (e.def.kind == VAR)
                                enterShadow(tree.pos, switchEnv.info.scope, (VarDef)e.def);
                            e = e.sibling;
                        }
                        e = switchEnv.info.scope.elems;
                        while (e != null) {
                            VarDef v = (VarDef)e.def;
                            e = e.sibling;
                            if (v.adr > 0) {
                                switchEnv.info.scope.remove(v);
                                v.modifiers &= ~ABSTRACT;
                                c.stats = trees.append(makeShadowDef(tree.pos, v), c.stats);
                                enterShadow(tree.pos, switchEnv.info.scope, v);
                            }
                        }
                    }
                }
                switchEnv.info.scope.leave();
                trees.popPos();
                return sofar;
            default:
                return super.attribStat(tree, env, pt, sofar);
        }
    }
    
    /** determine the selector type of a switch statement
     */
    protected Type selectorType(int pos, Type seltype) {
        if (seltype == null)
            return Type.AnyType;
        Type    t = seltype.deref();
        switch (t) {
            case NumType(int tag):
                if ((tag >= MIN_BASICTYPE_TAG) && (tag <= INT))
                    return types.intType;
                break;
            case ClassType(_):
                if ((t = types.algebraicSupertype(t)) != null)
                    return t;
                break;
            case ErrType:
                return Type.ErrType;
        }
        report.error(pos, seltype + " is not a valid switch selector type");
        return Type.ErrType;
    }
    
    protected Definition findCaseDef(int pos, Name name, ContextEnv env, int kind, Type pt) {
        Definition  def;
        if (kind == FUN)
            def = ((XNameResolver)namer).findCase(env, pt, name);
        else
            def = namer.findField(env, pt.tdef(), name);
        if (def.kind >= BAD)
            def = Scope.errDef;
        else if ((def.owner != pt.tdef()) || ((def.modifiers & CASEDEF) == 0))
            def = Scope.errDef;
        return def;
    }
    
    /** find a definition representing the case given by 'tree'
     */
    protected Definition findCase(Tree tree, ContextEnv env, int kind, Type pt) {
        Definition res = Scope.errDef;
        tree.setDef(res);
        tree.type = res.type;
        switch (tree) {
            case Ident(Name name, _):
                Type pt1 = pt;
                while ((res == Scope.errDef) && types.isAlgebraicType(pt1)) {
                    res = findCaseDef(tree.pos, name, env, kind, pt1.tdef().owner.type);
                    pt1 = pt1.supertype();
                }
                if (res == Scope.errDef) {
                    Definition cas = namer.findType(env, name);
                    if (cas.kind >= BAD) {
                        report.error(tree.pos, "case " + name + " not found");
                        break;
                        //return res;
                    } else if ((cas.modifiers & CASEDEF) == 0) {
                        report.error(tree.pos, name + " is not a case of an algebraic type");
                        break;
                        //return res;
                    } else if (!types.subtype(pt1 = types.algebraicSupertype(cas.type), pt) &&
                               !types.subtype(pt, pt1)) {
                        report.error(tree.pos, "incompatible case of " + pt);
                        break;
                        //return res;
                    }
                    while ((res == Scope.errDef) && types.isAlgebraicType(pt1)) {
                        res = findCaseDef(tree.pos, name, env, kind, pt1.tdef().owner.type);
                        pt1 = pt1.supertype();
                    }
                    if (res == Scope.errDef)
                    	report.error(tree.pos, "case " + name + " not found");
                }
                break;
            case Select(Tree selected, Name name, _):
                Type pt0 = attribType(selected, env);
                if (!types.isModule(pt0)) {
                    report.error(tree.pos, "case selector has to denote a module");
                    return res;
                }
                while ((res == Scope.errDef) && types.isModule(pt0)) {
                    res = findCaseDef(tree.pos, name, env, kind, pt0);
                    pt0 = pt0.supertype();
                }
                break;
        }
        tree.setDef(res);
        tree.type = res.type;
        return res;
    }
    
    /** attribute pattern 'tree' in environment 'env', where 'pt' is the
     *  expected type
     */
    public Type attribPattern(Tree tree, ContextEnv env, Type pt) {
        Type pattype = pt;
        switch (tree) {
            case VarDecl(Name name,_, Tree vartype, _, _):
                // check if it's not a blank
                if (name != null) {
                    attribNonVoidType(vartype, env);
                    VarDef v = enterVar((VarDecl)tree, env);
                    // check consistency; allow subtype constraints
                    if (!types.sametype(v.type, pt) &&
                        !(types.isAlgebraicType(pt) &&
                          types.isCaseType(v.type) &&
                          types.subtype(v.type, pt))) {
                        checks.typeError(tree.pos, "pattern argument has wrong type",
                                         v.type, pt);
                        pattype = Type.ErrType;
                    }
                }
                break;
            case Apply(Tree fn, Tree[] args):
                switch (pt.deref()) {
                    case ClassType(_):
                        Definition casedef = findCase(fn, env, FUN, pt);
                        Type[] patargs = null;
                        if (casedef.type != Type.ErrType) {
                            Definition  ct = namer.findMemberType(env, casedef.owner, casedef.name);
                            report.assert(ct.kind < BAD);
                            pattype = ct.type;
                            patargs = casedef.type.argtypes();
                            if (patargs.length != args.length) {
                                report.error(tree.pos, "wrong number of arguments in pattern");
                                pattype = Type.ErrType;
                            }
                        }
                        else
                            pattype = Type.ErrType;
                       	Type site = (casedef.type == Type.ErrType) ?
                       		Type.ErrType
                       	  : createType(tree.pos, casedef.owner, env);
                        if (pattype != Type.ErrType)
                            for (int i = 0; i < args.length; i++)
                                attribPattern(args[i], env, modules.migrateType(
                                    modules.modularizeType(patargs[i]), casedef, site, env));
                        break;
                    default:
                        if (pt != Type.ErrType)
                            report.error(tree.pos, "constant expression required");
                        pattype = Type.ErrType;
                }
                break;
            case Ident(_, _):
            case Select(_, _, _):
                switch (pt.deref()) {
                    case ClassType(_):
                        findCase(tree, env, VAL, pt);
                        pattype = tree.type;
                        break;
                    default:
                        pattype = attribConstExpr(tree, env, pt);
                }
                break;
            default:
                pattype = attribConstExpr(tree, env, pt);
        }
        tree.type = pattype;
        return pattype;
    }
}

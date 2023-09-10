//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    enter members pass
//                           
//  [XEnterMembers.java (22171) 8-May-01 16:24 -> 8-Jul-01 23:35]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import XTree.*;
import Definition.*;


public class XEnterMembers extends EnterMembers
                           implements XModifierConst, AlgebraicSupportConst {

	/** helper components
	 */
    protected Constants constants;
    protected XDefinitions definitions;
    protected XTypes types;
    protected PrettyPrinter pretty;
    protected XTypeChecker checks;
    protected Mangler mangler;
    protected XEnterClasses classes;
    protected ModuleContexts modules;
    
    protected Set allModules = new HashSet();
    
	/** constructors in modules
	 */
    protected HashMap caseConstr = new HashMap();
    
    
    public String getName() {
        return "XEnterMembers";
    }
    
    public void init(SemanticContext context) {
        super.init(context);
        constants = context.compilerContext.mainContext.Constants();
        pretty = context.compilerContext.mainContext.PrettyPrinter();
        mangler = context.compilerContext.mainContext.Mangler();
        checks = (XTypeChecker)context.TypeChecker();
        definitions = (XDefinitions)super.definitions;
        types = (XTypes)super.types;
        classes = (XEnterClasses)((XSemanticContext)context).EnterClasses();
        modules = ((XMainContext)context.compilerContext.mainContext).ModuleContexts();
    }
    
    ////
    //// first sub-pass: initialize algebraic types
    ////
    
    public TreeList enter(TreeList treelist) throws AbortCompilation {
        TreeList res = super.enter(treelist);
        ClassDecl[] cds = (ClassDecl[])classes.newAlgebraics.keySet().
            toArray(new ClassDecl[classes.newAlgebraics.size()]);
        Set defset = new HashSet();
        for (int i = 0; i < cds.length; i++) {
            ContextEnv env = (ContextEnv)classes.newAlgebraics.get(cds[i]);
            ContextEnv localEnv = accountant.classEnv(cds[i], env);
            XClassDef xcd = (XClassDef)cds[i].def();
            if (cds[i].extending == null)
                xcd.setSupertype(types.objectType);
            else {
                Type st = attribute.attribClassType(cds[i].extending, localEnv);
                xcd.setSupertype(st);
            }
            defset.add(cds[i].def());
        }
        for (int i = 0; i < cds.length; i++) {
            XClassDef cd = (XClassDef)cds[i].def();
            XClassDef s = (XClassDef)cd.supertype().tdef();
            cd.baseClass = (XClassDef)definitions.algebraicBase(cd);
            //System.out.println("base class of " + cd + " is " + cd.baseClass);
            while (((s.modifiers & ALGEBRAIC) != 0) && defset.contains(s)) {
                cd.tag += s.tag;
                s = (XClassDef)s.supertype().tdef();
            }
            if ((s.modifiers & ALGEBRAIC) != 0)
                cd.tag += s.tag;
            defset.remove(cd);
        }
        return res;
    }
    
    protected Tree caseToVar(int pos, Name name, int mods, int tag, Definition algebraic) {
        Tree init = trees.at(pos).make(trees.New(classes.variableName(algebraic),
                                trees.newdef.Literal(constants.make.IntConst(tag))));
        return trees.at(pos).make(trees.newdef.VarDecl(
                        name,
                        (mods & modifiers.AccessMods) | CASEDEF,
                        trees.newdef.Ident(classes.variableName(algebraic)),
                        init));
    }
    
    protected Tree genConstructor(ClassDecl c) {
        if (c.name.endsWith(VAR_CASE_N))
            return null;
        MethodDecl realConstr = (MethodDecl)c.members[c.members.length - 1];
        return trees.at(c.pos).make(trees.newdef.MethodDecl(
            c.name, c.mods | CASEDEF, trees.newdef.Ident(c.name),
            (VarDecl[])trees.copy(realConstr.params,
                new VarDecl[realConstr.params.length]),
            trees.noTrees,
            new Tree[]{trees.newdef.Return(
                trees.newdef.NewObj(null,
                    trees.newdef.Ident(c.name),
                    trees.Idents(realConstr.params), null))}));
    }
    
    protected Tree genBool(boolean val) {
        return trees.Ident(val ? constants.trueConst : constants.falseConst);
    }
    
    protected Tree[] genTagAndConstr(int pos, XClassDef owner) {
        int old = trees.setPos(pos);
        boolean base = (owner == owner.baseClass);
        XClassDef extendsalgeb = (XClassDef)owner.supertype().tdef();
        VarDecl[] param = new VarDecl[]{(VarDecl)trees.newdef.VarDecl(
                                TAG_N, 0, trees.toTree(types.intType), null)};
        Tree tag = base ? trees.newdef.VarDecl(
            TAG_N,
            PUBLIC | FINAL | SYNTHETIC,
            trees.toTree(types.intType), null) : null;
        Tree supercall = false ? //((extendsalgeb.owner.modifiers & MODULE) != 0) ?
            trees.newdef.Exec(
                trees.newdef.Apply(
                    trees.newdef.Self(trees.toTree(extendsalgeb.owner.type), SUPER),
                    trees.Idents(param))) :
            trees.SuperCall(false, param);
        Tree constr = trees.newdef.MethodDecl(
            PredefConst.INIT_N,
            PROTECTED | SYNTHETIC,
            null,
            param,
            trees.noTrees,
            base ?  new Tree[]{trees.FieldInit(TAG_N)} :
                    new Tree[]{supercall});
        trees.setPos(old);
        if (base)
            return new Tree[]{tag, constr};
        else
            return new Tree[]{constr};
    }
        
    ////
    //// second sub-pass: enter class/module members
    ////
    
    public Definition memberEnter(Tree tree, ContextEnv env) {
        trees.pushPos(tree.pos);
        Definition result = null;
        main: switch (tree) {
            case ClassDecl(Name name, int mods, Tree ext, Tree[] implementing,
                           Tree[] members, ClassDef c):
                result = c;
                if (accountant.todo.get(c) == null)
                    break;
                XClassDef xcd = (XClassDef)c;
                XClassDecl cdecl = (XClassDecl)tree;
                // check if we have a class field
                if ((mods & CLASSFIELD) != 0) {
                    ContextEnv localEnv = (ContextEnv)accountant.classEnvs.get(c);
                    if (localEnv == null) {
                        // setup supertype
                        localEnv = accountant.classEnv((ClassDecl)tree, env);
                        accountant.classEnvs.put(c, localEnv);
                        if (ext == null)
                            xcd.vcIntf.depends = new Type[0];
                        else
                            switch (ext) {
                                case CompoundType(Tree[] fields):
                                    Type[] depends = new Type[fields.length];
                                    for (int i = 0; i < fields.length; i++)
                                        depends[i] = attribDependency(fields[i], c, localEnv);
                                    //System.out.println("found " + depends.length + " dependencies for " + xcd);
                                    xcd.vcIntf.depends = depends;
                                    break;
                                default:
                                    xcd.vcIntf.depends = new Type[]{attribDependency(ext, c, localEnv)};
                            }
                        c.setSupertype(types.objectType);
                        // setup implemented interfaces
                        Type[] is = new Type[implementing.length];
                        for (int i = 0; i < is.length; i++)
                            is[i] = attribSuper(implementing[i], localEnv, true);
                        c.setInterfaces(is);
                        namer.fixupScope(tree.pos, c);
                        accountant.todo.remove(c);
                        accountant.classEnvs.remove(c);
                        // propagate members from interfaces
                        Scope scope = c.locals();
                        for (int i = 0; i < is.length; i++) {
                            Definition e = is[i].tdef().locals().elems;
                            while (e != null) {
                                if ((e.def.kind == FUN) &&
                                    ((e.def.modifiers & STATIC) == 0)) {
                                    Definition f = definitions.make.MethodDef(
                                        PUBLIC, e.def.name, null, c);
                                    if ((e.def.name == null) ||
                                        (e.def.name == PredefConst.INIT_N)) {
                                        f.type = types.make.MethodType(
                                            types.prepend(
                                                modules.modularizeType(c.owner.type),
                                                e.def.type.argtypes()),
                                            e.def.type.restype(),
                                            e.def.type.thrown());
                                        scope.enter(f); // only enter constructors
                                    } else
                                        f.type = e.def.type;
                                    if (isUnique(f, scope)) {
                                        //System.out.println("propagating " + f + ": " + f.type + " to " + f.owner + " / " + f.hashCode());
                                        //scope.enter(f);
                                    }
                                }
                                e = e.sibling;
                            }
                        }
                        // attribute right-hand-side
                        if (members != null) {
                            // now computer supertype and interfaces
                            XClassDef sc = xcd.vcIntf.withClass;
                            ClassDecl t = (ClassDecl)trees.at(tree.pos).make(
                                trees.newdef.ClassDecl(name, sc.modifiers, ext,
                                    implementing, members));
                            t.setDef(sc);
                            localEnv = env.dup(t, env.info, new Scope(null, sc));
                            localEnv.enclClass = t;
                            localEnv.outer = env;
                            localEnv.info.isStatic = false;
                            // do we really need the next expression?
                            //localEnv.info.scope.enter(sc.proxy(c.name));
                            // find correct supertype
                            if (cdecl.superimpl != null) {
                                XClassDef de = (XClassDef)attribSuper(
                                    cdecl.superimpl, localEnv, false).tdef();
                                if ((de.modifiers & (CLASSFIELD | ABSTRACT)) ==
                                        (CLASSFIELD | ABSTRACT))
                                    report.error(cdecl.superimpl.pos,
                                        "abstract class field not allowed " +
                                        "as super implementation");
                                else
                                    while ((de.modifiers & CLASSFIELD) != 0) {
                                        de = de.vcIntf.withClass;
                                        if ((de.modifiers & (CLASSFIELD | ABSTRACT)) ==
                                                (CLASSFIELD | ABSTRACT)) {
                                            report.error(ext.pos, "abstract class " +
                                                "field not allowed as super implementation");
                                            break;
                                        }
                                    }
                                sc.setSupertype(de.type);
                            }
                            // enter interfaces
                            for (int i = 0; i < implementing.length; i++)
                                memberEnter(implementing[i].type.tdef());
                            sc.setInterfaces(is);
                            // fix ClassDecl
                            //System.out.println("set supertype of " + sc + " to " + ssc);//DEBUG
                            namer.fixupScope(tree.pos, sc);
                            // add constructor if necessary
                            if (!hasConstructors(sc, members)) {
                                //System.out.println("default constr for " + sc);
                                Tree constr = trees.at(tree.pos).make(
                                            trees.DefaultConstructor(
                                            PUBLIC, //c.modifiers & modifiers.AccessMods,
                                            false, types.noTypes));
                                ((ClassDecl)tree).members = members =
                                            trees.append(members, constr);
                            }
                            for (int i = 0; i < members.length; i++)
                                switch (members[i]) {
                                    case ClassDecl(_, _, _, _, _, _):
                                        break;
                                    default:
                                        memberEnter(members[i], localEnv);
                                }
                            if ((sc.modifiers & ABSTRACT) != 0)
                                implementInterfaceMethods((ClassDecl)tree, sc, localEnv);
                            for (int i = 0; i < members.length; i++) {
                                switch (members[i]) {
                                    case ClassDecl(_, _, _, _, _, _):
                                        memberEnter(members[i], localEnv);
                                }
                                if ((members[i].mods() & STATIC) != 0)
                                    report.error(members[i].pos, "static.members.in.innerclass");
                            }
                        } else if (xcd.vcIntf.withClass != null) {
                            if (((xcd.vcIntf.withClass.modifiers & ABSTRACT) != 0) &&
                                ((xcd.vcIntf.withClass.modifiers & ABSTRACT) == 0))
                                report.error(tree.pos, "class field implementation is abstract");
                        }
                    }
                // attribute the rest (classes and modules)
                } else {
                    Tree[] reqs = cdecl.required;
                    // extra module checks
                    if (((mods & MODULE) != 0) && (accountant.todo.get(c) != null)) {
                        allModules.add(c);
                        report.useEnv(env.toplevel.info);
                        // check that modules do not have static members
                        for (int i = 0; i < members.length; i++)
                            if ((members[i].mods() & (STATIC | SYNTHETIC)) == STATIC) {
                                report.error(members[i].pos, "modules must not have static members");
                                members[i] = trees.at(members[i].pos).make(Tree.Bad());
                            }
                        // check that modules do not have constructors
                        if (hasConstructors(members))
                            report.error(tree.pos, "modules must not have constructors; use initializers instead");
                        
                        // check that modules are specialized consistently
                        //System.out.println(xcd.modIntf);
                        if ((c.supertype().tdef().modifiers & MODULE) != 0) {
                            ModuleIntf oldModIntf = ((XClassDef)xcd.supertype().tdef()).modIntf;
                            ModuleIntf newModIntf = xcd.modIntf;
                            // check requires
                            ModuleSet selfOver = new ModuleSet();
                            if (xcd.modIntf.specialized != null) {
                                selfOver.add(xcd.modIntf.specialized);
                                xcd.modIntf.overriddenRequired.put(xcd.modIntf.specialized, xcd);
                            }
                            xcd.modIntf.required.put(xcd, selfOver);
                            // check that 'xcd' gets specializes consistently
                            if ((xcd.modifiers & SPECIALIZES) != 0)
                                checkSpecConsistency(tree.pos, xcd);
                            // check contained
                            XClassDef[] oldcont = oldModIntf.contained.keys();
                            for (int i = 0; i < oldcont.length; i++) {
                                XClassDef newcont = (XClassDef)xcd.modIntf.overriddenContained.get(oldcont[i]);
                                XClassDef[] dep = oldcont[i].modIntf.required.keys();
                                for (int j = 0; j < dep.length; j++) {
                                    XClassDef fordep = (XClassDef)xcd.modIntf.overriddenRequired.get(dep[j]);
                                    if (fordep == null)
                                        fordep = (XClassDef)xcd.modIntf.overriddenContained.get(dep[j]);
                                    if (fordep != null) {
                                        if (fordep == xcd) {
                                            if (newcont == null)
                                                report.error(tree.pos, "module " + xcd + " specializes "
                                                                       + dep[j] + ", but submodule " +
                                                                       oldcont[i] + " still refers to " + dep[j]);
                                            else if (!newcont.modIntf.overriddenRequired.contains(dep[j]))
                                                report.error(tree.pos, "module " + xcd + " specializes "
                                                                       + dep[j] + ", but submodule " +
                                                                       newcont + " refers to " + dep[j]);
                                        } else if (newcont == null)
                                            report.error(tree.pos, "illegal specialization; module " + xcd + " defines " +
                                                                   fordep + " as " + dep[j] + ", but module " +
                                                                   oldcont[i] + " still refers to " + dep[j]);
                                        else if (!newcont.modIntf.overriddenRequired.contains(dep[j]))
                                            report.error(tree.pos, "illegal specialization; module " + xcd + " defines " +
                                                                   fordep + " as " + dep[j] + ", but specialized module " +
                                                                   newcont + " still refers to " + dep[j]);
                                    } else if ((newcont != null) &&
                                               oldModIntf.provides(dep[j]) &&
                                               newcont.modIntf.overriddenRequired.contains(dep[j]))
                                        report.error(tree.pos, "illegal specialization; module " + newcont  + " defines " +
                                                               newcont.modIntf.overriddenRequired.get(dep[j]) + " as " +
                                                               dep[j]);
                                }
                            }
                        }
                        xcd.modIntf.required.remove(xcd);
                        if (xcd.modIntf.specialized != null)
                            xcd.modIntf.overriddenRequired.remove(xcd.modIntf.specialized);
                        // enter rest
                        ContextEnv localEnv = accountant.classEnv((ClassDecl)tree, env);
                        accountant.classEnvs.put(c, localEnv);
                        // make sure we attribute superclasses and interfaces first
                        if (ext != null)
                            memberEnter(ext.type.tdef());
                        for (int i = 0; i < implementing.length; i++)
                            memberEnter(implementing[i].type.tdef());
                        // fixup scope
                        namer.fixupScope(tree.pos, c);
                        accountant.todo.remove(c);
                        if (((mods & INTERFACE) == 0) &&
                            !hasConstructors(members)) {
                            //System.out.println("generating constructor for " + c);
                            ((ClassDecl)tree).members = members =
                                    trees.append(members, trees.at(tree.pos).make(
                                            trees.DefaultConstructor(
                                                PUBLIC, //c.modifiers & modifiers.AccessMods,
                                                false, types.noTypes)));
                        }
                        // enter all module members
                        TreeList newMembers = new TreeList();
                        for (int i = 0; i < members.length; i++)
                            switch (members[i]) {
                                case ClassDecl(Name nam, _, _, _, _, ClassDef def):
                                    XClassDef xdef = (XClassDef)def;
                                    if ((xdef.modifiers & CASEDEF) != 0) {
                                        Tree constr = genConstructor((ClassDecl)members[i]);
                                        if (constr != null) {
                                            xdef.tag += ((XClassDef)xdef.baseClass.supertype().tdef()).tag;
                                            newMembers.append(constr);
                                            ((XMethodDef)memberEnter(constr, localEnv)).tag = xdef.tag;
                                        } else
                                            xdef.tag = -1;
                                    }
                                    break;
                                case CaseDecl(Name na, int mo, _, Tree[] inits, ClassDef owner):
                                    XEnterClasses.BaseAndTag bat =
                                        (XEnterClasses.BaseAndTag)classes.varCaseTags.get(members[i]);
                                    int tag = bat.tag + ((XClassDef)bat.base.supertype().tdef()).tag;
                                    members[i] = caseToVar(members[i].pos, na, mo, tag, bat.base);
                                    ((XVarDef)memberEnter(members[i], localEnv)).tag = tag;
                                    break;
                                default:
                                    memberEnter(members[i], localEnv);
                            }
                        newMembers.append(members);
                        ((ClassDecl)tree).members = newMembers.toArray();
                        if ((c.modifiers & ABSTRACT) != 0 &&
                            ((c.modifiers & INTERFACE) == 0))
                            implementInterfaceMethods((ClassDecl)tree, c, localEnv);
                        for (int i = 0; i < members.length; i++) {
                            switch (members[i]) {
                                case ClassDecl(_, _, _, _, _, _):
                                    memberEnter(members[i], localEnv);
                                    break;
                            }
                        }
                        accountant.classEnvs.remove(c);
                        report.usePreviousEnv();
                    } else if ((mods & MODULE) == 0) {
                        // fixup algebraic types
                        if ((mods & ALGEBRAIC) != 0) {
                            ((ClassDecl)tree).members = members =
                                trees.append(genTagAndConstr(tree.pos, xcd), members);
                            if ((xcd.modifiers & FINAL) != 0) {
                                ((ClassDecl)tree).mods &= ~FINAL;
                                ((ClassDecl)tree).mods |= ALG_FINAL;
                                xcd.modifiers = (xcd.modifiers ^ FINAL) | ALG_FINAL;
                            }
                            if ((xcd.supertype().tdef().modifiers & ALG_FINAL) != 0)
                                report.error(tree.pos, "cannot extend final algebraic class");
                        }
                        // enter regular class members
                        xcd = (XClassDef)super.memberEnter(tree, env);
                    }
                    ContextEnv localEnv = accountant.classEnv((ClassDecl)tree, env);
                    // module/context postprocessing
                    if ((ext != null) && (ext.type.tdef() != types.objectType.tdef())) {
                        if ((mods & MODULE) != 0) {
                            if ((ext.type.tdef().modifiers & MODULE) == 0)
                                report.error(ext.pos, "modules cannot extend classes");
                        } else {
                            if ((ext.type.tdef().modifiers & MODULE) != 0)
                                report.error(ext.pos, "classes cannot extend modules");
                        }
                    }
                    // check that modules only implement module interfaces
                    // and classes implement only regular interfaces
                    if ((mods & MODULE) != 0) {
                        Type[] itfs = xcd.type.interfaces();
                        for (int i = 0; i < itfs.length; i++)
                            if ((itfs[i].tdef().modifiers & MODULE) == 0)
                                report.error(tree.pos, "module " + xcd + 
                                    " cannot implement class interface " + itfs[i]);
                    } else {
                        Type[] itfs = xcd.type.interfaces();
                        for (int i = 0; i < itfs.length; i++)
                            if ((itfs[i].tdef().modifiers & MODULE) != 0)
                                report.error(tree.pos, "class " + xcd + 
                                    " cannot implement module interface " + itfs[i]);
                    }
                    result = xcd;
                }
                break;
            case ModuleFieldDecl(int m, Tree impl, Tree[] intf):
                break;
            case VarDecl(Name name, int mods, Tree vartype, _, _):
                /* if (definitions.isAlgebraicSubclass(env.info.scope.owner) && ((mods & STATIC) == 0)) {
                    report.error(tree.pos, "algebraic subclasses must not have " +
                                           "variable declarations");
                    result = super.memberEnter(tree, env);
                    result.type = Type.ErrType;
                }
                else */
                    result = super.memberEnter(tree, env);
                break;
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):
                result = super.memberEnter(tree, env);
                //System.out.println("entered " + result + ": " + result.type + " in " + result.owner);
                if (result.isConstructor() && ((result.owner.modifiers & CASEDEF) != 0)) {
                    if ((stats != null) && (stats.length > 0))
                        switch (stats[0]) {
                            case Exec(Apply(Self(_, SUPER, _), _)):
                                break main;
                        }
                    ((MethodDecl)tree).stats = trees.append(
                            trees.newdef.Exec(trees.newdef.Apply(trees.Super(),
                                new Tree[]{trees.newdef.Literal(
                                    constants.make.IntConst(((XClassDef)result.owner).tag))})),
                            stats);
                }
                break;
            default:
                result = super.memberEnter(tree, env);
                break;
        }
        trees.popPos();
        return result;
    }
    
    /** check if module 'overriding' is specialized consistently
     */
    void checkSpecConsistency(int pos, XClassDef overriding) {
        checkSpecConsistency(pos,
            null,
            overriding,
            (XClassDef)overriding.supertype().tdef(),
            specializationMap(overriding),
            new ModuleSet());
    }
    
    void checkSpecConsistency(int pos, String context, XClassDef overriding, XClassDef overridden, ModuleMap specMap, ModuleSet visited) {
        XClassDef c = (XClassDef)specMap.get(overridden);
        if ((c == null) && (overriding != null))
            report.error(pos, "inconsistent specialization; the context of this module does not specialize " + overridden +
                              ", but " + context + " requires " + overriding + " as " + overridden);
        else if ((c != null) && (overriding == null))
            report.error(pos, "inconsistent specialization; the context of this module requires " + c + " as " + overridden +
                              ", but " + context + " does not specialize " + overridden + " accordingly");
        else if ((c != null) &&
            (overriding != null) &&
            !c.refinementOf(overriding) &&
            !overriding.refinementOf(c))
            report.error(pos, "inconsistent specialization; the context of this module requires " + c + " as " + overridden +
                              ", but " + context + " requires " + overriding + " as " + overridden);
        if (visited.contains(overridden))
            return;
        visited.add(overridden);
        if (overriding == null)
            overriding = overridden;
        context = (context == null) ? overriding.toString() : overriding + ", required by " + context;
        XClassDef[] req = overridden.modIntf.required.keys();
        for (int i = 0; i < req.length; i++)
            checkSpecConsistency(pos, context, (XClassDef)overriding.modIntf.overriddenRequired.get(req[i]), req[i], specMap, visited);
    }
    
    /** derive a mapping from modules to specialized modules
     */
    ModuleMap specializationMap(XClassDef overriding) {
        ModuleMap res = new ModuleMap();
        ModuleSet visited = new ModuleSet();
        if ((overriding.modifiers & SPECIALIZES) != 0) {
            res.put((XClassDef)overriding.supertype().tdef(), overriding);
            specializationMap(overriding, (XClassDef)overriding.supertype().tdef(), res, visited);
        }
        return res;
    }
    
    void specializationMap(XClassDef overriding, XClassDef overridden, ModuleMap map, ModuleSet visited) {
        if (visited.contains(overriding))
            return;
        visited.add(overriding);
        XClassDef[] req = overriding.modIntf.overriddenRequired.keys();
        for (int i = 0; i < req.length; i++)
            if (!map.contains(req[i]))
                map.put(req[i], overriding.modIntf.overriddenRequired.get(req[i]));
        for (int i = 0; i < req.length; i++)
            specializationMap((XClassDef)overriding.modIntf.overriddenRequired.get(req[i]),
                              req[i], map, visited);
    }
    
    /** attribute a class field dependency
     */
    Type attribDependency(Tree ext, ClassDef c, ContextEnv localEnv) {
        Type supert = attribSuper(ext, localEnv, false);
        if ((supert.tdef().modifiers & CLASSFIELD) == 0) {
            report.error(ext.pos, "class field " + c +
                " depends on type " + supert);
            supert = types.objectType;
        } else if (false) {
            report.error(ext.pos, "class field " + c +
                " depends on class field " + supert +
                " of module " + supert.tdef().owner);
            supert = types.objectType;
        }
        return supert;
    }
    
    /** is this definition unique in scope
     */
    public boolean isUnique(Definition def, Scope s) {
        Definition  e = s.lookup(def.name);
        while (e.scope == s) {
            if ((e.def != def) &&
                (e.def.kind == def.def.kind) &&
                (def.name != PredefConst.ERROR_N) &&
                ((e.def.kind != FUN) || types.sametype(e.def.type, def.def.type)))
                return false;
            e = e.next();
        }
        return true;
    }
    
    ////
    //// third sub-pass: collect class field dependencies for all modules
    ////
    
    public TreeList exit(TreeList treelist)  throws AbortCompilation {
        TreeList res = super.exit(treelist);
        XClassDef[] mods = (XClassDef[])allModules.toArray(
            new XClassDef[allModules.size()]);
        Set modSet = new HashSet();
        for (int i = 0; i < mods.length; i++)
            collectClassFieldDependencies(mods[i], modSet);
        return res;
    }
    
    protected TypeMap collectClassFieldDependencies(XClassDef mod, Set modSet) {
        // check for recursive nesting
        //if (modSet.contains(mod)) {
        //  report.error(Position.NOPOS, "module " + mod + " is a submodule of itself");
        //  return null;
        //}
        ModuleIntf modIntf = mod.modIntf;
        if (modIntf.maxInterfaces != null)
            return modIntf.maxInterfaces;
        // create a new type map for representing the class field constraints
        // of mod
        modIntf.maxInterfaces = new TypeMap(types);
        modSet.add(mod);
        // collect class field dependencies for all contained submodules
        XClassDef[] contained = modIntf.contained.keys();
        for (int i = 0; i < contained.length; i++) {
            TypeMap max = collectClassFieldDependencies(contained[i], modSet);
            Type[] cfs = max.keys();
            for (int j = 0; j < cfs.length; j++)
                enterDependency(mod,
                                modules.refineType(cfs[j], mod),
                                modules.refineType(max.get(cfs[j]).types(), mod));
        }
        //System.out.println("collect(" + mod + ", " + modSet + ")");
        // collect class field dependencies of mod (by checking all
        // supermodules)
        Set overridden = new HashSet();
        XClassDef d = mod;
        while ((d.modifiers & MODULE) != 0) {
            Definition e = d.locals().elems;
            while (e != null) {
                if ((e.def.kind == TYP) &&
                    ((e.def.modifiers & CLASSFIELD) != 0) &&
                    !overridden.contains(e.def.name)) {
                    XType.XClassType t = (XType.XClassType)modules.modularizeType(e.def.type);
                    //System.out.println(">>> " + t);
                    Type[] cfs = t.depends();
                    for (int j = 0; j < cfs.length; j++)
                        enterDependency(mod,
                                        modules.refineType(cfs[j], mod),
                                        modules.refineType(t.interfaces(), mod));
                }
                e = e.sibling;
            }
            d = (XClassDef)d.supertype().tdef();
        }
        modSet.remove(mod);
        return modIntf.maxInterfaces;
    }
    
    protected void enterDependency(XClassDef mod, Type cf, Type[] max) {
        TypeSet maxSet = new TypeSet(types, max);
        //System.out.println("enterDependency(" + mod + ", " + cf + ", " + maxSet + ")");
        //if (!(cf instanceof XType.XClassType))
        //  System.out.println("===> " + cf);
        Type[] intf = cf.interfaces();
        //System.out.println("  " + new TypeSet(types, intf));
        for (int i = 0; i < intf.length; i++)
            if (!maxSet.contains(intf[i]))
                report.error(Position.NOPOS,
                             "class field " + cf + " implements interface " + intf[i] +
                             " which is not supported by depending class fields");
        XClassDef c = (XClassDef)modules.outermostModule(cf).tdef();
        if (mod.modIntf.required.key(c) != null)
            mod.modIntf.maxInterfaces.putIntersection(cf, max);
    }
}

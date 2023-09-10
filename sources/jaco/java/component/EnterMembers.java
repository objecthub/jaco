//      /   _ _      JaCo
//  \  //\ / / \     - third pass of semantic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 24/01/99

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import Definition.*;


public class EnterMembers extends Processor
                          implements ModifierConst, DefinitionConst,
                                     OperatorConst, TreeConst, TypeConst
{
/** helper components
 */
    public NameResolver     namer;
    public Attribute        attribute;
    public Accountant       accountant;
    
/** language components
 */
    public Modifiers        modifiers;
    public Types            types;
    public Definitions      definitions;
    public Trees            trees;


/** component name
 */
    public String getName()
    {
        return "JavaEnterMembers";
    }
    
    public String getDescription()
    {
        return "entering class members";
    }
                
    public String getDebugName()
    {
        return "entermem";
    }
    
/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context.compilerContext);
        MainContext mainContext = context.compilerContext.mainContext;
        namer = mainContext.NameResolver();
        modifiers = mainContext.Modifiers();
        types = mainContext.Types();
        definitions = mainContext.Definitions();
        trees = mainContext.Trees();
        attribute = context.Attribute();
        accountant = context.Accountant();
    }
    
/** check that variables or functions are unique
 */
    public boolean checkUnshadowed(int pos, VarDef v, Scope s)
    {
        if (s.next != null)
        {
            Definition  e = s.next.lookup(v.name);
            while ((e.scope != null) && (e.def.owner == v.owner))
            {
                if ((e.def.kind == VAR) &&
                    (e.def.owner.kind == FUN) &&
                    ((e.def.modifiers & ABSTRACT) == 0) &&
                    (v.name != PredefConst.ERROR_N))
                {
                    report.error(pos, "already.defined", v, e.def.location());
                    return false;
                }
                e = e.next();
            }
        }
        return true;
    }

    public boolean checkUnique(int pos, Definition def, Scope s)
    {
        Definition  e = s.lookup(def.name);
        while (e.scope == s)
        {
            if ((e.def != def) &&
                (e.def.kind == def.def.kind) &&
                (def.name != PredefConst.ERROR_N) &&
                ((e.def.kind != FUN) || types.sametype(e.def.type, def.def.type)))
            {
                report.error(pos, "duplicate.def", def + def.location());
                return false;
            }
            e = e.next();
        }
        return true;
    }


/** check for/supply constructors
 */
    public boolean isConstructor(Tree def)
    {
        switch (def)
        {
            case MethodDecl(_, _, Tree restype, _, _, _, _):
                    return (restype == null);
        }
        return false;
    }

    public boolean hasConstructors(Tree[] defs)
    {
        int i = 0;
        while ((i < defs.length) && !isConstructor(defs[i]))
            i++;
        return (i < defs.length);
    }

    public boolean hasConstructors(Definition c, Tree[] defs)
    {
        return hasConstructors(defs);
    }
    
/** enter a variable into local scope, after its type has been attributed
 */
    public VarDef enterVar(VarDecl tree, ContextEnv env)
    {
        Scope   enclscope = accountant.enterScope(env);
        VarDef  v = (VarDef)definitions.make.VarDef(tree.mods, tree.name, tree.vartype.type,
                                            enclscope.owner);
        tree.def = v;
        if (((tree.mods & FINAL) != 0) && (tree.init != null))
            v.initializer = accountant.initEnv(env, tree);
        if (checkUnique(tree.pos, v, enclscope))
        {
            checkUnshadowed(tree.pos, v, enclscope);
            enclscope.enter(v);
        }
        return v;
    }

    public Definition memberEnter(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        Definition result = null;
        switch (tree)
        {
            case PackageDecl(_):
            case Import(_, _):
                break;

            case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] defs, ClassDef c):
                if (accountant.todo.get(c) != null)
                {
                    //report.useEnv(env.toplevel.info);
                    ContextEnv  localEnv = (ContextEnv)accountant.classEnvs.get(c);
                    if ((localEnv != null) && ((c.modifiers & INTERFACE) != 0))
                    {
                        report.error(tree.pos, "cyclic.inheritance", c);
                        c.setSupertype(types.objectType);
                        c.setInterfaces(types.noTypes);
                    }
                    else
                    if (localEnv == null)
                    {
                        c.setSupertype(null);
                        localEnv = accountant.classEnv((ClassDecl)tree, env);
                        accountant.classEnvs.put(c, localEnv);
                        Type st = extending == null ?
                                    (c.type == types.objectType ?
                                        null :
                                        types.objectType) :
                                    attribSuper(extending, localEnv, false);
                        boolean unset = (c.supertype() == null);
                        if (unset)
                            c.setSupertype(st);
                        Type[] is = new Type[implementing.length];
                        // needed because of possible cycle break
                        for (int i = 0; i < is.length; i++)
                            is[i] = attribSuper(implementing[i], localEnv, true);
                        if (unset)
                            c.setInterfaces(is);
                        namer.fixupScope(tree.pos, c);
                        accountant.todo.remove(c);
                        
                        if (((mods & INTERFACE) == 0) && !hasConstructors(c, defs))
                        {
                            Tree    constr = trees.at(tree.pos).make(
                                            trees.DefaultConstructor(
                                            PUBLIC, //c.modifiers & modifiers.AccessMods,
                                            false, types.noTypes));
                            ((ClassDecl)tree).members = defs =
                                            trees.append(defs, constr);
                        }
                        for (int i = 0; i < defs.length; i++)
                        {
                            switch (defs[i])
                            {
                                case ClassDecl(_, _, _, _, _, _):
                                    break;
                            
                                default:
                                    memberEnter(defs[i], localEnv);
                            }
                        }
                                
                        if ((c.modifiers & ABSTRACT) != 0 &&
                            ((c.modifiers & INTERFACE) == 0))
                        implementInterfaceMethods((ClassDecl)tree, c, localEnv);
                        
                        for (int i = 0; i < defs.length; i++)
                        {
                            switch (defs[i])
                            {
                                case ClassDecl(_, _, _, _, _, _):
                                    memberEnter(defs[i], localEnv);
                                    break;
                            }
                            if ((c.owner.kind != PCK) &&
                                ((c.modifiers & INTERFACE) == 0) &&
                                ((mods & STATIC) == 0) &&
                                ((defs[i].mods() & STATIC) != 0))
                                report.error(defs[i].pos, "static.members.in.innerclass");
                        }
                        accountant.classEnvs.remove(c);
                    }
                    //report.usePreviousEnv();
                }
                result = c;
                break;

            case Block(int mods, Tree[] stats):
                modifiers.checkMods(tree.pos, mods, STATIC, false);
                break;
                
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params, Tree[] thrown, Tree[] stats, _):
                int fmods = modifiers.MethMods;
                if (restype == null) // we encounter a constructor declaration here
                {
                    if ((name == env.enclClass.name) || (name == PredefConst.INIT_N))
                    {
                        ((MethodDecl)tree).name = name = PredefConst.INIT_N;
                        fmods = modifiers.ConstrMods;
                    }
                    else
                    {
                        report.error(tree.pos, "return.type.required");
                        ((MethodDecl)tree).restype = trees.errorTree;
                    }
                }
                fmods = modifiers.completeMods(tree.pos, mods, env, fmods,
                                                modifiers.InterfaceMethMods);
                Scope enclscope = accountant.enterScope(env);
                if ((enclscope.owner.modifiers & STRICTFP) != 0)
                    fmods |= STRICTFP;
                Definition f = definitions.make.MethodDef(fmods, name, null, enclscope.owner);
                tree.setDef(f);
                ContextEnv  localEnv = accountant.methodEnv((MethodDecl)tree, env);
                if (params == null)
                    ((MethodDecl)tree).params = params = new VarDecl[0];
                f.type = attribute.attribSignature(params, restype, thrown, localEnv);
                f.type.setDef(f);
                if (checkUnique(tree.pos, f, enclscope))
                    enclscope.enter(f);
                localEnv.info.scope.leave();
                result = f;
                break;

            case VarDecl(Name name, int mods, Tree vartype, _, _):
                ((VarDecl)tree).mods = modifiers.completeMods(
                        tree.pos, mods, env,
                        (env.info.scope.owner.kind == TYP) ?
                            modifiers.VarMods :
                            modifiers.LocalVarMods,
                        modifiers.InterfaceVarMods);
                ContextEnv varEnv = accountant.initEnv(env, (VarDecl)tree);
                attribute.attribNonVoidType(vartype, varEnv);
                result = enterVar((VarDecl)tree, varEnv);
                break;

            case Bad():
                break;

            default:
                throw new InternalError();
        }
        trees.popPos();
        return result;
    }

    public Definition memberEnter(Definition c)
    {
        Tree    cdef = (Tree)accountant.todo.get(c);
        if (cdef != null)
        {
            if (c.owner.kind == PCK)
                memberEnter(cdef, (ContextEnv)accountant.topEnvs.get(((ClassDef)c).
                                    sourcefile.toString()));
            else
            {
                Definition  cowner = c.owner;
                memberEnter(cowner);
                if (accountant.todo.get(c) != null)
                    memberEnter(cdef, (ContextEnv)accountant.classEnvs.get(cowner));
            }
        }
        return c;
    }
        
/** attribute extended or implemented type reference
 */
    public Type attribSuper(Tree tree, ContextEnv env, boolean interfaceExpected)
    {
        Type t = attribute.attribClassType(tree, env);
        switch (t)
        {
            case ClassType(_):
                memberEnter(t.tdef());
                if (interfaceExpected)
                {
                    if ((t.tdef().modifiers & INTERFACE) == 0) {
                        System.out.println("*** " + t.tdef());
                        report.error(tree.pos, "no.interface");
                    }
                }
                else
                {
                    if ((t.tdef().modifiers & INTERFACE) != 0)
                        report.error(tree.pos, "no.interface.allowed");
                    else
                    if ((t.tdef().modifiers & FINAL) != 0)
                        report.error(tree.pos,"final.superclass", t.tdef());
                }
        }
        return t;
    }
        
/** add an abstract method definition to class definition `cd'
 *  for some method `f' defined in an interface
 */
    public void addAbstractMethod(ClassDecl cd, Definition f, ContextEnv env)
    {
        f.owner = cd.def;
        Tree    fd = trees.at(cd.pos).make(trees.MethodDecl(f, null));
        cd.members = trees.append(cd.members, fd);
//      Pretty.printDecl(fd); System.out.println(" added");//DEBUG
        memberEnter(fd, env);
    }

/** add abstract method definitions for all methods defined in one of c's
 *  interfaces, provided they are not already implemented
 */
    public void implementInterfaceMethods(ClassDecl cd, Definition c, ContextEnv env)
    {
        Type[]  is = c.interfaces();
        for (int i = 0; i < is.length; i++)
        {
            Definition  iface = is[i].tdef();
            for (Definition e = iface.locals().elems; e != null; e = e.sibling)
            {
                if ((e.def.kind == FUN) && ((e.def.modifiers & STATIC) == 0))
                {
                    Definition  absfun = e.def;
                    Definition  implfun = definitions.implementation(absfun, cd.def);
                    if (implfun == null)
                        addAbstractMethod(cd, absfun, env);
                }
            }
            implementInterfaceMethods(cd, iface, env);
        }
    }
    
/** the tree processor
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        ContextEnv  localEnv = (ContextEnv)accountant.topEnvs.
                                        get(tree.info.source.toString());
        for (int i = 0; i < tree.decls.length; i++)
            memberEnter(tree.decls[i], localEnv);
        if (tree.info.shortRun)
        {
            AbortCompilation    e = new AbortCompilation(1);
            e.info = tree;
            throw e;
        }
        else
            return tree;
    }
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return !info.attributed;
    }
}

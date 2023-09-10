//      /   _ _      JaCo
//  \  //\ / / \     - translation of inner class definitions to Java 1.0
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import Definition.*;


public class TreeEnvPair
{
    public Tree     tree;
    public InnerEnv env;
    
    public TreeEnvPair(Tree tree, InnerEnv env)
    {
        this.tree = tree;
        this.env = env;
    }
}

public class TransInner extends Translator
                        implements ModifierConst, OperatorConst,
                                   DefinitionConst, TreeConst
{
/** other components
 */
    Definitions             definitions;
    Modifiers               modifiers;
    NameResolver            namer;
    
/** tables
 */
    protected int           idCount;
    protected int           accessCount;
    protected Hashtable     accessNums;
    protected Hashtable     accessCodes;
    protected Hashtable     accessIds;
    protected Hashtable     freevars;
    protected Vector        freevarsNodes;
    protected TreeList      topdefs;
    protected int           nestingDepth;
//    protected boolean       containsAsserts;
    
/** component name
 */
    public String getName()
    {
        return "TransInnerClasses";
    }
    
/** return descrition of tree processor
 */
    public String getDescription()
    {
        return "translating innerclasses";
    }
    
/** default getDebugName method; returns an invalid debug name
 */
    public String getDebugName()
    {
        return "inner";
    }
    
/** component initialization
 */
    public void init(BackendContext context)
    {
        super.init(context.compilerContext);
        MainContext mainContext = context.compilerContext.mainContext;
        modifiers = mainContext.Modifiers();
        definitions = mainContext.Definitions();
        namer = mainContext.NameResolver();
    }
    
/** the tree processing method
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        accessCount = 0;
        accessNums = new Hashtable();
        accessCodes = new Hashtable();
        accessIds = new Hashtable();
        freevars = new Hashtable();
        freevarsNodes = new Vector();
        topdefs = new TreeList();
        nestingDepth = 0;
        tree.info.attributed = false;
        Tree[]  decls = tree.decls;
        Tree    t = trees.at(tree).make(
                    redef.CompilationUnit(
                        decls = trees.append(transDecls(decls,
                                                        initialEnv(tree)),
                                     topdefs.toArray()),
                        tree.info));
        passFreeVars();
        makeAccessible(decls);
        return t;
    }
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return (info.errors == 0) && info.nestedClasses;
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist)  throws AbortCompilation
    {
        compilerContext.mainContext.classesNest = false;
        return super.exit(treelist);
    }
    

//////////// builder methods
    
    protected Tree This()
    {
        if (owner != null)
            return newdef.Self(null, THIS).setDef(owner.enclClass());
        else
            return newdef.Self(null, THIS);
    }

    protected Tree Qualid(Definition def)
    {
        if ((def.owner == null) || (def.owner.name.length() == 0))
            return trees.Ident(def);
        else
        {
            Definition  owner = def.owner;
            if (def.kind == TYP)
                while (owner.kind == TYP)
                    owner = owner.owner;
            if (owner.name.length() == 0)
                return trees.Ident(def);
            else
                return newdef.Select(Qualid(owner), def.name).
                                        setType(def.type).setDef(def);
        }
    }
    
    protected Name thisName()
    {
        return Name.fromString("this$" + (nestingDepth - 2));
    }
    
    protected Name thisName(int level)
    {
        return Name.fromString("this$" + level);
    }
    
    protected VarDecl[] addThis0Param(VarDecl[] params)
    {
        VarDecl[]   newParams = new VarDecl[params.length + 1];
        System.arraycopy(params, 0, newParams, 1, params.length);
        newParams[0] = (VarDecl)newdef.VarDecl(thisName(), 0,
                            trees.toTree(owner.owner.enclClass().type), null);
        return newParams;
    }
    
    protected Tree[] addThis0Init(Tree[] stats)
    {
        Tree[]  newstats = new Tree[stats.length + 1];
        newstats[0] = stats[0];
        newstats[1] = newdef.Exec(
                        newdef.Assign(newdef.Select(This(), thisName()),
                        newdef.Ident(thisName())));
        System.arraycopy(stats, 1, newstats, 2, stats.length - 1);
        return newstats;
    }
    
    protected Tree makeThis(Definition c)
    {
        Definition c1 = owner;
        if (definitions.subclass(c1, c))
            return This();
        else
        {
            Tree    t = newdef.Ident(thisName());
            int     level = nestingDepth - 2;
            
            c1 = c1.owner.enclClass();
            while (!definitions.subclass(c1, c))
            {
                t = newdef.Select(t, thisName(--level));
                c1 = c1.owner.enclClass();
            }
            return t;
        }
    }
    
    protected Env initialEnv(Tree tree)
    {
        return new InnerEnv(tree, new Vector());
    }
    
//////////// translate
    
    public Tree translateDecl(Tree tree, Env env)
    {
        switch (tree)
        {
            case PackageDecl(_):
                return tree; //todo
                
            case Import(_, _):
                return tree;
                
            case ClassDecl(Name name, _, Tree extending, Tree[] implementing,
                            Tree[] defs, ClassDef c):
                // boolean containedAsserts = containsAsserts;
                // containsAsserts = false;
                nestingDepth++;
                InnerEnv localEnv = ((InnerEnv)env).dup(tree, new Vector());
                localEnv.enclClass = (ClassDecl)tree;
                report.note("[flattening class " + name + "]");
                Tree[] defs1 = transStats(defs, localEnv);
                if (definitions.isInnerclass(c))
                    defs1 = trees.append(
                                newdef.VarDecl(
                                    thisName(), FINAL | SYNTHETIC,
                                    trees.toTree(c.owner.enclClass().type),
                                    null),
                                defs1);
                if (c.owner.kind == FUN)
                {
                    freevars.put(c, localEnv.info);
                    defs1 = trees.append(
                                freevarsDecls(FINAL | SYNTHETIC, localEnv.info),
                                defs1);
                }
                /* if (containsAsserts && (c.owner.kind != PCK)) {
                    Definition toplevel = c.owner.outermostClass();
                    Definition ae = namer.findField(null, toplevel,
                        BytecodeGenerator.ASSERTIONS_ENABLED);
                    defs1 = trees.append(
                        newdef.VarDecl(
                            BytecodeGenerator.ASSERTIONS_ENABLED,
                            (((c.modifiers & INTERFACE) == 0) ?
                              PRIVATE : 0) | STATIC | FINAL | SYNTHETIC,
                            newdef.BasicType(TypeConst.BOOLEAN),
                            ((toplevel.modifiers & INTERFACE) != 0) ?
                                newdef.Select(
                                    trees.toTree(toplevel.type),
                                    BytecodeGenerator.ASSERTIONS_ENABLED) :
                                newdef.Apply(
                                    newdef.Select(
                                        trees.toTree(toplevel.type),
                                        accessName(ae, null, localEnv)),
                                    trees.noTrees)),
                        defs1);
                } */
                c.modifiers = ((((c.modifiers & PROTECTED) != 0) ?
                                ((c.modifiers & ~PROTECTED) | PUBLIC) :
                                c.modifiers) &
                                (0xffff0000 | modifiers.ClassMods)) | SYNTHETIC;
                // containsAsserts = containedAsserts;
                tree = trees.at(tree).make(
                        redef.ClassDecl(c.name, c.modifiers,
                                        transType(extending, localEnv),
                                        transTypes(implementing, localEnv),
                                        defs1));
                nestingDepth--;
                return tree;
            
            default:
                return super.translateDecl(tree, env);
        }
    }
    
    public Tree translateStat(Tree tree, Env env)
    {
        switch (tree)
        {
            case ClassDecl(_, _, _, _, _, _):
                topdefs.append(transDecl(tree, env));
                return null;
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef f):
                InnerEnv localEnv = ((InnerEnv)env).dup(tree);
                localEnv.enclMethod = (MethodDecl)tree;
                VarDecl[]   params1 = transVarDecls(params, localEnv);
                Tree[]      stats1 = transStats(stats, localEnv);
                if (f.isConstructor())
                {
                    if (definitions.isInnerclass(f.owner))
                    {
                        stats1 = addThis0Init(stats1);
                        params1 = addThis0Param(params1);
                    }
                    tree = trees.at(tree).make(
                            redef.MethodDecl(name, mods,
                                            transType(restype, env), params1,
                                            transTypes(thrown, env), stats1));
                    if (f.owner.owner.kind == FUN)
                        freevarsNodes.addElement(
                                    new TreeEnvPair(tree, (InnerEnv)env));
                    if (f.owner.owner.kind != PCK)
                        mods = mods & ~PRIVATE;
                    return tree;
                }
                else
                    return trees.at(tree).make(
                            redef.MethodDecl(name, mods,
                                            transType(restype, env), params1,
                                            transTypes(thrown, env), stats1));
            
            case Assert(_, _):
                // containsAsserts = true;
                return super.translateStat(tree, env);
                
            default:
                return super.translateStat(tree, env);
        }
    }

    public Tree translateExpr(Tree tree, Env env)
    {
        switch (tree)
        {
            case Apply(Tree fn, Tree[] args):
                switch (fn)
                {
                    case Self(Tree encl, int tag, Definition def):
                        if (encl != null)
                        {
                            fn = trees.at(fn).make(redef.Self(null, tag));
                            args = trees.append(encl, args);
                        }
                        tree = trees.at(tree).make(
                                redef.Apply(transExpr(fn, env), transExprs(args, env)));
                        if (def.owner.owner.kind == FUN)
                            freevarsNodes.addElement(
                                    new TreeEnvPair(tree, (InnerEnv)env));
                        return tree;
                }
                return trees.at(tree).make(
                        redef.Apply(transExpr(fn, env), transExprs(args, env)));
            
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def,
                        Definition constructor):
                if (encl != null)
                    args = trees.append(encl, args);
                Tree    clazz1 = transType(clazz, env);
                if (def != null)
                    clazz1 = trees.Qualid(def.def().fullname);
                tree = trees.at(tree).make(
                        redef.NewObj(null, clazz1, transExprs(args, env),
                                    transStat(def, env)));
                if (constructor.owner.owner.kind == FUN)
                    freevarsNodes.addElement(
                                new TreeEnvPair(tree, (InnerEnv)env));
                return tree;
                
            case Assign(Tree lhs, Tree rhs):
                Tree    result = tree;
                Tree    lhs1 = transExpr(lhs, ((InnerEnv)env).dup(tree));
                Tree    rhs1 = transExpr(rhs, env);
                switch (lhs1)
                {
                    case Apply(Tree fn, Tree[] args):
                        result = trees.at(lhs1).make(
                                    redef.Apply(fn, new Tree[]{rhs1}));
                        break;
                    
                    default:
                        result = trees.at(tree).make(redef.Assign(lhs1, rhs1));
                        break;
                }
                return result;

            case Assignop(int opcode, Tree lhs, Tree rhs, Definition operator):
                Tree    result = tree;
                Tree    lhs1 = transExpr(lhs, ((InnerEnv)env).dup(tree));
                Tree    rhs1 = transExpr(rhs, env);
                switch (lhs1)
                {
                    case Apply(Tree fn, Tree[] args):
                        result = trees.at(lhs1).make(
                                    redef.Apply(fn, new Tree[]{rhs1}));
                        break;
                    
                    default:
                        result = trees.at(tree).make(
                                    redef.Assignop(opcode, lhs1, rhs1));
                        break;
                }
                return result;
            
            case Unop(int opcode, Tree operand, Definition operator):
                Tree    result = tree;
                if ((opcode >= PREINC) && (opcode <= POSTDEC))
                {
                    Tree    operand1 = transExpr(operand,
                                                ((InnerEnv)env).dup(tree));
                    switch (operand1)
                    {
                        case Apply(Tree fn, Tree[] args):
                            result = operand1;
                            break;
                        
                        default:
                            result = trees.at(tree).make(
                                        redef.Unop(opcode, operand1));
                    }
                }
                else
                    result = trees.at(tree).make(
                                redef.Unop(opcode, transExpr(operand, env)));
                return result;
            
            case Select(Tree selected, Name selector, Definition def):
                if ((def.kind == TYP) && (def.owner.kind != PCK))
                    return trees.Qualid(def.fullname);
                else
                {
                    if (((def.modifiers & PRIVATE) != 0) && (def.owner != owner))
                    {
                        Tree result = trees.at(tree).make(
                                        redef.Select(transExpr(selected, env),
                                            accessName(def, tree, (InnerEnv)env)));
                        if (def.kind == VAR)
                            result = newdef.Apply(result, trees.noTrees);
                        return result;
                    }
                    else
                    {
                        return trees.at(tree).make(
                                redef.Select((selector == PredefConst.CLASS_N) ?
                                                transType(selected, env) :
                                                transExpr(selected, env),
                                            selector));
                    }
                }
            
            case Ident(Name idname, Definition def):
                switch (def.kind)
                {
                    case PCK:
                        return super.translateExpr(tree, env);
                        
                    case TYP:
                        if (def.owner.kind != PCK)
                            return trees.Qualid(def.fullname);
                        else
                            return tree;
                    
                    case VAR:
                        if (def.owner.kind == TYP)
                            return accessMember(def, tree, (InnerEnv)env);
                        else
                        if ((def.modifiers & CAPTURED) != 0)
                            return accessFreeVar((VarDef)def, (InnerEnv)env);
                        else
                            return tree;

                    case FUN:
                        return accessMember(def, tree, (InnerEnv)env);
                    
                    default:
                        throw new InternalError();
                }
            
            case Self(Tree clazz, int tag, Definition def):
                if ((tag == THIS) && (clazz != null))
                    return makeThis(clazz.type.tdef());
                else
                    return super.translateExpr(tree, env);
    
            default:
                return super.translateExpr(tree, env);
        }
    }
    
    public Tree translateType(Tree tree, Env env)
    {
        switch (tree)
        {
            case Select(_, _, _):
            case Ident(_, _):
                return translateExpr(tree, env);
            
            default:
                return super.translateType(tree, env);
        }
    }


//////////// access methods

/** the name of a access method nr 'anum' with access code 'acode'
 */
    protected Name accessName(int anum, int acode)
    {
        Integer aref = new Integer((anum << 8) | (acode & 0xff));
        Integer id = (Integer)accessIds.get(aref);
        if (id == null)
            accessIds.put(aref, id = new Integer(idCount++));
        return Name.fromString("access$" + id);
    }
    
/** access a private Definition from an inner class, where `tree' is the
 *  accessing identifier occurrence
 */
    protected Name accessName(Definition def, Tree tree, InnerEnv env)
    {
        Integer anum = (Integer)accessNums.get(def);
        if (anum == null)
        {
            anum = new Integer(accessCount++);
            accessNums.put(def, anum);
            if (def.kind == VAR)
                accessCodes.put(def, new Bits());
            def.modifiers |= CAPTURED;
        }
        int acode = DEREF;
        if (def.kind == VAR)
        {
            switch (env.tree)
            {
                case Assign(Tree lhs, Tree rhs):
                    if (tree == lhs)
                        acode = ASSIGN;
                    break;

                case Assignop(int opcode, Tree lhs, Tree rhs,
                                Definition operator):
                    if (tree == lhs)
                        acode = opcode;
                    break;
        
                case Unop(int opcode, Tree operand, Definition operator):
                    if (tree == operand) acode = opcode;
                    break;
                
                default:
            }
            ((Bits)accessCodes.get(def)).incl(acode);
        }
        return accessName(anum.intValue(), acode);
    }

/** ensure that identifier 'tree' which refers to member Definition 'def'
 *  is accessible
 */
    protected Tree accessMember(Definition def, Tree tree, InnerEnv env)
    {
        Tree result;
        if (((def.modifiers & PRIVATE) != 0) && (def.owner != owner))
        {
            result = newdef.Select(accessBase(def), accessName(def, tree, env));
            if (def.kind == VAR)
                result = newdef.Apply(result, trees.noTrees);
        }
        else
        if ((def.owner != definitions.predefClass) &&
           !definitions.subclass(owner, def.owner))
            result = newdef.Select(accessBase(def), def.name);
        else
            result = tree;
        return result;
    }
    
    protected Tree accessBase(Definition def)
    {
        return ((def.modifiers & (STATIC | INTERFACE)) != 0) ?
                    trees.Qualid(def.owner.fullname) :
                    makeThis(def.owner.enclClass());
    }

/** add access methods
 */
    protected void makeAccessible(Tree[] decls)
    {
        for (int i = 0; i < decls.length; i++)
            makeAccessible(decls[i]);
    }
    
    protected void makeAccessible(Tree tree)
    {
        int old = trees.setPos(tree.pos);
        switch (tree)
        {
            case ClassDecl(Name name, int mods, Tree extending,
                            Tree[] implementing, Tree[] defs, ClassDef c):
                Definition  oldowner = owner;
                owner = c;
                TreeList accessdecls = new TreeList();
                for (Definition e = c.locals().elems; e != null; e = e.sibling)
                    if ((e.def.modifiers & (CAPTURED | PRIVATE)) ==
                        (CAPTURED | PRIVATE))
                        addAccessDecls(e.def, accessdecls);
                ((ClassDecl)tree).members = trees.append(defs,
                                                accessdecls.toArray());
                owner = oldowner;
                break;
        }
        trees.setPos(old);
    }
    
    protected void addAccessDecls(Definition def, TreeList defs)
    {
        int anum = ((Integer)accessNums.get(def)).intValue();
        if (def.kind == VAR)
        {
            Bits    acodes = (Bits)accessCodes.get(def);
            for (int i = 0; !acodes.empty(); i++)
                if (acodes.member(i))
                {
                    defs.append(accessDecl(def, anum, i));
                    acodes.excl(i);
                }
        }
        else
            defs.append(accessDecl(def, anum, DEREF));
    }

    protected Tree accessDecl(Definition def, int anum, int acode)
    {
        VarDecl[]   params;
        if (def.kind == FUN)
        {
            MethodType  ftype = (MethodType)def.type;
            params = trees.Params(ftype.argtypes);
            Tree    call = trees.Call(trees.App(
                                        newdef.Ident(def.name).setType(ftype),
                                        trees.Idents(params)));
            return newdef.MethodDecl(accessName(anum, acode),
                                (def.modifiers & STATIC) | SYNTHETIC | FINAL,
                                trees.toTree(ftype.restype),
                                params,
                                trees.toTree(ftype.thrown),
                                new Tree[]{call});
        }
        else
        {
            Tree    lhs = newdef.Ident(def.name);
            Tree    expr;
            switch (acode)
            {
                case DEREF:
                    expr = lhs;
                    params = new VarDecl[0];
                    break;
                
                case ASSIGN:
                    expr = newdef.Assign(lhs, newdef.Ident(trees.paramName(0)));
                    params = trees.Params(new Type[]{def.type});
                    break;
        
                case PREINC:
                case POSTINC:
                case PREDEC:
                case POSTDEC:
                    expr = newdef.Unop(acode, lhs);
                    params = new VarDecl[0];
                    break;
                
                default:
                    expr = newdef.Assignop(acode, lhs,
                                            newdef.Ident(trees.paramName(0)));
                    params = trees.Params(new Type[]{def.type});
                    break;
            }
            return newdef.MethodDecl(accessName(anum, acode),
                                (def.modifiers & STATIC) | SYNTHETIC | FINAL,
                                trees.toTree(def.type),
                                params,
                                trees.noTrees,
                                new Tree[]{newdef.Return(expr)});
        }
    }


//////////// adding freevars to all constructors of local classes

//todo: check freevars final

    protected Name freevarName(Name name)
    {
        return Name.fromString("val$" + name);
    }
    
/** add v to free variable set of given info, if not yet there
 */
    protected void addFreeVar(VarDef v, Vector freevars)
    {
        for (Enumeration e = freevars.elements(); e.hasMoreElements();)
            if (e.nextElement() == v)
                return;
        freevars.addElement(v);
    }
    
/** access free variable
 */
    protected Tree accessFreeVar(VarDef v, InnerEnv env)
    {
        Definition  vclass = v.owner.enclClass();
        InnerEnv    capturingEnv = null;
        while ((env != null) && (vclass != env.enclClass.def))
        {
            capturingEnv = env;
            env = (InnerEnv)env.outer;
        }
        if (capturingEnv != null)
        {
            addFreeVar(v, capturingEnv.info);
            return newdef.Select(makeThis(capturingEnv.enclClass.def),
                                freevarName(v.name));
        }
        else
            return newdef.Ident(v.name);
    }

/** construct a vector of free variables
 */
    protected Tree[] freevarsArgs(Vector freevars, InnerEnv env)
    {
        int         l = freevars.size();
        Enumeration e = freevars.elements();
        Tree[]      args = new Tree[l];
        
        for (int i = 0; i < l; i++)
        {
            VarDef  fv = (VarDef)e.nextElement();
            args[i] = accessFreeVar(fv, env);
        }
        return args;
    }
    
/** definitions for all freevars in given list
 */
    protected VarDecl[] freevarsDecls(int mods, Vector freevars)
    {
        int         l = freevars.size();
        Enumeration e = freevars.elements();
        VarDecl[]   defs = new VarDecl[l];
        
        for (int i = 0; i < l; i++)
        {
            VarDef  fv = (VarDef)e.nextElement();
            defs[i] = (VarDecl)newdef.VarDecl(freevarName(fv.name), mods,
                                     trees.toTree(fv.type), null);
        }
        return defs;
    }

    protected Tree[] freevarsInits(Vector freevars)
    {
        int         l = freevars.size();
        Enumeration e = freevars.elements();
        Tree[]      inits = new Tree[l];
        
        for (int i = 0; i < l; i++)
        {
            Name fvname = freevarName(((Definition)e.nextElement()).name);
            inits[i] = newdef.Exec(
                        newdef.Assign(newdef.Select(This(), fvname),
                                      newdef.Ident(fvname)));
        }
        return inits;
    }
    
    protected void passFreeVars()
    {
        for (Enumeration e = freevarsNodes.elements(); e.hasMoreElements();)
        {
            TreeEnvPair pair = (TreeEnvPair)e.nextElement();
            passFreeVars(pair.tree, pair.env);
        }
    }

    protected void passFreeVars(Tree tree, InnerEnv env)
    {
        int old = trees.setPos(tree.pos);
        switch (tree)
        {
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def,
                            Definition constructor):
                ((NewObj)tree).args = trees.append(
                    freevarsArgs((Vector)freevars.get(
                                    (Definition)constructor.owner), env),
                    args);
                break;
            
            case Apply(Tree fn, Tree[] args):
                switch (fn)
                {
                    case Self(Tree encl, int tag, Definition def):
                        ((Apply)tree).args = trees.append(
                            freevarsArgs((Vector)freevars.get(
                                            (Definition)def.owner), env),
                            args);
                }
                break;
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef f):
                Vector      fvs = (Vector)freevars.get((Definition)f.owner);
                TreeList    par = new TreeList(freevarsDecls(0, fvs)).
                                                            append(params);
                ((MethodDecl)tree).params = (VarDecl[])par.toArray(
                                                new VarDecl[par.length()]);
                Tree[]  fvinits = freevarsInits(fvs);
                if (fvinits.length != 0)
                    ((MethodDecl)tree).stats = trees.append(
                        trees.append(stats[0], fvinits),
                        trees.extract(stats, 1, new Tree[stats.length - 1]));
                break;
            
            default:
                throw new InternalError();
        }
        trees.setPos(old);
    }
}

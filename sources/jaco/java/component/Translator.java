//      /   _ _      JaCo
//  \  //\ / / \     - translator super class
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;


public abstract class Translator extends Processor
{
/** components
 */
    protected Trees         trees;
    protected Tree.Factory  newdef;
    protected Tree.Factory  redef;
    
/** current owner
 */
    protected Definition    owner;
    
    
/** component initialization
 */
    public void init(CompilerContext context)
    {
        super.init(context);
        trees = context.mainContext.Trees();
        newdef = trees.newdef;
        redef = trees.redef;
    }
    
/** the tree processing method
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        tree.info.attributed = false;
        return trees.at(tree).make(
                    redef.CompilationUnit(
                                transDecls(tree.decls, initialEnv(tree)),
                                tree.info));
    }
    
/** environment constructor methods
 */
    protected Env initialEnv(Tree tree)
    {
        return new Env(tree);
    }
    
    protected Env blockEnv(Tree tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        return localEnv;
    }
    
    protected Env methodEnv(MethodDecl tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        localEnv.enclMethod = tree;
        return localEnv;
    }
    
    protected Env classEnv(ClassDecl tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        localEnv.enclClass = tree;
        localEnv.enclMethod = null;
        localEnv.outer = env;
        return localEnv;
    }
    
    
/** translation methods
 */
    public Tree transDecl(Tree tree, Env env)
    {
        if (tree == null)
            return null;
        int         old = trees.setPos(tree.pos);
        Definition  newowner = tree.def();
        if ((newowner == null) ||
            ((newowner.kind &
                (DefinitionConst.TYP | DefinitionConst.FUN)) == 0))
            newowner = owner;
        Definition  oldowner = owner;
        owner = newowner;
        Tree    result = translateDecl(tree, env);
        owner = oldowner;
        trees.setPos(old);
        return result;
    }
    
    protected Tree translateDecl(Tree tree, Env env)
    {
        switch (tree)
        {
            case PackageDecl(Tree qualid):
                return trees.at(tree).make(
                        redef.PackageDecl(transType(qualid, env)));
            
            case Import(int tag, Tree qualid):
                return trees.at(tree).make(
                        redef.Import(tag, transType(qualid, env)));
            
            case ClassDecl(Name name, int mods, Tree extending,
                            Tree[] implementing, Tree[] members, _):
                return trees.at(tree).make(
                        redef.ClassDecl(name, mods, transType(extending, env),
                                        transTypes(implementing, env),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env))));
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):
                return trees.at(tree).make(
                        redef.MethodDecl(name, mods, transType(restype, env),
                                        transVarDecls(params, env),
                                        transTypes(thrown, env),
                                        transStats(stats,
                                            methodEnv((MethodDecl)tree, env))));
            
            case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                return trees.at(tree).make(
                        redef.VarDecl(name, mods, transType(vartype, env),
                        transExpr(init, env)));
            
            case Block(int mods, Tree[] stats):
                return trees.at(tree).make(
                    redef.Block(mods, transStats(stats, blockEnv(tree, env))));
            
            default:
                throw new InternalError("transDecl(" + tree + ")");
        }
    }
    
    public Tree transStat(Tree tree, Env env)
    {
        if (tree == null)
            return null;
        int old = trees.setPos(tree.pos);
        Tree    result = translateStat(tree, env);
        trees.setPos(old);
        return result;
    }
    
    protected Tree translateStat(Tree tree, Env env)
    {
        switch (tree)
        {
            case ClassDecl(_, _, _, _, _, _):
            case VarDecl(_, _, _, _, _):
            case Block(_, _):
                return translateDecl(tree, env);
            
            case Loop(int tag, Tree cond, Tree body):
                return trees.at(tree).make(
                    redef.Loop(tag, transExpr(cond, env), transStat(body, env)));
            
            case ForLoop(Tree[] init, Tree cond, Tree[] step, Tree body):
                return trees.at(tree).make(
                        redef.ForLoop(transStats(init, env),
                                      transExpr(cond, env),
                                      transStats(step, env),
                                      transStat(body, env)));
            
            case Labelled(Name label, Tree body):
                return trees.at(tree).make(
                        redef.Labelled(label, transStat(body, env)));
            
            case Switch(Tree selector, Case[] cases):
                return trees.at(tree).make(
                        redef.Switch(transExpr(selector, env),
                                     transCases(cases, env)));
            
            case Case(Tree[] pat, Tree[] stats):
                return trees.at(tree).make(
                        redef.Case(transPatterns(pat, env),
                                   transStats(stats, env)));
            
            case Synchronized(Tree lock, Tree body):
                return trees.at(tree).make(
                        redef.Synchronized(transExpr(lock, env),
                                           transStat(body, env)));
            
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                return trees.at(tree).make(
                        redef.Try(transStat(body, env),
                                  transCatches(catchers, env),
                                  transStat(finalizer, env)));
            
            case Catch(VarDecl param, Tree body):
                return trees.at(tree).make(
                        redef.Catch((VarDecl)transDecl(param, env),
                                    transStat(body, env)));
            
            case If(Tree cond, Tree thenpart, Tree elsepart):
                return trees.at(tree).make(
                        redef.If(transExpr(cond, env),
                                 transStat(thenpart, env),
                                 transStat(elsepart, env)));
            
            case Exec(Tree expr):
                return trees.at(tree).make(redef.Exec(transExpr(expr, env)));
            
            case Break(Name label,  _):
                return trees.at(tree).make(redef.Break(label));
            
            case Continue(Name label, _):
                return trees.at(tree).make(redef.Continue(label));
            
            case Return(Tree expr, _):
                return trees.at(tree).make(redef.Return(transExpr(expr, env)));

            case Throw(Tree expr):
                return trees.at(tree).make(redef.Throw(transExpr(expr, env)));
            
            case Assert(Tree cond, Tree message):
                return trees.at(tree).make(redef.Assert(transExpr(cond, env),
                                                   transExpr(message, env)));
            
            default:
                throw new InternalError("transStat(" + tree + ")");
        }
    }
    
    public Tree transExpr(Tree tree, Env env)
    {
        if (tree == null)
            return null;
        int old = trees.setPos(tree.pos);
        Tree result = translateExpr(tree, env);
        trees.setPos(old);
        return result;
    }

    protected Tree translateExpr(Tree tree, Env env)
    {
        switch (tree)
        {
            case VarDecl(_, _, _, _, _):
                return translateDecl(tree, env);

            case If(Tree cond, Tree thenpart, Tree elsepart):
                return trees.at(tree).make(
                        redef.If(transExpr(cond, env),
                                 transExpr(thenpart, env),
                                 transExpr(elsepart, env)));

            case Aggregate(Tree[] elems, Tree arrtype):
                return trees.at(tree).make(
                        redef.Aggregate(transExprs(elems, env),
                                        transType(arrtype, env)));
            
            case Apply(Tree fn, Tree[] args):
                return trees.at(tree).make(
                        redef.Apply(transExpr(fn, env), transExprs(args, env)));
            
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def, _):
                return trees.at(tree).make(
                        redef.NewObj(transExpr(encl, env),
                                     transType(clazz, env),
                                     transExprs(args, env),
                                     transDecl(def, env)));
            
            case NewArray(Tree elemtype, Tree[] dims):
                return trees.at(tree).make(
                        redef.NewArray(transType(elemtype, env),
                                        transExprs(dims, env)));
            
            case Assign(Tree lhs, Tree rhs):
                return trees.at(tree).make(
                        redef.Assign(transExpr(lhs, env), transExpr(rhs, env)));
            
            case Assignop(int opcode, Tree lhs, Tree rhs, _):
                return trees.at(tree).make(
                        redef.Assignop(opcode, transExpr(lhs, env),
                                        transExpr(rhs, env)));
            
            case Binop(int opcode, Tree lhs, Tree rhs, _):
                return trees.at(tree).make(
                        redef.Binop(opcode, transExpr(lhs, env),
                                    transExpr(rhs, env)));
            
            case Unop(int opcode, Tree operand, _):
                return trees.at(tree).make(
                        redef.Unop(opcode, transExpr(operand, env)));
            
            case Typeop(int opcode, Tree expr, Tree clazz):
                return trees.at(tree).make(
                        redef.Typeop(opcode, transExpr(expr, env),
                                     transType(clazz, env)));
            
            case Index(Tree indexed, Tree index):
                return trees.at(tree).make(
                        redef.Index(transExpr(indexed, env),
                                    transExpr(index, env)));
            
            case Select(Tree selected, Name selector, _):
                return trees.at(tree).make(
                        redef.Select((selector == PredefConst.CLASS_N) ?
                                        transType(selected, env) :
                                        transExpr(selected, env),
                                     selector));
            
            case Ident(Name name, _):
                return trees.at(tree).make(redef.Ident(name));
            
            case Self(Tree clazz, int tag, _):
                if (tag == TreeConst.THIS)
                    return trees.at(tree).make(
                            redef.Self(transType(clazz, env), tag));
                else
                    return trees.at(tree).make(
                            redef.Self(transExpr(clazz, env), tag));
            
            case Literal(Constant value):
                return trees.at(tree).make(redef.Literal(value));
            
            default:
                throw new InternalError("transExpr(" + tree + ")");
        }
    }
    
    public Tree transType(Tree tree, Env env)
    {
        if (tree == null)
            return null;
        int     old = trees.setPos(tree.pos);
        Tree    result = translateType(tree, env);
        trees.setPos(old);
        return result;
    }

    protected Tree translateType(Tree tree, Env env)
    {
        switch (tree)
        {
            case Select(Tree selected, Name selector, _):
                return trees.at(tree).make(
                        redef.Select(transType(selected, env), selector));
            
            case Ident(Name name, _):
                return trees.at(tree).make(redef.Ident(name));
                
            case BasicType(int tag):
                return trees.at(tree).make(redef.BasicType(tag));
    
            case ArrayTypeTerm(Tree elemtype):
                return trees.at(tree).make(
                        redef.ArrayTypeTerm(transType(elemtype, env)));

            default:
                throw new InternalError("transType(" + tree + ")");
        }
    }
    
    public Tree[] transDecls(Tree[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    decls = new TreeList();
        Tree        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = transDecl(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    decls.append(tree);
                else
                    decls.append(container);
            }
        return decls.toArray();
    }
    
    public Tree[] transStats(Tree[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    stats = new TreeList();
        Tree        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = transStat(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    stats.append(tree);
                else
                    stats.append(container);
            }
        return stats.toArray();
    }
    
    public Tree[] transExprs(Tree[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    exprs = new TreeList();
        Tree        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = transExpr(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    exprs.append(tree);
                else
                    exprs.append(container);
            }
        return exprs.toArray();
    }
    
    public Tree[] transPatterns(Tree[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    exprs = new TreeList();
        Tree        tree;
        boolean     inclDefault = false;
        for (int i = 0; i < ts.length; i++)
            if (ts[i] == null)
                inclDefault = true;
            else
            if ((tree = transExpr(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    exprs.append(tree);
                else
                    exprs.append(container);
            }
        tree = null;
        return inclDefault ? trees.append(tree, exprs.toArray()) :
                             exprs.toArray();
    }
    
    public Tree[] transTypes(Tree[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    types = new TreeList();
        Tree        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = transType(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    types.append(tree);
                else
                    types.append(container);
            }
        return types.toArray();
    }
    
    public VarDecl[] transVarDecls(VarDecl[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    decls = new TreeList();
        Tree        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = transDecl(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    decls.append(tree);
                else
                    decls.append(container);
            }
        return (VarDecl[])decls.toArray(new VarDecl[decls.length()]);
    }
    
    public Case[] transCases(Case[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    stats = new TreeList();
        Case        tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = (Case)transStat(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    stats.append(tree);
                else
                    stats.append(container);
            }
        return (Case[])stats.toArray(new Case[stats.length()]);
    }
    
    public Catch[] transCatches(Catch[] ts, Env env)
    {
        if (ts == null)
            return null;
        TreeList    stats = new TreeList();
        Catch       tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = (Catch)transStat(ts[i], env)) != null)
            {
                Tree[]  container = trees.getContainerContent(tree);
                if (container == null)
                    stats.append(tree);
                else
                    stats.append(container);
            }
        return (Catch[])stats.toArray(new Catch[stats.length()]);
    }
}

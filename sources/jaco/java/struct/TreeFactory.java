//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import Tree.*;
import Definition.*;


public class TreeFactory implements Tree.Factory, TreeConst
{
    public Tree CompilationUnit(Tree[] decls, CompilationEnv info)
    {
        return new CompilationUnit(decls, null, null, info);
    }
    
    public Tree PackageDecl(Tree qualid)
    {
        return new PackageDecl(qualid);
    }
    
    public Tree Import(int tag, Tree qualid)
    {
        return new Import(tag, qualid);
    }
    
    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] members)
    {
        return new ClassDecl(name, mods, extending, implementing, members, null);
    }
    
    public Tree MethodDecl(Name name, int mods, Tree restype,
                                 VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        return new MethodDecl(name, mods, restype, params, thrown, stats, null);
    }
    
    public Tree VarDecl(Name name, int mods, Tree vartype, Tree init)
    {
        return new VarDecl(name, mods, vartype, init, null);
    }
    
    public Tree Block(int mods, Tree[] stats)
    {
        return new Block(mods, stats);
    }
    
    public Tree Loop(int opcode, Tree cond, Tree body)
    {
        return new Loop(opcode, cond, body);
    }
    
    public Tree ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body)
    {
        return new ForLoop(inits, cond, steps, body);
    }
    
    public Tree Labelled(Name label, Tree body)
    {
        return new Labelled(label, body);
    }
    
    public Tree Switch(Tree selector, Case[] cases)
    {
        return new Switch(selector, cases);
    }
    
    public Tree Case(Tree[] pat, Tree[] stats)
    {
        return new Case(pat, stats);
    }
    
    public Tree Synchronized(Tree lock, Tree body)
    {
        return new Synchronized(lock, body);
    }
    
    public Tree Try(Tree body, Catch[] catchers, Tree finalizer)
    {
        return new Try(body, catchers, finalizer);
    }
    
    public Tree Catch(VarDecl exception, Tree body)
    {
        return new Catch(exception, body);
    }
    
    public Tree If(Tree cond, Tree thenpart, Tree elsepart)
    {
        return new If(cond, thenpart, elsepart);
    }
    
    public Tree Exec(Tree expr)
    {
        return new Exec(expr);
    }
    
    public Tree Break(Name label)
    {
        return new Break(label, null);
    }
    
    public Tree Continue(Name label)
    {
        return new Continue(label, null);
    }
    
    public Tree Return(Tree expr)
    {
        return new Return(expr, null);
    }
    
    public Tree Throw(Tree expr)
    {
        return new Throw(expr);
    }
    
    public Tree Aggregate(Tree[] elems, Tree arrtype)
    {
        return new Aggregate(elems, arrtype);
    }
    
    public Tree Apply(Tree fn, Tree[] args)
    {
        return new Apply(fn, args);
    }
    
    public Tree NewObj(Tree encl,   Tree clazz, Tree[] args, Tree def)
    {
        return new NewObj(encl, clazz, args, def, null);
    }
    
    public Tree NewArray(Tree elemtype, Tree[] dims)
    {
        return new NewArray(elemtype, dims);
    }
    
    public Tree Assign(Tree lhs, Tree rhs)
    {
        return new Assign(lhs, rhs);
    }
    
    public Tree Assignop(int opcode, Tree lhs, Tree rhs)
    {
        return new Assignop(opcode, lhs, rhs, null);
    }
    
    public Tree Binop(int opcode, Tree lhs, Tree rhs)
    {
        return new Binop(opcode, lhs, rhs, null);
    }
    
    public Tree Unop(int opcode, Tree operand)
    {
        return new Unop(opcode, operand, null);
    }
    
    public Tree Typeop(int opcode, Tree expr, Tree clazz)
    {
        return new Typeop(opcode, expr, clazz);
    }
    
    public Index Index(Tree indexed, Tree index)
    {
        return new Index(indexed, index);
    }
    
    public Tree Select(Tree selected, Name selector)
    {
        return new Select(selected, selector, null);
    }
    
    public Tree Ident(Name name)
    {
        return new Ident(name, null);
    }
    
    public Tree Self(Tree encl, int tag)
    {
        return new Self(encl, tag, null);
    }
    
    public Tree Literal(Constant value)
    {
        return new Literal(value);
    }
    
    public Tree BasicType(int tag)
    {
        return new BasicType(tag);
    }
    
    public Tree ArrayTypeTerm(Tree elemtype)
    {
        return new ArrayTypeTerm(elemtype);
    }
    
    public Tree Assert(Tree cond, Tree message)
    {
        return new Assert(cond, message);
    }
}

public class TreeCreate implements Tree.Factory, TreeConst
{
    public int              protoPos;
    
    
    public Tree CompilationUnit(Tree[] decls, CompilationEnv info)
    {
        CompilationUnit t = new CompilationUnit(decls, null, null, info);
        t.pos = protoPos;
        return t;
    }
    
    public Tree PackageDecl(Tree qualid)
    {
        PackageDecl t = new PackageDecl(qualid);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Import(int tag, Tree qualid)
    {
        Import  t = new Import(tag, qualid);
        t.pos = protoPos;
        return t;
    }
    
    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] members)
    {
        ClassDecl   t = new ClassDecl(name, mods, extending, implementing, members, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree MethodDecl(Name name, int mods, Tree restype,
                                 VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        MethodDecl  t = new MethodDecl(name, mods, restype, params, thrown, stats, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree VarDecl(Name name, int mods, Tree vartype, Tree init)
    {
        VarDecl t = new VarDecl(name, mods, vartype, init, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Block(int mods, Tree[] stats)
    {
        Block   t = new Block(mods, stats);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Loop(int opcode, Tree cond, Tree body)
    {
        Loop    t = new Loop(opcode, cond, body);
        t.pos = protoPos;
        return t;
    }
    
    public Tree ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body)
    {
        ForLoop t = new ForLoop(inits, cond, steps, body);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Labelled(Name label, Tree body)
    {
        Labelled    t = new Labelled(label, body);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Switch(Tree selector, Case[] cases)
    {
        Switch  t = new Switch(selector, cases);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Case(Tree[] pat, Tree[] stats)
    {
        Case    t = new Case(pat, stats);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Synchronized(Tree lock, Tree body)
    {
        Synchronized    t = new Synchronized(lock, body);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Try(Tree body, Catch[] catchers, Tree finalize)
    {
        Try t = new Try(body, catchers, finalize);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Catch(VarDecl exception, Tree body)
    {
        Catch   t = new Catch(exception, body);
        t.pos = protoPos;
        return t;
    }
    
    public Tree If(Tree cond, Tree thenpart, Tree elsepart)
    {
        If  t = new If(cond, thenpart, elsepart);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Exec(Tree expr)
    {
        Exec    t = new Exec(expr);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Break(Name label)
    {
        Break   t = new Break(label, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Continue(Name label)
    {
        Continue    t = new Continue(label, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Return(Tree expr)
    {
        Return  t = new Return(expr, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Throw(Tree expr)
    {
        Throw   t = new Throw(expr);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Aggregate(Tree[] elems, Tree arrtype)
    {
        Aggregate   t = new Aggregate(elems, arrtype);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Apply(Tree fn, Tree[] args)
    {
        Apply   t = new Apply(fn, args);
        t.pos = protoPos;
        return t;
    }
    
    public Tree NewObj(Tree encl,   Tree clazz, Tree[] args, Tree def)
    {
        NewObj  t = new NewObj(encl, clazz, args, def, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree NewArray(Tree elemtype, Tree[] dims)
    {
        NewArray    t = new NewArray(elemtype, dims);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Assign(Tree lhs, Tree rhs)
    {
        Assign  t = new Assign(lhs, rhs);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Assignop(int opcode, Tree lhs, Tree rhs)
    {
        Assignop    t = new Assignop(opcode, lhs, rhs, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Binop(int opcode, Tree lhs, Tree rhs)
    {
        Binop   t = new Binop(opcode, lhs, rhs, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Unop(int opcode, Tree operand)
    {
        Unop    t = new Unop(opcode, operand, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Typeop(int opcode, Tree expr, Tree clazz)
    {
        Typeop  t = new Typeop(opcode, expr, clazz);
        t.pos = protoPos;
        return t;
    }
    
    public Index Index(Tree indexed, Tree index)
    {
        Index   t = new Index(indexed, index);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Select(Tree selected, Name selector)
    {
        Select  t = new Select(selected, selector, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Ident(Name name)
    {
        Ident   t = new Ident(name, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Self(Tree encl, int tag)
    {
        Self    t = new Self(encl, tag, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Literal(Constant value)
    {
        Literal t = new Literal(value);
        t.pos = protoPos;
        return t;
    }
    
    public Tree BasicType(int tag)
    {
        BasicType   t = new BasicType(tag);
        t.pos = protoPos;
        return t;
    }
    
    public Tree ArrayTypeTerm(Tree elemtype)
    {
        ArrayTypeTerm   t = new ArrayTypeTerm(elemtype);
        t.pos = protoPos;
        return t;
    }
    
    public Tree Assert(Tree cond, Tree message)
    {
        Assert t = new Assert(cond, message);
        t.pos = protoPos;
        return t;
    }
}


public class TreeRedef implements Tree.Factory, TreeConst
{
    public Tree             protoTree;
    
    
    public Tree CompilationUnit(Tree[] decls, CompilationEnv info)
    {
        CompilationUnit t = (CompilationUnit)protoTree;
        t = new CompilationUnit(decls, t.def, t.importScope, info);
        t.pos = protoTree.pos;
        return t;
    }
    
    public Tree PackageDecl(Tree qualid)
    {
        PackageDecl t = new PackageDecl(qualid);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Import(int tag, Tree qualid)
    {
        Import  t = new Import(tag, qualid);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] members)
    {
        ClassDecl   t = (ClassDecl)protoTree;
        t = new ClassDecl(name, mods, extending, implementing, members, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree MethodDecl(Name name, int mods, Tree restype,
                                 VarDecl[] params, Tree[] thrown, Tree[] stats)
    {
        MethodDecl  t = (MethodDecl)protoTree;
        t = new MethodDecl(name, mods, restype, params, thrown, stats, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree VarDecl(Name name, int mods, Tree vartype, Tree init)
    {
        VarDecl t = (VarDecl)protoTree;
        t = new VarDecl(name, mods, vartype, init, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Block(int mods, Tree[] stats)
    {
        Block   t = new Block(mods, stats);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Loop(int opcode, Tree cond, Tree body)
    {
        Loop    t = new Loop(opcode, cond, body);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body)
    {
        ForLoop t = new ForLoop(inits, cond, steps, body);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Labelled(Name label, Tree body)
    {
        Labelled    t = new Labelled(label, body);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Switch(Tree selector, Case[] cases)
    {
        Switch  t = new Switch(selector, cases);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Case(Tree[] pat, Tree[] stats)
    {
        Case    t = new Case(pat, stats);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Synchronized(Tree lock, Tree body)
    {
        Synchronized    t = new Synchronized(lock, body);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Try(Tree body, Catch[] catchers, Tree finalize)
    {
        Try t = new Try(body, catchers, finalize);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Catch(VarDecl exception, Tree body)
    {
        Catch   t = new Catch(exception, body);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree If(Tree cond, Tree thenpart, Tree elsepart)
    {
        If  t = new If(cond, thenpart, elsepart);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Exec(Tree expr)
    {
        Exec    t = new Exec(expr);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Break(Name label)
    {
        Break   t = (Break)protoTree;
        t = new Break(label, t.target);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Continue(Name label)
    {
        Continue    t = (Continue)protoTree;
        t = new Continue(label, t.target);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Return(Tree expr)
    {
        Return  t = (Return)protoTree;
        t = new Return(expr, t.target);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Throw(Tree expr)
    {
        Throw   t = new Throw(expr);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Aggregate(Tree[] elems, Tree arrtype)
    {
        Aggregate   t = new Aggregate(elems, arrtype);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Apply(Tree fn, Tree[] args)
    {
        Apply   t = new Apply(fn, args);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree NewObj(Tree encl,   Tree clazz, Tree[] args, Tree def)
    {
        NewObj  t = (NewObj)protoTree;
        t = new NewObj(encl, clazz, args, def, t.constructor);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree NewArray(Tree elemtype, Tree[] dims)
    {
        NewArray    t = new NewArray(elemtype, dims);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Assign(Tree lhs, Tree rhs)
    {
        Assign  t = new Assign(lhs, rhs);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Assignop(int opcode, Tree lhs, Tree rhs)
    {
        Assignop    t = (Assignop)protoTree;
        t = new Assignop(opcode, lhs, rhs, t.operator);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Binop(int opcode, Tree lhs, Tree rhs)
    {
        Binop   t = (Binop)protoTree;
        t = new Binop(opcode, lhs, rhs, t.operator);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Unop(int opcode, Tree operand)
    {
        Unop    t = (Unop)protoTree;
        t = new Unop(opcode, operand, t.operator);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Typeop(int opcode, Tree expr, Tree clazz)
    {
        Typeop  t = new Typeop(opcode, expr, clazz);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Index Index(Tree indexed, Tree index)
    {
        Index   t = new Index(indexed, index);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Select(Tree selected, Name selector)
    {
        Select  t = (Select)protoTree;
        t = new Select(selected, selector, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Ident(Name name)
    {
        Ident   t = (Ident)protoTree;
        t = new Ident(name, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Self(Tree encl, int tag)
    {
        Self    t = (Self)protoTree;
        t = new Self(encl, tag, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Literal(Constant value)
    {
        Literal t = new Literal(value);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree BasicType(int tag)
    {
        BasicType   t = new BasicType(tag);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree ArrayTypeTerm(Tree elemtype)
    {
        ArrayTypeTerm   t = new ArrayTypeTerm(elemtype);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public Tree Assert(Tree cond, Tree message)
    {
        Assert t = new Assert(cond, message);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
}

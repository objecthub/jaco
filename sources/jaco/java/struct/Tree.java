//      /   _ _      JaCo
//  \  //\ / / \     - internal program representation as an abstract syntax
//   \//  \\_\_/       tree
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import Definition.*;


/** abstract syntax trees for Java
 */
public class Tree
{
    public int  pos = Position.NOPOS;
    public Type type = null;
    
    
    public case Bad();
    
    public case CompilationUnit(Tree[] decls, PackageDef def,
                                Scope[] importScope, CompilationEnv info);
    
    public case PackageDecl(Tree qualid);
    
    public case Import(int tag, Tree qualid);
    
    public case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                            Tree[] members, ClassDef def);
    
    public case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                             Tree[] thrown, Tree[] stats, MethodDef def);
    
    public case VarDecl(Name name, int mods, Tree vartype, Tree init, VarDef def);
        
    public case Block(int mods, Tree[] stats);
    
    public case Loop(int opcode, Tree cond, Tree body);
    
    public case ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body);
    
    public case Labelled(Name label, Tree body);
    
    public case Switch(Tree selector, Case[] cases);
    
    public case Case(Tree[] pat, Tree[] stats);
    
    public case Synchronized(Tree lock, Tree body);
    
    public case Try(Tree body, Catch[] catchers, Tree finalizer);
    
    public case Catch(VarDecl exception, Tree body);
    
    public case If(Tree cond, Tree thenpart, Tree elsepart);
    
    public case Exec(Tree expr);
    
    public case Break(Name label, Tree target);
    
    public case Continue(Name label, Tree target);
    
    public case Return(Tree expr, Tree target);
    
    public case Throw(Tree expr);
    
    public case Aggregate(Tree[] elems, Tree arrtype);
    
    public case Apply(Tree fn, Tree[] args);
    
    public case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def,
                        Definition constructor);
    
    public case NewArray(Tree elemtype, Tree[] dims);
    
    public case Assign(Tree lhs, Tree rhs);
    
    public case Assignop(int opcode, Tree lhs, Tree rhs,
                         Definition operator);
    
    public case Binop(int opcode, Tree lhs, Tree rhs,
                      Definition operator);
    
    public case Unop(int opcode, Tree operand, Definition operator);
    
    public case Typeop(int opcode, Tree expr, Tree clazz);
    
    public case Index(Tree indexed, Tree index);
    
    public case Select(Tree selected, Name selector, Definition def);
    
    public case Ident(Name name, Definition def);
    
    public case Self(Tree encl, int tag, Definition def);
    
    public case Literal(Constant value);
    
    public case BasicType(int tag);
    
    public case ArrayTypeTerm(Tree elemtype);
    
    public case Assert(Tree cond, Tree message);
    
    
    public Tree at(int pos)
    {
        this.pos = pos;
        return this;
    }
    
    public Tree setType(Type type)
    {
        this.type = type;
        return this;
    }
    
    public Tree setDef(Definition def)
    {
        switch (this)
        {
            case CompilationUnit(_, _, _, _):
                ((CompilationUnit)this).def = (PackageDef)def;
                break;
                
            case ClassDecl(_, _, _, _, _, _):
                ((ClassDecl)this).def = (ClassDef)def;
                break;
                
            case MethodDecl(_, _, _, _, _, _, _):
                ((MethodDecl)this).def = (MethodDef)def;
                break;
                
            case VarDecl(_, _, _, _, _):
                ((VarDecl)this).def = (VarDef)def;
                break;
                
            case NewObj(_, _, _, _, _):
                ((NewObj)this).constructor = def;
                break;
                
            case Assignop(_, _, _, _):
                ((Assignop)this).operator = def;
                break;
            
            case Binop(_, _, _, _):
                ((Binop)this).operator = def;
                break;
            
            case Unop(_, _, _):
                ((Unop)this).operator = def;
                break;
                
            case Select(_, _, _):
                ((Select)this).def = def;
                break;
            
            case Ident(_, _):
                ((Ident)this).def = def;
                break;
            
            case Self(_, _, _):
                ((Self)this).def = def;
                break;
            
            default:
        }
        if ((def != null) && (type == null))
            type = def.type;
        return this;
    }
    
    public Definition def()
    {
        switch (this)
        {
            case CompilationUnit(_, PackageDef def, _, _):
                return def;
                
            case ClassDecl(_, _, _, _, _, ClassDef def):
                return def;
                
            case MethodDecl(_, _, _, _, _, _, MethodDef def):
                return def;
                
            case VarDecl(_, _, _, _, VarDef def):
                return def;
                
            case NewObj(_, _, _, _, Definition constructor):
                return constructor;
                
            case Assignop(_, _, _, Definition operator):
                return operator;
            
            case Binop(_, _, _, Definition operator):
                return operator;
            
            case Unop(_, _, Definition operator):
                return operator;
                
            case Select(_, _, Definition def):
                return def;
            
            case Ident(_, Definition def):
                return def;
            
            case Self(_, _, Definition def):
                return def;
            
            default:
                return null;
        }
    }
    
    public int mods()
    {
        switch (this)
        {
            case ClassDecl(_, int mods, _, _, _, _):
                return mods;
            
            case MethodDecl(_, int mods, _, _, _, _, _):
                return mods;
            
            case VarDecl(_, int mods, _, _, _):
                return mods;
            
            case Block(int mods, _):
                return mods;
            
            default:
                return 0;
        }
    }
    
    
    public static interface Factory
    {
        Tree CompilationUnit(Tree[] decls, CompilationEnv info);
        Tree PackageDecl(Tree qualid);
        Tree Import(int tag, Tree qualid);
        Tree ClassDecl(Name name, int mods, Tree extending,
                        Tree[] implementing, Tree[] members);
        Tree MethodDecl(Name name, int mods, Tree restype,
                                 VarDecl[] params, Tree[] thrown, Tree[] stats);
        Tree VarDecl(Name name, int mods, Tree vartype, Tree init);
        Tree Block(int mods, Tree[] stats);
        Tree Loop(int opcode, Tree cond, Tree body);
        Tree ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body);
        Tree Labelled(Name label, Tree body);
        Tree Switch(Tree selector, Case[] cases);
        Tree Case(Tree[] pat, Tree[] stats);
        Tree Synchronized(Tree lock, Tree body);
        Tree Try(Tree body, Catch[] catchers, Tree finalizer);
        Tree Catch(VarDecl exception, Tree body);
        Tree If(Tree cond, Tree thenpart, Tree elsepart);
        Tree Exec(Tree expr);
        Tree Break(Name label);
        Tree Continue(Name label);
        Tree Return(Tree expr);
        Tree Throw(Tree expr);
        Tree Aggregate(Tree[] elems, Tree arrtype);
        Tree Apply(Tree fn, Tree[] args);
        Tree NewObj(Tree encl, Tree clazz, Tree[] args, Tree def);
        Tree NewArray(Tree elemtype, Tree[] dims);
        Tree Assign(Tree lhs, Tree rhs);
        Tree Assignop(int opcode, Tree lhs, Tree rhs);
        Tree Binop(int opcode, Tree lhs, Tree rhs);
        Tree Unop(int opcode, Tree operand);
        Tree Typeop(int opcode, Tree expr, Tree clazz);
        Index Index(Tree indexed, Tree index);
        Tree Select(Tree selected, Name selector);
        Tree Ident(Name name);
        Tree Self(Tree encl, int tag);
        Tree Literal(Constant value);
        Tree BasicType(int tag);
        Tree ArrayTypeTerm(Tree elemtype);
        Tree Assert(Tree cond, Tree message);
    }
}

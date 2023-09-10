package jaco.join.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.join.component.*;


/** abstract syntax trees for Join
 */
public class JTree
{
    public int pos = Position.NOPOS;
    public JType type = null;
    
    
    public case Bad();
    
    public case TopLevel(JTree[] terms, Tree.CompilationUnit unit);
    
    public case Literal(Constant lit);
    
    public case SpecialLiteral(int tag);
    
    public case Ident(Name name);
    
    public case Select(JTree selected, Name selector);
    
    public case Apply(JTree fun, JTree arg);
    
    public case Oper(int opcode, JTree left, JTree right);
    
    public case If(JTree cond, JTree ifterm, JTree elseterm);
    
    public case Let(JTree head, JTree body);
    
    public case Join(JTree left, JTree right);
    
    public case Tupel(JTree[] trees);
    
    public case Decl(JTree lhs, JTree rhs, JScope scope);
    
    public case Decls(JTree[] decls, JScope scope);
    
    public case Rec(JTree[] decls, JScope scope);
    
    
    public static final JTree at(int pos, JTree tree)
    {
        tree.pos = pos;
        return tree;
    }
    
    public JTree setType(JType type)
    {
        this.type = type;
        return this;
    }
    
    public JTree setDef(Object def)
    {
        /* switch (this)
        {
            
            default:
        } */
        //if ((def != null) && (type == null))
        //  type = def.type;
        return this;
    }
    
    public Object def()
    {
    /*
        switch (this)
        {
            
            default:
                return null;
        }
        */
        return null;
    }
    
    public static interface Factory
    {
        JTree Bad();
        JTree TopLevel(JTree[] terms, Tree.CompilationUnit unit);
        JTree Literal(Constant lit);
                JTree SpecialLiteral(int tag);
        JTree Ident(Name name);
        JTree Select(JTree selected, Name selector);
        JTree Apply(JTree fun, JTree arg);
        JTree Oper(int opcode, JTree left, JTree right);
        JTree If(JTree cond, JTree ifterm, JTree elseterm);
        JTree Let(JTree head, JTree body);
        JTree Join(JTree left, JTree right);
        JTree Tupel(JTree[] trees);
        JTree Decl(JTree lhs, JTree rhs);
        JTree Decls(JTree[] decls);
                JTree Rec(JTree[] decls);
    }
}

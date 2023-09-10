package jaco.join.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import JTree.*;


public class JTreeFactory implements JTree.Factory
{
    public JTree Bad()
    {
        return new Bad();
    }
    
    public JTree TopLevel(JTree[] terms, Tree.CompilationUnit unit)
    {
        return new TopLevel(terms, unit);
    }
    
    public JTree Literal(Constant lit)
    {
        return new Literal(lit);
    }
    
        public JTree SpecialLiteral(int tag)
    {
        return new SpecialLiteral(tag);
    }
        
    public JTree Ident(Name name)
    {
        return new Ident(name);
    }
    
    public JTree Select(JTree selected, Name selector)
    {
        return new Select(selected, selector);
    }
    
    public JTree Apply(JTree fun, JTree arg)
    {
        return new Apply(fun, arg);
    }
    
    public JTree Oper(int opcode, JTree left, JTree right)
    {
        return new Oper(opcode, left, right);
    }
    
    public JTree If(JTree cond, JTree ifterm, JTree elseterm)
    {
        return new If(cond, ifterm, elseterm);
    }
    
    public JTree Let(JTree head, JTree body)
    {
        return new Let(head, body);
    }
    
    public JTree Join(JTree left, JTree right)
    {
        return new Join(left, right);
    }
    
    public JTree Tupel(JTree[] trees)
    {
        return new Tupel(trees);
    }
    
    public JTree Decl(JTree lhs, JTree rhs)
    {
        return new Decl(lhs, rhs, null);
    }
    
    public JTree Decls(JTree[] decls)
    {
        return new Decls(decls, null);
    }
        
        public JTree Rec(JTree[] decls)
    {
        return new Rec(decls, null);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - factory for PatternNodes
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;


public class PatternNodeFactory implements PatternNode.Factory
{
    public PatternNode Slot(int pos, Type type, Tree switchTree, Name casted,
                              Type guard)
    {
        PatternNode p = PatternNode.Slot(switchTree, casted, guard);
        p.type = type;
        p.pos = pos;
        return p;
    }
    
    public PatternNode SwitchHeader(int pos, Type type, Tree switchTree, Name casted)
    {
        PatternNode p = PatternNode.SwitchHeader(switchTree, casted, null);
        p.type = type;
        p.pos = pos;
        p.and = p;
        return p;
    }
    
    public PatternNode ConstrPat(int pos, Type type, int tag, int args)
    {
        PatternNode p = PatternNode.ConstrPat(tag, args);
        p.type = type;
        p.pos = pos;
        return p;
    }
    
    public PatternNode ConstantPat(int pos, Type type)
    {
        PatternNode p = PatternNode.ConstantPat(type.tconst());
        p.type = type;
        p.pos = pos;
        return p;
    }
    
    public PatternNode DefaultPat(int pos, Type type)
    {
        PatternNode p = PatternNode.DefaultPat();
        p.type = type;
        p.pos = pos;
        return p;
    }
    
    public PatternNode VariablePat(int pos, Tree tree)
    {
        PatternNode p = PatternNode.VariablePat(tree);
        p.type = tree.type;
        p.pos = pos;
        return p;
    }
    
    public PatternNode BodyNode(int pos, VarDecl[] boundVars, Tree[] stats)
    {
        PatternNode p = PatternNode.BodyNode(boundVars, stats);
        p.pos = pos;
        return p;
    }
}

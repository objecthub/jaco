//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    factory for pattern nodes
//                           
//  [ModuleSet.java (3119) 16-May-01 16:58 -> 9-Jul-01 01:39]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;


public class PatternNodeFactory implements PatternNode.Factory {
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
    
    public PatternNode BodyNode(int pos, VarDecl[] boundVars, Tree[] stats)
    {
        PatternNode p = PatternNode.BodyNode(boundVars, stats);
        p.pos = pos;
        return p;
    }
}

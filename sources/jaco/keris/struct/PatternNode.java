//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    internal representation for pattern matcher
//                           
//  [ModuleSet.java (3119) 16-May-01 16:58 -> 9-Jul-01 01:39]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;


public class PatternNode {
    public int              pos = Position.NOPOS;
    public Type             type;
    public PatternNode      or;
    public PatternNode      and;
    
    public case Slot(Tree switchTree, Name casted, Type guard);
    public case SwitchHeader(Tree switchTree, Name casted, SwitchHeader next);
    public case DefaultPat();
    public case ConstrPat(int tag, int args);
    public case ConstantPat(Constant value);
    public case BodyNode(VarDecl[] boundVars, Tree[] stats);
    
    public static interface Factory {
        PatternNode Slot(int pos, Type type, Tree switchTree, Name casted, Type guard);
        PatternNode SwitchHeader(int pos, Type type, Tree switchTree, Name casted);
        PatternNode ConstrPat(int pos, Type type, int tag, int args);
        PatternNode DefaultPat(int pos, Type type);
        PatternNode ConstantPat(int pos, Type type);
        PatternNode BodyNode(int pos, VarDecl[] boundVars, Tree[] stats);
    }
}

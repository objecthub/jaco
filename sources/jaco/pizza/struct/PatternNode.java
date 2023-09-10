//      /   _ _      JaCo
//  \  //\ / / \     - internal representation for translation of pattern matching
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Tree.*;


public class PatternNode
{
    public int              pos = Position.NOPOS;
    public Type             type;
    public PatternNode      or;
    public PatternNode      and;
    
    public case Slot(Tree switchTree, Name casted, Type guard);
    public case SwitchHeader(Tree switchTree, Name casted, SwitchHeader next);
    public case DefaultPat();
    public case ConstrPat(int tag, int args);
    public case ConstantPat(Constant value);
    public case VariablePat(Tree tree);
    public case BodyNode(VarDecl[] boundVars, Tree[] stats);
    
    public static interface Factory
    {
        PatternNode Slot(int pos, Type type, Tree switchTree, Name casted, Type guard);
        PatternNode SwitchHeader(int pos, Type type, Tree switchTree, Name casted);
        PatternNode ConstrPat(int pos, Type type, int tag, int args);
        PatternNode DefaultPat(int pos, Type type);
        PatternNode ConstantPat(int pos, Type type);
        PatternNode VariablePat(int pos, Tree tree);
        PatternNode BodyNode(int pos, VarDecl[] boundVars, Tree[] stats);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - items are addressable entities, used to generate code
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import Definition.*;


public class Item implements ItemConst
{
/** JVM type code of the item
 */
    public int  typecode;
    
    public case VoidItem;
    public case StackItem();
    public case IndexedItem(Type type);
    public case SelfItem(boolean isSuper);
    public case LocalItem(int pos, int adr, Type type);
    public case StaticItem(int pos, Definition member);
    public case MemberItem(int pos, Definition member,
                            boolean nonvirtual, boolean selfref);
    public case ImmediateItem(Type type);
    public case AssignItem(Item lhs);
    public case CondItem(int opcode, Chain trueJumps, Chain falseJumps);
    
    static
    {
        VoidItem.typecode = VOID_CODE;
    }
    
    public int pos()
    {
        switch (this)
        {
            case LocalItem(int pos, _, _):
                return pos;
            
            case StaticItem(int pos, _):
                return pos;
            
            case MemberItem(int pos, _, _, _):
                return pos;
            
            default:
                throw new InternalError();
        }
    }
    
    public Definition def()
    {
        switch (this)
        {
            case StaticItem(_, Definition member):
                return member;
            
            case MemberItem(_, Definition member, _, _):
                return member;
            
            default:
                throw new InternalError();
        }
    }
    
    public Type type()
    {
        switch (this)
        {
            case IndexedItem(Type type):
                return type;
            
            case LocalItem(_, _, Type type):
                return type;
            
            case StaticItem(_, Definition member):
                return member.type;
            
            case MemberItem(_, Definition member, _, _):
                return member.type;
            
            case ImmediateItem(Type type):
                return type;
            
            default:
                throw new InternalError();
        }
    }
    
    public static interface Factory
    {
    /** a mapping from types to JVM typecodes (used implicitly by the factory)
     */
        int typecode(Type type);
    
    /** the factory methods
     */
        Item StackItem(int typecode);
        Item IndexedItem(Type type);
        Item SelfItem(boolean isSuper);
        Item LocalItem(int pos, int adr, Type type);
        Item LocalItem(int pos, VarDef def);
        Item StaticItem(int pos, Definition member);
        Item MemberItem(int pos, Definition member,
                            boolean nonvirtual, boolean selfref);
        Item ImmediateItem(Type type);
        Item AssignItem(Item lhs);
        Item CondItem(int opcode, Chain trueJumps, Chain falseJumps);
    }
}


/** A chain represents a list of unresolved jumps
 */
public class Chain
{
//////////// invariant: all elements of a chain list have the same stacksize

/** the next jump in the list
 */
    public Chain next;

/** the position of the jump instruction
 */
    public int pc;

/** the stacksize after the jump instruction
 */
    public int stacksize;

/** the set of possibly uninitialized variables immediately before the jump
 */
    public Bits uninits;

/** the set of possibly initialized variables immediately before the jump
 */
    public Bits inits;


    public Chain(int pc, Chain next, int stacksize, Bits uninits, Bits inits)
    {
        this.pc = pc;
        this.next = next;
        this.stacksize = stacksize;
        this.uninits = uninits;
        this.inits = inits;
    }
}

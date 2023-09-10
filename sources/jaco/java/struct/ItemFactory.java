//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.java.component.*;
import Item.*;
import Definition.*;


public class ItemFactory implements Item.Factory, BytecodeTags, ItemConst, TypeConst
{
/** singleton items
 */
    public Item     thisItem;
    public Item     superItem;
    public Item[]   stackItem;

    
    public ItemFactory()
    {
        thisItem = new SelfItem(false);
        thisItem.typecode = OBJECT_CODE;
        superItem = new SelfItem(true);
        superItem.typecode = OBJECT_CODE;
        stackItem = new Item[TypeCodeCount];
        for (int i = 0; i < TypeCodeCount; i++)
        {
            stackItem[i]   = new StackItem();
            stackItem[i].typecode = i;
        }
        stackItem[VOID_CODE] = Item.VoidItem;
    }
    
    public int typecode(Type type)
    {
        switch (type)
        {
            case NumType(int tag):
                switch (tag)
                {
                    case BYTE:
                        return BYTE_CODE;
                    
                    case SHORT:
                        return SHORT_CODE;
                    
                    case CHAR:
                        return CHAR_CODE;
                    
                    case INT:
                        return INT_CODE;
                    
                    case LONG:
                        return LONG_CODE;
                    
                    case FLOAT:
                        return FLOAT_CODE;
                    
                    case DOUBLE:
                        return DOUBLE_CODE;
                    
                    case BOOLEAN:
                        return BYTE_CODE;
                
                    default:
                        throw new InternalError();
                }
                
            case VoidType:
                return VOID_CODE;
            
            case NullType:
            case ClassType(_):
            case ArrayType(_):
            case MethodType(_, _, _):
                return OBJECT_CODE;
            
            default:
                throw new InternalError("type " + type);
        }
    }


    public Item StackItem(int typecode)
    {
        return stackItem[typecode];
    }
    
    public Item IndexedItem(Type type)
    {
        IndexedItem i = new IndexedItem(type);
        i.typecode = typecode(type);
        return i;
    }
    
    public Item SelfItem(boolean isSuper)
    {
        return isSuper ? superItem : thisItem;
    }
    
    public Item LocalItem(int pos, int adr, Type type)
    {
        LocalItem   i = new LocalItem(pos, adr, type);
        i.typecode = typecode(type);
        return i;
    }
    
    public Item LocalItem(int pos, VarDef def)
    {
        return LocalItem(pos, def.adr, def.type);
    }
    
    public Item StaticItem(int pos, Definition member)
    {
        StaticItem  i = new StaticItem(pos, member);
        i.typecode = typecode(member.type);
        return i;
    }
    
    public Item MemberItem(int pos, Definition member,
                            boolean nonvirtual, boolean selfref)
    {
        MemberItem  i = new MemberItem(pos, member, nonvirtual, selfref);
        i.typecode = typecode(member.type);
        return i;
    }
    
    public Item ImmediateItem(Type type)
    {
        if (type.isTypeOfConstant())
        {
            ImmediateItem   i = new ImmediateItem(type);
            i.typecode = typecode(type);
            return i;
        }
        else
            throw new InternalError();
    }
    
    public Item AssignItem(Item lhs)
    {
        AssignItem  i = new AssignItem(lhs);
        i.typecode = lhs.typecode;
        return i;
    }
    
    public Item CondItem(int opcode, Chain trueJumps, Chain falseJumps)
    {
        CondItem    i = new CondItem(opcode, trueJumps, falseJumps);
        i.typecode = BYTE_CODE;
        return i;
    }
}

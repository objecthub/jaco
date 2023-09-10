//      /   _ _      JaCo
//  \  //\ / / \     - library for items
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Item.*;
import Type.*;
import Definition.*;


public interface ItemConst
{
/** type codes
 */
    int INT_CODE = 0;
    int LONG_CODE = 1;
    int FLOAT_CODE = 2;
    int DOUBLE_CODE = 3;
    int OBJECT_CODE = 4;
    int BYTE_CODE = 5;
    int CHAR_CODE = 6;
    int SHORT_CODE = 7;
    int VOID_CODE = 8;
    
    int TypeCodeCount = 9;

/** shift * mask constants for shifting prefix instructions.
 *  a pair of instruction codes such as LCMP ; IFEQ is encoded
 *  in Symtab as (LCMP << preShift) + IFEQ.
 */
    final int preShift = 9;
    final int preMask   = 0x01FF;
}

public class Items extends Component
                   implements ModifierConst, TypeConst, BytecodeTags,
                              ItemConst, ConstantConst
{
/** other components
 */
    protected ErrorHandler      report;
    protected Constants         constants;
    protected Coder             coder;
    
/** factory for items
 */
    public Item.Factory         make;

    
/** component name
 */
    public String getName()
    {
        return "Items";
    }

/** component initialization
 */
    public void init(BackendContext context)
    {
        super.init(context);
        make = new ItemFactory();
        report = context.compilerContext.mainContext.ErrorHandler();
        constants = context.compilerContext.mainContext.Constants();
        coder = context.Coder();
    }



/** generate code to load object onto stack
 */
    public strictfp Item load(Item that)
    {
        switch (that)
        {
            case StackItem():
                return that;
            
            case IndexedItem(_):
                coder.emitop(iaload + that.typecode);
                break;
            
            case SelfItem(_):
                coder.emitop(aload_0);
                break;
            
            case LocalItem(int pos, int adr, _):
                if (adr < 0)
                {
                    report.error(pos, "variable might not have been initialized");
                    coder.emitop(iload_0 + coder.truncate(that.typecode) * 4);
                }
                else
                {
                    coder.checkInit(pos, adr);
                    int reg = coder.regOf(adr);
                    if (reg <= 3)
                        coder.emitop(iload_0 + coder.truncate(that.typecode) * 4 + reg);
                    else
                        coder.emitop1w(iload + coder.truncate(that.typecode), reg);
                }
                break;
            
            case StaticItem(int pos, Definition member):
                if ((member.modifiers & FINAL) != 0)
                    coder.checkInit(pos, (VarDef)member);
                coder.emitop(getstatic, coder.width(that.typecode));
                coder.emit2(coder.mkref(member));
                break;
            
            case MemberItem(int pos, Definition member, _, boolean selfref):
                if (selfref && ((member.modifiers & FINAL) != 0))
                    coder.checkInit(pos, (VarDef)member);
                coder.emitop(getfield, coder.width(that.typecode) - 1);
                coder.emit2(coder.mkref(member));
                break;
            
            case ImmediateItem(Type type):
                Constant    value = type.tconst();
                switch (that.typecode)
                {
                    case INT_CODE:
                    case BYTE_CODE:
                    case SHORT_CODE:
                    case CHAR_CODE:
                        int ival = value.intValue();
                        if ((-1 <= ival) && (ival <= 5))
                            coder.emitop(iconst_0 + ival);
                        else
                        if (MIN_BYTE <= ival && ival <= MAX_BYTE)
                            coder.emitop1(bipush, ival);
                        else
                        if (MIN_SHORT <= ival && ival <= MAX_SHORT)
                            coder.emitop2(sipush, ival);
                        else
                            ldc(that);
                        break;
                    
                    case LONG_CODE:
                        long    lval = value.longValue();
                        if ((lval == 0) || (lval == 1))
                            coder.emitop(lconst_0 + (int)lval);
                        else
                            ldc(that);
                        break;

                    case FLOAT_CODE:
                        float   fval = value.floatValue();
                        if (fval == 0.0 || fval == 1.0 || fval == 2.0)
                            coder.emitop(fconst_0 + (int)fval);
                        else
                            ldc(that);
                        break;
                
                    case DOUBLE_CODE:
                        double  dval = value.doubleValue();
                        if (dval == 0.0 || dval == 1.0)
                            coder.emitop(dconst_0 + (int)dval);
                        else
                            ldc(that);
                        break;
                    
                    case OBJECT_CODE:
                        ldc(that);
                        break;

                    default:
                        throw new InternalError();
                }
                break;
            
            case AssignItem(Item lhs):
                stash(lhs, that.typecode);
                store(lhs);
                break;
            
            case CondItem(int opcode, Chain trueJumps, Chain falseJumps):
                Chain   trueExit = null;
                Chain   falseChain = jump(false, (CondItem)that);
                if ((trueJumps != null) || (opcode != coder.dontgoto))
                {
                    coder.resolve(trueJumps);
                    coder.emitop(iconst_1);
                    trueExit = coder.branch(goto_);
                }
                if (falseChain != null)
                {
                    coder.resolve(falseChain);
                    coder.emitop(iconst_0);
                }
                coder.resolve(trueExit);
                break;
            
            default:
                throw new InternalError();
        }
        return make.StackItem(that.typecode);
    }

/** generate code to overwrite object with top of stack.
 */
    public Item store(Item that)
    {
        switch (that)
        {
            case IndexedItem(_):
                coder.emitop(iastore + that.typecode);
                break;
            
            case LocalItem(int pos, int adr, _):
                int reg = coder.regOf(adr);
                if (reg <= 3)
                    coder.emitop(istore_0 + coder.truncate(that.typecode) * 4 + reg);
                else
                    coder.emitop1w(istore + coder.truncate(that.typecode), reg);
                if ((adr < coder.locals.length) && (coder.locals[adr] != null) &&
                        ((coder.locals[adr].modifiers & FINAL) != 0))
                        coder.checkUninit(pos, adr);
                    coder.letInit(adr);
                break;
            
            case StaticItem(int pos, Definition member):
                if ((member.modifiers & FINAL) != 0)
                    coder.checkFirstInit(pos, (VarDef)member);
                coder.emitop(putstatic, -coder.width(that.typecode));
                coder.emit2(coder.mkref(member));
                break;
            
            case MemberItem(int pos, Definition member, _, boolean selfref):
                if (selfref && ((member.modifiers & FINAL) != 0))
                    coder.checkFirstInit(pos, (VarDef)member);
                coder.emitop(putfield, -coder.width(that.typecode) - 1);
                coder.emit2(coder.mkref(member));
                break;
            
            default:
                throw new InternalError();
        }
        return Item.VoidItem;
    }
    
/** generate code to call function represented by item
 */
    public Item invoke(Item that)
    {
        switch (that)
        {
            case StaticItem(int pos, Definition member):
                MethodType  ftype = (MethodType)member.type;
                int         argsize = coder.width(ftype.argtypes);
                int         rescode = make.typecode(ftype.restype);
                int         sdiff = coder.width(rescode) - argsize;
                coder.emitop(invokestatic, sdiff);
                coder.emit2(coder.mkref(member));
                return make.StackItem(rescode);
            
            case MemberItem(int pos, Definition member, boolean nonvirtual, _):
                MethodType  ftype = (MethodType)member.type;
                int         argsize = coder.width(ftype.argtypes);
                int         rescode = make.typecode(ftype.restype);
                int         sdiff = coder.width(rescode) - argsize;
                if ((member.owner.modifiers & INTERFACE) != 0)
                {
                    coder.emitop(invokeinterface, sdiff - 1);
                    coder.emit2(coder.mkref(member));
                    coder.emit1(argsize + 1);
                    coder.emit1(0);
                }
                else
                if (nonvirtual)
                {
                    coder.emitop(invokespecial, sdiff - 1);
                    coder.emit2(coder.mkref(member));
                }
                else
                {
                    coder.emitop(invokevirtual, sdiff - 1);
                    coder.emit2(coder.mkref(member));
                }
                return make.StackItem(rescode);
            
            default:
                throw new InternalError();
        }
    }

/** generate code to use item twice.
 */
    public void duplicate(Item that)
    {
        switch (that)
        {
            case StackItem():
                coder.emitop(coder.width(that.typecode) == 2 ? dup2 : dup);
                break;
            
            case IndexedItem(_):
                coder.emitop(dup2);
                break;
            
            case MemberItem(_, _, _, _):
                duplicate(make.StackItem(OBJECT_CODE));
                break;
            
            case AssignItem(_):
                duplicate(load(that));
                break;
            
            case CondItem(_, _, _):
                duplicate(load(that));
                break;
        }
    }

/** generate code to avoid having to use item
 */
    public void drop(Item that)
    {
        switch (that)
        {
            case StackItem():
                coder.emitop(coder.width(that.typecode) == 2 ? pop2 : pop); 
                break;
            
            case IndexedItem(_):
                coder.emitop(pop2);
                break;
            
            case MemberItem(_, _, _, _):
                drop(make.StackItem(OBJECT_CODE));
                break;
            
            case AssignItem(Item lhs):
                store(lhs);
                break;
            
            case CondItem(_, _, _):
                drop(load(that));
                break;
        }
    }

/** generate code to stash a copy of top of stack - of typecode toscode  -
 *  under item
 */
    public void stash(Item that, int toscode)
    {
        switch (that)
        {
            case StackItem():
                coder.emitop((coder.width(that.typecode) == 2 ? dup_x2 : dup_x1) +
                            3 * (coder.width(toscode) - 1));
                break;
            
            case IndexedItem(_):
                coder.emitop(dup_x2 + 3 * (coder.width(toscode) - 1));
                break;
                
            case MemberItem(_, _, _, _):
                stash(make.StackItem(OBJECT_CODE), toscode);
                break;
            
            case AssignItem(_):
                throw new InternalError();
            
            case CondItem(_, _, _):
                throw new InternalError();
            
            default:
                duplicate(make.StackItem(toscode));
        }
    }
    
/** generate code to turn item into a testable condition
 */
    public CondItem mkCond(Item that)
    {
        switch (that)
        {
            case CondItem(_, _, _):
                return (CondItem)that;
            
            case ImmediateItem(Type type):
                return (CondItem)make.CondItem(
                            (type.tconst().intValue() == 0) ?
                                coder.dontgoto : goto_, null, null);
            
            default:
                load(that);
                return (CondItem)make.CondItem(ifne, null, null);
        }
    }
    
/** generate code to coerce item to type code 'targetcode'
 */
    public strictfp Item coerce(Item that, int targetcode)
    {
        switch (that)
        {
            case ImmediateItem(Type type):
                Constant    value = type.tconst();
                if (that.typecode == targetcode)
                    return that;
                else
                    switch (targetcode)
                    {
                        case BYTE_CODE:
                            return make.ImmediateItem(constants.toType(
                                constants.make.IntConst((byte)value.intValue(), BYTE)));
                        
                        case CHAR_CODE:
                            return make.ImmediateItem(constants.toType(
                                constants.make.IntConst((char)value.intValue(), CHAR)));
                        
                        case SHORT_CODE:
                            return make.ImmediateItem(constants.toType(
                                constants.make.IntConst((short)value.intValue(), SHORT)));
                        
                        case INT_CODE:
                            if (coder.truncate(that.typecode) == INT_CODE)
                                return that;
                            else
                                return make.ImmediateItem(constants.toType(
                                    constants.make.IntConst(value.intValue())));
                        
                        case LONG_CODE:
                            return make.ImmediateItem(constants.toType(
                                    constants.make.LongConst(value.longValue())));
                        
                        case FLOAT_CODE:
                            return make.ImmediateItem(constants.toType(
                                    constants.make.FloatConst(value.floatValue())));
                        
                        case DOUBLE_CODE:
                            return make.ImmediateItem(constants.toType(
                                    constants.make.DoubleConst(value.doubleValue())));
                    }
        }
        if (that.typecode == targetcode)
            return that;
        else
        {
            load(that);
            int typeCode = coder.truncate(that.typecode);
            int targetCode = coder.truncate(targetcode);
            if (typeCode != targetCode)
            {
                int offset = (targetCode > typeCode) ? targetCode - 1 : targetCode;
                coder.emitop(i2l + typeCode * 3 + offset);
            }
            if (targetcode != targetCode)
                coder.emitop(int2byte + targetcode - BYTE_CODE);
            return make.StackItem(targetcode);
        }
    }
    
    public Item coerce(Item that, Type targettype)
    {
        return coerce(that, make.typecode(targettype));
    }
    
    public void incr(Item that, int x)
    {
        switch (that)
        {
            case LocalItem(int pos, int adr, _):
                if ((that.typecode == INT_CODE) &&
                    (x >= -0x8000) && (x <= 0x7fff))
                {
                    coder.checkInit(pos, adr);
                    if ((adr < coder.locals.length) &&
                        (coder.locals[adr] != null) &&
                        ((coder.locals[adr].modifiers & FINAL) != 0))
                        coder.checkUninit(pos, adr);
                    int reg = coder.regOf(adr);
                    if ((reg > 0xff) || (x < -128) || (x > 127))
                    {
                        coder.emit1(wide);
                        coder.emitop2(iinc, reg);
                        coder.emit2(x);
                    }
                    else
                    {
                        coder.emitop1(iinc, reg);
                        coder.emit1(x);
                    }
                }
                else
                {
                    load(that);
                    load(make.ImmediateItem(constants.toType(constants.make.IntConst(x))));
                    coder.emitop(iadd);
                    coerce(make.StackItem(INT_CODE), that.typecode);
                    store(that);
                }
        }
    }

/** load a constant
 */
    protected void ldc(Item that)
    {
        switch (that)
        {
            case ImmediateItem(Type type):
                Constant    value = type.tconst();
                int         idx = coder.putConstant(value);
                if ((that.typecode == LONG_CODE) || (that.typecode == DOUBLE_CODE))
                {
                    coder.emitop(ldc2_w, 2);
                    coder.emit2(idx);
                }
                else
                if (idx <= 255)
                {
                    coder.emitop(ldc, 1);
                    coder.emit1(idx);
                }
                else
                {
                    coder.emitop(ldc_w, 1);
                    coder.emit2(idx);
                }
                break;
                
            default:
                throw new InternalError();
        }
    }

/** load an integer constant
 */
    public Item loadIntConst(int n)
    {
        return load(make.ImmediateItem(constants.toType(constants.make.IntConst(n))));
    }

    public Chain jump(boolean where, CondItem ci)
    {
        if (where)
            return coder.mergeChains(ci.trueJumps, coder.branch(ci.opcode));
        else
            return coder.mergeChains(ci.falseJumps, coder.branch(coder.negate(ci.opcode)));
    }

    public Item negate(CondItem ci)
    {
        return make.CondItem(coder.negate(ci.opcode), ci.falseJumps, ci.trueJumps);
    }

/** make a tempory variable
 */
    public Item makeTemp(Type type)
    {
        return make.LocalItem(Position.NOPOS, coder.newLocal(type), type);
    }
}

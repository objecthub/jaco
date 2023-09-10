//      /   _ _      JaCo
//  \  //\ / / \     - library for constants
//   \//  \\_\_/     
//         \         Matthias Zenger, 24/01/99

package jaco.java.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import Definition.*;
import Type.*;


public interface ConstantConst
{
/** minimal and maximal values of bytes, shorts, chars and ints
 */
    static final byte MIN_BYTE = -0x80;
    static final byte MAX_BYTE = 0x7f;

    static final short MIN_SHORT = -0x8000;
    static final short MAX_SHORT = 0x7fff;

    static final char MIN_CHAR = 0;
    static final char MAX_CHAR = 0xffff;

    static final int MIN_INT = 0x80000000;
    static final int MAX_INT = 0x7fffffff;
}

public strictfp class Constants extends Component
                                implements  ConstantConst, ModifierConst,
                                            TypeConst, ItemConst, BytecodeTags
{
/** other components
 */
    protected ErrorHandler          report;
    protected Types                 types;
    protected Definitions           definitions;

/** constant factory
 */
    public Constant.Factory make;

/** java constants
 */
    public Definition               nullConst;
    public Definition               trueConst;
    public Definition               falseConst;

    
/** component name
 */
    public String getName()
    {
        return "JavaConstants";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        report = context.ErrorHandler();
        types = context.Types();
        definitions = context.Definitions();
        
        // create factory
        make = new ConstantFactory();
        
        // enter constants
        enterConstants();
    }
    
/** initialize all predefined constants
 */
    protected void enterConstants()
    {
        nullConst = enterConstant("null", types.nullType);
                                // types.nullType.setConst(make.NullConst()));
        trueConst = enterConstant("true", toType(make.IntConst(1, BOOLEAN)));
        falseConst = enterConstant("false", toType(make.IntConst(0, BOOLEAN)));
    }

/** enter a constant in the predefined class
 */
    public Definition enterConstant(String name, Type type)
    {
        Definition c = definitions.make.VarDef(PUBLIC | STATIC | FINAL,
                                                Name.fromString(name),
                                            type,
                                            definitions.predefClass);
        definitions.predefClass.locals().enter(c);
        return c;
    }
    
/** return type of the constant (not as a 'constant type')
 */
    public Type typeOf(Constant c)
    {
        switch (c)
        {
            case NullConst:
                return types.nullType;
                
            case IntConst(_):
                return types.typeOfTag[c.tag()];
                
            case LongConst(_):
                return types.longType;
            
            case FloatConst(_):
                return types.floatType;
            
            case DoubleConst(_):
                return types.doubleType;
            
            case StringConst(_):
                return types.stringType;
            
            default:
                throw new InternalError();
        }
    }

/** return type for the constant
 */
    public Type toType(Constant c)
    {
        switch (c)
        {
            case NullConst:
                return types.nullType;
                
            case IntConst(_):
                return types.make.NumType(c.tag()).
                                    setDef(types.classOfTag[c.tag()]).
                                    setConst(c);
                
            case LongConst(_):
                return types.longType.dup().setConst(c);
            
            case FloatConst(_):
                return types.floatType.dup().setConst(c);
            
            case DoubleConst(_):
                return types.doubleType.dup().setConst(c);
            
            case StringConst(_):
                return types.stringType.dup().setConst(c);
            
            default:
                throw new InternalError();
        }
    }
    
/** is constant 'c' assignable to a variable of type 'that'?
 */
    public boolean assignable(Constant c, Type that)
    {
        switch (c)
        {
            case IntConst(int value):
                if (c.tag() != BOOLEAN)
                {
                    switch (that.deref())
                    {
                        case NumType(int thattag):
                            switch (thattag)
                            {
                                case BYTE:
                                    if ((MIN_BYTE <= value) && (value <= MAX_BYTE))
                                        return true;
                                    break;
                                
                                case SHORT:
                                    if ((MIN_SHORT <= value) && (value <= MAX_SHORT))
                                        return true;
                                    break;
                                
                                case CHAR:
                                    if ((MIN_CHAR <= value) && (value <= MAX_CHAR))
                                        return true;
                                    break;
                                
                                case INT:
                                    return true;
                            }
                            break;
                    }
                    
                }
                break;
        }
        return types.subtype(typeOf(c), that);
    }

/** coerce constant 'c' to type 'that' and return resulting constant
 */
    public Constant coerce(Constant c, Type that)
    {
        switch (c)
        {
            case NullConst:
                if (that.isRef())
                    return c;
                else
                    throw new InternalError();
            
            case StringConst(Name str):
                return c;
            
            default:
                if (that.tag() == c.tag())
                    return c;
                else
                    switch (that.tag())
                    {
                        case BOOLEAN:
                        case BYTE:
                        case CHAR:
                        case SHORT:
                        case INT:
                            return make.IntConst(c.intValue(), that.tag());
                        
                        case LONG:
                            return make.LongConst(c.longValue());
                        
                        case FLOAT:
                            return make.FloatConst(c.floatValue());
                        
                        case DOUBLE:
                            return make.DoubleConst(c.doubleValue());
                        
                        default:
                            throw new InternalError();
                    }
        }
    }
    
/** convert boolean to integer (true = 1, false = 0)
 */
    static protected int b2i(boolean b)
    {
        return b ? 1 : 0;
    }

/** fold binary operation
 */
    public Constant fold(int pos, Constant l, Constant r, int opc, int restag)
    {
        try
        {
            if (opc >= (1 << preShift))
            {
                Constant    c = fold(pos, l, r, opc >> preShift, INT);
                if (c == null)
                    return null;
                else
                    return fold(pos, c, opc & preMask, restag);
            }
            switch (opc)
            {
                case iadd:
                    return make.IntConst(l.intValue() + r.intValue(), restag);
                case isub:
                    return make.IntConst(l.intValue() - r.intValue(), restag);
                case imul:
                    return make.IntConst(l.intValue() * r.intValue(), restag);
                case idiv:
                    return make.IntConst(l.intValue() / r.intValue(), restag);
                case imod:
                    return make.IntConst(l.intValue() % r.intValue(), restag);
                case iand:
                case bool_and:
                    return make.IntConst(l.intValue() & r.intValue(), restag);
                case ior:
                case bool_or:
                    return make.IntConst(l.intValue() | r.intValue(), restag);
                case ixor:
                    return make.IntConst(l.intValue() ^ r.intValue(), restag);
                case ishl:
                case ishll:
                    return make.IntConst(l.intValue() << r.intValue(), restag);
                case ishr:
                case ishrl:
                    return make.IntConst(l.intValue() >> r.intValue(), restag);
                case iushr:
                case iushrl:
                    return make.IntConst(l.intValue() >>> r.intValue(), restag);
                case if_icmpeq:
                    return make.IntConst(b2i(l.intValue()==r.intValue()), restag);
                case if_icmpne:
                    return make.IntConst(b2i(l.intValue()!=r.intValue()), restag);
                case if_icmplt:
                    return make.IntConst(b2i(l.intValue() < r.intValue()), restag);
                case if_icmpgt:
                    return make.IntConst(b2i(l.intValue() > r.intValue()), restag);
                case if_icmple:
                    return make.IntConst(b2i(l.intValue() <= r.intValue()), restag);
                case if_icmpge:
                    return make.IntConst(b2i(l.intValue() >= r.intValue()), restag);

                case ladd:
                    return make.LongConst(l.longValue() + r.longValue());
                case lsub:
                    return make.LongConst(l.longValue() - r.longValue());
                case lmul:
                    return make.LongConst(l.longValue() * r.longValue());
                case ldiv:
                    return make.LongConst(l.longValue() / r.longValue());
                case lmod:
                    return make.LongConst(l.longValue() % r.longValue());
                case land:
                    return make.LongConst(l.longValue() & r.longValue());
                case lor:
                    return make.LongConst(l.longValue() | r.longValue());
                case lxor:
                    return make.LongConst(l.longValue() ^ r.longValue());
                case lshl:
                case lshll:
                    return make.LongConst(l.longValue() << r.intValue());
                case lshr:
                case lshrl:
                    return make.LongConst(l.longValue() >> r.intValue());
                case lushr:
                    return make.LongConst(l.longValue() >>> r.intValue());
                case lcmp:
                    if (l.longValue() < r.longValue())
                        return make.IntConst(-1, restag);
                    else
                    if (l.longValue() > r.longValue())
                        return make.IntConst(1, restag);
                    else
                        return make.IntConst(0, restag);
                case fadd:
                    return make.FloatConst(l.floatValue() + r.floatValue());
                case fsub:
                    return make.FloatConst(l.floatValue() - r.floatValue());
                case fmul:
                    return make.FloatConst(l.floatValue() * r.floatValue());
                case fdiv:
                    return make.FloatConst(l.floatValue() / r.floatValue());
                case fmod:
                    return make.FloatConst(l.floatValue() % r.floatValue());
                case fcmpg:
                case fcmpl:
                    if (l.floatValue() < r.floatValue())
                        return make.IntConst(-1, restag);
                    else
                    if (l.floatValue() > r.floatValue())
                        return make.IntConst(1, restag);
                    else
                    if (l.floatValue() == r.floatValue())
                        return make.IntConst(0, restag);
                    else
                    if (opc == fcmpg)
                        return make.IntConst(1, restag);
                    else
                        return make.IntConst(-1, restag);
        
                case dadd:
                    return make.DoubleConst(l.doubleValue() + r.doubleValue());
                case dsub:
                    return make.DoubleConst(l.doubleValue() - r.doubleValue());
                case dmul:
                    return make.DoubleConst(l.doubleValue() * r.doubleValue());
                case ddiv:
                    return make.DoubleConst(l.doubleValue() / r.doubleValue());
                case dmod:
                    return make.DoubleConst(l.doubleValue() % r.doubleValue());
                
                case dcmpg:
                case dcmpl:
                    if (l.doubleValue() < r.doubleValue())
                        return make.IntConst(-1, restag);
                    else
                    if (l.doubleValue() > r.doubleValue())
                        return make.IntConst(1, restag);
                    else
                    if (l.doubleValue() == r.doubleValue())
                        return make.IntConst(0, restag);
                    else
                    if (opc == dcmpg)
                        return make.IntConst(1, restag);
                    else
                        return make.IntConst(-1, restag);

                case string_add:
                    return make.StringConst(Name.fromString(l.stringValue() + r.stringValue()));
        
                default:
                    return null;
            }
        }
        catch (ArithmeticException e)
        {
            return null;
        }
    }

/** fold unary operation
 */
    public Constant fold(int pos, Constant od, int opc, int restag)
    {
        try
        {
            switch (opc)
            {
                case nop:
                    if ((restag >= MIN_BASICTYPE_TAG) && (restag <= INT))
                        return make.IntConst(od.intValue(), restag);
                    else
                        return od;
                
                case ineg:
                    return make.IntConst(-od.intValue(), restag);
                
                case ixor:
                    return make.IntConst(~od.intValue(), restag);
                
                case bool_not:
                    return make.IntConst(~od.intValue() & 1, restag);
                
                case ifeq:
                    return make.IntConst(b2i(od.intValue() == 0), restag);
                    
                case ifne:
                    return make.IntConst(b2i(od.intValue() != 0), restag);
                
                case iflt:
                    return make.IntConst(b2i(od.intValue() < 0), restag);
                
                case ifgt:
                    return make.IntConst(b2i(od.intValue() > 0), restag);
                    
                case ifle:
                    return make.IntConst(b2i(od.intValue() <= 0), restag);
                    
                case ifge:
                    return make.IntConst(b2i(od.intValue() >= 0), restag);

                case lneg:
                    return make.LongConst(-od.longValue());
                
                case lxor:
                    return make.LongConst(~od.longValue());

                case fneg:
                    return make.FloatConst(-od.floatValue());

                case dneg:
                    return make.DoubleConst(-od.doubleValue());

                default:
                    return null;
            }
        }
        catch (ArithmeticException e)
        {
            return null;
        }
    }
}

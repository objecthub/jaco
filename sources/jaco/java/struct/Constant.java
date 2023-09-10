//      /   _ _      JaCo
//  \  //\ / / \     - internal representation of constants
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;


public strictfp class Constant implements TypeConst
{
/** type tag
 */
    public int  tag;
    
    public case NullConst;
    public case IntConst(int value);
    public case LongConst(long value);
    public case FloatConst(float value);
    public case DoubleConst(double value);
    public case StringConst(Name str);
    
    static
    {
        NullConst.tag = NULL;
    }
    
    public int intValue()
    {
        switch (this)
        {
            case IntConst(int value):
                return value;
            
            case LongConst(long value):
                return (int)value;
            
            case FloatConst(float value):
                return (int)value;
            
            case DoubleConst(double value):
                return (int)value;
            
            default:
                throw new InternalError();
        }
    }
    
    public long longValue()
    {
        switch (this)
        {
            case IntConst(int value):
                return (long)value;
            
            case LongConst(long value):
                return value;
            
            case FloatConst(float value):
                return (long)value;
            
            case DoubleConst(double value):
                return (long)value;
            
            default:
                throw new InternalError();
        }
    }
    
    public float floatValue()
    {
        switch (this)
        {
            case IntConst(int value):
                return (float)value;
            
            case LongConst(long value):
                return (float)value;
            
            case FloatConst(float value):
                return value;
            
            case DoubleConst(double value):
                return (float)value;
            
            default:
                throw new InternalError();
        }
    }
    
    public double doubleValue()
    {
        switch (this)
        {
            case IntConst(int value):
                return (double)value;
            
            case LongConst(long value):
                return (double)value;
            
            case FloatConst(float value):
                return (double)value;
            
            case DoubleConst(double value):
                return value;
            
            default:
                throw new InternalError();
        }
    }
    
    public String stringValue()
    {
        switch (this)
        {
            case NullConst:
                return "null";
            
            case IntConst(int value):
                if (tag == CHAR)
                    return String.valueOf((char)value);
                else
                if (tag == BOOLEAN)
                    return (value == 0) ? "false" : "true";
                else
                    return String.valueOf(value);
                
            case LongConst(long value):
                return String.valueOf(value);
            
            case FloatConst(float value):
                return String.valueOf(value);
            
            case DoubleConst(double value):
                return String.valueOf(value);
            
            case StringConst(Name str):
                return str.toString();
            
            default:
                throw new InternalError();
        }
    }
    
    public String toString()
    {
        switch (this)
        {
            case IntConst(int value):
                String  s = stringValue();
                if (tag == CHAR)
                    return "\'" + SourceRepr.escape(s) + "\'";
                else
                    return s;
                
            case LongConst(long value):
                return stringValue() + "L";
            
            case FloatConst(float value):
                return stringValue() + "F";
            
            case StringConst(Name str):
                return "\"" + SourceRepr.escape(stringValue()) + "\"";
            
            default:
                return stringValue();
        }
    }
    
    public int hashCode()
    {
        switch (this)
        {
            case NullConst:
                return 0;
            
            case IntConst(int value):
                return value;
                
            case LongConst(long value):
                return (int)value;
            
            case FloatConst(float value):
                return (int)(value * 10000);
            
            case DoubleConst(double value):
                return (int)(value * 10000);
            
            case StringConst(Name str):
                return str.index;
            
            default:
                throw new InternalError();
        }
    }
    
    public boolean equals(Object other)
    {
        switch (this)
        {
            case NullConst:
                return (this == other);
            
            case IntConst(int value):
                return  (other instanceof IntConst) &&
                        (value == ((IntConst)other).value);
                
            case LongConst(long value):
                return  (other instanceof LongConst) &&
                        (value == ((LongConst)other).value);
            
            case FloatConst(float value):
                return  (other instanceof FloatConst) &&
                        (Float.floatToIntBits(value) ==
                         Float.floatToIntBits(((FloatConst)other).value));
            
            case DoubleConst(double value):
                return  (other instanceof DoubleConst) &&
                        (Double.doubleToLongBits(value) ==
                         Double.doubleToLongBits(((DoubleConst)other).value));
            
            case StringConst(Name str):
                return  (other instanceof StringConst) &&
                        (str == ((StringConst)other).str);
            
            default:
                throw new InternalError();
        }
    }
    
    public int tag()
    {
        return tag;
    }
    
    
    public static interface Factory
    {
        Constant NullConst();
        Constant IntConst(int value);
        Constant IntConst(int value, int tag);
        Constant LongConst(long value);
        Constant FloatConst(float value);
        Constant DoubleConst(double value);
        Constant StringConst(Name str);
    }
}

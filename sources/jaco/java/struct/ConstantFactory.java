//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import Constant.*;


public strictfp class ConstantFactory implements Constant.Factory, TypeConst
{
/** number of predefined ints
 */
    protected int   predefInts = 17;

/** predefined constants for common literals
 */
    Constant[]      boolConst;
    Constant[]      intConst;
    
    
    public ConstantFactory()
    {
        boolConst = new Constant[2];
        boolConst[0] = new IntConst(0);
        boolConst[0].tag = BOOLEAN;
        boolConst[1] = new IntConst(1);
        boolConst[1].tag = BOOLEAN;
        
        intConst = new Constant[predefInts];
        for (int i = 0; i < predefInts; i++)
        {
            intConst[i] = new IntConst(i);
            intConst[i].tag = INT;
        }
    }
    
    public Constant NullConst()
    {
        return Constant.NullConst;
    }
    
    public Constant IntConst(int value)
    {
        if ((value >= 0) && (value < predefInts))
            return intConst[value];
        else
        {
            IntConst    c = new IntConst(value);
            c.tag = INT;
            return c;
        }
    }
    
    public Constant IntConst(int value, int tag)
    {
        if (tag == BOOLEAN)
            return (value == 0) ? boolConst[0] : boolConst[1];
        else
        if ((tag == INT) && (value >= 0) && (value < predefInts))
            return intConst[value];
        else
        {
            IntConst    c = new IntConst(value);
            c.tag = tag;
            return c;
        }
    }
    
    public Constant LongConst(long value)
    {
        LongConst   c = new LongConst(value);
        c.tag = LONG;
        return c;
    }
    
    public Constant FloatConst(float value)
    {
        FloatConst  c = new FloatConst(value);
        c.tag = FLOAT;
        return c;
    }
    
    public Constant DoubleConst(double value)
    {
        DoubleConst c = new DoubleConst(value);
        c.tag = DOUBLE;
        return c;
    }
    
    public Constant StringConst(Name str)
    {
        StringConst c = new StringConst(str);
        c.tag = REF;
        return c;
    }
}

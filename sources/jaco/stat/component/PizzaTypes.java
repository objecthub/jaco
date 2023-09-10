//      /   _ _      JaCo
//  \  //\ / / \     - adapted type functions
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.stat.context.*;


public class PizzaTypes extends Types implements PizzaModifierConst
{
    public String getName()
    {
        return "PizzaTypes";
    }
    
    public boolean isAlgebraicType(Type type)
    {
        return (type != null) && ((type.tdef().modifiers & ALGEBRAIC) != 0);
    }
    
    public boolean isCaseType(Type type)
    {
        return (type != null) && ((type.tdef().modifiers & CASEDEF) != 0);
    }
    
    public Type algebraicSupertype(Type type)
    {
        if (type == null)
            return null;
        else
        if ((type.tdef().complete().modifiers & ALGEBRAIC) != 0)
            return type;
        else
            return algebraicSupertype(type.supertype());
    }
    
    public boolean subtype(Type thi, Type that)
    {
        if (thi == that)
            return true;
        else
        if ((thi == null) || (that == null))
            return false;
        switch (that)
        {
            case ErrType:
            case AnyType:
                    return true;
        }
        switch (thi)
        {
            case ClassType(_):
                switch (that)
                {
                    case ClassType(_):
                        if (thi.tdef() == that.tdef())
                            return true;
                        else
                        if (((MainPizzaContext)context).algebraicClasses &&
                            isAlgebraicType(that))
                        {
                            if (isAlgebraicType(thi))
                                return subtype(thi.supertype(), that);
                            else
                            {
                                do
                                {
                                    thi = thi.supertype();
                                }
                                while ((thi != null) && !isAlgebraicType(thi));
                                return subtype(that, thi) || subtype(thi, that);
                            }
                        }
                        else
                        {
                            if (subtype(thi.supertype(), that))
                                return true;
                            Type[] is = thi.interfaces();
                            for (int i = 0; i < is.length; i++)
                                if (subtype(is[i], that))
                                    return true;
                            return false;
                        }
                }
                return false;
            
            default:
                return super.subtype(thi, that);
        }
    }
    
    public boolean javaSubtype(Type thi, Type that)
    {
        return super.subtype(thi, that);
    }
}

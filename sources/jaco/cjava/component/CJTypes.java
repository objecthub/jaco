package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import java.util.*;


/**
 * Extension of Types that takes compound types into account.
 */
public class CJTypes extends Types
{
    public Type serializableType;
    /** component name
     */
    public String getName()
    {
    return "CJTypes";
    }

    /** component initialization
     */
    public void init(MainContext context)
    {
    super.init(context);
    make = new CJTypeFactory(this);
    serializableType = enterJavaClass("java.io.Serializable");
    }    

    /** check whether thi is the same type as that.
     *
     * CompoundTypes:
     *
     *  two CompoundTypes are the same if they contain the same
     *  interfaces and classes. The order is not significant.
     */
    public boolean sametype(Type thi, Type that)
    {
    thi = followAlias(thi);
    that = followAlias(that);
    if(!super.sametype(thi, that))
        {
        if(thi==null || that==null)
            return false;

        switch((CJType)thi)
            {
            case CompoundType(_):
            switch((CJType)that)
                {
                case CompoundType(_):
                {
                    if(sametype(thi.tdef().supertype(), that.tdef().supertype()))
                    {
                        Type[] componentsA = thi.tdef().interfaces();
                        Type[] componentsB = that.tdef().interfaces();
                        if(componentsA.length==componentsB.length)
                        {
                            int found = 0;
                            int l = componentsA.length;
                            boolean[] map = new boolean[l];
                            for(int i=0; i<l; i++)
                            map[i] = false;
                            
                        Aloop: for(int i=0; i<l; i++)
                            {
                            Bloop: for(int j=0; j<l; j++)
                            {
                                if(!map[j])
                                {
                                    if(sametype(componentsA[i], componentsB[j]))
                                    {
                                        map[j] = true;
                                        found ++;
                                        break Bloop;
                                    }
                                }
                            }
                            if(found!=i+1)
                            return false;
                            }
                            if(found==l)
                            return true;
                        }
                    }
                }
                }
            }
        return false;
        }
    return true;
    }

    /** check whether thi is assignable to that.
     *
     * CompoundTypes:
     *
     *  true if thi is a subtype of that; nothing to add. 
     */
    public boolean assignable(Type thi, Type that)
    {
    thi = followAlias(thi);
    that = followAlias(that);
    return super.assignable(thi, that);
    }

    /** check whether thi is a subtype of that.
     *
     * CompoundTypes:
     *
     *  thi is a subtype of a compound that if
     *  thi is a subtype of all the types in the compound.
     *
     *  A compound thi is a subtype of that if 
     *  that is a supertype of at least one of the types in
     *  the compound. 
     *
     */
    public boolean subtype(Type thi, Type that)
    {
    thi = followAlias(thi);
    that = followAlias(that);

    if(!super.subtype(thi, that))
        {
        if(thi==null || that==null)
            return false;

        /* bug fix ? */
        if(thi instanceof Type.ArrayType && (sametype(that, cloneableType) || sametype(that, serializableType)))
            return true;
        
        switch((CJType)thi)
            {
            case CompoundType(Type[] thiC):
            switch((CJType)that)
                {
                case CompoundType(Type[] thatC):
                if(sametype(thi, that))
                    return true;
                }

            for(int i=0; i<thiC.length; i++)
                {
                if(subtype(thiC[i], that))
                    return true;
                }
            }

        switch((CJType)that)
            {
            case CompoundType(Type[] thatC):
            for(int i=0; i<thatC.length; i++)
                {
                if(!subtype(thi, thatC[i]))
                    return false;
                }
            return true;
            }

        return false;
        }
    return true;
    }
    

    /** 
     *
     * A compound type is treated as a class type here. 
     * It relies on subtype(), which should be fine. 
     */
    protected boolean subelemtype(Type thi, Type that)
    {
    thi = followAlias(thi);
    that = followAlias(that);

    if(!super.subelemtype(thi, that))
        {
        switch((CJType)thi)
            {
            case CompoundType(_):
            return subtype(thi, that);
            }
        return false;
        }
    return true;
    }

    /** check whether thi can be casted to that.
     *
     * I don't really understand the purpose of this method...
     * 
     * CompoundTypes:
     *
     *  thi can be casted to a compound that as long as it
     *  could be casted to all the members of the compound.
     *
     *  a compound thi can be casted to that if at least one 
     *  member of the compound can be casted to that.
     */
    public boolean castable(Type thi, Type that)
    {
    thi = followAlias(thi);
    that = followAlias(that);
    
    if(!super.castable(thi, that))
        {
        // bug fix ? 
        if(thi !=null && that!=null)
            {
            if(that instanceof Type.ArrayType && (sametype(thi, cloneableType) || sametype(thi, serializableType)))
                return true;
            }
        
        switch((CJType)that)
            {
            case CompoundType(Type[] thatComp):
            {
                /* cast from some type to 
                 * a compound type.
                 */
                for(int i=0; i<thatComp.length; i++)
                {
                    if(!castable(thi, thatComp[i]))
                    return false;
                }
                return true;
            }
            
            default:
            switch((CJType)thi)
                {
                case CompoundType(Type[] thisComp):
                /* cast from a compound type to 
                 * a class
                 */
                for(int i=0; i<thisComp.length; i++)
                    {
                    if(castable(thisComp[i], that))
                        return true;
                    }
                return false;
                }
            }
        return false;
        }
    return true;
    }

    /**
     * Follow AliasType.
     *
     * @param type original type
     * @param type, the real type
     */
    protected Type followAlias(Type t)
    {
    if(t==null)
        return t;
    int debug_i =0;
    while(true)
        {
        switch((CJType)t)
            {
            case AliasType(_):
            t = t.deref();
            debug_i++;
            break;
            
            default:
            return t;
            }
        if(debug_i>120)
            throw new InternalError(t.tdef().fullname.toString());
        }
    }

}

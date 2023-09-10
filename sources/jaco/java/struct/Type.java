//      /   _ _      JaCo
//  \  //\ / / \     - internal representation of types
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.java.component.*;


public class Type
{
/** defining class
 */
    protected Definition    tdef;

/** is it a type of a constant?
 */
    protected Constant      tconst;


/** type 'void'
 */
    public case VoidType;

/** type of 'null'
 */
    public case NullType;

/** basic type
 */
    public case NumType(int tag);

/** class type
 *  @param outer outer class type if this is a (non-static) inner class
 *               types.packageType if not
 */
    public case ClassType(Type outer);

/** array type
 *  @param elemtype type of the elements in the array
 */
    public case ArrayType(Type elemtype);

/** method type
 *  @param argtypes method parameter types
 *  @param restype  type of the return value
 *  @param thrown   exceptions thrown by the method
 */
    public case MethodType(Type[] argtypes, Type restype, Type[] thrown);

/** package type
 */
    public case PackageType();

/** unknown, or arbitrary type
 */
    public case AnyType;

/** invalid type.
 */
    public case ErrType;


/** copy a type
 */
    public Type dup()
    {
        Type    res = this;
        switch (this)
        {
            case NumType(int tag):
                res = NumType(tag);
                break;
            
            case ClassType(Type outer):
                res = ClassType(outer);
                break;
            
            case ArrayType(Type elemtype):
                res = ArrayType(elemtype);
                break;

            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                res = MethodType(argtypes, restype, thrown);
                break;
            
            case PackageType():
                res = PackageType();
                break;
        }
        res.tdef = tdef;
        res.tconst = tconst;
        return res;
    }

/** return a string representation of the type
 */
    public String toString()
    {
        switch (this)
        {
            case ClassType(Type outer):
                switch (outer)
                {
                    case ClassType(_):
                        return outer + "." + tdef.name;
                    
                    default:
                        return tdef.fullname.toString();
                }
            
            case ArrayType(Type elemtype):
                return elemtype + "[]";
            
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                String  throwns = ")";
                if (thrown.length > 0)
                    throwns = ") throws " + Tools.toString(thrown);
                return restype + " (" + Tools.toString(argtypes) + throwns;
            
            default:
                if (tdef == null)
                    return super.toString();
                else
                    return tdef.name.toString();
        }
    }

/** return the type symbol of this type
 */
    public Definition tdef()
    {
        return tdef;
    }

/** set the type symbol for this type
 */ 
    public Type setDef(Definition tdef)
    {
        this.tdef = tdef;
        return this;
    }

/** return the constant, if it's a type of a special constant
 */
    public Constant tconst()
    {
        return tconst;
    }
    
/** set a constant for this type
 */
    public Type setConst(Constant tconst)
    {
        this.tconst = tconst;
        return this;
    }
    
/** return the type that results from following variable binding links
 */
    public Type deref()
    {
        return this;
    }

/** is this a type of a constant?
 */
    public boolean isTypeOfConstant()
    {
        return (tconst != null);
    }

/** only for arrays; returns the type of the elements
 */
    public Type elemtype()
    {
        switch (this)
        {
            case ArrayType(Type elemtype):
                return elemtype;

            default:
                throw new InternalError();
        }
    }

/** only for methods; returns the types of the arguments
 */
    public Type[] argtypes()
    {
        switch (this)
        {
            case MethodType(Type[] argtypes, _, _):
                return argtypes;
            
            default:
                throw new InternalError();
        }
    }

/** only for methods; returns the type of the return value
 */
    public Type restype()
    {
        switch (this)
        {
            case MethodType(_, Type restype, _):
                return restype;
        }
        throw new InternalError("restype " + this);
    }

/** only for functions; returns all thrown exceptions
 */
    public Type[] thrown()
    {
        switch (this)
        {
            case MethodType(_, _, Type[] thrown):
                return thrown;
            
            default:
                throw new InternalError("thrown " + this);
        }
    }

/** for class types; returns the type of the outer class
 */
    public Type outer()
    {
        switch (this)
        {
            case ClassType(Type outer):
                if (tdef.completer != null)
                {
                    tdef.complete();
                    return outer();
                }
                return outer;
            
            default:
                throw new InternalError("outer " + this);
        }
    }

/** returns the supertype of this type.
 */
    public Type supertype()
    {
        switch (this)
        {
            case ClassType(_):
                return tdef.supertype();
        
            default:
                return null;
        }
    }

/** returns the types of implemented interfaces
 */
    public Type[] interfaces()
    {
        switch (this)
        {
            case ClassType(_):
                return tdef.interfaces();

            default:
                return null;
        }
    }

/** return the tag of a type
 */
    public int tag()
    {
        switch (this)
        {
            case NumType(int tag):
                return tag;
                
            case VoidType:
                return TypeConst.VOID;
            
            case NullType:
                return TypeConst.NULL;
            
            case ClassType(_):
                return TypeConst.REF;
            
            case ArrayType(_):
                return TypeConst.REF;
                
            default:
                return TypeConst.NONE;
        }
    }

/** is this type a basic type?
 */
    public boolean isBasic()
    {
        return (tag() >= TypeConst.MIN_BASICTYPE_TAG);
    }

/** is this a reference type?
 */
    public boolean isRef()
    {
        switch (tag())
        {
            case TypeConst.REF:
                return true;
            
            case TypeConst.NULL:
                return true;
            
            default:
                return false;
        }
    }
    
/** does this type contain "error" elements?
 */
    public boolean erroneous()
    {
        switch (this)
        {
            case ErrType:
                return true;
            
            case ClassType(_):
                return outer().erroneous();
            
            case ArrayType(Type elemtype):
                return elemtype.erroneous();
            
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                for (int i = 0; i < thrown.length; i++)
                    if (thrown[i].erroneous())
                        return true;
                for (int i = 0; i < argtypes.length; i++)
                    if (argtypes[i].erroneous())
                        return true;
                return restype.erroneous();
            
            default:
                return false;
        }
    }
    

    public static interface Factory
    {
        Type VoidType();
        Type NullType();
        Type NumType(int tag);
        Type ClassType(Type outer);
        Type ArrayType(Type elemtype);
        Type MethodType(Type[] argtypes, Type restype, Type[] thrown);
        Type PackageType();
    }
}

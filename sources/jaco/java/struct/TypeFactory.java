//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import Type.*;
import Definition.*;


public class TypeFactory implements Type.Factory, TypeConst
{
    Types       types;
    

    public TypeFactory(Types types)
    {
        this.types = types;
    }
    
    
    public Type VoidType()
    {
        return Type.VoidType;
    }
    
    public Type NullType()
    {
        return Type.NullType;
    }
    
    public Type NumType(int tag)
    {
        return Type.NumType(tag);
    }
    
    public Type ClassType(Type outer)
    {
        return Type.ClassType(outer);
    }
    
    public Type ArrayType(Type elemtype)
    {
        return Type.ArrayType(elemtype).setDef(types.arrayType.tdef());
    }
    
    public Type MethodType(Type[] argtypes, Type restype, Type[] thrown)
    {
        return Type.MethodType(argtypes, restype, thrown).
                        setDef(types.methodType.tdef());
    }
    
    public Type PackageType()
    {
        return Type.PackageType();
    }
}

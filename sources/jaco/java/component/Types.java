//      /   _ _      JaCo
//  \  //\ / / \     - library for types
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;


public interface TypeConst
{
    static final int NONE = 0;
    static final int VOID = 1;
    static final int NULL = 2;
    static final int REF = 3;
    
    static final int BYTE = 10;
    static final int CHAR = 11;
    static final int SHORT = 12;
    static final int INT = 13;
    static final int LONG = 14;
    static final int FLOAT = 15;
    static final int DOUBLE = 16;
    static final int BOOLEAN = 17;
    
    static final int MIN_BASICTYPE_TAG = 10;
    static final int MAX_BASICTYPE_TAG = 100;
}

public class Types extends Component implements TypeConst, ModifierConst
{
/** other components
 */
    protected Trees         trees;
    protected Definitions   definitions;
    protected Constants     constants;
    protected Mangler       mangler;
    
/** factory for types
 */
    public Type.Factory     make;

/** number of basic types
 */
    protected int           basiccount = 18;

/** basic type strings, types and classes
 */
    public String[]         stringOfTag;
    public Name[]           boxedName;
    public Type[]           typeOfTag;
    public Definition[]     classOfTag;

/** length field of arrays
 */
    public Definition       lengthVar;

/** predefined types
 */
    public Type[]       noTypes;
    public Type         packageType;
    public Type         methodType;
    public Type         arrayType;

/** basic types
 */
    public Type         doubleType;
    public Type         floatType;
    public Type         longType;
    public Type         intType;
    public Type         charType;
    public Type         shortType;
    public Type         byteType;
    public Type         booleanType;
    public Type         voidType;
    public Type         nullType;

/** essential java types
 */
    public Type         objectType;
    public Type         classType;
    public Type         stringType;
    public Type         throwableType;
    public Type         errorType;
    public Type         runtimeExceptionType;
    public Type         objectArrayType;
    public Type         cloneableType;
    public Type         serializableType;
    public Type         cloneNotSupportedExceptionType;
    public Type         assertionErrorType;
    public Type         assertionParameterType;

/** component name
 */
    public String getName()
    {
        return "JavaTypes";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        trees = context.Trees();
        mangler = context.Mangler();
        
        // create factory
        if (make == null)
            make = new TypeFactory(this);
        
        // initialize artificial types
        noTypes = new Type[0];
        packageType = make.PackageType();
        methodType  = make.ClassType(packageType);
        arrayType   = make.ClassType(packageType);
        
        // init component Definitions
        definitions = context.Definitions();
        definitions.initComponent(context);
        
        // create basic types
        initBasicTypes();
        loadClasses();
        
        // complete artificial types
        completeType(methodType, "<method>");
        completeType(arrayType, "<array>");
        completeType(Type.ErrType, "<error>");
        completeType(Type.AnyType, "<anything>");
        Scope   scope = definitions.predefClass.locals();
        scope.enter(Type.AnyType.tdef());
        scope.enter(Type.ErrType.tdef());
        
        // complete array type
        initArrayType();
        
        // setup Scope error definition
        Type.ErrType.tdef().setLocals(Scope.errScope);
        Scope.errDef = definitions.make.ErrorDef(definitions.emptyPackage);
        Scope.errScope.owner = Type.ErrType.tdef();
        
        // now, after Definitions and Types are initialized, we can
        // get component Constants
        constants = context.Constants();
    }


/** initialize basic types
 */
    protected void initBasicTypes()
    {
        stringOfTag = new String[basiccount];
        boxedName   = new Name[basiccount];
        typeOfTag   = new Type[basiccount];
        classOfTag  = new Definition[basiccount];
        
        nullType    = enterBasicType(NULL, "null", make.NullType(), null);
        voidType    = enterBasicType(VOID, "void", make.VoidType(), "java.lang.Void");
        byteType    = enterBasicType(BYTE, "byte", "java.lang.Byte");
        charType    = enterBasicType(CHAR, "char", "java.lang.Character");
        shortType   = enterBasicType(SHORT, "short", "java.lang.Short");
        intType     = enterBasicType(INT, "int", "java.lang.Integer");
        longType    = enterBasicType(LONG, "long", "java.lang.Long");
        floatType   = enterBasicType(FLOAT, "float", "java.lang.Float");
        doubleType  = enterBasicType(DOUBLE, "double", "java.lang.Double");
        booleanType = enterBasicType(BOOLEAN, "boolean", "java.lang.Boolean");
    }

/** load basic java classes
 */
    protected void loadClasses()
    {
        objectType              = enterJavaClass("java.lang.Object");
         classType              = enterJavaClass("java.lang.Class");
         stringType             = enterJavaClass("java.lang.String");
         throwableType          = enterJavaClass("java.lang.Throwable");
         errorType              = enterJavaClass("java.lang.Error");
         runtimeExceptionType   = enterJavaClass("java.lang.RuntimeException");
         cloneableType          = enterJavaClass("java.lang.Cloneable");
         serializableType       = enterJavaClass("java.io.Serializable");
         cloneNotSupportedExceptionType =
                    enterJavaClass("java.lang.CloneNotSupportedException");
         assertionErrorType     = errorType;
         assertionParameterType = stringType;
    }
    
    protected void updateAssertionType() {
        if ("1.4".equals(((JavaSettings)context.settings).targetversion)) {
            assertionErrorType = enterJavaClass("java.lang.AssertionError");
            assertionParameterType = objectType;
        }
    }
    
/** initialize virtual array class
 */
    protected void initArrayType()
    {
        objectArrayType = make.ArrayType(objectType);
        Definition arrayClass = arrayType.tdef();
        arrayClass.setSupertype(objectType);
        arrayClass.setInterfaces(new Type[]{cloneableType});
        arrayClass.setLocals(new Scope(null, arrayClass));
        lengthVar = definitions.make.VarDef(PUBLIC | FINAL,
                                        Name.fromString("length"),
                                        intType, arrayClass);
        arrayClass.locals().enter(lengthVar);
        Definition  cloneMethod = definitions.make.MethodDef(
                                PUBLIC,
                                Name.fromString("clone"),
                                make.MethodType(noTypes, objectType, noTypes),
                                objectType.tdef());
        arrayClass.locals().enter(cloneMethod);
    }

/** create new basic type
 */
    public Type enterBasicType(int tag, String name, String boxedname)
    {
        Type    type = make.NumType(tag);
        stringOfTag[tag] = name;
        if (boxedname != null)
            boxedName[tag] = Name.fromString(boxedname);
        type = (typeOfTag[tag] = type.setDef(
                    classOfTag[tag] = definitions.make.ClassDef(
                                    PUBLIC, Name.fromString(name), type,
                                    definitions.emptyPackage)));
        definitions.predefClass.locals().enter(type.tdef());
        return type;
    }
    
    public Type enterBasicType(int tag, String name, Type type, String boxedname)
    {
        stringOfTag[tag] = name;
        if (boxedname != null)
            boxedName[tag] = Name.fromString(boxedname);
        typeOfTag[tag] = type.setDef(definitions.make.ClassDef(
                                    PUBLIC, Name.fromString(name), type,
                                    definitions.emptyPackage));
        definitions.predefClass.locals().enter(type.tdef());
        return type;
    }

/** complete type definition
 */
    public void completeType(Type type, String name)
    {
        type.setDef(definitions.make.ClassDef(PUBLIC,
                                Name.fromString(name),
                                type, definitions.emptyPackage));
    }

/** define a type by a Java class
 */
    public Type enterJavaClass(String name)
    {
        return definitions.defineClass(Name.fromString(name)).type;
    }

/** return name of basic type
 */
    public String toString(int basicType)
    {
        return (basicType < basiccount) ? stringOfTag[basicType] :
                                     ("<basic type#" + basicType);
    }


/** do the types contain any 'error' definitions
 */
    public boolean erroneous(Type[] ts)
    {
        if (ts != null)
        {
            for (int i = 0; i < ts.length; i++)
                if (ts[i].erroneous())
                    return true;
        }
        return false;
    }
   
/** are the types in 'as' subtypes of the corresponding types in 'bs'
 */
    public boolean subtypes(Type[] as, Type[] bs)
    {
        if (as.length != bs.length)
            return false;
        else
        {
            for (int i = 0; i < as.length; i++)
                if (!subtype(as[i], bs[i]))
                    return false;
            return true;
        }
    }
    
/** is type 'thi' a subtype of type 'that'?
 */
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
            case NullType:
                return that.isRef();
                
            case NumType(int thistag):
                switch (that)
                {
                    case NumType(int thattag):
                        if (thistag == thattag)
                            return true;
                        int nexttag = thistag <= CHAR ?
                                        thistag + 2 :
                                        thistag + 1;
                        if ((nexttag <= thattag) && (thattag <= DOUBLE))
                            return true;
                }
                break;
            
            case ClassType(_):
                switch (that)
                {
                    case ClassType(_):
                        if (thi.tdef() == that.tdef())
                            return true;
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
                break;

            case ArrayType(Type elemthis):
                switch (that)
                {
                    case ClassType(_):
                        return (that.tdef() == objectType.tdef()) ||
                               (that.tdef() == cloneableType.tdef()) ||
                               (that.tdef() == serializableType.tdef());

                    case ArrayType(Type elemthat):
                        return subelemtype(elemthis, elemthat);
                }
                break;

            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                return (that.tdef().type == objectType) || sametype(thi, that);
            
            case ErrType:
                return true;
        }
        return false;
    }
    
/** are the types in 'as' equal to the corresponding types in 'bs'
 */
    public boolean sametypes(Type[] as, Type[] bs)
    {
        if (as.length != bs.length)
            return false;
        else
        {
            for (int i = 0; i < as.length; i++)
                if (!sametype(as[i], bs[i]))
                    return false;
            return true;
        }
    }
    
/** are types 'thi' and 'that' equal
 */
    public boolean sametype(Type thi, Type that)
    {
        if (thi == that)
            return true;
        else
        if ((thi == null) || (that == null))
            return false;

        switch (that)
        {
            case ErrType:
                return true;
        }

        switch (thi)
        {
            case ErrType:
                return true;

            case NumType(int thistag):
                switch (that)
                {
                    case NumType(int thattag):
                        return (thistag == thattag);
                    }
                    break;

            case ClassType(_):
                switch (that)
                {
                    case ClassType(_):
                        if (thi.tdef() == that.tdef())
                            return sametype(thi.outer(), that.outer());
                }
                break;

            case ArrayType(Type elemthis):
                switch (that)
                {
                    case ArrayType(Type elemthat):
                        return sametype(elemthis, elemthat);
                }
                break;

            case MethodType(Type[] argsthis, Type resthis, Type[] thrownthis):
                switch (that)
                {
                    case MethodType(Type[] argsthat, Type resthat, Type[] thrownthat):
                        return sameset(thrownthis, thrownthat) &&
                               sametype(resthis, resthat) &&
                               sametypes(argsthis, argsthat);
                }
                break;

            case PackageType():
                switch (that)
                {
                    case PackageType():
                        return true;
                    }
        }
        return false;
    }

    protected boolean subelemtype(Type thi, Type that)
    {
        if (thi == that)
            return true;
        else
        if ((thi == null) || (that == null))
            return false;
        
        switch (that.deref())
        {
            case ErrType:
            case AnyType:
                return true;
        }
        
        switch (thi.deref())
        {
            case ArrayType(_):
            case ClassType(_):
                return subtype(thi, that);
            
            default:
                return sametype(thi, that);
        }
    }
    
/** are the types in 'as' equal to the types in 'bs'
 */
    public boolean sameset(Type[] as, Type[] bs)
    {
        if (as.length != bs.length)
            return false;
        else
        {
            Type[]  cs = new Type[bs.length];
            System.arraycopy(bs, 0, cs, 0, bs.length);
            mainloop: for (int i = 0; i < as.length; i++)
            {
                for (int j = 0; j < cs.length; j++)
                    if (sametype(as[i], cs[j]))
                    {
                        cs[i] = null;
                        continue mainloop;
                    }
                return false;
            }
            return true;
        }
    }

/** is type 't' contained in type array 'ts'?
 */
    public boolean elem(Type t, Type[] ts)
    {
        if (ts == null)
            return false;
        for (int i = 0; i < ts.length; i++)
            if (subtype(t, ts[i]))
                return true;
        return false;
    }

/** intersection of type sets 'xs' and 'ys'
 */
    public Type[] intersect(Type[] xs, Type[] ys)
    {
        Type[]  zs = new Type[xs.length];
        int     j = 0;
        for (int i = 0; i < xs.length; i++)
            if (elem(xs[i], ys))
                zs[j++] = xs[i];
        Type[]  ts = new Type[j];
        System.arraycopy(zs, 0, ts, 0, j);
        return ts;
    }

/** include type in type set
 */
    public Type[] incl(Type[] set, Type type)
    {
        if (elem(type, set))
            return set;
        else
        {
            Type[]  newSet = new Type[set.length + 1];
            System.arraycopy(set, 0, newSet, 0, set.length);
            newSet[set.length] = type;
            return newSet;
        }
    }
    
/** is it legal to cast from 'thi' type to 'that' type?
 */
    public boolean castable(Type thi, Type that)
    {
        switch (thi)
        {
            case ErrType:
            case AnyType:
                return true;
        }
        switch (that)
        {
            case NullType:
                return (thi == that);
                
            case NumType(int thattag):
                switch (thi)
                {
                    case NumType(int thistag):
                        if ((thistag == thattag) ||
                            (thistag <= DOUBLE) && (thattag <= DOUBLE))
                                return true;
                        break;
                }
                break;
            
            case ArrayType(Type elemthat):
                switch (thi)
                {
                    case NullType:
                        return true;
                        
                    case ArrayType(Type elemthis):
                        if (elemthis.isBasic() || elemthat.isBasic())
                            return sametype(elemthis, elemthat);
                        else
                            return castable(elemthis, elemthat);
                    
                    case ClassType(_):
                        if ((thi.tdef() == objectType.tdef()) ||
                            (thi.tdef() == cloneableType.tdef()) ||
                            (thi.tdef() == serializableType.tdef()))
                            return true;
                        break;
                }
                break;
                
            case ClassType(_):
                switch (thi)
                {
                    case NullType:
                        return true;
                    
                    case ClassType(_):
                        if (subtype(thi, that) ||
                            subtype(that, thi) ||
                            ((thi.tdef().modifiers & INTERFACE) != 0 &&
                                    (that.tdef().modifiers & FINAL) == 0) ||
                                ((that.tdef().modifiers & INTERFACE) != 0 &&
                                    (thi.tdef().modifiers & FINAL) == 0))
                            return true;
                        break;
                    
                    case ArrayType(_):
                        if ((that.tdef() == objectType.tdef()) ||
                            (that.tdef() == cloneableType.tdef()) ||
                            (that.tdef() == serializableType.tdef()))
                            return true;
                        break;
                        
                    case MethodType(_, _, _):
                        if (that.tdef() == objectType.tdef())
                            return true;
                        break;
                }
                break;

            case ErrType:
                return true;
        }
        return false;
    }

/** is this type 'thi' assignable to type 'that'?
 */
    public boolean assignable(Type thi, Type that)
    {
        if (thi.isTypeOfConstant())
            return constants.assignable(thi.tconst(), that);
        else
            return subtype(thi, that);
    }

/** return most general base type of this type that starts with def
 *  if none exists, return null
 */
    public Type mgb(Type that, Definition def)
    {
        if (subtype(that, def.type))
            return def.type;
        else
            return null;
    }

/** fold binary operation for types
 */
    public Type fold(int pos, Type l, Type r, int opc, Type res)
    {
        if (l.isTypeOfConstant() && r.isTypeOfConstant())
        {
            Constant    c = constants.fold(pos, l.tconst(), r.tconst(),
                                            opc, res.tag());
            if (c == null)
                return res;
            else
                return constants.toType(c);
        }
        else
            return res;
    }

/** fold unary operation for types
 */
    public Type fold(int pos, Type od, int opc, Type res)
    {
        if (od.isTypeOfConstant())
        {
            Constant    c = constants.fold(pos, od.tconst(), opc, res.tag());
            if (c == null)
                return res;
            else
                return constants.toType(c);
        }
        else
            return res;
    }

/** coerce types, specifying constants
 */
    public Type coerce(Type etype, Type ttype)
    {
        if (etype.isTypeOfConstant())
            return constants.toType(constants.coerce(etype.tconst(), ttype));
        else
            return ttype;
    }

/** get type without constant part
 */
    public Type deconst(Type that)
    {
        if (that.isTypeOfConstant())
            return constants.typeOf(that.tconst());
        else
            return that;
    }


/** array operations
 */
    public Type[] prepend(Type t, Type[] ts)
    {
        Type[]  newTypes = new Type[ts.length + 1];
        System.arraycopy(ts, 0, newTypes, 1, ts.length);
        newTypes[0] = t;
        return newTypes;
    }

    public Type[] append(Type[] ts0, Type[] ts1)
    {
        if ((ts0 == null) || (ts0.length == 0))
            return ts1;
        if ((ts1 == null) || (ts1.length == 0))
            return ts0;
        Type[]  newTypes = new Type[ts0.length + ts1.length];
        System.arraycopy(ts0, 0, newTypes, 0, ts0.length);
        System.arraycopy(ts1, 0, newTypes, ts0.length, ts1.length);
        return newTypes;
    }
    
    public Type[] append(Type[] ts, Type t)
    {
        Type[]  newTypes = new Type[ts.length + 1];
        System.arraycopy(ts, 0, newTypes, 0, ts.length);
        newTypes[ts.length] = t;
        return newTypes;
    }
    
    public Type[] extract(Type[] xs, int start, Type[] target)
    {
        System.arraycopy(xs, start, target, 0, target.length);
        return target;
    }
}

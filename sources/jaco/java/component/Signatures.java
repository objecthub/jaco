//      /   _ _      JaCo
//  \  //\ / / \     - support for classfile type signatures
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;


public interface SignatureConst
{
    Name    BYTE_SIG = Name.fromString("B");
    Name    SHORT_SIG = Name.fromString("S");
    Name    CHAR_SIG = Name.fromString("C");
    Name    INT_SIG = Name.fromString("I");
    Name    LONG_SIG = Name.fromString("J");
    Name    FLOAT_SIG = Name.fromString("F");
    Name    DOUBLE_SIG = Name.fromString("D");
    Name    BOOLEAN_SIG = Name.fromString("Z");
    Name    VOID_SIG = Name.fromString("V");
    Name    CLASS_SIG = Name.fromString("L");
    Name    ARRAY_SIG = Name.fromString("[");
    Name    ARGBEGIN_SIG = Name.fromString("(");
    Name    ARGEND_SIG = Name.fromString(")");
}


public class Signatures extends Component
                        implements SignatureConst, TypeConst
{
/** other components
 */
    protected Classfiles    classfiles;
    protected Types         types;
    protected Definitions   definitions;
    
    
/** component name
 */
    public String getName()
    {
        return "JavaSignatures";
    }
    
/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        classfiles = context.Classfiles();
        definitions = context.Definitions();
        types = context.Types();
    }
    
    
/** return signature of type 'type'
 */
    public Name typeToSig(Type type)
    {
        Name sig;
        switch (type.deref())
        {
            case NumType(int tag):
                switch (tag)
                {
                    case BYTE:
                        return BYTE_SIG;
                    
                    case SHORT:
                        return SHORT_SIG;
                    
                    case CHAR:
                        return CHAR_SIG;

                    case INT:
                        return INT_SIG;

                    case LONG:
                        return LONG_SIG;
                    
                    case FLOAT:
                        return FLOAT_SIG;
                    
                    case DOUBLE:
                        return DOUBLE_SIG;
                    
                    case BOOLEAN:
                        return BOOLEAN_SIG;
                    
                    default:
                        throw new InternalError();
                }
            
            case VoidType:
                return VOID_SIG;
            
            case ClassType(_):
                Type    outer = type.outer();
                Name    fullname = type.tdef().fullname;
                byte[]  ascii = new byte[fullname.length() + 2];
                ascii[0] = 'L';
                fullname.copyAscii(ascii, 1);
                ascii[ascii.length - 1] = ';';
                sig = Name.fromAscii(classfiles.externalize(ascii, 0, ascii.length),
                                        0, ascii.length);
                // if (outer instanceof ClassType)
                //  System.out.println("PANIC: outer != null OF " + type.deref() + " (signatures)");//DEBUG
                return sig;
            
            case ArrayType(Type elemtype):
                return ARRAY_SIG.append(typeToSig(elemtype));
            
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                sig = compoundSig(ARGBEGIN_SIG, argtypes, ARGEND_SIG);
                return sig.append(typeToSig(restype));
            
            default:
                throw new InternalError("typeToSig " + type.tag());
        }
    }
    
    public Name compoundSig(Name prefix, Type[] args, Name suffix)
    {
        Name[]  components = new Name[args.length + 2];
        components[0] = prefix;
        for (int i = 0; i < args.length; i++)
            components[i + 1] = typeToSig(args[i]);
        components[args.length + 1] = suffix;
        return Name.concat(components);
    }

/** return type for signature
 */
 
/** the type represented by signature[offset..].
 */
    protected byte[]    signature;
    protected int       sigp;
    protected int       limit;


    public Type sigToType(byte[] sig, int offset, int len)
    {
        signature = sig;
        sigp = offset;
        limit = offset + len;
        //System.out.println("transsig: " + Name.fromAscii(sig, offset, len));
        return sigToType();
    }

    protected Type sigToType()
    {
        switch (signature[sigp])
        {
            case 'B':
                sigp++;
                return types.byteType;
                
            case 'C':
                sigp++;
                return types.charType;
            
            case 'D':
                sigp++;
                return types.doubleType;
        
            case 'F':
                sigp++;
                return types.floatType;
            
            case 'I':
                sigp++;
                return types.intType;
            
            case 'J':
                sigp++;
                return types.longType;
                
            case 'L':
                sigp++;
                int start = sigp;
                while (signature[sigp] != ';')
                    sigp++;
                Type    t = definitions.defineClass(
                                Name.fromAscii(signature, start, sigp - start)).type;
                sigp++;
                return t;
                
            case 'S':
                sigp++;
                return types.shortType;

            case 'V':
                sigp++;
                return types.voidType;
        
            case 'Z':
                sigp++;
                return types.booleanType;
        
            case '[':
                sigp++;
                while (('0' <= signature[sigp]) && (signature[sigp] <= '9'))
                    sigp++;
                return types.make.ArrayType(sigToType());
            
            case '(':
                return types.make.MethodType(sigToTypes(')'), sigToType(), types.noTypes);
            
            default:
                throw new RuntimeException("bad signature: " +
                                    SourceRepr.ascii2string(signature, sigp, 1));
        }
    }

    protected Type[] sigToTypes(char terminator)
    {
        sigp++;
        return sigToTypes(terminator, 0);
    }
    
    protected Type[] sigToTypes(char terminator, int i)
    {
        if (signature[sigp] == terminator)
        {
            sigp++;
            return new Type[i];
        }
        else
        {
            Type    t = sigToType();
            Type[]  vec = sigToTypes(terminator, i+1);
            vec[i] = t;
            return vec;
        }
    }
}

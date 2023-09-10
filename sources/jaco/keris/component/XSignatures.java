//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    classfile signature support
//                           
//  [XSignatures.java (5734) 22-Jun-01 23:23 -> 22-Jun-01 23:24]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.keris.struct.*;
import java.util.*;
import Type.*;


public interface XSignatureConst extends SignatureConst {
    Name MODULE_SIG = Name.fromString("M");
    Name COMPOUND_SIG = Name.fromString("K");
    Name SEPARATOR_SIG = Name.fromString(":");
}


public class XSignatures extends Signatures
                         implements XSignatureConst, DefinitionConst {
    
    boolean extended = false;
    
/** component name
 */
    public String getName() {
        return "XSignatures";
    }
    
/** component initialization
 */
    public void init(MainContext context) {
        super.init(context);
    }
    
/** return signature of type 'type'
 */
    public Name typeToSig(Type type) {
        Name sig;
        switch ((XType)type.deref()) {
            case ModuleType(Type outer):
                if (outer != null)
                    switch ((XType)outer) {
                        case ModuleType(_):
                            sig = SEPARATOR_SIG.append(typeToSig(outer));
                            break;
                        default:
                            sig = Name.fromString("");
                    }
                else
                    sig = Name.fromString("");
                Name fullname = type.tdef().fullname;
                byte[]  ascii = new byte[fullname.length() + 2];
                ascii[0] = 'M';
                fullname.copyAscii(ascii, 1);
                ascii[ascii.length - 1] = ';';
                sig = sig.append(Name.fromAscii(
                    classfiles.externalize(ascii, 0, ascii.length),
                    0, ascii.length));
                return sig;
            case ClassType(Type outer):
                if (extended && (outer != null))
                    switch ((XType)outer) {
                        case ModuleType(_):
                        case ClassType(_):
                            sig = SEPARATOR_SIG.append(typeToSig(outer));
                            break;
                        default:
                            sig = Name.fromString("");
                    }
                else
                    sig = Name.fromString("");
                Name fullname = type.tdef().fullname;
                byte[]  ascii = new byte[fullname.length() + 2];
                ascii[0] = 'L';
                fullname.copyAscii(ascii, 1);
                ascii[ascii.length - 1] = ';';
                sig = sig.append(Name.fromAscii(
                    classfiles.externalize(ascii, 0, ascii.length),
                    0, ascii.length));
                return sig;
            case CompoundType(Type[] components):
                Name sigs = COMPOUND_SIG;
                for (int i = 0; i < components.length; i++)
                    sigs = sigs.append(typeToSig(components[i]));
                return sigs.append(Name.fromString(";"));
            default:
                return super.typeToSig(type);
        }
    }

    protected Type sigToType() {
        switch (signature[sigp]) {
            case ':':
                sigp++;
                Type outer = sigToType();
                Type inner = sigToType();
                switch ((XType)inner) {
                    case ClassType(_):
                        Type t = types.make.ClassType(outer);
                        t.setDef(inner.tdef());
                        return t;
                    case ModuleType(_):
                        ((XType.ModuleType)inner).outer = outer;
                        return inner;
                    default:
                        return inner;
                }
            case 'M':
                sigp++;
                int start = sigp;
                while (signature[sigp] != ';')
                    sigp++;
                Type t = ((XTypeFactory)types.make).ModuleType(null);
                t.setDef(definitions.defineClass(
                    Name.fromAscii(signature, start, sigp - start)));
                sigp++;
                return t;
            case 'K':
                sigp++;
                Type[] components = new Type[0];
                while (signature[sigp] != ';') {
                    Type[] newcomps = new Type[components.length + 1];
                    System.arraycopy(components, 0, newcomps, 0, components.length);
                    newcomps[components.length] = sigToType();
                    components = newcomps;
                }
                sigp++;
                return ((XTypeFactory)types.make).CompoundType(components);
            default:
                return super.sigToType();
        }
    }
    
    public Types getTypesModule() {
        return types;
    }
}

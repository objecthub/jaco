package jaco.join.struct;

import jaco.framework.*;
import jaco.join.component.*;


public class JType {
    public final static int ERRTYPE = 0;
    public final static int BOTTOM = 1;
    public final static int BOOL = 2;
    public final static int INT = 3;
    
    public case PrimitiveType(int tag);
    public case FunctionType(JType argtype, JType restype);
    public case TupelType(JType[] comptypes);
    public case RecordType(JDefinitionSet members);
    public case TypeVar(int id, JConstraintSet cs);
    public case ForAll(TypeVar[] vars, JType type);
    public case Mu(TypeVar var, JType type);
    
    public static JType errorType = PrimitiveType(ERRTYPE);
    public static JType bottomType = PrimitiveType(BOTTOM);
    public static JType boolType = PrimitiveType(BOOL);
    public static JType intType = PrimitiveType(INT);
    
    public static JTypes jtypes = new JTypes();
    
    public String toString() {
        switch (this) {
            case PrimitiveType(int tag):
                switch (tag) {
                    case ERRTYPE:
                        return "<errtype>";
                    case BOTTOM:
                        return "nil";
                    case BOOL:
                        return "bool";
                    case INT:
                        return "int";
                    default:
                        return "<" + tag + ">";
                }
            case FunctionType(JType argtype, JType restype):
                return "(" + argtype + " -> " + restype + ")";
            case TupelType(JType[] comptypes):
                if (comptypes.length == 0)
                    return "()";
                String res = "(";
                for (int i = 0; i < comptypes.length - 1; i++)
                    res += comptypes[i] + ", ";
                return res + comptypes[comptypes.length - 1] + ")";
            case RecordType(JDefinitionSet members):
                return members.toString();
            case TypeVar(int id, JConstraintSet cs):
                if ((cs.size == 0) || (id > 32000))
                    return toName();
                else {
                    ((JType.TypeVar)this).id = id * 32000;
                    String res = toName() + "[" + cs + "]";
                    ((JType.TypeVar)this).id = id;
                    return res;
                }
            case ForAll(TypeVar[] vars, JType type):
                if (vars.length == 0)
                    return type.toString();
                String res = "V" + vars[0];
                for (int i = 1; i < vars.length; i++)
                    res += "," + vars[i];
                return res + "." + type;
            case Mu(TypeVar var, JType type):
                return "(mu " + var + "." + type + ")";
            default:
                return "<?>";
        }
    }
    
    public String toName() {
        switch (this) {
            case TypeVar(int id, _):
                if (id < 0)
                    id = -id;
                if (id > 32000)
                    id /= 32000;
                String res = new String(new char[]{(char)((id % 26) + 'a' - 1)});
                id /= 26;
                while (id > 0) {
                    res = ((char)((id % 26) + 'a' - 1)) + res;
                    id /= 26;
                }
                return res;
            default:
                return toString();
        }
    }
    
    public boolean isBottom() {
        switch (this) {
            case PrimitiveType(BOTTOM):
                return true;
            default:
                return false;
        }
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - java operator support
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import Definition.*;
import Type.*;


public interface OperatorConst
{
/** ast tags
 */
    /** assignment */
    int ASSIGN = 1;

    /** unary + */
    int POS = 2;
    /** unary - */
    int NEG = 3;
    /** unary ! */
    int NOT = 4;
        /** unary ~ */
    int COMPL = 5;
    /** unary ++ prefix */
    int PREINC = 6;
    /** unary -- prefix */
    int PREDEC = 7;
    /** unary ++ postfix*/
    int POSTINC = 8;
    /** unary -- postfix */
    int POSTDEC = 9;

    /** binary or assignment || */
    int OR = 10;
    /** binary or assignment && */
    int AND = 11;
    /** binary == */
    int EQ = 12;
    /** binary != */
    int NE = 13;
    /** binary < */
    int LT = 14;
    /** binary > */
    int GT = 15;
    /** binary <= */
    int LE = 16;
    /** binary >= */
    int GE = 17;
    /** binary or assignment | */
    int BITOR = 18;
    /** binary or assignment ^ */
    int BITXOR = 19;
    /** binary or assignment & */
    int BITAND = 20;
    /** binary or assignment << */
    int SL = 21;
    /** binary or assignment >> */
    int SR = 22;
    /** binary or assignment >>> */
    int USR = 23;
    /** binary or assignment + */
    int PLUS = 24;
    /** binary or assignment - */
    int MINUS = 25;
    /** binary or assignment * */
    int TIMES = 26;
    /** binary or assignment / */
    int DIV = 27;
    /** binary or assignment % */
    int MOD = 28;

    /** instanceof operation */
    int TYPETEST = 30;
    /** type cast operation */
    int TYPECAST = 31;
    /** save type cast operation */
    int SAVECAST = 32;
}


public class Operators  extends Component
                        implements  OperatorConst, BytecodeTags,
                                    ItemConst, ModifierConst
{
/** other components
 */
    Definitions         definitions;
    Types               types;

/** operator precedences
 */
    public int postfixPrec = 11;
    public int prefixPrec = 10;
    public int mulPrec = 9;
    public int addPrec = 8;
    public int shiftPrec = 7;
    public int ordPrec = 6;
    public int eqPrec = 5;
    public int bitandPrec = 4;
    public int bitxorPrec = 3;
    public int bitorPrec = 2;
    public int andPrec = 1;
    public int orPrec = 0;
    public int condPrec = -1;
    public int assignPrec = -2;
    public int noPrec = -3;
    
/** number of operators
 */
    int opcount = 33;
    
/** precedence table
 */
    public int[]    opprec;

/** operator names
 */
    public Name[]   opname;

/** some predefined operators
 */
    public Definition negOperator;
    

/** component name
 */
    public String getName()
    {
        return "JavaOperators";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        definitions = context.Definitions();
        types = context.Types();
        
        // set operator precedences
        opprec = new int[opcount];
        opprec[ASSIGN] = assignPrec;
        opprec[POS] = prefixPrec;
        opprec[NEG] = prefixPrec;
        opprec[NOT] = prefixPrec;
        opprec[COMPL] = prefixPrec;
        opprec[PREINC] = prefixPrec;
        opprec[PREDEC] = prefixPrec;
        opprec[POSTINC] = postfixPrec;
        opprec[POSTDEC] = postfixPrec;
        opprec[OR] = orPrec;
        opprec[AND] = andPrec;
        opprec[EQ] = eqPrec;
        opprec[NE] = eqPrec;
        opprec[LT] = ordPrec;
        opprec[GT] = ordPrec;
        opprec[LE] = ordPrec;
        opprec[GE] = ordPrec;
        opprec[BITOR] = bitorPrec;
        opprec[BITXOR] = bitxorPrec;
        opprec[BITAND] = bitandPrec;
        opprec[SL] = shiftPrec;
        opprec[SR] = shiftPrec;
        opprec[USR] = shiftPrec;
        opprec[PLUS] = addPrec;
        opprec[MINUS] = addPrec;
        opprec[TIMES] = mulPrec;
        opprec[DIV] = mulPrec;
        opprec[MOD] = mulPrec;
        opprec[TYPETEST] = prefixPrec;
        opprec[TYPECAST] = prefixPrec;
        opprec[SAVECAST] = prefixPrec;
        
        // initialize operator names
        opname = new Name[opcount];
        opname[ASSIGN] = Name.fromString("=");
        opname[POS] = Name.fromString("+");
        opname[NEG] = Name.fromString("-");
        opname[NOT] = Name.fromString("!");
        opname[COMPL] = Name.fromString("~");
        opname[PREINC] = Name.fromString("++");
        opname[PREDEC] = Name.fromString("--");
        opname[POSTINC] = Name.fromString("++");
        opname[POSTDEC] = Name.fromString("--");
        opname[OR] = Name.fromString("||");
        opname[AND] = Name.fromString("&&");
        opname[EQ] = Name.fromString("==");
        opname[NE] = Name.fromString("!=");
        opname[LT] = Name.fromString("<");
        opname[GT] = Name.fromString(">");
        opname[LE] = Name.fromString("<=");
        opname[GE] = Name.fromString(">=");
        opname[BITOR] = Name.fromString("|");
        opname[BITXOR] = Name.fromString("^");
        opname[BITAND] = Name.fromString("&");
        opname[SL] = Name.fromString("<<");
        opname[SR] = Name.fromString(">>");
        opname[USR] = Name.fromString(">>>");
        opname[PLUS] = Name.fromString("+");
        opname[MINUS] = Name.fromString("-");
        opname[TIMES] = Name.fromString("*");
        opname[DIV] = Name.fromString("/");
        opname[MOD] = Name.fromString("%");
        
        // enter operators in definition table
        enterOperators();
    }
    
/** initialize all predefined operators
 */
    protected void enterOperators()
    {
        enterUnop("+", types.intType, types.intType, nop);
        enterUnop("+", types.longType, types.longType, nop);
        enterUnop("+", types.floatType, types.floatType, nop);
        enterUnop("+", types.doubleType, types.doubleType, nop);
        enterUnop("-", types.intType, types.intType, ineg);
        enterUnop("-", types.longType, types.longType, lneg);
        enterUnop("-", types.floatType, types.floatType, fneg);
        enterUnop("-", types.doubleType, types.doubleType, dneg);
        enterUnop("~", types.intType, types.intType, ixor);
        enterUnop("~", types.longType, types.longType, lxor);
        enterUnop("++", types.byteType, types.byteType, iadd);
        enterUnop("++", types.shortType, types.shortType, iadd);
        enterUnop("++", types.charType, types.charType, iadd);
        enterUnop("++", types.intType, types.intType, iadd);
        enterUnop("++", types.longType, types.longType, ladd);
        enterUnop("++", types.floatType, types.floatType, fadd);
        enterUnop("++", types.doubleType, types.doubleType, dadd);
        enterUnop("--", types.byteType, types.byteType, isub);
        enterUnop("--", types.shortType, types.shortType, isub);
        enterUnop("--", types.charType, types.charType, isub);
        enterUnop("--", types.intType, types.intType, isub);
        enterUnop("--", types.longType, types.longType, lsub);
        enterUnop("--", types.floatType, types.floatType, fsub);
        enterUnop("--", types.doubleType, types.doubleType, dsub);
        negOperator = enterUnop("!", types.booleanType, types.booleanType, bool_not);
        
        enterBinop("+", types.intType, types.intType, types.intType, iadd);
        enterBinop("+", types.longType, types.longType, types.longType, ladd);
        enterBinop("+", types.floatType, types.floatType, types.floatType, fadd);
        enterBinop("+", types.doubleType, types.doubleType, types.doubleType, dadd);
        enterBinop("+", types.stringType, types.stringType, types.stringType, string_add);
        enterBinop("+", types.stringType, Type.AnyType, types.stringType, string_add);
        enterBinop("+", Type.AnyType, types.stringType, types.stringType, string_add);
        
        enterBinop("-", types.intType, types.intType, types.intType, isub);
        enterBinop("-", types.longType, types.longType, types.longType, lsub);
        enterBinop("-", types.floatType, types.floatType, types.floatType, fsub);
        enterBinop("-", types.doubleType, types.doubleType, types.doubleType, dsub);
        enterBinop("*", types.intType, types.intType, types.intType, imul);
        enterBinop("*", types.longType, types.longType, types.longType, lmul);
        enterBinop("*", types.floatType, types.floatType, types.floatType, fmul);
        enterBinop("*", types.doubleType, types.doubleType, types.doubleType, dmul);
        enterBinop("/", types.intType, types.intType, types.intType, idiv);
        enterBinop("/", types.longType, types.longType, types.longType, ldiv);
        enterBinop("/", types.floatType, types.floatType, types.floatType, fdiv);
        enterBinop("/", types.doubleType, types.doubleType, types.doubleType, ddiv);
        enterBinop("%", types.intType, types.intType, types.intType, imod);
        enterBinop("%", types.longType, types.longType, types.longType, lmod);
        enterBinop("%", types.floatType, types.floatType, types.floatType, fmod);
        enterBinop("%", types.doubleType, types.doubleType, types.doubleType, dmod);
        enterBinop("&", types.intType, types.intType, types.intType, iand);
        enterBinop("&", types.longType, types.longType, types.longType, land);
        enterBinop("|", types.intType, types.intType, types.intType, ior);
        enterBinop("|", types.longType, types.longType, types.longType, lor);
        enterBinop("^", types.intType, types.intType, types.intType, ixor);
        enterBinop("^", types.longType, types.longType, types.longType, lxor);
        enterBinop("<<", types.intType, types.intType, types.intType, ishl);
        enterBinop("<<", types.longType, types.intType, types.longType, lshl);
        enterBinop(">>", types.intType, types.intType, types.intType, ishr);
        enterBinop(">>", types.longType, types.intType, types.longType, lshr);
        enterBinop(">>>", types.intType, types.intType, types.intType, iushr);
        enterBinop(">>>", types.longType, types.intType, types.longType, lushr);
        enterBinop("<<", types.intType, types.longType, types.intType, ishll);
        enterBinop("<<", types.longType, types.longType, types.longType, lshll);
        enterBinop(">>", types.intType, types.longType, types.intType, ishrl);
        enterBinop(">>", types.longType, types.longType, types.longType, lshrl);
        enterBinop(">>>", types.intType, types.longType, types.intType, iushrl);
        enterBinop(">>>", types.longType, types.longType, types.longType, lushrl);

        enterBinop("<", types.intType, types.intType, types.booleanType, if_icmplt);
        enterBinop("<", types.longType, types.longType, types.booleanType, lcmp, iflt);
        enterBinop("<", types.floatType, types.floatType, types.booleanType, fcmpg, iflt);
        enterBinop("<", types.doubleType, types.doubleType, types.booleanType, dcmpg, iflt);
        
        enterBinop(">", types.intType, types.intType, types.booleanType, if_icmpgt);
        enterBinop(">", types.longType, types.longType, types.booleanType, lcmp, ifgt);
        enterBinop(">", types.floatType, types.floatType, types.booleanType, fcmpl, ifgt);
        enterBinop(">", types.doubleType, types.doubleType, types.booleanType, dcmpl, ifgt);

        enterBinop("<=", types.intType, types.intType, types.booleanType, if_icmple);
        enterBinop("<=", types.longType, types.longType, types.booleanType, lcmp, ifle);
        enterBinop("<=", types.floatType, types.floatType, types.booleanType, fcmpg, ifle);
        enterBinop("<=", types.doubleType, types.doubleType, types.booleanType, dcmpg, ifle);

        enterBinop(">=", types.intType, types.intType, types.booleanType, if_icmpge);
        enterBinop(">=", types.longType, types.longType, types.booleanType, lcmp, ifge);
        enterBinop(">=", types.floatType, types.floatType, types.booleanType, fcmpl, ifge);
        enterBinop(">=", types.doubleType, types.doubleType, types.booleanType, dcmpl, ifge);

        enterBinop("==", types.intType, types.intType, types.booleanType, if_icmpeq);
        enterBinop("==", types.longType, types.longType, types.booleanType, lcmp, ifeq);
        enterBinop("==", types.floatType, types.floatType, types.booleanType, fcmpl, ifeq);
        enterBinop("==", types.doubleType, types.doubleType, types.booleanType, dcmpl, ifeq);
        enterBinop("==", types.booleanType, types.booleanType, types.booleanType, if_icmpeq);
        enterBinop("==", types.objectType, types.objectType, types.booleanType, if_acmpeq);
        
        enterBinop("!=", types.intType, types.intType, types.booleanType, if_icmpne);
        enterBinop("!=", types.longType, types.longType, types.booleanType, lcmp, ifne);
        enterBinop("!=", types.floatType, types.floatType, types.booleanType, fcmpl, ifne);
        enterBinop("!=", types.doubleType, types.doubleType, types.booleanType, dcmpl, ifne);
        enterBinop("!=", types.booleanType, types.booleanType, types.booleanType, if_icmpne);
        enterBinop("!=", types.objectType, types.objectType, types.booleanType, if_acmpne);

        enterBinop("&", types.booleanType, types.booleanType, types.booleanType, iand);
        enterBinop("|", types.booleanType, types.booleanType, types.booleanType, ior);
        enterBinop("^", types.booleanType, types.booleanType, types.booleanType, ixor);
        enterBinop("&&", types.booleanType, types.booleanType, types.booleanType, bool_and);
        enterBinop("||", types.booleanType, types.booleanType, types.booleanType, bool_or);
    }


/** enter a binary operation; opcode = bytecode of the operation
 */
    public void enterBinop(String name, Type left, Type right, Type res, int opcode)
    {
        Type [] argtypes = {left, right};
        definitions.predefClass.locals().enter(
                            definitions.make.OperatorDef(
                                Name.fromString(name),
                                types.make.MethodType(argtypes, res, types.noTypes),
                                opcode));
    }

/** enter a binary operation; two opcodes needed to implement the operation
 *  opcode encoding: (opc1 << preShift) + opc2
 */
    public void enterBinop(String name, Type left, Type right, Type res, int opc1, int opc2)
    {
        Type[]  argtypes = {left, right};
        definitions.predefClass.locals().enter(
                            definitions.make.OperatorDef(
                                Name.fromString(name),
                                types.make.MethodType(argtypes, res, types.noTypes),
                                (opc1 << preShift) + opc2));
    }
    
/** enter a unary operation
 */
    public Definition enterUnop(String name, Type arg, Type res, int opcode)
    {
        Type[]  argtypes = {arg};
        Definition def;
        definitions.predefClass.locals().enter(def =
                            definitions.make.OperatorDef(
                                Name.fromString(name),
                                types.make.MethodType(argtypes, res, types.noTypes),
                                opcode));
        return def;
    }


    public int prec(int tag)
    {
        return opprec[tag];
    }
    
    public String toString(int tag)
    {
        return opname[tag].toString();
    }
}

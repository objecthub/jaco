//      /   _ _      JaCo
//  \  //\ / / \     - a bytecode disassembler
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;
import Type.*;
import Definition.*;
import java.io.*;
import java.util.Stack;


public class Disassembler extends Component
                          implements ModifierConst, BytecodeTags, TreeProcessor
{
/** other components
 */
    protected ErrorHandler  report;
    protected Modifiers     modifiers;
    
/** the print writer
 */
    protected PrintWriter   out;

/** suffix of output file (= null iff out-output)
 */
    protected String        suffix;

/** worklist for nested classes
 */
    protected Stack         worklist;
    
/** bytecode disassembler
 */
    protected int           pc;
    protected byte[]        bytecode;
    protected int           codelen;
    protected Object[]      globalpool;
    
    
    public Disassembler()
    {
        out = new PrintWriter(System.out, true);
    }
    
    public Disassembler(PrintWriter out)
    {
        this.out = out;
    }
    
    public Disassembler(String suffix)
    {
        this.suffix = suffix;
    }
    
    
/** component name
 */
    public String getName()
    {
        return "JavaDisassembler";
    }
    
/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        report = context.ErrorHandler();
        modifiers = context.Modifiers();
    }
    
    protected final int getNext1()
    {
        if (pc >= codelen)
            return 0;
        else
            return bytecode[pc++] & 0xff;
    }
    
    protected int getNext2()
    {
        return (getNext1() << 8) | getNext1();
    }
    
    protected int getNext4()
    {
        return  (getNext1() << 24) |
                (getNext1() << 16) |
                (getNext1() << 8) |
                (getNext1());
    }
    
    public String getPoolEntry(int index)
    {
        if ((index < 0) || (index >= globalpool.length))
            return " #" + index + "<index out of bounds>";
        else
        if (globalpool[index] == null)
            return " #" + index + "<null>";
        else
            return " #" + index + "<" + globalpool[index] + ">";
    }
    
    protected int printAddressAndOp(int indent)
    {
        int     opcode;
        String  s = pc + " ";
        for (int i = indent - s.length() + 1; i > 0; i--)
            s = " " + s;
        if ((opcode = getNext1()) >= ByteCodeCount)
            out.print(s + "<invalid_opcode #" + opcode + ">");
        else
            out.print(s + mnem[opcode]);
        return opcode;
    }
    
    public synchronized void disassemble(int indent)
    {
        int     opcode = 0;
        String  indentStr = "";
        pc = 0;
        for (int i = 0; i < indent; i++)
            indentStr += " ";
        while (pc < codelen)
        {
            if (opcode < 0)
                opcode = printAddressAndOp(indent);
            if (opcode < ByteCodeCount)
            {
                switch (opcode)
                {
                    case aload:
                    case astore:
                    case dload:
                    case dstore:
                    case fload:
                    case fstore:
                    case iload:
                    case istore:
                    case lload:
                    case lstore:
                        out.println(" " + getNext1());
                        break;
                        
                    case sipush:
                        out.println(" " + (short)getNext2());
                        break;
                        
                    case anewarray:
                    case checkcast:
                    case getfield:
                    case getstatic:
                    case instanceof_:
                    case invokespecial:
                    case invokestatic:
                    case invokevirtual:
                    case ldc_w:
                    case ldc2_w:
                    case new_:
                    case putfield:
                    case putstatic:
                        out.println(getPoolEntry(getNext2()));
                        break;
                    
                    case ldc:
                    case newarray:
                    case ret:
                        out.println(getPoolEntry(getNext1()));
                        break;
                        
                    case bipush:
                        out.println(" " + (int)bytecode[pc++]);
                        break;
                    
                    case goto_:
                    case if_acmpeq:
                    case if_acmpne:
                    case if_icmpeq:
                    case if_icmpne:
                    case if_icmplt:
                    case if_icmpge:
                    case if_icmpgt:
                    case if_icmple:
                    case ifeq:
                    case ifne:
                    case iflt:
                    case ifge:
                    case ifgt:
                    case ifle:
                    case ifnonnull:
                    case ifnull:
                    case jsr:
                        out.println(" " + (pc - 1 + (short)getNext2()));
                        break;
                    
                    case goto_w:
                    case jsr_w:
                        out.println(" " + (pc - 1 + getNext4()));
                        break;
                    
                    case iinc:
                        out.println(" " + getNext1() + " " + (int)bytecode[pc++]);
                        break;
                    
                    case invokeinterface:
                        out.println(" " + getPoolEntry(getNext2()));
                        getNext2();
                        break;
                    
                    case lookupswitch:
                        int     opadr = pc - 1;
                        String  localIndent = indentStr + "    ";
                        pc += (4 - (pc % 4)) % 4;
                        int def = getNext4();
                        int npairs = getNext4();
                        out.println(" " + npairs);
                        out.println(localIndent + "default: " + (def - opadr));
                        for (int i = 0; i < npairs; i++)
                            out.println(localIndent + getNext4() + ": " +
                                        (getNext4() - opadr));
                        break;
                    
                    case tableswitch:
                        int     opadr = pc - 1;
                        String  localIndent = indentStr + "    ";
                        pc += (4 - (pc % 4)) % 4;
                        int def = getNext4();
                        int lo, hi;
                        out.println(" " + (lo = getNext4()) + " to " +
                                    (hi = getNext4()));
                        out.println(localIndent + "default: " + (def - opadr));
                        for (int i = lo; i <= hi; i++)
                            out.println(localIndent + i + ": " +
                                        (getNext4() - opadr));
                        break;
                    
                    case wide:
                        out.println();
                        if ((pc < codelen) &&
                            ((opcode = printAddressAndOp(indent)) == iinc))
                            out.println(" " + getNext2() + " " + (short)getNext2());
                        else
                            continue;
                        break;
                    
                    case multianewarray:
                        out.println(getPoolEntry(getNext2()) + " " + getNext1());
                        break;
                        
                    default:
                        out.println();
                        break;
                }
                opcode = -1;
            }
        }
    }

/** print methods
 */
    void disassembleTree(Tree tree)
    {
        switch (tree)
        {
            case Bad():
                out.println("<bad tree>");
                break;
                
            case CompilationUnit(Tree[] decls, _, _, _):
                worklist = new Stack();
                disassembleTrees(decls);
                disassembleWorklist();
                break;
            
            default:
                throw new InternalError();
        }
    }
    
    void outputTree(Tree tree, String suffix)
    {
        switch (tree)
        {
            case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                String  dest = exchangeSuffix(info.source.toString(), suffix);
                try
                {
                    File        outfile = new File(dest);
                    PrintWriter oldOut = out;
                    out = new PrintWriter(new BufferedOutputStream(
                                                new FileOutputStream(outfile)));
                    out.println("/* generated by jaco");
                    out.println(" */");
                    out.println();
                    worklist = new Stack();
                    disassembleTrees(decls);
                    disassembleWorklist();
                    out.close();
                    out = oldOut;
                    report.note("[wrote " + outfile.getPath() + "]");
                }
                catch (IOException e)
                {
                    report.error(Position.NOPOS, "cannot.write.class", dest, e);
                }
                break;
                
            case Bad():
                break;
                
            default:
                throw new InternalError();
        }
    }
    
    void disassembleTrees(Tree[] trees)
    {
        for (int i = 0; i < trees.length; i++)
            switch (trees[i])
            {
                case ClassDecl(_, _, _, _, _, ClassDef def):
                    disassembleDef(def);
                    break;
            }
    }
    
    private String exchangeSuffix(String sourcefile, String suffix)
    {
        return sourcefile.substring(0, sourcefile.lastIndexOf('.')) +
                "." + suffix;
    }

    void disassembleDef(Definition def)
    {
        switch (def)
        {
            case ClassDef(Type supertype, Type[] interfaces, Scope locals,
                          Pool pool, Name sourcefile):
                out.println();
                out.print(modifiers.toString(def.modifiers & ~INTERFACE));
                if ((def.modifiers & INTERFACE) != 0)
                {
                    out.print("interface " + def.name);
                    if (interfaces.length > 0)
                        out.print(" extends ");
                        printTypes(interfaces);
                }
                else
                {
                    out.print("class " + def.name);
                    out.print(" extends " + supertype);
                    if (interfaces.length > 0)
                    {
                        out.print(" implements ");
                        printTypes(interfaces);
                    }
                }
                out.println(" {");
                disassembleScope(locals);
                out.println("}");
                break;
                
            case MethodDef(Code code):
                out.println();
                out.print("    " + modifiers.toString(def.modifiers));
                switch (def.type)
                {
                    case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                        out.print(restype + " " + def.name + "(");
                        printTypes(argtypes);
                        if (thrown.length > 0)
                        {
                            out.print(") throws ");
                            printTypes(thrown);
                        }
                        else
                            out.print(")");
                        break;
                    
                    default:
                        throw new InternalError(def.name.toString());
                }
                if (code == null) // abstract method
                    out.println(";");
                else
                {
                    bytecode = code.code;
                    codelen = code.cp;
                    globalpool = ((ClassDef)def.owner).pool.pool;
                    disassemble(8);
                    out.println("      // stacksize = " + code.max_stack +
                                ", locals = " + code.max_locals);
                    if (code.ncatches > 0)
                    {
                        out.println("    exception table (from, to, target, type):");
                        for (int i = 0; i < code.ncatches; i++)
                            out.println("        (" + (short)code.exc_start_pc[i] + ", " +
                                        (short)code.exc_end_pc[i] + ", " +
                                        (short)code.exc_handler_pc[i] + ", " +
                                        (short)code.exc_catch_type[i] + ")");
                    }
                }
                break;
            
            case VarDef(_, _, _):
                out.println();
                out.println("    " + modifiers.toString(def.modifiers) + def.type +
                            " " + def.name + ";");
                break;
            
            default:
                throw new InternalError();
        }
    }
    
    void disassembleScope(Scope scope)
    {
        Definition  def = scope.elems;
        while (def != null)
        {
            switch (def.def)
            {
                case ClassDef(_, _, _, _, _):
                    worklist.push(def.def);
                    break;
                
                default:
                    disassembleDef(def.def);
                    break;
            }
            def = def.sibling;
        }
    }
    
    void disassembleWorklist()
    {
        while (!worklist.empty())
            disassembleDef((Definition)worklist.pop());
    }
    
    void printTypes(Type[] types)
    {
        if ((types == null) || (types.length == 0))
            return;
        else
        {
            out.print(types[0]);
            for (int i = 1; i < types.length; i++)
                out.print(", " + types[i]);
        }
    }
    
    
    protected static String[]   mnem;
    
    static
    {
        mnem = new String[ByteCodeCount];
        mnem[nop]           = "nop";
        mnem[aconst_null]   = "aconst_null";
        mnem[iconst_m1]     = "iconst_m1";
        mnem[iconst_0]      = "iconst_0";
        mnem[iconst_1]      = "iconst_1";
        mnem[iconst_2]      = "iconst_2";
        mnem[iconst_3]      = "iconst_3";
        mnem[iconst_4]      = "iconst_4";
        mnem[iconst_5]      = "iconst_5";
        mnem[lconst_0]      = "lconst_0";
        mnem[lconst_1]      = "lconst_1";
        mnem[fconst_0]      = "fconst_0";
        mnem[fconst_1]      = "fconst_1";
        mnem[fconst_2]      = "fconst_2";
        mnem[dconst_0]      = "dconst_0";
        mnem[dconst_1]      = "dconst_1";
        mnem[bipush]        = "bipush";
        mnem[sipush]        = "sipush";
        mnem[ldc]           = "ldc";
        mnem[ldc_w]         = "ldc_w";
        mnem[ldc2_w]        = "ldc2_w";
        mnem[iload]         = "iload";
        mnem[lload]         = "lload";
        mnem[fload]         = "fload";
        mnem[dload]         = "dload";
        mnem[aload]         = "aload";
        mnem[iload_0]       = "iload_0";
        mnem[lload_0]       = "lload_0";
        mnem[fload_0]       = "fload_0";
        mnem[dload_0]       = "dload_0";
        mnem[aload_0]       = "aload_0";
        mnem[iload_1]       = "iload_1";
        mnem[lload_1]       = "lload_1";
        mnem[fload_1]       = "fload_1";
        mnem[dload_1]       = "dload_1";
        mnem[aload_1]       = "aload_1";
        mnem[iload_2]       = "iload_2";
        mnem[lload_2]       = "lload_2";
        mnem[fload_2]       = "fload_2";
        mnem[dload_2]       = "dload_2";
        mnem[aload_2]       = "aload_2";
        mnem[iload_3]       = "iload_3";
        mnem[lload_3]       = "lload_3";
        mnem[fload_3]       = "fload_3";
        mnem[dload_3]       = "dload_3";
        mnem[aload_3]       = "aload_3";
        mnem[iaload]        = "iaload";
        mnem[laload]        = "laload";
        mnem[faload]        = "faload";
        mnem[daload]        = "daload";
        mnem[aaload]        = "aaload";
        mnem[baload]        = "baload";
        mnem[caload]        = "caload";
        mnem[saload]        = "saload";
        mnem[istore]        = "istore";
        mnem[lstore]        = "lstore";
        mnem[fstore]        = "fstore";
        mnem[dstore]        = "dstore";
        mnem[astore]        = "astore";
        mnem[istore_0]      = "istore_0";
        mnem[lstore_0]      = "lstore_0";
        mnem[fstore_0]      = "fstore_0";
        mnem[dstore_0]      = "dstore_0";
        mnem[astore_0]      = "astore_0";
        mnem[istore_1]      = "istore_1";
        mnem[lstore_1]      = "lstore_1";
        mnem[fstore_1]      = "fstore_1";
        mnem[dstore_1]      = "dstore_1";
        mnem[astore_1]      = "astore_1";
        mnem[istore_2]      = "istore_2";
        mnem[lstore_2]      = "lstore_2";
        mnem[fstore_2]      = "fstore_2";
        mnem[dstore_2]      = "dstore_2";
        mnem[astore_2]      = "astore_2";
        mnem[istore_3]      = "istore_3";
        mnem[lstore_3]      = "lstore_3";
        mnem[fstore_3]      = "fstore_3";
        mnem[dstore_3]      = "dstore_3";
        mnem[astore_3]      = "astore_3";
        mnem[iastore]       = "iastore";
        mnem[lastore]       = "lastore";
        mnem[fastore]       = "fastore";
        mnem[dastore]       = "dastore";
        mnem[aastore]       = "aastore";
        mnem[bastore]       = "bastore";
        mnem[castore]       = "castore";
        mnem[sastore]       = "sastore";
        mnem[pop]           = "pop";
        mnem[pop2]          = "pop2";
        mnem[dup]           = "dup";
        mnem[dup_x1]        = "dup_x1";
        mnem[dup_x2]        = "dup_x2";
        mnem[dup2]          = "dup2";
        mnem[dup2_x1]       = "dup2_x1";
        mnem[dup2_x2]       = "dup2_x2";
        mnem[swap]          = "swap";
        mnem[iadd]          = "iadd";
        mnem[ladd]          = "ladd";
        mnem[fadd]          = "fadd";
        mnem[dadd]          = "dadd";
        mnem[isub]          = "isub";
        mnem[lsub]          = "lsub";
        mnem[fsub]          = "fsub";
        mnem[dsub]          = "dsub";
        mnem[imul]          = "imul";
        mnem[lmul]          = "lmul";
        mnem[fmul]          = "fmul";
        mnem[dmul]          = "dmul";
        mnem[idiv]          = "idiv";
        mnem[ldiv]          = "ldiv";
        mnem[fdiv]          = "fdiv";
        mnem[ddiv]          = "ddiv";
        mnem[imod]          = "imod";
        mnem[lmod]          = "lmod";
        mnem[fmod]          = "fmod";
        mnem[dmod]          = "dmod";
        mnem[ineg]          = "ineg";
        mnem[lneg]          = "lneg";
        mnem[fneg]          = "fneg";
        mnem[dneg]          = "dneg";
        mnem[ishl]          = "ishl";
        mnem[lshl]          = "lshl";
        mnem[ishr]          = "ishr";
        mnem[lshr]          = "lshr";
        mnem[iushr]         = "iushr";
        mnem[lushr]         = "lushr";
        mnem[iand]          = "iand";
        mnem[land]          = "land";
        mnem[ior]           = "ior";
        mnem[lor]           = "lor";
        mnem[ixor]          = "ixor";
        mnem[lxor]          = "lxor";
        mnem[iinc]          = "iinc";
        mnem[i2l]           = "i2l";
        mnem[i2f]           = "i2f";
        mnem[i2d]           = "i2d";
        mnem[l2i]           = "l2i";
        mnem[l2f]           = "l2f";
        mnem[l2d]           = "l2d";
        mnem[f2i]           = "f2i";
        mnem[f2l]           = "f2l";
        mnem[f2d]           = "f2d";
        mnem[d2i]           = "d2i";
        mnem[d2l]           = "d2l";
        mnem[d2f]           = "d2f";
        mnem[int2byte]      = "int2byte";
        mnem[int2char]      = "int2char";
        mnem[int2short]     = "int2short";
        mnem[lcmp]          = "lcmp";
        mnem[fcmpl]         = "fcmpl";
        mnem[fcmpg]         = "fcmpg";
        mnem[dcmpl]         = "dcmpl";
        mnem[dcmpg]         = "dcmpg";
        mnem[ifeq]          = "ifeq";
        mnem[ifne]          = "ifne";
        mnem[iflt]          = "iflt";
        mnem[ifge]          = "ifge";
        mnem[ifgt]          = "ifgt";
        mnem[ifle]          = "ifle";
        mnem[if_icmpeq]     = "if_icmpeq";
        mnem[if_icmpne]     = "if_icmpne";
        mnem[if_icmplt]     = "if_icmplt";
        mnem[if_icmpge]     = "if_icmpge";
        mnem[if_icmpgt]     = "if_icmpgt";
        mnem[if_icmple]     = "if_icmple";
        mnem[if_acmpeq]     = "if_acmpeq";
        mnem[if_acmpne]     = "if_acmpne";
        mnem[goto_]         = "goto";
        mnem[jsr]           = "jsr";
        mnem[ret]           = "ret";
        mnem[tableswitch]   = "tableswitch";
        mnem[lookupswitch]  = "lookupswitch";
        mnem[ireturn]       = "ireturn";
        mnem[lreturn]       = "lreturn";
        mnem[freturn]       = "freturn";
        mnem[dreturn]       = "dreturn";
        mnem[areturn]       = "areturn";
        mnem[return_]       = "return";
        mnem[getstatic]     = "getstatic";
        mnem[putstatic]     = "putstatic";
        mnem[getfield]      = "getfield";
        mnem[putfield]      = "putfield";
        mnem[invokevirtual] = "invokevirtual";
        mnem[invokespecial] = "invokespecial";
        mnem[invokestatic]  = "invokestatic";
        mnem[invokeinterface] = "invokeinterface";
        mnem[xxxunusedxxx]  = "xxxunusedxxx";
        mnem[new_]          = "new";
        mnem[newarray]      = "newarray";
        mnem[anewarray]     = "anewarray";
        mnem[arraylength]   = "arraylength";
        mnem[athrow]        = "athrow";
        mnem[checkcast]     = "checkcast";
        mnem[instanceof_]   = "instanceof";
        mnem[monitorenter]  = "monitorenter";
        mnem[monitorexit]   = "monitorexit";
        mnem[wide]          = "wide";
        mnem[multianewarray]= "multianewarray";
        mnem[ifnull]        = "ifnull";
        mnem[ifnonnull]     = "ifnonnull";
        mnem[goto_w]        = "goto_w";
        mnem[jsr_w]         = "jsr_w";
        mnem[breakpoint]    = "breakpoint";
    }
    
/** the tree processor methods
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        return treelist;
    }
    
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        return treelist;
    }
    
    public Tree process(Tree tree) throws AbortCompilation
    {
        if (tree == null)
            out.println("<null tree>");
        else
            switch (tree)
            {
                case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                    if (info.codeGenerated)
                    {
                        out.println("//==== " + info.source);
                        worklist = new Stack();
                        disassembleTrees(decls);
                        disassembleWorklist();
                    }
                    break;
                    
                case Bad():
                    out.println("<bad tree>");
                    break;
                    
                default:
                    throw new InternalError();
            }
        return tree;
    }
}

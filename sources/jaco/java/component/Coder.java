//      /   _ _      JaCo
//  \  //\ / / \     - support for bytecode generation and constant pool maintanance
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Definition.*;


public class Coder extends Component
                   implements BytecodeTags, ModifierConst, ItemConst,
                              ConstantConst, TypeConst
{
/** other components
 */
    ErrorHandler        report;
    Items               items;
    

/** all conditional jumps come in pairs. To streamline the
 *  treatment of jumps, we also introduce a negation of an
 *  unconditional jump. That opcode happens to be jsr
 */
    public int          dontgoto = jsr;

/** The currently written code buffer
 */
    protected Code      code;

/** is code emitted now not dead?
 */
    public boolean      alive;

/** the method for which code is generated
 */
    public MethodDef    curMethod;

/** is it not allowed to compactify code, because something is
 *  pointing to current location?
 */
    protected boolean   fixedPc;

/** the current stacksize
 */
    protected int       stacksize;

/** the next variable address
 */
    protected int       nextadr;

/** the next avaiable register
 */
    protected int       nextreg;

/** the map from variable addresses to register numbers
 */
    protected int[]     adrmap;

/** The source code position of the currently translated statement
 */
    public int          statPos;

/** the vector of all local or blank final variables
 */
    public VarDef[]     locals;

/** the set of all variables declared in the currently innermost loop
 *  null if we are not in a loop
 */
    public Bits         inloop;

/** the set of all possibly uninitialized local variables
 */
    public Bits         uninits;

/** the set of all possibly initialized local variables
 */
    public Bits         inits;

/** the currently written constant pool
 */
    protected Pool      pool;


/** marks an unused slot in adrmap
 */
    protected static final int  UNUSED = MIN_INT;

/** stack difference of opcodes that need special treatment
 */
    public static final int     SPECIAL = -999;


/** component name
 */
    public String getName()
    {
        return "Coder";
    }

/** component initialization
 */
    public void init(BackendContext context)
    {
        super.init(context);
        report = context.compilerContext.mainContext.ErrorHandler();
        items = context.Items();
        
        adrmap = new int[32];
        locals = new VarDef[32];
    }

/** get a new constant pool
 */
    public Pool newPool()
    {
        return pool = ((BackendContext)context).Pool();
    }

/** get a new code buffer
 */
    public Code newCode(boolean fatcode, MethodDef meth)
    {
        code = ((BackendContext)context).Code(fatcode);
        uninits = new Bits();
        inits = new Bits();
        curMethod = meth;
        fixedPc = false;
        stacksize = 0;
        nextadr = 0;
        nextreg = 0;
        statPos = Position.NOPOS;
        inloop = null;
        alive = true;
        return code;
    }


//////////// Operations having to do with code generation

/** the current output code pointer
 */
    public int curPc()
    {
        fixedPc = true;
        return code.cp;
    }

/** the current stack size
 */
    public int stackSize()
    {
        return stacksize;
    }

/** The next available variable slot
 */
    public int nextVar()
    {
        return nextadr;
    }

/** the current un-initialization vector
 */
    public Bits uninitSet()
    {
        return uninits.dup();
    }

/** the current initialization vector
 */
    public Bits initSet()
    {
        return inits.dup();
    }

/** operation frequencies
 */
    boolean statistics = false;
    int[] freq = new int[ByteCodeCount];
    int total = 0;
    int adr1 = 0;
    int adr2 = 0;
    int adr4 = 0;
    int field2 = 0;
    int field4 = 0;

/** emit a byte of code
 */
    public void emit1(int od)
    {
        if (alive)
        {
            if (code.cp == code.code.length)
                code.addCode((byte)od);
            else
                code.code[code.cp++] = (byte)od;
            if (statistics) total++;
        }
    }

/** emit two bytes of code
 */
    public void emit2(int od)
    {
        if (alive)
        {
            if (code.cp + 2 > code.code.length)
            {
                code.addCode(od >> 8);
                code.addCode(od);
            }
            else
            {
                code.code[code.cp++] = (byte)(od >> 8);
                code.code[code.cp++] = (byte)od;
            }
                if (statistics)
                {
                    total += 2;
                field2++;
            }
        }
    }

/** emit four bytes of code
 */
    public void emit4(int od)
    {
        emit1(od >> 24);
        emit1(od >> 16);
        emit1(od >> 8);
        emit1(od);
        if (statistics)
            field4++;
    }

/** emit an opcode, adjust stacksize by sdiff
 */
    public void emitop(int op, int sdiff)
    {
    	//System.out.println("emitop(" + op + ", " + sdiff + ")");
        if (alive)
        {
            emit1(op);
            if (sdiff <= -1000)
            {
                stacksize = stacksize + sdiff + 1000;
                markDead();
                report.assert(stacksize == 0);
            }
            else
            {
                stacksize += sdiff;
                report.assert(stacksize >= 0);
                if (stacksize > code.max_stack)
                    code.max_stack = stacksize;
                }
                if (statistics)
                    freq[op]++;
        }
    }

/** emit an opcode, adjust stacksize by stackdiff[op]
 */
    public void emitop(int op)
    {
        emitop(op, stackdiff[op]);
    }

/** emit an opcode with a one-byte field
 */
    public void emitop1(int op, int od)
    {
        emitop(op);
        emit1(od);
        if (statistics)
            adr1++;
    }

/** emit an opcode with a one-byte field.
 *  widen if field does not fit in a byte.
 */
    public void emitop1w(int op, int od)
    {
        if (od > 0xff)
        {
            emit1(wide);
            emitop2(op, od);
        }
        else
            emitop1(op, od);
    }

/** emit an opcode with a two-byte field.
 */
    public void emitop2(int op, int od)
    {
        emitop(op);
        emit2(od);
        if (statistics)
        {
            adr2++;
            field2--;
        }
    }

/** emit an opcode with a four-byte field.
 */
    public void emitop4(int op, int od)
    {
        emitop(op);
        emit4(od);
        if (statistics)
        {
            adr4++;
            field4--;
        }
    }

/** place a byte into code at address pc.
 */
    public void put1(int pc, int op)
    {
        // pre: pc < code.cp
        code.code[pc] = (byte)op;
    }

/** place two bytes into code at address pc.
 */
    public void put2(int pc, int od)
    {
        // pre: pc + 2 <= code.cp
        put1(pc, od >> 8);
        put1(pc+1, od);
    }

/** place four  bytes into code at address pc.
 */
    public void put4(int pc, int od)
    {
        // pre: pc + 4 <= code.cp
        put1(pc  , od >> 24);
        put1(pc+1, od >> 16);
        put1(pc+2, od >> 8);
        put1(pc+3, od);
    }

/** return code byte at position pc as an unsigned
 */
    public int get1(int pc)
    {
        return code.code[pc] & 0xff;
    }

/** return four code bytes at position pc
 */
    public int get4(int pc)
    {
        // pre: pc + 4 <= code.cp
        return
            (get1(pc) << 24) |
            (get1(pc+1) << 16) |
            (get1(pc+2) << 8) |
            (get1(pc+3));
    }

/** align code pointer to next factor boundary:
 */
    public void align(int factor)
    {
        if (alive)
            while ((code.cp % factor) != 0)
                emit1(0);
    }

/** mark code dead
 */
    public void markDead()
    {
        alive = false;
        uninits.clear();
        inits.clear();
    }

/** generate an entry point with un-initialization vector uninits:
 */
    public void entryPoint(Bits newuninits, Bits newinits)
    {
        uninits.orSet(newuninits);
        inits.orSet(newinits);
        alive = true;
    }

/** enter loop; return previous 'inloop' status
 */
    public Bits enterLoop()
    {
        Bits prev = inloop;
        inloop = new Bits();
        return prev;
    }

/** exit loop; restore previous status
 */
    public void exitLoop(Bits previnloop)
    {
        inloop = previnloop;
    }

/** set stack size
 */
    public void clearStack()
    {
        stacksize = 0;
    }

/** add one to stack
 */
    public void pushStack(int tos)
    {
        stacksize = stacksize + width(tos);
        if (stacksize > code.max_stack)
            code.max_stack = stacksize;
    }

/** pop one from stack
 */
    public void popStack(int tos)
    {
        stacksize -= width(tos);
    }

/** register a catch clause
 */
    public void registerCatch(int start_pc, int end_pc,
                              int handler_pc, int catch_type)
    {
        if (start_pc != end_pc)
            code.addCatch(start_pc, end_pc, handler_pc, catch_type);
    }

/** create a new local variable address and return it.
 */
    public int newLocal(int typecode)
    {
        int adr = nextadr++;
        int w = width(typecode);
        int reg = nextreg;
    
        nextreg = reg + w;
        if (nextreg > code.max_locals)
            code.max_locals = nextreg;
        while (adr >= adrmap.length)
        {
            int[]   newadrmap = new int[adrmap.length * 2];
                System.arraycopy(adrmap, 0, newadrmap, 0, adrmap.length);
                adrmap = newadrmap;
        }
        if (w == 2)
            adrmap[adr] = -reg;
        else
            adrmap[adr] = reg;
        return adr;
    }

    public int newLocal(Type type)
    {
        return newLocal(items.make.typecode(type));
    }

    public void newLocal(VarDef v)
    {
        v.adr = newLocal(items.make.typecode(v.type));
        code.addLocalVar(regOf(v.adr), v);
        rememberLocal(v);
        if (((v.modifiers & FINAL) != 0) && (inloop != null))
            inloop.incl(v.adr);
    }

    public int newThis(MethodDef owner) {
        VarDef def = new Definition.VarDef(null,
            newLocal(items.make.typecode(owner.owner.type)), -1);
        def.kind = DefinitionConst.VAR;
    def.modifiers = FINAL;
    def.name = Name.fromString("this");
    def.type = owner.owner.type;
    def.owner = owner;
    def.def = def;
    def.fullname = def.name;
        code.addLocalVar(regOf(def.adr), def);
        return def.adr;
    }
    
    public void newFinal(VarDef v)
    {
        v.adr = nextadr++;
        rememberLocal(v);
    }

    public void endFinal(VarDef v)
    {
        locals[v.adr] = null;
    }

    public void newRegSegment()
    {
        nextreg = code.max_locals;
    }

    protected void rememberLocal(VarDef v)
    {
        while (v.adr >= locals.length)
        {
              VarDef[]  newlocals = new VarDef[locals.length * 2];
              System.arraycopy(locals, 0, newlocals, 0, locals.length);
              locals = newlocals;
        }
        locals[v.adr] = v;
    }

/** return the register corresponding to given adr
 */
    public int regOf(int adr)
    {
        int reg = adrmap[adr];
        if (reg < 0)
            reg = -reg;
        return reg;
    }

/** end scopes of all variables with numbers >= first
 */
    public void endScopes(int first)
    {
        if (first < nextadr)
        {
              for (int i = first; i < nextadr; i++)
                code.setEndPc(i);
            nextreg = regOf(first);
        }
    }

/** mark beginning of statement
 */
    public void statBegin(int pos)
    {
        if (pos != Position.NOPOS)
        {
            if (alive)
            {
                statPos = pos;
                report.assert(stacksize == 0);
                code.addLineNumber(code.cp, Position.line(pos));
            }
            else
                report.warning(pos, "statement is unreachable");
        }
    }

/** let variable at adr be uninitialized
  */
    public void letUninit(int adr)
    {
        uninits.incl(adr);
        inits.excl(adr);
    }

/** let variable at adr be initialized
 */
    public void letInit(int adr)
    {
        if (alive)
        {
              uninits.excl(adr);
              inits.incl(adr);
              code.setStartPc(adr);
        }
    }

/** check that variable has been initialized
 */
    public void checkInit(int pos, int adr)
    {
        if (alive)
        {
            if (uninits.member(adr))
            {
                report.error(pos, locals[adr] + " might not have been " +
                                "initialized");
                uninits.excl(adr);
                }
        }
    }

/** check that variable has not been initialized
 */
    public void checkUninit(int pos, int adr)
    {
        if (alive)
        {
                if (inits.member(adr) ||
                ((inloop != null) && !inloop.member(adr)))
            {
                report.error(pos, locals[adr] +
                                " might already have been assigned to");
                inits.excl(adr);
                }
        }
    }

    public void checkInit(int pos, VarDef v)
    {
        if ((v.adr >= 0) && (v.adr < locals.length) && (locals[v.adr] == v))
              checkInit(pos, v.adr);
    }

    public void checkFirstInit(int pos, VarDef v)
    {
        if ((v.adr >= 0) && (v.adr < locals.length) && (locals[v.adr] == v))
        {
                checkUninit(pos, v.adr);
                letInit(v.adr);
        }
    }


//////////// Operations having to do with constant pool generation

/** put an object into the pool
 */
    public int putConstant(Constant c)
    {
        return pool.put(c);
    }

/** put a reference to type t in the constant pool;
 *  return the reference's index.
 */
    public int mkref(Type type)
    {
        int idx;
        switch (type.deref())
        {
            case ClassType(_):
                return pool.put(type.tdef());

            default:
                return pool.put(type);
        }
    }

/** put a reference to object obj in the constant pool;
 *  return the reference's index.
 */
    public int mkref(Definition def)
    {
        return pool.put(def);
    }

//////////// type codes & related stuff

/** collapse code for subtypes of int to INTcode
 */
    public int truncate(int tc)
    {
        switch (tc)
        {
            case BYTE_CODE:
            case SHORT_CODE:
            case CHAR_CODE:
                return INT_CODE;
            
            default:
                return tc;
        }
    }

/** return collapsed form of type code
 */
    public int shortcode(Type type)
    {
        return truncate(items.make.typecode(type));
    }

/** the width in bytes of objects of the type
 */
    public int width(int typecode)
    {
        switch (typecode)
        {
            case LONG_CODE:
            case DOUBLE_CODE:
                return 2;
            
            case VOID_CODE:
                return 0;
            
            default:
                return 1;
        }
    }
    
    public int width(Type type)
    {
        return width(items.make.typecode(type));
    }

/** the total width taken up by a vector of objects.
 */
    public int width(Type types[])
    {
        int w = 0;
        for (int i = 0; i < types.length; i++)
            w = w + width(types[i]);
        return w;
    }

/** given a type code tc, the opcode that loads a zero of that type.
 */
    public int zero(int tc)
    {
        switch(tc)
        {
            case INT_CODE:
            case BYTE_CODE:
            case SHORT_CODE:
            case CHAR_CODE:
                    return iconst_0;
                
            case LONG_CODE:
                return lconst_0;
            
            case FLOAT_CODE:
                return fconst_0;
            
            case DOUBLE_CODE:
                return dconst_0;
            
            default:
                throw new InternalError();
        }
    }

/** given a typecode tc, the opcode that loads a one of that type.
 */
    public int one(int tc)
    {
        return zero(tc) + 1;
    }

/** given a type code tc, emit a -1 of the given type (either int or long)
 */
    public void emitMinusOne(int tc)
    {
        if (tc == LONG_CODE)
        {
            emitop(lconst_0);
            emitop(lconst_1);
            emitop(lsub);
        }
        else
            emitop(iconst_m1);
    }


//////////// Operations having to do jumps

/** negate a branch opcode
 */
    public int negate(int opcode)
    {
        if ((opcode == ifnonnull) || (opcode == ifnull))
            return opcode ^ 1;
        else
            return ((opcode + 1) ^ 1) - 1;
    }
    
/** emit a jump instruction; l is the chain pointing to the jump.
 *  l needs to be adjusted if this is a wide conditional jump
 */
    public void emitJump(Chain l, int opcode)
    {
        if (code.fatcode)
        {
            if ((opcode == goto_) || (opcode == jsr))
                emitop4(opcode + goto_w - goto_, 0);
            else
            {
                emitop2(negate(opcode), 8);
                emitop4(goto_w, 0);
                l.pc += 3;
            }
        }
        else
            emitop2(opcode, 0);
    }


/** emit a branch with given opcode; return its chain.
 *  branch differs from jump in that jsr is treated as noop
 */
    public Chain branch(int opcode)
    {
        if ((opcode == dontgoto) || !alive)
            return null;
            else
            {
                int sdiff = stackdiff[opcode]/* >> SDIFF_SHIFT*/;
                stacksize = stacksize + sdiff;
                Chain   l = new Chain(code.cp, null, stacksize, uninitSet(), initSet());
            stacksize = stacksize - sdiff;
            emitJump(l, opcode);
            fixedPc = code.fatcode;
            if (opcode == goto_)
                markDead();
            return l;
        }
    }

/** resolve chain l to point to given target
 */
    public void resolve(Chain l, int target)
    {
        if (l != null)
        {
            report.assert((target > l.pc) || (stacksize == 0));
            if (target > code.cp)
                target = code.cp;
            if ((get1(l.pc) == goto_) &&
                (l.pc + 3 == target) &&
                (target == code.cp) && !fixedPc)
            {
                code.cp = code.cp - 3;
                target = target - 3;
            }
            else
            {
                if (code.fatcode)
                    put4(l.pc + 1, target - l.pc);
                else
                if ((target - l.pc < MIN_SHORT) || (target - l.pc > MAX_SHORT))
                    code.fatcode = true;
                else
                    put2(l.pc + 1, target - l.pc);
                report.assert(!alive || (l.stacksize == stacksize));
            }
            fixedPc = true;
            resolve(l.next, target);
            if (code.cp == target)
            {
                stacksize = l.stacksize;
                alive = true;
                uninits.orSet(l.uninits);
                inits.orSet(l.inits);
            }
        }
    }

/** Resolve chain l to point to current code pointer:
 */
    public void resolve(Chain l)
    {
        resolve(l, code.cp);
    }

/** Merge the jumps in of two chains into one:
 */
    public Chain mergeChains(Chain l1, Chain l2)
    {
        if (l2 == null)
            return l1;
        else
        if (l1 == null)
            return l2;
        else
        {
            report.assert(l1.stacksize == l2.stacksize);
            return new Chain(l2.pc,
                             mergeChains(l1, l2.next),
                             l2.stacksize, l2.uninits, l2.inits);
        }
    }


//////////// Setting things up

/** how opcodes affect the stack size;
 *  opcodes with stackdiff value SPECIAL need to be teated specially;
 *  opcodes with stackdiff value < -1000 are jumps after which code is dead
 */
    protected static int[]  stackdiff;
    static
    {
        stackdiff = new int[ByteCodeCount];
        stackdiff[nop]              = 0;
        stackdiff[aconst_null]      = 1;
        stackdiff[iconst_m1]        = 1;
        stackdiff[iconst_0]         = 1;
        stackdiff[iconst_1]         = 1;
        stackdiff[iconst_2]         = 1;
        stackdiff[iconst_3]         = 1;
        stackdiff[iconst_4]         = 1;
        stackdiff[iconst_5]         = 1;
        stackdiff[lconst_0]         = 2;
        stackdiff[lconst_1]         = 2;
        stackdiff[fconst_0]         = 1;
        stackdiff[fconst_1]         = 1;
        stackdiff[fconst_2]         = 1;
        stackdiff[dconst_0]         = 2;
        stackdiff[dconst_1]         = 2;
        stackdiff[bipush]           = 1;
        stackdiff[sipush]           = 1;
        stackdiff[ldc]              = SPECIAL;
        stackdiff[ldc_w]            = SPECIAL;
        stackdiff[ldc2_w]           = SPECIAL;
        stackdiff[iload]            = 1;
        stackdiff[lload]            = 2;
        stackdiff[fload]            = 1;
        stackdiff[dload]            = 2;
        stackdiff[aload]            = 1;
        stackdiff[iload_0]          = 1;
        stackdiff[lload_0]          = 2;
        stackdiff[fload_0]          = 1;
        stackdiff[dload_0]          = 2;
        stackdiff[aload_0]          = 1;
        stackdiff[iload_1]          = 1;
        stackdiff[lload_1]          = 2;
        stackdiff[fload_1]          = 1;
        stackdiff[dload_1]          = 2;
        stackdiff[aload_1]          = 1;
        stackdiff[iload_2]          = 1;
        stackdiff[lload_2]          = 2;
        stackdiff[fload_2]          = 1;
        stackdiff[dload_2]          = 2;
        stackdiff[aload_2]          = 1;
        stackdiff[iload_3]          = 1;
        stackdiff[lload_3]          = 2;
        stackdiff[fload_3]          = 1;
        stackdiff[dload_3]          = 2;
        stackdiff[aload_3]          = 1;
        stackdiff[iaload]           = -1;
        stackdiff[laload]           = 0;
        stackdiff[faload]           = -1;
        stackdiff[daload]           = 0;
        stackdiff[aaload]           = -1;
        stackdiff[baload]           = -1;
        stackdiff[caload]           = -1;
        stackdiff[saload]           = -1;
        stackdiff[istore]           = -1;
        stackdiff[lstore]           = -2;
        stackdiff[fstore]           = -1;
        stackdiff[dstore]           = -2;
        stackdiff[astore]           = -1;
        stackdiff[istore_0]         = -1;
        stackdiff[lstore_0]         = -2;
        stackdiff[fstore_0]         = -1;
        stackdiff[dstore_0]         = -2;
        stackdiff[astore_0]         = -1;
        stackdiff[istore_1]         = -1;
        stackdiff[lstore_1]         = -2;
        stackdiff[fstore_1]         = -1;
        stackdiff[dstore_1]         = -2;
        stackdiff[astore_1]         = -1;
        stackdiff[istore_2]         = -1;
        stackdiff[lstore_2]         = -2;
        stackdiff[fstore_2]         = -1;
        stackdiff[dstore_2]         = -2;
        stackdiff[astore_2]         = -1;
        stackdiff[istore_3]         = -1;
        stackdiff[lstore_3]         = -2;
        stackdiff[fstore_3]         = -1;
        stackdiff[dstore_3]         = -2;
        stackdiff[astore_3]         = -1;
        stackdiff[iastore]          = -3;
        stackdiff[lastore]          = -4;
        stackdiff[fastore]          = -3;
        stackdiff[dastore]          = -4;
        stackdiff[aastore]          = -3;
        stackdiff[bastore]          = -3;
        stackdiff[castore]          = -3;
        stackdiff[sastore]          = -3;
        stackdiff[pop]              = -1;
        stackdiff[pop2]             = -2;
        stackdiff[dup]              = 1;
        stackdiff[dup_x1]           = 1;
        stackdiff[dup_x2]           = 1;
        stackdiff[dup2]             = 2;
        stackdiff[dup2_x1]          = 2;
        stackdiff[dup2_x2]          = 2;
        stackdiff[swap]             = 0;
        stackdiff[iadd]             = -1;
        stackdiff[ladd]             = -2;
        stackdiff[fadd]             = -1;
        stackdiff[dadd]             = -2;
        stackdiff[isub]             = -1;
        stackdiff[lsub]             = -2;
        stackdiff[fsub]             = -1;
        stackdiff[dsub]             = -2;
        stackdiff[imul]             = -1;
        stackdiff[lmul]             = -2;
        stackdiff[fmul]             = -1;
        stackdiff[dmul]             = -2;
        stackdiff[idiv]             = -1;
        stackdiff[ldiv]             = -2;
        stackdiff[fdiv]             = -1;
        stackdiff[ddiv]             = -2;
        stackdiff[imod]             = -1;
        stackdiff[lmod]             = -2;
        stackdiff[fmod]             = -1;
        stackdiff[dmod]             = -2;
        stackdiff[ineg]             = 0;
        stackdiff[lneg]             = 0;
        stackdiff[fneg]             = 0;
        stackdiff[dneg]             = 0;
        stackdiff[ishl]             = -1;
        stackdiff[lshl]             = -1;
        stackdiff[ishr]             = -1;
        stackdiff[lshr]             = -1;
        stackdiff[iushr]            = -1;
        stackdiff[lushr]            = -1;
        stackdiff[iand]             = -1;
        stackdiff[land]             = -2;
        stackdiff[ior]              = -1;
        stackdiff[lor]              = -2;
        stackdiff[ixor]             = -1;
        stackdiff[lxor]             = -2;
        stackdiff[iinc]             = 0;
        stackdiff[i2l]              = 1;
        stackdiff[i2f]              = 0;
        stackdiff[i2d]              = 1;
        stackdiff[l2i]              = -1;
        stackdiff[l2f]              = -1;
        stackdiff[l2d]              = 0;
        stackdiff[f2i]              = 0;
        stackdiff[f2l]              = 1;
        stackdiff[f2d]              = 1;
        stackdiff[d2i]              = -1;
        stackdiff[d2l]              = 0;
        stackdiff[d2f]              = -1;
        stackdiff[int2byte]         = 0;
        stackdiff[int2char]         = 0;
        stackdiff[int2short]        = 0;
        stackdiff[lcmp]             = -3;
        stackdiff[fcmpl]            = -1;
        stackdiff[fcmpg]            = -1;
        stackdiff[dcmpl]            = -3;
        stackdiff[dcmpg]            = -3;
        stackdiff[ifeq]             = -1;
        stackdiff[ifne]             = -1;
        stackdiff[iflt]             = -1;
        stackdiff[ifge]             = -1;
        stackdiff[ifgt]             = -1;
        stackdiff[ifle]             = -1;
        stackdiff[if_icmpeq]        = -2;
        stackdiff[if_icmpne]        = -2;
        stackdiff[if_icmplt]        = -2;
        stackdiff[if_icmpge]        = -2;
        stackdiff[if_icmpgt]        = -2;
        stackdiff[if_icmple]        = -2;
        stackdiff[if_acmpeq]        = -2;
        stackdiff[if_acmpne]        = -2;
        stackdiff[goto_]            = 0;
        stackdiff[jsr]              = 0;
        stackdiff[ret]              = 0;
        stackdiff[tableswitch]      = -1;
        stackdiff[lookupswitch]     = -1;
        stackdiff[ireturn]          = -1001;
        stackdiff[lreturn]          = -1002;
        stackdiff[freturn]          = -1001;
        stackdiff[dreturn]          = -1002;
        stackdiff[areturn]          = -1001;
        stackdiff[return_]          = -1000;
        stackdiff[getstatic]        = SPECIAL;
        stackdiff[putstatic]        = SPECIAL;
        stackdiff[getfield]         = SPECIAL;
        stackdiff[putfield]         = SPECIAL;
        stackdiff[invokevirtual]    = SPECIAL;
        stackdiff[invokespecial]    = SPECIAL;
        stackdiff[invokestatic]     = SPECIAL;
        stackdiff[invokeinterface]  = SPECIAL;
        stackdiff[xxxunusedxxx]     = 0;
        stackdiff[new_]             = 1;
        stackdiff[newarray]         = 0;
        stackdiff[anewarray]        = 0;
        stackdiff[arraylength]      = 0;
        stackdiff[athrow]           = -1001;
        stackdiff[checkcast]        = 0;
        stackdiff[instanceof_]      = 0;
        stackdiff[monitorenter]     = -1;
        stackdiff[monitorexit]      = -1;
        stackdiff[wide]             = 0;
        stackdiff[multianewarray]   = SPECIAL;
        stackdiff[ifnull]           = -1;
        stackdiff[ifnonnull]        = -1;
        stackdiff[goto_w]           = 0;
        stackdiff[jsr_w]            = 0;
        stackdiff[breakpoint]       = 0;
    }

    public void stats()
    {
        int ninsts = 0;
        for (int i = 0; i < ByteCodeCount; i++)
            ninsts += freq[i];
        for (int i = 0; i < ByteCodeCount; i++)
            System.out.println(100f * (float)freq[i]/(float)ninsts + "% " +
                               Disassembler.mnem[i]);
        System.out.println(ninsts + " instuctions");
        System.out.println(total + " bytes");
        System.out.println(adr1 + " one byte operands");
        System.out.println(adr2 + " two byte operands");
        System.out.println(adr4 + " four byte operands");
        System.out.println(field2 + " two byte fields");
        System.out.println(field4 + " four byte fields");
    }
}

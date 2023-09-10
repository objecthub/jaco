package jaco.join.component;

import jaco.framework.*;
import jaco.join.struct.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import join.runtime.*;


class Program implements Bytecodes, ModifierConst {
    int funs;
    int[] guards = new int[8];
    int[] lefttoks = new int[8];
    int[] args = new int[8];
    byte[][] codes = new byte[8][];
    
    int spaces;
    int[] members = new int[8];
    int[] actions = new int[8];
    int[] tokens = new int[8];
    int[][] actiontable = new int[8][];
    
    Code toplevel;
    
    Program() {
        toplevel = new Code(this);
    }
    
    int newfun(int gs, int lt, int as, byte[] code) {
        if (funs == guards.length) {
            int[] nguards = new int[funs * 2];
            int[] nlefttoks = new int[funs * 2];
            int[] nargs = new int[funs * 2];
            byte[][] ncodes = new byte[funs * 2][];
            System.arraycopy(guards, 0, nguards, 0, funs);
            System.arraycopy(lefttoks, 0, nlefttoks, 0, funs);
            System.arraycopy(args, 0, nargs, 0, funs);
            System.arraycopy(codes, 0, ncodes, 0, funs);
            guards = nguards;
            lefttoks = nlefttoks;
            args = nargs;
            codes = ncodes;
        }
        guards[funs] = gs;
        lefttoks[funs] = lt;
        args[funs] = as;
        codes[funs] = code;
        return funs++;
    }

    int newspace(JScope s) {
        if (spaces == members.length) {
            int[] nmembers = new int[spaces * 2];
            int[] nactions = new int[spaces * 2];
            int[] ntokens = new int[spaces * 2];
            int[][] nactiontable = new int[spaces * 2][];
            System.arraycopy(members, 0, nmembers, 0, spaces);
            System.arraycopy(actions, 0, nactions, 0, spaces);
            System.arraycopy(tokens, 0, ntokens, 0, spaces);
            System.arraycopy(actiontable, 0, nactiontable, 0, spaces);
            members = nmembers;
            actions = nactions;
            tokens = ntokens;
            actiontable = nactiontable;
        }
        members[spaces] = s.defs.size;
        actions[spaces] = 0;
        tokens[spaces] = 0;
        actiontable[spaces] = new int[members[spaces]];
        return s.id = spaces++;
    }
    
    void print(int pc, String line) {
        System.out.println("    " + (pc - 1) + ": " + line);
    }
    
    int get2b(byte[] code, int pc) {
        int i = code[pc];
        if (i < 0)
            i += 256;
        int j = code[pc + 1];
        if (j < 0)
            j += 256;
        return (i << 8) + j;
    }
    
    int get4b(byte[] code, int pc) {
        return (get2b(code, pc) << 16) + get2b(code, pc + 2);
    }
    
    void disassemble(byte[] code) {
        int pc = 0;
        while (pc < code.length)
            switch (code[pc++]) {
                case NOP:
                    print(pc, "NOP");
                    break;
                case RET:
                    print(pc, "RET");
                    break;
                case STOP:
                    print(pc, "STOP");
                    break;
                case UNIT:
                    print(pc, "UNIT");
                    break;
                case BYTE:
                    print(pc, "BYTE " + code[pc++]);
                    break;
                case INT:
                    print(pc, "INT " + get4b(code, pc));
                    pc += 4;
                    break;
                case TRUE:
                    print(pc, "TRUE");
                    break;
                case FALSE:
                    print(pc, "FALSE");
                    break;
                case TUPEL:
                    print(pc, "TUPEL " + code[pc++]);
                    break;
                case DECOMP:
                    print(pc, "DECOMP");
                    break;
                case FUN:
                    print(pc, "FUN " + get2b(code, pc));
                    pc += 2;
                    break;
                case TOKEN:
                    print(pc, "TOKEN " + code[pc++]);
                    break;
                case LOAD:
                    print(pc, "LOAD " + code[pc++] + ", " +  code[pc++]);
                    break;
                case STORE:
                    print(pc, "STORE " + code[pc++]);
                    break;
                case DUP:
                    print(pc, "DUP");
                    break;
                case POP:
                    print(pc, "POP");
                    break;
                case ENTER:
                    print(pc, "ENTER " + get2b(code, pc));
                    pc += 2;
                    break;
                case LEAVE:
                    print(pc, "LEAVE");
                    break;
                case GOTO:
                    print(pc, "GOTO " + get2b(code, pc));
                    pc += 2;
                    break;
                case JMPT:
                    print(pc, "JMPT " + get2b(code, pc));
                    pc += 2;
                    break;
                case JMPF:
                    print(pc, "JMPF " + get2b(code, pc));
                    pc += 2;
                    break;
                case APPLY:
                    print(pc, "APPLY");
                    break;
                case YIELD:
                    print(pc, "YIELD");
                    break;
                case FORK:
                    print(pc, "FORK " + get2b(code, pc));
                    pc += 2;
                    break;
                case ADD:
                    print(pc, "ADD");
                    break;
                case SUB:
                    print(pc, "SUB");
                    break;
                case MULT:
                    print(pc, "MULT");
                    break;
                case DIV:
                    print(pc, "DIV");
                    break;
                case EQL:
                    print(pc, "EQL");
                    break;
                case NOT:
                    print(pc, "NOT");
                    break;
                case ERROR:
                    print(pc, "ERROR");
                    break;
                default:
                    print(pc, "?");
            }
    }
    
    void dump() {
        System.out.println("FUNCTIONS");
        System.out.println();
        System.out.println("  TOPLEVEL()");
        disassemble(toplevel.bytecode());
        for (int i = 0; i < funs; i++) {
            System.out.println();
            System.out.println("  FUNCTION " + i + "(" + args[i] +
                               " variables, guard is " + guards[i] + ", " +
                               " left token map is " + lefttoks[i] + ")");
            disassemble(codes[i]);
        }
        System.out.println();
        System.out.println("STATE SPACES");
        for (int i = 0; i < spaces; i++) {
            System.out.println();
            System.out.println("  SPACE " + i + ":");
            System.out.println("    members: " + members[i] + ", tokens: " +
                               tokens[i] + ", actions: " + actions[i]);
            for (int j = 0; j < actions[i]; j++)
                System.out.println("    invoke function " + actiontable[i][j] + " for " +
                                   guards[actiontable[i][j]]);
        }
    }
    
    Trees trees;
    Constants consts;
    
    Tree[] translate(String name, Trees trees, Constants consts) {
        this.trees = trees;
        this.consts = consts;
        Tree[] ts = new Tree[funs + spaces + 2];
        ts[0] = generateInit();
        for (int i = 0; i < spaces; i++)
            ts[i + 1] = generateSpaceDecl(i);
        for (int i = 0; i < funs; i++)
            ts[i + spaces + 1] = generateFunDecl(i);
        ts[ts.length - 1] = generateStartup();
        Tree t = trees.make.MethodDecl(Name.fromString("main"),
                PUBLIC | STATIC, trees.make.BasicType(TypeConst.VOID),
                new Tree.VarDecl[]{
                    (Tree.VarDecl)trees.make.VarDecl(
                        Name.fromString("args"), 0,
                            trees.make.ArrayTypeTerm(
                                trees.make.Ident(Name.fromString("String"))), null)},
                new Tree[0],
                ts);
        return new Tree[]{
            trees.make.Import(TreeConst.IMPORTSTAR,
                trees.make.Select(trees.make.Ident(Name.fromString("join")),
                    Name.fromString("runtime"))),
                trees.make.ClassDecl(
                    Name.fromString(name), PUBLIC, null, new Tree[0], new Tree[]{t})};
    }
    
    Tree generateProgName() {
        return trees.make.Ident(Name.fromString("prog"));
    }
    
    Tree generateByteLit(byte b) {
        return trees.make.Literal(consts.make.IntConst(b, TypeConst.BYTE));
    }
    
    Tree generateInit() {
        return trees.make.VarDecl(Name.fromString("prog"), 0,
                trees.make.Ident(Name.fromString("JoinProgram")),
                trees.make.NewObj(null,
                    trees.make.Ident(Name.fromString("JoinProgram")),
                    new Tree[]{
                        trees.make.Literal(consts.make.IntConst(spaces)),
                        trees.make.Literal(consts.make.IntConst(funs))},
                    null));
    }
    
    byte findMatchingAction(int space, int i) {
        for (byte j = 0; j < actions[space]; j++)
            if ((guards[actiontable[space][j]] & i) == guards[actiontable[space][j]])
                return j;
        return -1;
    }
    
    Tree generateSpaceDecl(int i) {
        Tree[] args;
        Tree t, t2;
        if (actions[i] > 0) {
            args = new Tree[actions[i]];
            for (int j = 0; j < args.length; j++)
                args[j] = trees.make.Literal(consts.make.IntConst(actiontable[i][j]));
            t = trees.make.Aggregate(args,
                    trees.make.ArrayTypeTerm(trees.make.BasicType(TypeConst.INT)));
        } else
            t = trees.Null();
        
        if (tokens[i] == 0)
            t2 = trees.Null();
        else {
            args = new Tree[1 << tokens[i]];
            for (int j = 0; j < args.length; j++)
                args[j] = generateByteLit(findMatchingAction(i, j));
            t2 = trees.make.Aggregate(args,
                        trees.make.ArrayTypeTerm(trees.make.BasicType(TypeConst.BYTE)));
        }
        args = new Tree[4];
        args[0] = trees.make.Literal(consts.make.IntConst(i));
        args[1] = trees.make.Literal(consts.make.IntConst(members[i]));
        args[2] = t;
        args[3] = t2;
        t = trees.make.Select(generateProgName(), Name.fromString("defineStateSpace"));
        t = trees.make.Apply(t, args);
        return trees.make.Exec(t);
    }
    
    Tree generateFunDecl(int i) {
        Tree[] bcode = new Tree[codes[i].length];
        for (int j = 0; j < bcode.length; j++)
            bcode[j] = generateByteLit(codes[i][j]);
        Tree t = trees.make.Aggregate(bcode,
                    trees.make.ArrayTypeTerm(trees.make.BasicType(TypeConst.BYTE)));
        bcode = new Tree[5];
        bcode[0] = trees.make.Literal(consts.make.IntConst(i));
        bcode[1] = trees.make.Literal(consts.make.IntConst(guards[i]));
        bcode[2] = trees.make.Literal(consts.make.IntConst(lefttoks[i]));
        bcode[3] = trees.make.Literal(consts.make.IntConst(args[i]));
        bcode[4] = t;
        t = trees.make.Select(generateProgName(), Name.fromString("defineFunction"));
        t = trees.make.Apply(t, bcode);
        return trees.make.Exec(t);
    }
    
    Tree generateStartup() {
        Tree[] bcode = new Tree[toplevel.pc];
        for (int j = 0; j < bcode.length; j++)
            bcode[j] = generateByteLit(toplevel.bytecode[j]);
        Tree t = trees.make.Aggregate(bcode,
                trees.make.ArrayTypeTerm(trees.make.BasicType(TypeConst.BYTE)));
        t = trees.make.Apply(trees.make.Select(
                generateProgName(), Name.fromString("execute")),
                new Tree[]{t});
        t = trees.make.Apply(trees.make.Select(
                generateProgName(), Name.fromString("print")),
                new Tree[]{t});
        return trees.make.Exec(t);
    }
}

class Code implements Bytecodes {
    Program prog;
    int pc;
    byte[] bytecode;
    
    Code(Program prog) {
        this.prog = prog;
        this.bytecode = new byte[64];
    }
    
    void extend(int needed) {
        if ((pc + needed) >= bytecode.length) {
            byte[] nbytecode = new byte[bytecode.length * 2];
            System.arraycopy(bytecode, 0, nbytecode, 0, pc);
            bytecode = nbytecode;
        }
    }
    
    void emit(byte instr) {
        extend(1);
        bytecode[pc++] = instr;
    }
    
    void emit(byte instr, int arg) {
        extend(2);
        bytecode[pc++] = instr;
        bytecode[pc++] = (byte)arg;
    }
    
    void emit(byte instr, int arg1, int arg2) {
        extend(3);
        bytecode[pc++] = instr;
        bytecode[pc++] = (byte)arg1;
        bytecode[pc++] = (byte)arg2;
    }
    
    void emit2b(byte instr, int arg) {
        extend(3);
        bytecode[pc++] = instr;
        bytecode[pc++] = (byte)((arg >> 8) & 0xff);
        bytecode[pc++] = (byte)(arg & 0xff);
    }
    
    void emit4b(byte instr, int arg) {
        extend(5);
        bytecode[pc++] = instr;
        bytecode[pc++] = (byte)((arg >> 24) & 0xff);
        bytecode[pc++] = (byte)((arg >> 16) & 0xff);
        bytecode[pc++] = (byte)((arg >> 8) & 0xff);
        bytecode[pc++] = (byte)(arg & 0xff);
    }
    
    int fwdjump(byte instr) {
        extend(3);
        bytecode[pc++] = instr;
        int fixadr = pc;
        pc += 2;
        return fixadr;
    }
    
    void fixjump(int adr) {
        bytecode[adr] = (byte)((pc >> 8) & 0xff);
        bytecode[adr + 1] = (byte)(pc & 0xff);
    }
    
    int label() {
        return pc;
    }
    
    byte[] bytecode() {
        if (bytecode.length == pc)
            return bytecode;
        else {
            byte[] b = new byte[pc];
            System.arraycopy(bytecode, 0, b, 0, pc);
            return b;
        }
    }
}

class JoinCodeGenerator implements Bytecodes {

    int sortLhs(JTree lhs, JScope scope, JTree[] ts, int[] tags) {
        switch (lhs) {
            case Ident(Name name):
                System.out.println("error: " + name + " cannot be used as a token");
                return 0;
            case Apply(Ident(Name name), JTree arg):
                JDefinition def = scope.lookup(name);
                if (def.tag == JDefinition.UNSEEN)
                    def.tag = ++scope.tokens;
                int i = 0;
                while ((ts[i] != null) && (tags[i] < def.tag))
                    i++;
                if (ts[i] == null) {
                    ts[i] = arg;
                    tags[i] = def.tag;
                } else if (tags[i] == def.tag) {
                    System.out.println("error: duplicate token in left hand side");
                    return 0;
                } else {
                    System.arraycopy(ts, i, ts, i + 1, ts.length - i - 1);
                    System.arraycopy(tags, i, tags, i + 1, tags.length - i - 1);
                    ts[i] = arg;
                    tags[i] = def.tag;
                }
                return 1 << (def.tag - 1);
            case Join(JTree left, JTree right):
                return sortLhs(right, scope, ts, tags) | sortLhs(left, scope, ts, tags);
            default:
                throw new InternalError();
        }
    }
    
    void matchArguments(JTree[] ts, JScope scope, Code code) {
        for (int i = ts.length - 1; i >= 0; i--)
            if (ts[i] != null)
                matchArgument(ts[i], scope, code);
    }
    
    void matchArgument(JTree arg, JScope scope, Code code) {
        switch (arg) {
            case Ident(Name name):
                JDefinition def = scope.lookup(name);
                if (def.tag != JDefinition.UNSEEN)
                    System.out.println("error: duplicate use of variable name " + name +
                                       "on left hand side");
                else {
                    def.tag = JDefinition.SEEN;
                    code.emit(STORE, def.id - 1);
                }
                break;
            case Tupel(JTree[] ts):
                if (ts.length > 0) {
                    code.emit(DECOMP);
                    for (int i = ts.length - 1; i >= 0; i--)
                        matchArgument(ts[i], scope, code);
                } else
                    code.emit(POP);
                break;
            default:
                throw new InternalError();
        }
    }
    
    int generateCode(JTree lhs, JTree rhs, JScope scope, Code parent) {
        Code code = new Code(parent.prog);
        JTree[] ts = new JTree[16];
        int[] tags = new int[16];
        int guard = sortLhs(lhs, scope.outer, ts, tags);
        matchArguments(ts, scope, code);
        generateCode(rhs, scope, code);
        if (JType.jtypes.expand(rhs.type).isBottom())
            code.emit(UNIT);
        code.emit(RET);
        return code.prog.newfun(guard,
                                0,
                                scope.defs.size,
                                code.bytecode());
    }
    
    void generateCode(JTree tree, JScope scope, Code code) {
        switch (tree) {
            case TopLevel(JTree[] terms, _):
                for (int i = 0; i < terms.length; i++) {
                    generateCode(terms[i], scope, code);
                    if (i < (terms.length - 1))
                        code.emit(POP);
                }
                break;
            case Literal(Constant lit):
                switch (lit) {
                    case IntConst(int value):
                        if (value < 128)
                            code.emit(BYTE, (byte)value);
                        else
                            code.emit4b(INT, value);
                        break;
                    case LongConst(_):
                        throw new InternalError();
                    case FloatConst(_):
                        throw new InternalError();
                    case DoubleConst(_):
                        throw new InternalError();
                    case StringConst(_):
                        throw new InternalError();
                    default:
                        throw new InternalError();
                }
                break;
            case SpecialLiteral(int tag):
                switch (tag) {
                    case JoinConst.ERROR:
                        code.emit(ERROR);
                        break;
                    case JoinConst.FALSE:
                        code.emit(FALSE);
                        break;
                    case JoinConst.TRUE:
                        code.emit(TRUE);
                        break;
                    default:
                        throw new InternalError();
                }
                break;
            case Ident(Name name):
                code.emit(LOAD, scope.distance(name), scope.lookup(name).id - 1);
                break;
            case Select(JTree selected, Name selector):
                throw new InternalError();
            case Apply(JTree fun, JTree arg):
                generateCode(fun, scope, code);
                generateCode(arg, scope, code);
                if (JType.jtypes.expand(tree.type).isBottom())
                    code.emit(YIELD);
                else
                    code.emit(APPLY);
                break;
            case Join(JTree left, JTree right):
                int adr = code.fwdjump(FORK);
                generateCode(right, scope, code);
                code.emit(STOP);
                code.fixjump(adr);
                generateCode(left, scope, code);
                break;
            case Oper(int opcode, JTree left, JTree right):
                if (opcode == JoinConst.NOT) {
                    generateCode(right, scope, code);
                    code.emit(NOT);
                } else if (opcode == JoinConst.NEG) {
                    code.emit(BYTE, 0);
                    generateCode(right, scope, code);
                    code.emit(SUB);
                } else if (opcode == JoinConst.AND) {
                    generateCode(left, scope, code);
                    int adr = code.fwdjump(JMPT);
                    code.emit(FALSE);
                    int adr2 = code.fwdjump(GOTO);
                    code.fixjump(adr);
                    generateCode(right, scope, code);
                    code.fixjump(adr2);
                } else if (opcode == JoinConst.OR) {
                    generateCode(left, scope, code);
                    int adr = code.fwdjump(JMPF);
                    code.emit(TRUE);
                    int adr2 = code.fwdjump(GOTO);
                    code.fixjump(adr);
                    generateCode(right, scope, code);
                    code.fixjump(adr2);
                } else {
                    generateCode(left, scope, code);
                    generateCode(right, scope, code);
                    switch (opcode) {
                        case JoinConst.PLUS:
                            code.emit(ADD);
                            break;
                        case JoinConst.MINUS:
                            code.emit(SUB);
                            break;
                        case JoinConst.TIMES:
                            code.emit(MULT);
                            break;
                        case JoinConst.DIV:
                            code.emit(DIV);
                            break;
                        case JoinConst.EQ:
                            code.emit(EQL);
                            break;
                        case JoinConst.NOTEQ:
                            code.emit(EQL);
                            code.emit(NOT);
                            break;
                        case JoinConst.GT:
                            throw new InternalError();
                        case JoinConst.LT:
                            throw new InternalError();
                        case JoinConst.GTEQ:
                            throw new InternalError();
                        case JoinConst.LTEQ:
                            throw new InternalError();
                        default:
                            throw new InternalError();
                    }
                }
                break;
            case If(JTree cond, JTree ifpart, JTree elsepart):
                generateCode(cond, scope, code);
                int adr = code.fwdjump(JMPF);
                generateCode(ifpart, scope, code);
                int adr2 = code.fwdjump(GOTO);
                code.fixjump(adr);
                generateCode(elsepart, scope, code);
                code.fixjump(adr2);
                break;
            case Let(JTree head, JTree body):
                generateCode(head, scope, code);
                generateCode(body, ((JTree.Decls)head).scope, code);
                code.emit(LEAVE);
                break;
            case Tupel(JTree[] trees):
                if (trees.length == 0)
                    code.emit(UNIT);
                else {
                    for (int i = 0; i < trees.length; i++)
                        generateCode(trees[i], scope, code);
                    code.emit(TUPEL, trees.length);
                }
                break;
            case Decl(JTree lhs, JTree rhs, JScope s):
                switch (lhs) {
                    case Ident(Name name):
                        JDefinition def = scope.lookup(name);
                        if (def.tag != JDefinition.UNSEEN)
                            System.out.println("duplicate definition of " + name);
                        else {
                            def.tag = JDefinition.SEEN;
                            generateCode(rhs, s, code);
                            code.emit(STORE, def.id - 1);
                        }
                        break;
                    case Apply(Ident(Name name), JTree arg):
                        JDefinition def = scope.lookup(name);
                        if (def.tag != JDefinition.UNSEEN)
                            System.out.println("duplicate definition of " + name);
                        else {
                            def.tag = JDefinition.SEEN;
                            Code funcode = new Code(code.prog);
                            matchArgument(arg, s, funcode);
                            generateCode(rhs, s, funcode);
                            if (JType.jtypes.expand(rhs.type).isBottom())
                                funcode.emit(UNIT);
                            funcode.emit(RET);
                            code.emit2b(FUN, code.prog.newfun(0,
                                                              0,
                                                              s.defs.size,
                                                              funcode.bytecode()));
                            code.emit(STORE, def.id - 1);
                        }
                        break;
                    default:
                        code.prog.actiontable[scope.id][code.prog.actions[scope.id]++] =
                            generateCode(lhs, rhs, s, code);
                        break;
                }
                break;
            case Decls(JTree[] decls, JScope s):
                int space = code.prog.newspace(s);
                code.emit2b(ENTER, space);
                int adr = code.fwdjump(GOTO);
                int adr2 = code.label();
                for (int i = 0; i < decls.length; i++)
                    generateCode(decls[i], s, code);
                int adr3 = code.fwdjump(GOTO);
                code.fixjump(adr);
                for (int i = 0; i < s.defs.size; i++)
                    if (s.defs.ds[i].tag > 0) {
                        code.emit(TOKEN, s.defs.ds[i].tag);
                        code.emit(STORE, s.defs.ds[i].id - 1);
                    }
                code.emit2b(GOTO, adr2);
                code.fixjump(adr3);
                code.prog.tokens[space] = s.tokens;
                break;
            case Rec(JTree[] decls, JScope s):
                for (int i = 0; i < decls.length; i++)
                    generateCode(decls[i], s, code);
                break;
            default:
                throw new InternalError();
        }
    }
    
    public Program generateCode(JTree tree) {
        Program prog = new Program();
        generateCode(tree, null, prog.toplevel);
        prog.toplevel.emit(STOP);
        return prog;
    }
}

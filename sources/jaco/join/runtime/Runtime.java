package join.runtime;

import java.util.*;


public class JValue {
    public case Nil;
    public case Boolean(boolean value);
    public case Integer(int value);
    public case Tupel(JValue[] values);
    public case Record(  );
    public case Function(int id, StateSpace defining);
    public case Token(int tag, ArgumentQueue args, StateSpace defining);
    
    public static JValue Unit = Tupel(new JValue[0]);
    
    
    public boolean boolValue() {
        switch (this) {
            case Boolean(boolean value):
                return value;
            default:
                throw new InternalError();
        }
    }
    
    public int intValue() {
        switch (this) {
            case Integer(int value):
                return value;
            default:
                throw new InternalError();
        }
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof JValue))
            return false;
        JValue o = (JValue)obj;
        switch (this) {
            case Boolean(boolean value):
                switch (o) {
                    case Boolean(boolean v):
                        return value == v;
                    default:
                        return false;
                }
            case Integer(int value):
                switch (o) {
                    case Integer(int v):
                        return value == v;
                    default:
                        return false;
                }
            default:
                return this == o;
        }
    }
    
    public String toString() {
        switch (this) {
            case Nil:
                return "Nil";
            case Boolean(boolean value):
                return value ? "True" : "False";
            case Integer(int value):
                return "" + value;
            case Tupel(JValue[] values):
                if (values.length == 0)
                    return "()";
                else {
                    String res = "(" + values[0];
                    for (int i = 1; i < values.length; i++)
                        res += ", " + values[i];
                    return res + ")";
                }
            case Record(  ):
                return "<Record>";
            case Function(int id, _):
                return "<Function " + id + ":" + super.toString() + ">";
            case Token(int tag, _, _):
                return "<Token " + tag + ":" + super.toString() + ">";
            default:
                return "<JValue?>";
        }
    }
}

public class ArgumentQueue {
    int end;
    int start;
    Descriptor[] stack = new Descriptor[4];
    
    public void queue(JValue value, ExecutionContext context) {
        if (end == stack.length) {
            if (start >= (stack.length / 2)) {
                System.arraycopy(stack, start, stack, 0, end - start);
                end -= start;
                start = 0;
            } else {
                Descriptor[] newstack = new Descriptor[stack.length * 2];
                System.arraycopy(stack, start, newstack, 0, end - start);
                stack = newstack;
            }
        }
        stack[end++] = new Descriptor(value, context);
    }

    public Descriptor dequeue() {
        return stack[start++];
    }
    
    public boolean isEmpty() {
        return ((end - start) == 0);
    }
    
    static class Descriptor {
        JValue value;
        ExecutionContext context;
        
        Descriptor(JValue value, ExecutionContext context) {
            this.value = value;
            this.context = context;
        }
    }
}

public class StateSpace {

/** the join program to which this state space belongs
 */
    JoinProgram prog;
    
/** the enclosing state space (scope)
 */
    StateSpace encl;

/** the current state
 */
    int state;

/** members of the state space (values, functions or tags)
 */
    JValue[] members;
    
/** action definitions (functions associated with more than one tag)
 */
    JValue[] actions;

/** state table maps states to actions
 */
    byte[] states;

    
    StateSpace(JoinProgram prog, int ms, JValue[] actions,
               byte[] states, StateSpace defining) {
        this.prog = prog;
        this.encl = defining;
        this.state = 0;
        this.members = new JValue[ms];
        for (int i = 0; i < ms; i++)
            members[i] = JValue.Nil;
        this.actions = actions;
        this.states = states;
    }
    
    public JValue getMember(int back, int idx) {
        StateSpace space = this;
        while (back-- > 0)
            space = space.encl;
        try {
            return space.members[idx];
        } catch (ArrayIndexOutOfBoundsException e) {
            space = this;
            while (space != null) {
                if ((space.members != null) && (space.actions != null))
                    System.out.println("** members = " + space.members.length +
                                   "; actions = " + space.actions.length);
                else if (space.members == null)
                    System.out.println("** actions = " + space.actions.length);
                else
                    System.out.println("** members = " + space.members.length);
                space = space.encl;
            }
            return null;
        }
    }
    
    public void setMember(int idx, JValue value) {
        members[idx] = value;
    }
    
    public ExecutionContext yield(int tag, ArgumentQueue as,
                                  JValue arg, ExecutionContext parent) {
        state |= 1 << (tag - 1);
        as.queue(arg, parent);
        if (states[state] >= 0) {
            JValue.Function fun = (JValue.Function)actions[states[state]];
            int guard = prog.guards[fun.id];
            int lefttok = prog.lefttoks[fun.id];
            JValue[] args = new JValue[members.length];
            ExecutionContext context = null;
            for (int i = 0; i < members.length; i++)
                switch (members[i]) {
                    case Token(int id, ArgumentQueue c, _):
                        int bitmap = 1 << (id - 1);
                        if ((guard & bitmap) != 0) {
                            ArgumentQueue.Descriptor descr = c.dequeue();
                            //if ((lefttok & bitmap) != 0)
                            if (descr.context != null)
                                context = descr.context;
                            args[id] = descr.value;
                            if (c.isEmpty())
                                state &= ~bitmap;
                        }
                        break;
                }
            if (context == null)
                context = ExecutionContext.asyncContext(fun, this);
            else
                context = context.funContext(fun.id, fun.defining, null);
            for (int i = 0; i < members.length; i++)
                if (args[i] != null)
                    context.push(args[i]);
            return context;
        } else
            return null;
    }
}

public class ExecutionContext implements Bytecodes {
    
/** dynamic predecessor
 */
    ExecutionContext parent;

/** static predecessor
 */
    StateSpace space;

/** program counter
 */
    int pc = 0;

/** the bytecode
 */
    byte[] code;

/** stack pointer
 */
    int sp = 0;

/** local execution stack
 */
    JValue[] stack = new JValue[4];
    
/** used for representing lists of execution contexts
 */
    ExecutionContext next;
    ExecutionContext prev;
    
    int id;
    static int ids;
    
    protected ExecutionContext(ExecutionContext parent, StateSpace space, byte[] code) {
        this.parent = parent;
        this.space = space;
        this.code = code;
        id = ids++;
    }

    void fork() {
        ExecutionContext exec = new ExecutionContext(parent, space, code);
        exec.pc = pc;
        space.prog.putProcess(exec);
    }
    
/** bytecode interpretation
 */
    public static void exec(ExecutionContext current) {
        //System.out.println("entering context (0) " + current.id);// DEBUG
        while (true) {
            //Disassembler.disassemble(current.code, current.pc);//DEBUG
            switch (current.code[current.pc++]) {
                case FORK:
                    int newpc = current.get2b();
                    current.fork();
                    current.pc = newpc;
                    break;
                case RET:
                    if (current.parent == null)
                        return;
                    JValue v = current.pop();
                    //System.out.println("<-- " + v);//DEBUG
                    current.parent.push(v);
                    //System.out.println("leaving context " + current.id);//DEBUG
                    current = current.parent;
                    //System.out.println("entering context (1) " + current.id);//DEBUG
                    break;
                case STOP:
                    if (current.parent == null)
                        return;
                    current = current.parent;
                    //System.out.println("entering context (2) " + current.id);//DEBUG
                    break;
                case UNIT:
                    current.push(JValue.Unit);
                    break;
                case BYTE:
                    current.push(JValue.Integer(current.code[current.pc++]));
                    break;
                case INT:
                    current.push(JValue.Integer(current.get4b()));
                    break;
                case TRUE:
                    current.push(JValue.Boolean(true));
                    break;
                case FALSE:
                    current.push(JValue.Boolean(false));
                    break;
                case TUPEL:
                    JValue[] vs = new JValue[current.code[current.pc++]];
                    System.arraycopy(current.stack, current.sp - vs.length, vs, 0, vs.length);
                    current.sp -= vs.length;
                    current.push(JValue.Tupel(vs));
                    break;
                case DECOMP:
                    JValue.Tupel t = (JValue.Tupel)current.pop();
                    System.arraycopy(t.values, 0, current.stack, current.sp, t.values.length);
                    current.sp += t.values.length;
                    break;
                case LOAD:
                    current.push(current.space.getMember(
                                    current.code[current.pc++],
                                    current.code[current.pc++]));
                    break;
                case FUN:
                    current.push(JValue.Function(current.get2b(), current.space));
                    break;
                case TOKEN:
                    current.push(JValue.Token(current.code[current.pc++],
                                              new ArgumentQueue(),
                                              current.space));
                    break;
                case DUP:
                    current.push(current.stack[current.sp - 1]);
                    break;
                case POP:
                    current.pop();
                    break;
                case STORE:
                    current.space.setMember(current.code[current.pc++], current.pop());
                    break;
                case ENTER: // enter new state space
                    int x0;
                    current.space = current.space.prog.getStateSpace(x0 = current.get2b(), current.space);
                    //System.out.println("entering space " + x0);//DEBUG
                    break;
                case LEAVE: // leave current state space
                    current.space = current.space.encl;
                    //System.out.println("leaving space");//DEBUG
                    break;
                case GOTO:
                    current.pc = current.get2b();
                    break;
                case JMPT:
                    int newpc = current.get2b();
                    if (current.pop().boolValue())
                        current.pc = newpc;
                    break;
                case JMPF:
                    int newpc = current.get2b();
                    if (!current.pop().boolValue())
                        current.pc = newpc;
                    break;
                case APPLY:
                    JValue arg = current.pop();
                    JValue fun = current.pop();
                    //System.out.println("--> " + fun + "(" + arg + ")");//DEBUG
                    switch (fun) {
                        case Function(int id, StateSpace defining):
                            current = current.funContext(id, defining, arg);
                            //System.out.println("entering context (3) " + current.id);//DEBUG
                            break;
                        case Token(int tag, ArgumentQueue args, StateSpace def):
                            ExecutionContext context;
                            if ((context = def.yield(tag, args, arg, current)) != null) {
                                current = context;
                                //System.out.println("entering context (4) " + current.id);//DEBUG
                            }
                            else
                                return;
                            break;
                    }
                    break;
                case YIELD:
                    JValue arg = current.pop();
                    JValue fun = current.pop();
                    //System.out.println("==> " + fun + "(" + arg + ")");//DEBUG
                    switch (fun) {
                        case Function(int id, StateSpace defining):
                            current = current.funContext(id, defining, arg);
                            current.parent = null;
                            //System.out.println("entering context (5) " + current.id);//DEBUG
                            break;
                        case Token(int tag, ArgumentQueue args, StateSpace def):
                            ExecutionContext context;
                            if ((context = def.yield(tag, args, arg, null)) != null) {
                                current = context;
                                //System.out.println("entering context (6) " + current.id);//DEBUG
                            }
                            else
                                return;
                            break;
                    }
                    break;
                case ADD:
                    current.push(current.pop().intValue() + current.pop().intValue());
                    break;
                case SUB:
                    int right = current.pop().intValue();
                    current.push(current.pop().intValue() - right);
                    break;
                case MULT:
                    current.push(current.pop().intValue() * current.pop().intValue());
                    break;
                case DIV:
                    int right = current.pop().intValue();
                    current.push(current.pop().intValue() / right);
                    break;
                case EQL:
                    current.push(JValue.Boolean(current.pop().equals(current.pop())));
                    break;
                case NOT:
                    current.push(JValue.Boolean(current.pop().boolValue()));
                    break;
                case ERROR:
                    throw new RuntimeException("user abortion");
                default:
                    throw new InternalError();
            }
        }
    }
    
    public boolean finished() {
        return (pc >= code.length);
    }
    
/** code operations
 */
    public int get2b() {
        int i = code[pc++];
        if (i < 0)
            i += 256;
        int j = code[pc++];
        if (j < 0)
            j += 256;
        return (i << 8) + j;
    }
    
    public int get4b() {
        return (get2b() << 16) + get2b();
    }

/** stack operations
 */
    public void push(JValue v) {
        if (sp == stack.length) {
            JValue[] newstack = new JValue[sp * 2];
            System.arraycopy(stack, 0, newstack, 0, sp);
            stack = newstack;
        }
        stack[sp++] = v;
    }
    
    public void push(int i) {
        if (sp == stack.length) {
            JValue[] newstack = new JValue[sp * 2];
            System.arraycopy(stack, 0, newstack, 0, sp);
            stack = newstack;
        }
        stack[sp++] = JValue.Integer(i);
    }
    
    public JValue top(int rel) {
        return stack[sp - rel - 1];
    }
    
    public JValue pop() {
        return stack[--sp];
    }
    
    public void pop(int args) {
        sp -= args;
    }
    
    public void stash(int adr) {
        push(stack[adr]);
    }
    
/** constructors
 */
    public ExecutionContext funContext(int id, StateSpace defining, JValue arg) {
        ExecutionContext context = new ExecutionContext(
                this,
                space.prog.getStateSpace(
                    space.prog.members.length - space.prog.guards.length + id,
                    (defining == null) ? space : defining),
                space.prog.codes[id]);
        if (arg != null)
            context.push(arg);
        return context;
    }
    
    public static ExecutionContext asyncContext(JValue.Function fun, StateSpace space) {
        return new ExecutionContext(
                null,
                space.prog.getStateSpace(
                    space.prog.members.length - space.prog.guards.length + fun.id,
                    space),
                space.prog.codes[fun.id]);
    }
    
    public static ExecutionContext initialContext(JoinProgram prog, byte[] code) {
        return new ExecutionContext(null,
                                    new StateSpace(prog, 0, null, null, null),
                                    code);
    }
}

public class JoinProgram {
    
/** number of members for all state spaces
 */
    int[] members;
    
/** action table for all state spaces
 */
    JValue[][] actions;
    
/** state table for all state spaces
 */
    byte[][] states;

/** guards for all actions
 */
    int[] guards;

/** left tokens for all actions
 */
    int[] lefttoks;
    
/** bytecode for all functions
 */
    byte[][] codes;

    
/** a queue of state spaces in which reductions are possible
 */
    protected ExecutionContext active;
    protected ExecutionContext lastactive;


/** constructor for a new join program
 */
    public JoinProgram(int statespaces, int functions) {
        members = new int[statespaces + functions];
        actions = new JValue[statespaces + functions][];
        states = new byte[statespaces + functions][];
        guards = new int[functions];
        lefttoks = new int[functions];
        codes = new byte[functions][];
    }

/** define function id
 */
    public void defineFunction(int id, int guard, int lt, int args, byte[] code) {
        this.guards[id] = guard;
        this.lefttoks[id] = lt;
        this.codes[id] = code;
        defineStateSpace(members.length - guards.length + id, args, null, null);
    }

/** define state space id; this method requires all functions defined
 *  by defineFunction
 */
    public void defineStateSpace(int id, int members, int[] actions, byte[] states) {
        this.members[id] = members;
        this.states[id] = states;
        if (actions != null) {
            this.actions[id] = new JValue[actions.length];
            for (int i = 0; i < actions.length; i++)
                this.actions[id][i] = JValue.Function(actions[i], null);
        }
    }
    
/** return a new instance of state space id
 */
    public StateSpace getStateSpace(int id, StateSpace defining) {
        return new StateSpace(this, members[id], actions[id], states[id], defining);
    }

/** remember a state space in which a reduction is possible
 */
    void putProcess(ExecutionContext exec) {
        if ((exec.next == null) && (exec != lastactive)) {
            if (active != null) {
                exec.next = active;
                active.prev = exec;
            } else
                lastactive = exec;
            active = exec;
        }
    }

/** forget state space since a reduction is no longer possible
 */
    void removeProcess(ExecutionContext exec) {
        if (exec == lastactive) {
            if (exec != active) {
                lastactive = exec.prev;
                lastactive.next = null;
            } else
                active = lastactive = null;
            exec.prev = null;
        } else if (exec.prev != null) {
            exec.prev.next = exec.next;
            exec.next.prev = exec.prev;
            exec.prev = exec.next = null;
        } else if (active == exec) {
            active = active.next;
            exec.next = active.prev = null;
        }
    }

/** execute the program
 */
    public JValue execute(byte[] code) {
        long msec = System.currentTimeMillis();
        ExecutionContext initial = ExecutionContext.initialContext(this, code);
        ExecutionContext.exec(initial);
        while (active != null) {
            ExecutionContext exec = lastactive;
            removeProcess(lastactive);
            ExecutionContext.exec(exec);
        }
        if (!initial.finished())
            throw new RuntimeException("execution deadlock at adress " + initial.pc);
        System.out.println("[runtime: " + (System.currentTimeMillis() - msec) + "ms]");
        return initial.pop();
    }

/** give out something
 */
    public void print(Object obj) {
        System.out.println(obj);
    }
}

class Disassembler implements Bytecodes {
    
    static void print(int pc, String line) {
        System.out.println("    " + (pc - 1) + ": " + line);
    }
    
    static int get2b(byte[] code, int pc) {
        int i = code[pc];
        if (i < 0)
            i += 256;
        int j = code[pc + 1];
        if (j < 0)
            j += 256;
        return (i << 8) + j;
    }
    
    static int get4b(byte[] code, int pc) {
        return (get2b(code, pc) << 16) + get2b(code, pc + 2);
    }
    
    static void disassemble(byte[] code, int pc) {
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
}

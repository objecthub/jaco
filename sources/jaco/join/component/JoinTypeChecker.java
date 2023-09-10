package jaco.join.component;

import jaco.framework.*;
import jaco.join.struct.*;
import jaco.java.struct.*;
import java.io.*;


public class JTypes implements JoinConst {
    static JoinPrettyPrinter pretty;
    
    int nextvar = 1;
    
    JType.RecordType makeRecordType() {
        return new JType.RecordType(new JDefinitionSet());
    }
    
    JType.TypeVar makeTypeVar() {
        JType.TypeVar tv = new JType.TypeVar(nextvar++, null);
        tv.cs = new JConstraintSet(tv);
        return tv;
    }
    
    JType.TypeVar dupTypeVar(JType.TypeVar t) {
        JType.TypeVar tv = new JType.TypeVar(nextvar++, null);
        tv.cs = new JConstraintSet(tv);
        if (t.cs.size > 0) {
            tv.cs.cs = new JConstraint[tv.cs.size = t.cs.size];
            for (int i = 0; i < t.cs.size; i++)
                tv.cs.cs[i] = new JConstraint(t.cs.cs[i].tag, t.cs.cs[i].type);
        }
        return tv;
    }
    
    JType instantiate(JType type) {
        switch (type) {
            case ForAll(JType.TypeVar[] vars, JType body):
                JType.TypeVar[] vs = new JType.TypeVar[vars.length];
                for (int i = 0; i < vars.length; i++)
                    body = substitute(vars[i], vs[i] = dupTypeVar(vars[i]), body);
                for (int i = 0; i < vs.length; i++)
                    for (int j = i + 1; j < vs.length; j++)
                        substitute(vars[i], vs[i], vs[j]);
                return body;
            case Mu(JType.TypeVar var, JType body):
                return substitute(var, type, body);
            default:
                return type;
        }
    }
    
    JType expand(JType type) {
        switch (type) {
            case TypeVar(_, JConstraintSet cs):
                if (cs != null) {
                    ((JType.TypeVar)type).cs = null;
                    for (int i = 0; i < cs.size; i++) {
                        if (cs.cs[i].tag != JConstraint.BUSY) {
                            int tag = cs.cs[i].tag;
                            cs.cs[i].tag = JConstraint.BUSY;
                            cs.cs[i].type = expand(cs.cs[i].type);
                            cs.cs[i].tag = tag;
                        }
                    }
                    ((JType.TypeVar)type).cs = cs;
                    if (cs.size == 1) {
                        if (occurs(type, cs.cs[0].type)) {
                            JType.TypeVar v = makeTypeVar();
                            v.id = -v.id;
                            return cs.cs[0].type = JType.Mu(v, substitute(type, v, cs.cs[0].type));
                        } else
                            return cs.cs[0].type;
                    } else
                        return type;
                }
                return type;
            case FunctionType(JType argtype, JType restype):
                return JType.FunctionType(expand(argtype),
                                          expand(restype));
            case TupelType(JType[] compts):
                JType[] ts = new JType[compts.length];
                for (int i = 0; i < compts.length; i++)
                    ts[i] = expand(compts[i]);
                return JType.TupelType(ts);
            case RecordType(JDefinitionSet members):
                JDefinitionSet ms = members.dup();
                for (int i = 0; i < ms.size; i++)
                    ms.ds[i].type = expand(ms.ds[i].type);
                return JType.RecordType(ms);
            case ForAll(JType.TypeVar[] vars, JType body):
                return JType.ForAll(vars, expand(body));
            case Mu(JType.TypeVar v, JType body):
                return JType.Mu(v, expand(body));
            default:
                return type;
        }
    }
    
    JType substitute(JType var, JType by, JType in) {
        if (in == var)
            return by;
        switch (in) {
            case FunctionType(JType argtype, JType restype):
                return JType.FunctionType(substitute(var, by, argtype),
                                          substitute(var, by, restype));
            case TupelType(JType[] compts):
                JType[] ts = new JType[compts.length];
                for (int i = 0; i < compts.length; i++)
                    ts[i] = substitute(var, by, compts[i]);
                return JType.TupelType(ts);
            case RecordType(JDefinitionSet members):
                JDefinitionSet ms = members.dup();
                for (int i = 0; i < ms.size; i++)
                    ms.ds[i].type = substitute(var, by, ms.ds[i].type);
                return JType.RecordType(ms);
            case TypeVar(_, JConstraintSet cs):
                if (cs != null) {
                    ((JType.TypeVar)in).cs = null;
                    for (int i = 0; i < cs.size; i++)
                        cs.cs[i].type = substitute(var, by, cs.cs[i].type);
                    ((JType.TypeVar)in).cs = cs;
                }
                return in;
            case ForAll(JType.TypeVar[] vars, JType body):
                JType.TypeVar[] vs = new JType.TypeVar[vars.length];
                for (int i = 0; i < vars.length; i++) {
                    body = substitute(vars[i], vs[i] = dupTypeVar(vars[i]), body);
                    vs[i].id = -vs[i].id;
                }
                for (int i = 0; i < vs.length; i++) {
                    substitute(var, by, vs[i]);
                    for (int j = i + 1; j < vs.length; j++)
                        substitute(vars[i], vs[i], vs[j]);
                }
                return JType.ForAll(vs, substitute(var, by, body));
            case Mu(JType.TypeVar v, JType body):
                JType.TypeVar tv = dupTypeVar(v);
                tv.id = -tv.id;
                substitute(var, by, tv);
                body = substitute(v, tv, body);
                return JType.Mu(tv, substitute(var, by, body));
            default:
                return in;
        }
    }
    
    boolean occurs(JType var, JType in) {
        if (in == var)
            return true;
        switch (in) {
            case FunctionType(JType argtype, JType restype):
                return occurs(var, argtype) || occurs(var, restype);
            case TupelType(JType[] compts):
                for (int i = 0; i < compts.length; i++)
                    if (occurs(var, compts[i]))
                        return true;
                return false;
            case RecordType(JDefinitionSet ms):
                for (int i = 0; i < ms.size; i++)
                    if (occurs(var, ms.ds[i].type))
                        return true;
                return false;
            case TypeVar(_, JConstraintSet cs):
                if (cs != null) {
                    ((JType.TypeVar)in).cs = null;
                    for (int i = 0; i < cs.size; i++)
                        if (occurs(var, cs.cs[i].type)) {
                            ((JType.TypeVar)in).cs = cs;
                            return true;
                        }
                    ((JType.TypeVar)in).cs = cs;
                }
                return false;
            case ForAll(JType.TypeVar[] vars, JType body):
                for (int i = 0; i < vars.length; i++)
                    if (occurs(var, vars[i]))
                        return true;
                return occurs(var, body);
            case Mu(JType.TypeVar v, JType body):
                return occurs(var, v) || occurs(var, body);
            default:
                return false;
        }
    }
    
    JType unify(JType thisType, JType thatType, TypeEqualities h) {
        if (thisType == thatType)
            return thisType;
        switch (thisType) {
            case PrimitiveType(JType.ERRTYPE):
                return thisType;
            case TypeVar(_, JConstraintSet cs):
                cs.add(JConstraint.EQUALS, thatType);
                return expand(thisType);
            case ForAll(_, _):
                throw new InternalError();
        }
        if (h.find(thisType, thatType))
            return thisType;
        switch (thatType) {
            case PrimitiveType(JType.ERRTYPE):
                return thatType;
            case PrimitiveType(int thatTag):
                switch (thisType) {
                    case PrimitiveType(int thisTag):
                        return (thisTag == thatTag) ? thatType : error(thisType, thatType);
                    default:
                        return error(thisType, thatType);
                }
            case FunctionType(JType thatArg, JType thatRes):
                switch (thisType) {
                    case FunctionType(JType thisArg, JType thisRes):
                        return JType.FunctionType(unify(thisArg, thatArg, h),
                                                  unify(thisRes, thatRes, h));
                    default:
                        return error(thisType, thatType);
                }
            case TupelType(JType[] thatCs):
                switch (thisType) {
                    case TupelType(JType[] thisCs):
                        if (thatCs.length != thisCs.length)
                            return error(thisType, thatType);
                        for (int i = 0; i < thisCs.length; i++)
                            thisCs[i] = unify(thisCs[i], thatCs[i], h);
                        return thisType;
                    default:
                        return error(thisType, thatType);
                }
            case RecordType(JDefinitionSet members):
                throw new InternalError();
            case TypeVar(_, JConstraintSet cs):
                cs.add(JConstraint.EQUALS, thisType);
                return expand(thatType);
            case ForAll(_, _):
                throw new InternalError();
            case Mu(JType.TypeVar var, JType type):
                return unify(instantiate(thatType), thisType, h.add(thisType, thatType));
            default:
                throw new InternalError();
        }
    }
    
    JType unify(JType thisType, JType thatType) {
        return unify(thisType, thatType, new TypeEqualities(this));
    }
    
    boolean equal(JType thisType, JType thatType) {
        if (thisType == thatType)
            return true;
        switch (thisType) {
            case PrimitiveType(JType.ERRTYPE):
                return true;
            case TypeVar(_, JConstraintSet cs):
                return false;
        }
        switch (thatType) {
            case PrimitiveType(JType.ERRTYPE):
                return true;
            case PrimitiveType(int thatTag):
                switch (thisType) {
                    case PrimitiveType(int thisTag):
                        return (thisTag == thatTag);
                    default:
                        return false;
                }
            case FunctionType(JType thatArg, JType thatRes):
                switch (thisType) {
                    case FunctionType(JType thisArg, JType thisRes):
                        return equal(thisArg, thatArg) && equal(thisRes, thatRes);
                    default:
                        return false;
                }
            case TupelType(JType[] thatCs):
                switch (thisType) {
                    case TupelType(JType[] thisCs):
                        if (thatCs.length != thisCs.length)
                            return false;
                        for (int i = 0; i < thisCs.length; i++)
                            if (!equal(thisCs[i], thatCs[i]))
                                return false;
                        return true;
                    default:
                        return false;
                }
            case RecordType(JDefinitionSet members):
                throw new InternalError();
            case TypeVar(_, JConstraintSet cs):
                return false;
            case ForAll(_, _):
                throw new InternalError();
            case Mu(_, JType thatBody):
                switch (thisType) {
                    case Mu(_, JType thisBody):
                        return equal(thisBody, thatBody);
                    default:
                        return false;
                }
            default:
                throw new InternalError();
        }
    }
    
    
    JTree root;
    int n;
    
    JType typeCheck(JTree tree) {
        n = 0;
        return inferType(root = tree, null);
    }
    
    JType inferType(JTree tree, JScope scope) {
        tree.type = iType(tree, scope);
        System.out.print("(" + (++n) + ")  ");
        System.out.flush();
        pretty.print(tree);
        System.out.println(":: " + tree.type);
        return tree.type;
    }
    
    JDefinition funDef(JTree left, JScope scope) {
        switch (left) {
            case Ident(Name name):
                return scope.lookup(name);
            case Apply(JTree fun, _):
                return funDef(fun, scope);
            case Join(JTree l, _):
                return funDef(l, scope);
            default:
                throw new InternalError();
        }
    }
    
    boolean occursOutside(JType var, JScope scope) {
        if (scope == null)
            return false;
        JDefinitionSet ds = scope.defs;
        for (int i = 0; i < ds.size; i++)
            if ((ds.ds[i] != null) && occurs(var, ds.ds[i].type))
                return true;
        return occursOutside(var, scope.outer);
    }
    
    JType.TypeVar[] append(JType.TypeVar[] xs, JType.TypeVar[] ys) {
        if ((xs == null) || (xs.length == 0))
            return ys;
        if ((ys == null) || (ys.length == 0))
            return xs;
        JType.TypeVar[] zs = new JType.TypeVar[xs.length + ys.length];
        System.arraycopy(xs, 0, zs, 0, xs.length);
        System.arraycopy(ys, 0, zs, xs.length, ys.length);
        return zs;
    }
    
    JType.TypeVar[] getFreeVars(JType type, JScope scope) {
        switch (type) {
            case FunctionType(JType arg, JType res):
                return append(getFreeVars(arg, scope), getFreeVars(res, scope));
            case TupelType(JType[] cs):
                JType.TypeVar[] vs = null;
                for (int i = 0; i < cs.length; i++)
                    vs = append(vs, getFreeVars(cs[i], scope));
                return vs;
            case RecordType(JDefinitionSet members):
                JType.TypeVar[] vs = null;
                for (int i = 0; i < members.size; i++)
                    vs = append(vs, getFreeVars(members.ds[i].type, scope));
                return vs;
            case TypeVar(int id, JConstraintSet cs):
                if ((id > 0) && !occursOutside(type, scope)) {
                    ((JType.TypeVar)type).id = -id;
                    return new JType.TypeVar[]{(JType.TypeVar)type};
                } else
                    return null;
            case ForAll(_, JType body):
                return getFreeVars(body, scope);
            case Mu(_, JType body):
                return getFreeVars(body, scope);
            default:
                return null;
        }
    }
    
    void toTypeScheme(JTree tree, JScope scope) {
        switch (tree) {
            case Decl(JTree lhs, _, _):
                JDefinition def = funDef(lhs, scope);
                def.type = expand(def.type);
                scope.defs.ds[def.id - 1] = null;
                JType.TypeVar[] vs = getFreeVars(def.type, scope);
                if (vs != null)
                    def.type = JType.ForAll(vs, def.type);
                scope.defs.ds[def.id - 1] = def;
                break;
            case Rec(JTree[] decls, JScope s):
                System.out.println("TOTYPESCHEME1");
                JDefinition[] defs = new JDefinition[decls.length];
                for (int i = 0; i < decls.length; i++) {
                    defs[i] = funDef(((JTree.Decl)decls[i]).lhs, s);
                    System.out.println("EXPAND TYPE " + defs[i].type);
                    defs[i].type = expand(defs[i].type);
                }
                System.out.println("TOTYPESCHEME2");
                for (int i = 0; i < decls.length; i++)
                    s.defs.ds[defs[i].id - 1] = null;
                for (int i = 0; i < decls.length; i++) {
                    JType.TypeVar[] vs = getFreeVars(defs[i].type, s);
                    // todo: sort free vars
                    if (vs != null) {
                        for (int j = 0; j < vs.length; j++) {
                            vs[j].id = -vs[j].id;
                            JType.TypeVar v = dupTypeVar(vs[j]);
                            v.id = -v.id;
                            defs[i].type = substitute(vs[j], v, defs[i].type);
                            vs[j] = v;
                        }
                        defs[i].type = JType.ForAll(vs, defs[i].type);
                    }
                }
                for (int i = 0; i < decls.length; i++)
                    s.defs.ds[defs[i].id - 1] = defs[i];
                System.out.println("TOTYPESCHEME3");
                break;
            default:
                throw new InternalError();
        }
    }
    
    JType iType(JTree tree, JScope scope) {
        switch (tree) {
            case TopLevel(JTree[] terms, _):
                JType type = JType.errorType;
                for (int i = 0; i < terms.length; i++)
                    type = inferType(terms[i], scope);
                return type;
            case Literal(Constant lit):
                switch (lit) {
                    case IntConst(_):
                        return JType.intType;
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
            case SpecialLiteral(int tag):
                switch (tag) {
                    case TRUE:
                    case FALSE:
                        return JType.boolType;
                    case ERROR:
                        return makeTypeVar();
                    default:
                        throw new InternalError();
                }
            case Ident(Name name):
                return expand(instantiate(scope.lookup(name).type));
            case Select(JTree selected, Name selector):
                throw new InternalError();
            case Apply(JTree fun, JTree arg):
                JType res;
                return and(unify(
                            inferType(fun, scope),
                            JType.FunctionType(inferType(arg, scope),
                                               res = makeTypeVar())),
                           expand(res));
            case Oper(int opcode, JTree left, JTree right):
                JType operType = ((opcode == AND) || (opcode == OR)) ?
                                    JType.boolType : JType.intType;
                return and(unify(inferType(left, scope), operType),
                           unify(inferType(right, scope), operType));
            case If(JTree cond, JTree ifpart, JTree elsepart):
                return and(unify(inferType(cond, scope), JType.boolType),
                           unify(inferType(ifpart, scope),
                                 inferType(elsepart, scope)));
            case Let(JTree head, JTree body):
                return and(inferType(head, scope),
                           inferType(body, ((JTree.Decls)head).scope));
            case Join(JTree left, JTree right):
                return and(unify(inferType(right, scope), JType.bottomType),
                           inferType(left, scope));
            case Tupel(JTree[] trees):
                JType[] ts = new JType[trees.length];
                for (int i = 0; i < ts.length; i++)
                    ts[i] = inferType(trees[i], scope);
                return JType.TupelType(ts);
            case Decl(JTree lhs, JTree rhs, JScope s):
                return unify(inferType(lhs, s), inferType(rhs, s));
            case Decls(JTree[] decls, JScope s):
                for (int i = 0; i < decls.length; i++) {
                    inferType(decls[i], s);
                    toTypeScheme(decls[i], s);
                }
                return JType.RecordType(s.defs);
            case Rec(JTree[] decls, JScope s):
                for (int i = 0; i < decls.length; i++)
                    inferType(decls[i], s);
                return JType.RecordType(s.defs);
            default:
                throw new InternalError();
        }
    }
    
    JType error(JType thisType, JType thatType) {
        System.out.println("cannot unify: " + thisType + " == " + thatType);
        return JType.errorType;
    }
    
    JType and(JType thisType, JType thatType) {
        switch (thisType) {
            case PrimitiveType(JType.ERRTYPE):
                return thisType;
            default:
                return thatType;
        }
    }
}

public class JDefinition {
    Name name;
    JType type;
    JScope owner;
    public int id;
    public int tag = UNSEEN; // == -1, iff function or value (not token)
    
    public static final int UNSEEN = 0;
    public static final int SEEN = -1;
    
    static JDefinition errDef = new JDefinition(Name.fromString("<errdef>"),
                                                JType.errorType,
                                                null,
                                                -1);

    JDefinition(Name name, JType type, JScope owner, int id) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.id = id;
        //if (owner != null)
        //    System.out.println("IN SCOPE " + owner.snum + " DEFINE " + name + " :: " + type);//DEBUG
    }
    
    JDefinition uses(JDefinition def) {
        if ((def != null) && (def.owner == owner) && (owner != null)) {
            //System.out.println(toString() + " USES " + def);//DEBUG
            owner.enterUse(this, def);
        }
        return def;
    }
    
    JDefinition usesRefl(JDefinition def) {
        if ((def != null) && (def.owner == owner) && (owner != null)) {
            owner.enterUse(this, def);
            owner.enterUse(def, this);
        }
        return def;
    }
    
    public String toString() {
        return name.toString() + "::" + type;
    }
}

public class JDefinitionSet {
    JDefinition[] ds = new JDefinition[4];
    int size;
    
    JDefinition add(Name name, JType type, JScope owner) {
        JDefinition d = lookup(name, null);
        if (d != null)
            return d;
        if (size == ds.length) {
            JDefinition[] newds = new JDefinition[size * 2];
            System.arraycopy(ds, 0, newds, 0, size);
            ds = newds;
        }
        int i = size++;
        return ds[i] = new JDefinition(name, type, owner, size);
    }
    
    JDefinition lookup(Name name, JDefinition defaultDef) {
        int i = size;
        while (i-- > 0)
            if (ds[i].name == name)
                return ds[i];
        return defaultDef;
    }
    
    JDefinition lookup(Name name) {
        JDefinition jdef = lookup(name, JDefinition.errDef);
        if (jdef == JDefinition.errDef)
            System.out.println("error: '" + name + "' not found in " + this);
        return jdef;
    }
    
    JDefinitionSet dup() {
        JDefinition[] ds = new JDefinition[size];
        for (int i = 0; i < size; i++)
            ds[i] = new JDefinition(this.ds[i].name, this.ds[i].type,
                                    this.ds[i].owner, this.ds[i].id);
        JDefinitionSet d = new JDefinitionSet();
        d.ds = ds;
        d.size = size;
        return d;
    }
    
    public String toString() {
        if (size == 0)
            return "{}";
        else {
            String res = "{";
            for (int i = 0; i < size - 1; i++)
                res += ds[i] + ", ";
            return res + ds[size - 1] + "}";
        }
    }
}

public class JScope {
    JDefinitionSet defs;
    JScope outer;
    int[][] uses;
    
    public int tokens;
    public int id = -1;
    public int snum = num++;
    static int num;
    
    JScope(JDefinitionSet defs, JScope outer) {
        this.defs = defs;
        this.outer = outer;
    }
    
    static JScope newScope(JScope outer) {
        return new JScope(new JDefinitionSet(), outer);
    }
    
    JDefinition enter(Name name) {
        return defs.add(name, JType.jtypes.makeTypeVar(), this);
    }
    
    public JDefinition lookup(Name name) {
        JDefinition def = JDefinition.errDef;
        JScope scope = this;
        while (scope != null) {
            if ((def = scope.defs.lookup(name, def)) != JDefinition.errDef)
                return def;
            scope = scope.outer;
        }
        System.out.println("error: '" + name + "' not found");
        return JDefinition.errDef;
    }
    
   public int distance(Name name) {
        JDefinition def = JDefinition.errDef;
        JScope scope = this;
        int dist = 0;
        while (scope != null) {
            if ((def = scope.defs.lookup(name, def)) != JDefinition.errDef)
                return dist;
            scope = scope.outer;
            dist++;
        }
        return dist;
    }
    
    void enterUse(JDefinition thisDef, JDefinition otherDef) {
        if (uses == null)
            uses = new int[defs.size][defs.size];
        uses[thisDef.id - 1][otherDef.id - 1] = -1;
    }
    
    private int numberGroups(int i, int num) {
        if (uses[i][i] <= 0) {
            for (int j = 0; j < defs.size; j++)
                if ((uses[i][j] != 0) && (uses[j][i] == 0))
                    num = numberGroups(j, num);
            uses[i][i] = ++num;
            for (int j = 0; j < defs.size; j++)
                if ((uses[i][j] != 0) && (uses[j][i] != 0))
                    uses[j][j] = num;
        }
        return num;
    }
    
    int createUsesClosure() {
        if (uses != null) {
            boolean changed = true;
            for (int n = 0; n < defs.size; n++) {
                if (!changed)
                    break;
                changed = false;
                for (int i = 0; i < defs.size; i++)
                    for (int j = 0; j < defs.size; j++)
                        for (int k = 0; k < defs.size; k++)
                            if ((uses[i][j] != 0)  && (uses[j][k] != 0)) {
                                uses[i][k] = -1;
                                changed = true;
                            }
            }
            int recBlocks = 0;
            for (int i = 0; i < defs.size; i++)
                recBlocks = numberGroups(i, recBlocks);
            return recBlocks;
        } else
            return defs.size;
    }
    
    int groupMembers(int groupId) {
        int members = 0;
        groupId++;
        for (int i = 0; i < defs.size; i++)
            if (uses[i][i] == groupId)
                members++;
        return members;
    }
    
    boolean belongsToGroup(int groupId, int i) {
        return (uses[i - 1][i - 1] == (groupId + 1));
    }
    
    public String toString() {
        return "scope " + snum;
    }
    
    void printUses() {
        if (uses == null)
            return;
        for (int i = 0; i < uses.length; i++) {
            for (int j = 0; j < uses[i].length; j++)
                System.out.print(uses[i][j] + " ");
            System.out.println();
        }
        for (int i = 0; i < defs.size; i++)
            System.out.println("  " + defs.ds[i].id + ": " + defs.ds[i]);
        System.out.println("--------");
    }
}

public class JConstraint {
    final static int BUSY = 0;
    final static int EQUALS = 1;
    final static int SUBTYPE = 2;

    int tag;
    JType type;
    
    
    JConstraint(int tag, JType type) {
        this.tag = tag;
        this.type = type;
    }
    
    public String toString() {
        String res;
        switch (tag) {
            case EQUALS:
                res = " = ";
                break;
            case SUBTYPE:
                res = " < ";
                break;
            default:
                res = " ? ";
        }
        return res + type;
    }
}

public class JConstraintSet {
    JType.TypeVar tv;
    JConstraint[] cs = new JConstraint[4];
    public int size;
    
    JConstraintSet(JType.TypeVar tv) {
        this.tv = tv;
    }
    
    void add(int tag, JType type) {
        if (tag == JConstraint.EQUALS) {
            int i = 0;
            while (i < size)
                if (cs[i].tag == JConstraint.EQUALS) {
                    JType old = cs[i].type;
                    System.arraycopy(cs, i + 1, cs, i, size - i - 1);
                    size--;
                    type = JType.jtypes.unify(old, type);
                } else
                    i++;
        }
        if (type != tv) {
            if (size == cs.length) {
                JConstraint[] newcs = new JConstraint[size * 2];
                System.arraycopy(cs, 0, newcs, 0, size);
                cs = newcs;
            }
            cs[size++] = new JConstraint(tag, type);
        }
    }
    
    JConstraintSet dup() {
        JConstraint[] cs = new JConstraint[size];
        for (int i = 0; i < size; i++)
            cs[i] = new JConstraint(this.cs[i].tag, this.cs[i].type);
        JConstraintSet d = new JConstraintSet(tv);
        d.cs = cs;
        d.size = size;
        return d;
    }
    
    public String toString() {
        if (size == 0)
            return "";
        String res = tv.toName() + cs[size - 1];
        for (int i = size - 2; i >= 0; i--)
            res = tv.toName() + cs[i] + "; " + res;
        return res;
    }
}

class JPrinter {
    PrintWriter out = new PrintWriter(System.out, true);
    
    void printType(JType type) {
        out.print(type);
    }
    
    void printConstraints(JType.TypeVar tv) {
        out.print(tv.cs);
    }
}


public class JoinTypeChecker implements JoinConst { 
    final static int LHS = 0x0001;
    final static int RHS = 0x0002;
    final static int DEP = 0x0004;
    
    JoinPrettyPrinter   pretty;
    JTypes              jtypes = JType.jtypes;
    
    JoinTypeChecker(JoinPrettyPrinter pretty) {
        this.pretty = pretty;
        JTypes.pretty = pretty;
    }
    
    JDefinition dependencyAnalysis(JTree tree, int mode, JScope scope, JDefinition base) {
        switch (tree) {
            case TopLevel(JTree[] terms, _):
                for (int i = 0; i < terms.length; i++)
                    dependencyAnalysis(terms[i], mode, scope, base);
                return null;
            case Literal(_):
            case SpecialLiteral(_):
                return null;
            case Ident(Name name):
                if ((mode & DEP) != 0) {
                    if (base != null)
                        return base.uses(scope.lookup(name));
                    else
                        return scope.lookup(name);
                } else if ((mode & LHS) != 0)
                    return scope.enter(name);
                else
                    return null;
            case Select(JTree selected, Name selector):
                return dependencyAnalysis(selected, mode, scope, base);
            case Apply(JTree fun, JTree arg):
                if (((mode & DEP) == 0) && ((mode & LHS) != 0)) {
                    dependencyAnalysis(arg, mode, scope, base);
                    return scope.outer.enter(((JTree.Ident)fun).name);
                } else {
                    dependencyAnalysis(arg, mode, scope, base);
                    return dependencyAnalysis(fun, mode, scope, base);
                }
            case Oper(int opcode, JTree left, JTree right):
                dependencyAnalysis(left, mode, scope, base);
                dependencyAnalysis(right, mode, scope, base);
                return null;
            case If(JTree cond, JTree ifpart, JTree elsepart):
                dependencyAnalysis(cond, mode, scope, base);
                dependencyAnalysis(ifpart, mode, scope, base);
                dependencyAnalysis(elsepart, mode, scope, base);
                return null;
            case Let(JTree head, JTree body):
                dependencyAnalysis(head, mode, scope, base);
                dependencyAnalysis(body, mode, ((JTree.Decls)head).scope, base);
                return null;
            case Join(JTree left, JTree right):
                base = dependencyAnalysis(left, mode, scope, base);
                JDefinition def = dependencyAnalysis(right, mode, scope, base);
                return ((def == null) || ((mode & DEP) == 0)) ? base : def.usesRefl(base);
            case Tupel(JTree[] trees):
                for (int i = 0; i < trees.length; i++)
                    dependencyAnalysis(trees[i], mode, scope, base);
                return null;
            case Decl(JTree left, JTree right, JScope s):
                if ((mode & DEP) == 0) {
                    switch (left) {
                        case Ident(Name name):
                            scope.enter(name);
                            break;
                        default:
                            scope = JScope.newScope(scope);
                            dependencyAnalysis(left, (mode & ~RHS) | LHS, scope, base);
                            break;
                    }
                    ((JTree.Decl)tree).scope = scope;
                    dependencyAnalysis(right, (mode & ~LHS) | RHS, scope, base);
                    return null;
                } else {
                    if (base == null)
                        base = dependencyAnalysis(left, (mode & ~RHS) | LHS, s, null);
                    dependencyAnalysis(right, (mode & ~LHS) | RHS, s, base);
                    return base;
                }
            case Decls(JTree[] decls, JScope s):
                scope = ((mode & DEP) == 0) ?
                            (((JTree.Decls)tree).scope = JScope.newScope(scope)) : s;
                for (int i = 0; i < decls.length; i++)
                    dependencyAnalysis(decls[i], (mode & ~LHS) | RHS, scope, base);
                if ((mode & DEP) == 0) {
                    JDefinition[] defs = new JDefinition[decls.length];
                    for (int i = 0; i < decls.length; i++)
                        defs[i] = dependencyAnalysis(decls[i], DEP | RHS, scope, null);
                    //System.out.println("OLD USES: ");
                    //scope.printUses();
                    int numGroups = scope.createUsesClosure();
                    //System.out.println("NEW USES " + numGroups + ": ");
                    //scope.printUses();
                    if (numGroups < decls.length) {
                        JTree[] newdecls = new JTree[numGroups];
                        for (int i = 0; i < numGroups; i++) {
                            int n = scope.groupMembers(i);
                            System.out.println("  " + i + ": group members = " + n + "; " + newdecls.length);//DEBUG
                            if (n > 1) {
                                JTree[] block = new JTree[n];
                                n = 0;
                                for (int j = 0; j < decls.length; j++)
                                    if (scope.belongsToGroup(i, defs[j].id))
                                        block[n++] = decls[j];
                                JTree[] nblock = new JTree[n];
                                System.arraycopy(block, 0, nblock, 0, n);
                                newdecls[i] = JTree.Rec(nblock, scope);
                            } else {
                                for (int j = 0; j < decls.length; j++)
                                    if (scope.belongsToGroup(i, defs[j].id)) {
                                        newdecls[i] = decls[j];
                                        break;
                                    }
                            }
                        }
                        ((JTree.Decls)tree).decls = newdecls;
                        //System.out.println("NEW: " + newdecls.length);//DEBUG
                        //pretty.print(tree);
                    }
                    scope.uses = null;
                }
                return null;
            case Rec(JTree[] decls, _):
                for (int i = 0; i < decls.length; i++)
                    dependencyAnalysis(decls[i], (mode & ~LHS) | RHS, scope, base);
                return null;
            default:
                throw new InternalError();
        }
    }
    
    JType typeCheck(JTree tree) {
        dependencyAnalysis(tree, RHS, null, null);
        return jtypes.typeCheck(tree);
    }
}

/*

    void typeswitch(JTree tree) {
        switch (tree) {
            case TopLevel(JTree[] terms, _):
                break;
            case Literal(Constant lit):
                break;
            case SpecialLiteral(int tag):
                break;
            case Ident(Name name):
                break;
            case Select(JTree selected, Name selector):
                break;
            case Apply(JTree fun, JTree arg):
                break;
            case Oper(int opcode, JTree left, JTree right):
                break;
            case If(JTree cond, JTree ifpart, JTree elsepart):
                break;
            case Let(JTree head, JTree body):
                break;
            case Join(JTree left, JTree right):
                break;
            case Tupel(JTree[] trees):
                break;
            case Decl(JTree lhs, JTree rhs, JScope s):
                break;
            case Decls(JTree[] decls, JScope s):
                break;
            case Rec(JTree[] decls, JScope s):
                break;
            default:
                
        }
    }
*/

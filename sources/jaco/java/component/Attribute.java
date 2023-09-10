//      /   _ _      JaCo
//  \  //\ / / \     - tree attribution and type checking for Java syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/11/99

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import java.util.*;
import Tree.*;
import Type.*;
import Definition.*;


public class Attribute extends Processor
                       implements DefinitionConst, ModifierConst,
                                  TreeConst, BytecodeTags,
                                  OperatorConst, TypeConst, TreeProcessor
{
/** the context
 */
    protected SemanticContext   context;
    
/** other components
 */
    protected TypeChecker       checks;
    protected NameResolver      namer;
    protected Mangler           mangler;
    protected Accountant        accountant;
    protected EnterClasses      classes;
    protected ImportClasses     imports;
    protected EnterMembers      members;
    
/** language components
 */
    protected Trees             trees;
    protected Modifiers         modifiers;
    protected Operators         operators;
    protected Types             types;
    protected Definitions       definitions;
    protected Constants         constants;
    
    
/** component name
 */
    public String getName()
    {
        return "JavaAttribute";
    }
    
/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context.compilerContext);
        this.context = context;
        checks = context.TypeChecker();
        classes = context.EnterClasses();
        imports = context.ImportClasses();
        members = context.EnterMembers();
        accountant = context.Accountant();
        namer = context.compilerContext.mainContext.NameResolver();
        mangler = context.compilerContext.mainContext.Mangler();
        trees = context.compilerContext.mainContext.Trees();
        modifiers = context.compilerContext.mainContext.Modifiers();
        operators = context.compilerContext.mainContext.Operators();
        types = context.compilerContext.mainContext.Types();
        definitions = context.compilerContext.mainContext.Definitions();
        constants = context.compilerContext.mainContext.Constants();
    }
    
    
/** is statement stat a call to this or super?
 */
    protected boolean isSelfCall(Tree stat)
    {
        switch (stat)
        {
            case Exec(Apply(Self(_, _, _), _)):
                return true;
            
            default:
                return false;
        }
    }

/** warn about deprecated defbol if not in currently compiled file
 */
    protected void warnDeprecated(int pos, Definition def)
    {
        if (!accountant.compiled.contains(def.enclClass().fullname))
            report.deprecation(pos, def + def.location() + " is deprecated");
    }
    
/** If `c' is an inner class:
 *    Substitute enclosing class part to this, super,
 *    or new(...) if it is not present.
 *    Then attribute enclosing class part and return its type prepended
 *    to constructor arguments `argtypes'
 */
    protected Type[] attribConstrCall(Tree tree, Type encltype, ContextEnv env,
                                        Definition c, Type[] argtypes)
    {
        if (definitions.isInnerclass(c))
        {
            if (encltype == null)
            {
                Definition  cowner = c.owner.enclClass();
                Definition  c1 = env.enclClass.def;
                while ((c1.kind == TYP) && !definitions.subclass(c1, cowner))
                    c1 = c1.owner.enclClass();
                if (c1.kind == TYP)
                {
                    Tree encl = trees.at(tree.pos).make(
                                    trees.newdef.Self(trees.ClassName(c1), THIS));
                    encltype = attribExpr(encl, env, VAL, Type.AnyType);
                    setEncl(tree, encl);
                }
                else
                {
                    report.error(tree.pos, "outof.scope", cowner, c);
                    encltype = Type.ErrType;
                }
            }
            argtypes = types.prepend(encltype, argtypes);
        }
        else
        if (encltype != null)
            report.error(tree.pos, "no.innerclass", c);
        return argtypes;
    }

    protected Definition resolveConstructor(int pos, Type ctype, ContextEnv env,
                                            Type[] argtypes)
    {
        boolean prev = env.info.selectSuper;
        env.info.selectSuper = true;
        Definition res = namer.resolveConstructor(pos, ctype, env, argtypes);
        env.info.selectSuper = prev;
        return res;
    }

    protected Type attribNewObj(NewObj tree, ContextEnv env,
                                Type encltype, Type ctype, Type[] argtypes)
    {
        Definition c = ctype.tdef();
        argtypes = attribConstrCall(tree, encltype, env, c, argtypes);
        if ((c.modifiers & (ABSTRACT | INTERFACE)) != 0)
        {
            report.error(tree.pos, "is.abstract", c);
            return Type.ErrType;
        }
        else
        {
            Definition constr = resolveConstructor(tree.pos, ctype, env, argtypes);
            tree.constructor = constr;
            if ((constr.modifiers & DEPRECATED) != 0)
                warnDeprecated(tree.pos, constr);
            return constrType(tree.pos, ctype, argtypes, env, constr.type);
        }
    }

/** set enclosing class of a this, super, or new
 */
    protected void setEncl(Tree tree, Tree encl)
    {
        switch (tree)
        {
            case Self(_, _, _):
                ((Self)tree).encl = encl;
                break;
            
            case NewObj(_, _, _, _, _):
                ((NewObj)tree).encl = encl;
        }
    }

/** make a shadow defininition for a variable used in more than one case
 */
    protected Tree makeShadowDef(int pos, VarDef v)
    {
        return trees.at(pos).make(
                    trees.newdef.VarDecl(v.name, 0, trees.toTree(v.type), null)
                    .setDef(v));
    }

/** evaluate a final variable's initializer, if not yet done
 *   and set variable's type
 */
    protected void evalInit(VarDef v)
    {
        ContextEnv  env = v.initializer;
        boolean     prevsilent = report.setSilent(true);
        v.initializer = null;
        Type        itype = attribExpr(((VarDecl)env.tree).init, env, VAL, v.type);
        if (itype.isTypeOfConstant())
            v.type = types.coerce(itype, v.type);
        report.setSilent(prevsilent);
    }
    
/** check that variable is initialized and evaluate the variable's
 *  initializer, if not yet done
 */
    protected void checkInit(int pos, ContextEnv env, VarDef v)
    {
        if ((v.pos == -1) &&
            ((v.owner.kind == FUN) ||
                ((v.owner.kind == TYP) && (v.owner == env.info.scope.owner) &&
                (((v.modifiers & STATIC) != 0) == env.info.isStatic))))
            report.error(pos, "illegal.ref", v.name);
        if (v.initializer != null)
            evalInit(v);
    }

/** enter & attribute variable
 */
    public VarDef enterVar(VarDecl tree, ContextEnv env)
    {
        VarDef v = members.enterVar(tree, env);
        attribDecl(tree, env);
        return v;
    }

/** return first abstract object in class c that is not defined in impl,
/** find & check legal targets of jumps, returns, throws
 */
    protected Tree findJumpTarget(int pos, ContextEnv env, Name label, boolean loopOnly)
    {
        Loop: while (env != null)
        {
            switch (env.tree)
            {
                case Labelled(Name label1, Tree body):
                    if (label == label1)
                    {
                        if (loopOnly)
                        {
                            switch (body)
                            {
                                case Loop(_, _, _):
                                case ForLoop(_,_,_,_):
                                    break;
                                
                                default:
                                    report.error(env.tree.pos, "not.looplabel", label);
                            }
                        }
                        return body;
                    }
                    break;
                
                case Loop(_, _, _):
                case ForLoop(_,_,_,_):
                    if (label == null)
                        return env.tree;
                    break;
                
                case Switch(_,_):
                    if (label == null && !loopOnly)
                        return env.tree;
                    break;

                default:
            }
            env = (ContextEnv)env.next;
        }
        if (label != null)
            report.error(pos, "undef.label", label);
        else
        if (loopOnly)
            report.error(pos, "outside.of", "continue", "loop");
        else
            report.error(pos, "outside.of", "break", "switch/loop");
        return null;
    }

    protected Tree findReturnTarget(int pos, Env env)
    {
        while (env != null)
        {
            switch (env.tree)
            {
                case MethodDecl(_, _, _, _, _, _, _):
                    return env.tree;
            }
            env = (ContextEnv)env.next;
        }
        report.error(pos, "outside.of.method", "return");
        return null;
    }
    
    protected int firstCatchPos(int defpos, ContextEnv env)
    {
        int pos = 0;
        while (env != null && env.tree.pos > defpos)
        {
            switch (env.tree)
            {
                case Loop(_, _, _):
                case ForLoop(_, _, _, _):
                    pos = env.tree.pos;
                    break;
            }
            env = (ContextEnv)env.next;
        }
        return pos;
    }
    
/** return type on which to switch, where 'seltype' is type of selector
 *  expression; return Type.ErrType if 'seltype' is illegal; return
 *  selector proto-type, if seltype == null
 */
    protected Type selectorType(int pos, Type seltype)
    {
        if (seltype == null)
            return types.intType;
        switch (seltype.deref())
        {
            case NumType(int tag):
                if (tag <= INT)
                    return types.intType;
                break;
            
            case ErrType:
                return Type.ErrType;
        }
        report.error(pos, "switch.selector", seltype);
        return Type.ErrType;
    }
    
    protected boolean isDuplicateCase(Type pattype, Hashtable tags)
    {
        return pattype.isTypeOfConstant() &&
                (tags.put(pattype.tconst(), pattype) != null);
    }
    
/** return the result type of a `new ctype(argtypes)' constructor expression
 *  where constrtype is the type of the constructor function. This requires
 *  work since a constructor's return type as recorded in the definition table
 *  is always `void'.
 */
    protected Type constrType(int pos, Type ctype, Type[] argtypes,
                        ContextEnv env, Type constrtype)
    {
        switch (constrtype)
        {
            case MethodType(Type[] formals, Type restype, Type[] thrown):
                checks.checkHandled(pos, thrown, env.info.reported);
                env.info.thrown = types.append(thrown, env.info.thrown);
                if (!types.subtypes(argtypes, formals))
                    throw new InternalError();
                else
                    return ctype;
            
            case ErrType:
                    return Type.ErrType;
            
            default:
                throw new InternalError("constrtype " + constrtype);
        }
    }
    
/** enter variable defined in a previous case block
 */
    protected void enterShadow(int pos, Scope scope, Definition def)
    {
        if (scope.lookup(def.name).scope != scope)
        {
                Definition  v = definitions.make.VarDef(ABSTRACT,
                                                    def.name, def.type, def.owner);
                ((VarDef)v).pos = pos;
                scope.enter(v);
        }
    }

    public void attribDecl(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        switch (tree)
        {
            case PackageDecl(_):
                break;
            
            case Import(_, _):
                break;
            
            case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                            Tree[] defs, ClassDef c):
                namer.fixupScope(tree.pos, c);
                if ((c.modifiers & (ABSTRACT | INTERFACE)) == 0)
                    checks.checkAllDefined(tree.pos, c);
                tree.type = c.type;
                ContextEnv  localEnv = accountant.classEnv((ClassDecl)tree, env);
                if ((c.modifiers & INTERFACE) == 0)
                {
                    localEnv.info.scope.enter(definitions.make.VarDef(FINAL,
                                                    PredefConst.THIS_N, c.type, c));
                    if (c.supertype() != null)
                    {
                        localEnv.info.scope.enter(
                            definitions.make.VarDef(FINAL, PredefConst.SUPER_N,
                                                    c.supertype(), c));
                    }
                    checks.checkImplementations(tree.pos, c);
                }
                for (int i = 0; i < defs.length; i++)
                    attribDecl(defs[i], localEnv);
                if (c.owner.kind != PCK)
                    report.info.nestedClasses = true;
                break;
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef f):
                ClassDef    owner = env.enclClass.def;
                checks.checkOverride(tree.pos, f);
                tree.type = f.type;
                
                ContextEnv localEnv = accountant.methodEnv((MethodDecl)tree, env);
                localEnv.info.reported = f.type.thrown();
                checks.checkBound(thrown, types.throwableType);
                
                for (int i = 0; i < params.length; i++)
                    enterVar(params[i], localEnv);

                if (stats == null)
                {
                    if ((owner.modifiers & INTERFACE) == 0 &&
                        ((mods & (ABSTRACT | NATIVE)) == 0))
                            report.error(tree.pos, "not.abstract");
                }
                else
                if ((owner.modifiers & INTERFACE) != 0)
                    report.error(tree.pos, "has.body", "interface");
                else
                if ((mods & ABSTRACT) != 0)
                    report.error(tree.pos, "has.body", "abstract");
                else
                if ((mods & NATIVE) != 0)
                    report.error(tree.pos, "has.body", "native");
                else
                {
                    if ((f.name == PredefConst.INIT_N) &&
                        (owner.type != types.objectType))
                    {
                        if ((stats.length == 0) || !isSelfCall(stats[0]))
                        {
                            ((MethodDecl)tree).stats = stats =
                                trees.append(trees.at(tree.pos).make(
                                    trees.SuperCall(false, new VarDecl[0])), stats);
                        }
                        localEnv.info.isSelfCall = true;
                    }
                    Type    resulttype = f.type.restype();
                    attribStats(stats, localEnv, resulttype, resulttype);
                }
                localEnv.info.scope.leave();
                break;
            
            case VarDecl(Name name, int mods, Tree vartype, Tree init, VarDef v):
                
                if (init != null)
                {
                    ContextEnv  initEnv = accountant.initEnv(env, (VarDecl)tree);
                    Type    itype = attribExpr(init, initEnv, VAL, v.type);
                    if (v.initializer != null)
                        v.type = types.coerce(itype, v.type);
                    v.initializer = null;
                    env.info.thrown = types.append(initEnv.info.thrown, env.info.thrown);
                }
                v.pos = tree.pos;
                tree.type = v.type;
                break;

            case Block(int mods, Tree[] stats):
                ContextEnv  localEnv = accountant.blockEnv(tree, env, mods);
                Scope       locals = env.enclClass.def.locals();
                Definition  e = locals.lookup(PredefConst.INIT_N);
                if (e.scope != null)
                {
                    localEnv.info.reported = e.def.type.thrown();
                    if (e.shadowed != null) {
                        e = e.next();
                        while (e.scope == locals)
                        {
                            localEnv.info.reported = types.intersect(
                                    localEnv.info.reported, e.def.type.thrown());
                            e = e.next();
                        }
                    }
                }
                attribStats(stats, localEnv, Type.VoidType, Type.VoidType);
                localEnv.info.scope.leave();
                break;

            case Bad():
                break;
            
            default:
                throw new InternalError();
        }
        trees.popPos();
    }
    
    public Type attribStat(Tree tree, ContextEnv env, Type pt, Type sofar)
    {
        trees.pushPos(tree.pos);
        tree.type = sofar;
        switch (tree)
        {
            case ClassDecl(_, _, _, _, _, _):
                context.EnterClasses().classEnter(tree, env);
                context.EnterMembers().memberEnter(tree, env);
                attribDecl(tree, env);
                break;
            
            case VarDecl(Name name, _, Tree vartype, _, _):
                attribNonVoidType(vartype, env);
                enterVar((VarDecl)tree, env);
                break;
            
            case Block(int mods, Tree[] stats):
                ContextEnv  localEnv = env.dup(tree);
                sofar = attribStats(stats, localEnv, pt, sofar);
                env.info.thrown = types.append(localEnv.info.thrown, env.info.thrown);
                localEnv.info.scope.leave();
                break;
            
            case Loop(int tag, Tree cond, Tree body):
                if ((tag == WHILE) && (body != null))
                    sofar = attribStat(body, env.dup(tree, env.info), pt, sofar);
                attribExpr(cond, env, VAL, types.booleanType);
                if ((tag == DO) && (body != null))
                    sofar = attribStat(body, env.dup(tree, env.info), pt, sofar);
                break;
            
            case ForLoop(Tree[] init, Tree cond, Tree[] step, Tree body):
                ContextEnv  loopEnv = env.dup(env.tree);
                sofar = attribStats(init, loopEnv, pt, sofar);
                if (cond != null)
                    attribExpr(cond, loopEnv, VAL, types.booleanType);
                loopEnv.tree = tree;
                sofar = attribStats(step, loopEnv, pt, sofar);
                if (body != null)
                    sofar = attribStat(body, loopEnv, pt, sofar);
                env.info.thrown = types.append(loopEnv.info.thrown, env.info.thrown);
                loopEnv.info.scope.leave();
                break;
            
            case Labelled(Name label, Tree body):
                sofar = attribStat(body, env.dup(tree, env.info), pt, sofar);
                break;
            
            case Switch(Tree selector, Case[] cases):
                Type    selpt = selectorType(selector.pos, null);
                Type    seltype = selectorType(selector.pos,
                                            attribExpr(selector, env, VAL, selpt));
                ContextEnv switchEnv = env.dup(tree);

                Hashtable   tags = new Hashtable();
                boolean     hasDefault = false;
                for (int i = 0; i < cases.length; i++)
                {
                    Case        c = cases[i];
                    ContextEnv  caseEnv = switchEnv.dup(c);
                    for (int j = 0; j < c.pat.length; j++)
                    {
                        if (c.pat[j] != null)
                        {
                            Type    pattype = attribPattern(c.pat[j], caseEnv, seltype);
                            if (seltype != Type.ErrType)
                            {
                                if (isDuplicateCase(pattype, tags))
                                    report.error(c.pos, "duplicate.label", "case");
                            }
                        }
                        else
                        if (hasDefault)
                            report.error(c.pos, "duplicate.label", "default");
                        else
                            hasDefault = true;
                    }
                    sofar = attribStats(c.stats, caseEnv, pt, sofar);
                    env.info.thrown = types.append(caseEnv.info.thrown, env.info.thrown);
                    Definition e = caseEnv.info.scope.elems;
                    caseEnv.info.scope.leave();
                    while (e != null)
                    {
                        if (e.def.kind == VAR)
                            enterShadow(tree.pos, switchEnv.info.scope, (VarDef)e.def);
                        e = e.sibling;
                    }
                    e = switchEnv.info.scope.elems;
                    while (e != null)
                    {
                        VarDef v = (VarDef)e.def;
                        e = e.sibling;
                        if (v.adr > 0)
                        {
                            switchEnv.info.scope.remove(v);
                            v.modifiers &= ~ABSTRACT;
                            c.stats = trees.append(makeShadowDef(tree.pos, v), c.stats);
                            enterShadow(tree.pos, switchEnv.info.scope, v);
                        }
                    }
                }
                switchEnv.info.scope.leave();
                break;
            
            case Synchronized(Tree lock, Tree body):
                attribExpr(lock, env, VAL, types.objectType);
                sofar = attribStat(body, env, pt, sofar);
                break;
            
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                for (int i = 0; i < catchers.length; i++)
                {
                    sofar = attribCatch(catchers[i], env, pt, sofar);
                    for (int j = 0; j < i; j++)
                        if ((catchers[i].exception.type != Type.ErrType) &&
                            types.subtype(catchers[i].exception.type,
                                            catchers[j].exception.type))
                            report.error(catchers[i].pos, "not.reached", "catch");
                }
                if (finalizer != null)
                    sofar = attribStat(finalizer, env, pt, sofar);
                ContextEnv  tryEnv = env.dup(tree, env.info, env.info.scope);
                Type[]      catched = new Type[catchers.length];
                for (int i = 0; i < catchers.length; i++)
                    tryEnv.info.reported = types.incl(tryEnv.info.reported,
                                    catched[i] = catchers[i].exception.type);
                sofar = attribStat(body, tryEnv, pt, sofar);
                for (int i = 0; i < catchers.length; i++)
                    if (!checks.isThrown(catchers[i].exception.type, tryEnv.info.thrown))
                        report.error(catchers[i].pos, "catch.not.thrown",
                                     catchers[i].exception.type);
                Type[]      thrown = tryEnv.info.thrown;
                for (int i = 0; i < thrown.length; i++)
                    if (!checks.isHandled(thrown[i], catched))
                        env.info.thrown = types.prepend(thrown[i], env.info.thrown);
                break;
            
            case If(Tree cond, Tree thenpart, Tree elsepart):
                attribExpr(cond, env, VAL, types.booleanType);
                sofar = attribStat(thenpart, env, pt, sofar);
                if (elsepart != null)
                    sofar = attribStat(elsepart, env, pt, sofar);
                break;
            
            case Exec(Tree expr):
                attribExpr(expr, env, VAL, Type.AnyType);
                break;
            
            case Break(Name label, Tree target):
                ((Break)tree).target = findJumpTarget(tree.pos, env, label, false);
                break;
            
            case Continue(Name label, Tree target):
                ((Continue)tree).target = findJumpTarget(tree.pos, env, label, true);
                break;
            
            case Return(Tree expr, Tree target):
                if (pt == Type.VoidType)
                {
                    if (expr != null)
                        report.error(expr.pos, "void.method");
                }
                else
                {
                    if (expr != null)
                    {
                        sofar = checks.join(
                        tree.pos, sofar, attribExpr(expr, env, VAL, pt));
                    }
                    else
                        report.error(tree.pos, "missing.return.val");
                }
                ((Return)tree).target = findReturnTarget(tree.pos, env);
                break;
            
            case Throw(Tree expr):
                checks.checkHandled(tree.pos,
                        attribExpr(expr, env, VAL, types.throwableType),
                        env.info.reported);
                if (types.subtype(expr.type, types.throwableType))
                    env.info.thrown = types.prepend(expr.type, env.info.thrown);
                break;
            
            case Assert(Tree cond, Tree message):
                attribExpr(cond, env, VAL, types.booleanType);
                if (message != null) {
                    attribExpr(message, env, VAL, Type.AnyType);
                    checks.checkNonVoid(message);
                }
                if (env.toplevel.info.classesWithAsserts.get(env.enclClass.def) == null)
                    env.toplevel.info.classesWithAsserts.put(
                        env.enclClass.def, env.enclClass.def.outermostClass());
                break;
                
            case Bad():
                break;
            
            default:
                throw new InternalError("bad statement: " + tree);
        }
        trees.popPos();
        return sofar;
    }

    public Type attribStats(Tree[] stats, ContextEnv env, Type sofar, Type pt)
    {
        for (int i = 0; i < stats.length; i++)
        {
            sofar = attribStat(stats[i], env, sofar, pt);
            env.info.isSelfCall = false;
        }
        return sofar;
    }

/** is vardefbol `v' "frozen", i.e. in a scope where it may not be assigned to
 *  if it is final?
 */
    protected boolean frozen(Definition v, ContextEnv env)
    {
        return v.type.isTypeOfConstant() ||
                    (v.owner.kind == TYP &&
                        (v.owner != env.enclClass.def ||
                        ((v.modifiers & STATIC) != 0) && !env.info.isStatic ||
                        (env.enclMethod != null) &&
                        (env.enclMethod.def.name != PredefConst.INIT_N)));
    }

 /** attribute pattern `tree' in environment `env', where `pt' is the
 *  expected type after application of substitution `s'.
 */
    public Type attribPattern(Tree tree, ContextEnv env, Type pt)
    {
        return attribConstExpr(tree, env, pt);
    }

    public Type attribConstExpr(Tree tree, ContextEnv env, Type pt)
    {
        tree.type = attribExpr(tree, env, VAL, pt);
        if ((pt != Type.ErrType) && !tree.type.isTypeOfConstant())
              report.error(tree.pos, "const.expr.required");
        return tree.type;
    }

    public Type attribCatch(Catch tree, ContextEnv env, Type pt, Type sofar)
    {
        ContextEnv  localEnv = env.dup(tree);
        attribNonVoidType(tree.exception.vartype, localEnv);
        checks.checkBound(tree.exception.vartype, types.throwableType);
        enterVar(tree.exception, localEnv);
        sofar = attribStat(tree.body, localEnv, pt, sofar);
        localEnv.info.scope.leave();
        return sofar;
    }   

    public Type attribExpr(Tree tree, ContextEnv env, int kind, Type pt)
    {
        int     ownkind = VAL;
        Type    owntype = Type.ErrType;
        Type    treetype = null;
        
        trees.pushPos(tree.pos);
        switch (tree)
        {
            case If(Tree cond, Tree thenpart, Tree elsepart):
                attribExpr(cond, env, VAL, types.booleanType);
                attribExpr(thenpart, env, VAL, pt);
                attribExpr(elsepart, env, VAL, pt);
                if (cond.type.isTypeOfConstant() &&
                    thenpart.type.isTypeOfConstant() &&
                    elsepart.type.isTypeOfConstant())
                {
                    if (cond.type.tconst().intValue() == 0)
                        owntype = elsepart.type;
                    else
                        owntype = thenpart.type;
                }
                else
                if (thenpart.type.isTypeOfConstant() &&
                    !elsepart.type.isTypeOfConstant() &&
                            types.assignable(thenpart.type, elsepart.type))
                            owntype = elsepart.type;
                else
                if (elsepart.type.isTypeOfConstant() &&
                    !thenpart.type.isTypeOfConstant() &&
                            types.assignable(elsepart.type, thenpart.type))
                            owntype = thenpart.type;
                else
                    owntype = checks.join(elsepart.pos, thenpart.type, elsepart.type);
                break;

            case Aggregate(Tree[] elems, Tree arrtype):
                Type elempt;
                owntype = pt;
                if (arrtype != null)
                {
                    switch (arrtype)
                    {
                        case ArrayTypeTerm(Tree elemtype):
                            owntype = arrtype.type =
                                types.make.ArrayType(attribType(elemtype, env));
                    }
                }
                switch (owntype.deref())
                {
                    case ArrayType(Type etype):
                        elempt = etype;
                        break;
                    
                    case ErrType:
                        elempt = Type.ErrType;
                        break;
                        
                    default:
                        report.error(tree.pos, "found.wrong", owntype.deref(),
                                     "array initializer");
                        elempt = Type.ErrType;
                        break;
                }
                for (int i = 0; i < elems.length; i++)
                    attribExpr(elems[i], env, VAL, elempt);
                break;
            
            case Apply(Tree fn, Tree[] args):
                ContextEnv argEnv = env;
                if (env.info.isSelfCall)
                {
                    argEnv = env.dup(env.tree, env.info, env.info.scope);
                    argEnv.info.isSelfArgs = true;
                }
                Type[]  argtypes = attribArgs(args, argEnv, Type.AnyType);
                Type    ftype = attribExpr(fn, env, FUN,
                                    types.make.MethodType(argtypes, null, null));
                switch (ftype.deref())
                {
                    case MethodType(Type[] formals, Type restype, Type[] thrown):
                        switch (fn)
                        {
                            case Self(Tree clazz, _, _):
                                if (clazz != null)
                                    argtypes = types.prepend(clazz.type, argtypes);
                        }
                        if (types.subtypes(argtypes, formals))
                        {
                            checks.checkHandled(tree.pos, thrown, env.info.reported);
                            env.info.thrown = types.append(thrown, env.info.thrown);
                            owntype = restype;
                        }
                        else
                            checks.typeError(tree.pos, "incompatible method arguments",
                                                argtypes, formals);
                        break;
                    
                    case ErrType:
                        break;
                    
                    default:
                        report.error(fn.pos, "method.expected", ftype);
                }
                break;
            
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def, _):
                Type    encltype = (encl == null) ? null :
                                        attribExpr(encl, env, VAL, Type.AnyType);
                if (((def == null) || (((ClassDecl)def).name == null)) &&
                    (encltype != null) && (encltype != Type.ErrType))
                {
                    /* Ident cid = (Ident)clazz;
                    ((NewObj)tree).clazz = clazz = trees.at(tree.pos).make(
                            trees.newdef.Select(trees.toTree(encltype),
                                mangler.unmangleShort(cid.name, trees.fullName(cid)))); */
                    if (def != null)
                        ((NewObj)tree).args = args = trees.append(encl, args);
                }
                Type    ctype = attribType(clazz, env);
                Type[]  argtypes = attribArgs(args, env, Type.AnyType);
                switch (ctype)
                {
                    case ClassType(_):
                        if (def != null)
                        {
                            ClassDecl cdef = (ClassDecl)def;
                            if (cdef.name == null)
                            {
                                if ((ctype.tdef().modifiers & INTERFACE) != 0)
                                {
                                    cdef.implementing = new Tree[]{clazz};
                                    if (args.length != 0)
                                    {
                                        report.error(tree.pos, "anon.class.args");
                                        argtypes = types.noTypes;
                                    }   
                                }
                                else
                                    cdef.extending = clazz;
                                cdef.name = classes.formAnonClassName();
                                cdef.members = trees.append(cdef.members,
                                    trees.at(tree.pos).make(trees.
                                        DefaultConstructor(0, encltype != null, argtypes)));
                            }
                            attribStat(cdef, env, pt, pt);
                            owntype = attribNewObj((NewObj)tree, env,
                                                    null, cdef.def.type, argtypes);
                        }
                        else
                            owntype = attribNewObj((NewObj)tree, env,
                                                    encltype, ctype, argtypes);
                        if ((encl != null) || (def != null))
                            report.info.nestedClasses = true;
                        break;
                        
                    case ErrType:
                        break;
                        
                    default:
                        report.error(tree.pos, "reftype.for.new");
                }
                break;
                
            case NewArray(Tree elemtype, Tree[] dims):
                attribArgs(dims, env, types.intType);
                owntype = attribType(elemtype, env);
                for (int i = 0; i < dims.length; i++)
                    owntype = types.make.ArrayType(owntype);
                break;
                
            case Assign(Tree lhs, Tree rhs):
                owntype = attribExpr(lhs, env, VAR, pt);
                report.assert(owntype);
                attribExpr(rhs, env, VAL, owntype);
                break;
            
            case Assignop(int opcode, Tree lhs, Tree rhs, _):
                Type[]  argtypes = {attribExpr(lhs, env, VAR, Type.AnyType),
                                    attribExpr(rhs, env, VAL, Type.AnyType)};
                Definition  ldef = lhs.def();
                if ((ldef != null) && (ldef.kind == VAR) && (ldef.owner.kind == FUN))
                    ldef.modifiers |= USED;
                Definition  operator = namer.resolveOperator(
                                                tree.pos, opcode, env, argtypes);
                ((Assignop)tree).operator = operator;
                owntype = argtypes[0];
                if (operator.kind == FUN)
                {
                    if ((owntype.tag() >= MIN_BASICTYPE_TAG) &&
                        (owntype.tag() <= DOUBLE))
                        checks.checkCastable(rhs.pos, operator.type.restype(), owntype);
                    else
                        checks.checkType(rhs.pos, operator.type.restype(), owntype);
                }
                break;
            
            case Binop(int opcode, Tree lhs, Tree rhs, _):
                Type[]  argtypes = attribArgs(new Tree[]{lhs, rhs}, env, Type.AnyType);
                Definition operator = namer.resolveOperator(
                                    tree.pos, opcode, env, argtypes);
                ((Binop)tree).operator = operator;
                if (operator.kind == FUN)
                {
                    owntype = operator.type.restype();
                    int opc = ((OperatorDef)operator).opcode;
                    if (argtypes[0].isTypeOfConstant() &&
                        argtypes[1].isTypeOfConstant())
                            owntype = types.fold(tree.pos, argtypes[0], argtypes[1],
                                            opc, owntype);
                    if ((opc == if_acmpeq) || (opc == if_acmpne))
                    {
                        if (!types.castable(argtypes[0], argtypes[1]) &&
                            !types.castable(argtypes[1], argtypes[0]))
                        {
                            report.error(tree.pos, "inconvertible.types",
                                         argtypes[0], argtypes[1]);
                        }
                    }
                }
                break;
            
            case Unop(int opcode, Tree operand, _):
                Type[] argtypes = {attribExpr(operand, env,
                                        opcode >= PREINC ? VAR : VAL, Type.AnyType)};
                if (opcode >= PREINC)
                {
                    Definition  ldef = operand.def();
                    if ((ldef != null) && (ldef.kind == VAR) && (ldef.owner.kind == FUN))
                        ldef.modifiers |= USED;
                }
                Definition operator = namer.resolveOperator(tree.pos, opcode,
                                                            env, argtypes);
                ((Unop)tree).operator = operator;
                if (operator.kind == FUN)
                {
                    owntype = operator.type.restype();
                    if (argtypes[0].isTypeOfConstant())
                            owntype = types.fold(tree.pos, argtypes[0],
                                        ((OperatorDef)operator).opcode, owntype);
                }
                break;
            
            case Typeop(int opcode, Tree expr, Tree clazz):
                Type etype = attribExpr(expr, env, VAL, Type.AnyType);
                Type ttype = attribNonVoidType(clazz, env);
                Type rtype = checks.checkCastable(expr.pos, etype, ttype);
                if (opcode == TYPETEST)
                {
                    rtype = checks.checkType(clazz.pos, rtype, types.objectType);
                    owntype = types.booleanType;
                }
                else
                if (etype.isTypeOfConstant())
                    owntype = types.coerce(etype, rtype);
                else
                    owntype = rtype;
                break;

            case Index(Tree indexed, Tree index):
                Type atype = attribExpr(indexed, env, VAL, Type.AnyType);
                attribExpr(index, env, VAL, types.intType);
                switch (atype.deref())
                {
                    case ArrayType(Type elemtype):
                        owntype = elemtype;
                        break;
                        
                    case ErrType:
                        break;
                    
                    default:
                        report.error(tree.pos, "found.wrong", "array", atype);
                        break;
                }
                ownkind = VAR;
                break;
            
            case Select(Tree selected, Name selector, _):
                if (selector == PredefConst.CLASS_N)
                {
                    Type    stype = attribType(selected, env);
                    owntype = types.classType;
                    tree.setDef(definitions.make.VarDef(STATIC | PUBLIC,
                                    PredefConst.CLASS_N, types.classType, stype.tdef()));
                }
                else
                {
                    int skind = 0;
                    if ((kind & PCK) != 0)
                        skind |= PCK;
                    if ((kind & TYP) != 0)
                        skind |= TYP | PCK;
                    if ((kind & ~(TYP | PCK)) != 0)
                        skind |= VAL | TYP;
                    // don't move this into expr below:
                    Type        stype = attribExpr(selected, env, skind, Type.AnyType);
                    Definition  sdef = selected.def();
                    env.info.selectSuper = (sdef != null) &&
                                                ((sdef.name == PredefConst.SUPER_N) ||
                                                (sdef.kind == TYP));
                    Definition  def = attribSelect((Select)tree, stype, env, kind, pt);
                    tree.setDef(def);
                    ownkind = def.kind;
                    if (ownkind == VAR)
                    {
                        checkInit(tree.pos, env, (VarDef)def);
                        if ((def.modifiers & FINAL) != 0 &&
                            ((sdef == null) ||
                                (sdef.name != PredefConst.THIS_N) && (sdef.kind != TYP) ||
                            frozen(def, env)))
                            ownkind = VAL;
                    }
                    if (env.info.selectSuper)
                    {
                        if ((def.modifiers & STATIC) == 0)
                        {
                            if (sdef.name == PredefConst.SUPER_N)
                                namer.checkNonAbstract(tree.pos, def);
                            else
                                namer.checkStatic(tree.pos, def);
                            Type stype1 = types.mgb(env.enclClass.def.type, stype.tdef());
                            if (stype1 != null)
                                stype = stype1;
                        }
                            env.info.selectSuper = false;
                    }
                    if ((def.modifiers & DEPRECATED) != 0)
                        warnDeprecated(tree.pos, def);
                    owntype = def.type;
                }
                break;
                
            case Ident(Name idname, _):
                Definition  def = namer.resolveIdent(tree.pos, idname, env, kind, pt);
                tree.setDef(def);
                ownkind = def.kind;
                if (ownkind == VAR)
                {
                    VarDef  v = (VarDef)def;
                    checkInit(tree.pos, env, v);
                    if (((v.modifiers & FINAL) != 0) && frozen(v, env))
                        ownkind = VAL;
                    if ((v.owner.kind != TYP) && (v.owner != env.info.scope.owner) &&
                        ((v.modifiers & CAPTURED) == 0))
                    {
                        v.modifiers |= CAPTURED;
                        if ((v.modifiers & FINAL) == 0)
                            report.error(tree.pos, "access.to.uplevel", v);
                    }
                    if (kind == VAR)
                    {
                        if ((v.modifiers & CAPTURED) == 0)
                            v.adr = tree.pos;
                    }
                    else
                        v.modifiers |= USED;
                }
                if ((def.modifiers & STATIC) == 0)
                {
                    if (namer.existsStatic(def.owner, env))
                        namer.checkStatic(tree.pos, def);
                    else
                    if ((def.owner != null) &&
                        (def.owner.kind == TYP) && env.info.isSelfArgs)
                            report.error(tree.pos, "call.superconstr.first", def);
                }
                if ((def.modifiers & DEPRECATED) != 0)
                    warnDeprecated(tree.pos, def);
                owntype = def.type;
                break;
                
            case Self(Tree clazz, int tag, _):
                Definition def;
                Name name = (tag == THIS) ? PredefConst.THIS_N : PredefConst.SUPER_N;
                if (env.info.isSelfArgs)
                {
                    report.error(tree.pos, "call.superconstr.first", name);
                    def = Scope.errDef;
                }
                else
                {
                    Definition  cc = ((clazz == null) || env.info.isSelfCall) ?
                                            env.enclClass.def :
                                            attribType(clazz, env).tdef();
                    def = namer.resolveSelf(tree.pos, tag, cc, env);
                    if (def.type != Type.ErrType)
                    {
                        if (kind == FUN)
                        {
                            if (env.info.isSelfCall)
                            {
                                env.info.isSelfCall = false;
                                Type    clazztype = (clazz == null) ?
                                                null :
                                                attribExpr(clazz, env, VAL, Type.AnyType);
                                Type[] argtypes = attribConstrCall(
                                                    tree, clazztype, env,
                                                    def.type.tdef(), pt.argtypes());
                                def = resolveConstructor(tree.pos, def.type, env, argtypes);
                                ownkind = FUN;
                                env.info.isSelfCall = true;
                            }
                            else
                                report.error(tree.pos,"must.be.first.stat", name);
                        }
                        if (clazz != null)
                            report.info.nestedClasses = true;
                    }
                    if (namer.existsStatic(def.owner, env))
                            namer.checkStatic(tree.pos, def);
                    owntype = def.type;
                }
                tree.setDef(def);
                break;

            case Literal(Constant value):
                owntype = constants.toType(value);
                break;
            
            case BasicType(int tag):
                owntype = types.typeOfTag[tag];
                ownkind = TYP;
                break;
            
            case ArrayTypeTerm(Tree elemtype):
                Type    etype = attribNonVoidType(elemtype, env);
                owntype = types.make.ArrayType(etype);
                ownkind = TYP;
                break;
            
            case Bad():
                owntype = Type.ErrType;
                ownkind = ANY;
                break;
            
            default:
                throw new InternalError("bad expr");
        }
        if ((owntype != Type.ErrType) &&
            definitions.checkKind(tree.pos, ownkind, kind))
                tree.type = checks.checkType(tree.pos, owntype, pt);
        else
                tree.type = Type.ErrType;
        trees.popPos();
        return tree.type;
    }
    
    protected Definition attribSelect(Select tree, Type stype, ContextEnv env, int kind, Type pt)
    {
        Name        name = tree.selector;
        int         pos = tree.pos;
        Definition  c = stype.tdef();
        Definition  def;
        
        switch (stype.deref())
        {
            case PackageType():
                def = namer.resolveSelectFromPackage(c, name, env, kind);
                if (def.kind >= BAD)
                    def = namer.access(def, pos, definitions.formFullName(name, def));
                    break;
                
                case ArrayType(_):
                case ClassType(_):
                    namer.fixupScope(pos, c);
                    def = namer.resolveSelectFromType(stype, name, env, kind & ~PCK, pt);
                    if (def.kind >= BAD)
                    {
                    if ((kind & TYP) != 0)
                    {
                        Name pname = trees.fullName(tree.selected);
                        if (pname != PredefConst.ERROR_N)
                        {
                            Definition p = namer.loadPackage(env, pname);
                            if (p.kind < BAD)
                            {
                                Definition def1 = namer.resolveSelectFromPackage(
                                                    (PackageDef)p, name, env, kind);
                                if (def1.kind < BAD)
                                    def = def1;
                            }
                        }
                    }
                    def = namer.access(def, pos, stype, name, namer.protoArgs(pt));
                }
                break;
            
            case ErrType:
                def = Scope.errDef;
                break;
                    
            default:
                report.error(pos, "deref", stype);
                def = Scope.errDef;
                break;
        }
        return def;
    }
    
    public Type[] attribArgs(Tree[] args, ContextEnv env, Type pt)
    {
        Type[] argtypes = new Type[args.length];
        for (int i = 0; i < args.length; i++)
        {
            attribExpr(args[i], env, VAL, pt);
            argtypes[i] = checks.checkNonVoid(args[i]);
        }
        return argtypes;
    }

    public Type attribType(Tree tree, ContextEnv env)
    {
        return attribExpr(tree, env, TYP, Type.AnyType);
    }

    public Type attribNonVoidType(Tree tree, ContextEnv env)
    {
        attribType(tree, env);
        return checks.checkNonVoid(tree);
    }
    
    public Type attribClassType(Tree tree, ContextEnv env)
    {
        attribType(tree, env);
        switch (tree.type.deref())
        {
            case ClassType(_):
            case ErrType:
                break;
                
            default:
                report.error(tree.pos, "requires.class.type", tree.type);
                tree.type = Type.ErrType;
        }
        return tree.type;
    }

    public Type attribSignature(Tree[] params, Tree res, Tree[] excs, ContextEnv env)
    {
        Type[] argtypes = new Type[params.length];
        for (int i = 0; i < params.length; i++)
        {
            Tree partype = params[i];
            switch (partype)
            {
                case VarDecl(_, _, Tree vartype, _, _):
                    partype = vartype;
            }
            argtypes[i] = attribNonVoidType(partype, env);
        }
        Type restype;
        if (res == null) // it's a constructor signature
        {
            if (definitions.isInnerclass(env.enclClass.def))
                argtypes = types.prepend(env.outer.enclClass.def.type, argtypes);
            restype = Type.VoidType;
        }
        else
            restype = attribType(res, env);
        Type[]  thrown = new Type[excs.length];
        for (int i = 0; i < excs.length; i++)
            thrown[i] = attribClassType(excs[i], env);
        return types.make.MethodType(argtypes, restype, thrown);
    }


/** return descrition of tree processor
 */
    public String getDescription()
    {
        return "attributing syntax tree";
    }

/** the debugging name
 */ 
    public String getDebugName()
    {
        return "attrib";
    }
    
/** the debug method
 */
    public boolean debug(int debugId, TreeList treelist) throws AbortCompilation
    {
        if (super.debug(debugId, treelist))
            switch (debugId)
            {
                case Debug.ENTER:
                    treelist.process(pretty);
                    return true;
                    
                case Debug.EXIT:
                    report.print("[finished with " + getDescription() + "]");
                    return true;
                
                default:
                    return true;
            }
        return false;
    }
    
/** the tree processor
 */
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        tree.info.nestedClasses = false;
        ContextEnv  localEnv = accountant.ContextEnv(tree);
        for (int i = 0; i < tree.decls.length; i++)
            attribDecl(tree.decls[i], localEnv);
        tree.info.attributed = true;
        return tree;
    }
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return !info.attributed;
    }
}

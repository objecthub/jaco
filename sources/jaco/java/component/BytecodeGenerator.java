//      /   _ _      JaCo
//  \  //\ / / \     - java bytecode generator
//   \//  \\_\_/     
//         \         Matthias Zenger, 26/02/02

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.io.IOException;
import java.util.*;
import Type.*;
import Definition.*;
import Tree.*;
import Item.*;


public class BytecodeGenerator extends Processor
                               implements ModifierConst, TypeConst,
                                          OperatorConst, TreeConst, ItemConst,
                                          BytecodeTags, DefinitionConst
{
/** other components
 */
    protected Types         types;
    protected Definitions   definitions;
    protected Operators     operators;
    protected Constants     constants;
    protected Trees         trees;
    protected Coder         coder;
    protected Items         items;
    protected NameResolver  namer;
    protected ClassReader   reader;
    protected ClassWriter   writer;
    protected Disassembler  disassem;
    
/** this is a map from class definitions to assertionDisabled variables
 */
    protected Map assertionDisabledVars = new HashMap(23);
    protected boolean updatedAssertionType;
    protected Definition dasDef;
    
/** component name
 */
    public String getName()
    {
        return "BytecodeGenerator";
    }
    
/** description of tree processor
 */
    public String getDescription()
    {
        return "generating bytecode";
    }
    
/** component initialization
 */
    public void init(BackendContext context)
    {
        super.init(context.compilerContext);
        types = context.compilerContext.mainContext.Types();
        definitions = context.compilerContext.mainContext.Definitions();
        operators = context.compilerContext.mainContext.Operators();
        constants = context.compilerContext.mainContext.Constants();
        trees = context.compilerContext.mainContext.Trees();
        namer = context.compilerContext.mainContext.NameResolver();
        reader = context.compilerContext.mainContext.ClassReader();
        disassem = context.compilerContext.mainContext.Disassembler();
        writer = context.compilerContext.ClassWriter();
        coder = context.Coder();
        items = context.Items();
    }
    

//////////// generate code for definitions


/** support for assertions
 */
    static final public Name ASSERTIONS_DISABLED = Name.fromString("$assertionsDisabled");
    static final public Name DESIRED_ASSERTION_STATUS = Name.fromString("desiredAssertionStatus");
    
    protected void initAssertions() {
         if (!updatedAssertionType) {
            updatedAssertionType = true;
            if (!"1.4".equals(((JavaSettings)context.settings).targetversion))
                return;
            dasDef = namer.findMethod(null,
                        types.classType,
                        DESIRED_ASSERTION_STATUS, 
                        new Type[0]);
            if (dasDef.kind >= BAD)
                dasDef = null;
            else
                types.updateAssertionType();
        }
    }
    
    protected void genAssertException(Tree message, GenEnv env) {
        initAssertions();
        checkFinalsInit(env.enclMethod);
        coder.emitop2(new_, coder.mkref(types.assertionErrorType));
        coder.emitop(dup);
        if (message == null) {
            items.invoke(items.make.MemberItem(coder.statPos,
                namer.resolveConstructor(coder.statPos, types.assertionErrorType, null,
                    new Type[0]), true, false));
        } else {
            items.load(genExpr(message, message.type));
            toStringOnStack(message.type);
            items.invoke(items.make.MemberItem(coder.statPos,
                namer.resolveConstructor(coder.statPos, types.assertionErrorType, null,
                    new Type[]{types.assertionParameterType}), true, false));
        }
        items.make.StackItem(items.make.typecode(types.assertionErrorType));
        coder.emitop(athrow);
    }
    
    protected Tree makeAssign(int pos, Definition v, Tree init) {
        return trees.at(pos).make(trees.newdef.Exec(
                    trees.newdef.Assign(
                            trees.newdef.Ident(v.name).setDef(v).setType(v.type),
                            init).setType(v.type)).setType(Type.VoidType));
    }
    
    protected Tree[] normalizeDecls(CompilationEnv env, Tree[] defs, ClassDef c) {
        int pos = Position.NOPOS;
        TreeList initCode = new TreeList();
        TreeList clinitCode = new TreeList();
        TreeList fundefs = new TreeList();
        ClassDef top = (ClassDef)env.classesWithAsserts.get(c);
        // add assertion support to clinit code if needed
        if (top != null) {
            initAssertions();
            VarDef assertDis = (VarDef)definitions.make.VarDef(
                PRIVATE | STATIC | FINAL | SYNTHETIC,
                ASSERTIONS_DISABLED,
                types.booleanType,
                c);
            c.locals().enter(assertDis);
            assertionDisabledVars.put(c, assertDis);
            if ((dasDef != null) &&
                "1.4".equals(((JavaSettings)context.settings).targetversion))
                clinitCode.append(makeAssign(pos, assertDis,
                    trees.at(pos).make(
                        trees.newdef.Unop(
                            NOT,
                            trees.newdef.Apply(
                                trees.newdef.Select(
                                    trees.newdef.Select(
                                        trees.newdef.Ident(top.name).at(pos).setType(top.type).setDef(top),
                                        PredefConst.CLASS_N).setType(types.classType),
                                DESIRED_ASSERTION_STATUS).setType(dasDef.type).setDef(dasDef),
                                new Tree[0]).setType(types.booleanType))
                        .setType(types.booleanType).setDef(operators.negOperator))));
            else
                clinitCode.append(makeAssign(pos, assertDis,
                    trees.at(pos).make(
                        trees.newdef.Ident(PredefConst.FALSE_N).at(pos)
                            .setType(constants.falseConst.type).setDef(constants.falseConst))));
        }
        // collect init and clinit code
        for (int i = 0; i < defs.length; i++) {
            Tree def = defs[i];
            if (pos == Position.NOPOS)
                pos = def.pos;
            switch (def) {
                case Block(int bmods, _):
                    (((bmods & STATIC) != 0) ? clinitCode : initCode).append(def);
                    break;
                case MethodDecl(Name name, _, _, _, _, _, MethodDef f):
                    fundefs.append(def);
                    break;
                case VarDecl(_, int vmods, _, Tree init, VarDef v):
                    if (init != null && !v.type.isTypeOfConstant())
                        ((vmods & STATIC) != 0 ? clinitCode : initCode).
                                append(makeAssign(def.pos, v, init));
                    break;
                default:
                    throw new InternalError();
            }
        }
        // deal with init code
        if (initCode.length() != 0) {
            Tree[]  inits = initCode.toArray();
            for (TreeList.Cons  e = fundefs.chain(); e != null; e = e.next())
                normalizeMethod((MethodDecl)e.head, inits);
        }
        // deal with clinit code
        if (clinitCode.length() != 0) {
            int clinitmods = ((c.modifiers & STRICTFP) != 0) ?
                                (STATIC | STRICTFP) : STATIC;
            MethodDef clinit =
                    (MethodDef)definitions.make.MethodDef(
                            clinitmods,
                            PredefConst.CLINIT_N,
                            types.make.MethodType(types.noTypes,
                                                  Type.VoidType,
                                                  types.noTypes), c);
            c.locals().enter(clinit);
            Tree[] clinitStats = clinitCode.toArray();
            Definition  voidDef = Type.VoidType.tdef();
            fundefs.append(trees.at(clinitStats[0].pos).make(
                trees.newdef.MethodDecl(clinit.name, clinit.modifiers,
                        trees.newdef.Ident(voidDef.name).setDef(voidDef).setType(Type.VoidType),
                        new VarDecl[0], trees.noTrees, clinitStats).
                    setDef(clinit).setType(clinit.type)));
        }
        return fundefs.toArray();
    }

    protected void enterFinals(MethodDef f)
    {
        for (Definition e = (f.owner).locals().elems;
                    e != null; e = e.sibling)
            if ((e.def.kind == VAR) &&
                ((e.def.modifiers & FINAL) != 0) &&
                (!e.def.type.isTypeOfConstant()) &&
                ((e.def.modifiers & STATIC) == (f.modifiers & STATIC)))
            {
                VarDef v = (VarDef)e.def;
                coder.newFinal(v);
                coder.letUninit(v.adr);
            }
    }

    protected void checkFinalsInit(int pos, MethodDef f)
    {
        for (Definition e = (f.owner).locals().elems;
                e != null; e = e.sibling)
            if ((e.def.kind == VAR) &&
                ((e.def.modifiers & FINAL) != 0) &&
                (!e.def.type.isTypeOfConstant()) &&
                ((e.def.modifiers & STATIC) == (f.modifiers & STATIC)))
            {
                VarDef v = (VarDef)e.def;
                coder.checkInit(pos, v.adr);
            }
    }

    protected void checkFinalsInit(Tree def)
    {
        switch (def)
        {
            case MethodDecl(Name name, _, _, _, _, _, MethodDef f):
                 if (isInitialConstructor((MethodDecl)def))
                    checkFinalsInit(def.pos, f);
        }
    }
    
    protected void endFinals(MethodDef f)
    {
        for (Definition e = f.owner.locals().elems;
                    e != null; e = e.sibling)
            if ((e.def.kind == VAR) &&
                ((e.def.modifiers & FINAL) != 0) &&
                (!e.def.type.isTypeOfConstant()) &&
                ((e.def.modifiers & STATIC) == (f.modifiers & STATIC)))
            {
                VarDef v = (VarDef)e.def;
                coder.endFinal(v);
            }
    }
    
/** is this statement an initializer for a synthetic variable?
 */
    protected boolean isSyntheticInit(Tree stat)
    {
        switch (stat)
        {
            case Exec(Assign(Select(Self(_, int tag, _), Name name, Definition def), _)):
                return ((def.modifiers & SYNTHETIC) != 0) &&
                        (tag == THIS) &&
                        ((name == PredefConst.THIS0_N) ||
                            (name.startsWith(PredefConst.VALD_N)));
            
            default:
                return false;
        }
    }
    
/** is this method declaration a <clinit> or an <init> without a this(...)?
 */
    protected boolean isInitialConstructor(MethodDecl fd)
    {
        if ((fd.name != PredefConst.INIT_N) && (fd.name != PredefConst.CLINIT_N))
            return false;
        if (fd.stats.length >= 1)
        {
            switch (fd.stats[0])
            {
                case Exec(Apply(Self(_, int tag, _), _)):
                    if (tag == THIS)
                        return false;
            }
        }
        return true;
    }
    
    protected void normalizeMethod(MethodDecl fd, Tree[] initCode)
    {
        if ((fd.name == PredefConst.INIT_N) && isInitialConstructor(fd))
        {
            TreeList    newstats = new TreeList();
            newstats.append(fd.stats[0]);
            int i = 1;
            while ((i < fd.stats.length) && isSyntheticInit(fd.stats[i]))
                newstats.append(fd.stats[i++]);
            newstats.append(initCode);
            while (i < fd.stats.length)
                newstats.append(fd.stats[i++]);
            fd.stats = newstats.toArray();
        }
    }
    
    protected void genTry(GenEnv env, boolean fatcode)
    {
        switch ((Tree)env.enclMethod)
        {
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef f):
                if (stats != null)
                {
                    f.code = coder.newCode(fatcode, f);
                    int thisAdr = -1;
                    if ((mods & STATIC) == 0) {
                        thisAdr = coder.newThis(f);
                        f.code.setStartPc(thisAdr);
                    }
                    for (int i = 0; i < params.length; i++)
                    {
                        coder.newLocal(params[i].def);
                        coder.letInit(params[i].def.adr);
                    }
                    if (isInitialConstructor(env.enclMethod))
                            enterFinals(f);
                    genStats(stats, env);
                    report.assert(coder.stackSize() == 0);
                    if (isInitialConstructor(env.enclMethod))
                    {
                        checkFinalsInit(env.enclMethod.pos, f);
                        endFinals(f);
                    }
                    if (coder.alive)
                    {
                        if (f.type.restype() != Type.VoidType)
                        {
                            report.error(env.enclMethod.pos, "missing return in method");
                            coder.markDead();
                        }
                        else
                        {
                            if (stats.length == 0)
                                coder.statBegin(env.enclMethod.pos);
                            coder.emitop(return_);
                        }
                    }
                    if (thisAdr >= 0)
                        f.code.setEndPc(thisAdr);
                    coder.endScopes(0);
                    if (!fatcode && f.code.fatcode)
                    {
                        boolean prevsilent = report.setSilent(true);
                        genTry(env, true);
                        report.setSilent(prevsilent);
                    }
                }
        }
    }

    protected void genDecl(Tree tree, GenEnv env)
    {
        switch (tree)
        {
            case PackageDecl(_):
            case Import(_, _):
                break;
            
            case ClassDecl(_, _, _, _, Tree[] defs, ClassDef c):
                ((ClassDecl)tree).members = defs = normalizeDecls(env.toplevel.info, defs, c);
                c.pool = coder.newPool();
                GenEnv localEnv = env.dup(tree);
                localEnv.enclClass = (ClassDecl)tree;
                if (debug(4))
                    pretty.printDecl(tree);
                for (int i = 0; i < defs.length; i++)
                    genDecl(defs[i], localEnv);
              	++report.classes;
                break;
                
            case MethodDecl(_, _, _, _, _, _, _):
                GenEnv localEnv = env.dup(tree);
                localEnv.enclMethod = (MethodDecl)tree;
                genTry(localEnv, false);
                ++report.methods;
                break;
                
            default:
                throw new InternalError();
        }
    }

//////////// generate code for statements

    protected void callFinalizer(GenEnv env)
    {
        if (coder.alive)
        {
            coder.pushStack(OBJECT_CODE);
            env.info.cont = new Chain(coder.curPc(), env.info.cont,
                                        coder.stackSize(), coder.uninitSet(),
                                        coder.initSet());
            coder.popStack(OBJECT_CODE);
            coder.emitJump(env.info.cont, jsr);
        }
    }

    protected GenEnv jumpto(Tree target, GenEnv env)
    {
        GenEnv  env1 = env;
        while (env1 != null && env1.tree != target)
            env1 = (GenEnv)env1.next;
        if (env1 != null)
        {
            Tree last = null;
            while (env != env1)
            {
                if (env.tree != last)
                {
                    switch (env.tree)
                    {
                        case Try(Tree body, Catch[] catchers, Tree finalizer):
                            if (finalizer != null)
                                callFinalizer(env);
                            break;
              
                        case Synchronized(Tree lock, Tree body):
                            callFinalizer(env);
                            break;
                    }
                        last = env.tree;
                }
                env = (GenEnv)env.next;
            }
        }
        return env1;
    }

    protected boolean hasFinalizers(Tree target, GenEnv env)
    {
        while (env.tree != target)
        {
            switch (env.tree)
            {
                case Try(Tree body, Catch[] catchers, Tree finalizer):
                    if (finalizer != null)
                        return true;
                    break;

                case Synchronized(Tree lock, Tree body):
                    return true;
            }
            env = (GenEnv)env.next;
        }
        return false;
    }

    protected void genStat(Tree tree, GenEnv env)
    {
        coder.statBegin(tree.pos);
        switch (tree)
        {
            case VarDecl(_, _, _, Tree init, VarDef v):
                // check if this local variable is used
                if ((v.owner.kind == FUN) && ((v.modifiers & USED) == 0))
                {
                    // check if initializer has any side-effects
                    if (trees.hasSideEffects(init))
                        items.drop(genExpr(init, init.type));
                }
                else
                {
                    coder.newLocal(v);
                    coder.letUninit(v.adr);
                    if ((init != null) && !v.type.isTypeOfConstant())
                    {
                        items.load(genExpr(init, v.type));
                        items.store(items.make.LocalItem(tree.pos, v));
                    }
                }
                break;

            case Block(_, Tree[] stats):
                if (coder.alive || (tree.pos != Position.NOPOS))
                {
                    // Position.NOPOS indicates that this is a case block
                    int limit = coder.nextVar();
                    genStats(stats, env);
                    coder.endScopes(limit);
                }
                break;
            
            case Loop(int opcode, Tree cond, Tree body):
                int     startpc = coder.curPc();
                Bits    prevInLoop = coder.enterLoop();
                GenEnv  loopEnv = env.dup(tree, env.info.dup());
                if (opcode == DO)
                {
                    genStat(body, loopEnv);
                    coder.resolve(loopEnv.info.cont);
                    coder.statBegin(cond.pos);
                    CondItem    c = items.mkCond(genExpr(cond, types.booleanType));
                    coder.resolve(items.jump(true, c), startpc);
                    coder.resolve(c.falseJumps);
                }
                else
                if (opcode == WHILE)
                {
                    CondItem    c = items.mkCond(genExpr(cond, types.booleanType));
                    loopEnv.info.exit = items.jump(false, c);
                    if (c.trueJumps != null || (c.opcode != coder.dontgoto))
                    {
                        coder.resolve(c.trueJumps);
                        if (body != null)
                            genStat(body, loopEnv);
                        coder.resolve(loopEnv.info.cont);
                        coder.resolve(coder.branch(goto_), startpc);
                    }
                }
                else
                    throw new InternalError();
                coder.resolve(loopEnv.info.exit);
                coder.exitLoop(prevInLoop);
                break;
            
            case ForLoop(Tree[] init, Tree cond, Tree[] step, Tree body):
                genStats(init, env);
                int     startpc = coder.curPc();
                Bits    prevInLoop = coder.enterLoop();
                CondItem c;
                if (cond != null)
                {
                    coder.statBegin(cond.pos);
                    c = items.mkCond(genExpr(cond, types.booleanType));
                }
                else
                    c = (CondItem)items.make.CondItem(goto_, null, null);
                GenEnv loopEnv = env.dup(tree, env.info.dup());
                loopEnv.info.exit = items.jump(false, c);
                if ((c.trueJumps != null) || (c.opcode != coder.dontgoto))
                {
                    coder.resolve(c.trueJumps);
                    if (body != null)
                        genStat(body, loopEnv);
                    coder.resolve(loopEnv.info.cont);
                    genStats(step, loopEnv);
                    coder.resolve(coder.branch(goto_), startpc);
                }
                coder.resolve(loopEnv.info.exit);
                coder.exitLoop(prevInLoop);
                break;

            case Labelled(Name label, Tree body):
                GenEnv localEnv = env.dup(body, env.info.dup());
                genStat(body, localEnv);
                coder.resolve(localEnv.info.exit);
                break;

            case Switch(Tree selector, Case[] cases):
                Item sel = genExpr(selector, types.intType);
                if (cases.length == 0)
                    items.drop(items.load(sel));
                else
                if ((cases.length == 1) && (cases[0].pat[0] == null))
                {
                    items.drop(sel);
                    genStats(cases[0].stats, env);
                }
                else
                {
                    items.load(sel);
                    genSwitch(cases, env.dup(tree, env.info.dup()));
                }
                break;
            
            case Synchronized(Tree lock, Tree body):
                final Item  lockVar = items.makeTemp(types.objectType);
                items.load(genExpr(lock, lock.type));
                items.store(lockVar);
                items.load(lockVar);
                coder.emitop(monitorenter);

                GenEnv syncEnv = env.dup(tree, env.info.dup());
                genTryBlock(body, null,
                    new FCode()
                    {
                        public void genFinalCode()
                        {
                            items.load(lockVar);
                            coder.emitop(BytecodeTags.monitorexit);
                        }
                    },
                    syncEnv);
                break;
            
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                final GenEnv    outerEnv = env;
                final Tree      finalizerTree = finalizer;
                GenEnv          tryEnv = env.dup(tree, env.info.dup());
                genTryBlock(body, catchers, (finalizer == null) ? null :
                    new FCode()
                    {
                        public void genFinalCode()
                        {
                            genStat(finalizerTree, outerEnv);
                        }
                    },
                    tryEnv);
                    break;
            
            case If(Tree cond, Tree thenpart, Tree elsepart):
                if (cond.type.isTypeOfConstant())
                {
                    if (cond.type.tconst().intValue() != 0)
                        genStat(thenpart, env);
                    else
                    if (elsepart != null)
                        genStat(elsepart, env);
                }
                else
                {
                    Chain       thenExit = null;
                    CondItem    c = items.mkCond(genExpr(cond, types.booleanType));
                    Chain       elseChain = items.jump(false, c);
                    
                    if ((c.trueJumps != null) || (c.opcode != coder.dontgoto))
                    {
                        coder.resolve(c.trueJumps);
                        genStat(thenpart, env);
                        thenExit = coder.branch(goto_);
                    }
                    if ((elsepart != null) && (elseChain != null))
                    {
                        coder.resolve(elseChain);
                        genStat(elsepart, env);
                        coder.resolve(thenExit);
                    }
                    else
                    {
                        coder.resolve(thenExit);
                        coder.resolve(elseChain);
                    }
                }
                break;
            
            case Exec(Tree expr):
                switch (expr)
                {
                    case Unop(int opcode, Tree operand, Definition operator):
                        if (opcode == POSTINC)
                            ((Unop)expr).opcode = PREINC;
                        else
                        if (opcode == POSTDEC)
                            ((Unop)expr).opcode = PREDEC;
                }
                if (trees.hasSideEffects(expr))
                    items.drop(genExpr(expr, expr.type));
                break;
            
            case Break(Name label, Tree target):
                GenEnv  targetEnv = jumpto(target, env);
                if (targetEnv != null)
                    targetEnv.info.addExit(coder.branch(goto_));
                break;
            
            case Continue(Name label, Tree target):
                jumpto(target, env).info.addCont(coder.branch(goto_));
                break;
            
            case Return(Tree expr, Tree target):
                checkFinalsInit(target);
                if (expr != null)
                {
                    Item r = genExpr(expr, tree.type);
                    if (hasFinalizers(target, env))
                    {
                            items.load(r);
                            r = items.makeTemp(tree.type);
                            items.store(r);
                    }
                    jumpto(target, env);
                    items.load(r);
                    coder.emitop(ireturn + coder.truncate(items.make.typecode(tree.type)));
                }
                else
                {
                    jumpto(target, env);
                    coder.emitop(return_);
                }
                break;

            case Throw(Tree expr):
                checkFinalsInit(env.enclMethod);
                items.load(genExpr(expr, expr.type));
                coder.emitop(athrow);
                break;
            
            case Assert(Tree cond, Tree message):
                if (cond.type.isTypeOfConstant())
                {
                    if (cond.type.tconst().intValue() == 0) {
                        Item it = items.make.StaticItem(tree.pos,
                            (Definition)assertionDisabledVars.get(env.enclClass.def.topLevelClass()));
                        //    namer.findField(null, coder.curMethod.owner, ASSERTIONS_DISABLED));
                        CondItem c = (CondItem)items.negate(items.mkCond(it));
                        Chain exit = items.jump(false, c);
                        if ((c.trueJumps != null) || (c.opcode != coder.dontgoto))
                        {
                            coder.resolve(c.trueJumps);
                            genAssertException(message, env);
                        }
                        coder.resolve(exit);
                    }
                }
                else
                {
                    Item it = items.make.StaticItem(tree.pos,
                        (Definition)assertionDisabledVars.get(env.enclClass.def.topLevelClass()));
                    //    namer.findField(null, coder.curMethod.owner, ASSERTIONS_DISABLED));
                    CondItem c = (CondItem)items.negate(items.mkCond(it));
                    if ((c.trueJumps != null) ||
                        (c.opcode != coder.dontgoto))
                    {
                        Chain falseJumps = items.jump(false, c);
                        coder.resolve(c.trueJumps);
                        CondItem rcond = items.mkCond(genExpr(cond, types.booleanType));
                        rcond = (CondItem)items.negate(rcond);
                        c = (CondItem)items.make.CondItem(rcond.opcode,
                            rcond.trueJumps,
                            coder.mergeChains(falseJumps, rcond.falseJumps));
                    }
                    Chain exit = items.jump(false, c);
                    if ((c.trueJumps != null) || (c.opcode != coder.dontgoto))
                    {
                        coder.resolve(c.trueJumps);
                        genAssertException(message, env);
                    }
                    coder.resolve(exit);
                }
                break;
        }
    }
    
    protected void genCatch(Tree tree, GenEnv env, int startpc, int endpc)
    {
        switch (tree)
        {
            case Catch(VarDecl param, Tree body):
                if (startpc != endpc)
                {
                    coder.registerCatch(startpc, endpc, coder.curPc(), coder.mkref(param.type));
                    coder.newLocal(param.def);
                    items.store(items.make.LocalItem(tree.pos, param.def));
                    genStat(body, env);
                }
                break;

            default:
                throw new InternalError();
        }
    }

    protected Tree[] stats(Tree s)
    {
        switch (s)
        {
            case Block(_, Tree[] stats):
                return stats;
            
            default:
                return new Tree[]{s};
        }
    }
    
    protected static interface FCode
    {
        void genFinalCode();
    }
    
    protected void genTryBlock(Tree body, Catch[] catchers, FCode genfinal, GenEnv env)
    {
        int     limit = coder.nextVar();
        Bits        uninits = coder.uninitSet();
        int     startpc = coder.curPc();
        genStat(body, env);
        int     endpc = coder.curPc();
        Bits    initsEnd = coder.initSet();
        Bits    inits = initsEnd;
        if (genfinal != null)
            callFinalizer(env);
        Chain   exitChain = coder.branch(goto_);
        if ((startpc != endpc) && (catchers != null))
        {
            for (int i = 0; i < catchers.length; i++)
            {
                coder.entryPoint(uninits, initsEnd);
                coder.clearStack();
                coder.pushStack(OBJECT_CODE);
                genCatch(catchers[i], env, startpc, endpc);
                inits.orSet(coder.initSet());
                if (genfinal != null)
                    callFinalizer(env);
                exitChain = coder.mergeChains(exitChain, coder.branch(goto_));
            }
        }
        if (genfinal != null)
        {
            coder.entryPoint(uninits, inits);
            coder.registerCatch(startpc, coder.curPc(), coder.curPc(), 0);
            coder.newRegSegment();
    
            coder.clearStack();
            coder.pushStack(OBJECT_CODE);
            Item excVar = items.makeTemp(types.throwableType);
            items.store(excVar);
            callFinalizer(env);
            items.load(excVar);
            coder.emitop(athrow);

            coder.entryPoint(uninits, inits);
            coder.clearStack();
            coder.pushStack(OBJECT_CODE);
            coder.resolve(env.info.cont);
            LocalItem   retVar = (LocalItem)items.makeTemp(types.throwableType);
            items.store(retVar);
            genfinal.genFinalCode();
            Bits uninitsEnd = coder.uninitSet();
            coder.emitop1w(ret, coder.regOf(retVar.adr));
            if (coder.alive)
            {
                coder.markDead();
                coder.resolve(exitChain);
                coder.uninits.andSet(uninitsEnd);
            }
        }
        else
            coder.resolve(exitChain);
        coder.endScopes(limit);
    }

    protected void genSwitch(Case[] cases, GenEnv env)
    {
        int     lo = Integer.MAX_VALUE;
        int     hi = Integer.MIN_VALUE;
        int     ntags = 0;
        
        int[]   labels = null;
        int     defaultIndex = -1;
        int     k = 0;
        
        for (int i = 0; i < cases.length; i++)
            k += cases[i].pat.length;
        int[]   tags = new int[k];
        k = 0;
        for (int i = 0; i < cases.length; i++)
        {
            Case    c = cases[i];
            for (int j = 0; j < c.pat.length; j++)
                if (c.pat[j] != null)
                {
                    int val = c.pat[j].type.tconst().intValue();
                    tags[k++] = val;
                    if (val < lo)
                        lo = val;
                    if (hi < val)
                        hi = val;
                    ntags++;
                }
                else
                {
                    report.assert(defaultIndex == -1);
                    defaultIndex = k++;
                }
        }

        long    table_space_cost = (long)4 + (hi - lo + 1); // words
        long    table_time_cost = 3; // comparisons
        long    lookup_space_cost = (long)3 + 2 * ntags;
        long    lookup_time_cost = ntags;
        int     opcode = (table_space_cost + 3 * table_time_cost) <=
                            (lookup_space_cost + 3 * lookup_time_cost) ?
                            tableswitch : lookupswitch;
        
        if (coder.alive)
        {
            Bits uninits = coder.uninitSet();
            Bits inits = coder.initSet();
            int startpc = coder.curPc();
            coder.emitop(opcode);
            coder.align(4);
            int tableBase = coder.curPc();
            coder.emit4(-1);
            if (opcode == tableswitch)
            {
                coder.emit4(lo);
                coder.emit4(hi);
                for (int i = lo; i <= hi; i++)
                    coder.emit4(-1);
            }
            else
            {
                coder.emit4(ntags);
                for (int i = 0; i < ntags; i++)
                {
                    coder.emit4(-1);
                    coder.emit4(-1);
                }
                labels = new int[tags.length];
            }
            
            coder.markDead();
            k = 0;
            for (int i = 0; i < cases.length; i++)
            {
                Case    c = cases[i];
                for (int j = 0; j < c.pat.length; j++, k++)
                {
                    if (k != defaultIndex)
                    {
                        if (opcode == tableswitch)
                            coder.put4(tableBase + 4 * (tags[k] - lo + 3),
                                        coder.curPc() - startpc);
                        else
                            labels[k] = coder.curPc() - startpc;
                    }
                    else
                        coder.put4(tableBase, coder.curPc() - startpc);         
                }
                coder.entryPoint(uninits, inits);
                genStats(c.stats, env);
                if (false /* Switches.switchCheck */ &&
                    coder.alive &&
                    (c.stats.length != 0) &&
                    (k < (tags.length - 1)))
                    report.warning(c.pos, "possible fall-through from case");
            }
            
            coder.resolve(env.info.exit);
            if (coder.get4(tableBase) == -1)
            {
                coder.put4(tableBase, coder.curPc() - startpc);
                coder.entryPoint(uninits, inits);
            }
            if (opcode == tableswitch)
            {
                int defaultOffset = coder.get4(tableBase);
                for (int i = lo; i <= hi; i++)
                    if (coder.get4(tableBase + 4 * (i - lo + 3)) == -1)
                        coder.put4(tableBase + 4 * (i - lo + 3), defaultOffset);
            }
            else
            {
                if (defaultIndex >= 0)
                    for (int i = defaultIndex; i < tags.length - 1; i++)
                    {
                        tags[i] = tags[i + 1];
                        labels[i] = labels[i + 1];
                    }
                Tools.qsort2(tags, labels, 0, ntags - 1);
                for (int i = 0; i < ntags; i++)
                {
                    int caseidx = tableBase + 8 * (i + 1);
                    coder.put4(caseidx, tags[i]);
                    coder.put4(caseidx + 4, labels[i]);
                }
            }
        }
    }

    protected void genStats(Tree[] stats, GenEnv env)
    {
        for (int i = 0; i < stats.length; i++)
            genStat(stats[i], env);
    }

//////////// generate code for expressions

    public Name valueOfS = Name.fromString("valueOf");
    public Name concatS  = Name.fromString("concat");
    public Name forNameS = Name.fromString("forName");
    public Name TYPES = Name.fromString("TYPE");


    protected Item genClassOf(Type type)
    {
        switch (type.deref())
        {
            case NumType(int tag):
                Name bname = types.boxedName[tag];
                try
                {
                    Definition  c = reader.loadClass(bname);
                    Definition  typeDef = namer.resolveMember(
                                coder.statPos, TYPES, c.type, null, STATIC);
                    if (typeDef != null)
                            return items.make.StaticItem(coder.statPos, typeDef);
                }
                catch (IOException ex)
                {
                    namer.notFound(coder.statPos, TYPES, bname, null);
                }
                break;

            default:
                items.load(items.make.ImmediateItem(constants.toType(
                        constants.make.StringConst(
                            writer.xClassName(type).replace((byte)'/', (byte)'.')))));
                Definition  forNameDef = namer.resolveMember(coder.statPos,
                                forNameS, types.classType,
                                new Type[]{types.stringType}, STATIC);
                if (forNameDef != null)
                    return items.invoke(items.make.StaticItem(coder.statPos, forNameDef));
        }
        coder.emitop(aconst_null);
        return items.make.StackItem(OBJECT_CODE);
    }

    void toStringOnStack(Type argtype) {
        Definition  valueOfDef;
        if (argtype.isTypeOfConstant())
            switch (argtype.tconst()) {
                case StringConst(_):
                    return;
            }
        switch (argtype) {
            case NullType:
            case ArrayType(_):
                valueOfDef = namer.resolveMember(coder.statPos, valueOfS,
                                types.stringType, new Type[]{types.objectType}, STATIC);
                if (valueOfDef != null)
                    items.invoke(items.make.StaticItem(coder.statPos, valueOfDef));
                break;
            case ClassType(_):
                valueOfDef = namer.resolveMember(coder.statPos, valueOfS,
                                    types.stringType, new Type[]{argtype}, STATIC);
                if (valueOfDef != null)
                    items.invoke(items.make.StaticItem(coder.statPos, valueOfDef));
                valueOfDef = namer.resolveMember(coder.statPos, valueOfS,
                                types.stringType, new Type[]{types.objectType}, STATIC);
                if (valueOfDef != null)
                    items.invoke(items.make.StaticItem(coder.statPos, valueOfDef));
                break;
            default:
                valueOfDef = namer.resolveMember(coder.statPos, valueOfS,
                                types.stringType, new Type[]{argtype}, STATIC);
                if (valueOfDef != null)
                    items.invoke(items.make.StaticItem(coder.statPos, valueOfDef));
                break;
        }
    }
    
    protected boolean isNullRef(Tree tree)
    {
        switch (tree)
        {
            case Ident(_, Definition def):
                return (def == constants.nullConst);
            
            default:
                return false;
        }
    }
    
    protected boolean isNullInt(Tree tree)
    {
        if (tree.type.isTypeOfConstant())
            switch (tree.type.tconst())
            {
                case IntConst(int value):
                    return (value == 0);
            }
        return false;
    }
    
    protected boolean isUnusedLocal(Tree tree)
    {
        Definition  def = tree.def();
        return ((def != null) && (def.kind == VAR) &&
               (def.owner.kind == FUN)) && ((def.modifiers & USED) == 0);
    }
    
    protected Item completeBinop(Item l, Tree lhs, Tree rhs, int opcode, Type[] argtypes, Type restype)
    {
        if (opcode == string_add)
        {
            items.load(l);
            toStringOnStack(lhs.type);
            items.load(genExpr(rhs, rhs.type));
            toStringOnStack(rhs.type);
            Definition  concatDef = namer.resolveMember(coder.statPos, concatS,
                            types.stringType, new Type[]{types.stringType}, 0);
            if (concatDef != null)
                return items.invoke(items.make.MemberItem(coder.statPos,
                                        concatDef, false, false));
            else
                return items.make.StackItem(OBJECT_CODE);
        }
        else
        {
            Type    ltype = argtypes[0];
            Type    rtype = argtypes[1];
            if ((opcode >= ishll) && (opcode <= lushrl))
            {
                opcode = opcode + (ishl - ishll);
                rtype = types.intType;
            }
            l = items.load(items.coerce(l, ltype));
            if ((opcode >= if_acmpeq) && (opcode <= if_acmpne) &&
                isNullRef(rhs))
                opcode += ifnull - if_acmpeq;
            else
            if ((opcode >= if_icmpeq) && (opcode <= if_icmple) &&
                isNullInt(rhs))
                opcode += ifeq - if_icmpeq;
            else
                items.load(genExpr(rhs, rtype));
            if (opcode >= (1 << preShift))
            {
                coder.emitop(opcode >> preShift);
                opcode = opcode & 0xff;
            }
            if (((opcode >= ifeq) && (opcode <= if_acmpne)) ||
                (opcode == ifnull) || (opcode == ifnonnull))
                return items.make.CondItem(opcode, null, null);
            else
            {
                coder.emitop(opcode);
                return items.make.StackItem(items.make.typecode(restype));
            }
        }
    }

    protected Item completeUnop(Item od, int optag, OperatorDef operator)
    {
        int     opcode = operator.opcode;
        Item    res;
        switch (optag)
        {
            case POS:
                res = items.load(od);
                break;
    
            case NEG:
                res = items.load(od);
                coder.emitop(opcode);
                break;
            
            case NOT:
                res = items.negate(items.mkCond(od));
                break;
            
            case COMPL:
                res = items.load(od);
                coder.emitMinusOne(od.typecode);
                coder.emitop(opcode);
                break;
            
            case PREINC:
            case PREDEC:
                items.duplicate(od);
                if ((od instanceof LocalItem) &&
                    ((opcode == iadd) || (opcode == isub)))
                {
                    items.incr((LocalItem)od, (optag == PREINC) ? 1 : -1);
                    res = od;
                }
                else
                {
                    items.load(od);
                    coder.emitop(coder.one(od.typecode));
                    coder.emitop(opcode);
                    res = items.make.AssignItem(od);
                }
                break;
            
            case POSTINC:
            case POSTDEC:
                items.duplicate(od);
                if ((od instanceof LocalItem) &&
                    ((opcode == iadd) || (opcode == isub)))
                {
                    res = items.load(od);
                    items.incr((LocalItem)od, (optag == POSTINC) ? 1 : -1);
                }
                else
                {
                    res = items.load(od);
                    items.stash(od, od.typecode);
                    coder.emitop(coder.one(od.typecode));
                    coder.emitop(opcode);
                    items.store(od);
                }
                break;
            
            default:
                throw new InternalError();
        }
        return res;
    }

    protected int tcode(Type type)
    {
        switch (type.deref())
        {
            case NumType(int tag):
                switch (tag)
                {
                    case BYTE:
                        return 8;
                        
                    case BOOLEAN:
                        return 4;
                        
                    case SHORT:
                        return 9;
                        
                    case CHAR:
                        return 5;
                        
                    case INT:
                        return 10;
                            
                    case LONG:
                        return 11;
                            
                    case FLOAT:
                        return 6;
                            
                    case DOUBLE:
                        return 7;
                    
                    default:
                        throw new InternalError();
                }
            
            case ClassType(_):
                return 0;
                
            case ArrayType(_):
                return 1;
            
            default:
                throw new InternalError("tcode " + type);
        }
    }

    protected Item makeNewArray(Type type, int ndims, Type pt)
    {
        Type    elemtype = type.elemtype();
        int     elemcode = tcode(elemtype);
        
        if ((elemcode == 0) || ((elemcode == 1) && (ndims == 1)))
            coder.emitop2(anewarray, coder.mkref(elemtype));
        else
        if (elemcode == 1)
        {
            coder.emitop(multianewarray, 1 - ndims);
            coder.emit2(coder.mkref(type));
            coder.emit1(ndims);
        }
        else
            coder.emitop1(newarray, elemcode);
        return items.make.StackItem(items.make.typecode(type));
    }
    
    protected Tree codeForQualification(Tree tree)
    {
        switch (tree)
        {
            case Select(Tree selected, _, Definition def):
                if (def.kind == TYP)
                    return null;
                else
                    return codeForQualification(selected);
            
            case Ident(_, _):
                return null;
        }
        return tree;
    }
    
    protected Item genExpr(Tree tree, Type pt)
    {
        Item res;
        
        if (tree.type.isTypeOfConstant())
            res = items.make.ImmediateItem(tree.type);
        else
        {
            switch (tree)
            {
                case If(Tree cond, Tree thenpart, Tree elsepart):
                    Chain       thenExit = null;
                    CondItem    c = items.mkCond(genExpr(cond, types.booleanType));
                    Chain       elseChain = items.jump(false, c);
                    if ((c.trueJumps != null) || (c.opcode != coder.dontgoto))
                    {
                        coder.resolve(c.trueJumps);
                        items.load(genExpr(thenpart, pt));
                        thenExit = coder.branch(goto_);
                    }
                    if ((elsepart != null) && (elseChain != null))
                    {
                        coder.resolve(elseChain);
                        items.load(genExpr(elsepart, pt));
                        coder.resolve(thenExit);
                    }
                    else
                    {
                        coder.resolve(thenExit);
                        coder.resolve(elseChain);
                    }
                    res = items.make.StackItem(items.make.typecode(pt));
                    break;

                case Aggregate(Tree[] elems, Tree arrtype):
                    Type    elemtype = tree.type.elemtype();
                    items.loadIntConst(elems.length);
                    Item    arr = makeNewArray(tree.type, 1, pt);
                    for (int i = 0; i < elems.length; i++)
                    {
                        items.duplicate(arr);
                        items.loadIntConst(i);
                        items.load(genExpr(elems[i], elemtype));
                        items.store(items.make.IndexedItem(elemtype));
                    }
                    res = arr;
                    break;
                
                case Apply(Tree fn, Tree[] args):
                    Item    f = genExpr(fn, fn.type);
                    loadArgs(args, fn.type.argtypes());
                    res = items.invoke(f);
                    break;
                
                case NewObj(Tree encl, Tree clazz, Tree[] args,
                            Tree def, Definition constructor):
                    report.assert((encl == null) && (def == null));
                    coder.emitop2(new_, coder.mkref(tree.type));
                    coder.emitop(dup);
                    loadArgs(args, constructor.type.argtypes());
                    items.invoke(items.make.MemberItem(tree.pos, constructor,
                                    true, false));
                    res = items.make.StackItem(items.make.typecode(tree.type));
                    break;
                
                case NewArray(Tree elemtype, Tree[] dims):
                    for (int i = 0; i < dims.length; i++)
                        items.load(genExpr(dims[i], types.intType));
                    res = makeNewArray(tree.type, dims.length, pt);
                    break;
                
                case Assign(Tree lhs, Tree rhs):
                    if (isUnusedLocal(lhs))
                        res = genExpr(rhs, lhs.type);
                    else
                    {
                        Item    l = genExpr(lhs, lhs.type);
                        items.load(genExpr(rhs, lhs.type));
                        res = items.make.AssignItem(l);
                    }
                    break;
                
                case Assignop(int opcode, Tree lhs, Tree rhs, Definition operator):
                    Item    l = genExpr(lhs, lhs.type);
                    if (((opcode == PLUS) || (opcode == MINUS)) &&
                            (l instanceof LocalItem) &&
                            (lhs.type.tag() >= MIN_BASICTYPE_TAG) &&
                            (rhs.type.tag() >= MIN_BASICTYPE_TAG) &&
                            (lhs.type.tag() <= INT) &&
                            (rhs.type.tag() <= INT) &&
                            rhs.type.isTypeOfConstant())
                    {
                        int ival = rhs.type.tconst().intValue();
                        if (opcode == MINUS)
                            ival = -ival;
                        if ((-0x8000 <= ival) && (ival <= 0x7fff))
                        {
                            items.incr(((LocalItem)l), ival);
                            return l;
                        }
                    }
                    items.duplicate(l);
                    int         opc = ((OperatorDef)operator).opcode;
                    MethodType  otype = (MethodType)operator.type;
                    items.coerce(completeBinop(l, lhs, rhs, opc, otype.argtypes, otype.restype),
                                lhs.type);
                    res = items.make.AssignItem(l);
                    break;
                
                case Binop(int opcode, Tree lhs, Tree rhs, Definition operator):
                    if (opcode == OR)
                    {
                        CondItem    lcond = items.mkCond(genExpr(lhs, lhs.type));
                        if ((lcond.falseJumps != null) ||
                            (lcond.opcode != goto_))
                        {
                            Chain       trueJumps = items.jump(true, lcond);
                            coder.resolve(lcond.falseJumps);
                            CondItem    rcond = items.mkCond(genExpr(rhs, rhs.type));
                            res = items.make.CondItem(rcond.opcode,
                                    coder.mergeChains(trueJumps, rcond.trueJumps),
                                    rcond.falseJumps);
                        }
                        else
                            res = lcond;
                    }
                    else
                    if (opcode == AND)
                    {
                        CondItem    lcond = items.mkCond(genExpr(lhs, lhs.type));
                        if ((lcond.trueJumps != null) ||
                            (lcond.opcode != coder.dontgoto))
                        {
                            Chain       falseJumps = items.jump(false, lcond);
                            coder.resolve(lcond.trueJumps);
                            CondItem    rcond = items.mkCond(genExpr(rhs, rhs.type));
                            res = items.make.CondItem(rcond.opcode,
                                    rcond.trueJumps,
                                    coder.mergeChains(falseJumps, rcond.falseJumps));
                        }
                        else
                            res = lcond;
                    }
                    else
                    {
                        MethodType  otype = (MethodType)operator.type;
                        int         opc = ((OperatorDef)operator).opcode;
                        if (((opc >= if_acmpeq) && (opc <= if_acmpne) && isNullRef(lhs)) ||
                            ((opc >= if_icmpeq) && (opc <= if_icmpne) && isNullInt(lhs)))
                            res = completeBinop(genExpr(rhs, rhs.type), rhs, lhs, opc,
                                                otype.argtypes, otype.restype);
                        else
                        if ((opc >= if_icmplt) && (opc <= if_icmple) && isNullInt(lhs))
                        {
                            switch (opc)
                            {
                                case if_icmplt:
                                    opc = if_icmpgt;
                                    break;
                                
                                case if_icmpge:
                                    opc = if_icmple;
                                    break;
                                
                                case if_icmpgt:
                                    opc = if_icmplt;
                                    break;
                                
                                case if_icmple:
                                    opc = if_icmpge;
                                    break;
                            }
                            res = completeBinop(genExpr(rhs, rhs.type), rhs, lhs, opc,
                                                new Type[]{otype.argtypes[1],
                                                            otype.argtypes[0]},
                                                otype.restype);
                        }
                        else
                            res = completeBinop(genExpr(lhs, lhs.type), lhs, rhs, opc,
                                                otype.argtypes, otype.restype);
                    }
                    break;
                
                case Unop(int opcode, Tree operand, Definition operator):
                    res = completeUnop(genExpr(operand, operator.type.argtypes()[0]),
                                            opcode, (OperatorDef)operator);
                    break;

                case Typeop(int opcode, Tree expr, Tree clazz):
                    Item    od = genExpr(expr, expr.type);
                    // an instanceof operator
                    if (opcode == TYPETEST)
                    {
                        items.load(od);
                        coder.emitop2(instanceof_, coder.mkref(clazz.type));
                        res = items.make.StackItem(BYTE_CODE);
                    }
                    else
                    // a typecast to a basic type
                    if (clazz.type.isBasic())
                        res = items.load(items.coerce(od, clazz.type));
                    else
                    // a save typecast
                    if (((JavaSettings)context.settings).optimize && (opcode == SAVECAST))
                        res = od;
                    // a regular typecast
                    else
                    {
                        res = items.load(od);
                        if (types.mgb(expr.type, clazz.type.tdef()) == null)
                            coder.emitop2(checkcast, coder.mkref(clazz.type));
                    }
                    break;
                
                case Index(Tree indexed, Tree index):
                    items.load(genExpr(indexed, indexed.type));
                    items.load(genExpr(index, types.intType));
                    res = items.make.IndexedItem(tree.type);
                    break;
                
                case Select(Tree selected, Name selector, Definition def):
                    if (selector == PredefConst.CLASS_N)
                        res = genClassOf(selected.type);
                    else
                    if ((def.modifiers & STATIC) != 0)
                    {
                        if ((selected = codeForQualification(selected)) != null)
                            items.drop(items.load(genExpr(selected, selected.type)));
                        res = items.make.StaticItem(tree.pos, def);
                    }
                    else
                    {
                        Definition  sdef = selected.def();
                        boolean selectSuper = (sdef != null) &&
                            ((sdef.kind == TYP) || (sdef.name == PredefConst.SUPER_N));
                        boolean selectThis = (sdef != null) &&
                            ((sdef.kind == TYP) || (sdef.name == PredefConst.THIS_N));
                        items.load(selectSuper ?
                                        items.make.SelfItem(true) :
                                        genExpr(selected, selected.type));
                        if (def == types.lengthVar)
                        {
                            coder.emitop(arraylength);
                            res = items.make.StackItem(INT_CODE);
                        }
                        else
                            res = items.make.MemberItem(tree.pos, def,
                                    ((def.modifiers & PRIVATE) != 0) || selectSuper,
                                    selectThis);
                    }
                    break;

                case Ident(_, Definition def):
                    if (def == constants.nullConst)
                    {
                        coder.emitop(aconst_null);
                        res = items.make.StackItem(items.make.typecode(tree.type));
                    }
                    else
                    if ((def.kind == VAR) && (def.owner.kind == FUN))
                        res = items.make.LocalItem(tree.pos, (VarDef)def);
                    else
                    if ((def.modifiers & STATIC) != 0)
                        res = items.make.StaticItem(tree.pos, def);
                    else
                    {
                        items.load(items.make.SelfItem(false));
                        res = items.make.MemberItem(tree.pos, def,
                                    (def.modifiers & PRIVATE) != 0, true);
                    }
                    break;
                    
                case Self(Tree clazz, int tag, Definition def):
                    report.assert(clazz == null);
                    res = items.make.SelfItem(tag != THIS);
                    if (def.kind == FUN)
                    {
                        items.load(res);
                        res = items.make.MemberItem(tree.pos, def, true, false);
                    }
                    break;

                case Literal(Constant value):
                    res = items.make.ImmediateItem(constants.toType(value));
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        return items.coerce(res, pt);
    }
    
    protected void loadArgs(Tree[] exprs, Type[] pts)
    {
        for (int i = 0; i < exprs.length; i++)
            items.load(genExpr(exprs[i], pts[i]));
    }
    
/** the debugging name
 */ 
    public String getDebugName()
    {
        return "gen";
    }

/** the debug method
 */
    public boolean debug(int debugId, TreeList treelist) throws AbortCompilation
    {
        if (super.debug(debugId))
            switch (debugId)
            {
                case Debug.ENTER:
                    return true;
                
                case Debug.EXIT:
                    treelist.process(disassem);
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
        GenEnv  localEnv = new GenEnv(tree, coder);
        localEnv.toplevel = tree;
        for (int i = 0; i < tree.decls.length; i++)
            genDecl(tree.decls[i], localEnv);
        tree.info.codeGenerated = true;
        return tree;
    }
}

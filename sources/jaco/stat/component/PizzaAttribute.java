//      /   _ _      JaCo
//  \  //\ / / \     - pizza attribution
//   \//  \\_\_/     
//         \         Matthias Zenger, 29/01/01

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.stat.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import java.util.*;
import Tree.*;
import PizzaTree.*;
import Type.*;
import Definition.*;


public class PizzaAttribute extends Attribute
                            implements PizzaModifierConst
{
/** other components
 */
    protected PizzaTypes        types;
    protected PizzaTypeChecker  checks;
    protected PizzaDefinitions  definitions;

    
    public String getName()
    {
        return "PizzaAttribute";
    }
    
    public void init(SemanticContext context)
    {
        super.init(context);
        checks = (PizzaTypeChecker)super.checks;
        definitions = (PizzaDefinitions)super.definitions;
        types = (PizzaTypes)super.types;
    }
    
    public void attribDecl(Tree tree, ContextEnv env)
    {
        switch ((PizzaTree)tree)
        {
            case Import(int tag, Tree qualid):
                /* if (compilerContext.mainContext.classesNest)
                {
                    switch (qualid)
                    {
                        case Select(Tree selected, Name selector, Definition def):
                            Definition  c = selected.type.tdef();
                            if (c.kind == TYP)
                            {
                                if (tag == IMPORTSTAR)
                                {
                                    imports.includeAll(
                                        env.toplevel.importScope[STAR_SCOPE],
                                        env, c, VAR | FUN);
                                }
                                else
                                if (tag == IMPORT)
                                {
                                    imports.includeNamed(
                                        env.toplevel.importScope[NAMED_SCOPE],
                                        env, c, selector, VAR | FUN);
                                }
                                else
                                    throw new InternalError();
                            }
                    }
                } */
                break;
            
            case Tree.MethodDecl(_, _, _, _, _, _, MethodDef f):
                checks.checkOverload(tree.pos, f);
                if (definitions.isAlgebraicSubclass(f.owner))
                {
                    if (((f.modifiers & STATIC) == 0) && !f.isConstructor())
                        checks.mustOverride(tree.pos, f);
                }
                super.attribDecl(tree, env);
                break;
            
            default:
                super.attribDecl(tree, env);
        }
    }
    
    protected Type selectorType(int pos, Type seltype)
    {
        if (seltype == null)
            return Type.AnyType;
        Type    t = seltype.deref();
        switch (t)
        {
            case NumType(int tag):
                if ((tag >= MIN_BASICTYPE_TAG) && (tag <= INT))
                    return types.intType;
                break;
            
            case ClassType(_):
                if ((t = types.algebraicSupertype(t)) != null)
                    return t;
                break;
            
            case ErrType:
                return Type.ErrType;
        }
        report.error(pos, seltype + " is not a valid switch selector type");
        return Type.ErrType;
    }
    
    protected Definition findCaseDef(int pos, Name name, ContextEnv env, int kind, Type pt)
    {
        Definition  def;
        if (kind == FUN)
            def = namer.findMethod(env, pt, name, null);
        else
            def = namer.findField(env, pt.tdef(), name);
        if (def.kind >= BAD)
            def = Scope.errDef;
        else
        if ((def.owner != pt.tdef()) || ((def.modifiers & CASEDEF) == 0))
            def = Scope.errDef;
        return def;
    }
    
    protected Definition findCase(Tree tree, ContextEnv env, int kind, Type pt)
    {
        Type        switchType = pt;
        Definition  res = Scope.errDef;
        
        while ((res == Scope.errDef) && types.isAlgebraicType(pt))
        {
            switch (tree)
            {
                case Ident(Name name, _):
                    res = findCaseDef(tree.pos, name, env, kind, pt);
                    break;
                    
                case Select(Tree selected, Name name, _):
                    Type    st = attribType(selected, env);
                    if (st.tdef() == pt.tdef())
                        res = findCaseDef(tree.pos, name, env, kind, pt);
                    break;
            }
            tree.setDef(res);
            tree.type = res.type;
            pt = pt.supertype();
        }
        if (res == Scope.errDef)
            report.error(tree.pos, "not a valid case of " + switchType);
        return res;
    }
    
/** attribute pattern 'tree' in environment 'env', where 'pt' is the
 *  expected type
 */
    public Type attribPattern(Tree tree, ContextEnv env, Type pt)
    {
        Type    pattype = pt;
        
        switch (tree)
        {
            case VarDecl(Name name,_, Tree vartype, _, _):
                // check if it's not a blank
                if (name != null)
                {
                    attribNonVoidType(vartype, env);
                    VarDef  v = enterVar((VarDecl)tree, env);
                    // check consistency; allow subtype constraints
                    if (!types.sametype(v.type, pt) &&
                        !(types.isAlgebraicType(pt) && types.subtype(v.type, pt)))
                    {
                        checks.typeError(tree.pos, "pattern argument has wrong type",
                                         v.type, pt);
                        pattype = Type.ErrType;
                    }
                }
                break;
                
            case Apply(Tree fn, Tree[] args):
                switch (pt.deref())
                {
                    case ClassType(_):
                        Definition  casedef = findCase(fn, env, FUN, pt);
                        Type[]      patargs = null;
                        if (casedef.type != Type.ErrType)
                        {
                            Definition  ct = namer.findMemberType(env, casedef.owner, casedef.name);
                            report.assert(ct.kind < BAD);
                            pattype = ct.type;
                            patargs = casedef.type.argtypes();
                            if (patargs.length != args.length)
                            {
                                report.error(tree.pos, "wrong number of arguments in pattern");
                                pattype = Type.ErrType;
                            }
                        }
                        else
                            pattype = Type.ErrType;
                        if (pattype != Type.ErrType)
                            for (int i = 0; i < args.length; i++)
                                attribPattern(args[i], env, patargs[i]);
                        break;
            
                    default:
                        if (pt != Type.ErrType)
                            report.error(tree.pos, "constant expression required");
                        pattype = Type.ErrType;
                }
                break;
            
            case Ident(_, _):
            case Select(_, _, _):
                switch (pt.deref())
                {
                    case ClassType(_):
                        findCase(tree, env, VAL, pt);
                        pattype = tree.type;
                        break;
                    
                    default:
                        pattype = attribConstExpr(tree, env, pt);
                }
                break;
            
            default:
                pattype = attribConstExpr(tree, env, pt);
        }
        tree.type = pattype;
        return pattype;
    }
    
    public Type attribStat(Tree tree, ContextEnv env, Type pt, Type sofar) {
        switch (tree) {
            case Switch(Tree selector, Case[] cases):
                trees.pushPos(tree.pos);
                tree.type = sofar;
                Type selpt = selectorType(selector.pos, null);
                Type seltype = selectorType(selector.pos,
                    attribExpr(selector, env, VAL, selpt));
                ContextEnv switchEnv = env.dup(tree);
                Hashtable tags = new Hashtable();
                boolean hasDefault = false;
                Stat.addSwitch(env.enclMethod.def(), seltype.tdef());
                for (int i = 0; i < cases.length; i++) {
                    Case c = cases[i];
                    ContextEnv caseEnv = switchEnv.dup(c);
                    for (int j = 0; j < c.pat.length; j++) {
                        if (c.pat[j] != null) {
                            Type pattype = attribPattern(c.pat[j], caseEnv, seltype);
                            if (seltype != Type.ErrType) {
                                if (isDuplicateCase(pattype, tags))
                                    report.error(c.pos, "duplicate.label", "case");
                            }
                        } else if (hasDefault)
                            report.error(c.pos, "duplicate.label", "default");
                        else
                            hasDefault = true;
                    }
                    sofar = attribStats(c.stats, caseEnv, pt, sofar);
                    env.info.thrown = types.append(caseEnv.info.thrown, env.info.thrown);
                    Definition e = caseEnv.info.scope.elems;
                    caseEnv.info.scope.leave();
                    if ((seltype.tdef().modifiers & ALGEBRAIC) == 0) {
                        while (e != null) {
                            if (e.def.kind == VAR)
                                enterShadow(tree.pos, switchEnv.info.scope, (VarDef)e.def);
                            e = e.sibling;
                        }
                        e = switchEnv.info.scope.elems;
                        while (e != null) {
                            VarDef v = (VarDef)e.def;
                            e = e.sibling;
                            if (v.adr > 0) {
                                switchEnv.info.scope.remove(v);
                                v.modifiers &= ~ABSTRACT;
                                c.stats = trees.append(makeShadowDef(tree.pos, v), c.stats);
                                enterShadow(tree.pos, switchEnv.info.scope, v);
                            }
                        }
                    }
                }
                switchEnv.info.scope.leave();
                trees.popPos();
                return sofar;
            default:
                return super.attribStat(tree, env, pt, sofar);
        }
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - extension of EnterMembers
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.stat.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import PizzaTree.*;
import Definition.*;


public class PizzaEnterMembers extends EnterMembers
                               implements PizzaModifierConst, EACConst
{
/** helper components
 */
    protected Constants             constants;
    protected PizzaDefinitions      definitions;
    protected PizzaTypes            types;
    protected PrettyPrinter         pretty;
    protected PizzaTypeChecker      checks;
    protected Mangler               mangler;
    
    
    public String getName()
    {
        return "PizzaEnterMembers";
    }
    
    public void init(SemanticContext context)
    {
        super.init(context);
        constants = context.compilerContext.mainContext.Constants();
        pretty = context.compilerContext.mainContext.PrettyPrinter();
        mangler = context.compilerContext.mainContext.Mangler();
        checks = (PizzaTypeChecker)context.TypeChecker();
        definitions = (PizzaDefinitions)super.definitions;
        types = (PizzaTypes)super.types;
    }
    
    protected Tree caseToVar(int pos, Name name, int mods, int tag, CDef owner,
                          Name initName)
    {
        owner.tag++;
        Tree    init = trees.at(pos).make(trees.New(VAR_CASE_N,
                                trees.newdef.Literal(constants.make.IntConst(tag))));
        if (initName != null)
            init = trees.at(pos).make(trees.newdef.Apply(
                    trees.newdef.Select(init, initName), trees.noTrees));
        return trees.at(pos).make(trees.newdef.VarDecl(
                        name,
                        (mods & modifiers.AccessMods) | STATIC | FINAL | CASEDEF,
                        trees.newdef.Ident(VAR_CASE_N),
                        init));
    }
    
    protected Tree genConstructor(ClassDecl c)
    {
        if (c.name == VAR_CASE_N)
            return null;
        MethodDecl  realConstr = (MethodDecl)c.members[c.members.length - 1];
        return trees.at(c.pos).make(trees.newdef.MethodDecl(
                                c.name,
                                c.mods | CASEDEF,
                                trees.newdef.Ident(c.name),
                                (VarDecl[])trees.copy(realConstr.params,
                                                new VarDecl[realConstr.params.length]),
                                trees.noTrees,
                                new Tree[]{trees.newdef.Return(
                                    trees.newdef.NewObj(null,
                                        trees.newdef.Ident(c.name),
                                        trees.Idents(realConstr.params), null))}));
    }
    
    protected Tree genBool(boolean val)
    {
        if (val)
            return trees.Ident(constants.trueConst);
        else
            return trees.Ident(constants.falseConst);
    }
    
    protected Tree[] genTagAndConstr(int pos, CDef owner)
    {
        int         old = trees.setPos(pos);
        boolean     base = (owner == owner.baseClass);
        VarDecl[]   param;
        Tree    tag = base ? trees.newdef.VarDecl(TAG_N,
                                PUBLIC | FINAL | SYNTHETIC,
                                trees.toTree(types.intType), null) : null;
        Tree    constr = trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PROTECTED | SYNTHETIC,
                                null,
                                param = new VarDecl[]{(VarDecl)trees.newdef.VarDecl(
                                    TAG_N, 0, trees.toTree(types.intType), null)},
                                trees.noTrees,
                                base ?  new Tree[]{trees.FieldInit(TAG_N)} :
                                        new Tree[]{trees.SuperCall(false, param)});         
        trees.setPos(old);
        if (base)
            return new Tree[]{tag, constr};
        else
            return new Tree[]{constr};
    }
        
    public Definition memberEnter(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        Definition result = null;
        main: switch (tree)
        {
            case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] members, ClassDef c):
                if (accountant.todo.get(c) != null)
                {
                    report.useEnv(env.toplevel.info);
                    ContextEnv  localEnv = (ContextEnv)accountant.classEnvs.get(c);
                    if ((localEnv != null) && ((c.modifiers & INTERFACE) != 0))
                    {
                        report.error(tree.pos, "cyclic inheritance involving " + c);
                        c.setSupertype(types.objectType);
                        c.setInterfaces(types.noTypes);
                    }
                    else
                    {
                        c.setSupertype(null);
                        localEnv = accountant.classEnv((ClassDecl)tree, env);
                        accountant.classEnvs.put(c, localEnv);
                        Type st = (extending == null) ?
                                    (c.type == types.objectType ?
                                        null :
                                        types.objectType) :
                                    attribSuper(extending, localEnv, false);
                        boolean unset = (c.supertype() == null);
                        if (unset)
                            c.setSupertype(st);
                        Type[] is = new Type[implementing.length];
                        for (int i = 0; i < is.length; i++)
                            is[i] = attribSuper(implementing[i], localEnv, true);
                        if (unset)
                            c.setInterfaces(is);
                        namer.fixupScope(tree.pos, c);
                        accountant.todo.remove(c);
                        if (((c.modifiers & CASEDEF) == 0) &&
                            (definitions.isAlgebraicClass(st.tdef())))
                        {
                            if ((c.modifiers & ALGEBRAIC) == 0)
                            {
                                if ((c.modifiers & ABSTRACT) == 0)
                                {
                                    report.error(tree.pos, "algebraic class " + c.name +
                                        " has to be declared abstract; it does not " +
                                        "define a case");
                                    c.modifiers |= ABSTRACT;
                                }
                                c.modifiers |= ALGEBRAIC;
                            }
                            if (unset && (is.length > 0))
                                checks.noNewTypeIntro(implementing[0].pos, is, st);
                        }
                        if (((mods & INTERFACE) == 0) && hasConstructors(members))
                        {
                            if ((c.modifiers & ALGEBRAIC) != 0)
                                report.error(tree.pos, "algebraic classes must not have java constructors");
                        }
                        else
                        if (((mods & INTERFACE) == 0) && ((c.modifiers & ALGEBRAIC) == 0))
                        {
                            ((ClassDecl)tree).members = members =
                                    trees.append(members, trees.at(tree.pos).make(
                                            trees.DefaultConstructor(
                                                c.modifiers & modifiers.AccessMods,
                                                false, types.noTypes)));
                        }
                        if ((c.modifiers & ALGEBRAIC) != 0)
                        {
                            ((CDef)c).baseClass = (CDef)definitions.algebraicBase(c);
                            ((ClassDecl)tree).members = members =
                                trees.append(genTagAndConstr(tree.pos, (CDef)c), members);
                            if ((c.modifiers & FINAL) != 0)
                            {
                                ((ClassDecl)tree).mods &= ~FINAL;
                                ((ClassDecl)tree).mods |= ALG_FINAL;
                                c.modifiers = (c.modifiers ^ FINAL) | ALG_FINAL;
                            }
                            if ((st.tdef().modifiers & ALG_FINAL) != 0)
                                report.error(tree.pos, "cannot extend final algebraic class");
                        }
                        TreeList    newMembers = new TreeList();
                        for (int i = 0; i < members.length; i++)
                        {
                            switch ((PizzaTree)members[i])
                            {
                                case Tree.ClassDecl(Name nam, _, _, _, _, ClassDef def):
                                    if ((def.modifiers & CASEDEF) != 0)
                                    {
                                        ((CDef)def).baseClass = (CDef)c;
                                        Tree    constr = genConstructor((ClassDecl)members[i]);
                                        if (constr != null)
                                        {
                                            ((CDef)def).tag += ((CDef)st.tdef()).tag;
                                            newMembers.append(constr);
                                            ((MDef)memberEnter(constr, localEnv)).tag = ((CDef)def).tag;
                                        }
                                        else
                                            ((CDef)def).tag = -1;
                                    }
                                    break;
                                    
                                case CaseDecl(Name na, int mo, _, Tree[] inits, ClassDef owner):
                                    int tag = ((CDef)c).tag + ((CDef)st.tdef()).tag;
                                    members[i] = caseToVar(members[i].pos, na, mo, tag, (CDef)c,
                                        (inits != null) ? ((Ident)inits[0]).name : null);
                                    ((VDef)memberEnter(members[i], localEnv)).tag = tag;
                                    break;
                                    
                                default:
                                    memberEnter(members[i], localEnv);
                            }
                        }
                        if (definitions.isAlgebraicClass(c))
                        {
                            if ((((CDef)c).tag > 0) && ((c.modifiers & ABSTRACT) != 0))
                            {
                                report.error(tree.pos, "an algebraic class that defines " +
                                                       "cases cannot be abstract");
                                ((ClassDecl)tree).mods &= ~ABSTRACT;
                                c.modifiers &= ~ABSTRACT;
                            }
                            ((CDef)c).tag += ((CDef)st.tdef()).tag;
                        }
                        if (newMembers.length() > 0)
                            ((ClassDecl)tree).members = members =
                                            trees.append(members, newMembers.toArray());
                        if ((c.modifiers & ABSTRACT) != 0 &&
                            ((c.modifiers & INTERFACE) == 0))
                            implementInterfaceMethods((ClassDecl)tree, c, localEnv);
                        for (int i = 0; i < members.length; i++)
                        {
                            switch (members[i])
                            {
                                case ClassDecl(_, _, _, _, _, _):
                                    memberEnter(members[i], localEnv);
                                    break;
                            }
                            if ((c.owner.kind != PCK) &&
                                ((c.modifiers & INTERFACE) == 0) &&
                                ((mods & STATIC) == 0) &&
                                ((members[i].mods() & STATIC) != 0))
                                report.error(members[i].pos, "inner classes must not " +
                                                             "have static members");
                        }
                        accountant.classEnvs.remove(c);
                        //System.out.println("ENTER_MEMBER: ");
                        //pretty.printDecl(tree);//DEBUG
                    }
                    report.usePreviousEnv();
                }
                result = c;
                break;
            
            case VarDecl(Name name, int mods, Tree vartype, _, _):
                if (definitions.isAlgebraicSubclass(env.info.scope.owner) &&
                    ((mods & STATIC) == 0))
                {
                    report.error(tree.pos, "algebraic subclasses must not have " +
                                           "variable declarations");
                    result = super.memberEnter(tree, env);
                    result.type = Type.ErrType;
                }
                else
                    result = super.memberEnter(tree, env);
                break;
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):
                result = super.memberEnter(tree, env);
                Stat.addMethod(result);
                if (result.isConstructor() &&
                    ((result.owner.modifiers & CASEDEF) != 0))
                {
                    if ((stats != null) && (stats.length > 0))
                        switch (stats[0])
                        {
                            case Exec(Apply(Self(_, SUPER, _), _)):
                                break main;
                        }
                    ((MethodDecl)tree).stats = trees.append(
                            trees.newdef.Exec(trees.newdef.Apply(trees.Super(),
                                new Tree[]{trees.newdef.Literal(
                                    constants.make.IntConst(((CDef)result.owner).tag))})),
                            stats);
                }
                break;
                
            default:
                result = super.memberEnter(tree, env);
                break;
        }
        trees.popPos();
        return result;
    }
}

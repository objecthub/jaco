//      /   _ _      JaCo
//  \  //\ / / \     - extended enter classes attribution pass
//   \//  \\_\_/     
//         \         Matthias Zenger, 25/01/99

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.stat.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;
import PizzaTree.*;
import Tree.*;
import Definition.*;


public class PizzaEnterClasses extends EnterClasses
                               implements PizzaModifierConst, EACConst
{
/** helper components
 */
    public PizzaDefinitions     definitions;
    public PizzaTypes           types;

/** is a variable class already created?
 */
    protected Tree              varCaseTree;
    protected int               caseVars;
    
    
    public String getName()
    {
        return "PizzaEnterClasses";
    }
    
    public void init(SemanticContext context)
    {
        super.init(context);
        definitions = (PizzaDefinitions)super.definitions;
        types = (PizzaTypes)super.types;
    }
    
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        tree.importScope = new Scope[2];
        tree.importScope[NAMED_SCOPE] = new Scope(null, null);
        tree.importScope[STAR_SCOPE] = new Scope(null, null);
        ContextEnv  localEnv = accountant.ContextEnv(tree);
        if (tree.decls.length > 0)
        {
            switch (tree.decls[0])
            {
                case PackageDecl(Tree qualid):
                    tree.setDef(definitions.definePackage(
                                    trees.fullName(qualid)));
                    Stat.addLines(tree.def(), tree.info.source.toString(),
                                  tree.info.source.lines,
                                  tree.info.source.bytes);
                    break;
                    
                default:
                    tree.setDef(definitions.emptyPackage);
            }
            reader.directory(tree.def());
            for (int i = 0; i < tree.decls.length; i++)
            {
                nextclassnum = 0;
                tree.decls[i] = classEnter(tree.decls[i], localEnv);
            }
        }
        return tree;
    }
    
    protected void sortCases(String[] keys, Tree[] values, int lo, int hi)
    {
        int     i = lo;
        int     j = hi;
        String  pivot = keys[(i+j)/2];
        do
        {
            while (keys[i].compareTo(pivot) < 0)
                i++;
            while (pivot.compareTo(keys[j]) < 0)
                j--;
            if (i <= j)
            {
                String  temp1 = keys[i];
                keys[i] = keys[j];
                keys[j] = temp1;
                Tree    temp2 = values[i];
                values[i++] = values[j];
                values[j--] = temp2;
            }
        }
        while (i <= j);
        if (lo < j)
            sortCases(keys, values, lo, j);
        if (i < hi)
            sortCases(keys, values, i, hi);
    }
        
    protected void sortDecls(Tree[] decls)
    {
        if ((decls == null) || (decls.length < 2))
            return;
        String[]    keys = new String[decls.length];
        Tree[]      rest = new Tree[decls.length];
        int         ncases = 0;
        int         nrest = 0;
        for (int i = 0; i < decls.length; i++)
            switch((PizzaTree)decls[i])
            {
                case CaseDecl(Name name, _, _, _, _):
                    keys[ncases] = name.toString();
                    decls[ncases++] = decls[i];
                    break;
                
                default:
                    rest[nrest++] = decls[i];
                    break;
            }
        if (ncases > 0)
            sortCases(keys, decls, 0, ncases - 1);
        if (nrest > 0)
            System.arraycopy(rest, 0, decls, ncases, nrest);
    }
    
    protected Tree[] genFieldDefs(VarDecl[] fields)
    {
        Tree[]  fielddefs = new Tree[fields.length];

        for (int i = 0; i < fields.length; i++)
        {
            if ((fields[i].mods & ~FINAL) != 0)
            {
                fields[i].mods &= FINAL;
                report.error(fields[i].pos, "not a valid case field modifier");
            }
            fielddefs[i] = trees.at(fields[i].pos).make(
                            trees.newdef.VarDecl(
                                fields[i].name,
                                fields[i].mods | PUBLIC | SYNTHETIC,
                                trees.copy(fields[i].vartype),
                                null));
        }
        return fielddefs;
    }
        
    protected Tree genCaseConstr(int pos, VarDecl[] fields, Tree[] inits)
    {
        int     old = trees.setPos(pos);
        Tree    tree = trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PUBLIC | SYNTHETIC,
                                null,
                                trees.Params(fields),
                                trees.noTrees,
                                trees.append(inits, trees.FieldInits(fields)));
        trees.setPos(old);
        return tree;
    }
    
    protected Tree genDefaultCaseConstr(int pos, Tree[] inits)
    {
        int     old = trees.setPos(pos);
        Tree    tree = trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PROTECTED | SYNTHETIC,
                                null,
                                new VarDecl[0],
                                trees.noTrees,
                                inits);
        trees.setPos(old);
        return tree;
    }
    
    protected Tree caseToInner(int pos, VarDecl[] fields, Tree[] inits, CDef def)
    {
        def.tag = ((CDef)def.owner).tag++;
        Tree[]  body = ((fields.length > 0) && ((def.modifiers & FINAL) == 0)) ?
                            new Tree[]{genDefaultCaseConstr(pos, inits)} : new Tree[0];
        body = trees.append(genFieldDefs(fields), body);
        body = trees.append(body, genCaseConstr(pos, fields, inits));
        return trees.at(pos).make(trees.newdef.ClassDecl(
                    mangler.unmangleShort(def.fullname, def.fullname),
                    def.modifiers & (FINAL | PUBLIC | PROTECTED | PRIVATE) |
                        STATIC | SYNTHETIC | CASEDEF,
                    trees.Qualid(Name.fromString(mangler.unmangle(def.owner.fullname))),
                    trees.noTrees,
                    body).setDef(def));
    }
    
    protected Tree variableClass(int pos, Definition owner)
    {
        VarDecl[]   param = trees.Params(new Type[]{types.intType});
        return trees.at(pos).make(trees.newdef.ClassDecl(
                    VAR_CASE_N,
                    STATIC | SYNTHETIC | CASEDEF | FINAL | PUBLIC,
                    trees.newdef.Ident(mangler.unmangleShort(owner.name,
                                                             owner.fullname)),
                    trees.noTrees,
                    new Tree[]{
                        trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PUBLIC | SYNTHETIC,
                                null, param, trees.noTrees,
                                new Tree[]{trees.SuperCall(false, param)})}));
    }
    
    protected Name simpleInitName(int tag)
    {
        return Name.fromString("init$" + tag);
    }
    
    protected Tree[] appendInitToVarClass(Tree[] inits)
    {
        if ((inits == null) || (inits.length == 0))
            return null;
        ((ClassDecl)varCaseTree).members =
            trees.append(((ClassDecl)varCaseTree).members,
                trees.at(inits[0].pos).make(trees.newdef.MethodDecl(
                                simpleInitName(caseVars),
                                PRIVATE | SYNTHETIC,
                                trees.newdef.Ident(VAR_CASE_N),
                                new VarDecl[0],
                                trees.noTrees,
                                trees.append(inits,
                                    trees.newdef.Return(trees.This())))));
        return new Tree[]{trees.newdef.Ident(simpleInitName(caseVars++))};
    }
    
    public Tree classEnter(Tree tree, ContextEnv env)
    {
        switch ((PizzaTree)tree)
        {
            case Tree.ClassDecl(_, _, _, _, Tree[] decls, _):
                Tree    varCase = varCaseTree;
                int     vars = caseVars;
                varCaseTree = null;
                caseVars = 0;
                sortDecls(decls);
                tree = super.classEnter(tree, env);
                if (varCaseTree != null)
                    switch (tree)
                    {
                        case ClassDecl(_, _, _, _, Tree[] d, _):
                            ((ClassDecl)tree).members = trees.append(d, varCaseTree);
                            break;
                    }
                varCaseTree = varCase;
                caseVars = vars;
                Stat.addClass(tree.def());
                return tree;
                
            case CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits, ClassDef def):
                Definition  owner = (env.info.scope == null) ?
                                        env.toplevel.def :
                                        env.info.scope.owner;
                owner.modifiers |= ALGEBRAIC;
                if (fields == null)
                {
                    if (varCaseTree == null)
                        varCaseTree = super.classEnter(variableClass(tree.pos, owner), env);
                    ((CaseDecl)tree).inits = appendInitToVarClass(inits);
                    return tree;
                }
                trees.pushPos(tree.pos);
                Name        fullname = (owner.kind == FUN) ?
                                        formAnonFullName(name, owner) :
                                        mangler.formFullName(name, owner);
                if ((accountant.compiled.get(fullname) != null) || (name == owner.name))
                {
                    if (name == owner.name)
                        report.error(tree.pos, "illegal case name");
                    else
                        report.error(tree.pos,
                                "duplicate class: " + mangler.unmangle(fullname));
                    Name    newname = Name.fromString(name + "$$$" +
                                        accountant.compiled.size());
                    ((CaseDecl)tree).name = newname;
                    fullname = mangler.formFullName(newname, owner);
                }
                accountant.compiled.put(fullname, fullname);
                ClassDef    c = def;
                if (def == null)
                {
                    c = (ClassDef)definitions.defineClass(fullname);
                    c.completer = null;
                    c.modifiers = modifiers.checkMods(
                                    tree.pos, mods, modifiers.InnerClassMods, true);
                    c.modifiers |= STATIC | SYNTHETIC | CASEDEF;
                    if ((owner.modifiers & STRICTFP) != 0)
                        c.modifiers |= STRICTFP;
                }
                c.owner = owner;
                c.sourcefile = env.toplevel.info.source.getName();
                c.setLocals(new Scope(null, c));
                env.toplevel.def.locals().enterIfAbsent(c);
                mangler.put(c.fullname, (ClassDef)owner, name, c.modifiers);
                Definition  proxy = c.proxy(name);
                Scope enclscope = accountant.enterScope(env);
                if (checkUnique(tree.pos, proxy, enclscope))
                {
                    enclscope.enter(proxy);
                    checkNoDuplicate(tree.pos, c);
                }
                tree.setDef(c);
                accountant.todo.put(c, tree);
                trees.popPos();
                Stat.addCase(c);
                return caseToInner(tree.pos, fields, inits, (CDef)c);
                
            default:
                return super.classEnter(tree, env);
        }
    }
}

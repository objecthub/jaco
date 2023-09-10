//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    enter classes pass
//                           
//  [XEnterClasses.java (3468) 24-Apr-01 17:54 -> 23-Jun-01 00:06]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.keris.component.*;
import jaco.keris.struct.*;
import java.util.*;
import Type.*;
import XTree.*;
import Tree.*;
import Definition.*;


public class XEnterClasses extends EnterClasses
                           implements XModifierConst, AlgebraicSupportConst {
    
    /** helper components
     */
    public XDefinitions definitions;
    public XTypes types;

    /** is a variable class already created?
     */
    protected Tree varCaseTree;
    protected TreeList caseClasses;
    
    /** map trees to ids (for variable cases)
     */
    public HashMap varCaseTags = new HashMap();
    
    /** helper class for representing the base class and the tag
     *  of a variant
     */
    public static class BaseAndTag {
        public int tag;
        public XClassDef base;
        
        public BaseAndTag(int tag, XClassDef base) {
            this.tag = tag;
            this.base = base;
        }
    }
    
    /** maps algebraic type declarations to trees representing the supertype
     */
    public HashMap newAlgebraics = new HashMap();
    
    public String getName() {
        return "XEnterClasses";
    }
    
    public void init(SemanticContext context) {
        super.init(context);
        definitions = (XDefinitions)super.definitions;
        types = (XTypes)super.types;
    }
    
    ////
    //// methods for pre-processing algebraic types
    ////
    
    protected void sortCases(String[] keys, Tree[] values, int lo, int hi) {
        int     i = lo;
        int     j = hi;
        String  pivot = keys[(i+j)/2];
        do {
            while (keys[i].compareTo(pivot) < 0)
                i++;
            while (pivot.compareTo(keys[j]) < 0)
                j--;
            if (i <= j) {
                String  temp1 = keys[i];
                keys[i] = keys[j];
                keys[j] = temp1;
                Tree    temp2 = values[i];
                values[i++] = values[j];
                values[j--] = temp2;
            }
        } while (i <= j);
        if (lo < j)
            sortCases(keys, values, lo, j);
        if (i < hi)
            sortCases(keys, values, i, hi);
    }
        
    protected void sortDecls(Tree[] decls) {
        if ((decls == null) || (decls.length < 2))
            return;
        String[]    keys = new String[decls.length];
        Tree[]      rest = new Tree[decls.length];
        int         ncases = 0;
        int         nrest = 0;
        for (int i = 0; i < decls.length; i++)
            switch((XTree)decls[i]) {
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
    
    protected Tree[] genFieldDefs(VarDecl[] fields) {
        Tree[]  fielddefs = new Tree[fields.length];
        for (int i = 0; i < fields.length; i++) {
            if ((fields[i].mods & ~FINAL) != 0) {
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
    
    protected Tree genCaseConstr(int pos, VarDecl[] fields) {
        int     old = trees.setPos(pos);
        Tree    tree = trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PUBLIC | SYNTHETIC,
                                null,
                                trees.Params(fields),
                                trees.noTrees,
                                trees.FieldInits(fields));
        trees.setPos(old);
        return tree;
    }
    
    protected Tree genDefaultCaseConstr(int pos) {
        int     old = trees.setPos(pos);
        Tree    tree = trees.newdef.MethodDecl(
                                PredefConst.INIT_N,
                                PROTECTED | SYNTHETIC,
                                null,
                                new VarDecl[0],
                                trees.noTrees,
                                trees.noTrees);
        trees.setPos(old);
        return tree;
    }
    
    protected Tree caseToInner(int pos, VarDecl[] fields, XClassDef def, XClassDef algebraic) {
        def.tag = algebraic.tag++;
        Tree[]  body = ((fields.length > 0) && ((def.modifiers & FINAL) == 0)) ?
                            new Tree[]{genDefaultCaseConstr(pos)} : new Tree[0];
        body = trees.append(genFieldDefs(fields), body);
        body = trees.append(body, genCaseConstr(pos, fields));
        return trees.at(pos).make(trees.newdef.ClassDecl(
                    mangler.unmangleShort(def.name, def.fullname),
                    def.modifiers & (FINAL | PUBLIC | PROTECTED | PRIVATE) |
                        SYNTHETIC | CASEDEF,
                    trees.newdef.Ident(mangler.unmangleShort(algebraic.name, algebraic.fullname)),
                    trees.noTrees,
                    body).setDef(def));
    }
    
    protected Name variableName(Definition algebraic) {
        return Name.fromString(mangler.unmangleShort(algebraic.name,
                               algebraic.fullname) + "" + VAR_CASE_N);
    }
    
    protected Tree variableClass(int pos, Definition owner)
    {
        VarDecl[]   param = trees.Params(new Type[]{types.intType});
        return trees.at(pos).make(trees.newdef.ClassDecl(
                    variableName(owner),
                    SYNTHETIC | CASEDEF | FINAL | PUBLIC,
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
    
    ////
    //// enter modules and classes
    ////
    
    public Tree classEnter(Tree tree, ContextEnv env) {
        switch ((XTree)tree) {
            case ClassDecl(Name name, int mods, Tree ext, _, Tree[] defs, ClassDef def):
                // handle everything except class fields
                if ((mods & CLASSFIELD) == 0) {
                    if ((mods & ALGEBRAIC) != 0) {
                        caseClasses = new TreeList();
                        varCaseTree = null;
                        sortDecls(defs);
                        newAlgebraics.put(tree, env);
                    }
                    tree = super.classEnter(tree, env);
                    if ((mods & ALGEBRAIC) != 0) {
                        if (varCaseTree != null)
                            caseClasses.append(varCaseTree);
                        caseClasses.prepend(tree);
                        newAlgebraics.put(tree, env);
                        tree = trees.Container(caseClasses.toArray());
                        caseClasses = null;
                        varCaseTree = null;
                    }
                // handle class fields
                } else {
                    Definition owner = env.info.scope.owner;
                    Definition enclClass = owner.enclClass();
                    Name fullname = mangler.formFullName(name, enclClass);
                    if (accountant.compiled.get(fullname) != null) {
                        report.error(tree.pos, "duplicate.class", mangler.unmangle(fullname));
                        Name newname = Name.fromString(name + "$_$" + accountant.compiled.size());
                        ((ClassDecl)tree).name = newname;
                        fullname = mangler.formFullName(newname, enclClass);
                    }
                    accountant.compiled.put(fullname, fullname);
                    ClassDef c = def;
                    if (def == null) {
                        c = (ClassDef)definitions.defineClass(fullname);
                        c.completer = null;
                        // check legal combinations
                        int missing = 0;
                        if ((mods & FINAL) != 0) {
                        	missing |= FINAL;
                        	mods &= ~FINAL;
                        }
                        c.modifiers = modifiers.checkMods(
                            tree.pos,
                            mods,
                            FINAL | ABSTRACT | PUBLIC | PRIVATE | OPAQUE,
                            true) | CLASSFIELD;
                        c.modifiers |= missing;
                        if ((c.modifiers & PRIVATE) != 0)
                        	c.modifiers = (c.modifiers & ~PRIVATE) | PROTECTED;
                        else
                        	c.modifiers |= PUBLIC;
                        if ((c.modifiers & (FINAL | OPAQUE)) == (FINAL | OPAQUE))
                            report.error(tree.pos,
                                "illegal.mod.combination", "final", "opaque");
                    }
                    c.owner = owner;
                    c.sourcefile = env.toplevel.info.source.getName();
                    c.setLocals(new Scope(null, c));
                    env.toplevel.def.locals().enterIfAbsent(c);
                    mangler.put(c.fullname, (ClassDef)owner.enclClass(), name, c.modifiers);
                    Definition  proxy = c.proxy(name);
                    Scope enclscope = accountant.enterScope(env);
                    if (checkUnique(tree.pos, proxy, enclscope)) {
                        enclscope.enter(proxy);
                        checkNoDuplicate(tree.pos, c);
                    }
                    tree.setDef(c);
                    accountant.todo.put(c, tree);
                    ContextEnv localEnv = env.dup(tree, env.info, c.locals());
                    localEnv.enclClass = (ClassDecl)tree;
                    Tree[] decls = localEnv.enclClass.members;
                    if (decls != null)
                        for (int i = 0; i < decls.length; i++)
                            decls[i] = classEnter(decls[i], localEnv);
                }
                return tree;
            case CaseDecl(Name name, int mods, VarDecl[] fields, _, ClassDef def):
                XClassDef algebraic = (XClassDef)env.info.scope.owner;
                XClassDef owner = (XClassDef)algebraic.owner;
                //System.out.println("module " + owner + " defines algebraic class " + algebraic);
                ContextEnv outerEnv = (ContextEnv)newAlgebraics.get(env.enclClass);
                if (fields == null) {
                    if (varCaseTree == null)
                        varCaseTree = super.classEnter(variableClass(tree.pos, algebraic), outerEnv);
                    caseClasses.append(tree);
                    varCaseTags.put(tree, new BaseAndTag(algebraic.tag++, algebraic));
                    return null;
                }
                trees.pushPos(tree.pos);
                Name fullname = mangler.formFullName(name, owner);
                if ((accountant.compiled.get(fullname) != null) || (name == owner.name)) {
                    if (name == owner.name)
                        report.error(tree.pos, "illegal case name");
                    else
                        report.error(tree.pos,
                            "duplicate class: " + mangler.unmangle(fullname));
                    Name  newname = Name.fromString(name + "$$$" +
                                        accountant.compiled.size());
                    ((CaseDecl)tree).name = newname;
                    fullname = mangler.formFullName(newname, owner);
                }
                accountant.compiled.put(fullname, fullname);
                XClassDef c = (XClassDef)def;
                if (def == null) {
                    c = (XClassDef)definitions.defineClass(fullname);
                    c.completer = null;
                    c.modifiers = modifiers.checkMods(
                                    tree.pos, mods, modifiers.InnerClassMods, true);
                    c.modifiers |= SYNTHETIC | CASEDEF;
                    if ((algebraic.modifiers & STRICTFP) != 0)
                        c.modifiers |= STRICTFP;
                }
                c.owner = owner;
                c.sourcefile = env.toplevel.info.source.getName();
                c.setLocals(new Scope(null, c));
                c.baseClass = algebraic;
                owner.locals().enterIfAbsent(c);
                mangler.put(c.fullname, owner, name, c.modifiers);
                Definition proxy = c.proxy(name);
                Scope enclscope = accountant.enterScope(outerEnv);
                if (checkUnique(tree.pos, proxy, enclscope)) {
                    enclscope.enter(proxy);
                    checkNoDuplicate(tree.pos, c);
                }
                tree.setDef(c);
                caseClasses.append(
                	tree = caseToInner(tree.pos, fields, (XClassDef)c, algebraic));
                accountant.todo.put(c, tree);
                trees.popPos();
                return null;
            default:
                return super.classEnter(tree, env);
        }
    }
}

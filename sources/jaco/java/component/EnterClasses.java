//      /   _ _      JaCo
//  \  //\ / / \     - first pass of semantic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 24/01/99

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import Definition.*;


public class EnterClasses extends Processor
                          implements ModifierConst, DefinitionConst,
                                     OperatorConst, TreeConst, TypeConst
{
/** helper components
 */
    protected Mangler       mangler;
    protected NameResolver  namer;
    protected ClassReader   reader;
    protected Accountant    accountant;
    
/** language components
 */
    protected Modifiers     modifiers;
    protected Types         types;
    protected Definitions   definitions;
    protected Trees         trees;
    
/** number of anonymous classes
 */
    protected int           nextclassnum;

/** handle nested classes
 */
    protected boolean       classesNest;
    

/** component name
 */
    public String getName()
    {
        return "JavaEnterClasses";
    }

    public String getDescription()
    {
        return "entering classes";
    }
    
    public String getDebugName()
    {
        return "enterclass";
    }
    
/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context.compilerContext);
        MainContext mainContext = context.compilerContext.mainContext;
        mangler = mainContext.Mangler();
        reader = mainContext.ClassReader();
        namer = mainContext.NameResolver();
        modifiers = mainContext.Modifiers();
        types = mainContext.Types();
        definitions = mainContext.Definitions();
        trees = mainContext.Trees();
        accountant = context.Accountant();
        
        nextclassnum = 0;
        classesNest = mainContext.classesNest;
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
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return !info.attributed;
    }
    
/** form full name of an anonymous class
 */
    public Name formAnonClassName()
    {
        return Name.fromString("");
    }
    
    public Name formAnonFullName(Name shortname, Definition enclClass)
    {
        String s = enclClass.fullname + "$" + (nextclassnum++);
        if ((shortname != null) && (shortname.length() > 0))
            s = s + "$" + shortname;
        return Name.fromString(s);
    }
    
    public boolean checkUnique(int pos, Definition def, Scope s)
    {
        if ((def.name == null) || (def.name.length() == 0))
            return true;
        Definition  e = s.lookup(def.name);
        while (e.scope == s)
        {
            if ((e.def != def) &&
                (e.def.kind == def.def.kind) &&
                (def.name != PredefConst.ERROR_N) &&
                ((e.def.kind != FUN) || types.sametype(e.def.type, def.def.type)))
            {
                report.error(pos, "duplicate.def", def + def.location());
                return false;
                }
            e = e.next();
        }
        return true;
    }

/** check that class does not have the same name as one of
 *  its enclosing classes
 */
//todo: this does not work yet, but it is not needed anyway
    public void checkNoDuplicate(int pos, ClassDef c)
    {
        Definition  oc = c.owner;
        while (oc != null)
        {
            if ((oc.kind == TYP) && (oc.name == c.name))
            {
                report.error(pos, "classname.equals.outer");
                break;
            }
            oc = oc.owner;
        }
    }
    
    public Tree classEnter(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        switch (tree)
        {
            case ClassDecl(Name name, int mods, _, _, Tree[] defs, ClassDef def):
                Definition  owner = (env.info.scope == null) ?
                                        env.toplevel.def :
                                        env.info.scope.owner;
                Definition  enclClass = owner.enclClass();
                Name        fullname = (((owner.kind == TYP) ||
                                         (owner.kind == PCK)) &&
                                         (name != null) &&
                                         (name.length() > 0)) ?
                                        mangler.formFullName(name, enclClass) :
                                        formAnonFullName(name, enclClass);
                if (classesNest && (accountant.compiled.get(fullname) != null))
                {
                    report.error(tree.pos, "duplicate.class", mangler.unmangle(fullname));
                    Name    newname = Name.fromString(name + "$_$" + accountant.compiled.size());
                    ((ClassDecl)tree).name = newname;
                    fullname = mangler.formFullName(newname, enclClass);
                }
                accountant.compiled.put(fullname, fullname);
                ClassDef    c = def;
                if (def == null)
                {
                    c = (ClassDef)definitions.defineClass(fullname);
                    c.completer = null;
                    c.modifiers = modifiers.checkMods(
                                    tree.pos, mods, c.isLocal() ?
                                        modifiers.LocalClassMods :
                                        (owner.kind == TYP) ?
                                            modifiers.InnerClassMods :
                                            modifiers.ClassMods, true);
                    if (env.info.isStatic)
                        c.modifiers |= STATIC;
                    if ((owner.modifiers & STRICTFP) != 0)
                        c.modifiers |= STRICTFP;
                }
                c.owner = owner;
                
//ISSUE: are class members of interfaces allowed?
//                  if ((c.owner.modifiers & INTERFACE) != 0)
//                      ((ClassDecl)tree).mods = mods = mods | PUBLIC | STATIC;
                c.sourcefile = env.toplevel.info.source.getName();
                c.setLocals(new Scope(null, c));
                env.toplevel.def.locals().enterIfAbsent(c);
                if (owner.kind != PCK)
                {
                    mangler.put(c.fullname, (ClassDef)owner.enclClass(), name, c.modifiers);
                    Definition  proxy = c.proxy(name);
                    Scope enclscope = accountant.enterScope(env);
                    if (checkUnique(tree.pos, proxy, enclscope))
                    {
                        enclscope.enter(proxy);
                        checkNoDuplicate(tree.pos, c);
                    }
                }
                else
                if (!classesNest)
                {
                    Mangler.Mangle  info = mangler.get(c.fullname);
                    if (info != null)
                        info.owner.locals().enterIfAbsent(c.proxy(info.name));
                }
                tree.setDef(c);
                accountant.todo.put(c, tree);
                
                ContextEnv localEnv = env.dup(tree, env.info, c.locals());
                localEnv.enclClass = (ClassDecl)tree;
                TreeList newdecls = new TreeList();
                Tree[] decls = localEnv.enclClass.members;
                for (int i = 0; i < decls.length; i++) {
                    Tree res = classEnter(decls[i], localEnv);
                    if (res != null) {
                        Tree[] ts = trees.getContainerContent(res);
                        if (ts == null)
                            newdecls.append(res);
                        else
                            newdecls.append(ts);
                    }
                }
                localEnv.enclClass.members = newdecls.toArray();
                break;
        }
        trees.popPos();
        return tree;
    }
}

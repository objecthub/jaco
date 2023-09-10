//      /   _ _      JaCo
//  \  //\ / / \     - second pass of semantic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import Tree.*;
import Definition.*;


public class ImportClasses extends Processor
                           implements ModifierConst, OperatorConst, TreeConst,
                                      DefinitionConst, TypeConst
{
/** helper components
 */
    public Mangler          mangler;
    public NameResolver     namer;
    public ClassReader      reader;
    public Classfiles       classfiles;
    public Attribute        attribute;
    public Accountant       accountant;
    
/** language components
 */
    public Modifiers        modifiers;
    public Definitions      definitions;
    public Trees            trees;


/** component name
 */
    public String getName()
    {
        return "JavaImportClasses";
    }

    public String getDescription()
    {
        return "importing classes";
    }
    
    public String getDebugName()
    {
        return "import";
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
        definitions = mainContext.Definitions();
        trees = mainContext.Trees();
        classfiles = mainContext.Classfiles();
        attribute = context.Attribute();
        accountant = context.Accountant();
    }
    
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        trees.pushPos(tree.pos);
        ContextEnv  localEnv = accountant.ContextEnv(tree);
        accountant.topEnvs.put(tree.info.source.toString(), localEnv);
        importStandardPackages(localEnv);
        int i = 0;
        while ((i < tree.decls.length) && prologue(tree.decls[i], localEnv))
            i++;
        trees.popPos();
        return tree;
    }
    
    protected boolean needsProcessing(CompilationEnv info)
    {
        return !info.attributed;
    }
    
/** import a class/all classes in a package
 */
    public void include(Scope scope, Definition def)
    {
        for (Definition e = scope.lookup(def.name); e.scope == scope; e = e.next())
            if ((e.def.kind == def.def.kind) && (e.def.fullname == def.def.fullname))
                return;
        scope.enter(def);
    }
    
    public void includeAll(Scope inscope, ContextEnv env, PackageDef p)
    {
        Scope s = reader.directory(p);
        if (s != null)
        {
            for (Definition e = s.elems; e != null; e = e.sibling)
                include(inscope, e.def);
        }
        else
            report.warning(trees.getPos(), "cannot access directory " +
                           classfiles.externalizeFileName(p.fullname));
    }
    
    public void includeAll(Scope inscope, ContextEnv env, Definition c, int kinds)
    {
        Scope s = c.locals();
        for (Definition e = s.elems; e != null; e = e.sibling)
        {
            if (((e.def.kind & ~kinds) == 0) && namer.accessible(env, c, e.def))
                include(inscope, imported(e.def));
        }
    }
    
    public void includeNamed(Scope inscope, ContextEnv env, Definition c,
                    Name name, int kinds)
    {
        Scope   s = c.locals();
        boolean found = false;
        for (Definition e = s.lookup(name); e.scope == s; e = e.next())
        {
            if (((e.def.kind & ~kinds) == 0) && namer.accessible(env, c, e.def))
            {
                include(inscope, imported(e.def));
                found = true;
            }
        }
        if (!found && (c.type != Type.ErrType))
            report.error(trees.getPos(), "not.found.in", name, c);
    }
    
    public Definition imported(Definition def)
    {
        if (def.kind == TYP)
        {
            Mangler.Mangle  info = mangler.get(def.fullname);
            if (info != null)
            {
                def = def.proxy(info.name);
                def.modifiers |= STATIC;
            }
        }
        return def;
    }

    public void importPackage(Name packge, ContextEnv localEnv)
    {
        Definition p = namer.resolvePackage(trees.getPos(), packge, localEnv);
        if (p.kind == PCK)
            includeAll(localEnv.toplevel.importScope[STAR_SCOPE], localEnv, (PackageDef)p);
    }
    
    public void importStandardPackages(ContextEnv localEnv)
    {
        importPackage(PredefConst.JAVA_LANG_N, localEnv);
    }
    
    public boolean prologue(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        boolean result;
        switch (tree)
        {
            case PackageDecl(Tree qualid):
                    result = true;
                    break;
        
            case Import(int tag, Tree qualid):
                if (tag == IMPORT)
                {
                    switch (qualid)
                    {
                        case Select(Tree selected, Name selector, _):
                                Definition p = attribute.attribExpr(selected, env,
                                                        TYP | PCK, Type.AnyType).tdef();
                            if (p.kind == PCK)
                            {
                                Definition  c = attribute.attribType(qualid, env).tdef();
                                if (c.kind == TYP)
                                    include(env.toplevel.importScope[NAMED_SCOPE], c);
                            }
                            else
                            if (p.kind == TYP)
                                includeNamed(env.toplevel.importScope[NAMED_SCOPE],
                                                env, p, selector, TYP);
                    }
                }
                else
                if (tag == IMPORTSTAR)
                {
                    Definition p = attribute.attribExpr(qualid, env,
                                                        TYP | PCK, Type.AnyType).tdef();
                    if (p.kind == PCK)
                        includeAll(env.toplevel.importScope[STAR_SCOPE], env, (PackageDef)p);
                    else
                    if (p.kind == TYP)
                        includeAll(env.toplevel.importScope[STAR_SCOPE], env, p, TYP);
                }
                result = true;
                break;
            
            default:
                result = false;
        }
        trees.popPos();
        return result;
    }
}

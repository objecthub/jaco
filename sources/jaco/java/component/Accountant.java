//      /   _ _      JaCo
//  \  //\ / / \     - access to global data for semantic analyzer
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;
import Tree.*;
import Definition.*;


public class Accountant extends Component
                        implements ModifierConst, DefinitionConst, TreeConst
{
/** language components
 */
    protected Types         types;
    protected Definitions   definitions;
    protected Trees         trees;
    
/** the set of all compiled classes
 */
    public Hashtable        compiled;
    
/** environment tables
 */
    public Hashtable        todo;
    public Hashtable        topEnvs;
    public Hashtable        classEnvs;

/** internal representation of the root class
 */
    public ClassDecl        rootClassDecl;


/** component name
 */
    public String getName()
    {
        return "JavaEnter";
    }
    
/** component initialization
 */
    public void init(SemanticContext context)
    {
        super.init(context);
    
        // init used components
        types = context.compilerContext.mainContext.Types();
        definitions = context.compilerContext.mainContext.Definitions();
        trees = context.compilerContext.mainContext.Trees();
        
        // init component
        compiled = new Hashtable();
        todo = new Hashtable();
        topEnvs = new Hashtable();
        classEnvs = new Hashtable();
        
        rootClassDecl = (ClassDecl)trees.make.ClassDecl(definitions.predefClass.name,
                                        PUBLIC, null, new Tree[0], new Tree[0]).
                                    setDef(definitions.predefClass);
    }
    
/** factory methods for creating contexts
 */
    public ContextEnv ContextEnv(CompilationUnit tree)
    {
        ContextEnv  env = new ContextEnv(tree);
        env.toplevel = tree;
        env.enclClass = rootClassDecl;
        return env;
    }

/** environment construction
 */
    public ContextEnv blockEnv(Tree tree, ContextEnv env, int mods)
    {
        ContextEnv  localEnv = env.dup(tree);
        Definition  prevOwner = env.info.scope.owner;
        localEnv.info.scope.owner = definitions.make.MethodDef(mods | BLOCK,
                                            prevOwner.name, null, prevOwner);
        localEnv.info.isStatic = env.info.isStatic || ((mods & STATIC) != 0);
        return localEnv;
    }

    public ContextEnv methodEnv(MethodDecl tree, ContextEnv env)
    {
        ContextEnv  localEnv = env.dup(tree);
        localEnv.enclMethod = tree;
        localEnv.info.scope.owner = tree.def;
        localEnv.info.isStatic = env.info.isStatic ||
                                 ((tree.mods & STATIC) != 0);
        return localEnv;
    }
    
    public ContextEnv classEnv(ClassDecl tree, ContextEnv env)
    {
        ContextEnv localEnv = env.dup(tree, env.info, new Scope(null, tree.def));
        localEnv.enclClass = tree;
        localEnv.outer = env;
        localEnv.info.isStatic = false;
        return localEnv;
    }

/** the environment to evaluate a variable's initializer
 */
    public ContextEnv initEnv(ContextEnv env, VarDecl vardef)
    {
        ContextEnv  ienv = env.dup(vardef, env.info, env.info.scope);
        ienv.info.isStatic = env.info.isStatic || ((vardef.mods & STATIC) != 0);
        return ienv;
    }
    
/** the scope in which a definition in environment env is to be entered
 */
    public Scope enterScope(ContextEnv env)
    {
        while (true)
        {
            switch (env.tree)
            {
                case CompilationUnit(_, PackageDef packge, _, _):
                    return packge.locals;
                    
                case ClassDecl(_, _, _, _, _, ClassDef def):
                    return def.locals();
                    
                case MethodDecl(_, _, _, _, _, _, _):
                case Case(_, _):
                case Catch(_, _):
                case Block(_, _):
                    return env.info.scope;
                
                default:
                    env = (ContextEnv)env.next;
            }
        }
    }
}

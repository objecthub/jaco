//      /   _ _      JaCo
//  \  //\ / / \     - environments for the tree processors
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.component.*;
import java.util.Vector;
import Tree.*;


public class Env
{
/** the next outer environment
 */
    public Env              next;

/** the environment enclosing the class
 */
    public Env              outer;

/** the tree to which this environment belongs
 */
    public Tree             tree;
    
    public CompilationUnit  toplevel;
    public ClassDecl        enclClass;
    public MethodDecl       enclMethod;
    

    public Env(Tree tree)
    {
        this.tree = tree;
    }
    
    public Env copyTo(Env env)
    {
        env.next = this;
        env.outer = outer;
        env.toplevel = toplevel;
        env.enclClass = enclClass;
        env.enclMethod = enclMethod;
        return env;
    }
    
    public Env dup()
    {
        return copyTo(new Env(tree));
    }
}

public class ContextEnv extends Env
{
    public ContextInfo  info;

    
    public ContextEnv(Tree tree)
    {
        this(tree, new ContextInfo());
    }
    
    public ContextEnv(Tree tree, ContextInfo info)
    {
        super(tree);
        this.info = info;
    }
    
    public ContextEnv(Tree tree, ContextInfo info, Scope scope)
    {
        super(tree);
        this.info = info.dup();
        this.info.scope = scope;
    }
    
    public ContextEnv(Tree tree, Scope scope)
    {
        super(tree);
        this.info = info.dup();
        this.info.scope = scope;
    }
    
    public Env dup()
    {
        return dup(tree, info, ((info != null) && (info.scope != null)) ? info.scope.dup() : null);
    }
    
    public ContextEnv dup(Tree tree)
    {
        return dup(tree, info, info.scope.dup());
    }
    
    public ContextEnv dup(Tree tree, ContextInfo info)
    {
        return (ContextEnv)copyTo(new ContextEnv(tree, info));
    }
    
    public ContextEnv dup(Tree tree, ContextInfo info, Scope scope)
    {
        return (ContextEnv)copyTo(new ContextEnv(tree, info, scope));
    }
    
    public void printscopes()
    {
        ContextEnv  env = this;
        while (env.outer != null)
        {
                System.out.println("------------------------------");
                env.info.scope.print();
                if (env.outer instanceof ContextEnv)
                    env = (ContextEnv)env.outer;
                else
                    break;
        }
    }
}

public class GenEnv extends Env
{
    public GenInfo  info;

    
    public GenEnv(Tree tree, GenInfo info)
    {
        super(tree);
        this.info = info;
    }
    
    public GenEnv(Tree tree, Coder coder)
    {
        this(tree, new GenInfo(coder));
    }
    
    public Env dup()
    {
        return dup(tree, info.dup());
    }
    
    public GenEnv dup(Tree tree)
    {
        return dup(tree, info);
    }
    
    public GenEnv dup(Tree tree, GenInfo info)
    {
        return (GenEnv)copyTo(new GenEnv(tree, info));
    }
}

public class InnerEnv extends Env
{
    public Vector       info;
    
    public InnerEnv(Tree tree)
    {
        this(tree, new Vector());
    }
    
    public InnerEnv(Tree tree, Vector info)
    {
        super(tree);
        this.info = info;
    }
    
    public Env dup()
    {
        return dup(tree, info);
    }
    
    public InnerEnv dup(Tree tree)
    {
        return dup(tree, info);
    }
    
    public InnerEnv dup(Tree tree, Vector info)
    {
        return (InnerEnv)copyTo(new InnerEnv(tree, info));
    }
}

public class ContextInfo
{
    public Scope        scope = null;
    public Type[]       reported = new Type[0];
    public Type[]       thrown = new Type[0];
    public boolean      isStatic = false;
    public boolean      isSelfCall = false;
    public boolean      isSelfArgs = false;
    public boolean      selectSuper = false;
    
    public ContextInfo()
    {}
    
    public ContextInfo(Scope scope)
    {
        this.scope = scope;
    }
    
    public ContextInfo dup()
    {
        ContextInfo info = new ContextInfo();
        info.scope = scope;
        info.reported = reported;
        info.isStatic = isStatic;
        info.isSelfCall = isSelfCall;
        info.isSelfArgs = isSelfArgs;
        info.selectSuper = selectSuper;
        return info;
    }
}

public class GenInfo
{
    protected Coder coder;
    public Chain    exit;
    public Chain    cont;
    
    
    public GenInfo(Coder coder)
    {
        this.coder = coder;
    }
    
    public void addExit(Chain c)
    {
        exit = coder.mergeChains(exit, c);
    }
    
    public void addCont(Chain c)
    {
        cont = coder.mergeChains(cont, c);
    }
    
    public GenInfo dup()
    {
        GenInfo info = new GenInfo(coder);
        return info;
    }
}

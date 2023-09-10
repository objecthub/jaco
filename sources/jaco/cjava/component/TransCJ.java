package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import CJTree.*;
import Definition.*;
import Type.*;
import Tree.*;

/**
 * Class used by the sub-translator to pass results
 * to the translator.
 */
public class TranslatedTree
{
    public Tree tree;

    public void reset(Tree _tree)
    {
    tree = _tree;
    }
    public TranslatedTree()
    {
        reset(null);
    }
}

/**
 * Translate a valid CJAVA tree into a standard java tree. 
 *
 * This class is almost empty. All it does is create the
 * subtranslators and call them. (The code for compounds
 * and aliases was becoming unmanagable...)
 * 
 * A subtranslator implements some translate###() methods. 
 * These work more or less the same as a translator's
 * method, with one difference: it must be possible for
 * a sub-translator's method to decide not to translate
 * a tree. Another sub-translator or the original translator
 * methods are called instead to create a translated version
 * of the tree.
 *
 * This translator relies on the CJCompounds.SubTranslator
 * and the CJAliases.SubTranslator.
 *
 * @see CJCompounds.SubTranslator
 * @see CJAliases.SubTranslator
 */
public class TransCJ extends jaco.java.component.Translator {

    /* other components */
    protected CJCompounds cjcompounds;
    protected CJAliases cjaliases;
    protected CJCompounds.SubTranslator cjcompoundsTrans;
    protected CJAliases.SubTranslator cjaliasesTrans;

    /** tt used by the subcomponents to pass results */
    protected TranslatedTree tt;
    
    /**
     * Initialization.
     *
     * Get the CJCompounds & the CJAliases objects. Create the subtranslators.
     */
    public void init(CompilerContext context)
    {
    super.init(context);

    CJMainContext maincontext = (CJMainContext)context.mainContext;

    cjcompounds = maincontext.CJCompounds();
    cjcompoundsTrans = cjcompounds.SubTranslator(context, this, newdef, redef);

    cjaliases = maincontext.CJAliases();
    cjaliasesTrans = cjaliases.SubTranslator(context, this, newdef, redef);

    tt = new TranslatedTree();
    }
    
    /** component name
     */
    public String getName()
    {
    return "TransCJ";
    }
    
    /** return descrition of translator
     */
    public String getDescription()
    {
    return "translating CJ";
    }

    /**  
     * Translate Declarations.
     *
     */
    protected Tree translateDecl(Tree tree, Env env) 
    {
    tt.reset(tree);
    if(cjaliasesTrans.translateDecl(tt.tree, env, tt) | cjcompoundsTrans.translateDecl(tt.tree, env, tt))
        return tt.tree;;
    return super.translateDecl(tt.tree, env);
    }
    
    /**
     * Translate Statements.
     *
     */
    protected Tree translateStat(Tree tree, Env env) 
    {
    tt.reset(tree);
    if(cjaliasesTrans.translateStat(tt.tree, env, tt) | cjcompoundsTrans.translateStat(tt.tree, env, tt))
        return tt.tree;
    return super.translateStat(tree, env);
    }
    
    /**
     * Translate Expressions.
     *
     */
    protected Tree translateExpr(Tree tree, Env env) 
    {
    tt.reset(tree);
    if(cjcompoundsTrans.translateExpr(tt.tree, env, tt))
        return tt.tree;
    return super.translateExpr(tt.tree, env);
    }
    
    /**
     * Translate type expressions.
     *
     */
    protected Tree translateType(Tree tree, Env env) 
    {
    tt.reset(tree);
    if(cjaliasesTrans.translateType(tt.tree, env, tt) | cjcompoundsTrans.translateType(tt.tree, env, tt))
        return tt.tree;
    return super.translateType(tt.tree, env);
    }

    public Catch[] transCatches(Catch[] ts, Env env)
    {
    return cjcompoundsTrans.transCatches(ts, env);
    }
}

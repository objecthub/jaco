/*
 * This class is used by other CJ components to handle aliases.
 *
 * CJAliases itself is a component, part of CJMainContext.
 *
 * CJAliases.Translator is a sub-stranslator used by CJTrans. It
 * is returned by CJAliases.Translator().
 */
package jaco.cjava.component;
import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import CJTree.*;
import CJDefinition.*;
import Type.*;
import Tree.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * This component contains a few methods that can
 * be useful everywhere. 
 *
 * There's also a sub-translator that TransCJ relies
 * on to translate trees containing compounds. It is created
 * using CJAliases.SubTranslator().
 *
 */
public class CJAliases extends Component
{
    /* other components  
     */
    protected Definitions definitions;
    protected Types types;
    protected Mangler mangler;


    /**
     * Check whether aliases are enabled.
     *
     */
    public boolean isEnabled()
    {
    return ((CJMainContext)context).useCJ();
    }

    /**
     * Disable aliases.
     *
     * Once the aliases have been disabled, it is not
     * possible to use alias again. After this method has
     * been called, some methods in CJAliases won't work
     * at all, and others will work incorrectly. 
     * 
     * Called by CJMainContext.disableCJ() ONLY !
     *
     * @see CJMainContext#disableCJ
     */
    public void disable()
    {
    }

    /** component name
     */
    public String getName()
    {
    return "CJAliases";
    }

    /** initialization
     */
    public void init(MainContext context)
    {
    super.init(context);
    definitions = context.Definitions();
    types = context.Types();
    mangler = context.Mangler();
    }

    /**
     * Create a new instance of the sub translator.
     *
     * @param context compiler context
     * @param translator the caller translator object
     * @param newdef factory to use to create new trees
     * @param redef factory to use to re-create trees.
     */
    public SubTranslator SubTranslator(CompilerContext context, Translator caller, Tree.Factory newdef, Tree.Factory redef)
    {
    if(!isEnabled())
        throw new InternalError("CJAliases.SubTranslator() used after CJAliases have been disabled");
    SubTranslator subtrans = new SubTranslator();
    subtrans.init(context, caller, newdef, redef);
    return subtrans;
    }


    /**
     * Translate trees using aliases to standard java code.
     *
     * - it translate aliases into standard types wherever they are used
     * 
     * - it convert aliases declaration into class declaration to
     *   export aliases and use them in another compilation. 
     *   (the fake classes representing aliases are empty classes 
     *    with a special attribute. other compiler will just
     *    see the empty class.)
     * 
     */
    public class SubTranslator
    {
    /* components  
     */
    protected Trees trees;
    protected Definitions definitions;
    protected Types types;
    protected Constants constants;
    protected CJCompounds cjcompounds;

    
    /* info about the caller
     */
    protected Translator translator;
    protected Tree.Factory newdef;
    protected Tree.Factory redef;

    /**
     * Initiazile the sub-translator.
     *
     * @param context compiler context
     * @param caller the parent translator
     * @param newdef the factory to use to create new trees
     * @param redef the factory to use to re-create trees
     */
    public void init(CompilerContext context, Translator caller, Tree.Factory _newdef, Tree.Factory _redef)
    {
        trees = context.mainContext.Trees();
        definitions = context.mainContext.Definitions();
        types = context.mainContext.Types();
        constants = context.mainContext.Constants();
        cjcompounds = ((CJMainContext)context.mainContext).CJCompounds();

        translator = caller;
        newdef = _newdef;
        redef = _redef;
    }

    /**
     * Translate AliasDecl() into a special class declaration.
     * @param tree the tree
     * @param env general environment
     * @result a redefined tree or null
     */
    public boolean translateDecl(Tree tree, Env env, TranslatedTree tt)
    {
        switch((CJTree)tree)
        {
        case AliasDecl(Name name, int mods, Tree realtypetree, Definition def):
            /* no reason to externalize private aliases */
            if( (mods&Modifiers.PRIVATE)!=0 )
            {
                tt.tree = null;
                return true; /* no tree set; simply remove it */
            }
            
            int newmods = (def.modifiers & (Modifiers.PUBLIC|Modifiers.PRIVATE|Modifiers.PROTECTED))/*|Modifiers.FINAL*/;
            if(def.owner.kind!=Definition.PCK)
            newmods|=Modifiers.STATIC;
            
            /* set the real type */
            Type classtype = types.make.ClassType(types.packageType);
            classtype.setDef(def);
            ((TwoFacedClassDef)def).setStandardType( classtype );
            ((TwoFacedClassDef)def).setSpecialType( ((CJClassDef)def).getRealType() );
            ((TwoFacedClassDef)def).special();

            ((CJClassDef)def).setAliasSignature(aliasSignature(realtypetree));

            Tree decl = newdef.ClassDecl(name, 
                         newmods,
                         null,
                         trees.noTrees,
                         trees.noTrees);
            tt.tree= trees.at(tree).make(decl);
            return true;
        }
        return false;
    }

    /**
     * Create a special signature for alias.
     *
     * This signature follows CJSignature syntax. It differs from
     * CJSignature.typeToSig() in that it doesn't follow aliases.
     *
     * @param typetree the tree that defines the alias
     */
    protected Name aliasSignature(Tree typetree)
    {
        StringBuffer buffer = new StringBuffer();
        aliasSignatureRecursive(typetree, buffer);
        return Name.fromString(buffer.toString());
    }

    private void aliasSignatureRecursive(Tree typetree, StringBuffer buffer)
    {
        switch((CJTree)typetree)
        {
        case CompoundType(Tree[] types):
            buffer.append('P');
            for(int i=0; i<types.length; i++)
            aliasSignatureRecursive(types[i], buffer);
            buffer.append(';');
            break;
            
        case Ident(_, Definition def):
            buffer.append('L');
            buffer.append(def.fullname.replace((byte)'.', (byte)'/').toString());
            buffer.append(';');
            break;

        case Select(_, _, Definition def):
            buffer.append('L');
            buffer.append(def.fullname.replace((byte)'.', (byte)'/').toString());
            buffer.append(';');
            break;
        }
    }

    /**
     * Remove local alias declarations.
     */
    public boolean translateStat(Tree tree, Env env, TranslatedTree tt)
    {
        switch((CJTree)tree)
        {
        case AliasDecl(_, _, _, _):
            /* not exported */
            tt.tree = null;
            return true;
        }
        return false;
    }


    /**
     * Do the translations necessary for implementing aliases.
     *
     * If it detects an alias used as a type, it translate it
     * into the corresponding standard type.
     * 
     * If the tree wasn't meant to be translated specially for
     * Aliases, return null.
     *
     * @param tree the tree
     * @param env general environment
     * @result a redefined tree or null
     */
    public boolean translateType(Tree tree, Env env, TranslatedTree tt)
    {
        switch((CJTree)tree)
        {
        case Ident(_, Definition def):      
            if(def instanceof CJClassDef)
            {
                CJClassDef c = (CJClassDef)def;
                if(c!=null && c.isAlias())
                {
                    tt.tree= trees.at(tree).make(trees.toTree(cjcompounds.defaultTypeFor(c.getRealType())));
                    return true;
                }
            }
            break;
        
        case Select(_, _, Definition def):
            if(def instanceof CJClassDef)
            {
                CJClassDef c = (CJClassDef)def;
                if(c!=null && c.isAlias())
                {
                    tt.tree= trees.at(tree).make(trees.toTree(cjcompounds.defaultTypeFor(c.getRealType())));
                    return true;
                }
            }
            break;
        }
        return false;
    }
    }
}

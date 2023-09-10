package jaco.cjava.component;
import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import CJTree.*;
import Definition.*;
import CJDefinition.*;
import Type.*;
import Tree.*;
import java.util.*;

/**
 * This component contains a few methods that can
 * be useful everywhere. 
 *
 * There's also a sub-translator on which TransCJ relies
 * on to translate trees containing compounds. 
 *
 * @see CJCompounds.SubTranslator
 */
public class CJCompounds extends Component
{
    /* other components  
     */
    protected Definitions definitions;
    protected Types types;
    protected CJErrorHandler error;

    /** component name
     */
    public String getName()
    {
    return "CJCompounds";
    }

    /* HACK: for CJType.tdef(); I'll remove it later */
    protected static CJCompounds instance;
    public static CJCompounds instance()
    {
    return instance;
    }
    public CJCompounds()
    {
    instance=this;
    }

    /** initialization
     */
    public void init(MainContext context)
    {
    super.init(context);
    definitions = context.Definitions();
    types = context.Types();
    error = (CJErrorHandler)context.ErrorHandler();
    }

    /** Create a new sub-translator for compounds.
     * 
     * @param context compiler context
     * @param translator the caller translator object
     * @param newdef factory to use to create new trees
     * @param redef factory to use to re-create trees.
     */
    public SubTranslator SubTranslator(CompilerContext context, Translator caller, Tree.Factory newdef, Tree.Factory redef)
    {
    SubTranslator subtrans;
    subtrans = new SubTranslator();
    subtrans.init(context, caller, newdef, redef);
    return subtrans;
    }

    /**
     * Transform a type, standard or compound, into
     * an array of types.
     *
     * @param original the type
     * @result an array of standard types (class and interfaces)
     */
    protected Type[] typeList(Type original)
    {
    Type[] list;
    switch((CJType)original.deref())
        {
        case CompoundType(_):
        Definition def = original.tdef();
        Type[] interfaces = def.interfaces();
        if(def.supertype()!=null)
            {
            list = new Type[ interfaces.length+1 ];
            list[0] = def.supertype();
            for(int i=1; i<=interfaces.length; i++)
                list[i] = interfaces[i-1];
            }
        else
            list = interfaces;
        break;
        default:
        list = new Type[1];
        list[0] = original;
        }
    return list;
    }

    /**
     * Return the default type for a compound type.
     *
     * The default type of a compound type is the one
     * that it will have in the non-compound version
     * of the code tree. 
     *
     * @param type a compound type
     * @result the standard type to use instead
     */
    public Type defaultTypeFor(Type type)
    {
    return defaultTypeFor(typeList(type));
    }

    /**
     * Return the type to use instead of an array of types.
     *
     * @param types a list of types (returned by typeList() !)
     * @result the standard type to use instead
     */
    private Type defaultTypeFor(Type[] types)
    {
    /* typeList() returns the types always in 
     * the same order : the class first (if any), and
     * the interfaces (sorted)
     *
     * No need to check anything; I just return the first element.
     *
    if(types.length>1)
        {
        for(int i=0; i<types.length; i++)
            {
            Definition def = types[i].tdef();
            if((def.modifiers & Definition.INTERFACE)==0)
                return types[i];
            }
        return types[0];
        }
    */
    return types[0];
    }

    /**
     * Return the type that an expression should
     * be cast to.
     *
     * It is used to check whether a compound type should
     * be casted to a more specific type before using it.
     *
     * @param goal the type the compound is used as (compound or standard)
     * @param candidate the type of the compound 
     * @see castExpr
     */
    protected Type realTypeFor(Type goal, Type candidate)
    {
    Type[] goals=typeList(goal);
    Type goals_default = defaultTypeFor(goal);
    Type[] candidates=typeList(candidate);
    Type candidates_default = defaultTypeFor(candidate);

    if(candidates.length==1 && goals.length==1)
        return null; /* no conversion */
    if(types.subtype(candidates_default, goals_default))
        return null;

    for(int i=0; i<goals.length; i++)
        {
        for(int j=0; j<candidates.length; j++)
            {
            if(types.subtype(candidates[j], goals[i]))
                {
                return goals_default;
                }
            }
        }
    throw new InternalError("real type for " + goal.toString() + " not found in " + candidate.toString());
    }

    /**
     * Check whether the type uses compounds.
     *
     * @param type the type to check
     * @result true if it's a compound type or an array of compounds
     */
    public boolean usesCompounds(Type t)
    {
    while(true)
        {
        switch((CJType)t.deref())
            {
            case CompoundType(_):
            return true;
            
            case ArrayType(Type elem):
            t = elem;
            break;
            
            default:
            return false;
            }
        }
    }

    /**
     * Check whether an array of type uses compounds.
     *
     * @param type the type to check
     * @result true if one of the types in the array uses compounds
     */
    public boolean usesCompounds(Type[] t)
    {
    if(t!=null)
        for(int i=0; i<t.length; i++)
        {
            if(usesCompounds(t[i]))
            return true;
        }
    return false;
    }

    static final Name ALTNAME_SEPARATOR = Name.fromString("$cj$");
    public Name alternateNameFor( MethodDef def )
    {
    return alternateNameFor(def, 0);
    }
    public Name alternateNameFor( MethodDef def, int num )
    {
    Name signature = methodSignature(def, false);
    return def.name.append(ALTNAME_SEPARATOR).append(Name.fromString(""+Integer.toHexString(signature.toString().hashCode()+num)) );
    }

    public Name methodSignature(Definition def, boolean nocompounds)
    {
    StringBuffer str = new StringBuffer("");
    switch(def.type)
        {
        case MethodType(Type[] args, Type result, _):
        if(nocompounds)
            result = defaultTypeFor(result);
        else
            result.tdef();
        str.append(result);
        str.append(' ');
        str.append(def.name.toString());
        str.append('(');
        if(args!=null)
            for(int i=0; i<args.length; i++)
            {
                Type t = args[i];
                if(nocompounds)
                t = defaultTypeFor(t);
                else
                t.tdef();
                str.append(';');
                str.append(t.toString());
            }
        break;
        
        default:
        throw new InternalError(def.type.toString());
        }
    return Name.fromString(str.toString());
    }

    public Type convertToDefault(Type t)
    {
    FlatArray flat = new FlatArray(t);
    return flat.remake(flat.defaultElemType());
    }

    public Type[] convertArrayToDefault(Type[] at)
    {
    int l = (at==null) ? 0:at.length;
    Type[] array = new Type[l];
    for(int i=0; i<l; i++)
    {
        array[i] = convertToDefault(at[i]);
    }
    return array;
    }


    /**
     * Create a fake definition for a compound type
     *
     * If the type list contains an invalid type,
     * no checks are performed (and no exceptions
     * are thrown.)
     *
     * the typelist may contain compounds.
     *
     * @param compound the compound
     * @param typelist the list of types in the compound
     * @param nochecks if true, no checks are done
     * @result a definition
     * @exception InvalidCompoundException
     */
    protected CompoundDef fakeDefinition(Type compound, Type[] components, boolean nochecks) throws InvalidCompoundException
    {
    Vector interfaces = new Vector(components.length);
    Type supertype = null;
    boolean erroneous = nochecks;
    
    int classes_n = 0;
    for(int i=0; i<components.length; i++)
        {
        /* special case; make sure aliases have been loaded and follow them. */
        Definition mdef = components[i].tdef();
        mdef.complete();
        components[i] = mdef.type; 

        switch((CJType)components[i].deref())
            {
            case ClassType(_):
            {
                Definition cdef = components[i].deref().tdef();
                if( (cdef.completer==null && (cdef.modifiers & ModifierConst.INTERFACE) == 0))
                {
                    classes_n ++;
                    if(classes_n>1)
                    components[i] = Type.ErrType;
                    else
                    supertype = components[i];
                    /* error is printed later */
                }
                else
                interfaces.add(components[i]);
            }
            break;
            
            case CompoundType(Type[] subcomponents):
            Definition def = components[i].tdef();

            if(def==null)
                components[i].setDef(def=fakeDefinition(components[i], subcomponents, true));
            if(components[i].erroneous())
                erroneous = true;

            if(def.supertype() != null)
                {
                classes_n ++;
                if(classes_n>1)
                    components[i] = Type.ErrType;
                else
                    {
                    supertype = def.supertype();
                    interfaces.addAll(Arrays.asList(def.interfaces()));
                    }
                }
            else
                interfaces.addAll(Arrays.asList(def.interfaces()));
            break;
            
            case ErrType:
            erroneous = true;
            break;
            
            default:
            throw new InternalError("Found a " + components[i].toString() + " in a compound type.");
            
            }
        }

    Collections.sort(interfaces, TypeComparator());


    /* remove duplicates in a sorted list */
    ListIterator iter = interfaces.listIterator();
    if(iter.hasNext())
    {
        Type prev = (Type)iter.next();
        while(iter.hasNext())
        {
            Type t = (Type)iter.next();
            if(types.sametype(prev, t))
            {
                iter.remove();
            }
            else 
            prev=t;
        }
    }

    /* no supertype => Object, so remove object if it's been given explicitely unless it's
    * the only member of the compound.
    */
    if(supertype!=null && types.sametype(supertype, types.objectType) && interfaces.size()>0)
        supertype = null;
    
    Type[] interfaceArray = new Type[interfaces.size()];
    interfaces.toArray(interfaceArray);

    Definition def = 
        ((CJDefinition.Factory)definitions.make).CompoundDef(
                                 null,
                                 (CJType.CompoundType)compound,
                                 supertype,
                                 interfaceArray,
                                 definitions.emptyPackage);
    def.name = Name.fromString(compound.toString());
    def.fullname = def.name;

    if(classes_n>1 && !erroneous)
        throw new InvalidCompoundException("invalid.compound");

    return (CompoundDef)def;
    }


    /**
     * Create a fake definition for a compound type
     *
     * If the type list of the compound contains an invalid type,
     * no checks are performed (and no exceptions
     * are thrown.)
     *
     * @param compound the compound
     * @result a definition
     * @exception InvalidCompoundException
     */
    public CompoundDef fakeDefinition(Type compound) throws InvalidCompoundException
    {
    switch((CJType)compound)
        {
        case CompoundType(Type[] tlist):
        return fakeDefinition(compound, tlist);
        
        default:
        throw new InternalError("not a compound " + compound.toString());
        }
    }


    public CompoundDef fakeDefinition(Type compound, Type[] components) throws InvalidCompoundException
    {
    return fakeDefinition(compound, components, false);
    }

   public CompoundDef fakeDefinitionNoChecks(Type compound, Type[] components) 
    {
    try
        {
        return fakeDefinition(compound, components, true);
        }
    catch(InvalidCompoundException e)
        {
        throw new InternalError();
        }
    }

    /**
     * InvalidCompoundException.
     * 
     * Contains a message key to be used to display some information
     * to the user.
     */
    public static class InvalidCompoundException extends Exception 
    {
    String key;
    public InvalidCompoundException(String _key)
    {
        super("Message key: " + _key);
        key = _key;
    }
    
    public String getMessageKey()
    {
        return key;
    }
    }

    /**
     * Disable compounds. 
     *
     * Called by CJMainContext ONLY.
     *
     * This method can be called only once.
     *
     * @see CJMainContext#disableCJ
     */
    public void disable()
    {
    /* ignore exceptions now */
    error.ignore("unreported.exception");

    /* restore modified definitions */
    TwoFacedRegistery twofaced = ((CJDefinitions)definitions).TwoFaced();
    twofaced.standard();

    /* and save them in a map, for the classwriter */
    Iterator iter = twofaced.iterator();
    while(iter.hasNext())
    {
        TwoFacedDefinition def = (TwoFacedDefinition)iter.next();
        if(def instanceof TwoFacedMethodDef)
        {
            Name std_sig = methodSignature((MethodDef)def, false /* there are no compounds in standard definitions, anyway */);
            Name prefix=((Definition)def).owner.fullname;
            prefix = prefix.append(Name.fromString("."));
        }
    }
    }

    /**
     * Translate trees using compounds to standard java code.
     *
     * How compound types are translated:
     *
     *  The main idea is to give variables that are compounds any non-compound type 
     *  and place casts depending on how the compound type is used. The default type
     *  is chosen by the method CJCompounds.defaultTypeFor.
     *
     *  For example, a variable of type [A1, A2] (A1 and A2 being two interfaces) will
     *  be set the type A1 in the translated version of the tree and casts will be
     *  added to the tree whenever the variable is used as an object implementing
     *  the interface A2.
     *
     *  Most of the work is really done by the castExpr() method, which decides
     *  when a cast should be added. 
     *
     *  See the source for comments about translating catch expressions and the handling
     *  of temporary variables.
     * 
     * The sub translator should always be created using the method CJCompounds.SubTranslator().
     */
    public class SubTranslator
    {
    /* components  
     */
    protected Trees trees;
    protected Definitions definitions;
    protected Types types;
    protected Constants constants;
    protected CJClassWriter classwriter;
    protected NameResolver namer;

    
    /* info about the caller
     */
    protected Translator translator;
    protected Tree.Factory newdef;
    protected Tree.Factory redef;


    
    /** component name
     */
    public String getName()
    {
        return "CJCompounds";
    }
    
    /** component initialization
     */
    public void init(CompilerContext context, Translator caller, Tree.Factory _newdef, Tree.Factory _redef)
    {
        trees = context.mainContext.Trees();
        definitions = context.mainContext.Definitions();
        types = context.mainContext.Types();
        constants = context.mainContext.Constants();
        classwriter = (CJClassWriter)context.ClassWriter();
        namer = context.mainContext.NameResolver();

        translator = caller;
        newdef = _newdef;
        redef = _redef;
    }


    /**
     * Do the translations necessary for implementing compounds.
     *
     * If the tree wasn't meant to be translated specially for
     * Compounds, return null.
     *
     * @param tree the tree
     * @param env general environment
     * @result a redefined tree or null
     */
    public boolean translateType(Tree tree, Env env, TranslatedTree tt)
    {
        switch((CJTree)tree)
        {
        case CompoundType(Tree[] types):
            tt.tree = translator.transType(trees.toTree(defaultTypeFor(tree.type)), env);
            return true;
        }
        return false;
    }
    
    /** Create a class environment.
     * copied from Translator */
    protected Env classEnv(ClassDecl tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        localEnv.enclClass = tree;
        localEnv.enclMethod = null;
        localEnv.outer = env;
        return localEnv;
    }

    /** Create an environment.
     * copied from Translator */
    protected Env initialEnv(Tree tree)
    {
        return new Env(tree);
    }

    /** Create a sub-block environment.
     * copied from Translator */
    protected Env blockEnv(Tree tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        return localEnv;
    }

    /** Create a method environment.
     * copied from Translator */
    protected Env methodEnv(MethodDecl tree, Env env)
    {
        Env localEnv = env.dup();
        localEnv.tree = tree;
        localEnv.enclMethod = tree;
        return localEnv;
    }

    /**  
     * Translate Declarations.
     *
     * Compound Types: check whether a cast is needed 
     * in variable initialization
     */
    public boolean translateDecl(Tree tree, Env env, TranslatedTree tt) 
    {
        if(tree==null)
        return false;

        switch ((CJTree)tree) {
        
        case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
        if(init!=null)
            {              
            tt.tree = trees.at(tree).make(
                           redef.VarDecl(name, mods, translator.transType(vartype, env),
                                 castExpr(vartype.type, init.type, translator.transExpr(init, env))));
            return true;
            }
        break;
        
        /* compound types in declaration */
        case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] members, _):
        if(implementing!=null)
            {
            TreeList treelist = new TreeList();
            for(int i=0; i<implementing.length; i++)
                {
                CJType type = (CJType)implementing[i].type.tdef().type;
                switch(type)
                    {
                    case CompoundType(_):
                    Type[] interfaces = type.tdef().interfaces();
                    for(int j=0; j<interfaces.length; j++)
                        treelist.append(trees.toTree(interfaces[j]));
                    break;

                    default:
                    treelist.append(translator.transType(implementing[i], env));
                    }
                }
            implementing = treelist.toArray();
            }


        tt.tree= trees.at(tree).make(
                       redef.ClassDecl(name, mods, translator.transType(extending, env),
                               implementing,
                               translator.transDecls(members,
                                         classEnv((ClassDecl)tree, env))));
        
        return true;
        

        case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params, Tree[] thrown, Tree[] stats, MethodDef def):
        TwoFacedMethodDef m = (TwoFacedMethodDef)def;
        
        /* for temporary names, see at the end of the source file */
        if (stats != null)
            {
            stats = translator.transStats(stats, methodEnv((MethodDecl)tree, env));
            stats = trees.append(temporaryDecls(), stats);
            }

        tt.tree= trees.at(tree).make(
                       redef.MethodDecl(m.getStandardName(), mods,
                                translator.transType(restype, env),
                                translator.transVarDecls(params, env),
                                translator.transTypes(thrown, env),
                                stats));

        return true;
        }
        return false;
    }

    /**
     * Translate Statements.
     *
     * Compound Types: check whether a cast is needed in the return statement
     */
    public boolean translateStat(Tree tree, Env env, TranslatedTree tt) 
    {
        if(tree==null)
        return false;

        switch (tree) {
        case Return(Tree expr, MethodDecl(_, _, Tree restype, _, _, _, _)):
        if(expr!=null)
            {
            tt.tree = trees.at(tree).make(redef.Return(castExpr(restype.type, expr.type, translator.transExpr(expr, env))));
            return true;
            }
        }
        return false;
    }
    
    /**
     * Translate Expressions.
     *
     * Compound Types: 
     *     Select & Assign    : add a cast, if necessary
     *     Typeop(instanceof) : expand to a series of instanceof statements
     */
    public boolean translateExpr(Tree tree, Env env, TranslatedTree tt) 
    {
        if(tree==null)
        return false;

        switch ((CJTree)tree) {

        case Ident( Name name , Definition def):
        Name altname = redefMethodName(def);
        if(altname!=null)
            name = altname;
        tt.tree = trees.at(tree).make(redef.Ident(name));
        return true;

        case Select( Tree obj, Name selector, Definition def):
        Type enclType = def.enclClass().type;
        Name altname = redefMethodName(def);
        if(altname!=null)
            selector=altname;
        tt.tree= trees.at(tree).make( redef.Select( castExpr(enclType, obj.type, translator.transExpr(obj, env)), 
                              selector) );
        return true;

        case Apply(Tree fn, Tree[] args):
        Tree[] targs = null;
        Type ftype = fn.type.deref();
        Type[] formals = ((MethodType)ftype).argtypes;
        
        if(args!=null)
            {
            targs = new Tree[args.length];
            for(int i=0; i<args.length; i++)
                {
                Type t=formals[i].deref();
                targs[i] = translator.transExpr(args[i], env);
                targs[i] = castExpr(defaultTypeFor(t), args[i].type, targs[i]);
                }
            }
        tt.tree= trees.at(tree).make(
                         redef.Apply(translator.transExpr(fn, env), targs));
        return true;

        case Assign(Tree lvalue, Tree rvalue):
        tt.tree  = trees.at(tree).make(
                       redef.Assign(translator.transExpr(lvalue, env), 
                            castExpr(lvalue.type, rvalue.type, translator.transExpr(rvalue, env))) );
        return true;

        case Typeop(OperatorConst.TYPETEST, Tree expr, Tree type):
        if(usesCompounds(type.type))
            {
            tt.tree= trees.at(tree).make( newInstanceOf(translator.transExpr(expr, env), type.type, env));
            return true;
            }
        break;

        case Typeop(OperatorConst.TYPECAST, Tree expr, Tree type):
        {
            FlatArray flat = new FlatArray(type.type.deref());
            switch((CJType)flat.elem)
            {
            case CompoundType(_):
                {
                
                /* cast to everything, so that a ClassCastException is
                 * thrown if one fails.
                 *
                 * Cast from the last element to the first; since the first
                 * element in the list returned by typeList is the default, 
                 * it ensures that the type is cast to the default type last.
                 */
                Type[] tlist = typeList(flat.elem);
                
                expr = translator.transExpr(expr, env);
                for(int i=tlist.length-1; i>=0; i--)
                    {
                    Type tl = flat.remake(tlist[i]);
                    expr = newdef.Typeop(OperatorConst.TYPECAST, expr, trees.toTree(tl));
                    }
                
                tt.tree=trees.at(tree).make( expr );
                return true;
                }
            }
        }
        break;
        }
        return false;
    }

    /**
     * Return the redefined method name, if necessary.
     *
     * This method checks whether the standard and 
     * special method names are different, and if they
     * are, it modifies the tree.
     *
     * @param def the definition of the tree
     * @result a new method name, or null
     */
    protected Name redefMethodName(Definition def)
    {
        if(def instanceof TwoFacedMethodDef)
        {
            TwoFacedMethodDef m = (TwoFacedMethodDef)def;
            Name realname = m.getStandardName();
            if(!realname.equals(m.getSpecialName()))
            return realname;
        }
        return null;
    }

    /**
     * Create a new instanceof operation, but only if necessary.
     *
     * This operation can be used on compound as well as
     * on standard types; it expands to a series of
     * instanceof expression.
     *
         * WARNING:
         *   It is a very specific version, rather different from newInstanceOf():
     *   <ul>
     *   <il> it does not support arrays of compounds<
     *   <il> it sometimes returns null, which can be confusing
     *   <il> it never generates temporary variables, rather, it assumes
     *         that the expression may be safely copied. (meaning, it must
     *         be a local variable)
     *   </ul>
     *
     * @param expr the expression, an Ident pointing to a local variable
     * @param type the type to check (expr instanceof type)
     * @param expr_type expr.type the (compile-time) type of expr. (may not be null)
     * @return a Tree, may be null if no check is necessary
     * @see CJCompounds#newInstanceOf
     */
    public Tree newInstanceOfIfNeeded(Tree expr, Type type, Type expr_type, Env env)
    {
        Type[] types = typeList(type);
        Tree right=null;
        for(int i=0; i<types.length; i++)
        {
            if(this.types.subtype(expr_type, types[i]))
            continue;
            
            Tree newtest = newdef.Typeop(OperatorConst.TYPETEST, 
                         translator.transExpr(expr, env),  
                         trees.toTree(types[i]));
            if(right!=null)
            {
                right = newdef.Binop(OperatorConst.AND, right, newtest);
            }
            else
            right = newtest;
        }
        return right;
    }
    
    /**
     * Create a new instanceof operation.
     *
     * This operation can be used on compound as well as
     * on standard types; it expands to a series of
     * instanceof expression. Arrays of compounds are
     * supported. 
     *
     * It checks the expression and generates a local variable
     * if necessary.
     *
     * WARNING: It is very different from newInstanceOfIfNeeded !
     * 
     * @param expr the expression 
     * @param type the type to check (expr instanceof type)
     * @return a Tree, never null
     * @see CJCompounds#newInstanceOfIfNeeded
     */
    public Tree newInstanceOf(Tree expr, Type type, Env env)
    {
        Tree first_time = null;
        switch(expr)
        {
        case Ident(_, Definition def):
            if(def==null || def.isLocal()) // def==null => my own temporary variable
            {
                first_time = translator.transExpr(expr, env);
                break;
            }
            break;
        }
        if(first_time==null)
        {
            /* not just an identifier; use a temporary variable
             * so that the expression will be evaluated exactly
             * once.
             */
            pushDeclaredTemporary();
            first_time = newdef.Assign(temporaryIdent(), expr);
            expr = null; // => temporaryIdent()
        }

        FlatArray flat = new FlatArray(type);
        Type[] types = typeList(flat.elem);
        Tree right=null;
        for(int i=0; i<types.length; i++)
        {
            Tree newtest = newdef.Typeop(OperatorConst.TYPETEST, 
                         i==0 ? first_time: (expr!=null ? translator.transExpr(expr, env):temporaryIdent()),  
                         trees.toTree(flat.remake(types[i])));
            if(right!=null)
            {
                right = newdef.Binop(OperatorConst.AND, right, newtest);
            }
            else
            right = newtest;
        }
        if(expr==null)
        popTemporary();
        return right;
    }
    /**
     * Check whether an expression whose compile-time type is a compound
     * should be cast to something else.
     *
     * @param goal what standard type the compound is being used as
     * @param candidate the compound type
     * @param expr the expression 
     */
    protected Tree castExpr(Type _goal, Type _candidate, Tree cast_me)
    {
        FlatArray flatGoal = new FlatArray(_goal);
        FlatArray flatCandidate = new FlatArray(_candidate);
        if(flatGoal.depth!=flatCandidate.depth)
        return cast_me;
        
        Type realtype = realTypeFor(flatGoal.elem, flatCandidate.elem);
        if(realtype==null)// || types.subtype(realtype, goal))
        return cast_me;
        else 
        {
            return newdef.Typeop(OperatorConst.TYPECAST, cast_me, trees.toTree(flatGoal.remake(realtype)));
        }
    }

    /* ************************************************************
     *  Exception handling with compound types
     * ************************************************************
     *
     *  General Idea:
     *
     *   The main idea is to transform a catch clause that is
     *   passed a compound type into a clause catching a class
     *   type and then checking what interface the class implements
     *   using instanceof expression:
     *
     *     catch([Exception, Serializable] e) { ... } 
     *      
     *           becomes
     * 
     *     catch(Exception e)
     *     {
     *       if(e instanceof Serializable)
     *       {
     *          ...
     *       }
     *       else
     *         throw e; // rethrow
     *     }
     * 
     *   When more than one catch clause is present, rethrowing the exception
     *   if it does not implement the interfaces that are part of the compound
     *   is not always enough. 
     *
     *     catch([Exception, Serializable] e) { ... } 
     *     catch(Exception e) { ... } 
     *      
     *           becomes
     * 
     *     catch(Exception e)
     *     {
     *       if(e instanceof Serializable)
     *       {
     *          ... code for [Exception, Serializable] 
     *       }
     *       else
     *       {
     *          ... code for Exception
     *       }
     *     }
     *
     *   The algorithm tries to guess which clauses must be put together
     *   in this way so that the right exception handling code is
     *   executed. 
     * 
     *  Algorithm:
     *
     *   Note that catchable compound types always contain exactly
     *   one class, which is a subclass of Throwable. 
     * 
     *   The original catch clauses that have to be translated into one
     *   clause are put together into a CatchFamily. The choice of
     *   which family a clause must belong to is based on the possibility
     *   of an object matching the class of the compound but not the
     *   interfaces having to 'fall through' to another catch clause. 
     *
     *   This is done by iterating over the array of catch clauses:
     *
     *   As long as there are no families that may receive new
     *   members (they are sai to be closed):
     *   - when a compound type is found, a new family is created
     *     that contains the compound type. The 'head' of the family,
     *     the real class that will be caught by the resulting catch
     *     clause, is set to the class found in the compound. The new
     *     family is open.
     *   - when a class type is found, no family is created. Or rather,
     *     a new closed family is created containing only this catch
     *     clause.
     *
     *   If there are open families, they are checked in turn to find one that
     *   may receive the new catch clause. It is the case when the class
     *   caught by the clause is a subclass or a superclass of the head
     *   of a family. If a family is found for the clause, it receives
     *   the new member, and if the clause caught a non-compound type, the
     *   family is closed. 
     *   If no family was found for the new catch clause, it is handled
     *   as in the case where there are no open family. 
     *
     *   A case may need special handling: if a new catch clause is superclass
     *   of more than one head of open family. This means that the two 
     *   (or more) families must be merged into one, with the class caught 
     *   by the new catch clause as head. 
     * 
     *  ASSUMPTIONS:
     *     The whole algorithm is based on the assumption that the
     *     class hierarchy of the exception doesn't change after
     *     compilation. 
     *  
     *  TODO: 
     *     - rewrite this comment, too; it says too much or not enough. 
     *     - optimize
     */
    
    /**
     * Structure used to describe a specific catch statement.
     */
    protected class CatchInfo
    {
        /**
         * The original catch tree
         *
         * Set by the constructor; read-only after that.
         */
        public Catch tree;
        
        /**
         * If true, the catch tree tries to catch a
         * compound type.
         *
         * Set by the constructor; read-only after that.
         */
        public boolean is_compound;
        
        /** 
         * The standard class type that the catch
         * tree tries to catch.
         *
         * Should be a subclass of Throwable. (not enforced)
         *
         * Set by the constructor; read-only after that.
         */
        public Type  classtype;
        
        /**
         * The family that this catch clause belongs to.
         *
         * Set by the CatchFamily object itself.
         * @see CatchFamily
         */
        public CatchFamily family;
        
        /**
         * Check whether this catchinfo belongs to a family
         *
         */
        public boolean isOrphan()
        {
        return family==null;
        }
        
        /**
         * Create and initialize a CatchInfo
         *
         * By default, a catch clause is valid and has no
         * family.
         *
         * @param clause the catch clause this catchinfo
         *               represents
         */
        public CatchInfo(Catch catchtree) 
        {
        tree = catchtree;
        init();
        }
        
        /**
         * Initialize the catchinfo.
         *
         * Sets up all variable, based on the value of this.tree.
         */
        private void init()
        {
        VarDecl vardecl = tree.exception;
        Tree vartypetree = ((VarDecl)vardecl).vartype;
        boolean vartype_is_compound = false;
        Type vartype = vartypetree.type;
        
        classtype = null;
        is_compound = false;
        family = null;
        
        /* get classtype (a *real* class, not a compound) */
        switch((CJType)vartype.deref())
            {
            case CompoundType(_):
            classtype = (ClassType)vartype.tdef().supertype();
            is_compound = true;
            break;
            
            case ClassType(_):
            classtype = (ClassType)vartype.deref();
            break;
            }
        
        if(classtype==null)
            throw new InternalError();
        
    }
    }
    
    /**
     * A family of catch statements.
     *
     * A family of catch contains at least one
     * catch statement that tries to catch a compound.
     *
     * All the following catch statements the
     * original one may fall-through are included
     * afterwards.
     */
    protected class CatchFamily
    {
        /**
         * Smallest index of a catchinfo in the family.
         */
        private int first;
        
        /**
         * Biggest index of a catchinfo in the family
         */
        private int last;
        
        /**
         * Get the smallest index of the catchinfos in the family.
         */
        public int getFirstIndex()
        {
        return first;
        }
        
        /**
         * Get the largest index of the catchinfos in the family.
         */
        public int getLastIndex()
        {
        return last;
        }
        
        /**
         * The most common standard type of all the catch
         * clauses in the family.
         */
        private Type head;
        
        /**
         * The most common standard type of all the catch
         * clauses in the family.
         *
         * @result the head (!=null if the family is valid)
         */
        public Type getHead()
        {
        return head;
        }
        
        /**
         * If true, the family is closed and won't receive
         * any new member.
         */
        private boolean closed;
        
        /**
         * If true, the family is closed and won't receive
         * any new member.
         *
         * It is illegal to add members to a closed family.
         * 
         * See CatchFamily description.
         * @see close
         */
        public boolean isClosed()
        {
        return closed;
        }
        
        /**
         * Close the family.
         * @see isClosed
         */
        void close()
        {
        closed = true;
        }
        
        /**
         * Create a new catch family.
         *
         * The family is not closed.
         *
         * @param catchinfo the initial catchinfo
         * @param index index of the catchinfo in the array
         */
        public CatchFamily(CatchInfo catchinfo, int index) 
        { 
        head=catchinfo.classtype; 
        catchinfo.family = this;
        closed=false;
        first = index;
        last = index;
        }
        
        /**
         * Add a new catch clause to the family.
         *
         * It is illegal to add members to a closed family.
         * @param catchinfo the catchinfo to add
         * @param index index of the catchinfo in the array
         * @result true if the head was modified by this operation
         */
        public boolean add(CatchInfo catchinfo, int index)
        {
        if(closed)
            throw new InternalError();
        
        catchinfo.family = this;
        if(index<first)
            first = index;
        else if(index>last)
            last = index;
        
        return changeHead(catchinfo.classtype);
        }
        
        /**
         * Change the head of the family, if necessary.
         *
         * @param head the new head
         * @result true if the head was changed
         */
        private boolean changeHead(Type newhead)
        {
        if(types.subtype(head, newhead) && !types.sametype(head, newhead))
            {
            head = newhead;
            return true;
            }
        return false;
        }
        
        
        /**
         * Add the members of another family to this one.
         *
         * The family that's added is emptied.
         *
         * It is illegal to add members to a closed family.
         * @param family the family to add
         * @param catchinfos the catchinfo array
         * @result true if the head was modified by this operation
         */
        public boolean addFamily(CatchFamily family, CatchInfo[] array)
        {
        if(closed)
            throw new InternalError();
        
        if(family.first<first)
            first = family.first;
        if(family.last>last)
            last = family.last;
        for(int i=family.first; i<=family.last; i++)
            {
            if(array[i].family==family)
                array[i].family = this;
            }
        boolean retval = changeHead(family.head); 
        family.head = null;
        family.close();
        return retval;
        }
    }
    
    
    /**
     * Translate an array of catch clauses, corresponding
     * to one try statement.
     *
     * See the source for comments on how catch clauses are translated.
     */
    public Catch[] transCatches(Catch[] ts, Env env)
    {
        
        if (ts == null)
        return null;
        
        /* Vector of CatchFamily */
        Vector families = new Vector();

        CatchInfo[] catchinfo = new CatchInfo[ts.length];
        /* the result */
        TreeList    stats = new TreeList();

        for (int i = 0; i < ts.length; i++)
        {
        catchinfo[i] = new CatchInfo(ts[i]);
        
        for(ListIterator f_iter=families.listIterator(); f_iter.hasNext(); )
            {
            CatchFamily family = (CatchFamily)f_iter.next();

            if(family.isClosed())
                continue;

            if(types.subtype(family.getHead(), catchinfo[i].classtype))
                {
                if(family.add(catchinfo[i], i))
                    {
                    for(ListIterator f_subiter=families.listIterator(); f_subiter.hasNext(); )
                        {
                        CatchFamily otherfamily = (CatchFamily)f_subiter.next();
                        if(family!=otherfamily && !otherfamily.isClosed() 
                           && types.subtype(otherfamily.getHead(), family.getHead()))
                            {
                            /* to optimize: I know the head won't be changed, but
                             * addFamily still checks it.
                             */
                            family.addFamily(otherfamily, catchinfo);
                            f_subiter.remove();
                            }
                        }
                    }
                if(!catchinfo[i].is_compound)
                    family.close();
                break;
                }
            if(types.subtype(catchinfo[i].classtype, family.getHead()))
                family.add(catchinfo[i], i);
            }
        if(catchinfo[i].isOrphan() && catchinfo[i].is_compound)
            {
            families.add(new CatchFamily(catchinfo[i], i));
            }
        }

        for(int i=0; i<catchinfo.length; i++)
        {
        if(catchinfo[i].isOrphan())
        {
            /* a simple catch statement */
            Catch stmt = catchinfo[i].tree;
            Catch tree = (Catch)translator.transStat(stmt, env);
            if(tree!=null)
            {
            Tree[]  container = trees.getContainerContent(tree);
            if (container == null)
                stats.append(tree);
            else
                stats.append(container);
            }
        }
        else if(catchinfo[i].family.getLastIndex() == i)
        {
            pushTemporary();

            CatchFamily family = (CatchFamily)catchinfo[i].family;
            Tree[] catchblock = new Tree[1];
            Tree thecatchblocktree;
            Trees.PresetReDef family_redef = trees.at(catchinfo[family.getFirstIndex()].tree);
            Catch thecatch = (Catch)newdef.Catch((VarDecl)newdef.VarDecl(temporaryName(), 
                                         0, 
                                         trees.toTree(family.getHead()),
                                         null),
                             thecatchblocktree=newdef.Block(0, catchblock));

            If previous = null;
        
            for(int j=family.getFirstIndex(); j<=family.getLastIndex(); j++)
            {
            if(catchinfo[j].family!=family)
                continue;
            
            Tree tree = (Tree)catchinfo[j].tree;
            switch(tree)
                {
                case Catch(VarDecl vardecl, Block(_, Tree[] body)):
                Trees.PresetReDef catch_redef = trees.at(tree);
                Name varname = vardecl.name;
                Tree vartype = vardecl.vartype;
                Tree test = newInstanceOfIfNeeded(
                              temporaryIdent(),
                              vartype.type,
                              family.getHead(),
                              env);
                Tree newtree = null;
                if(test!=null)
                    newtree = (If)newdef.If( test,
                                 null /* then defined later */, 
                                 null /* else defined later */);
                Tree[] statements = new Tree[body.length+1];
                
                statements[0] = newdef.VarDecl(varname, 
                                   0, 
                                   trees.toTree(defaultTypeFor(vartype.type)), 
                                   castIfNeeded(vartype.type, family.getHead(), temporaryIdent()));


                if(newtree==null)
                    newtree = newdef.Block(0, statements);
                else
                    ((If)newtree).thenpart = newdef.Block(0, statements);
                catch_redef.make(test);

                for(int k=0; k<body.length; k++)
                    statements[k+1] = translator.transStat(body[k], env);

                if(previous==null)
                    {
                    thecatch.pos = tree.pos;
                    ((If)newtree).pos = tree.pos; // Cast error here => newInstanceOfIfNeeded returned null
                    // when it shouldn't  
                    thecatchblocktree.pos = tree.pos;
                    catchblock[0] = newtree;
                    }
                else
                    previous.elsepart = newtree;
                if(test!=null) // => newtree instanceof If
                    previous = (If)newtree;
                else 
                    previous = null;

                break;
                
                default:
                throw new InternalError();
                }
            }
            if(previous!=null)
            {
            previous.elsepart = newdef.Throw(temporaryIdent());
            }
            stats.append(family_redef.make(thecatch));

            popTemporary();
        }
        }

    
        return (Catch[])stats.toArray(new Catch[stats.length()]);
    }

    /**
     * Check whether an expression needs a cast to be converted
     * to another type.
     *
     * @param goal type to assign expr to (compound or standard)
     * @param has expr.type (compound or standard)
     * @param expr the expression to cast
     * @result a tree 
     */
    protected Tree castIfNeeded(Type goal, Type has, Tree expr)
    {
        goal = defaultTypeFor(goal);
        has = defaultTypeFor(has);
        if(types.subtype(has, goal))
        return expr;
        else
        return newdef.Typeop(OperatorConst.TYPECAST, expr, trees.toTree(goal));
    }


    /* ************************************************************
     *  Temporary variable names
     * ************************************************************
     *
     *   This function generates weird variable names and hopes
     *   that this name is not used anywhere else. To be sure
     *   to create a different name each time, a number is
     *   appended to the name. This number is incremented every
     *   time a new temporary variable name is needed.
     *  
     *   The temporary variable follow the pattern __cjava_t$number,
     *   with number starting with 1 for variable containing throwables,
     *   and number=0 for the variable that holds an object in a 
     *   complicated instanceof operation. 
     */
    
    /**
     * Names of the temporary variables to declare in
     * the method.
     *
     * Stack of Name.
     */
    protected boolean declare = false;
    
    /**
     * Variable that contains the declared variable
     */
    protected Name declareName = null;
    
    /**
     * Name of the temporary variables.
     *
     * Stack of Name.
     */
    protected Stack temporary = new Stack();

    /**
     * Create a new temporary variable.
     *
     * The name will be returned by temporaryName() as
     * long as popTemporary() is not used.
     */
    protected void pushTemporary()
    {
        temporary.push( Name.fromString("__cjava_t$" + (temporary.size()+1)) );
    }

    /**
     * Create a temporary variable with the type Object in
     * the current method.
     *
     * The name will be returned by temporaryName() as
     * long as popTemporary() is not used.
     */
    protected void pushDeclaredTemporary()
    {
        if(declareName==null)
        declareName = Name.fromString("__cjava_t$0");
        temporary.push( declareName );
        declare = true;
    }

    /**
     * Remove the last temporary variable name from the stack.
     */
    protected void popTemporary()
    {
        temporary.pop();
    }

    /**
     * Create an identifier object corresponding
     * to the current temporary variable name.
     */
    protected Tree temporaryIdent()
    {
        return newdef.Ident((Name)temporary.peek());
    }

    /**
     * Create a Name object corresponding
     * to the current temporary variable name.
     */
    protected Name temporaryName()
    {
        return (Name)temporary.peek();
    }

    /**
     * Create a declaration for one declared temporary variable, if necessary.
     *
     * 
     */
    protected Tree[] temporaryDecls()
    {
        if(declare)
        {
        Tree[] ts = new Tree[1];
        ts[0] = newdef.VarDecl(declareName, 0, trees.toTree(types.objectType), null);
        declare = false;
        return ts;
        }
        return trees.noTrees;
    }
    }

    /**
     * A simpler array type representation.
     *
     * FlatArray are used to access the base element
     * type of any array, no matter how deep it is.
     *
     * They contain a base type (element type) and
     * a depth (which are 0 for non arrays, 1 for
     * simple arrays...)
     *
     * They are created from a type and can later
     * be converted back into a type that uses
     * ArrayType. (see remake())
     *
     */
    protected class FlatArray
    {
    /**
     * Base element type.
     *
     * Anything except an ArrayType.
     */
    public Type elem; 

    /**
     * Depth of the array.
     *
     * >=0, 0 meaning that it's not an array
     */
    public int depth; 

    /**
     * Create a type making use of ArrayType if necessary.
     *
     * This function creates an array type of the right
     * depth and using the base type.
     *
     * @param basetype the final type (normally not an array)
     * @param depth the depth of the array to be created (>=0)
     * @return a new type
     */
        Type remake(Type basetype, int depth)
    {
        Type t = basetype;
        for(int i=0; i<depth; i++)
        t = types.make.ArrayType(t);
        return t;
    }

    /**
     * Create an array based on the given base type and
     * with the flatarray's depth.
     *
     * @parama base the final type
     * @return a new type
     */
    Type remake(Type basetype)
    {
        return remake(basetype, depth);
    }

    /**
     * Create an array based on the current base type (elem)
     * and depth of the flat array.
     *
     * @return a new type
     */
    Type remake()
    {
        return remake(elem, depth);
    }

    /**
     * Return the default element type
     *
     * @result a standard type
     */
    Type defaultElemType()
    {
        return defaultTypeFor(elem);
    }

    /**
     * Create a new flat array.
     * 
     * It sets elem and depth based on the original type.
     *
     * After the flat array has been created, elem is guaranteed
     * to be a valid type (if original is valid) different from ArrayType, 
     * and the depth >=0. 
     *
     * if depth==0 => elem=original
     * 
     * @param original the type the flat array should represent.
     */
    public FlatArray(Type original)
    {
        depth = 0;
        WhileArray: while(true)
        {
            switch(original)
            {
            case ArrayType(Type e):
                depth++;
                original = e;
                break;

            default:
                break WhileArray;
            }
        }
        elem = original;
    }
    }

    private TypeComparator typecomparator;
    public TypeComparator TypeComparator()
    {
    if(typecomparator==null)
        typecomparator = new TypeComparator();
    return typecomparator;
    }
}

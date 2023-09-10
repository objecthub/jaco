//      /   _ _      JaCo
//  \  //\ / / \     - name resolution module
//   \//  \\_\_/     
//         \         Matthias Zenger, 13/06/01

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.io.IOException;
import Definition.*;
import Type.*;
import Tree.*;


public class NameResolver extends Component
                          implements ModifierConst, DefinitionConst, TreeConst
{
/** other components
 */
    protected Definitions       definitions;
    protected Types             types;
    protected Modifiers         modifiers;
    protected Operators         operators;
    protected ErrorHandler      report;
    protected Mangler           mangler;
    protected ClassReader       reader;

/** name resolution errors
 */
    public NameError            varNotFound;
    public NameError            funNotFound;
    public NameError            typeNotFound;


/** component name
 */
    public String getName()
    {
        return "JavaNameResolver";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        definitions = context.Definitions();
        types = context.Types();
        modifiers = context.Modifiers();
        operators = context.Operators();
        report = context.ErrorHandler();
        mangler = context.Mangler();
        reader = context.ClassReader();
        
        varNotFound = NameError(ABSENT_VAR);
        funNotFound = NameError(ABSENT_FUN);
        typeNotFound = NameError(ABSENT_TYPE);
    }


// ************************************************************************
// identifier resolution
// ************************************************************************

/** is class c accessible in evironment env?
 */
    public boolean accessible(ContextEnv env, Definition c)
    {
        return (c.modifiers & PUBLIC) != 0 ||
                env != null &&
                (env.toplevel.def == c.owner ||
                env.toplevel.def == c.outermostClass().owner);
    }

/** is def accessible as a member of class c in evironment env?
 */
    public boolean accessible(ContextEnv env, Definition c, Definition def)
    {
        if ((def.name == PredefConst.INIT_N) && (def.owner.fullname != c.fullname))
            return false;
        else if (env == null)
            return accessible(env, c);
        int mods = def.modifiers;
        switch (mods & (PRIVATE | PUBLIC | PROTECTED))
        {
            case 0:
                return (env.toplevel.def == def.owner.owner) ||
                       (env.toplevel.def == def.outermostClass().owner);
            
            case PRIVATE:
                return (env.enclClass.def == def.owner) ||
                        (env.enclClass.def.outermostClass() ==
                            def.owner.outermostClass());

            case PROTECTED:
                return (env.toplevel.def == def.owner.owner) ||
                       (env.toplevel.def == def.outermostClass().owner) ||
                       (definitions.subclass(env.enclClass.def, def.owner) &&
                        (definitions.subclass(c, env.enclClass.def) || env.info.selectSuper));
            default:
                return accessible(env, c);
        }
    }

/** instantiate a function `fn' to a vector of argument types
 *  `argtypes' with `site' as the site class.
 */
    public boolean instantiatable(Definition fn, Type[] argtypes, Type site)
    {
        switch (fn.type)
        {
            case MethodType(Type[] formals,_,_):
               return (argtypes == null) || types.subtypes(argtypes, formals);
            
            default:
                return false;
        }
    }

/** instantiate a function `fn' to a vector of argument types
 *  `argtypes' with `site' as the site class.
 */
    public boolean instantiatable(Definition fn, Type[] argtypes, Type site, ContextEnv env)
    {
        return instantiatable(fn, argtypes, site);
    }

/** is method definition 'a' at least as good as method Definition 'b'
 *  when used from class 'site'?
 *  used to get the best-matching overloaded instance during object lookup.
 */
    public boolean asGood(ContextEnv env, Type site,
                          Definition a, Definition b)
    {
        return !accessible(env, site.tdef(), b) ||
               (accessible(env, site.tdef(), a) &&
                 ((((a.modifiers | b.modifiers) & STATIC) != 0) ||
                 (((a.owner.modifiers | b.owner.modifiers) & INTERFACE) != 0) ||
                 definitions.subclass(a.owner, b.owner)) &&
               instantiatable(b, a.type.argtypes(), site, env));
    }

/** name resolution methods
 */
    public Definition findField(ContextEnv env, Definition c, Name name)
    {
        Definition def = varNotFound;
        Definition e = c.locals().lookup(name);
        while (e.scope != null)
        {
            if (e.def.kind == VAR)
            {
                if ((env == null) || accessible(env, c, e.def))
                    return e.def;
                else
                if (def.kind > HIDDEN)
                    def = AccessError(e.def);
            }
            e = e.next();
        }
        // not found, look in interfaces now
        while (true)
        {
            Type[] is = c.interfaces();
            for (int i = 0; i < is.length; i++)
            {
                Definition def1 = findField(env, is[i].tdef(), name);
                if (def.kind < BAD && def1.kind < BAD && def.owner != def1.owner)
                    return AmbiguityError(def, def1);
                else
                if (def1.kind == AMBIGUOUS)
                    return def1;
                else
                if (def1.kind < def.kind)
                    def = def1;
            }
            Type st = c.supertype();
            if (def.kind < BAD || st == null)
                return def;
            c = st.tdef();
        }
    }

    public Definition findImportedField(ContextEnv env, Scope scope, Name name)
    {
        Definition e = scope.lookup(name);
        while ((e.scope != null) &&
                !(e.def.kind == VAR &&
                accessible(env, (Definition)e.def.owner, e.def)))
            e = e.next();
        if (e.scope != null)
        {
            Definition e1 = e.next();
            while (e1.scope != null && !(e1.def.kind == VAR &&
                    accessible(env, (Definition)e1.def.owner, e1.def)))
                e1 = e1.next();
            if (e1.scope != null)
                return AmbiguityError(e.def, e1.def);
            else
                return e.def;
        }
        else
            return varNotFound;
    }
    
    public Definition findVar(ContextEnv env, Name name)
    {
        Definition  def = varNotFound;
        Definition  def1;
        ContextEnv  env1 = env;
        do
        {
            Definition e = env1.info.scope.lookup(name);
            while (e.scope != null)
            {
                if (e.def.kind == VAR)
                    return e.def;
                e = e.next();
            }
            def1 = findField(env1, env1.enclClass.def, name);
            if (def1.kind < BAD)
                return def1;
            else
            if (def1.kind < def.kind)
                def = def1;
            env1 = (ContextEnv)env1.outer;
        }
        while (env1.outer != null);

        def1 = findImportedField(env, env.toplevel.importScope[NAMED_SCOPE], name);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
                
        def1 = findImportedField(env, env.toplevel.importScope[STAR_SCOPE], name);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
                
        def1 = findField(env, definitions.predefClass, name);
        if (def1.kind < BAD)
            return def1;
        return def;
    }

    public Definition findAbstractMethod(ContextEnv env, Definition c,
                                        Definition bestsofar,
                                        Name name, Type[] argtypes,
                                        Type site)
    {
        Definition e = c.locals().lookup(name);
        while (e.scope != null)
        {
            if (instantiatable(e.def, argtypes, site, env) &&
                (bestsofar.kind >= BAD || !asGood(env, site, bestsofar, e.def)))
            bestsofar = e.def;
            e = e.next();
        }
        while (true)
        {
            Type[] is = c.interfaces();
            for (int i = 0; i < is.length; i++)
                bestsofar = findAbstractMethod(env, is[i].tdef(),
                                            bestsofar, name, argtypes, site);
            Type st = c.supertype();
            if (st == null)
                return bestsofar;
            c = st.tdef();
        }
    }

    public Definition checkBestAbstractMethod(ContextEnv env, Definition c,
                                            Definition def, Type[] argtypes,
                                            Type site)
    {
        Definition e = c.locals().lookup(def.name);
        while (e.scope != null)
        {
            if ((def != e.def) &&
                instantiatable(e.def, argtypes, site, env) &&
                !asGood(env, site, def, e.def) &&
                !types.erroneous(argtypes))
                return AmbiguityError(def, e.def);
            e = e.next();
        }
        while (true)
        {
            Type[] is = c.interfaces();
            for (int i = 0; def.kind == FUN && i < is.length; i++)
                def = checkBestAbstractMethod(env, is[i].tdef(), def, argtypes, site);
            Type st = c.supertype();
            if (st == null)
            {
                if ((def.kind >= BAD) ||
                    accessible(env, site.tdef(), def))
                    return def;
                else
                    return AccessError(def);
            }
            c = st.tdef();
        }
    }

    public Definition findBestMethod(ContextEnv env, Type site,
                                    Definition e, Type[] argtypes)
    {
    	//System.out.println("  findBestMethod " + e.def + " in " + site);
        Definition def = e.def;
        Definition e1 = e.next();
        while (e1.scope != null)
        {
            if (instantiatable(e1.def, argtypes, site, env) &&
                !asGood(env, site, def, e1.def))
                def = e1.def;
            e1 = e1.next();
        }
        if (def != e.def)
        {
            e1 = e;
            while (e1.scope != null)
            {
                if (def != e1.def &&
                    instantiatable(e1.def, argtypes, site, env) &&
                    !asGood(env, site, def, e1.def) &&
                    !types.erroneous(argtypes))
                    return AmbiguityError(def, e1.def);
                e1 = e1.next();
            }
        }
        if ((def.kind >= BAD) || accessible(env, site.tdef(), def))
            return def;
        else
            return AccessError(def);    
    }
    
    public String types2str(Type[] ts) {
        if (ts == null)
            return "null";
        else if (ts.length == 0)
            return "()";
        else if (ts.length == 1)
            return "(" + ts[0] + ")";
        String res = "(" + ts[0];
        for (int i = 1; i < ts.length; i++)
            res += ", " + ts[i];
        return res +")";
    }
    
    public Definition findMethod(ContextEnv env, Type site,
                                 Name name, Type[] argtypes)
    {
    	//System.out.println("find " + name + " in " + site);
        //TODO: only for interfaces
        if ((site.tdef().modifiers & (ABSTRACT | INTERFACE)) != 0)
        {
            Definition def = findAbstractMethod(env, site.tdef(),
                                                funNotFound,
                                                name, argtypes, site);
            if (def.kind < BAD)
                def = checkBestAbstractMethod(env, site.tdef(), def, argtypes, site);
            return def;
        }
        else
        {
            if (site.tdef().locals() == null) //hack!
                return funNotFound;
            Definition e = site.tdef().locals().lookup(name);
            //System.out.println("trying to find " + name + " in " + site);
            while (e.scope != null)
            {
            	//System.out.println("  consider " + e.def + ": " + e.def.type);
                if (instantiatable(e.def, argtypes, site, env))
                    return findBestMethod(env, site, e, argtypes);
                e = e.next();
            }
            //System.out.println("=> not found");
            return funNotFound;
        }
    }

    public Definition findPredefFun(ContextEnv env, Name name, Type[] argtypes)
    {
        Definition e = definitions.predefClass.locals().lookup(name);
        while (e.scope != null)
        {
            if (instantiatable(e.def, argtypes, definitions.predefClass.type, env))
                return findBestMethod(env, definitions.predefClass.type, e, argtypes);
            e = e.next();
        }
        return funNotFound;
    }   

    public Definition findImportedMethod(ContextEnv env, Scope scope,
                                            Name name, Type[] argtypes)
    {
        Definition e = scope.lookup(name);
        while (e.scope != null)
        {
            if (instantiatable(e.def, argtypes, e.def.owner.type, env))
                return findBestMethod(env, env.enclClass.def.type, e, argtypes);
            e = e.next();
        }
        return funNotFound;
    }

    public Definition findFun(ContextEnv env, Name name, Type[] argtypes)
    {
        Definition def = funNotFound;
        Definition def1;
        ContextEnv env1 = env;
        while (true)
        {
            Definition e = env1.info.scope.lookup(name);
            while (e.scope != null)
            {
                if (instantiatable(e.def, argtypes, env.enclClass.def.type, env))
                    return e.def;
                e = e.next();
            }
            def1 = findMethod(env1, env1.enclClass.def.type, name, argtypes);
            if (def1.kind < BAD)
                return def1;
            else
            if (def1.kind < def.kind)
                def = def1;
            ContextEnv env2 = (ContextEnv)env1.outer;
            if (env2 == null || env2.outer == null)
                break;
            def1 = findAny(env1.enclClass.def, FUN, name);
            if (def1 != null && accessible(env1, env1.enclClass.def, def1))
                return def;
            env1 = env2;
        }
        
        def1 = findImportedMethod(env, env.toplevel.importScope[NAMED_SCOPE], name, argtypes);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
                
        def1 = findImportedMethod(env, env.toplevel.importScope[STAR_SCOPE], name, argtypes);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
        
        def1 = findMethod(env, definitions.predefClass.type, name, argtypes);
        if (def1.kind < BAD)
            return def1;
        return def;
    }

    public Definition findAny(Definition c, int kind, Name name)
    {
        Definition e = c.locals().lookup(name).def;
        while (e.scope != null)
        {
            if (e.def.kind == kind)
                return e.def;
            e = e.next();
        }
        return null;
    }
    
    public Definition loadClass(ContextEnv env, Name name)
    {
        try
        {
            Definition  c = reader.loadClass(name);
            if (accessible(env, c))
                return c;
            else
                return AccessError(c);
        }
        catch (IOException ex)
        {
            return LoadError(ex, TYP, name);
        }
    }

    public Definition loadPackage(ContextEnv env, Name name)
    {
        try
        {
            return reader.loadPackage(name);
        }
        catch (IOException ex)
        {
            return LoadError(ex, PCK, name);
        }
    }

    public Definition findImportedType(ContextEnv env, Scope scope, Name name)
    {
        Definition e = scope.lookup(name);
        while (e.scope != null && e.def.kind != TYP)
            e = e.next();
        if (e.scope != null)
        {
            Definition e1 = e.next();
            while (e1.scope != null && e1.def.kind != TYP)
                e1 = e1.next();
            if (e1.scope != null)
                return AmbiguityError(e.def, e1.def);
            else
                return loadClass(env, e.def.fullname);
        }
        else
            return typeNotFound;
    }

    public Definition findMemberType(ContextEnv env, Definition c, Name name)
    {
        Definition  def = typeNotFound;
        Definition  e = c.locals().lookup(name);
        while (e.scope != null)
        {
            if (e.def.kind == TYP)
            {
                if (accessible(env, c, e.def))
                    return e.def;
                else
                if (HIDDEN < def.kind)
                    def = AccessError(e.def);
            }
            e = e.next();
        }
        // not found, search in interfaces
        while (true)
        {
            Type[] is = c.interfaces();
            for (int i = 0; i < is.length; i++)
            {
                Definition def1 = findMemberType(env, is[i].tdef(), name);
                if ((def.kind < BAD) && (def1.kind < BAD) && (def.owner != def1.owner))
                    return AmbiguityError(def, def1);
                else if (def1.kind == AMBIGUOUS)
                    return def1;
                else if (def1.kind < def.kind)
                    def = def1;
            }
            Type st = c.supertype();
            if ((def.kind < BAD) || (st == null))
                return def;
            c = st.tdef();
        }
        //return def;
    }

    public Definition findType(ContextEnv env, Name name)
    {
        Definition def = typeNotFound;
        Definition def1;
        if (env.info.scope != null)
        {
            Definition e = env.info.scope.lookup(name);
            while (e.scope != null)
            {
                if (e.def.kind == TYP)
                    return e.def;
                e = e.next();
            }
            def1 = findMemberType(env, env.enclClass.def, name);
            if (def1.kind < BAD)
                return def1;
            else
            if (def1.kind < def.kind)
                def = def1;
            ContextEnv env1 = (ContextEnv)env.outer;
            while (env1.outer != null)
            {
                e = env1.info.scope.lookup(name);
                while (e.scope != null)
                {
                    if (e.def.kind == TYP)
                        return e.def;
                    e = e.next();
                }
                def1 = findMemberType(env1, env1.enclClass.def, name);
                if (def1.kind < BAD)
                    return def1;
                else
                if (def1.kind < def.kind)
                    def = def1;
                env1 = (ContextEnv)env1.outer;
            }
        }
        def1 = findImportedType(env, env.toplevel.importScope[NAMED_SCOPE], name);
        if (def1.kind < BAD)
            return def1;
        else
            if (def1.kind < def.kind) def = def1;
       
        def1 = findImportedType(env, env.toplevel.def.locals(), name);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
        
        def1 = findImportedType(env, env.toplevel.importScope[STAR_SCOPE], name);
        if (def1.kind < BAD)
            return def1;
        else
        if (def1.kind < def.kind)
            def = def1;
        
        return def;
    }

/** report error if this is a bad Definition
 *  PRE: Definition.name == null ==> site != null
 */
    public Definition access(Definition def, int pos, Type site,
                            Name name, Type[] argtypes)
    {
        if (def.kind >= BAD)
        {
            ((NameError)def).report(pos, site, name, argtypes);
            return Scope.errDef;
        }
        else
            return def;
    }

    public Definition access(Definition def, int pos, Name name)
    {
        if (def.kind >= BAD)
            return access(def, pos, definitions.predefClass.type, name, null);
        else
            return def;
    }

/** if `pt' is a function prototype, its arguments; null otherwise
 */
    public Type[] protoArgs(Type pt)
    {
        switch (pt.deref())
        {
            case MethodType(Type[] argtypes, Type restype, _):
                if (restype == null)
                    return argtypes;
                else
                    return null;

            default:
                return null;
        }
    }

    public Definition resolveIdent(int pos, Name name,
                                    ContextEnv env, int kind, Type pt)
    {
        Definition def = typeNotFound;
        if ((kind & VAR) != 0)
            def = findVar(env, name);
        if ((def.kind >= BAD) && ((kind & FUN) != 0))
        {
            Definition def1 = findFun(env, name, protoArgs(pt));
            if (def1.kind < def.kind)
                def = def1;
        }
        if ((def.kind >= BAD) && ((kind & TYP) != 0))
        {
            Definition def1 = findType(env, name);
            if (def1.kind < def.kind)
                def = def1;
        }
        if ((def.kind >= BAD) && ((kind & PCK) != 0))
        {
            Definition  def1 = loadPackage(env, name);
            if (def1.kind < BAD)
                def = def1;
            else
            if ((kind & TYP) != 0 && def.kind == ABSENT_TYPE &&
                def1.kind == LOADERROR)
                def = LoadError(((LoadError)def1).fault, TYP | PCK, name);
        }
        if (def.kind >= BAD)
        {
            //env.printscopes();//DEBUG
            def = access(def, pos, env.enclClass.def.type, name, protoArgs(pt));
        }
        return def;
    }

    public Definition resolveSelf(int pos, int tag, Definition c, ContextEnv env)
    {
        ContextEnv  env1 = env;
        Name        name = (tag == THIS) ? PredefConst.THIS_N : PredefConst.SUPER_N;
        while (env1 != null)
        {
            switch (env1.tree)
            {
                case ClassDecl(_, _, _, _, _, ClassDef cdef):
                    if (c == cdef)
                        return access(findVar(env1, name), pos, name);
            }
            env1 = (ContextEnv)env1.next;
        }
        report.error(pos, "not.encl.class", mangler.unmangle(c.fullname));
        return Scope.errDef;
    }

    public Definition resolveSelectFromPackage(Definition pck, Name name,
                                                ContextEnv env, int kind)
    {
        Name        fullname = definitions.formFullName(name, pck);
        Definition  def;
        Definition  c = definitions.getClass(fullname);
        
        if (c == null)
        {
            def = definitions.getPackage(fullname);
            if (def == null)
            {
                def = loadClass(env, fullname);
                if ((def.kind == LOADERROR) && ((kind & PCK) != 0))
                {
                    Definition  def1 = def;
                    def = loadPackage(env, fullname);
                    if (def.kind == LOADERROR)
                    {
                        def = LoadError(new IOException(
                                        ((LoadError)def1).fault + "; " +
                                        ((LoadError)def).fault),
                                        TYP | PCK, fullname);
                    }
                }
            }
        }
        else
        if (c.kind == PCK)
            def = c;
        else
            def = loadClass(env, fullname);
        return def;
    }

    public Definition resolveSelectFromType(Type stype, Name name,
                                            ContextEnv env, int kind, Type pt)
    {
        Definition def = typeNotFound;
        if ((kind & VAR) != 0)
            def = findField(env, stype.tdef(), name);
        if (def.kind >= BAD && (kind & FUN) != 0)
        {
            Definition def1 = findMethod(env, stype, name, protoArgs(pt));
            if (def1.kind < def.kind)
                def = def1;
        }
        if (def.kind >= BAD && (kind & TYP) != 0)
        {
            Definition def1 = findMemberType(env, stype.tdef(), name);
            if (def1.kind < def.kind)
                def = def1;
        }
        return def;
    }

    public Definition resolveConstructor(int pos, Type ctype,
                                        ContextEnv env, Type[] argtypes)
    {
        return access(findMethod(env, ctype, PredefConst.INIT_N, argtypes),
                                    pos, ctype, PredefConst.INIT_N, argtypes);
    }

    public Definition resolveOperator(int pos, int opcode,
                                    ContextEnv env, Type[] argtypes)
    {
        Name name = operators.opname[opcode];
        return access(findPredefFun(env, name, argtypes), pos, null, name, argtypes);
    }

    public Definition resolvePackage(int pos, Name fullname, ContextEnv env)
    {
        return access(loadPackage(env, fullname), pos, fullname);
    }


    public void notFound(int pos, Name member, Name clazz, Type[] argtypes)
    {
        report.error(pos,"class.wrong", member + ((argtypes == null) ?
                                                    "" : "(" + Tools.toString(argtypes) + ")"),
                     clazz);
    }
    
    public Definition resolveMember(int pos, Name name, Type site,
                                    Type[] argtypes, int staticmod)
    {
        Definition def = (argtypes == null) ?
                            findField(null, site.tdef(), name) :
                            findMethod(null, site, name, argtypes);
        if ((def.kind >= BAD) ||
            ((def.modifiers & PUBLIC) == 0) ||
            ((def.modifiers & STATIC) != staticmod))
        {
            notFound(pos, name, site.tdef().fullname, argtypes);
            throw new ResolveError();
        }
        return def;
    }
   

// ************************************************************************
// modifier checking
// ************************************************************************

/** if def is local to a class, check that it is declared static.
 */
    public void checkStatic(int pos, Definition def)
    {
        if ((def.modifiers & STATIC) == 0 &&
            def.owner != null &&
            def.owner.kind == TYP && def.kind != TYP)
            report.error(pos, "ref.nonpublic", def);
    }

/** is there a public environment between env and the one owned by upto?
 */
    public boolean existsStatic(Definition upto, ContextEnv env)
    {
        if (env.info.isStatic)
            return true;
        if (env.outer != null)
            while (env.outer.outer != null &&
                    !definitions.subclass(env.enclClass.def, upto))
            {
                if ((env.enclClass.def.modifiers & STATIC) != 0)
                    return true;
                env = (ContextEnv)env.outer;
            }
        return false;
    }

/** check that def is not an abstract method
 */
    public boolean checkNonAbstract(int pos, Definition def)
    {
        if ((def.modifiers & ABSTRACT) != 0)
        {
            report.error(pos, "abstract.method", def);
            return false;
        }
        else
            return true;
    }

/** base scope of class c on the scopes of all its superclasses, and
 *  do the same for them. This cannot be done
 *  when a class is first loaded or defined since superclasses
 *  might be defined later. Instead, fixups are done lazily,
 *  when the scope of a class is first needed.
 *  as a side effect, fixupScope checks for and eliminates cycles
 *  in the inheritance graph.
 */
    public void fixupScope(int pos, Definition c)
    {
        if (c.locals() != null)
        {
            if ((c.locals().next == null) && (c.supertype() != null))
            {
                c.locals().next = c.locals(); // mark to detect cycles
                fixupScope(pos, c.supertype().tdef());
                for (int i = 0; i < c.interfaces().length; i++)
                    fixupScope(pos, c.interfaces()[i].tdef());
                if (c.owner.kind != PCK)
                    fixupScope(pos, c.owner.enclClass());
                if (c.locals().next == c.locals())
                    c.locals().baseOn(c.supertype().tdef().locals());
            }
            else
            if (c.locals().next == c.locals())
            {
                report.error(pos, "cyclic.inheritance", c);
                c.locals().baseOn(types.objectType.tdef().locals());
                c.setSupertype(types.objectType);
                c.setInterfaces(new Type[0]);
            }
        }
    }
    
    public NameError NameError(int kind)
    {
        return new NameError(this, kind);
    }
    
    public NameError AccessError(Definition def)
    {
        return new AccessError(this, def);
    }
    
    public NameError LoadError(Exception fault, int kind, Name classname)
    {
        return new LoadError(this, fault, kind, classname);
    }
    
    public NameError AmbiguityError(Definition def1, Definition def2)
    {
        return new AmbiguityError(this, def1, def2);
    }
}


public class NameError extends Definition.ErrorDef
{
    NameResolver    origin;
    
    
    NameError(NameResolver origin, int kind)
    {
        this.kind = kind;
        this.origin = origin;
    }
    
    public void report(int pos, Type site, Name name, Type[] argtypes)
    {
        if (name != PredefConst.ERROR_N)
        {
            String  kindname = origin.definitions.kindName(kind);
            String  idname = name.toString();
            String  args = "";
            
            if (argtypes != null)
            {
                if (name == PredefConst.INIT_N)
                {
                    kindname = "constructor";
                    idname = ((ClassDef)site.tdef()).name.toString();
                    if (origin.definitions.isInnerclass(site.tdef()))
                    {
                        Mangler.Mangle  m = (Mangler.Mangle)origin.mangler.mangled.get(site.tdef().fullname);
                        if (m != null)
                            idname = m.name.toString();
                        if (argtypes.length >= 1)
                        {
                            idname = "(" + argtypes[0] + ")" + idname;
                            argtypes = origin.types.extract(argtypes, 1, new Type[argtypes.length - 1]);
                        }
                    }
                }
                args = "(" + Tools.toString(argtypes) + ")";
            }
            if ((site != null) && (site.tdef().name.length() != 0))
                origin.report.error(pos, "not.found.in.class", kindname + " " + idname + args, site);
            else
                origin.report.error(pos, "not.found", kindname + " " + idname + args);
        }
    }
}

public class AccessError extends NameError
{
    Definition def;


    AccessError(NameResolver origin, Definition def)
    {
        super(origin, HIDDEN);
        this.def = def;
    }

    public void report(int pos, Type site, Name name, Type[] argtypes)
    {
        if ((def.name == PredefConst.INIT_N) && (def.owner != site.tdef()))
            origin.funNotFound.report(pos, site, name, argtypes);
        else
        if ((def.modifiers & PUBLIC) != 0)
            origin.report.error(pos, "not.defined.in.public", def + def.location());
        else
        if ((def.modifiers & (PRIVATE | PROTECTED)) != 0)
            origin.report.error(pos, def + " has " +
                    origin.modifiers.toString(def.modifiers & (PRIVATE | PROTECTED)) +
                    "access" + def.location());
        else
            origin.report.error(pos, "not.defined.public", def, def.location());
    }
}

public class LoadError extends NameError
{
    public Exception   fault;
    int         kind;
    Name        classname;


    LoadError(NameResolver origin, Exception fault, int kind, Name classname)
    {
        super(origin, LOADERROR);
        this.fault = fault;
        this.kind = kind;
        this.classname = classname;
    }

    public void report(int pos, Type site, Name name, Type[] argtypes)
    {
        origin.report.error(pos, "cannot.access",
                            origin.definitions.kindNames(kind) + " " + name,
                            fault.getMessage());
    }
}

public class AmbiguityError extends NameError
{
    Definition def1;
    Definition def2;


    AmbiguityError(NameResolver origin, Definition def1, Definition def2)
    {
        super(origin, AMBIGUOUS);
        this.def1 = def1;
        this.def2 = def2;
    }

    public void report(int pos, Type site, Name name, Type[] argtypes)
    {
        Name sname = def1.name;
        if (sname == PredefConst.INIT_N)
            sname = def1.owner.name;
        origin.report.error(pos, "ambiguous.ref", sname,
                            def1 + def1.location(), def2 + def2.location());
    }
}

public class ResolveError extends Error {}

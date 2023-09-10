//      /   _ _      JaCo
//  \  //\ / / \     - library for definitions
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.util.Hashtable;
import java.io.IOException;
import Definition.*;


public interface DefinitionConst
{
/** kinds
 */
    int PROXY = 0;
    int PCK = 0x01;
    int TYP = 0x02;
    int VAR = 0x04;
    int FUN = 0x08;
    int VAL = VAR | 0x10;
    int ANY = PCK | TYP | FUN | VAL;

    int BAD = 0x100;
    int AMBIGUOUS = 0x100;
    int LOADERROR = 0x101;
    int HIDDEN = 0x102;
    int ABSENT_VAR = 0x103;
    int ABSENT_FUN = 0x104;
    int ABSENT_TYPE = 0x105;
    
/** import scopes
 */
    int NAMED_SCOPE = 0;
    int STAR_SCOPE = 1;
}

public class Definitions extends Component
                         implements DefinitionConst, ModifierConst,
                                    PredefConst, DefCompleter
{
/** other components
 */
    protected Types             types;
    protected Mangler           mangler;
    protected ErrorHandler      report;
    
/** factory for creating definitions
 */
    public Definition.Factory   make;

/** loaded classes and packages
 */
    protected Hashtable         classes;
    protected Hashtable         packages;
    
/** predefined definitions
 */
    public PackageDef           emptyPackage;
    public ClassDef             predefClass;

/** default completer
 */
    public DefCompleter         completer;


/** component name
 */
    public String getName()
    {
        return "JavaDefinitions";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        completer = this;
        context.Types();
        
        // set factory, initialize tables
        initComponent(context);
    }
    
    public void initComponent(MainContext context)
    {
        if (make == null)
        {
            types = context.Types();
            mangler = context.Mangler();
            report = context.ErrorHandler();
            
            // set factory, initialize tables
            make = DefinitionFactory(types);
            packages = new Hashtable();
            classes = new Hashtable();
            
            // set predefined definitions
            emptyPackage = (PackageDef)make.PackageDef(EMPTY_N, types.make.PackageType(), null);
            predefClass = (ClassDef)make.ClassDef(PUBLIC, EMPTY_N, emptyPackage);

            // create a new global definition table
            predefClass.setLocals(new Scope(null, predefClass));
            
            // enter base package and class
            definePackage(emptyPackage);
            defineClass(predefClass);
        }
    }
    
    protected DefinitionFactory DefinitionFactory(Types types)
    {
        return new DefinitionFactory(this, types);
    }
    
/** kind printing and checking
 */
    public String kindName(int kind)
    {
        switch (kind)
        {
            case PCK:
                return "package";
                
            case TYP:
                return "class";
                
            case VAR:
                return "variable";
                
            case FUN:
                return "method";
                
            case VAL:
                return "value";
            
            case ABSENT_VAR:
                return "variable";
                
            case ABSENT_FUN:
                return "method";
                
            case ABSENT_TYPE:
                return "class";
            
            default:
                return "identifier";
        }
    }
    
    public String kindNames(int kind)
    {
        String s[] = new String[4];
        int i = 0;
        if ((kind & VAR) != 0)
            s[i++] = kindName(VAR);
        if ((kind & FUN) != 0)
            s[i++] = kindName(FUN);
        if ((kind & TYP) != 0)
            s[i++] = kindName(TYP);
        if ((kind & PCK) != 0)
            s[i++] = kindName(PCK);
        String names = "";
        for (int j = 0; j < i - 2; j++)
            names = names + s[j] + ", ";
        if (i >= 2)
            names = names + s[i-2] + " or ";
        if (i >= 1)
            names = names + s[i-1];
        else
            names = "identifier";
        return names;
    }

    public boolean checkKind(int pos, int kind, int reqkind)
    {
        if ((kind == ANY) || ((kind & ~reqkind) == 0) ||
            (reqkind == FUN) && ((kind & ~VAL) == 0))
            return true;
        else
        {
            report.error(pos, "found.wrong", kindNames(reqkind), kindName(kind));
            return false;
        }
    }


/** define a new or loaded package
 */
    public Definition definePackage(Definition packge)
    {
        packages.put(packge.fullname, packge);
        return packge;
    }
    
    public Definition definePackage(Name fullname)
    {
        Definition p = (Definition)packages.get(fullname);
        if (p == null)
        {
            p = make.PackageDef(mangler.shortName(fullname),
                                types.make.PackageType(),
                                definePackage(mangler.packagePart(fullname)));
            packages.put(fullname, p);
        }
        return p;
    }
    
/** define a new or loaded class
 */
    public Definition defineClass(Definition clazz)
    {
        classes.put(clazz.fullname, clazz);
        return clazz;
    }
    
    public Definition defineClass(Name fullname)
    {
        Definition  def = (Definition)classes.get(fullname);
        if (def == null)
        {
            def = make.ClassDef(0, mangler.shortName(fullname),
                                definePackage(mangler.packagePart(fullname)));
            def.completer = completer;
            classes.put(fullname, def);
        }
        return def;
    }

    public void completeDef(Definition def)
    {
        ClassReader reader = ((MainContext)context).ClassReader();
        reader.completeDef(def);
    }

/** get a package definition by it's full name
 */
    public Definition getPackage(Name fullname)
    {
        return (Definition)packages.get(fullname);
    }
    
/** get a class definition by it's full name
 */
    public Definition getClass(Name fullname)
    {
        return (Definition)classes.get(fullname);
    }

/** remove a class from the definition table
 */
    public void removeClass(Name fullname)
    {
        classes.remove(fullname);
    }


/** form a full name out of a package and a classname
 */
    public Name formFullName(Name name, Definition owner)
    {
        if (owner == null)
            return name;
        Name    prefix = owner.fullname;
        if ((prefix == null) || (prefix == EMPTY_N))
            return name;
        else
            return prefix.append(PERIOD_N).append(name);
    }


/** is definition 'that' a subclass of definition 'base'
 */
    public boolean subclass(Definition that, Definition base)
    {
        switch (that.complete())
        {
            case ClassDef(_, Type[] is, _, _, _):
                if (that == base)
                    return true;
                else
                if ((base.modifiers & INTERFACE) != 0)
                {
                    for (int i = 0; i < is.length; i++)
                        if (subclass(is[i].tdef(), base))
                            return true;
                }
                else
                {
                    do
                    {
                        Type    st = that.supertype();
                        if (st == null)
                            return false;
                        that = st.tdef();
                    }
                    while (that != base);
                    return true;
                }
                return false;
            
            default:
                throw new InternalError("subclass " + this);
        }
    }

/** is this definition an innerclass?
 */
    public boolean isInnerclass(Definition that)
    {
        if (((MainContext)context).classesNest &&
            (that.owner.kind != PCK) &&
            ((that.modifiers & (STATIC | INTERFACE)) == 0))
        {
            Mangler.Mangle  info = (Mangler.Mangle)mangler.mangled.get(that.fullname);
            return (info != null) && ((info.mods & STATIC) == 0);
        }
        else
            return false;
    }


/** does function 'f' override function definition `other'?
 *  PRE: f.name == other.name
 */
    public boolean overrides(Definition f, Definition other)
    {
        return !f.isConstructor() &&
               !other.isConstructor() &&
                (other.kind == FUN) &&
                (f.kind == FUN) &&
                ((other.modifiers & PRIVATE) == 0) &&
                types.sametypes(f.type.argtypes(), other.type.argtypes());
    }

/** the implementation of (abstract) definition 'f' in class 'root';
 *  null if none exists
 */
    public Definition implementation(Definition f, Definition root)
    {
        for (Definition e = root.locals().lookup(f.name); e.scope != null; e = e.next())
            if (overrides(f, e.def))
                return e.def;
        return null;
    }
}

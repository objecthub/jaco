//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import java.io.*;
import jaco.framework.*;
import jaco.java.component.*;


public interface DefCompleter
{
    void completeDef(Definition def);
}

public class Definition implements DefinitionConst, ModifierConst
{
/** scope entry fields
 */
    public Definition   def;                // the referenced definition
    public Definition   shadowed = null;    // the next entry in the same hash bucket
    public Definition   sibling = null;     // the next entry in the same scope
    public Scope        scope = null;       // the entry's scope

/** the kind of this definition
 */
    public int          kind;

/** modifiers of this definition
 */
    public int  modifiers;

/** name of the definition in internal representation
 */
    public Name name;

/** qualified name (or real name for MethodDefs)
 */
    public Name fullname;

/** type of this definition
 */
    public Type type;

/** owner of this definition
 */
    public Definition   owner;

/** definition completer (== null, if definiton already complete)
 */
    public DefCompleter completer;


    case Proxy();
    public case PackageDef(Scope locals);
    public case ClassDef(Type supertype, Type[] interfaces,
                         Scope locals, Pool pool, Name sourcefile);
    public case MethodDef(Code code);
    public case OperatorDef(int opcode);
    public case VarDef(ContextEnv initializer, int adr, int pos);
    public case ErrorDef();


/** generate a proxy for this definition
 */
    public Definition proxy(Name name)
    {
        Proxy   proxy = new Proxy();
        proxy.kind = PROXY;
        proxy.name = name;
        proxy.def = this;
        proxy.owner = this.owner;
        return proxy;
    }


/** complete a definition
 */
    public Definition complete()
    {
        if (completer != null)
        {
            DefCompleter    defcompl = completer;
            completer = null;
            defcompl.completeDef(this);
        }
        return this;
    }

/** the name of this definition
 */
    public String toString()
    {
        if (name == PredefConst.INIT_N)
            return "constructor";
        else if ((kind == PCK) && (name.length() == 0))
            return "the unnamed package";
        else
            return name.toString();
    }

/** is this definition (directly or indirectly) local to a method?
 *  also includes fields of inner classes.
 */
    public boolean isLocal()
    {
        return owner.kind == FUN || ((owner.kind == TYP) && owner.isLocal());
    }

/** is this definition a constructor?
 */
    public boolean isConstructor()
    {
        return (name == PredefConst.INIT_N);
    }

/** the closest enclosing class or package
 */
    public Definition enclClass()
    {
        Definition c = this;
        while ((c.kind & (PCK | TYP)) == 0)
            c = c.owner;
        return c;
    }
    
/** the closest enclosing top-level class
 */
    public Definition topLevelClass()
    {
        Definition c = this;
        while (true) {
            while ((c.kind & (PCK | TYP)) == 0)
                c = c.owner;
            if (((c.kind & TYP) != 0) &&
                ((c.modifiers & STATIC) == 0) &&
                ((c.owner.kind & PCK) == 0))
                c = c.owner;
            else
                break;
        }
        return c;
    }
    
/** the outermost class which indirectly owns this definition
 */
    public Definition outermostClass()
    {
        Definition  def = this;
        Definition  result = null;
        while (def.kind != PCK)
        {
            if (def.kind == TYP)
                result = def;
            def = def.owner;
        }
        return result;
    }

    public Type supertype()
    {
        if (completer != null)
            complete();
        switch (this)
        {
            case PackageDef(_):
                return null;
            
            case ClassDef(Type supertype, _, _, _, _):
                return supertype;
            
            default:
                throw new InternalError();
        }
    }
    
    public Type[] interfaces()
    {
        if (completer != null)
            complete();
        switch (this)
        {
            case PackageDef(_):
                return null;
            
            case ClassDef(_, Type[] interfaces, _, _, _):
                return interfaces;
            
            default:
                throw new InternalError();
        }
    }
    
    public Scope locals()
    {
        if (completer != null)
            complete();
        switch (this)
        {
            case PackageDef(_):
                return ((PackageDef)this).locals;
            
            case ClassDef(_, _, _, _, _):
                return ((ClassDef)this).locals;
                
            default:
                throw new InternalError();
        }
    }
    
    public void setSupertype(Type t)
    {
        switch (this)
        {
            case ClassDef(_, _, _, _, _):
                ((ClassDef)this).supertype = t;
                break;
            
            default:
                throw new InternalError();
        }
    }

    public void setInterfaces(Type[] is)
    {
        switch (this)
        {
            case ClassDef(_, _, _, _, _):
                ((ClassDef)this).interfaces = is;
                break;
            
            default:
                throw new InternalError();
        }
    }

    public void setLocals(Scope s)
    {
        switch (this)
        {
            case PackageDef(_):
                ((PackageDef)this).locals = s;
                break;
            
            case ClassDef(_, _, _, _, _):
                ((ClassDef)this).locals = s;
                break;
                
            default:
                throw new InternalError();
        }
    }

    public String location()
    {
        if ((owner.name != null) && (owner.name != PredefConst.EMPTY_N))
            return " in " + owner;
        else
            return "";
    }
    
/** address of a variable definition
 */
    public int address()
    {
        switch (this)
        {
            case VarDef(_, int adr, _):
                return adr;
            
            default:
                throw new InternalError();
        }
    }

/** set address of a variable definition
 */
    public void setAddress(int adr)
    {
        switch (this)
        {
            case VarDef(_, _, _):
                ((VarDef)this).adr = adr;
                break;
                
            default:
                throw new InternalError();
        }
    }

/** next symbol with the same name as this symbol
 */
    public Definition next()
    {
        Definition def = shadowed;
        if (shadowed == null) { // this is a hack! I still have to
            this.scope = null;  // figure out what's going wrong here.
            return this;
        }
        while ((def.scope != null) && (def.name != name))
            def = def.shadowed;
        return def;
    }

/** return members of given kind
 */
    public Definition[] members(int kind)
    {
        Definition      elems = locals().elems;
        int             numElems = 0, i = 0;
        while (elems != null)
        {
            if (elems.def.kind == kind)
                numElems++;
            elems = elems.sibling;
        }
        Definition[]    ms = new Definition[numElems];
        elems = locals().elems;
        while (i < numElems)
        {
            if (elems.def.kind == kind)
                ms[i++] = elems.def;
            elems = elems.sibling;
        }
        return ms;
    }


    public static interface Factory
    {
        Definition PackageDef(Name name, Type type, Definition owner);
        Definition ClassDef(int mods, Name name, Type type, Definition owner);
        Definition ClassDef(int mods, Name name, Definition owner);
        Definition MethodDef(int mods, Name name, Type type, Definition owner);
        Definition ConstructorDef(int mods, Type type, Definition owner);
        Definition OperatorDef(Name name, Type type, int opcode);
        Definition VarDef(int mods, Name name, Type type, Definition owner);
        Definition ErrorDef(Definition owner);
    }
}

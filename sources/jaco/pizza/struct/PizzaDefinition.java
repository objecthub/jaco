//      /   _ _      JaCo
//  \  //\ / / \     - pizza definitions
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.struct;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import Definition.*;


public class CDef extends ClassDef
{
/** this is the base class of the definition; == this, if it is
 *  not an algebraic class
 */
    public CDef         baseClass;

/** the number of tags of an algebraic class or the tag of a case def
 */
    public int          tag;

/** the original class definition data (before the translation to pure Java)
 */
    public int          origModifiers;
    public Type         origSupertype;
    public Type[]       origInterfaces;
    public Scope        origLocals;
    
/** the meta data
 */
    public String metaData;
    
    
/** exchange current class definition data with the orig* variables
 */
    public void swap()
    {
        if (origSupertype == null)
        {
            origModifiers = modifiers;
            origSupertype = supertype;
            origInterfaces = interfaces;
            origLocals = locals;
        }
        else
        {
            int     mods = modifiers;
            Type    supert = supertype;
            Type[]  intfs = interfaces;
            Scope   scope = locals;
            supertype = origSupertype;
            interfaces = origInterfaces;
            locals = origLocals;
            modifiers = origModifiers;
            origSupertype = supert;
            origInterfaces = intfs;
            origLocals = scope;
            origModifiers = mods;
        }
    }
    
/** constructor
 */
    public CDef(Name name, int mods, Type type, Definition owner)
    {
        super(null, new Type[0], null, null, null);
        this.kind = DefinitionConst.TYP;
        this.modifiers = mods;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.def = this;
        this.baseClass = this;
    }
}

public class MDef extends MethodDef
{
/** if this is a constructor for an algebraic datatype, then
 *  this is the tag
 */
    public int tag;
    
/** the meta data
 */
    public String metaData;
    
/** constructor
 */
    public MDef(Name name, int mods, Type type, Definition owner)
    {
        super(null);
        this.kind = FUN;
        this.modifiers = mods;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.def = this;
    }
}

public class VDef extends VarDef
{
/** if this is a constructor for an algebraic datatype, then
 *  this is the tag
 */
    public int tag;
    
/** the meta data
 */
    public String metaData;
    
/** constructor
 */
    public VDef(Name name, int mods, Type type, Definition owner)
    {
        super(null, -1, -1);
        this.kind = VAR;
        this.modifiers = mods;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.def = this;
    }
}

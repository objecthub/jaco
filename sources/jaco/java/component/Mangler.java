//      /   _ _      JaCo
//  \  //\ / / \     - name mangling module
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import java.util.Hashtable;
import Tree.*;
import Definition.*;


public class Mangler extends Component
                     implements DefinitionConst, TypeConst
{
/** other components
 */
    Definitions         definitions;
    
    
/** name mangling archive
 */
    public Hashtable    mangled;

/** separation name
 */
    public Name         separator;


/** component name
 */
    public String getName()
    {
        return "JavaMangler";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        definitions = context.Definitions();
        mangled = new Hashtable();
        separator = Name.fromString("$");
    }


/** return the last part of a class name.
 */
    public Name shortName(Name classname)
    {
        return classname.subName(classname.lastPos((byte)'.') + 1,
                                 classname.length());
    }

/** return the package name of a class name, excluding the trailing '.',
 *  "" if not existent
 */
    public Name packagePart(Name classname)
    {
        return classname.subName(0, classname.lastPos((byte)'.'));
    }


/** mangle a class definition
 */
    public void mangle(Name fullname, ClassDef owner, Name name, int mods)
    {
        put(fullname, owner, name, mods);
    }
    
/** unmangle a fully qualified name
 */
    public String unmangle(Name fullname)
    {
        Mangle  info = get(fullname);
        if (info == null)
            return fullname.toString();
        else
            return unmangle(info.owner.fullname) + "." + info.name;
    }
    
/** return short unmangled portion of fully qualified name
 */
    public Name unmangleShort(Name shortname, Name fullname)
    {
        Mangle  info = get(fullname);
        if (info == null)
            return shortname;
        else
            return info.name;
    }

/** form fully qualified name from a classname and its owner
 */
    public Name formFullName(Name name, Definition owner)
    {
        if ((owner != null) && (owner.kind != PCK))
            return owner.enclClass().fullname.append(separator).append(name);
        else
            return definitions.formFullName(name, owner);
    }

/** form short name from a classname and its owner
 */
    public Name formShortName(Name name, Definition owner)
    {
        if ((owner != null) && (owner.kind != PCK))
            return owner.enclClass().name.append(separator).append(name);
        else
            return name;
    }

/** access methods
 */
    public void put(Name fullname, Definition owner, Name name, int mods)
    {
        mangled.put(fullname, new Mangle(owner, name, mods));
    }

    public Mangle get(Name fullname)
    {
        return (Mangle)mangled.get(fullname);
    }


    public static class Mangle
    {
        public Definition   owner;
        public Name         name;
        public int          mods;
        
        
        Mangle(Definition owner, Name name, int mods)
        {
            this.owner = owner;
            this.name = name;
            this.mods = mods;
        }
        
        public java.lang.String toString()
        {
            return "Mangle(" + owner + ", " + name + ", " + mods + ")";
        }
    }
}

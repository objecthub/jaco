//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    variable definitions
//                           
//  [XClassDef.java (5009) 9-May-01 18:14 -> 6-Jul-01 01:24]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.struct.*;
import Definition.*;


public class XVarDef extends VarDef {

	/** if this is a constructor for an algebraic datatype, then
	 *  this is the tag
	 */
    public int tag;
    
	/** constructor
	 */
    public XVarDef(Name name, int mods, Type type, Definition owner) {
        super(null, -1, -1);
        this.kind = VAR;
        this.modifiers = mods;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.def = this;
    }
}

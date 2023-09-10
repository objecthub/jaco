//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    definition factory
//                           
//  [XDefinitionFactory.java (1304) 2-Apr-01 02:28 -> 2-Apr-01 01:35]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.keris.component.*;
import Definition.*;


public class XDefinitionFactory extends DefinitionFactory
                                implements XModifierConst {
    
    public XDefinitionFactory(Definitions definitions, Types types) {
        super(definitions, types);
    }
    
    public Definition ClassDef(int mods, Name name, Type type, Definition owner) {
        ClassDef def = new XClassDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition ClassDef(int mods, Name name, Definition owner) {
        ClassDef def = new XClassDef(name, mods, null, owner);
        def.type = types.make.ClassType(types.packageType).setDef(def);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
    
    public Definition MethodDef(int mods, Name name, Type type, Definition owner) {
        MethodDef def = new XMethodDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        if ((mods & XModifierConst.MODULE) != 0)
        	throw new Error();
        return def;
    }
    
    public Definition VarDef(int mods, Name name, Type type, Definition owner) {
        VarDef def = new XVarDef(name, mods, type, owner);
        def.fullname = definitions.formFullName(name, owner);
        return def;
    }
}

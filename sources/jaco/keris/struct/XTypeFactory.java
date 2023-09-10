//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type factory
//                           
//  [XTypeFactory.java (928) 9-Jul-01 01:35 -> 1-Sep-01 01:34]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import Type.*;
import Definition.*;


public class XTypeFactory extends TypeFactory implements XType.Factory {
    
    public XTypeFactory(Types types) {
        super(types);
    }
    
    public Type ClassType(Type outer) {
        return new XType.XClassType(outer);
    }
    
    public Type ModuleType(Type outer) {
        return new XType.ModuleType(outer);
    }
    
    public Type CompoundType(Type[] components) {
        return new XType.CompoundType(components);
    }
}

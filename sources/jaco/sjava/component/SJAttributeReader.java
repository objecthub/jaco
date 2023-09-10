//      /   _ _      JaCo
//  \  //\ / / \     - extended attribute reader
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.sjava.struct.*;
import Type.*;
import java.util.Hashtable;


public class SJAttributeReader extends AttributeReader
                               implements SJAttributeConst, SJModifierConst {
    
    Hashtable activeMethods;
    
    public SJAttributeReader(ClassReader reader) {
        super(reader);
        activeMethods = ((SJClassReader)reader).activeMethods;
    }
    
    public int nameToId(Name name) {
        if (name == JACO_ACTIVE_N)
            return JACO_ACTIVE_ATTR;
        else
            return super.nameToId(name);
    }
    
    public void readAttribute(Definition def, int attr, int attrLen) {
        switch (attr) {
            case JACO_ACTIVE_ATTR:
                int tag = in.nextChar();
                switch (def) {
                    case MethodDef(_):
                        def.modifiers |= ACTIVE;
                        break;
                    case ClassDef(_, _, _, _, _):
                        def.modifiers |= ACTIVE;
                        activeMethods.put(def, new Integer(tag));
                        break;
                }
                break;

            default:
                super.readAttribute(def, attr, attrLen);
        }
    }
}

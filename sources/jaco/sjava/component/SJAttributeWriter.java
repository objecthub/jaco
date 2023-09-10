//      /   _ _      JaCo
//  \  //\ / / \     - extended attribute writer
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.sjava.struct.*;
import java.util.*;


public interface SJAttributeConst extends AttributeConst {
    int JACO_ACTIVE_ATTR = 0x0100;
    Name JACO_ACTIVE_N = Name.fromString("JacoActive");
}

public class SJAttributeWriter extends AttributeWriter
                                implements SJAttributeConst, SJModifierConst {
    
    Hashtable activeMethods;
    
    public SJAttributeWriter(ClassWriter parent) {
        super(parent);
        activeMethods = ((SJClassWriter)parent).activeMethods;
    }
    
    public boolean writeAttribute(int attr, Definition def) {
        switch (attr) {
            case JACO_ACTIVE_ATTR:
                int tag = -1;
                switch (def) {
                    case MethodDef(_):
                        Integer id = (Integer)activeMethods.get(new MethodNameId(def));
                        if (id != null)
                            tag = id.intValue();
                        break;
                    case ClassDef(_, _, _, _, _):
                        Integer id = (Integer)activeMethods.get(def.fullname);
                        if (id != null)
                            tag = id.intValue();
                        break;
                }
                if (tag >= 0) {
                    int adr = writeAttr(JACO_ACTIVE_N);
                    out.writeChar(tag);
                    return endAttr(adr);
                } else
                    return false;
        }
        return super.writeAttribute(attr, def);
    }
}

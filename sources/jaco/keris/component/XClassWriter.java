//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    classfile writer
//                           
//  [XClassWriter.java (977) 16-May-01 16:50 -> 23-Jun-01 00:05]

package jaco.keris.component;

import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;


public class XClassWriter extends ClassWriter
                          implements XAttributeConst {
    
    protected Map bridges = new HashMap();
    protected Signatures sigs;
    
    
    public String getName() {
        return "XClassWriter";
    }
    
    public void init(CompilerContext context) {
        super.init(context);
        sigs = context.mainContext.Signatures();
        classAttr |= JACO_MODULE_ATTR
                  |  JACO_CLASSFIELD_ATTR
                  |  JACO_SCOPE_ATTR;
    }
    
    public AttributeWriter AttributeWriter() {
        return new XAttributeWriter(this);
    }
}

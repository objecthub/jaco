//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    classfile reader
//                           
//  [XClassReader.java (1045) 16-May-01 16:51 -> 23-Jun-01 00:04]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.io.IOException;


public class XClassReader extends ClassReader
                          implements XAttributeConst {
    
    protected Signatures sigs;
    
    public String getName() {
        return "XClassReader";
    }
    
    public void init(MainContext context) {
        super.init(context);
        sigs = context.Signatures();
        classAttr |= JACO_MODULE_ATTR
                  |  JACO_CLASSFIELD_ATTR
                  |  JACO_SCOPE_ATTR;
    }
    
    protected AttributeReader AttributeReader() {
        return new XAttributeReader(this);
    }
    
    protected void readClassfileStruct(Definition c) throws IOException {
        super.readClassfileStruct(c);
        if (((XMainContext)context).algebraicClasses)
            ((XClassDef)c).swap();
    }
}

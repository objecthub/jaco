//      /   _ _      JaCo
//  \  //\ / / \     - extended class writer
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.sjava.context.*;
import java.util.Hashtable;


public class SJClassWriter extends ClassWriter implements SJAttributeConst {
    
    Hashtable activeMethods;
    
	public String getName() {
		return "SJClassWriter";
	}
	
	public void init(CompilerContext context) {
		super.init(context);
        activeMethods = ((SJMainContext)context.mainContext).activeMethods;
		classAttr |= JACO_ACTIVE_ATTR;
        methodAttr |= JACO_ACTIVE_ATTR;
	}
	
	public AttributeWriter AttributeWriter() {
		return new SJAttributeWriter(this);
	}
}

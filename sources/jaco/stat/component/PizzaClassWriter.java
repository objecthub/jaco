//      /   _ _      JaCo
//  \  //\ / / \     - extended class writer
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.java.component.*;
import jaco.java.context.*;


public class PizzaClassWriter extends ClassWriter
                              implements PizzaAttributeConst
{
    public String getName()
    {
        return "PizzaClassWriter";
    }
    
    public void init(CompilerContext context)
    {
        super.init(context);
        classAttr |= JACO_INTERFACE_ATTR;
    }
    
    public AttributeWriter AttributeWriter()
    {
        return new PizzaAttributeWriter(this);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - extended class writer
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.java.component.*;
import jaco.java.context.*;
import java.util.*;


public class PizzaClassWriter extends ClassWriter
                              implements PizzaAttributeConst
{
    protected Map bridges = new HashMap();
    
    public String getName()
    {
        return "PizzaClassWriter";
    }
    
    public void init(CompilerContext context)
    {
        super.init(context);
        classAttr |= JACO_INTERFACE_ATTR | JACO_META_ATTR;
        methodAttr |= JACO_META_ATTR;
        fieldAttr |= JACO_META_ATTR;
    }
    
    public AttributeWriter AttributeWriter()
    {
        return new PizzaAttributeWriter(this);
    }
}

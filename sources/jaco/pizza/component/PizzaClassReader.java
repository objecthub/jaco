//      /   _ _      JaCo
//  \  //\ / / \     - extended class reader
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.pizza.context.*;
import jaco.pizza.struct.*;
import java.io.IOException;


public class PizzaClassReader extends ClassReader
                              implements PizzaAttributeConst
{
    public String getName()
    {
        return "PizzaClassReader";
    }
    
    public void init(MainContext context)
    {
        super.init(context);
        classAttr |= JACO_INTERFACE_ATTR;
    }
    
    protected AttributeReader AttributeReader()
    {
        return new PizzaAttributeReader(this);
    }
    
    protected void readClassfileStruct(Definition c) throws IOException
    {
        super.readClassfileStruct(c);
        if (((MainPizzaContext)context).algebraicClasses)
            ((CDef)c).swap();
    }
}

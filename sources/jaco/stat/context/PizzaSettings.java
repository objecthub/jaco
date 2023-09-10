//      /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the Pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 31/03/98

package jaco.stat.context;

import jaco.framework.*;
import jaco.java.context.*;


public class PizzaSettings extends JavaSettings
{
    public boolean  pizza = true;
    public boolean  keepgenerated = false;
    
    
    public String resourceBase()
    {
        return "jaco.pizza.resources";
    }
    
    protected void parseSwitch(String option) throws AbortCompilation
    {
        if (option.equals("-java"))
            pizza = false;
        else
        if (option.equals("-keepgenerated"))
            keepgenerated = true;
        else
            super.parseSwitch(option);
    }
    
    public String getUsage()
    {
        return "pico {<option>} {<file>}";
    }
    
    public String toString()
    {
        return super.toString() + "\n" +
               "pizza = " + pizza + "\n" +
               "keepgenerated = " + keepgenerated;
    }
    
    public JavaContext JavaContext()
    {
        if (pizza)
            return new PizzaContext(this);
        else
            return new JavaContext(this);
    }
}

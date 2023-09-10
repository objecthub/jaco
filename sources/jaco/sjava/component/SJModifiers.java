//      /   _ _      JaCo
//  \  //\ / / \     - java modifiers support
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.java.component.*;



public interface SJModifierConst extends ModifierConst
{
    /** sjava active modifier */
    int ACTIVE			= 0x1000;
}


public class SJModifiers extends Modifiers implements SJModifierConst
{
    /** component name
     */
    public String getName()
    {
	return "SJavaModifiers";
    }

    /** component initialization
 */
    public void init(MainContext context)
    {
	modcount = 13;
	super.init(context);		
	LocalClassMods |= ACTIVE;
	InnerClassMods |= ACTIVE;
	ClassMods |= ACTIVE;
	modstring[12] = "active";
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - components define methods and variables that are related
//   \//  \\_\_/       to each other or a data structure
//         \         Matthias Zenger, 20/12/97

package jaco.framework;


public abstract class Component
{
/** the context of the component
 */
    public Context  context;
    
    
/** this method is called by the contexts in order to initialize the component
 */
    public void init(Context context)
    {
        this.context = context;
        if (context.settings.componentInit)
            System.out.println("[initializing component " + getName() + "]");
    }
    
/** get the name of the component
 */
    public abstract String getName();
}

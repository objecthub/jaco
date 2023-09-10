//      /   _ _      JaCo
//  \  //\ / / \     - toplevel class for all contexts
//   \//  \\_\_/     
//         \         Matthias Zenger, 09/01/98

package jaco.framework;


public abstract class Context {

/** compiler settings
 */
    public Settings         settings;

/** support for debugging the compiler
 */
    public Debug            debug;
    
    
/** generate a nested context
 */
    protected Context(Context enclContext) {
        this.settings = enclContext.settings;
        this.debug = enclContext.debug;
        showCreation();
    }
    
/** generate a new context based on a settings object
 */
    protected Context(Settings settings) {
        this.settings = settings;
        this.debug = new Debug(settings.debugspec);
        showCreation();
    }

/** output creation message
 */
    protected void showCreation() {
        if (settings.contextInit)
            System.out.println("[creating context " + getName() + "]");
    }

/** context name
 */
    protected String getName() {
        return getClass().toString().substring(6);
    }
}

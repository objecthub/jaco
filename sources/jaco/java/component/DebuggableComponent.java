//      /   _ _      JaCo
//  \  //\ / / \     - superclass of debuggable components
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;


public abstract class DebuggableComponent extends Component
                                          implements Debuggable
{
/** other components
 */
    protected ErrorHandler      report;
    protected PrettyPrinter     pretty;
    

/** default initializer
 */
    public void init(MainContext context)
    {
        super.init(context);
        report = context.ErrorHandler();
        pretty = context.PrettyPrinter();
    }
    
/** default getDebugName method; returns an invalid debug name
 */
    public String getDebugName()
    {
        return "@";
    }
    
/** debug method for debugging during tree processing
 */
    public boolean debug(int debugId)
    {
        return context.debug.debugSwitchSet(getDebugName(), debugId);
    }
    
/** default debug method; returns true, if debugging is switched on
 */
    public boolean debug(int debugId, TreeList treelist) throws AbortCompilation
    {
        switch (debugId)
        {
            case Debug.ENTER:
            case Debug.EXIT:
                if (context.debug.debugSwitchSet(getDebugName(), debugId))
                {
                    treelist.process(pretty);
                    return true;
                }
                else
                    return false;
            
            default:
                return context.debug.debugSwitchSet(getDebugName(), debugId);
        }
    }
    
/** static debug method
 */
    public static boolean debug(int debugId, Debuggable processor,
                                Context context, ErrorHandler report)
    {
        if (debugId == Debug.ENTER)
            report.note("[" + processor.getDescription() + "]");
        return context.debug.debugSwitchSet(processor.getDebugName(), debugId);
    }
}

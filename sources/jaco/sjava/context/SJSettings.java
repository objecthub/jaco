//      /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the SJ compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.sjava.context;

import jaco.framework.*;
import java.util.*;
import java.text.*;


public class SJSettings extends jaco.java.context.JavaSettings {
/** switches
 */
    public boolean java = false;
    public boolean preproc = false;
    public boolean selectLabel = false;
    
    
/** specifies the resource base directory for the compiler
 */
    public String resourceBase() {
        return "jaco.sjava.resources";
    }

/** command-line parsing
 */
    protected boolean parseOption(String option, String arg) throws AbortCompilation {
        if (option.equals("-newoption")) {
            // set something to arg
        } else
            return super.parseOption(option, arg);
        return true;
    }
    
    protected void parseSwitch(String option) throws AbortCompilation {
        if (option.equals("-java"))
            java = true;
        else if (option.equals("-p"))
            preproc = true;
	else if (option.equals("-label"))
	    selectLabel = true;
        else	    
            super.parseSwitch(option);
    }

/** get the usage of the compiler
 */
    public String getUsage() {
        return "sjavac {<option>} {<file>}";
    }

/** overrides Object
 */
    public String toString() {
        return super.toString() + "\n" +
               "java = " + java + "\n" +
               "p = " + preproc;
    }

/** factory method for the initial context
 */
    public SJContext SJContext() {
        return new SJContext(this);
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the Wrapper compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.context;

import jaco.framework.*;
import java.util.*;
import java.text.*;


public class WrapperSettings extends jaco.java.context.JavaSettings {
/** switches
 */
    public boolean  java = false;

/** specifies the resource base directory for the compiler
 */
    public String resourceBase() {
        return "jaco.wrapper.resources";
    }

/** configure settings with resource file
 
    protected void configure(String rsrc) {
        super.configure(rsrc);
        outpath = getRsrcSetting("path");
        classpath = getRsrcSetting("classpath");
        sourcepath = getRsrcSetting("sourcepath");
    }
*/

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
        else
            super.parseSwitch(option);
    }

/** get the usage of the compiler
 */
    public String getUsage() {
        return "wrapperc {<option>} {<file>}";
    }

/** overrides Object
 */
    public String toString() {
        return super.toString() + "\n" +
               "java = " + java;
    }

/** factory method for the initial context
 */
    public WrapperContext WrapperContext() {
        return new WrapperContext(this);
    }
}

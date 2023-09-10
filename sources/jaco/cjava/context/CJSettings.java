//  /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the CJ compiler
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import java.util.*;
import java.text.*;


public class CJSettings extends jaco.java.context.JavaSettings {
/** switches
 */
    public boolean  java = false;

/** specifies the resource base directory for the compiler
 */
    public String resourceBase() {
    return "jaco.cjava.resources";
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
    return "cjavac {<option>} {<file>}";
    }

/** overrides Object
 */
    public String toString() {
    return super.toString() + "\n" +
           "java = " + java;
    }

/** factory method for the initial context
 */
    public JavaContext CJContext() {
        if (java)
            return new JavaContext(this);
        else
        return new CJContext(this);
    }
}

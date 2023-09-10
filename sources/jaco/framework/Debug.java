//      /   _ _      JaCo
//  \  //\ / / \     - support for compiler debugging
//   \//  \\_\_/     
//         \         Matthias Zenger, 10/01/98

package jaco.framework;

import jaco.framework.*;
import java.util.Hashtable;


public class Debug
{
/** debug codes
 */
    public static final int ENTER   = 0x01;
    public static final int EXIT    = 0x02;

/** debugger switches
 */
    Hashtable       debugCodes = new Hashtable();
    
    
/** construct a debugger out of a debug string specification
 */
    public Debug(String debugspec)
    {
        parseDebugSpec(debugspec);
    }
    
/** parse a debug string specification;
 *  format: <name>@<number>,...
 */
    public boolean parseDebugSpec(String str)
    {
        int left = 0;
        int mid;
        int right;
        
        while (left < str.length())
            if ((mid = str.indexOf("@", left)) > left)
            {
                right = str.indexOf(",", mid);
                right = (right > mid) ? right : str.length();
                int code;
                try
                {
                    code = Integer.parseInt(str.substring(mid + 1, right), 16);
                }
                catch (NumberFormatException e)
                {
                    return false;
                }
                debugCodes.put(str.substring(left, mid), new Integer(code));
                left = right + 1;
            }
            else
                return false;
        return true;
    }
    
/** check debug specification
 */
    public static void checkDebugSpec(String str) throws AbortCompilation
    {
        int left = 0;
        int mid;
        int right;
        
        if (str == null)
            return;
        while (left < str.length())
            if ((mid = str.indexOf("@", left)) > left)
            {
                right = str.indexOf(",", mid);
                right = (right > mid) ? right : str.length();
                try
                {
                    Integer.parseInt(str.substring(mid + 1, right), 16);
                }
                catch (NumberFormatException e)
                {
                    throw new AbortCompilation(AbortCompilation.PANIC,
                                            "bad debug spec: " + str);
                }
                left = right + 1;
            }
            else
                throw new AbortCompilation(AbortCompilation.PANIC,
                                            "bad debug spec: " + str);
    }
    
/** is a specific debug switch for component 'name' set?
 */
    public boolean debugSwitchSet(String name, int debugMask)
    {
        Integer debugCode = (Integer)debugCodes.get(name);
        return ((debugCode != null) && ((debugCode.intValue() & debugMask) != 0));
    }
}

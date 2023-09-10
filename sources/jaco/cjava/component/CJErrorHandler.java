//      /   _ _      JaCo
//  \  //\ / / \     - global error handler and output module
//   \//  \\_\_/     
//         \         Matthias Zenger, 04/10/99

package jaco.cjava.component;

import jaco.java.component.*;
import java.util.*;

/**
 * Add a list of errors to be ignored to the error handler.
 *
 * By default no error is ignored.
 */
public class CJErrorHandler extends ErrorHandler
{
    /**
     * List of keys (String) of errors to ignor..
     */
    protected Set ignoreKeys = new HashSet();

    /**
     * Ignore a message, represented by its key.
     *
     * @param key the key, as passed to error()
     */
    public void ignore(String key)
    {
    ignoreKeys.add(key);
    }

    /**
     * Show and record errors, if they should not be ignored.
     */
    public void error(int pos, String key, Object arg0, Object arg1,
                      Object arg2, Object arg3) {
    if(!ignoreKeys.contains(key))
        super.error(pos, key, arg0, arg1, arg2, arg3);
    else
        note("[ignored error." + key + "]");
    }
    
}

//      /   _ _      JaCo
//  \  //\ / / \     - an ant task for JaCo
//   \//  \\_\_/     
//         \         Matthias Zenger, 13/12/2001

package jaco.java;

import jaco.framework.ant.*;


public class AntTask extends AntCompilerTask  {
    public String compilerAdaptor() {
        return "jaco.java.AntAdaptor";
    }
}

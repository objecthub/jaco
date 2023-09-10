//  /   _ _      JaCo
//  \  //\ / / \     - main compiler driver
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava;

import jaco.cjava.context.*;
import jaco.framework.*;


public class Main {
    public static void main(String[] args) {
    CJSettings jc = new CJSettings();
    try {
        ((CJSettings)jc.parse(args))
                           .CJContext()
                           .JavaCompiler()
                           .compile();
    }
    catch (AbortCompilation e) {
        System.out.println(e);
        if(jc.make)
        System.exit(-1);
    }
    }
}

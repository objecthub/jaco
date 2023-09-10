//      /   _ _      JaCo
//  \  //\ / / \     - main compiler driver
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop;

import jaco.javaop.context.*;
import jaco.framework.*;


public class Main {
    public static void main(String[] args) {
        try {
            ((OPSettings)new OPSettings().parse(args))
                                                   .OPContext()
                                                   .JavaCompiler()
                                                   .compile();
        }
        catch (AbortCompilation e) {
            System.out.println(e);
        }
    }
}

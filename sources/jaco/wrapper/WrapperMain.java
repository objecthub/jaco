//      /   _ _      JaCo
//  \  //\ / / \     - main compiler driver
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper;

import jaco.wrapper.context.*;
import jaco.framework.*;


public class Main {
    public static void main(String[] args) {
        try {
            ((WrapperSettings)new WrapperSettings().parse(args))
                                                   .WrapperContext()
                                                   .JavaCompiler()
                                                   .compile();
        }
        catch (AbortCompilation e) {
            System.out.println(e);
        }
    }
}

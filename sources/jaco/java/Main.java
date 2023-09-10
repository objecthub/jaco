//      /   _ _      JaCo
//  \  //\ / / \     - main compiler driver
//   \//  \\_\_/     
//         \         Matthias Zenger, 07/03/00

package jaco.java;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.framework.*;
import java.lang.*;

public class Main
{
    public static void main(java.lang.String[] args)
    {
        JavaSettings js = new JavaSettings();
        try
        {
            js.parse(ProcessArgs.expand(args));
            if (!js.JavaContext().JavaCompiler().compile() && js.make)
                System.exit(-1);
        }
        catch (AbortCompilation e)
        {
            System.out.println(e);
            if (js.make)
                System.exit(-1);
        }
    }
}

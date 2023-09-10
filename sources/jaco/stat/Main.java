//      /   _ _      JaCo
//  \  //\ / / \     - main compiler driver for PiCo
//   \//  \\_\_/     
//         \         Matthias Zenger, 20/12/97

package jaco.stat;

import jaco.stat.component.*;
import jaco.stat.context.*;
import jaco.framework.*;


public class Main
{
    public static void main(String[] args)
    {
        PizzaSettings js = new PizzaSettings();
        try
        {
            js.parse(args);
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

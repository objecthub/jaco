//		/	_ _ 	 SJavaC
//	\  //\ / / \	 - main compiler driver
//	 \//  \\_\_/	 
//		   \		 Matthias Zenger, 16/03/00

package jaco.sjava;

import jaco.sjava.context.*;
import jaco.framework.*;


public class Main {
    public static void main(String[] args) {
        SJSettings js = new SJSettings();
	try
	{
            js.parse(args);
            if (!js.SJContext().JavaCompiler().compile() && js.make)
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

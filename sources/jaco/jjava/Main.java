// the main compiler routine
// Stewart Itzstein


package jaco.jjava;

import jaco.java.component.*;
import jaco.jjava.context.*;
import jaco.framework.*;


public class Main
{
    public static void main(String[] args)
    {
        try
        {
            System.out.println("Join Java Prototype 1");
            ((JJavaSettings)new JJavaSettings().parse(args))
                                               .JavaContext()
                                               .JavaCompiler()
                                               .compile();
        }
        catch (AbortCompilation e)
        {
            System.out.println(e);
        }
    }
}

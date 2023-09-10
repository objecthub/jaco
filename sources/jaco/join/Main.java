package jaco.join;

import jaco.join.component.*;
import jaco.join.context.*;
import jaco.framework.*;


public class Main
{
    public static void main(String[] args)
    {
        try
        {
            ((JoinSettings)new JoinSettings().parse(args))
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

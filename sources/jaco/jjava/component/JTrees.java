package jaco.jjava.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.jjava.struct.*;


public class JTrees extends Trees
{
    public String getName()
    {
        return "JTrees";
    }
    
    public void init(MainContext context)
    {
        super.init(context);
        make = new JTreeFactory();
        newdef = new JTreeCreate();
        redef = new JTreeRedef();
    }
}

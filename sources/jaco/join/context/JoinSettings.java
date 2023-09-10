package jaco.join.context;

import jaco.framework.*;
import java.util.*;
import java.text.*;
import jaco.java.context.*;


public class JoinSettings extends JavaSettings
{
    public JoinSettings()
    {
        super();
    }
    
    public String resourceBase()
    {
        return "jaco.join.resources";
    }
    
    protected boolean parseOption(String option, String arg) throws AbortCompilation
    {
        return super.parseOption(option, arg);
    }
    
    protected void parseSwitch(String option) throws AbortCompilation
    {
        super.parseSwitch(option);
    }
    
    public String getUsage()
    {
        return "joco {<option>} {<file>}";
    }
    
    public JavaContext JavaContext()
    {
        return new JoinContext(this);
    }
}

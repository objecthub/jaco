//      /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the Pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 31/03/98

package jaco.jjava.context;

import jaco.framework.*;
import jaco.java.context.*;


public class JJavaSettings extends JavaSettings
{
    public boolean  jjava = true;
    public boolean  keepgenerated = false;
    public int  translation = 0;

    public String resourceBase()
    {
        return "jaco.jjava.resources";
    }
    
    protected void parseSwitch(String option) throws AbortCompilation
    {
        if (option.equals("-version")) {
            System.out.println("Join Java Compiler");
            System.out.println("Version .01 beta - unfinished");
            System.out.println("Base Compiler Courtesy of Matthias Zenger and Martin Odersky");
        }
        else
        if (option.equals("-java"))
            jjava = false;
        else
        if (option.equals("-keepgenerated"))
            keepgenerated = true;
        else
        if (option.equals("-trans0")) {
            System.out.println("Using Translation 0:");
            translation = 0;
        }
        else
        if (option.equals("-trans1")) {
            System.out.println("Using Translation 1:");
            translation = 1;
        }
        else
            super.parseSwitch(option);
    }
    
    public String getUsage()
    {
        return "jjava {<option>} {<file>}";
    }
    
    public String toString()
    {
        return super.toString() + "\n" +
               "jjava = " + jjava + "\n" +
               "keepgenerated = " + keepgenerated;
    }
    
    public JavaContext JavaContext()
    {
        if (jjava)
            return new JJavaContext(this);
        else
            return new JavaContext(this);
    }
}

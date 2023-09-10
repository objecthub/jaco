//      /   _ _      JaCo
//  \  //\ / / \     - compiler settings for the Java compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 29/01/02

package jaco.java.context;

import jaco.framework.*;
import java.util.*;
import java.text.*;


public class JavaSettings extends Settings
{
/** predefined settings
 */
    protected ResourceBundle    settingsRsrc;
    
/** switches
 */
    public boolean  optimize = false;
    public boolean  verbose = false;
    public boolean  stats = false;
    public boolean  debuginfo = false;
    public boolean  nowarn = false;
    public boolean  version = false;
    public boolean  nowrite = false;
    public boolean  assertions = false;
    public boolean  make = false;
    public boolean  prompt = false;
    
    public String   classpath = "";
    public String   sourcepath = null;
    public String   outpath = System.getProperty("user.dir");
    public String   sourceversion = "1.3";
    public String   targetversion = "1.4";
    
/** default constructor
 */
    public JavaSettings()
    {
        if (suffixes.length == 0)
            suffixes = new String[]{".java", ".jaco"};
    }
    
/** specifies the resource base directory for the compiler
 */
    public String resourceBase()
    {
        return "jaco.java.resources";
    }
    
/** configure settings with resource file
 */
    protected void configure(String rsrc)
    {
        super.configure(rsrc);
        outpath = getRsrcSetting("path");
        classpath = getRsrcSetting("classpath");
        sourcepath = getRsrcSetting("sourcepath");
    }
    
/** command-line parsing
 */
    protected boolean parseOption(String option, String arg) throws AbortCompilation
    {
        if (option.equals("-d"))
            outpath = arg;
        else
        if (option.equals("-classpath"))
            classpath = arg;
        else
        if (option.equals("-cp"))
            classpath = arg;
        else
        if (option.equals("-sourcepath"))
            sourcepath = arg;
        else
        if (option.equals("-source"))
            sourceversion = arg;
        else
        if (option.equals("-target"))
            targetversion = arg;
        else
            return super.parseOption(option, arg);
        return true;
    }
    
    protected void parseSwitch(String option) throws AbortCompilation
    {
        if (option.equals("-O"))
            optimize = true;
        else
        if (option.equals("-g"))
            debuginfo = true;
        else
        if (option.equals("-g:none"))
            debuginfo = false;
        else
        if (option.equals("-nowarn"))
            nowarn = true;
        else
        if (option.equals("-nowrite"))
            nowrite = true;
        else
        if (option.equals("-verbose"))
            verbose = true;
        else
        if (option.equals("-version"))
            version = true;
        else
        if (option.equals("-assertions"))
            assertions = true;
        else
        if (option.equals("-make"))
            make = true;
        else
        if (option.equals("-prompt"))
            prompt = true;
    	else
        if (option.equals("-stats"))
            stats = true;
        else
            super.parseSwitch(option);
    }

/** get the usage of the compiler
 */
    public String getUsage()
    {
        return "jaco {<option>} {<file>}";
    }

/** overrides Object
 */
    public String toString()
    {
        return super.toString() + "\n" +
               "optimize = " + optimize + "\n" +
               "debuginfo = " + debuginfo + "\n" +
               "nowarn = " + nowarn + "\n" +
               "nowrite = " + nowrite + "\n" +
               "verbose = " + verbose + "\n" +
               "version = " + version + "\n" +
               "assertions = " + assertions + "\n" +
               "classpath = " + classpath + "\n" +
               "outpath = " + outpath + "\n" +
               "sourcepath = " + sourcepath + "\n" +
               "source = " + sourceversion + "\n" +
               "target = " + targetversion + "\n" +
               "prompt = " + prompt;
    }

/** factory method for the initial context
 */
    public JavaContext JavaContext()
    {
        return new JavaContext(this);
    }
}

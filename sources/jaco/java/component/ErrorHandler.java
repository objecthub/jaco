//      /   _ _      JaCo
//  \  //\ / / \     - global error handler and output module
//   \//  \\_\_/     
//         \         Matthias Zenger, 30/10/00

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.CompilationEnv;
import java.io.*;
import java.util.*;
import java.text.*;


public class ErrorHandler extends Component
{
/** global switches
 */
    JavaSettings                settings;
    
/** total number of errros and warnings
 */
    public int                  nerrors;
    protected int               nwarnings;
    protected int               ndeprecations;
    
/** maximal number of reported errors or warnings
 */
    public int                  maxErrors = 100;
    public int                  maxWarnings = 100;
    public int                  maxDeprecations = 20;

/** jaco resource
 */
    protected ResourceBundle    messageRsrc;

/** the current top level environment
 */
    public CompilationEnv       info;

/** stack of currently processed environments
 */
    protected Stack             environments = new Stack();
    
/** are errors caused by the user possible?
 */
    public boolean              errorsPossible = true;

/** the error message plug-ins
 */
    public Object[]             errStr = new Object[4];
    
/** emit error messages?
 */
    public boolean              silent = false;

/** how many files/lines/tokens did we process?
 */
 	public int					files;
 	public int					lines;
 	public int					tokens;
    public int					classes;
    public int					methods;
    
/** component name
 */
    public String getName()
    {
        return "JavaErrorHandler";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        settings = (JavaSettings)context.settings;
        maxErrors = settings.getRsrcSetting("maxerrors", maxErrors);
        maxWarnings = settings.getRsrcSetting("maxwarnings", maxWarnings);
        maxDeprecations = settings.getRsrcSetting("maxdeprecations", maxDeprecations);
        try
        {
            messageRsrc = ResourceBundle.getBundle(settings.resourceBase() + ".Messages");
        }
        catch (MissingResourceException e)
        {
            throw new InternalError("fatal error -- messages resource is missing.");
        }
    }
    
/** set source environment
 */
    public void useEnv(CompilationEnv info)
    {
        environments.push(this.info);
        this.info = info;
    }
    
/** use previous source environment
 */
    public void usePreviousEnv()
    {
        info = (CompilationEnv)environments.pop();
    }

/** get current source environment
 */
    public CompilationEnv getCurrentEnv()
    {
        return info;
    }

/** set, if errors are possible
 */
    public boolean setErrorsPossible(boolean b)
    {
        boolean ep = errorsPossible;
        errorsPossible = b;
        return ep;
    }
    
/** report errors?
 */
    public boolean setSilent(boolean b)
    {
        boolean s = silent;
        silent = b;
        return s;
    }
    
/** print an error or warning message
 */
    protected boolean printError(int pos, String msg)
    {
        if (!errorsPossible)
        {
            System.out.println("*** suspicious error message");
            System.out.println("there might be something wrong with jaco's translation.");
            inviteBugReport();
        }
        if (info == null)
            System.out.println("error: " + msg);
        else
            return info.source.printMessageIfNew(pos, msg);
        return true;
    }
    
/** format an error message
 */
    protected String format(String key, Object[] args)
    {
        try
        {
            return MessageFormat.format(messageRsrc.getString("error." + key), args);
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }
    
/** report an error
 */
    public void error(int pos, String key)
    {
        error(pos, key, null, null, null, null);
    }
    
    public void error(int pos, String key, Object arg)
    {
        error(pos, key, arg, null, null, null);
    }
    
    public void error(int pos, String key, Object arg0, Object arg1)
    {
        error(pos, key, arg0, arg1, null, null);
    }
    
    public void error(int pos, String key, Object arg0, Object arg1, Object arg2)
    {
        error(pos, key, arg0, arg1, arg2, null);
    }
    
    public void error(int pos, String key, Object arg0, Object arg1,
                      Object arg2, Object arg3) {
        if (!silent && (nerrors < maxErrors)) {
            errStr[0] = (arg0 == null) ? "<err0>" : arg0.toString();
            errStr[1] = (arg1 == null) ? "<err1>" : arg1.toString();
            errStr[2] = (arg2 == null) ? "<err2>" : arg2.toString();
            errStr[3] = (arg3 == null) ? "<err3>" : arg3.toString();
            if (printError(pos, format(key, errStr))) {
                nerrors++;
                if (info != null)
                    info.errors++;
                if (settings.prompt) {
                    throw new Error();
                }
            }
        } else if (!silent) {
            nerrors++;
            if (info != null)
                info.errors++;
        }
    }
    
/** report a warning
 */
    public void warning(int pos, String key)
    {
        warning(pos, key, null, null);
    }
    
    public void warning(int pos, String key, Object arg)
    {
        warning(pos, key, arg, null);
    }
    
    public void warning(int pos, String key, Object arg0, Object arg1)
    {
        if (!silent && !settings.nowarn && errorsPossible && (nwarnings < maxWarnings)) {
            errStr[0] = (arg0 == null) ? "<warn0>" : arg0.toString();
            errStr[1] = (arg1 == null) ? "<warn1>" : arg1.toString();
            if (printError(pos, "warning: " + format(key, errStr))) {
                nwarnings++;
                if (info != null)
                    info.warnings++;
            }
        } else if (!silent) {
            nwarnings++;
            if (info != null)
                info.warnings++;
        }
    }
    
/** report a deprecation
 */
    public void deprecation(int pos, String msg)
    {
        if (!silent && errorsPossible && (ndeprecations < maxDeprecations)) {
            if (printError(pos, "deprecation: " + msg)) {
                ndeprecations++;
                if (info != null)
                    info.deprecations++;
            }
        } else if (!silent) {
            ndeprecations++;
            if (info != null)
                info.deprecations++;
        }
    }
    
/** report a message if verbose mode is switched on
 */
    public void note(String msg)
    {
        if (settings.verbose)
            System.out.println(msg);
    }
    
/** report operation together with time for operation
 */
    public void operation(String operation, long start)
    {
        start = System.currentTimeMillis() - start;
        note("[" + operation + " in " + start + "ms]");
    }
    
/** report a message
 */
    public void print(String msg)
    {
        System.out.println(msg);
    }
    
/** assertions for internal error checking
 */
    protected void assertionFailed()
    {
        if (settings.assertions) {
            inviteBugReport();
            String  s = "assertion failed";
            if ((info != null) && (info.source != null))
                s += " in " + info.source;
            throw new InternalError(s);
        }
    }
    
    public void assert(boolean b)
    {
        if (!b)
            assertionFailed();
    }
    
    public void assert(int i)
    {
        if (i == 0)
            assertionFailed();
    }
    
    public void assert(Object obj)
    {
        if (obj == null)
            assertionFailed();
    }
    
/** ask for a bug report
 */
    public void inviteBugReport()
    {
        System.out.println("please file a bug report by sending your");
        System.out.println("program and the following output to");
        System.out.println("matthias.zenger@epfl.ch -- thank you!");
    }
    
/** print number of error or warning count
 */
    protected void printCount(String kind, int count)
    {
        if (count != 0)
        {
            System.out.print(count);
            System.out.println(" " + kind + ((count == 1) ? "" : "s"));
        }
    }
    
/** print a summary concerning number of errors
 */
    public void printSummary()
    {
        printCount("error", nerrors);
        printCount("warning", nwarnings);
        printCount("deprecation", ndeprecations);
    }
    
/** print some statistics
 */
    public void printStats()
    {
        printCount("file", files);
        printCount("line", lines);
        printCount("token", tokens);
        printCount("generated classe", classes);
        printCount("generated method", methods);
    }
}

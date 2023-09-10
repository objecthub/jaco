//      /   _ _      JaCo
//  \  //\ / / \     - general compiler settings
//   \//  \\_\_/     
//         \         Matthias Zenger, 31/03/98

package jaco.framework;

import java.util.*;
import java.io.*;
import java.text.*;


public abstract class Settings
{
/** predefined settings
 */
    protected ResourceBundle    settingsRsrc;

/** the settings prefix
 */
    protected String            settingPrefix = "setting.";

/** general settings
 */
    public String[]             suffixes = {};
    public Vector               sources = new Vector();
    public Vector               skipped = new Vector();
    
    protected boolean           componentInit = false;
    protected boolean           contextInit = false;
    protected boolean           recursive = false;
    protected boolean           wildcards = false;
    public String               debugspec = "";
    public String               encoding = "8859_1";
    public int                  tabsize = 8;
    
    {
        configure(resourceBase() + ".Settings");
    }


/** specifies the resource base directory for the compiler
 */
    public abstract String resourceBase();

/** get the usage of the compiler
 */
    public abstract String getUsage();

/** configure settings with resource file
 */
    protected void configure(String rsrc)
    {
        try
        {
            settingsRsrc = ResourceBundle.getBundle(rsrc);
            suffixes = getRsrcSetting("suffixes", " ,\t\n\r");
            tabsize = getRsrcSetting("tabsize", 8);
            encoding = getRsrcSetting("encoding");
            try
            {
                Debug.checkDebugSpec(getRsrcSetting("debugspec"));
            }
            catch (AbortCompilation e) {}
            try
            {
                parse(getRsrcSetting("switches", " \t\n\r"));
            }
            catch (AbortCompilation e) {}
            try
            {
                parse(getRsrcSetting("files", " ,\t\n\r"));
            }
            catch (AbortCompilation e) {}
        }
        catch (MissingResourceException e)
        {
            throw new InternalError("fatal error -- " +
                                    "settings resource is missing.");
        }
    }
    
/** get resource setting
 */
    public String getRsrcSetting(String key)
    {
        if (settingsRsrc == null)
            throw new InternalError();
        try
        {
            String  str = settingsRsrc.getString(settingPrefix + key);
            if (str == null)
                return null;
            else
            if (str.startsWith("$"))
                return System.getProperty(str.substring(1));
            else
            if (str.equals("."))
                return null;
            else
                return str;
        }
        catch (MissingResourceException e)
        {
            return null;
        }
    }   

/** get resource setting as integer
 */
    public int getRsrcSetting(String key, int defaultNum)
    {
        String  str = getRsrcSetting(key);
        if (str == null)
            return defaultNum;
        else
            try
            {
                return Integer.parseInt(str);
            }
            catch (NumberFormatException e)
            {
                return defaultNum;
            }
    }
    
/** get resource setting list
 */
    public String[] getRsrcSetting(String key, String delim)
    {
        String  str = getRsrcSetting(key);
        if (str == null)
            return null;
        StringTokenizer tok = new StringTokenizer(str, delim);
        String[]    list = new String[tok.countTokens()];
            for (int i = 0; i < list.length; i++)
                list[i] = tok.nextToken();
        return list;
    }
    
/** command-line parsing
 */
    public Settings parse(String[] args) throws AbortCompilation
    {
        int     i = 0;
        if (args == null)
            return this;
        while (i < args.length)
            if (args[i].startsWith("-"))
            {
                if (((i + 1) < args.length) && parseOption(args[i], args[i + 1]))
                    i += 2;
                else
                    parseSwitch(args[i++]);
            }
            else
            if (wildcards)
                addExpandedSource(args[i++]);
            else
                addSource(null, args[i++]);
        return this;
    }
    
/** parse option with one argument; return true, if option is valid
 */
    protected boolean parseOption(String option, String arg) throws AbortCompilation
    {
        if (option.equals("-debug"))
            Debug.checkDebugSpec(debugspec = arg);
        else
        if (option.equals("-encoding"))
            encoding = arg;
        else
        if (option.equals("-settings"))
            configure(arg);
        else
            return false;
        return true;
    }
    
/** parse simple switch
 */
    protected void parseSwitch(String option) throws AbortCompilation
    {
        if (option.equals("-componentInit"))
            componentInit = true;
        else if (option.equals("-contextInit"))
            contextInit = true;
        else
        if (option.equals("-w"))
            wildcards = true;
        else
        if (option.equals("-r"))
            recursive = true;
        else
            throw new AbortCompilation(AbortCompilation.PANIC,
                                       "unknown switch: " + option + "\n" + getUsage());
    }

/** expand wildcards and add the matching sourcefiles
 */
    protected void addExpandedSource(String arg)
    {
        if (hasWildcards(arg))
        {
            File    f = new File(arg);
            String  path = f.getParent();
            String  name = f.getName();
            if (path == null)
            {
                if (arg.indexOf(File.separator) == 0)
                    path = "";
                else
                    path = ".";
            }
            f = new File(path);
            String[]    content;
            if ((f != null) && ((content = f.list()) != null))
            {
                for (int j = 0; j < content.length; j++)
                    if (matches(content[j], 0, name, 0))
                        addSource(f, content[j]);
                return;
            }
        }
        addSource(null, arg);
    }

/** add file to the sourcefile pool
 */
    protected void addSource(File dir, String arg)
    {
        if (recursive)
        {
            File        f;
            if (dir == null)
                f = new File(arg);
            else
                f = new File(dir, arg);
            String[]    content;
            if (f.exists() && ((content = f.list()) != null))
            {
                for (int j = 0; j < content.length; j++)
                    addSource(f, content[j]);
                return;
            }
        }
        for (int j = 0; j < suffixes.length; j++)
            if (arg.endsWith(suffixes[j]))
            {
                if (dir != null)
                    sources.addElement(new File(dir, arg).getAbsolutePath());
                else
                    sources.addElement(arg);
                return;
            }
        skipped.addElement(arg);
    }
    
/** are there any '*' or '?' characters in string s?
 */
    protected boolean hasWildcards(String s)
    {
        return (s.length() > 0) && 
               ((s.indexOf('*') >= 0) || (s.indexOf('?') >= 0));
    }
    
/** match string s[soffset..] against pattern p[poffset..]?
 */
    protected boolean matches(String s, int soffset, String p, int poffset)
    {
        if (poffset >= p.length())
            return (soffset >= s.length());
        else
        if (p.charAt(poffset) == '*')
            return matches(s, soffset, p, poffset + 1) ||
                   ((soffset < s.length()) && 
                   matches(s, soffset + 1, p, poffset));
        else
            return (soffset < s.length()) && 
                   ((p.charAt(poffset) == '?') || 
                   Character.toUpperCase(s.charAt(soffset)) == 
                        Character.toUpperCase(p.charAt(poffset))) && 
                   matches(s, soffset + 1, p, poffset + 1);
    }
    
/** overrides Object
 */
    public String toString()
    {
        return  "componentInit = " + componentInit +
                "\ncontextInit = " + contextInit +
                "\nrecursive =" + recursive +
                "\ndebugspec = " + debugspec +
                "\nencoding = " + encoding;
    }
}

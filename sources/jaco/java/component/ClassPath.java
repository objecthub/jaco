//      /   _ _      JaCo
//  \  //\ / / \     - the class (or source) path class
//   \//  \\_\_/     
//         \         Matthias Zenger, 03/02/00

package jaco.java.component;

import jaco.framework.*;
import java.io.*;


public class ClassPath
{
/** the string used as separator in class paths
 */
    protected static String pathSep = System.getProperty("path.separator");
    
/** the file separator character
 */
    protected static String fileSep = File.separator;

/** the boot class path (for JDK 1.2 compatibility)
 */
    protected static String bootPath = System.getProperty("sun.boot.class.path");
    
/** the extension path (for JDK 1.2+ compatibility)
 */
    protected static String extPath = System.getProperty("java.ext.dirs");

/** the value of the CLASSPATH environment variable
 */
    protected String    classPath;
    
/** the decomposed classpath
 */
    protected String[]  decomposed;


/** constructors
 */
    public ClassPath()
    {
        classPath = System.getProperty("java.class.path") + pathSep;
        addBootPath();
        parse();
    }
    
    public ClassPath(String classPath)
    {
        this.classPath = classPath;
        addBootPath();
        parse();
    }

/** prepend boot class path and extensions
 */
    protected void addBootPath() {
        if (!classPath.endsWith(pathSep))
            classPath += pathSep;
        if (extPath != null) {
            if(!extPath.endsWith(pathSep))
                extPath += pathSep;
            int l = extPath.length();
            int i = 0;
            int k;
            String extensionPath = "";
            while (i < l) {
                String dir = extPath.substring(i, k = extPath.indexOf(pathSep, i));
                String[] ext;
                if ((dir != null) &&
                    (dir.length() > 0) &&
                    ((ext = new File(dir).list()) != null)) {
                    if (!dir.endsWith(fileSep))
                        dir += fileSep;
                    for (int j = 0; j < ext.length; j++)
                        if (ext[j].endsWith(".jar"))
                            extensionPath += dir + ext[j] + pathSep;
                }
                i = k + 1;
            }
            classPath = extensionPath + classPath;
        }
        if (bootPath != null) {
            if (bootPath.endsWith(pathSep))
                classPath = bootPath + classPath;
            else
                classPath = bootPath + pathSep + classPath;
        }
    }

/** decompose path specification
 */
    protected void parse()
    {
        int i = 0;
        int n = 0;
        while (i < classPath.length())
        {
            i = classPath.indexOf(pathSep, i) + 1;
            n++;
        }
        decomposed = new String[n];
        i = 0;
        n = 0;
        while (i < classPath.length())
        {
            int end = classPath.indexOf(pathSep, i);
            decomposed[n++] = classPath.substring(i, end);
            i = end + 1;
        }
    }
    
/** return decomposed class path
 */
    public String[] decompose()
    {
        return decomposed;
    }
    
/** overrides Object
 */
    public String toString()
    {
        return classPath;
    }
}

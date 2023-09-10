//      /   _ _      JaCo
//  \  //\ / / \     - a file find strategy for the class path
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import java.io.*;


public class ClassPathFinder implements FindFileStrategy
{
/** the ClassPath
 */
    protected ClassPath classPath;

/** output file search?
 */
    public boolean      printSearch = false;
    
/** constructor
 */
    public ClassPathFinder(ClassPath classPath)
    {
        this.classPath = classPath;
    }

/** find file with given name in class path
 */
    public VirtualFile getFile(String name) throws FileNotFoundException
    {
        String[]    base = classPath.decompose();
        VirtualFile f;
        if (printSearch)
            System.out.println("looking for " + name);
        for (int i = 0; i < base.length; i++)
        {
            if (printSearch)
                System.out.println(" in " + base[i]);
            if ((f = VirtualFile.open(base[i], name)) != null)
                return f;
        }
        throw new FileNotFoundException("file '" + name + "' not found");
    }
}

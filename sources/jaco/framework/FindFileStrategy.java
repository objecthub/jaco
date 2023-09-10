//      /   _ _      JaCo
//  \  //\ / / \     - interface for finding files
//   \//  \\_\_/     
//         \         Matthias Zenger, 06/03/98

package jaco.framework;

import java.io.*;


public interface FindFileStrategy
{
    VirtualFile getFile(String filename) throws FileNotFoundException;
}

//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler component
//   \//  \\_\_/     
//         \         Matthias Zenger, 28/02/00

package jaco.sjava.component;


public class SJCompiler extends jaco.java.component.JavaCompiler
{
/** component name (here, this is the name of the compiler)
 */
    public String getName()
    {
        return "sjavac";
    }

/** version of the compiler
 */
    public String getVersion()
    {
        return "1.0, 15/05/03";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Matthias Zenger, David Cavin";
    }
}

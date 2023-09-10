//  /   _ _      JaCo
//  \  //\ / / \     - the main compiler component
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.component;


public class CJCompiler extends jaco.java.component.JavaCompiler
{
/** component name (here, this is the name of the compiler)
 */
    public String getName()
    {
        return "cjavac";
    }

/** version of the compiler
 */
    public String getVersion()
    {
        return "0.2, 29-Jan-2002";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Stephane Zermatten, Matthias Zenger [software@zenger.org]";
    }
}

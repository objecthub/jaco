//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler component
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.component;


public class OPCompiler extends jaco.java.component.JavaCompiler
{
/** component name (here, this is the name of the compiler)
 */
    public String getName()
    {
        return "OP";
    }

/** version of the compiler
 */
    public String getVersion()
    {
        return "0.0, 11/05/99";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Matthias Zenger";
    }
}

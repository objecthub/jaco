//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler component
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.component;


public class WrapperCompiler extends jaco.java.component.JavaCompiler
{
/** component name (here, this is the name of the compiler)
 */
    public String getName()
    {
        return "Wrapper";
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

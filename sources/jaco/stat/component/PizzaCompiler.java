//      /   _ _      JaCo
//  \  //\ / / \     - pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 07/03/00

package jaco.stat.component;

import jaco.java.component.*;


public class PizzaCompiler extends JavaCompiler
{
/** name of the compiler
 */
    public String getName()
    {
        return "statc";
    }
    
/** version of the compiler
 */
    public String getVersion()
    {
        return "1.00, 11/012/00";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Matthias Zenger";
    }
}

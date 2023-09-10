//      /   _ _      JaCo
//  \  //\ / / \     - pizza compiler
//   \//  \\_\_/     
//         \         Matthias Zenger, 07/03/00

package jaco.pizza.component;

import jaco.java.component.*;


public class PizzaCompiler extends JavaCompiler
{
/** name of the compiler
 */
    public String getName()
    {
        return "pico";
    }
    
/** version of the compiler
 */
    public String getVersion()
    {
        return "1.4, 29-Aug-2003";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Matthias Zenger [software@zenger.org]";
    }
}

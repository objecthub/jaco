//      /   _ _      JaCo
//  \  //\ / / \     - pizza modifiers support
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.java.component.*;


public interface PizzaModifierConst extends ModifierConst
{
/** class is an algebraic class
 */
    int ALGEBRAIC       = 0x00100000;
    
/** class is a case class, a case constructor or a case variable
 */
    int CASEDEF         = 0x00200000;
    
/** algebraic class is declared final; this modifier has to be
 *  independent of the FINAL modifier, since algebraic classes can
 *  never be final in their Java translation
 */
    int ALG_FINAL       = 0x00400000;

/** block represents a body of a case in a switch
 */
    int CASEBLOCK       = 0x00080000;
}

//                   Yacine Saidji, 22/05/00

package jaco.javaop.component;

import jaco.java.component.*;

public interface OPModifierConst extends ModifierConst {

  /** method is an operator */
  int OPERATOR          = 0x80000;
  
  /** if set, method is an left operand
   *  otherwise it is right operand
   */
  int LEFT_OPERAND  = 0x100000;
  
}

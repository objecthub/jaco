//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    modifier support
//                           
//  [XModifiers.java (767) 16-Apr-01 15:22 -> 25-May-01 00:09]

package jaco.keris.component;

import jaco.java.component.*;


public interface XModifierConst extends ModifierConst {
    
    /** class is an algebraic class
     */
    int ALGEBRAIC = 0x00100000;
    
    /** class is a case class, a case constructor or a case variable
     */
    int CASEDEF = 0x00200000;
    
    /** this class is a module
     */
    int MODULE = 0x02000000;
    
    /** algebraic class is declared final; this modifier has to be
     *  independent of the FINAL modifier, since algebraic classes can
     *  never be final in their Java translation
     */
    int ALG_FINAL = 0x00400000;

    /** block represents a body of a case in a switch
     */
    int CASEBLOCK = 0x00080000;
        
    /** this class is in fact a module
     */
    int FRAGMENT = 0x00040000;
    
    /** this class is in fact a class field
     */
    int CLASSFIELD = 0x00080000;
    
    /** this class definition is deferred
     */
    int OPAQUE = 0x04000000;
    
    /** this module specializes another module
     */
    int SPECIALIZES = 0x00800000;
}

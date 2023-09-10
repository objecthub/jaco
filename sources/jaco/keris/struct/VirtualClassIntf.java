//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    virtual classes relevant part of XClassDefs
//                           
//  [ModuleIntf.java (8802) 16-May-01 17:11 -> 8-Jun-01 01:38]

package jaco.keris.struct;

import java.util.*;
import jaco.java.struct.*;
import jaco.keris.component.*;
import jaco.framework.*;


public class VirtualClassIntf {
    
    /** the implementing class
     */
    public XClassDef withClass;
    
    /** do we have a local implementation of this class
     *  (a non-empty class block)?
     */
    public boolean thisImpl;
    
    /** the dependent virtual classes
     */
    public Type[] depends;
    
    /** all the virtual classes that get overridden
     */
    public XClassDef[] overrides;
    
}

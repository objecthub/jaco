//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    main compiler component
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris.component;

import jaco.java.component.*;


public class XJavaCompiler extends JavaCompiler {
    
/** name of the compiler
 */
    public String getName() {
        return "kerisc";
    }
    
/** version of the compiler
 */
    public String getVersion() {
        return "0.3, 23/07/02";
    }
    
/** author of the compiler
 */
    public String getAuthor() {
        return "(c) Matthias Zenger";
    }
}

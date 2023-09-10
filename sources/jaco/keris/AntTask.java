//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    ant task for the Keris compiler
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris;

import jaco.framework.ant.*;


public class AntTask extends AntCompilerTask  {
    public String compilerAdaptor() {
        return "jaco.keris.AntAdaptor";
    }
}

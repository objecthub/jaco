//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    main compiler component
//                           
//  [XJavaCompiler.java (775) 2-Apr-01 00:50 -> 23-Jun-01 00:08]

package jaco.keris;

import jaco.keris.context.*;
import jaco.framework.*;


public class Main {
    public static void main(String[] args) {
        XSettings js = new XSettings();
        try {
            js.parse(ProcessArgs.expand(args));
            if (!js.JavaContext().JavaCompiler().compile() && js.make)
                System.exit(-1);
        } catch (AbortCompilation e) {
            System.out.println(e);
            if (js.make)
                System.exit(-1);
        }
    }
}

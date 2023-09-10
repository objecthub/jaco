//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               compiler settings

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.context.*;
import jaco.java.context.*;


public class XSettings extends JavaSettings {
    
    public boolean keris = true;
    public boolean keepgenerated = false;
    
    
    public String resourceBase() {
        return "jaco.keris.resources";
    }

    protected void configure(String rsrc) {
        super.configure(rsrc);
        this.sourceversion = "1.4";
    }
    
    protected void parseSwitch(String option) throws AbortCompilation {
        if (option.equals("-java"))
        keris = false;
        else if (option.equals("-keepgenerated"))
        keepgenerated = true;
        else
            super.parseSwitch(option);
    }
    
    public String getUsage() {
        return "kerisc {<option>} {<file>}";
    }
    
    public String toString() {
        return super.toString() + "\n" +
               "keris = " + keris + "\n" +
               "keepgenerated = " + keepgenerated;
    }
    
    public JavaContext JavaContext() {
        if (keris)
            return new XContext(this);
        else
            return super.JavaContext();
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.Hashtable;


final class MethodNameId {
    Name owner;
    Name method;
    
    MethodNameId(Definition method) {
        this.owner = method.owner.fullname;
        this.method = method.name;
    }
    
    public int hashCode() {
        return owner.hashCode() + method.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof MethodNameId) {
            MethodNameId mid = (MethodNameId)obj;
            return (mid.owner == owner) && (mid.method == method);
        } else
            return false;
    }
    
    public String toString() {
        return "[" + method + "; " + owner + "]";
    }
}

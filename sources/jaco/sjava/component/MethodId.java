//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.Hashtable;


final class MethodId {
    Definition owner;
    Name method;
    
    MethodId(Definition method) {
        this.owner = method.owner;
        this.method = method.name;
    }
    
    static void enterInto(Definition def, Hashtable table) {
        Integer id = (Integer)table.get(def.owner);
	if (id == null)
	    table.put(def.owner, id = new Integer(0));
        MethodId mid = new MethodId(def);
        if (table.get(mid) == null) {
            id = new Integer(id.intValue() + 1);
            table.put(mid, id);
            table.put(def.owner, id);
        }
    }
    
    static int lookupAndRemove(Definition def, Hashtable table) {
        Integer id;
        MethodId mid = new MethodId(def);
        if ((id = (Integer)table.get(mid)) != null) {
            table.remove(mid);
            table.put(new MethodNameId(def), id);
            return id.intValue();
        } else {
            table.put(new MethodNameId(def), new Integer(32000));
            return -1;
        }
    }
    
    public int hashCode() {
        return owner.hashCode() + method.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof MethodId) {
            MethodId mid = (MethodId)obj;
            return (mid.owner == owner) && (mid.method == method);
        } else
            return false;
    }
    
    public String toString() {
        return "[" + method + "; " + owner + "]";
    }
}

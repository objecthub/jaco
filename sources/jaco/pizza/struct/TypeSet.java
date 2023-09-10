//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.struct;

import jaco.java.struct.*;
import jaco.java.component.Types;


public class TypeSet {

/** reference to the types component
 */
    Types types;

/** the set content
 */
    Type[] ts = new Type[4];
    int num = 0;
    
    
    public TypeSet(Types types) {
        this.types = types;
    }
    
    public TypeSet(Types types, Type[] ts) {
        this(types);
        for (int i = 0; i < ts.length; i++)
            add(ts[i]);
    }
    
    public boolean contains(Type type) {
        for (int i = 0; i < num; i++)
            if (types.subtype(ts[i], type))
                return true;
        return false;
    }
    
    public void add(Type type) {
        for (int i = 0; i < num; i++)
            if (types.subtype(ts[i], type))
                return;
            else if (types.subtype(type, ts[i])) {
                ts[i] = type;
                return;
            }
        if (num == ts.length) {
            Type[] res = new Type[num * 2];
            System.arraycopy(ts, 0, res, 0, num);
            ts = res;
        }
        ts[num++] = type;
    }
    
    public Type[] types() {
        Type[] res = new Type[num];
        System.arraycopy(ts, 0, res, 0, num);
        return res;
    }
    
    public String toString() {
        if (num == 0)
            return "{}";
        String res = "{" + ts[0];
        for (int i = 1; i < num; i++)
            res += ", " + ts[i];
        return res + "}";
    }
}

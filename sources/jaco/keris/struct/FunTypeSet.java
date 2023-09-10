//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    method sets
//                           
//  [FunTypeSet.java (1568) 24-May-01 17:04 -> 24-May-01 01:38]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.Types;


public class FunTypeSet extends TypeSet {

/** the set content
 */
    Name[] ns = new Name[4];
    
    
    public FunTypeSet(Types types) {
        super(types);
    }
    
    public boolean contains(Name name, Type type) {
        for (int i = 0; i < num; i++)
            if ((name == ns[i]) && types.subtype(ts[i], type))
                return true;
        return false;
    }
    
    public void add(Name name, Type type) {
        for (int i = 0; i < num; i++)
            if ((name == ns[i]) && types.subtype(ts[i], type))
                return;
            else if ((name == ns[i]) && types.subtype(type, ts[i])) {
                ts[i] = type;
                return;
            }
        if (num == ts.length) {
            Type[] res = new Type[num * 2];
            System.arraycopy(ts, 0, res, 0, num);
            ts = res;
            Name[] ms = new Name[num * 2];
            System.arraycopy(ns, 0, ms, 0, num);
            ns = ms;
        }
        ts[num] = type;
        ns[num++] = name;
    }
    
    public Name[] names() {
        Name[] res = new Name[num];
        System.arraycopy(ns, 0, res, 0, num);
        return res;
    }
    
    public String toString() {
        if (num == 0)
            return "{}";
        String res = "{" + ns[0] + ": " + ts[0];
        for (int i = 1; i < num; i++)
            res += ", " + ns[i] + ": " + ts[i];
        return res + "}";
    }
}

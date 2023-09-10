//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    bridge method sets
//                           
//  [BridgeMethodSet.java (1104) 12-Jul-01 02:49 -> 12-Jul-01 02:51]

package jaco.pizza.struct;

import jaco.java.struct.*;
import jaco.java.component.Types;


public class BridgeMethodSet extends TypeSet {
    
    public BridgeMethodSet(Types types, Type initial) {
        super(types);
        this.ts[num++] = initial;
    }
    
    public boolean contains(Type type) {
        Type restype = type.restype();
        for (int i = 0; i < num; i++)
            if (types.subtype(ts[i].restype(), restype))
                return true;
        return false;
    }
    
    public void add(Type type) {
        Type restype = type.restype();
        for (int i = 0; i < num; i++)
            if (types.subtype(ts[i].restype(), type.restype()))
                return;
            else if (types.subtype(restype, ts[i].restype())) {
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
}

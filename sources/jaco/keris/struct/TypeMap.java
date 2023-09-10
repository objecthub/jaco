//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type maps
//                           
//  [TypeSet.java (1382) 20-May-01 17:13 -> 24-May-01 01:37]

package jaco.keris.struct;

import jaco.java.struct.*;
import jaco.java.component.Types;


public class TypeMap {

    /** reference to the types component
     */
    Types types;

    /** the keys of this map
     */
    Type[] keys = new Type[4];
    
    /** the values of this map
     */
    public TypeSet[] values = new TypeSet[4];
    
    /** the number of entries
     */
    int num = 0;
    
    /** the constructor
     */
    public TypeMap(Types types) {
        this.types = types;
    }
    
    /** add a new key/value pair to this type map (assuming this
     *  operation is consistent)
     */
    public void add(Type key, TypeSet value) {
        if (num == keys.length) {
            Type[] nkeys = new Type[num * 2];
            TypeSet[] nvals = new TypeSet[num * 2];
            System.arraycopy(keys, 0, nkeys, 0, num);
            System.arraycopy(values, 0, nvals, 0, num);
            keys = nkeys;
            values = nvals;
        }
        keys[num] = key;
        values[num++] = value;
    }
    
    public TypeSet put(Type key, Type val) {
        TypeSet value = get(key);
        if (value == null)
            add(key, value = new TypeSet(types));
        value.add(val);
        return value;
    }
    
    public TypeSet putUnion(Type key, TypeSet set) {
        return putUnion(key, set.types());
    }
    
    public TypeSet putUnion(Type key, Type[] ts) {
        TypeSet value = get(key);
        if (value == null)
            add(key, value = new TypeSet(types));
        for (int i = 0; i < ts.length; i++)
            value.add(ts[i]);
        return value;
    }
    
    public TypeSet putIntersection(Type key, TypeSet set) {
        return putIntersection(key, set.types());
    }
    
    public TypeSet putIntersection(Type key, Type[] ts) {
        TypeSet value = null;
        int i = 0;
        for (; i < num; i++)
            if (types.sametype(keys[i], key)) {
                value = values[i];
                break;
            }
        if (value == null) {
            add(key, value = new TypeSet(types));
            for (int j = 0; j < ts.length; j++)
                value.add(ts[j]);
            return value;
        } else {
            values[i] = new TypeSet(types);
            for (int j = 0; j < ts.length; j++)
                if (value.contains(ts[j]))
                    values[i].add(ts[j]);
            return values[i];
        }
    }
    
    public TypeSet get(Type key) {
        for (int i = 0; i < num; i++)
            if (types.sametype(keys[i], key))
                return values[i];
        return null;
    }
    
    public Type[] keys() {
        Type[] res = new Type[num];
        System.arraycopy(keys, 0, res, 0, num);
        return res;
    }
    
    public String toString() {
        if (num == 0)
            return "{}";
        String res = "{" + keys[0] + "->" + values[0];
        for (int i = 1; i < num; i++)
            res += ", " + keys[i] + "->" + values[i];
        return res + "}";
    }
}

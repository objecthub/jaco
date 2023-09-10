//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    type sets
//                           
//  [TypeSet.java (1382) 20-May-01 17:13 -> 24-May-01 01:37]

package jaco.keris.struct;

import jaco.java.struct.*;
import jaco.java.component.Types;


/** a representation of type sets;
 *  invariant: type sets do not contain intersection types
 */
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
        switch (type) {
            case XType.CompoundType(Type[] fields):
                for (int i = 0; i < fields.length; i++)
                    if (!contains(fields[i]))
                        return false;
                return true;
            default:
                return containsNoCompound(type);
        }
    }
    
    protected boolean containsNoCompound(Type type) {
        for (int i = 0; i < num; i++)
            if (types.subtype(ts[i], type))
                return true;
        return false;
    }
        
    public void add(Type[] ts) {
        if (ts != null)
            for (int i = 0; i < ts.length; i++)
                add(ts[i]);
    }
    
    public void add(Type type) {
        switch (type) {
            case XType.CompoundType(Type[] fields):
                for (int i = 0; i < fields.length; i++)
                    add(fields[i]);
                break;
            default:
                addNoCompound(type);
        }
    }
    
    protected void addNoCompound(Type type) {
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
    
    public TypeSet intersect(TypeSet that) {
        if (that == null)
            return new TypeSet(types, types());
        TypeSet res = new TypeSet(types);
        for (int i = 0; i < num; i++)
            if (that.contains(ts[i]))
                res.addNoCompound(ts[i]);
        return res;
    }
    
    public TypeSet union(TypeSet that) {
        TypeSet res = new TypeSet(types, that.types());
        for (int i = 0; i < num; i++)
            res.addNoCompound(ts[i]);
        return res;
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

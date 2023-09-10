//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import Constant.*;
import Definition.*;


/** A (hashtable-) entry in the constant pool:
 */
public class ConstEntry
{
    public ConstEntry next;  // next entry with same hash code
    public Object obj;       // the object referred to be the entry
    public int index;        // the entry's index in the hashtable

    ConstEntry(ConstEntry next, Object obj, int index)
    {
        this.next = next;
        this.obj = obj;
        this.index = index;
    }
}

/** constant pool
 */
public class Pool
{
    static final int    HASH_SIZE = 0x800;
    static final int    HASH_MASK = 0x7FF;

/** index of next constant to be entered
 */
    public int          pp = 1;

/** the initial pool buffer
 */
    public Object[]     pool = new Object[64];

/** a hashtable containing all constants in the pool
 */
    public ConstEntry[] constants = new ConstEntry[HASH_SIZE];

/** a list containing all class references in the pool
 */
    public ConstEntry   classes = null;
    
    
/** remove everything from the pool
 */
    public void reset()
    {
        pp = 1;
        for (int i = 0; i < HASH_SIZE; i++)
            constants[i] = null;
        classes = null;
    }
    
/** double pool buffer in size
 */
    protected void extend()
    {
        Object[] newpool = new Object[pool.length * 2];
        System.arraycopy(pool, 0, newpool, 0, pool.length);
        pool = newpool;
    }

/** place an object in the pool, unless it is already there. Return
 *  the object's index in the pool
 */
    public int put(Object o)
    {
        int         hashcode = o.hashCode() & HASH_MASK;
        ConstEntry  e = constants[hashcode];
        
        while (e != null && !e.obj.equals(o))
            e = e.next;
        if (e == null)
        {
            e = new ConstEntry(constants[hashcode], o, pp);
            constants[hashcode] = e;
            if (pp == pool.length)
                extend();
            pool[pp++] = o;
            if ((o instanceof LongConst) || (o instanceof DoubleConst))
            {
                if (pp == pool.length)
                    extend();
                pool[pp++] = null;
            }
            else
            if (o instanceof ClassDef)
                classes = new ConstEntry(classes, o, 0);
        }
        return e.index;
    }

/** return the given object's index in the pool
 */
    public int get(Object o)
    {
        ConstEntry e = constants[o.hashCode() & HASH_MASK];
        while (e != null && !e.obj.equals(o))
            e = e.next;
        return e.index;
    }
}

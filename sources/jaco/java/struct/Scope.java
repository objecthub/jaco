//      /   _ _      JaCo
//  \  //\ / / \     - representation of nested scopes
//   \//  \\_\_/     
//         \         Matthias Zenger, 04/10/99

package jaco.java.struct;

import jaco.framework.*;


public final class Scope
{
    public Scope            next;           // the next outer scope
    public Definition       owner;          // owner of this scope
    public Definition[]     hashtable;      // the definition table
    public Definition       elems;          // definition list

/** size and mask of hash tables
 */
    static final int HASH_SIZE = 0x80;
    static final int HASH_MASK = 0x7f;

    public static Scope         emptyScope = new Scope(null, null, null);
    public static Scope         errScope = new Scope(null, null);
    public static Definition    sentinel = Definition.Proxy();
    public static Definition    errDef; // provided by a component
    
    static
    {
        sentinel.def = sentinel;
    }
    

/** construct a new scope, within scope next, with given owner, using
 *  given hashtable
 */
    public Scope(Scope next, Definition owner, Definition[] hashtable) {
        this.next = next;
        this.owner = owner;
        this.hashtable = hashtable;
        this.elems = null;
    }

/** construct a new scope, within scope next, with given owner, using a fresh
 *  hashtable
 */
    public Scope(Scope next, Definition owner) {
        this(next, owner, new Definition[HASH_SIZE]);
        if (next == null)
            for (int i = 0; i < HASH_SIZE; i++)
                hashtable[i] = sentinel;
        else
            for (int i = 0; i < HASH_SIZE; i++)
            	if (next.hashtable[i] == null)
            		hashtable[i] = sentinel;
            	else
               		hashtable[i] = next.hashtable[i];
    }

/** construct a fresh scope within this scope, with same owner,
 *  sharing hashtable with this scope. Used in connection with
 *  leave if scope access is stack-like in order to avoid allocation
 *  of fresh hashtables
 */
    public Scope dup()
    {
        return new Scope(this, this.owner, this.hashtable);
    }

/** remove all entries of this scope from hashtable.
 */
    public Scope leave()
    {
        while (elems != null)
        {
            int i = elems.name.index & HASH_MASK;
            hashtable[i] = hashtable[i].shadowed;
            elems = elems.sibling;
        }
        return next;
    }

/** enter definition def in this scope.
 */
    public void enter(Definition def)
    {
        int         i = def.name.index & HASH_MASK;
        Definition  e = (def.scope == null) ? def : def.proxy(def.name);
        e.scope = this;
        e.shadowed = hashtable[i];
        hashtable[i] = e;
        e.sibling = elems;
        elems = e;
        if (e.owner == null)
            e.owner = this.owner;
    }

/** enter definition def in this scope if not already there
 */
    public void enterIfAbsent(Definition def)
    {
        Definition  e = lookup(def.name);
        while ((e.scope == this) && (e.def.kind != def.def.kind))
            e = e.next();
        if (e.scope != this)
            enter(def);
    }

/** remove definition def
 */
    public void remove(Definition def)
    {
        if (def == sentinel)
            return;
        int         i = def.name.index & HASH_MASK;
        Definition  e = hashtable[i];
        if (e.def == def)
            hashtable[i] = e.shadowed;
        else {
            while ((e.shadowed != null) && (e.shadowed.def != def))
                e = e.shadowed;
            if (e.shadowed != null)
                e.shadowed = e.shadowed.shadowed;
        }
        e = elems;
        if (e.def == def)
            elems = e.sibling;
        else
        {
            while ((e.sibling != null) && (e.sibling.def != def))
                e = e.sibling;
            if (e.sibling != null)
                e.sibling = e.sibling.sibling;
        }
    }

/** place this scope directly within scope s
 */
    public void baseOn(Scope s)
    {
        for (int i = 0; i < HASH_SIZE; i++)
        {
        	Definition end = (s.hashtable[i] == null) ?
        		sentinel : s.hashtable[i];
            Definition e = hashtable[i];
            if (e.scope == null)
                hashtable[i] = end;
            else
            {
                while (e.shadowed.scope != null)
                    e = e.shadowed;
                e.shadowed = end;
            }
        }
        next = s;
    }

/** lookup given name, starting in this scope and proceeding outwards
 *  and return first matching object
 */
    public Definition lookup(Name name)
    {
    	if (name == null)
    		throw new Error();
        Definition e = hashtable[name.index & HASH_MASK];
        if (e == null) // error scope
            return errDef;
        while ((e.scope != null) && (e.name != name))
            e = e.shadowed;
        if ((e.scope == null) && (this == errScope))
            return errDef;
        else
            return e;
    }
    
    
/** print all scopes starting with scope s and proceeding outwards;
 *  used for debugging
 */
    public void print()
    {
        Scope   s = this;
        while (s != null)
        {
            if (s.owner != null)
                System.out.print(s.owner + ": ");
            for (Definition e = s.elems; e != null; e = e.sibling)
                System.out.print(e.def + "(" + e.name + "),\n");
            System.out.println();
            s = s.next;
        }
    }
    
    public void print2()
    {
        for (int i = 0; i < Scope.HASH_SIZE; i++)
            printList(i);
    }
    
    public void printList(int i)
    {
        Definition  e = hashtable[i];
        System.out.print(i + ":");
        while (e != null)
        {
        	try {
            	System.out.print(" " + e.def);
            } catch (Exception ex) {
            	System.out.print(" <null>");
            }
            e = e.shadowed;
        }
        System.out.println(";");
    }
    
    public int listLength(int i) {
        Definition e = hashtable[i];
        int len = 0;
        while (e != null) {
        	len++;
            e = e.shadowed;
        }
        return len;
    }
}

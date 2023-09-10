/** the runtime system for sjava
 *
 *  29.11.99, Matthias Zenger
 */

package sjava;
import java.io.*;


public final class ActiveObject extends Thread implements Serializable {

    private void writeObject(ObjectOutputStream out)  throws IOException {
    }
    private void readObject(ObjectInputStream stream)
	throws IOException, ClassNotFoundException {
    }
/** the owner
 */
    Active owner;

/** the value returned by the sync of the forall
 */
    public int forall$Value;

/** all pending selects within this active object
 */
    Select pending;

/** open call requests
 */
    Call[] callRequests;
    
    ActiveObject() {
    }
    
    public ActiveObject(Active owner, int nmethods) {
        this.owner = owner;
        pending = new Select();
        callRequests = new Call[nmethods];
        for (int i = 0; i < nmethods; i++)
            callRequests[i] = new Call(null, null, null, null);      
    }

/** the rendezvous tracker; 'obj' is the ActiveObject in which the select
 *  statement 'select' is called. If a select statement is placed inside
 *  a non-active object or in a static context, 'obj' will be null
 */
    synchronized static int rendezvousSucceeded(ActiveObject obj, Select select) {
	boolean fromBitSet = false;
        // is this a select inside of an active object?
        if (obj != null)
            // match accepted methods of 'select' with pending call requests
            // of 'obj'
            for (int j = 0; j < select.enabled; j++) {
		fromBitSet = (select.enabledMethods[j]&(1<<63)) != 0;
		int methodId = (int)((select.enabledMethods[j]&0x7fffffffl) >> 16);
		Call l = obj.callRequests[methodId].next;
                while (l != null) {
                    if (  ( (!fromBitSet) || 
			    (fromBitSet && (l.head.a$oCopy.owner == select.fromAccept[j])) ) &&  // 27.9.2001 CP
			  l.head.callRequest(methodId, obj, select) ) {
			obj.forall$Value = (int)((select.enabledMethods[j] >> 32) & 0x7fffffffl);
			//System.out.println("triggered by accept, accepting side "+obj.forall$Value);
			select.discard((int)(select.enabledMethods[j] & 0xffffl));
			return -1;
                    } else
                        l = l.next;
		}
            }
        // match called methods of 'select' with accepted methods of
        // their respective receivers
        for (int i = 0; i < select.called; i++) {
            int methodId = (int)((select.calledMethods[i] & 0x7fffffffl) >> 16);
            Select s = select.receivers[i].acceptRequest(methodId, (obj==null)?null:obj.owner);
            if (s != null) {
		if (obj!=null)
		    obj.forall$Value = (int)((select.calledMethods[i]>>32) & 0x7fffffffl);
		//System.out.println("triggered by call, caller side "+obj.forall$Value);
                return select.discard((int)(select.calledMethods[i] & 0xffffl), s).result;
	    }
            else
		select.receivers[i].callRequests[methodId].append(select);
	}
        if (obj == null) {
            select.prev = new Select();
            select.next = null;
        } else {
            select.prev = obj.pending; // insert select between the 1st (=dummy) and the 2nd element
            select.next = obj.pending.next;
            obj.pending.next = select;
            if (select.next != null)
                select.next.prev = select;
        }
        return -1;
    }

    private Select acceptRequest(int methodId, Active caller) {
        Select s = pending;
        while (s != null) {
            if (s.acceptRequest(methodId, caller))
                return s;
            s = s.next;
        }
        return null;
    }
}















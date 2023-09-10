/** the runtime system for sjava:  Select.java
 *
 *  29.11.99, Matthias Zenger
 *  18.7.02, C. Petitpierre
 */

package sjava;


public final class Select {

    public static final ActiveObject IGNORE_AO = new ActiveObject();
    public static final int IGNORE_WAITUNTIL = Integer.MIN_VALUE;
    public static final Active IGNORE_ACT = new Active(){ public void run() {} };

/** select description
 */
    ActiveObject a$oCopy;
    int enabled;
    long[] enabledMethods;  // from (1bit), forallVar (31 bits), methodId (16 bits), caseNb (16 bits)
    Active[] fromAccept;
    int called = 0;
    long[] calledMethods;  // from (1bit), forallVar (31 bits), methodId (16 bits), caseNb (16 bits)
    ActiveObject[] receivers;
    int nbTimeouts = 0;
    long [] timeoutIds;
    int defaultId = -1;
    int location;
    boolean waitFlag = true;
    Select nextS = null; // debugger
    
/** the result of the select
 */
    volatile int result = -1;
    volatile Select delayedSelect = null;
    
/** all call requests made by this select
 */
    Call trace;

/** the pending-list concatenation of an ActiveObject
 */
    Select next;
    Select prev;
    
/** the free list
 */
    static Select free;

    Select(int n) {
        enabledMethods = new long[n];
	fromAccept = new Active[n];
        calledMethods = new long[n];
        receivers = new ActiveObject[n];
	timeoutIds = new long[n];
    }
    
    Select() {
    }
    
/** the synchronisation method for the system thread (with a label)
 */
    public int syncNoWait(ActiveObject obj, int label) {
	syncNoWait(obj);
	return -1;
    }

/** the synchronisation method for the system thread
 */
    public synchronized int syncNoWait(ActiveObject obj) {
	a$oCopy = obj;
        if ((called == 0) && (enabled == 0) && (nbTimeouts == 0))
            return -1;
        for (int i = 0; i < called; i++) {
            int methodId = (int)((calledMethods[i] & 0x7fffffffl) >> 16);
	    receivers[i].callRequests[methodId].append(this);
	}
	if (obj == null) { // inside a passive object
	    prev = new Select();
	    next = null;
	} else {
	    prev = obj.pending;
	    next = obj.pending.next;
	    obj.pending.next = this;
	    if (next != null)
		next.prev = this;
	}
	Kernel.kernel.enqueue(this);
	return -1;
    }


/** the main synchronisation method (with a label)
 */
    public int syncWait(ActiveObject obj, int label) {
	location = label;
	return syncWait(obj);
    }

    
/** the main synchronisation method
 */
    public synchronized int syncWait(ActiveObject obj) {
	a$oCopy = obj;
	
        if ((called == 0) && (enabled == 0) && (nbTimeouts == 0))
            return -1;
        result = -1;

        for (int i = 0; i < called; i++) {
            int methodId = (int)((calledMethods[i] & 0x7fffffffl) >> 16);
	    receivers[i].callRequests[methodId].append(this);
	}
	if (obj == null) { // inside a passive object
	    prev = new Select();
	    next = null;
	} else {
	    prev = obj.pending;
	    next = obj.pending.next;
	    obj.pending.next = this;
	    if (next != null)
		next.prev = this;
	}
	// this is now turned into absolute timeouts
	Kernel.kernel.enqueue(this);
	try {
	    if (waitFlag) {
		wait();  // no rendezvous ready, wait
		waitFlag = true;
	    }
	    if (delayedSelect != this) // calls itself through a variable assigned to this
		return result();
	} catch (InterruptedException e) {	}
	return result();
    }

    public synchronized void notif () {
	waitFlag = false;
	notify();
    }
    public synchronized void triggerAccept() {
        synchronized (delayedSelect) {
            delayedSelect.delayedSelect = null;
	    delayedSelect.waitFlag = false;
            delayedSelect.notify();
            delayedSelect = null;
        }
        synchronized (Select.class) {
            next = free;
            free = this;
        }
    }
    
    public boolean triggerAccept(boolean x) {
        triggerAccept();
        return x;
    }
    
    public byte triggerAccept(byte x) {
        triggerAccept();
        return x;
    }
    
    public short triggerAccept(short x) {
        triggerAccept();
        return x;
    }
    
    public char triggerAccept(char x) {
        triggerAccept();
        return x;
    }
    
    public int triggerAccept(int x) {
        triggerAccept();
        return x;
    }
    
    public long triggerAccept(long x) {
        triggerAccept();
        return x;
    }
    
    public float triggerAccept(float x) {
        triggerAccept();
        return x;
    }
    
    public double triggerAccept(double x) {
        triggerAccept();
        return x;
    }
    
    public Object triggerAccept(Object x) {
        triggerAccept();
        return x;
    }
    
/** initialisation methods
 */
    public synchronized static Select make() {
        if (free != null) {
            Select res = free;
            free = free.next;
            res.next = null;
            res.result = -1;
            res.delayedSelect = null;
            return res;
        } else {
            Select res = new Select(8);
            return res;
        }
    }
    
    public synchronized int result() {
	return (result);
    }
    
    public Select call(int caseId, ActiveObject obj, int methodId, int forallValue) {
	//int forallValue=0; // remove when the parameter is available
        if (obj != IGNORE_AO) {
            if (called == calledMethods.length) {
                calledMethods = extend(calledMethods);
                ActiveObject[] res = new ActiveObject[called * 2];
                System.arraycopy(receivers, 0, res, 0, called);
                receivers = res;
            }
            calledMethods[called] = (((long)methodId) << 16) | caseId | ((long)forallValue<<32);
            receivers[called++] = obj;
        }
        return this;
    }
    
    // accept xxx;
    public Select accept(int caseId, int methodId, boolean guard) {
        if (guard) {
            if (enabled == enabledMethods.length)
                enabledMethods = extend(enabledMethods);
            enabledMethods[enabled++] = (((long)methodId << 16)) | caseId;
        }
        return this;
    }

    // accept xxx from yyy;
    public Select accept(int caseId, int methodId, boolean guard, Active fromCaller, int forallVal) {
        if (guard) {
            if (enabled == enabledMethods.length)
                enabledMethods = extend(enabledMethods);
            enabledMethods[enabled] = (((long)methodId << 16)) | caseId  | ((long)forallVal<<32)| (long)1<<63;
	    fromAccept[enabled] = fromCaller; // 9.7.2001 CP/ 27.7.2001 CP
	    enabled++;
        }
        return this;
    }
    
    public Select timeout(int caseId, long timer) {
        if (timer != IGNORE_WAITUNTIL) {
            if (nbTimeouts == timeoutIds.length) {
                timeoutIds = extend(timeoutIds);
            }
            timeoutIds[nbTimeouts++] = caseId;
	}
        return this;
    }
    
    public Select defaultCase(int caseId, boolean guard) {
        if (guard)
            defaultId = caseId;
        return this;
    }
    
/** synchronization methods
 *  Is there a local methodId ready to be called ?
 */
    synchronized boolean acceptRequest(int methodId, Active caller, int[] index) {
        for (int i = 0; i < enabled; i++) {
            if ( (((enabledMethods[i] & 0x7fffffffl) >> 16) == methodId) && 
		 (  ((enabledMethods[i] & (1<<63)) != 0) ? (caller==fromAccept[i]) : true) ){
//  		a$oCopy.forall$Value = (int)((enabledMethods[i] >> 32) & 0x7fffffffl);
		//System.out.println("triggered by call, accepting side "+a$oCopy.forall$Value);
		index[0] = i;  // return i's value
                return true;
            }
	}
        return false;
    }
    

/** memory management
 */
    private long[] extend(long[] ls) {
        long[] res = new long[ls.length * 2];
        System.arraycopy(ls, 0, res, 0, ls.length);
        return res;
    }

    private Active[] extend(Active[] ls) {
        Active[] res = new Active[ls.length * 2];
        System.arraycopy(ls, 0, res, 0, ls.length);
        return res;
    }
    
    synchronized Select discard(int r, Select delayedSelect) {
        discard(r);
        this.delayedSelect = delayedSelect;
        if (delayedSelect != null)
            delayedSelect.delayedSelect = delayedSelect;
        return this;
    }
    
    synchronized Select discard(int r) {
        for (int i = 0; i < called; i++)
            receivers[i] = null;
        while (trace != null) {
            trace.prev.next = trace.next;
            if (trace.next != null)
                trace.next.prev = trace.prev;
            trace = trace.tail;
        }
        enabled = 0;
        called = 0;
        nbTimeouts = 0;
        defaultId = -1;
        prev = null;
        next = null;
        result = r;
        return this;
    }




}

















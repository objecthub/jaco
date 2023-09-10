/** the runtime system for sjava
 *
 *  29.11.99, Matthias Zenger
 */

package sjava;


class Call {
    Select head;
    Call prev;
    Call next;
    Call tail;
    
    Call(Select head, Call prev, Call next, Call tail) {
        this.head = head;
        this.prev = prev;
        this.next = next;
        this.tail = tail;
    }
    
    void append(Select head) {
        head.trace = new Call(head, this, next, head.trace);
        if (next != null)
            next.prev = head.trace;
        next = head.trace;
    }
}

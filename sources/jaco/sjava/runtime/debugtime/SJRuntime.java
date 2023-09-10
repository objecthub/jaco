/** the runtime system for sjava
 *
 *  11.04.00, Matthias Zenger
 */

package sjava;

import java.lang.reflect.*;


public class SJRuntime {
    public static int version = 2;
    
    public static ActiveObject getActiveObject(Object activeObject) {
        if (activeObject == null)
            return null;
        Class c = activeObject.getClass();
        try {
            Field f = c.getField("a$o");
            return (ActiveObject)f.get(activeObject);
        } catch (Exception e) {
	    System.out.println(e.toString());
            return null;
	}
    }
    
    public static void setActiveObject(Object activeObject, ActiveObject aco) {
        Class c = activeObject.getClass();
        try {
            Field f = c.getField("a$o");
            f.set(activeObject, aco);
        } catch (Exception iaE) {
	    System.out.println(iaE.toString());
	}
    }
    
    public static void printPendingSelects(Object activeObject) {
        ActiveObject ao = getActiveObject(activeObject);
        if (ao != null) {
            Select s = ao.pending;
            if (s == null)
                System.out.println("no pending selects");
            while (s != null) {
                System.out.println(s);
                s = s.next;
            }
        } else
            System.out.println("no active object");
    }
    
    public static void main(String[] args) {
        System.out.println("sjava runtime version " + version);
    }
}

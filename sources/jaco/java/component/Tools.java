//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import java.io.*;

/** Some useful values and utility routines that don't fit anywhere else.
 */
public class Tools
{
/** return the last part of a class name.
 */
    public static Name shortName(Name classname) {
        return classname.subName(classname.lastPos((byte)'.') + 1,
                                 classname.length());
    }

/** assertions for internal error checking:
 */
    public static void assert(boolean c) {
        if (!c)
            throw new InternalError("assertion failed");
    }

    public static void assert(boolean c, Object info) {
        if (!c)
            throw new InternalError("assertion failed: " + info);
    }

/** convert an array of objects to a string:
 */
    public static String toString(Object[] a) {
        if (a == null || a.length == 0)
            return "";
        String s = a[0].toString();
        for (int i = 1; i < a.length; i++) {
            s = s + ", " + a[i].toString();
        }
        return s;
    }

/** sort (int) arrays of keys and values
 */
    public static void qsort2(int[] keys, int[] values, int lo, int hi) {
        int i = lo;
        int j = hi;
        int pivot = keys[(i+j)/2];
        do {
            while (keys[i] < pivot) i++;
            while (pivot < keys[j]) j--;
            if (i <= j) {
                int temp1 = keys[i];
                keys[i] = keys[j];
                keys[j] = temp1;
                int temp2 = values[i];
                values[i] = values[j];
                values[j] = temp2;
                i++;
                j--;
            }
        } while (i <= j);
        if (lo < j) qsort2(keys, values, lo, j);
        if (i < hi) qsort2(keys, values, i, hi);
    }
}

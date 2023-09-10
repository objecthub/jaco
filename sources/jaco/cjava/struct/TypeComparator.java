package jaco.cjava.struct;
import java.util.*;
/**
 * Simple type comparator based on string representation.
 *
 * It converts types into strings and compares that.
 *
 * It is used backwards ordering of types.
 */
public final class TypeComparator implements Comparator
{
    /**
     * Compare two strings
     */
    public int compare(Object o1, Object o2)
    {
    return o2.toString().compareTo(o1.toString());
    }
    /**
     * Check equality with another object.
     */
    public boolean equals(Object o)
    {
    /* all TypeComparator act the same */
    return o instanceof TypeComparator;
    }
}

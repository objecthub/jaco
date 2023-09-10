//      /   _ _     JaCo
//  \  //\ / / \    - symbol table
//   \//  \\_\_/    
//         \        


package jaco.framework;


/** names represent unique hashable strings. Two names are equal
 *  if their indices are equal. Ascii representation is used
 *  for storing names internally.
 */
public final class Name
{
/** address in the name memory
 */
    public int                  index;

/** length of the name
 */
    private int                 len;

/** next name in the same hash bucket
 */
    private Name                next;

    private final static int    HASH_SIZE = 0x8000;
    private final static int    HASH_MASK = 0x7FFF;
    private final static int    NAME_SIZE = 0x20000;
    
/** memory to store all names sequentially
 */
    static public byte[]        names = new byte[NAME_SIZE];
    private static int          nc = 0;

/** hashtable for finding names quickly
 */
    private static Name[]       hashtable = new Name[HASH_SIZE];


/** the hashcode of a name
 */
    private static int hashValue(byte cs[], int offset, int len)
    {
        if (len > 0)
            return
                len * (41 * 41 * 41) +
                cs[offset] * (41 * 41) +
                cs[offset + len - 1] * 41 +
                cs[offset + (len >> 1)];
        else
            return 0;
    }

/** is (the ascii representation of) name equal to
 *  cs[offset..offset+len-1]?
 */
    private static boolean equals(int index,
                                  byte cs[], int offset, int len)
    {
        int i = 0;
        while ((i < len) && (names[index + i] == cs[offset + i]))
            i++;
        return i == len;
    }

/** create a name from the bytes in cs[offset..offset+len-1].
 *  assume that bytes are in ascii format.
 */
    public static Name fromAscii(byte cs[], int offset, int len)
    {
        int     h = hashValue(cs, offset, len) & HASH_MASK;
        Name    n = hashtable[h];
        while ((n != null) &&
               ((n.len != len) || !equals(n.index, cs, offset, len)))
            n = n.next;
        if (n == null)
        {
            n = new Name();
            n.index = nc;
            n.len = len;
            n.next = hashtable[h];
            hashtable[h] = n;
            for (int i = 0; i < len; i++)
            {
                if (nc == names.length)
                {
                    byte[] newnames = new byte[names.length * 2];
                    System.arraycopy(names, 0, newnames, 0, names.length);
                    names = newnames;
                }
                names[nc++] = cs[offset + i];
            }
            if (len == 0)
                nc++;
        }
        return n;
    }

/** create a name from the bytes in cs[offset..offset+len-1];
 *  assume that characters are in source format
 */
    public static Name fromSource(byte cs[], int offset, int len)
    {
        byte[]  ascii = new byte[len * 2];
        int alen = SourceRepr.source2ascii(cs, offset, len, ascii);
        return fromAscii(ascii, 0, alen);
    }

/**
 * Create a name from the characters in string s
 */
    public static Name fromString(String s)
    {
        byte[]  source = SourceRepr.string2source(s);
        return fromSource(source, 0, source.length);
    }

/**
 * Copy bytes of this name to buffer cs, starting at offset
 */
    public void copyAscii(byte cs[], int offset)
    {
        System.arraycopy(names, index, cs, offset, len);
    }

/**
 * Return the ascii representation of this name
 */
    public byte[] toAscii() 
    {
        byte[]  ascii = new byte[len];
        System.arraycopy(names, index, ascii, 0, len);
        return ascii;
    }

/** return the source representation of this name
 */
    public byte[] toSource()
    {
        return SourceRepr.string2source(toString());
    }

/** return the string representation of this name
 */
    public String toString()
    {
        return SourceRepr.ascii2string(names, index, len);
    }

/** return the string hash value of this name
 */
    public int hashCode()
    {
        return index;
    }

/** is this name equal to other?
 */
    public boolean equals(Object other)
    {
        if (other instanceof Name)
            return index == ((Name)other).index;
        else
            return false;
    }

/** returns the length of this name
 */
    public int length()
    {
        return len;
    }

/** returns i'th byte of this name
 */
    public byte sub(int i)
    {
        return names[index + i];
    }

/** returns first occurrence of byte b in this name, len if not found
 */
    public int pos(byte b)
    {
        int i = 0;
        while (i < len && names[index + i] != b)
            i++;
        return i;
    }

/** returns last occurrence of byte b in this name, -1 if not found.
 */
    public int lastPos(byte b)
    {
        int i = len - 1;
        while (i >= 0 && names[index + i] != b)
            i--;
        return i;
    }

/** does this name start with prefix?
 */
    public boolean startsWith(Name prefix)
    {
        int i = 0;
        while ((i < prefix.len) &&
                (i < len) &&
                (names[index + i] == names[prefix.index + i]))
            i++;
        return i == prefix.len;
    }

/** does this name end with suffix?
 */
    public boolean endsWith(Name suffix)
    {
        int i = len - 1;
        int j = suffix.len - 1;
        while ((j >= 0) && (i >= 0) &&
                (names[index + i] == names[suffix.index + j]))
        {
                i--;
                j--;
        }
        return j < 0;
    }

/** returns the subName starting at position start, excluding position end
 */
    public Name subName(int start, int end)
    {
        if (end < start)
            end = start;
        byte[]  ascii = new byte[end - start];
        System.arraycopy(names, index + start, ascii, 0, end - start);
        return fromAscii(ascii, 0, ascii.length);
    }

/** replace all `from' characters with `to'
 */
    public Name replace(byte from, byte to)
    {
        byte[] ascii = new byte[len];
        copyAscii(ascii, 0);
        for (int i = 0; i < len; i++)
            if (ascii[i] == from) ascii[i] = to;
        return fromAscii(ascii, 0, len);
    }

/** returns the concatenation of this name and n
 */
    public Name append(Name n)
    {
        byte[] ascii = new byte[len + n.len];
        copyAscii(ascii, 0);
        n.copyAscii(ascii, len);
        return fromAscii(ascii, 0, ascii.length);
    }

/** returns the concatenation of all names in ns
 */
    public static Name concat(Name ns[])
    {
        int len = 0;
        for (int i = 0; i < ns.length; i++)
            len = len + ns[i].len;
        byte[] ascii = new byte[len];
        len = 0;
        for (int i = 0; i < ns.length; i++)
        {
            ns[i].copyAscii(ascii, len);
            len = len + ns[i].len;
        }
        return fromAscii(ascii, 0, len);
    }
}

//      /   _ _     JaCo
//  \  //\ / / \    - bitset representation
//   \//  \\_\_/    
//         \        

package jaco.framework;


public final class Bits
{
    final static int WORDLEN = 32;
    final static int WORDSHIFT = 5;
    final static int WORDMASK = WORDLEN - 1;

    private int[] bits;
    
    
    public Bits()
    {
        this(new int[1]);
    }

    public Bits(int[] bits)
    {
        this.bits = bits;
    }

    public Bits(int lo, int hi)
    {
        this();
        for (int i = lo; i < hi; i++)
            incl(i);
    }

    private void sizeTo(int len)
    {
        if (bits.length < len)
        {
            int[] newbits = new int[len];
            System.arraycopy(bits, 0, newbits, 0, bits.length);
            bits = newbits;
        }
    }

/** this set = {}
 */
    public void clear()
    {
        for (int i = 0; i < bits.length; i++)
            bits[i] = 0;
    }

/** is bit set empty?
 */
    public boolean empty()
    {
        for (int i = 0; i < bits.length; i++)
            if (bits[i] != 0)
                return false;
        return true;
    }

/** return a copy of this set.
 */
    public Bits dup()
    {
        int[] newbits = new int[bits.length];
        System.arraycopy(bits, 0, newbits, 0, bits.length);
        return new Bits(newbits);
    }

/** include x in this set
 */
    public void incl(int x)
    {
        sizeTo((x >>> WORDSHIFT) + 1);
        bits[x >>> WORDSHIFT] = bits[x >>> WORDSHIFT] | (1 << (x & WORDMASK));
    }

/** exclude x from this set. do nothing if x < 0.
 */
    public void excl(int x)
    {
        sizeTo((x >>> WORDSHIFT) + 1);
        bits[x >>> WORDSHIFT] = bits[x >>> WORDSHIFT] & ~(1 << (x & WORDMASK));
    }

/** is x an element of this set?
 */
    public boolean member(int x)
    {
        return (0 <= x) && (x < (bits.length << WORDSHIFT)) &&
                ((bits[x >>> WORDSHIFT] & (1 << (x & WORDMASK))) != 0);
    }

/** this set = xs
 */
    public void assign(Bits xs)
    {
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++)
            bits[i] = xs.bits[i];
    }

/** this set = this set & xs
 */
    public void andSet(Bits xs)
    {
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++)
            bits[i] = bits[i] & xs.bits[i];
    }

/** this set = this set | xs
 */
    public void orSet(Bits xs)
    {
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++)
            bits[i] = bits[i] | xs.bits[i];
    }

/** overrides Object
 */
    public String toString()
    {
        char[] digits = new char[bits.length * WORDLEN];
        for (int i = 0; i < bits.length * WORDLEN; i++)
            digits[i] = member(i) ? 'L' : 'O';
        return new String(digits);
    }
}

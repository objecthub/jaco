//      /   _ _      JaCo
//  \  //\ / / \     - encoding of source file positions
//   \//  \\_\_/     
//         \         Matthias Zenger, 20/12/97

package jaco.framework;


public class Position
{
/** source file positions are integers in the format:
 *     line-number << LINESHIFT + column-number
 *  NOPOS represents an undefined position.
 */
    public static final int LINESHIFT    = 10;
    public static final int COLUMNMASK   = 1023;

/** undefined position
 */
    public static final int NOPOS        = 0;

/** first position in a source file
 */
    public static final int FIRSTPOS     = (1 << LINESHIFT) + 1;

/** encode a line and column number into a single int
 */
    public static int encode(int line, int col)
    {
        return (line << Position.LINESHIFT) + col;
    }

/** get the line number out of an encoded position
 */
    public static int line(int pos)
    {
        return pos >>> LINESHIFT;
    }

/** return the column number of an encoded position
 */
    public static int column(int pos)
    {
        return pos & COLUMNMASK;
    }
}

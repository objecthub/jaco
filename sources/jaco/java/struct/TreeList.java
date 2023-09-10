//      /   _ _      JaCo
//  \  //\ / / \     - list of abstract syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;


public class TreeList
{
    protected int       length;
    protected Cons      first;
    protected Cons      last;
    
/** constructor for empty tree list
 */
    public TreeList()
    {}

/** constructor for tree list with one initial syntax tree
 */ 
    public TreeList(Tree t)
    {
        if (t != null)
        {
            length = 1;
            first = last = new Cons(t, null);
        }
    }

/** build tree list for tree array
 */
    public TreeList(Tree[] ts)
    {
        if (ts != null)
        {
            length = ts.length;
            if (length > 0)
            {
                first = last = new Cons(ts[0], null);
                for (int i = 1; i < length; i++)
                    last = (last.tail = new Cons(ts[i]));
            }
        }
    }

/** the length of the list
 */
    public int length()
    {
        return length;
    }

/** get the linked chains
 */
    public Cons chain()
    {
        return first;
    }

/** prepend a tree to the list
 */
    public TreeList prepend(Tree t)
    {
        if (t != null)
        {
            first = new Cons(t, first);
            if (last == null)
                last = first;
            length++;
        }
        return this;
    }

/** append a tree to the list
 */
    public TreeList append(Tree t)
    {
        if (t != null)
        {
            if (length++ > 0)
            {
                last.tail = new Cons(t);
                last = last.tail;
            }
            else
                first = last = new Cons(t);
        }
        return this;
    }

/** append a tree list
 */
    public TreeList append(TreeList l)
    {
        if ((l != null) && (l.length > 0))
        {
            if (length > 0)
            {
                last.tail = l.first;
                last = l.last;
            }
            else
            {
                first = l.first;
                last = l.last;
            }
            length += l.length;
        }
        return this;
    }

/** append a tree array
 */
    public TreeList append(Tree[] ts)
    {
        if ((ts == null) || (ts.length == 0))
            return this;
        if (length == 0)
            first = (last = new Cons(ts[0], null));
        else
            last = (last.tail = new Cons(ts[0]));
        for (int i = 1; i < ts.length; i++)
            last = (last.tail = new Cons(ts[i]));
        length += ts.length;
        return this;
    }

/** copy tree list
 */
    public TreeList dup()
    {
        TreeList    list = new TreeList();
        if ((list.length = length) > 0)
        {
            Cons    cons = first.tail;
            list.first = list.last = new Cons(first.head);
            for (int i = 1; i < length; i++, cons = cons.tail)
                list.last = (list.last.tail = new Cons(cons.head));
        }
        return list;
    }

/** convert to a new tree array
 */
    public Tree[] toArray()
    {
        Tree[]  ts = new Tree[length];
        int     i = 0;
        for (Cons l = first; l != null; l = l.tail)
            ts[i++] = l.head;
        return ts;
    }

/** store tree list in an existing tree array
 */
    public Tree[] toArray(Tree[] ts)
    {
        int i = 0;
        for (Cons l = first; l != null; l = l.tail)
            ts[i++] = l.head;
        return ts;
    }

/** apply a primitive processor to all trees in the list
 */
    public TreeList process(TreeProcessor processor) throws AbortCompilation
    {
        TreeList    tl = processor.enter(this);
        Cons        tree = tl.first;
        while (tree != null)
        {
            if (tree.head != null)
                tree.head = processor.process(tree.head);
            tree = tree.tail;
        }
        return processor.exit(tl);
    }

/** apply a composite processor to the tree list
 */
    public TreeList process(TreeListProcessor processor) throws AbortCompilation
    {
        return processor.exit(processor.process(processor.enter(this)));
    }
    

    public static class Cons
    {
        public Tree     head;
        Cons            tail;
        
        
        Cons(Tree head)
        {
            this.head = head;
        }
        
        Cons(Tree head, Cons tail)
        {
            this.head = head;
            this.tail = tail;
        }
        
    /** get next element
     */
        public Cons next()
        {
            return tail;
        }
    }
}

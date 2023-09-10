//      /   _ _      JaCo
//  \  //\ / / \     - graphs constructed with Nodes are used for the pattern
//   \//  \\_\_/       matchers exhaustive check
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;


/** a Node is a vertice in a graph, used by the pattern matcher to
 *  test, if the pattern matching is exhaustive. If the graph cannot
 *  be reduced to a single node, then the exhaustive check fails.
 *  
 *    succ       are the immediate successor nodes
 *    dependant  is the number of successors, a node depends on,
 *               for beeing reduced. If n = dependant with
 *                 n = 0  =>  node is complete (fully reduced)    ( )
 *                 n > 0  =>  node depends on the next n nodes    (*)
 *                            (the successors of succ[0];
 *                            succ.length = 1 in this case!)
 *                 n < 0  =>  node depends on succ[0..m], where   (+)
 *                            m = succ.length - 1
 */

public class Node
{
/** the number of nodes, on with this node depends
 */
    protected int       dependant;

/** the successors of this node
 */
    protected Node[]    succ;
    

/** constructor for complete node
 */
    public Node()
    {
        this.succ = new Node[1];
    }

/** constructor for OR (+) dependancy
 */
    public Node(Node[] succ)
    {
        this.dependant = -succ.length;
        this.succ = succ;
    }

/** constructor for AND (*) dependancy
 */
    public Node(int dependant)
    {
        this.dependant = dependant;
        this.succ = new Node[1];
    }

/** reduce method
 */
    public Node reduce()
    {
        if (dependant == 0)
        {
            // inherit incomplete cases
            if ((succ[0] != null) && !succ[0].reduce().complete())
                dependant = -1;
        }
        else
        {
            // reduce all successor nodes
            for (int i = 0; i < succ.length; i++)
                if ((succ[i] == null) || !succ[i].reduce().complete())
                    return this;
            // check all successors this node depends
            while (dependant > 0)
                if (!succ[0].complete())
                    return this;
                else
                {
                    // reduced nodes
                    succ[0] = succ[0].succ[0];
                    dependant--;
                }
            // this node is complete now
            if (dependant < 0)
                succ = new Node[]{succ[0].succ[0]};
            dependant = 0;
        }
        return this;
    }

/** is this node complete (fully reduced)?
 */
    public boolean complete()
    {
        return (dependant == 0);
    }

/** return successor
 */
    public Node next()
    {
        return succ[0];
    }

/** duplicate node
 */
    public Node dup()
    {
        Node    copy = new Node();
        copy.dependant = dependant;
        copy.succ = new Node[succ.length];
        for (int i = 0; i < succ.length; i++)
            if (succ[i] != null)
                copy.succ[i] = succ[i].dup();
        return copy;
    }

/** insert 'tail' as an immediate successor, if there is still none
 */
    public Node append(Node tail)
    {
        if (succ[0] == null)
            return (succ[0] = tail);
        else
            return succ[0];
    }

/** insert a new (*) node at successor position 'tag', if this slot
 *  is still empty
 */
    public Node insert(int tag, int args)
    {
        if (succ[tag] != null)
            return succ[tag];
        else
            return succ[tag] = new Node(args);
    }

/** insert 'i' complete nodes as successors of this node
 */
    public Node appendComplete(int i)
    {
        Node    node = this;
        while ((i--) > 0)
            node = (node.succ[0] = new Node());
        return node;
    }

/** insert a copy of 'node' as an immdeiate successor, if there is still
 *  none
 */
    public Node appendCopy(Node node)
    {
        if (node != null)
            return (succ[0] = node.dup());
        else
            return (succ[0] = null);
    }

/** if this node is complete, expand it to a (+) node with args.length
 *  alternatives and args[i] complete successors for case i
 */
    public Node expand(int[] args)
    {
        if (dependant == 0)
        {
            Node    old = succ[0];
            dependant = -args.length;
            succ = new Node[args.length];
            for (int i = 0; i < args.length; i++)
                (succ[i] = new Node(args[i])).
                            appendComplete(args[i]).
                            appendCopy(old);
        }
        return this;
    }

/** if there is still no successor, make a (+) node with args.length
 *  empty alternatives; otherwise expand successor
 */
    public Node expandNext(int[] args)
    {
        if (succ[0] == null)
            return (succ[0] = new Node(new Node[args.length]));
        else
            return succ[0].expand(args);
    }

/** print a graph
 */
    public void print()
    {
        System.out.print("--");
        succ[0].print("  ");
    }

/** print a node with special indentation
 */
    public void print(String indent)
    {
        String  str;
        if (dependant == 0)
        {
            str = "( )--";
            indent += "     ";
            if ((succ[0] == null) || (succ[0].dependant >= 0))
            {
                str += "-";
                indent += " ";
            }
        }
        else
        if (dependant > 0)
        {
            str = "(" + dependant + ")--";
            for (int i = 0; i < str.length(); i++)
                indent += " ";
            if ((succ[0] == null) || (succ[0].dependant >= 0))
            {
                str += "-";
                indent += " ";
            }
        }
        else
        {
            str = "+";
            indent += "|";
        }
        System.out.print(str);
        for (int i = 0; i < succ.length; i++)
        {
            if (i > 0)
            {
                System.out.println(indent);
                System.out.print(indent);
            }
            if (succ[i] == null)
                System.out.println("*");
            else
                succ[i].print(indent);
        }
    }
}

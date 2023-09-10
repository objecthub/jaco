//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.framework.parser;


public final class ParseStack
{
    public Symbol[] stack = new Symbol[64];
    public int      sp = 0;
    
    
    private void extend()
    {
        Symbol[]    newStack = new Symbol[stack.length * 2];
        System.arraycopy(stack, 0, newStack, 0, stack.length);
        stack = newStack;
    }
    
    public final Symbol push(Symbol sym)
    {
        if (sp == stack.length)
            extend();
        return stack[sp++] = sym;
    }
    
    public final Symbol pop()
    {
        return stack[--sp];
    }
    
    public final Symbol peek()
    {
        return stack[sp - 1];
    }
    
    public final boolean empty()
    {
        return sp == 0;
    }
    
    public final Symbol elementAt(int offset)
    {
        return stack[offset];
    }
    
    public final Symbol relativeToTop(int offsetToTop)
    {
        return stack[sp - 1 - offsetToTop];
    }
    
    public final int size()
    {
        return sp;
    }
    
    public final Symbol reuse(int id, int lpos, int rpos, Object o)
    {
        Symbol  sym = stack[lpos];
        sym.sym = id;
        sym.parse_state = -1;
        sym.right = stack[rpos].right;
        sym.value = o;
        return sym;
    }
    
    public final Symbol reuse(int id, int pos, Object o)
    {
        Symbol  sym = stack[pos];
        sym.sym = id;
        sym.parse_state = -1;
        sym.value = o;
        return sym;
    }
    
    public final Symbol reuse(int id)
    {
        Symbol  sym = stack[sp - 1];
        sym.sym = id;
        return sym;
    }
}

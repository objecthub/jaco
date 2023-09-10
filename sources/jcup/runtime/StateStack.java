package jcup.runtime;

public final class StateStack
{
    public int[]    stack = new int[64];
    public int      sp = 0;
    
    
    private void extend()
    {
        int[]   newStack = new int[stack.length * 2];
        System.arraycopy(stack, 0, newStack, 0, stack.length);
        stack = newStack;
    }
    
    public final int push(int state)
    {
        if (sp == stack.length)
            extend();
        return stack[sp++] = state;
    }
    
    public final int pop()
    {
        return stack[--sp];
    }
    
    public final int peek()
    {
        return stack[sp - 1];
    }
    
    public final boolean empty()
    {
        return sp == 0;
    }
    
    public final int elementAt(int offset)
    {
        return stack[offset];
    }
    
    public final int relativeToTop(int offsetToTop)
    {
        return stack[sp - 1 - offsetToTop];
    }
    
    public final int size()
    {
        return sp;
    }
}

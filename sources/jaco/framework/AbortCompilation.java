//      /   _ _      JaCo
//  \  //\ / / \     - abort compilation exception
//   \//  \\_\_/     
//         \         Matthias Zenger, 20/12/97

package jaco.framework;


public class AbortCompilation extends Exception
{
    public static final int PANIC = -1;
    public static final int TOPLEVEL = 0;
    
    protected int           level;
    public Object           info;
    
    
    public AbortCompilation(int level)
    {
        super("abort compilation (level " + level + ")");
        this.level = level;
    }
    
    public AbortCompilation(int level, String message)
    {
        super(message);
        this.level = level;
    }
    
    public int level()
    {
        return level;
    }
}

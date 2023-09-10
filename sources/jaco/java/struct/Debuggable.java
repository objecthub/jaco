//      /   _ _      JaCo
//  \  //\ / / \     - various interfaces
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.struct;

import jaco.framework.*;


public interface Debuggable
{
    String getDescription();
    String getDebugName();
    boolean debug(int debugId, TreeList tree) throws AbortCompilation;
}

public interface EnterExitHooks
{
    TreeList enter(TreeList treelist) throws AbortCompilation;
    TreeList exit(TreeList treelist) throws AbortCompilation;
}

public interface TreeProcessor extends EnterExitHooks
{
    Tree process(Tree tree) throws AbortCompilation;
}

public interface TreeListProcessor extends EnterExitHooks
{
    TreeList process(TreeList treelist) throws AbortCompilation;
}

//      /   _ _      JaCo
//  \  //\ / / \     - a Java semantic analyzer that does not report errors
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;


public class SilentSemanticAnalyzer extends SemanticAnalyzer
{
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        report.setErrorsPossible(false);
        return super.enter(treelist);
    }
    
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        treelist = super.exit(treelist);
        report.setErrorsPossible(true);
        return treelist;
    }
}

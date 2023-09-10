//      /   _ _      JaCo
//  \  //\ / / \     - Wrapper translator
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;


public class TransWrapper extends jaco.java.component.Translator {

/** component name
 */
    public String getName()
    {
        return "TransWrapper";
    }
    
/** return descrition of translator
 */
    public String getDescription()
    {
        return "translating Wrapper";
    }

/** the translation methods
 */
    protected Tree translateDecl(Tree tree, Env env) {
        switch (tree) {
            default:
                return super.translateDecl(tree, env);
        }
    }
    
    protected Tree translateStat(Tree tree, Env env) {
        switch (tree) {
            default:
                return super.translateStat(tree, env);
        }
    }
    
    protected Tree translateExpr(Tree tree, Env env) {
        switch (tree) {
            default:
                return super.translateExpr(tree, env);
        }
    }
    
    protected Tree translateType(Tree tree, Env env) {
        switch (tree) {
            default:
                return super.translateType(tree, env);
        }
    }
}

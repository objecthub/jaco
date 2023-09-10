//      /   _ _      JaCo
//  \  //\ / / \     - environment for a compilation unit; directs the compilation
//   \//  \\_\_/       of the compilation unit
//         \         Matthias Zenger, 30/05/01

package jaco.java.struct;

import jaco.framework.*;
import jaco.java.context.*;
import java.util.*;


public class CompilationEnv
{
/** the compiler environment
 */
    public JavaSettings settings;
    
/** the sourcefile of this tree
 */
    public Sourcefile   source;
    
/** number of errors and warnings detected in this
 *  compilation unit
 */
    public int          errors = 0;
    public int          warnings = 0;
    public int          deprecations = 0;

/** translate the compilation unit completely?
 */
    public boolean shortRun = false;
    
/** are there any nested classes?
 */
    public boolean nestedClasses = false;
    
/** is the tree attributed?
 */
    public boolean attributed = false;

/** has bytecode been generated for this tree?
 */
    public boolean codeGenerated = false;
    
/** which classes use asserts?
 */
    public HashMap classesWithAsserts = new HashMap();
    
/** constructor
 */
    public CompilationEnv(Sourcefile source, JavaSettings settings)
    {
        this.source = source;
        this.settings = settings;
    }
    
/** reset a toplevel environment
 */
    public void reset()
    {
        shortRun = false;
        nestedClasses = false;
        attributed = false;
        codeGenerated = false;
        classesWithAsserts.clear();
    }
}

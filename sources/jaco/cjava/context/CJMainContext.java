//  /   _ _      JaCo
//  \  //\ / / \     - the global compiler context
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.cjava.component.*;
import jaco.cjava.struct.*;
import jaco.java.component.*;


public class CJMainContext extends MainContext 
{

    /**
     * If true, the cj extensions are
     * enabled and the context objects will
     * return the CJ versions of the components
     * and context.
     */
    private boolean cj_enabled = true;

    /**
     * Disable CJ extensions.
     *
     * Note that it is not possible to re-enable CJ after this
     * call.
     *
     * @see CJAliases#disable
     * @see CJCompounds#disable
     * @see CJMainContext#useCJ
     */
    public void disableCJ()
    {
    if(cj_enabled)
        {
        cj_enabled = false;
        CJAliases().disable();
        CJCompounds().disable();
        }
    }

    /**
     * Check whether CJ extensions are enabled.
     */
    public boolean useCJ()
    {
    return cj_enabled;
    }

/** context constructor
 */
    public CJMainContext(CJContext context) {
    super(context);
    }

    CJAliases cjaliases;
    public CJAliases CJAliases()
    {
    if(cjaliases==null)
        {
        cjaliases = new CJAliases();
        cjaliases.init(this);
        }
    return cjaliases;
    }

    protected CJCompounds cjcompounds;
    public CJCompounds CJCompounds()
    {
    if(cjcompounds==null)
        {
        cjcompounds = new CJCompounds();
        cjcompounds.init(this);
    }
    return cjcompounds;
    }

    protected CJSignatures cjsignatures;
    public CJSignatures CJSignatures()
    {
    if(cjsignatures==null)
    {
            cjsignatures = new CJSignatures();
        cjsignatures.init(this);
    }
    return cjsignatures;
    }
    
/** factory methods for all tool components
 */
    public PrettyPrinter PrettyPrinter() {
    if (pretty == null) 
        {
        pretty = new CJPrettyPrinter();
        pretty.init(this);
    }
    return pretty;
    }

    public Trees Trees()
    {
    if (trees == null)
    {
        trees = new CJTrees();
        trees.init(this);
    }
    return trees;
    }

    public Types Types()
    {
    if (types == null)
    {
        types = new CJTypes();
        types.init(this);
    }
    return types;
    }

    public ErrorHandler ErrorHandler()
    {
    if (report == null)
    {
        report = new CJErrorHandler();
        report.init(this);
    }
    return report;
    }

    public Definitions Definitions()
    {
    if (definitions == null)
    {
        definitions = new CJDefinitions();
        definitions.init(this);
    }
    return definitions;
    }

    public ClassReader ClassReader()
    {
    if (reader == null)
    {
        reader = new CJClassReader();
        reader.init(this);
    }
    return reader;
    }

/** factory methods for tree processor components
 */
    public jaco.java.component.Compiler Compiler()
    {
    jaco.cjava.component.Compiler   compiler =
        new jaco.cjava.component.Compiler();
    compiler.init(CompilerContext());
    return compiler;
    }

/** factory methods for contexts
 */
    protected CompilerContext CompilerContext()
    {
    return new CJCompilerContext(this);
    }
}

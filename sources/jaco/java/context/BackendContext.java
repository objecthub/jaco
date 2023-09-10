//      /   _ _      JaCo
//  \  //\ / / \     - the backend context
//   \//  \\_\_/     
//         \         Matthias Zenger, 16/10/00

package jaco.java.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;


public class BackendContext extends Context
{
/** enclosing context
 */
    public CompilerContext      compilerContext;

/** singleton components of the context
 */
    protected Coder             coder;
    protected Items             items;
    protected BytecodeGenerator generator;
    
    
/** context constructor
 */
    public BackendContext(CompilerContext context)
    {
        super(context);
        compilerContext = context;
    }
    
/** factory methods for all components
 */
    public Coder Coder()
    {
        if (coder == null)
        {
            coder = new Coder();
            coder.init(this);
        }
        return coder;
    }
    
    public Items Items()
    {
        if (items == null)
        {
            items = new Items();
            items.init(this);
        }
        return items;
    }
    
    public TransInner TransInner()
    {
        TransInner  transinner = new TransInner();
        transinner.init(this);
        return transinner;
    }
    
    public SemanticAnalyzer SemanticAnalyzer()
    {
        SemanticAnalyzer attributor = new SemanticAnalyzer();
    attributor.init(SemanticContext());
    return attributor;
    }
    
    public BytecodeGenerator BytecodeGenerator()
    {
        if (generator == null)
        {
            generator = new BytecodeGenerator();
            generator.init(this);
        }
        return generator;
    }
    
/** local contexts
 */
    protected SemanticContext SemanticContext()
    {
        return new SemanticContext(compilerContext);
    }

/** factory methods for datatypes
 */
    public Code Code(boolean fat)
    {
        return new Code(fat, ((JavaSettings)settings).debuginfo);
    }
    
    public Pool Pool()
    {
        return new Pool();
    }
}

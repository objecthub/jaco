//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.stat.component.*;


public class MainPizzaContext extends MainContext
{
/** support for algebraic classes?
 */
    public boolean                  algebraicClasses = true;
    
    
    public MainPizzaContext(PizzaContext context)
    {
        super(context);
    }
    
    public Trees Trees()
    {
        if (trees == null)
        {
            trees = new PizzaTrees();
            trees.init(this);
        }
        return trees;
    }

    public PrettyPrinter PrettyPrinter()
    {
        if (pretty == null)
        {
            pretty = new PizzaPrettyPrinter();
            pretty.init(this);
        }
        return pretty;
    }

    public ClassReader ClassReader()
    {
        if (reader == null)
        {
            reader = new PizzaClassReader();
            reader.init(this);
        }
        return reader;
    }

    public Types Types()
    {
        if (types == null)
        {
            types = new PizzaTypes();
            types.init(this);
        }
        return types;
    }
    
    public Definitions Definitions()
    {
        if (definitions == null)
        {
            definitions = new PizzaDefinitions();
            definitions.init(this);
        }
        return definitions;
    }
    
/** factory methods for tree processor components
 */
    public jaco.java.component.Compiler Compiler()
    {
        ExtendedCompiler    compiler = new ExtendedCompiler();
        compiler.init(CompilerContext());
        return compiler;
    }
    
/** factory methods for contexts
 */
    protected CompilerContext CompilerContext()
    {
        return new PizzaCompilerContext(this);
    }
    
/** factory methods for data structures
 */
    public CompilationEnv CompilationEnv(Sourcefile source)
    {
        return new CompilationEnv(source, (PizzaSettings)settings);
    }
}

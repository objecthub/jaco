//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.jjava.context;

import jaco.framework.*;
import jaco.jjava.component.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;


public class JMainContext extends MainContext
{
    public JMainContext(JJavaContext context)
    {
        super(context);
    }

    public Trees Trees()
    {
        if (trees == null)
        {
            trees = new JTrees();
            trees.init(this);
        }
        return trees;
    }

    public PrettyPrinter PrettyPrinter()
    {
        if (pretty == null)
        {
            pretty = new JPrettyPrinter();
            pretty.init(this);
        }
        return pretty;
    }
/*

    public ClassReader ClassReader()
    {
        if (reader == null)
        {
            reader = new ClassReader();
            reader.init(this);
        }
        return reader;
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
*/
    public jaco.java.component.Compiler Compiler()
    {
        JCompiler   compiler = new JCompiler();
        compiler.init(CompilerContext());
        return compiler;
    }

    public Types Types()
    {
        if (types == null)
        {
            types = new JTypes();
            types.init(this);
        }
        return types;
    }
/** factory methods for contexts
 */
    protected CompilerContext CompilerContext()
    {
        return new JCompilerContext(this);
    }
}

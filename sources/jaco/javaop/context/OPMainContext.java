//      /   _ _      JaCo
//  \  //\ / / \     - the global compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.javaop.component.*;
import jaco.javaop.struct.*;
import jaco.java.component.*;


public class OPMainContext extends MainContext {

/** context constructor
 */
    public OPMainContext(OPContext context) {
        super(context);
    }
    
/** factory methods for all tool components
 */
    public PrettyPrinter PrettyPrinter() {
        if (pretty == null) {
            pretty = new OPPrettyPrinter();
            pretty.init(this);
        }
        return pretty;
    }

/*
    public ErrorHandler ErrorHandler()
    {
        if (report == null)
        {
            report = new ErrorHandler();
            report.init(this);
        }
        return report;
    }

    public Trees Trees()
    {
        if (trees == null)
        {
            trees = new Trees();
            trees.init(this);
        }
        return trees;
    }
    
    public Mangler Mangler()
    {
        if (mangler == null)
        {
            mangler = new Mangler();
            mangler.init(this);
        }
        return mangler;
    }
    
    public Disassembler Disassembler()
    {
        if (disassem == null)
        {
            disassem = new Disassembler();
            disassem.init(this);
        }
        return disassem;
    }
    
    public Classfiles Classfiles()
    {
        if (classfiles == null)
        {
            classfiles = new Classfiles();
            classfiles.init(this);
        }
        return classfiles;
    }
    
    public ClassReader ClassReader()
    {
        if (reader == null)
        {
            reader = new ClassReader();
            reader.init(this);
        }
        return reader;
    }
    
    public Signatures Signatures()
    {
        if (signatures == null)
        {
            signatures = new Signatures();
            signatures.init(this);
        }
        return signatures;
    }
*/  
    public NameResolver NameResolver()
    {
        if (namer == null)
        {
            namer = new OPNameResolver();
            namer.init(this);
        }
        return namer;
    }
/*  
    public Modifiers Modifiers()
    {
        if (modifiers == null)
        {
            modifiers = new Modifiers();
            modifiers.init(this);
        }
        return modifiers;
    }
*/
    public Operators Operators()
    {
        if (operators == null)
        {
            operators = new OPOperators();
            operators.init(this);
        }
        return operators;
    }
/*
    public Constants Constants()
    {
        if (constants == null)
        {
            constants = new Constants();
            constants.init(this);
        }
        return constants;
    }
    
    public Types Types()
    {
        if (types == null)
        {
            types = new Types();
            types.init(this);
        }
        return types;
    }
    
    public Definitions Definitions()
    {
        if (definitions == null)
        {
            definitions = new Definitions();
            definitions.init(this);
        }
        return definitions;
    }
*/

/** factory methods for tree processor components
 */
    public jaco.java.component.Compiler Compiler()
    {
        jaco.javaop.component.Compiler  compiler =
            new jaco.javaop.component.Compiler();
        compiler.init(CompilerContext());
        return compiler;
    }

/** factory methods for contexts
 */
    protected CompilerContext CompilerContext()
    {
        return new OPCompilerContext(this);
    }

/** factory methods for data structures

    public CompilationEnv CompilationEnv(Sourcefile source)
    {
        return new CompilationEnv(source, (JavaSettings)settings);
    }
*/
}

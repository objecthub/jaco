//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               main compiler context

package jaco.keris.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.keris.component.*;


public class XMainContext extends MainContext {
    
/** singleton components
 */
    ModuleContexts modules;

/** support for algebraic classes?
 */
    public boolean algebraicClasses = true;
    
    
/** constructor
 */
    public XMainContext(XContext context) {
        super(context);
    }
    
/** factory methods for components
 */
    public Trees Trees() {
        if (trees == null) {
            trees = new XTrees();
            trees.init(this);
        }
        return trees;
    }
    
    public PrettyPrinter PrettyPrinter() {
        if (pretty == null) {
            pretty = new XPrettyPrinter();
            pretty.init(this);
        }
        return pretty;
    }

    public ClassReader ClassReader() {
        if (reader == null) {
            reader = new XClassReader();
            reader.init(this);
        }
        return reader;
    }
    
    public Definitions Definitions() {
        if (definitions == null) {
            definitions = new XDefinitions();
            definitions.init(this);
        }
        return definitions;
    }

    public NameResolver NameResolver() {
        if (namer == null) {
            namer = new XNameResolver();
            namer.init(this);
        }
        return namer;
    }
    
    public Signatures Signatures() {
        if (signatures == null) {
            signatures = new XSignatures();
            signatures.init(this);
        }
        return signatures;
    }
    
    public Types Types() {
        if (types == null) {
            types = new XTypes();
            types.init(this);
        }
        return types;
    }
    
    public ModuleContexts ModuleContexts() {
        if (modules == null) {
            modules = new ModuleContexts();
            modules.init(this);
        }
        return modules;
    }
    
/** factory methods for tree processor components
 */
    public jaco.java.component.Compiler Compiler() {
        XCompiler compiler = new XCompiler();
        compiler.init(CompilerContext());
        return compiler;
    }
    
/** factory methods for contexts
 */
    protected CompilerContext CompilerContext() {
        return new XCompilerContext(this);
    }

/** factory methods for data structures
 */
    public CompilationEnv CompilationEnv(Sourcefile source) {
        return new CompilationEnv(source, (XSettings)settings);
    }
}

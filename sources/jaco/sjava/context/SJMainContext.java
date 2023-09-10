//      /   _ _      JaCo
//  \  //\ / / \     - the global compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.sjava.context;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.sjava.component.*;
import jaco.sjava.struct.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import java.util.Hashtable;


public class SJMainContext extends MainContext {

	public Hashtable activeMethods = new Hashtable();

/** context constructor
 */
	public SJMainContext(SJContext context) {
		super(context);
	}
	
/** factory methods for all tool components
 */
	public PrettyPrinter PrettyPrinter() {
		if (pretty == null) {
			pretty = new SJPrettyPrinter();
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
*/
	public Trees Trees()
	{
		if (trees == null)
		{
			trees = new SJTrees();
			trees.init(this);
		}
		return trees;
	}
/*
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
	}*/
	
	public ClassReader ClassReader()
	{
		if (reader == null)
		{
			reader = new SJClassReader();
			reader.init(this);
		}
		return reader;
	}
	
	public Modifiers Modifiers()
	{
		if (modifiers == null)
		{
			modifiers = new SJModifiers();
			modifiers.init(this);
		}
		return modifiers;
	}
/*
	public Operators Operators()
	{
		if (operators == null)
		{
			operators = new Operators();
			operators.init(this);
		}
		return operators;
	}
	
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
		jaco.sjava.component.Compiler	compiler =
			new jaco.sjava.component.Compiler();
		compiler.init(CompilerContext());
		return compiler;
	}

/** factory methods for contexts
 */
	protected CompilerContext CompilerContext()
	{
		return new SJCompilerContext(this);
	}

/** factory methods for data structures
 */
	public CompilationEnv CompilationEnv(Sourcefile source)
	{
		SJCompilationEnv ce = new SJCompilationEnv(source, (JavaSettings)settings);
		ce.activeMethods = activeMethods;
		return ce;
	}

}

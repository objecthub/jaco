//      /   _ _      JaCo
//  \  //\ / / \     - the initial compiler context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.sjava.context;

import jaco.framework.*;
import jaco.sjava.component.*;


public class SJContext extends jaco.java.context.JavaContext
{
/** context constructor
 */
	public SJContext(SJSettings settings)
	{
		super(settings);
	}

/** factory method for the compiler of this context
 */
	public jaco.java.component.JavaCompiler JavaCompiler()
	{
		SJCompiler	compiler = new SJCompiler();
		compiler.init(MainContext());
		return compiler;
	}
	
/** factory method for the main compilation context
 */
	protected jaco.java.context.MainContext MainContext()
	{
		if (((SJSettings)settings).java)
			return new jaco.java.context.MainContext(this);
		else
			return new SJMainContext(this);
	}
}

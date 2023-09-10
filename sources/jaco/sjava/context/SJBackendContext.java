//      /   _ _      JaCo
//  \  //\ / / \     - the backend context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.sjava.context;

import jaco.framework.*;


public class SJBackendContext extends jaco.java.context.BackendContext
{
/** context constructor
 */
	public SJBackendContext(SJCompilerContext context)
	{
		super(context);
	}
	
/** factory methods for all components
 
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
		TransInner	transinner = new TransInner();
		transinner.init(this);
		return transinner;
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
*/

/** factory methods for datatypes

	public Code Code(boolean fat)
	{
		return new Code(fat, ((JavaSettings)settings).debuginfo);
	}
	
	public Pool Pool()
	{
		return new Pool();
	}
*/
}

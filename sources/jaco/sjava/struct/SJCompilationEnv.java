package jaco.sjava.struct;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import java.util.Hashtable;


public class SJCompilationEnv extends CompilationEnv
{
	public Hashtable activeMethods;

	public SJCompilationEnv(Sourcefile source, JavaSettings settings)
	{
		super(source, settings);
	}
	
	public void reset()
	{
		super.reset();
		activeMethods.clear();
	}
}

package jaco.sjava.struct;

import jaco.java.struct.*;
import jaco.framework.*;
import Definition.*;

public class SJTree extends Tree {

	public case SelectStat(SelectCase[] cases, boolean b);
	
        // This is the old code of SelectCase
	// public case SelectCase(Tree when, Tree synchStat, Tree[] stats);
	
	public case AcceptStat(Name name, MethodDef[] defs, Tree formObj, Name forallVal, boolean fromDefined);
		
	public case WaitUntilStat(Tree expr);

        // DUY 03.10.01: Modify the parameter list of this function in order to apply the ForAll
	public case SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats); 


	public static interface Factory extends Tree.Factory {
		Tree SelectStat(SelectCase[] cases, boolean b);
                // This is the old code of SelectCase
		// Tree SelectCase(Tree when, Tree synchStat, Tree[] stats);	
		Tree AcceptStat(Name name, MethodDef[] defs, Tree formObj, Name forallVal, boolean fromDefined);
		Tree WaitUntilStat(Tree expr);							
                // DUY 03.10.01: Modify the parameter list of this function in order to apply the ForAll
		Tree SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats); 
	}
}

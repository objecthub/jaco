package jaco.sjava.struct;

import jaco.java.struct.*;
import SJTree.*;
import jaco.framework.*;
import Definition.*;

public class SJTreeFactory extends TreeFactory implements SJTree.Factory {
	public Tree SelectStat(SelectCase[] cases, boolean b) {
		return new SelectStat(cases, b);
	}

        // This is the old code of SelectCase
	/*public Tree SelectCase(Tree when, Tree synchStat, Tree[] stats) {
		return new SelectCase(when, synchStat, stats);
	}*/
	
	public Tree AcceptStat(Name name, MethodDef[] defs, Tree fromObj, Name forallName, boolean fromDefined) {
		return new AcceptStat(name, defs, fromObj, forallName, fromDefined);
	}
	
	public Tree WaitUntilStat(Tree expr) {
		return new WaitUntilStat(expr);
	}

	// DUY 03.10.01: Modify this SelectCase in order to apply for ForAll
	public Tree SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats) {
		return new SelectCase(index, maxNumber, when, syncCase, stats);
	}
}

public class SJTreeCreate extends TreeCreate implements SJTree.Factory {
	public Tree SelectStat(SelectCase[] cases, boolean b) {
		SelectStat t = new SelectStat(cases, b);
		t.pos = protoPos;
		return t;
	}
	
        // This is the old code of SelectCase
	/*public Tree SelectCase(Tree when, Tree synchStat, Tree[] stats) {
		SelectCase t = new SelectCase(when, synchStat, stats);
		t.pos = protoPos;
		return t;
	}*/
	
	public Tree AcceptStat(Name name, MethodDef[] defs, Tree fromObj, Name forallName, boolean fromDefined) {		
		AcceptStat t = new AcceptStat(name, defs, fromObj, forallName, fromDefined);
		t.pos = protoPos;
		return t;
	}
	
	public Tree WaitUntilStat(Tree expr) {
		WaitUntilStat t = new WaitUntilStat(expr);
		t.pos = protoPos;
		return t;
	}
        
	// DUY 03.10.01: Modify this SelectCase in order to apply for ForAll
	public Tree SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats) {
		SelectCase t = new SelectCase(index, maxNumber, when, syncCase, stats);
		t.pos = protoPos;
		return t;
	}
}

public class SJTreeRedef extends TreeRedef implements SJTree.Factory {
	public Tree SelectStat(SelectCase[] cases, boolean b) {
		SelectStat t = new SelectStat(cases, b);
		t.pos = protoTree.pos;
		t.type = protoTree.type;
		return t;
	}
	
        // This is the old code of SelectCase
	/*public Tree SelectCase(Tree when, Tree synchStat, Tree[] stats) {
		SelectCase t = new SelectCase(when, synchStat, stats);
		t.pos = protoTree.pos;
		t.type = protoTree.type;
		return t;
	}*/
	
	public Tree AcceptStat(Name name, MethodDef[] defs, Tree fromObj, Name forallName, boolean fromDefined) {
		AcceptStat t = new AcceptStat(name, defs, fromObj, forallName, fromDefined);
		t.pos = protoTree.pos;
		t.type = protoTree.type;
		return t;
	}
	
	public Tree WaitUntilStat(Tree expr) {
		WaitUntilStat t = new WaitUntilStat(expr);
		t.pos = protoTree.pos;
		t.type = protoTree.type;
		return t;
	}
        
	// DUY 03.10.01: Modify this SelectCase in order to apply for ForAll
	public Tree SelectCase(Name index, Tree maxNumber, Tree when, Tree syncCase, Tree[] stats) {
		SelectCase t = new SelectCase(index, maxNumber, when, syncCase, stats);
		t.pos = protoTree.pos;
		t.type = protoTree.type;
		return t;
	}
}

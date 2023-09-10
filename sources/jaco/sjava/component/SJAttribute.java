//      /   _ _      JaCo
//  \  //\ / / \     - tree attribution and type checking for Java syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/11/99

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.sjava.struct.*;
import java.util.*;
import Tree.*;
import Type.*;
import Definition.*;


public class SJAttribute extends Attribute implements SJModifierConst {

/** component name
 */
    public String getName() {
	return "SJavaAttribute";
    }
	
/** component initialization
 */
    public void init(SemanticContext context) {
	super.init(context);
    }
	
    public void attribDecl(Tree tree, ContextEnv env) {
	sw: switch (tree) {
	    case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
			   Tree[] defs, ClassDef c):
                SJCompilationEnv cenv = (SJCompilationEnv)env.toplevel.info;
                if (((mods & ACTIVE) != 0) &&
                    (cenv.activeMethods.get(c) == null))
                    cenv.activeMethods.put(c, new Integer(0));
		super.attribDecl(tree, env);
	       	if (((mods & ACTIVE) == 0) &&
                    ((((ClassDecl)tree).def.supertype().tdef().modifiers & ACTIVE) != 0))
		    report.error(tree.pos, "non-active classes cannot extend active classes");
		break;
	    case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
	  		    Tree[] thrown, Tree[] stats, MethodDef f):
		super.attribDecl(tree, env);
	        if ((f.modifiers & ACTIVE) != 0) {
                    Definition c = f.owner;
		    for (Definition e = c.locals().lookup(f.name); e.scope != null; e = e.next())
			if ((f != e.def) &&
                            (e.def.kind == FUN) &&
                            ((e.def.modifiers & ACTIVE) != 0))
			    break sw;
                    MethodId.enterInto(f, ((SJCompilationEnv)env.toplevel.info).activeMethods);
                }
	        break;
	    default:
		super.attribDecl(tree, env);
        }
    }

    public Type attribStat(Tree tree, ContextEnv env, Type pt, Type sofar) {
	trees.pushPos(tree.pos);
	tree.type = sofar;
	switch ((SJTree)tree) {
	    case SelectStat(SJTree.SelectCase[] cases, boolean b):	
		sofar = attribStats(cases,env,pt,sofar);	
		break;
	    case SelectCase(Name index, Tree maxNumber, Tree when, Tree synchStat, Tree[] stats):
		if (when != null)
                    attribExpr(when, env, VAL, types.booleanType);
		if (synchStat != null)
		    switch ((SJTree)synchStat) {
			case AcceptStat(_,_,_,_,_):
                            
                        case SelectCase(_,_,_,_,_): // DUY 03.10.01: more parameters for this function
                            
			case WaitUntilStat(_):
			    sofar = attribStat(synchStat, env, pt,sofar);
			    break;
			case Exec(Apply(Tree fn, _)):
			    switch (fn) {
                                case Select(Self(_, _, _), _, _):
                                    report.error(tree.pos, "this/super qualification for synchronous calls is illegal");
                                    break;
				case Select(_, _, _):
				    sofar = attribStat(synchStat, env, pt,sofar);
				    Definition f = ((Select)fn).def;
				    if ((f.kind < BAD) && ((f.modifiers & ACTIVE) == 0))
					report.error(fn.pos, "only methods of active classes can be called in a synchronization statement");
				    break;
				default:
				    report.error(tree.pos, "only qualified method calls allowed for synchronization statements");
			    }
			    break;
                        case Exec(Assign(_, Apply(Tree fn, _))):
			    switch (fn) {
                                case Select(Self(_, _, _), _, _):
                                    report.error(tree.pos, "this/super qualification for synchronous calls is illegal");
                                    break;
				case Select(_, _, _):
				    sofar = attribStat(synchStat, env, pt,sofar);
				    Definition f = ((Select)fn).def;
				    if ((f.kind < BAD) && ((f.modifiers & ACTIVE) == 0))
					report.error(fn.pos, "only methods of active classes can be called in a synchronization statement");
				    break;
				default:
				    report.error(tree.pos, "only qualified method calls allowed for synchronization statements");
			    }
			    break;
			default:
			    report.error(tree.pos, "only qualified method calls allowed for synchronization statements");
		    }
		if (stats.length != 0)
                    sofar = attribStats(stats, env.dup(tree, env.info), pt, sofar);
		break;
	    case AcceptStat(Name name, _, _, _, _):
		ClassDef c = env.enclClass.def;
		if ((c.modifiers & ACTIVE) == 0)
		    report.error(tree.pos, "cannot accept methods in non-active classes");
		else if (env.info.isStatic)
                    report.error(tree.pos, "cannot accept method in a static context");
                else {
		    MethodDef[] defs = new MethodDef[0];
		    for (Definition e = c.locals().lookup(name);
			 e.scope != null; e = e.next()) {
			if ((e.def.kind == FUN) && ((e.def.modifiers & ACTIVE) != 0)) {
			    MethodDef[] newdefs = new MethodDef[defs.length + 1];
			    System.arraycopy(defs, 0, newdefs, 0, defs.length);
			    newdefs[defs.length] = (MethodDef)e.def;
			    defs = newdefs;
			}
		    }
		    ((SJTree.AcceptStat)tree).defs = defs;		    
		    if (defs.length == 0) 
			report.error(tree.pos, "unknown method");		    
		}
		break;
            // DUY 03.10.01: This is the old case of SelectCase
	    /*case SelectCase(Name index, Name maxNumber, Tree when, Tree syncCase, Tree[] stats):
		// attribExpr(syncCase, env, VAL, types.longType);
		break;      
             */
                
	    case WaitUntilStat(Tree expr):
		attribExpr(expr, env, VAL, types.longType);
		break;	   
	    default:
		super.attribStat(tree, env, pt, sofar);
	}
	trees.popPos();
	return sofar;
    }
}

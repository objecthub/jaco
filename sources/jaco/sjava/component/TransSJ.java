//      /   _ _      JaCo
//  \  //\ / / \     - SJ translator
//   \//  \\_\_/
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;
import Definition.*;
import jaco.sjava.component.*;
import jaco.sjava.struct.*;
import jaco.sjava.context.*;
import java.util.*;
import jaco.framework.*;


public class TransSJ extends jaco.java.component.Translator
                     implements DefinitionConst, SJModifierConst, TypeConst {

    final static Name SJAVA_ACTIVE = Name.fromString("sjava.Active");
    final static Name SJAVA_ACTIVEOBJECT = Name.fromString("sjava.ActiveObject");
    final static Name SJAVA_SELECT = Name.fromString("sjava.Select");
    final static Name SJAVA_SELECT_MAKE = Name.fromString("make");
    final static Name AO = Name.fromString("a$o");
    final static Name GET_CLASS = Name.fromString("getClass");
    final static Name ZERO = Name.fromString("0");
    final static Name THIS = Name.fromString("this");
    final static Name ACTMETHCOUNT = Name.fromString("a$m");

    
    CompilationUnit toplevel;
    Hashtable tempVars;
    int tempVarCounter;
    int selectCounter;
    Name currentSelectName;
    
    SJMainContext mainContext;

    Set runnableActives = new HashSet();

    Constants constants;
    Types types;
    // DUY 31.10.01: 
    boolean[] checkArray = new boolean[100];
    int tempForLoopNumber;
    Tree maxNumberForLoop;

/** component name
 */
    public String getName()
    {
	return "TransSJ";
    }

    public void init(CompilerContext context) {
	super.init(context);
	mainContext = (SJMainContext)context.mainContext;
	constants = mainContext.Constants();
	types = mainContext.Types();
    }

/** return descrition of translator
 */
    public String getDescription()
    {
        return "translating SJ";
    }

    public String getDebugName()
    {
        return "transj";
    }
	
    protected Tree process(CompilationUnit tree) throws AbortCompilation
    {
        toplevel = tree;
        Tree res = super.process(tree);
        if (((SJSettings)mainContext.settings).preproc)
            pretty.outputUnit(res, "java", true);
        return res;
    }
    
    protected Name newTempVar(Tree tree) {
        Name res = Name.fromString("temp$Receiver$" + tempVarCounter++);
        tempVars.put(tree, res);
        return res;
    }
    
    protected Name newSelectVar() {
        return (currentSelectName = Name.fromString("temp$Select$" + selectCounter++));
    }
    
/** the translation methods
 */
    protected Tree translateDecl(Tree tree, Env env) {	
	switch (tree) {
	    case ClassDecl(Name name, int mods, Tree extending,
			   Tree[] implementing,Tree[] members, ClassDef c):		
		if ((c.modifiers & ACTIVE) == 0)
		    return super.translateDecl(tree, env);
		else {
		    ClassDecl cd = (ClassDecl)tree;		 
		    cd.implementing = trees.append(cd.implementing, trees.Qualid(SJAVA_ACTIVE));
		    // if no run method, don't start a thread
		    Definition fdef = cd.def.locals().lookup(Name.fromString("run"));
		    while (fdef.scope != null) {
			if ((fdef.kind == FUN) &&
			    ((fdef.modifiers & PUBLIC) != 0) &&
			    (fdef.type.argtypes().length == 0) &&
			    (fdef.type.restype() == Type.VoidType))
			    break;
			fdef = fdef.next();
		    }
		    if (fdef.scope != null) {
			runnableActives.add(cd.def);
			cd.implementing = trees.append(cd.implementing, trees.Qualid(Name.fromString("java.lang.Runnable")));
		    }

		    SJCompilationEnv cenv = (SJCompilationEnv)((CompilationUnit)env.tree).info;
		    cd.members = transDecls(cd.members,classEnv((ClassDecl)tree, env));
                    if ((c.supertype().tdef().modifiers & ACTIVE) == 0) {
                        if ((c.modifiers & INTERFACE) == 0) {
                            cd.members = trees.append(
                                newdef.MethodDecl(AO, PUBLIC, trees.Qualid(SJAVA_ACTIVEOBJECT), new Tree.VarDecl[0], trees.noTrees,
                                                    new Tree[]{newdef.Return(newdef.Ident(AO))}), cd.members);
                            cd.members = trees.append(newdef.VarDecl(AO, PUBLIC, trees.Qualid(SJAVA_ACTIVEOBJECT), null),cd.members);
			int incr = ((Number)cenv.activeMethods.get(c)).intValue();
 			cd.members = trees.append(
						  newdef.VarDecl(ACTMETHCOUNT, PUBLIC | STATIC | FINAL, newdef.BasicType(INT),
								 newdef.Literal(constants.make.IntConst(incr))),
						  cd.members);
			
			cd.members = trees.append(
						  newdef.MethodDecl(ACTMETHCOUNT, PROTECTED | STATIC, newdef.BasicType(INT),
								    new Tree.VarDecl[0], trees.noTrees,
								    new Tree[]{newdef.Return(newdef.Ident(ACTMETHCOUNT))}),
						  cd.members);
                        } else {
                            cd.members = trees.append(
                                newdef.MethodDecl(AO, PUBLIC, trees.Qualid(SJAVA_ACTIVEOBJECT), new Tree.VarDecl[0], trees.noTrees,
                                                    null), cd.members);
                        }
                    }
                   /* if (c.supertype != null) {
                        ClassDef c1 = (ClassDef)c.supertype().tdef();
		        while ((c1 != null) && cenv.activeMethods.containsKey(c1)) {
	    		    incr += ((Number)cenv.activeMethods.get(c1)).intValue();
                            if (c1.supertype() == null)
                                break;
                            c1 = (ClassDef)c1.supertype().tdef();
                        }
                    } */
		    cd.mods &= ~ACTIVE;
                    cenv.activeMethods.put(c.fullname, cenv.activeMethods.get(c));
		    return tree;
		}
            
	    case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
			    Tree[] thrown, Tree[] stats, MethodDef f):
                Tree res = null;
                Hashtable oldTempVars = tempVars;
                tempVars = new Hashtable();
                int oldCount = tempVarCounter;
                int oldSelectCount = selectCounter;
                tempVarCounter = 0;
                selectCounter = 0;
                MethodDecl md = (MethodDecl)tree;
                ClassDef owner = (ClassDef)env.enclClass.def();
	        if ((owner.modifiers & ACTIVE) != 0) {
            	        // translate constructor
	                if ((md.restype == null) && ((env.enclClass.mods & ABSTRACT) == 0)) {
		            SJCompilationEnv cenv = (SJCompilationEnv)(toplevel.info);
		            int number = ((Integer)(cenv.activeMethods.get(owner))).intValue();
                            if ((owner.supertype().tdef().modifiers & ACTIVE) != 0)
                    	        number += ((Integer)(cenv.activeMethods.get(owner.supertype().tdef()))).intValue();
		            Tree selected = newdef.Ident(env.enclClass.name);
			    Tree newStat = null;
			    if (runnableActives.contains(owner) ) {
				newStat =  newdef.If(newdef.Binop(Operators.EQ,
                                    newdef.Apply(trees.Qualid(GET_CLASS),new Tree[0]),
                                    newdef.Select(selected,Name.fromString("class"))),
				    newdef.Exec(newdef.Apply(newdef.Select(
                                          newdef.NewObj(null,trees.Qualid(Name.fromString("java.lang.Thread")),  // CP 30.7.02
                                                                              new Tree[]{trees.This()},null),    // CP 30.7.02
					  Name.fromString("start")), new Tree[0])), null);
			    }
		            md.stats = transStats(md.stats,methodEnv((MethodDecl)tree,env));
			    if ((owner.supertype().tdef().modifiers & ACTIVE) == 0) {
				Tree[] nstats = new Tree[md.stats.length + 1];
				nstats[0] = md.stats[0];
				nstats[1] = newdef.Exec(newdef.Assign(trees.Qualid(AO),
							      newdef.NewObj(null,trees.Qualid(SJAVA_ACTIVEOBJECT),
							        new Tree[]{trees.This(),
									   newdef.Apply(newdef.Ident(ACTMETHCOUNT),
											trees.noTrees)}, null)));
									   //newdef.Literal(constants.make.IntConst(number))}, null)))
				System.arraycopy(md.stats, 1, nstats, 2, md.stats.length - 1);
				md.stats = nstats;
			    }
			    if (newStat != null)
				md.stats = trees.append(md.stats,newStat);
            	        // translate
		        } else if ((f.modifiers & ACTIVE) != 0) {
                            md.mods |= 0;//SYNCHRONIZED;
		            ClassDef cd = (ClassDef)owner.supertype().tdef();
		            int incr = -1;
		            while ((cd != null) && ((SJCompilationEnv)toplevel.info).activeMethods.containsKey(cd)) {
	    		        incr += ((Integer)(((SJCompilationEnv)toplevel.info).activeMethods.get(cd))).intValue();
                                if (cd.supertype() == null)
                                    break;
                                cd = (ClassDef)cd.supertype().tdef();
                            }
                            cd = (ClassDef)owner.supertype().tdef();
                	    int localId = MethodId.lookupAndRemove(f, ((SJCompilationEnv)toplevel.info).activeMethods);
                	    if (localId >= 0) {
                	        md.stats = transStats(md.stats,methodEnv((MethodDecl)tree,env));
                                Name n = Name.fromString(name.toString() + "$SJID");
                                Definition fdef = owner.locals().lookup(n);
                                while ((fdef.scope != null) && ((fdef.owner.modifiers & INTERFACE) != 0))
                                    fdef = fdef.next();
                                if (fdef.scope == null) {
                                    if ((owner.modifiers & INTERFACE) != 0) {
                    	                res = trees.at(tree).make(trees.Container(
                                            new Tree[]{
                                                   newdef.MethodDecl(n,
                                                        PUBLIC, trees.toTree(types.intType), new Tree.VarDecl[0], trees.noTrees, null),
					           md = (MethodDecl)redef.MethodDecl(name, md.mods, restype, params, thrown, md.stats)}));
                                    } else {
                                        res = trees.at(tree).make(trees.Container(
                                            new Tree[]{newdef.VarDecl(n,
                                                                  PUBLIC | STATIC | FINAL,
							          trees.toTree(types.intType),
							          newdef.Literal(constants.make.IntConst(localId + incr))),
                                                   newdef.MethodDecl(n, PUBLIC, trees.toTree(types.intType), new Tree.VarDecl[0], trees.noTrees,
                                                        new Tree[]{newdef.Return(newdef.Ident(n))}),
					           md = (MethodDecl)redef.MethodDecl(name, md.mods, restype, params, thrown, md.stats)}));
                                    }
                                }
      			    } else
		                md = (MethodDecl)super.translateDecl(tree, env);
		        } else
            		    md = (MethodDecl)super.translateDecl(tree, env);
     		    } else
	                md = (MethodDecl)super.translateDecl(tree, env);
                for (int i = 0; i < tempVarCounter; i++) {
                    Type t = (Type)tempVars.get(Name.fromString("temp$Receiver$" + i)); 
                    // DUY 31.10.01                    
                    if (checkArray[i] == true){
                        md.stats = trees.append(newdef.VarDecl(Name.fromString("temp$Receiver$" + i), 
                                                                0, 
                                                                newdef.ArrayTypeTerm(trees.toTree(t)),
                                                                newdef.NewArray(trees.toTree(t), new Tree[]{newdef.Literal(constants.make.IntConst(0))})), 
                                                md.stats); // DUY 30.10.01
                    }    
                    else{
                        md.stats = trees.append(newdef.VarDecl(Name.fromString("temp$Receiver$" + i), 
                                         0, trees.toTree(t), trees.Null()), md.stats);
                    }                    
                    // DUY 31.10.01 End.
                }
                for (int i = 0; i < selectCounter; i++) {
                    md.stats = trees.append(newdef.VarDecl(Name.fromString("temp$Select$" + i),
                                     0, trees.Qualid(Name.fromString("sjava.Select")), trees.Null()), md.stats);
                }
                tempVars = oldTempVars;
                tempVarCounter = oldCount;
                selectCounter = oldSelectCount;
                return (res != null) ? res : md;
                
	    default:
	        return super.translateDecl(tree, env);
	}
    }

    public Tree getApply(Tree selector, Tree c, int i,Env env) {
        Tree retApply;
        // DUY 31.10.01 Initiate the checkArray = false: no array for the current variable
        // check if the length of this array is exceed, then the array must be extended!
        if (tempVarCounter >= checkArray.length){
            boolean[] tempArray = new boolean[checkArray.length + 100];
            System.arraycopy(checkArray, 0, tempArray, 0, checkArray.length);
            checkArray = tempArray;
        }
        checkArray[tempVarCounter] = false;
	switch((SJTree)c) {
	    case SelectCase(Name index, Tree maxNumber, Tree when, Tree synchStat, _):
                if (index != null){
                    // DUY 31.10.01 if there is index, -> the current variable must be declared as array
                    checkArray[tempVarCounter] = true;
                    tempForLoopNumber = tempVarCounter;
                    maxNumberForLoop = maxNumber;
                }

		if (synchStat == null) {
                    Tree guard = (when != null) ? transExpr(when, env) : newdef.Ident(Name.fromString("true"));
                    return (Apply)newdef.Apply(
                        newdef.Select(selector,Name.fromString("defaultCase")),
		        	      new Tree[]{newdef.Literal(constants.make.IntConst(i)), guard});
                }
                // DUY 21.09.2001: Add new parameter for the CALL, this is default as 0
                Tree newParam = newdef.Literal(constants.make.IntConst(0));
		switch((SJTree)synchStat) {
		    case AcceptStat(Name name, MethodDef[] defs, Tree fromExpr, Name forallName, boolean fromDefined):
		        Tree methodId = newdef.Apply(newdef.Ident(Name.fromString(name + "$SJID")), trees.noTrees);
			Tree guard = (when != null) ? transExpr(when,env) : newdef.Ident(Name.fromString("true"));
			Tree fromAddr = transExpr(fromExpr, env);
			Tree forallVal = (index!=null) ? newdef.Ident(index)
                                                            : newdef.Literal(constants.make.IntConst(0));
                        // DUY 09.10.01 Check if this is the ForAll, then set the value of newParam
                        if (index != null)
                            newParam = newdef.Ident(index);
                        // DUY 09.10.01 End
                                                            
			retApply = (newdef.Apply(newdef.Select(selector,Name.fromString("accept")),
						    (fromDefined) ? new Tree[]{newdef.Literal(constants.make.IntConst(i)),methodId,guard,fromAddr,forallVal}
				                                     : new Tree[]{newdef.Literal(constants.make.IntConst(i)),methodId,guard}));
                                                                     
                       // DUY 09.10.2001 
                       // check if this is the syntax for ForLoop or just normal case, if this is a ForLoop
                       // then build the ForLoop statement, but this return value will not be added by newdef.Exec
                       if (index != null){
                           Tree[] init = new Tree[1];
                           init[0] = newdef.Exec(newdef.Assign(newdef.Ident(index), newdef.Literal(constants.make.IntConst(0))));
                           Tree cond = newdef.Binop(OperatorConst.LT, newdef.Ident(index), maxNumber);
                           Tree[] step = new Tree[1];
                           step[0] = newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(index)));

                           return newdef.ForLoop(init, cond, step, cond); // DUY 01.11.01
                       }
                       return retApply;
                       
		    case Exec(Apply(Tree fn, _)):
			switch (fn) {
                            case Select(Ident(Name selname, Definition qualdef), Name selectSelector, Definition def):
                                // DUY 09.10.01 Check if this is the ForAll, then set the value of newParam
                                if (index != null)
                                    newParam = newdef.Ident(index);
                                    // DUY 09.10.01 End
                                
                                if (qualdef.owner.kind == FUN) {
                                    Tree num = newdef.Literal(constants.make.IntConst(i));
				    Tree objId = newdef.Apply(newdef.Select(newdef.Ident(selname),
			                        Name.fromString(selectSelector + "$SJID")), trees.noTrees);
				    Tree activeObject = (when!=null) ?
                                        newdef.If(transExpr(when,env),
					    newdef.Apply(newdef.Select(newdef.Ident(selname), AO), trees.noTrees),
					    newdef.Select(trees.Qualid(Name.fromString("sjava.Select")),
					        Name.fromString("IGNORE_AO"))) :
					newdef.Apply(newdef.Select(newdef.Ident(selname), AO), trees.noTrees);
				    retApply = (Apply)newdef.Apply(newdef.Select(selector,Name.fromString("call")),
							   		new Tree[]{num,activeObject,objId, newParam}); // DUY 21.09.2001
                                } else {
                                    Name temp = newTempVar(c);
                                    tempVars.put(temp, qualdef.type);
                                    
                                    // DUY 02.11.01
                                    Tree arrayElemt = newdef.Ident(temp);
                                    // DUY 22.10.01
                                    if (index != null)
                                        arrayElemt = newdef.Index(arrayElemt, newdef.Ident(index));
                                    
			            Tree num = newdef.Literal(constants.make.IntConst(i));
                                    Tree receiver = newdef.Assign(arrayElemt, newdef.Ident(selname));
				    Tree objId = 
                                        newdef.If(newdef.Binop(Operators.EQ,
                                            arrayElemt, trees.Null()),
                                            newdef.Literal(constants.make.IntConst(-1)),
                                            newdef.Apply(newdef.Select(
                                                    arrayElemt,
			                            Name.fromString(selectSelector + "$SJID")), trees.noTrees));
				    Tree activeObject = (when != null) ?
                                        newdef.If(transExpr(when,env),
					    newdef.Apply(newdef.Select(receiver, AO), trees.noTrees),
					    newdef.Select(trees.Qualid(Name.fromString("sjava.Select")),
					        Name.fromString("IGNORE_AO"))) :
					newdef.Apply(newdef.Select(receiver, AO), trees.noTrees);
				    retApply = (Apply)newdef.Apply(newdef.Select(selector,Name.fromString("call")),// DUY Test
							      new Tree[]{num, activeObject, objId, newParam}); // DUY 21.09.2001
                                }

                                // DUY 09.10.2001 
                                // check if this is the syntax for ForLoop or just normal case, if this is a ForLoop
                                // then build the ForLoop statement, but this return value will not be added by newdef.Exec
                                if (index != null){
                                    Tree[] init = new Tree[1];
                                    init[0] = newdef.Exec(newdef.Assign(newdef.Ident(index), newdef.Literal(constants.make.IntConst(0))));
                                    Tree cond = newdef.Binop(OperatorConst.LT, newdef.Ident(index), maxNumber);
                                    Tree[] step = new Tree[1];
                                    step[0] = newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(index)));

                                    return newdef.ForLoop(init, cond, step, newdef.Exec(retApply));
                                }
                                return retApply;
                                // Duy 09.10.01 End
			    case Select(Tree selected, Name selectSelector, Definition def):
                                // DUY 31.10.01
                                int tempNumber = tempVarCounter;
                                Name temp = newTempVar(c);
                                tempVars.put(temp, selected.type);
                                // DUY 02.11.01
                                Tree arrayElemt = newdef.Ident(temp);
                                // DUY 22.10.01
                                if (index != null)
                                    arrayElemt = newdef.Index(arrayElemt, newdef.Ident(index));
                                
			        Tree num = newdef.Literal(constants.make.IntConst(i));
                                Tree receiver = newdef.Assign(arrayElemt, transExpr(selected, env));
				Tree objId = 
                                    newdef.If(newdef.Binop(Operators.EQ,
                                        arrayElemt, trees.Null()),
                                        newdef.Literal(constants.make.IntConst(-1)),
                                        newdef.Apply(newdef.Select(arrayElemt,
			                        Name.fromString(selectSelector + "$SJID")), trees.noTrees));
				Tree activeObject = (when != null) ?
                                        newdef.If(transExpr(when,env),
					    newdef.Apply(newdef.Select(receiver, AO), trees.noTrees),
					    newdef.Select(trees.Qualid(Name.fromString("sjava.Select")),
					        Name.fromString("IGNORE_AO"))) :
					newdef.Apply(newdef.Select(receiver, AO), trees.noTrees);
                                        
                                // DUY 09.10.01 Check if this is the ForAll, then set the value of newParam
                                if (index != null)
                                    newParam = newdef.Ident(index);
                                // DUY 09.10.01 End
                                        
				retApply = (Apply)(newdef.Apply(newdef.Select(selector,Name.fromString("call")),// DUY Test
							   		new Tree[]{num,activeObject,objId, newParam})); // DUY 21.09.2001
                                // DUY 09.10.2001 
                                // check if this is the syntax for ForLoop or just normal case, if this is a ForLoop
                                // then build the ForLoop statement, but this return value will not be added by newdef.Exec
                                if (index != null){
                                    Tree[] init = new Tree[1];
                                    init[0] = newdef.Exec(newdef.Assign(newdef.Ident(index), newdef.Literal(constants.make.IntConst(0))));
                                    Tree cond = newdef.Binop(OperatorConst.LT, newdef.Ident(index), maxNumber);
                                    Tree[] step = new Tree[1];
                                    step[0] = newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(index)));
                                
                                    return newdef.ForLoop(init, cond, step, newdef.Exec(retApply));
                                }
                                return retApply;

                                // Duy 09.10.01 End
			    default:
				return null;
			}
                    case Exec(Assign(_, Apply(Select(Tree selected, Name selectSelector, Definition def), _))): {
                        Name temp = newTempVar(c);
                        tempVars.put(temp, selected.type);
                        
                        // DUY 02.11.01
                        Tree arrayElemt = newdef.Ident(temp);
                        // DUY 22.10.01
                        if (index != null)
                            arrayElemt = newdef.Index(arrayElemt, newdef.Ident(index));
                        
			Tree num = newdef.Literal(constants.make.IntConst(i));
                        Tree receiver = newdef.Assign(arrayElemt, transExpr(selected, env));
			Tree objId =
                            newdef.If(newdef.Binop(Operators.EQ,
                                arrayElemt, trees.Null()),
                                newdef.Literal(constants.make.IntConst(-1)),
                                newdef.Apply(newdef.Select(arrayElemt,
			                Name.fromString(selectSelector + "$SJID")), trees.noTrees));
			Tree activeObject = (when != null) ?
                                newdef.If(transExpr(when,env),
				    newdef.Apply(newdef.Select(receiver, AO), trees.noTrees),
				    newdef.Select(trees.Qualid(Name.fromString("sjava.Select")),
					Name.fromString("IGNORE_AO"))) :
				newdef.Apply(newdef.Select(receiver, AO), trees.noTrees);

                        // DUY 09.10.01 Check if this is the ForAll, then set the value of newParam
                        if (index != null)
                            newParam = newdef.Ident(index);
                        // DUY 09.10.01 End
                                
                        retApply = (Apply)(newdef.Apply(newdef.Select(selector,Name.fromString("call")),// DUY Test
							   	new Tree[]{num,activeObject,objId, newParam})); // DUY 21.09.2001
                        // DUY 09.10.2001 
                        // check if this is the syntax for ForLoop or just normal case, if this is a ForLoop
                        // then build the ForLoop statement, but this return value will not be added by newdef.Exec
                        if (index != null){
                           Tree[] init = new Tree[1];
                           init[0] = newdef.Exec(newdef.Assign(newdef.Ident(index), newdef.Literal(constants.make.IntConst(0))));
                           Tree cond = newdef.Binop(OperatorConst.LT, newdef.Ident(index), maxNumber);
                           Tree[] step = new Tree[1];
                           step[0] = newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(index)));

                           return newdef.ForLoop(init, cond, step, newdef.Exec(retApply));
                        }
                        return retApply;
                        // Duy 09.10.01 End
                    }
		    case WaitUntilStat(Tree expr):
			Tree id = newdef.Literal(constants.make.IntConst(i));
			Tree timeout = (when!=null)?newdef.If(transExpr(when,env),
							      transExpr(expr,env),
							      newdef.Select(trees.Qualid(
                                                                Name.fromString("sjava.Select")),
                                                                Name.fromString("IGNORE_WAITUNTIL"))):
							      transExpr(expr,env);
			return (Apply)newdef.Apply(newdef.Select(selector,Name.fromString("timeout")),
			                           new Tree[]{id,timeout});
		    default:
			throw new InternalError("illegal sync statement " + synchStat);
		}
                		
	    default:
                return null;		
	}
    }

    protected Tree translateStat(Tree tree, Env env) {
	switch ((SJTree)tree) {
	    case SelectStat(SJTree.SelectCase[] cases, boolean noWait):
                Name oldName = currentSelectName;
		Tree[] s = new Tree[1];
		Case[] c = new Case[0];
                Tree retApply;
                String stemp;
                int itemp;
                
		s[0] = newdef.Exec(trees.at(tree.pos).make(
                    newdef.Assign(newdef.Ident(newSelectVar()),
                                  newdef.Apply(trees.Qualid(Name.fromString("sjava.Select.make")),
                                               new Tree[0]))));
                
		for(int i=0;i<cases.length;i++) {
		    Tree[] temp1 = new Tree[s.length+1];
		    System.arraycopy(s,0,temp1,0,s.length);
                    s = temp1;
                    retApply = getApply(newdef.Ident(currentSelectName),cases[i],i,env);
                    // check if type of Tree is ForLoop or not, if this is not ForLoop, then transform to new.Exec()
                    stemp = retApply.toString();
                    // 21.11.01 DUY
                    // check the ForLoop tree node by the switch
                    switch (retApply){
                        case ForLoop(_, _, _, _):
                            // DUY 31.10.01
                            temp1 = new Tree[s.length+1];
                            System.arraycopy(s,0,temp1,0,s.length);
                            s = temp1;

                            // add this If statement before the ForLoop statement
                            Type t = (Type)tempVars.get(Name.fromString("temp$Receiver$" + tempForLoopNumber)); 
                            Tree cond = newdef.Binop(OperatorConst.LT, newdef.Select(newdef.Ident(Name.fromString("temp$Receiver$" + tempForLoopNumber)), 
                                                                        Name.fromString("length")), maxNumberForLoop);
                            Tree thenPart = newdef.Exec(newdef.Assign(newdef.Ident(Name.fromString("temp$Receiver$" + tempForLoopNumber)), 
                                                                        newdef.NewArray(trees.toTree(t), new Tree[]{maxNumberForLoop})));

                            s[s.length - 2] = newdef.If(cond, thenPart, null);
                            // DUY 31.10.01 End
                            break;
                        default:
                            retApply = newdef.Exec(retApply);
                    }
                    
                    s[s.length - 1] = retApply;

                    Case[] temp = new Case[c.length+1];
		    System.arraycopy(c,0,temp,0,c.length);
		    temp[temp.length-1]=(Case)newdef.Case(new Tree[]{newdef.Literal(constants.make.IntConst(i))},
		    trees.getContainerContent(transStat(cases[i],env)));
		    c=temp;
		}
                
                Tree[] temp1 = new Tree[s.length+1];
		System.arraycopy(s,0,temp1,0,s.length);
                s = temp1;
		String strSync;
		if (noWait)
		    strSync = "syncNoWait";
		else
		    strSync = "syncWait";
		if (((SJSettings)mainContext.settings).selectLabel)
		    s[s.length - 1] = newdef.Switch(newdef.Apply(
				        newdef.Select(newdef.Ident(currentSelectName),Name.fromString(strSync)),
					new Tree[]{(((env.enclClass.def.modifiers & ACTIVE) != 0) &&
					              ((env.enclMethod.def.modifiers & STATIC) == 0)) 
						   ? trees.Qualid(AO) : trees.Null(),
					newdef.Literal(constants.make.IntConst(Position.line(tree.pos)))}),
				       c); // CP 29.7.02
		else
		    s[s.length - 1] = newdef.Switch(newdef.Apply(
                             newdef.Select(newdef.Ident(currentSelectName),Name.fromString(strSync)),
			     new Tree[]{(((env.enclClass.def.modifiers & ACTIVE) != 0) &&
					 ((env.enclMethod.def.modifiers & STATIC) == 0)) 
					     ? trees.Qualid(AO) : trees.Null()}), c);

		currentSelectName = oldName;
                return newdef.Block(0, s);
                
	    case SelectCase(Name index, Tree maxNumber, Tree when, Tree synchStat, Tree[] stats):
		Tree newStat[] = new Tree[0];
		if (synchStat != null)
                    switch (synchStat) {
                        case Exec(Apply(Select(Tree sel, Name m, _), Tree[] args)): {
                            Name tempVar = (Name)tempVars.get(tree);// DUY 22.10.01	
                            Tree arrayElemt;
                            arrayElemt = newdef.Ident(tempVar);
                            // DUY 02.11.01
                            if (index != null)
                                  arrayElemt = newdef.Index(arrayElemt, newdef.Ident(index));
                                
                            if (tempVar == null)
                                newStat = trees.append(newStat, trees.at(synchStat).make(
                                            redef.Exec(
                                                newdef.Apply(newdef.Select(transExpr(sel, env), m), transExprs(args, env)))));
                            else{ 
                                if (index != null){ // DUY 02.11.01	Add new line "i=a$o.forall$Value;" before the 2 old lines
                                    newStat = trees.append(newStat, newdef.Exec(newdef.Assign(newdef.Ident(index), 
                                                            newdef.Select(trees.Qualid(AO),Name.fromString("forall$Value")))));
                                }
                                newStat = trees.append(newStat, trees.at(synchStat).make(
                                                            redef.Exec(newdef.Apply(newdef.Select(arrayElemt, m), transExprs(args, env)))));

                            }
                            newStat = trees.append(newStat, new Tree[]{trees.at(synchStat).make(
                                                            newdef.Exec(
                                                            newdef.Apply(newdef.Select(newdef.Ident(currentSelectName), Name.fromString("triggerAccept")), new Tree[0])))});
                            break;
                        }
                        case Exec(Assign(Tree lhs, Apply(Select(Tree sel, Name m, _), Tree[] args))): {
                            Name tempVar = (Name)tempVars.get(tree);
                            // DUY 04.11.01
                            Tree arrayElemt;
                            arrayElemt = newdef.Ident(tempVar);
                            // DUY 02.11.01
                            if (index != null)
                                  arrayElemt = newdef.Index(arrayElemt, newdef.Ident(index));
                            
                            if (tempVar == null)
                                newStat = trees.append(newStat, trees.at(synchStat).make(
                                        redef.Exec(
                                            newdef.Assign(transExpr(lhs, env),
	    				    newdef.Apply(newdef.Select(transExpr(sel, env), m), transExprs(args, env))))));
                            else
                                if (index != null){ // DUY 02.11.01	Add new line "i=a$o.forall$Value;" before the 2 old lines
                                    newStat = trees.append(newStat, newdef.Exec(newdef.Assign(newdef.Ident(index), 
                                                            newdef.Select(trees.Qualid(AO),Name.fromString("forall$Value")))));
                                }
                                newStat = trees.append(newStat, trees.at(synchStat).make(
                                        redef.Exec(
                                            newdef.Assign(transExpr(lhs, env),
	    				    newdef.Apply(newdef.Select(arrayElemt, m),
                                                         transExprs(args, env))))));
                            newStat = trees.append(newStat, new Tree[]{trees.at(synchStat).make(
                                newdef.Exec(
                                    newdef.Apply(newdef.Select(newdef.Ident(currentSelectName), Name.fromString("triggerAccept")), new Tree[0])))});
                            break;
                        }
                    }
		if (stats.length > 0)
		    newStat = trees.append(newStat, transStats(stats,env));			
		return trees.Container(trees.append(newStat,newdef.Break(null)));
	    case AcceptStat(_, _, _, _, _):
		throw new InternalError();
            case Exec(Apply(_, _)):
                Tree res;
                switch (res = super.translateStat(tree, env)) {
                    case Exec(If(Tree cond, Apply(Tree left, Tree[] args), Tree elsecase)):
                        return trees.at(tree.pos).make(newdef.If(cond,
                                    newdef.Exec(newdef.Apply(left, args)),
                                    newdef.Throw(newdef.NewObj(null,
                                        trees.Qualid(Name.fromString("java.lang.InternalError")),
                                        new Tree[0], null))));
                    case Exec(If(Tree cond, Tree thencase, Tree elsecase)):
                        return trees.at(tree.pos).make(newdef.If(cond, thencase, elsecase));
                    default:
                        return res;
                }
	    case WaitUntilStat(_):
		throw new InternalError();
	    default:
		return super.translateStat(tree, env);
	}
    }
 
   
    protected Tree translateExpr(Tree tree, Env env) {
       
	switch (tree) {
            case Apply(Select(Tree expr, Name m, Definition f), Tree[] args):
                switch (expr) {
                    case Self(_, _, _):
                        return super.translateExpr(tree, env);
                }
                if ((f.modifiers & ACTIVE) != 0)  {
                    Tree selectCase = new SJTree.SelectCase(null, null, null, new Tree.Exec(tree), null);
                    Name oldName = currentSelectName;
                    Tree selector =
                        getApply(
                            trees.at(tree.pos).make(
                                newdef.Assign(newdef.Ident(newSelectVar()),
                                    newdef.Apply(trees.Qualid(Name.fromString("sjava.Select.make")), new Tree[0]))),
                            selectCase,
                            0,
                            env);
		    if (((SJSettings)mainContext.settings).selectLabel)
			selector = trees.at(tree.pos).make(
                            newdef.Apply(
                                newdef.Select(selector, Name.fromString("syncWait")),
                            		new Tree[]{(((env.enclClass.def.modifiers & ACTIVE) != 0) &&
                                                                ((env.enclMethod.def.modifiers & STATIC) == 0)) ?
                                                                    trees.Qualid(AO) : trees.Null(),
						   newdef.Literal(constants.make.IntConst(Position.line(tree.pos)))})); //CP 29.7.02
		    else
			selector = trees.at(tree.pos).make(
                            newdef.Apply(
                                newdef.Select(selector, Name.fromString("syncWait")),
                            		new Tree[]{(((env.enclClass.def.modifiers & ACTIVE) != 0) &&
                                                                ((env.enclMethod.def.modifiers & STATIC) == 0)) ?
                                                                    trees.Qualid(AO) : trees.Null()}));
                    Tree alternate;
                    switch (tree.type) {
                        case ErrType:
                        case VoidType:
                            alternate = trees.at(tree.pos).make(
                                newdef.Throw(newdef.NewObj(null,
                                    trees.Qualid(Name.fromString("java.lang.InternalError")),
                                    new Tree[0], null)));
                            break;
                        case NullType:
                        case ClassType(_):
                        case ArrayType(_):
                            alternate = trees.at(tree.pos).make(trees.Null());
                            break;
                        case NumType(int tag):
                            switch (tag) {
                                case BOOLEAN:
                                    alternate = trees.at(tree.pos).make(newdef.Ident(Name.fromString("true")));
                                    break;
                                case BYTE:
                                case CHAR:
                                case SHORT:
                                case INT:
                                    alternate = trees.at(tree.pos).make(
                                                    newdef.Literal(constants.make.IntConst(0)));
                                    break;
                                case LONG:
                                    alternate = trees.at(tree.pos).make(
                                                    newdef.Literal(constants.make.LongConst(0)));
                                    break;
                                case FLOAT:
                                    alternate = trees.at(tree.pos).make(
                                                    newdef.Literal(constants.make.FloatConst(0.0f)));
                                    break;
                                case DOUBLE:
                                    alternate = trees.at(tree.pos).make(
                                                    newdef.Literal(constants.make.DoubleConst(0.0)));
                                    break;
                                default:
                                    throw new InternalError();
                            }
                            break;
                        default:
                            throw new InternalError();
                    }
                    Name tempVar = (Name)tempVars.get(selectCase);
                    Tree newExpr;
                    if (tempVar == null)
                        newExpr = super.translateExpr(tree, env);
                    else
                        newExpr = trees.at(tree.pos).make(
                                    newdef.Apply(newdef.Select(newdef.Ident(tempVar), m),
                                                 transExprs(args, env)));
                    if (tree.type != Type.ErrType) {
                        if (tree.type == Type.VoidType) {
                            newExpr = trees.at(tree.pos).make(
                                newdef.Block(0, new Tree[]{
                                    newdef.Exec(newExpr),
                                    newdef.Exec(newdef.Apply(
                                        newdef.Select(newdef.Ident(currentSelectName), Name.fromString("triggerAccept")),
                                        new Tree[0]))}));
                        } else {
                            newExpr = trees.at(tree.pos).make(
                                newdef.Apply(
                                    newdef.Select(newdef.Ident(currentSelectName), Name.fromString("triggerAccept")),
                                    new Tree[]{newExpr}));
                            switch (tree.type) {
                                case ClassType(_):
                                case ArrayType(_):
                                    newExpr = trees.at(tree.pos).make(
                                        newdef.Typeop(OperatorConst.TYPECAST, newExpr, trees.toTree(tree.type)));
                            }
                        }
                    }
                    currentSelectName = oldName;
                    return trees.at(tree.pos).make(
                        newdef.If(newdef.Binop(OperatorConst.EQ, selector, newdef.Literal(constants.make.IntConst(0))),
                                  newExpr, alternate));
                } else
                    return super.translateExpr(tree, env);

	    default:
		return super.translateExpr(tree, env);
	}
    }
}

// JEnterMembers
// Stewart Itzstein

package jaco.jjava.component;
 
import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.jjava.struct.*;
import jaco.jjava.component.*;
import jaco.jjava.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import Definition.*;
 
 

public class JEnterMembers extends EnterMembers implements JModifierConst{
  public String getName() {
    return "JEnterMembers";
  }
  public Definition memberEnter(Tree tree, ContextEnv env) {
    StringBuffer newName = null;
    Definition def = null;

    switch (tree) {
    case MethodDecl(Name name, int mods, Tree restype,VarDecl[] params, Tree[] thrown, Tree[] stats,_):
      JMethodDecl md = ((JMethodDecl)tree).join;
      newName = new StringBuffer().append(name);

      if (((JMethodDecl)tree).join != null) {
    System.out.println("Join Here");
    // if current join patterns exist enter them into scope after
    // checking that they are the same as the declaration
    
    while (md != null) {
      //create new name to be used
      newName.append("$");
      newName.append(md.name);
      // scan through params
      for (int i = 0; i < md.params.length; i++) {
        //System.out.println("Param found:" + md.params);
        //enterVar(md.params[i], localEnv);
        //System.out.println("Param found:" + md.params);
      }
      md = md.join;
    }
    System.out.println("Name found:" + newName.toString());
      }

      System.out.println("Entering Function Name");
      //insert supercode here
      int   fmods = modifiers.MethMods;
      if (restype == null) // we encounter a constructor declaration here
    {
      if ((name == env.enclClass.name) || (name == PredefConst.INIT_N))
        {
          ((MethodDecl)tree).name = name = PredefConst.INIT_N;
          fmods = modifiers.ConstrMods;
        }
      else
        {
          report.error(tree.pos, "return.type.required");
          ((MethodDecl)tree).restype = trees.errorTree;
        }
    }
      fmods = modifiers.completeMods(tree.pos, mods, env, fmods,
                     modifiers.InterfaceMethMods);
      Scope enclscope = accountant.enterScope(env);
      if ((enclscope.owner.modifiers & STRICTFP) != 0)
    fmods |= STRICTFP;
      Definition f = definitions.make.MethodDef(fmods, name, null, enclscope.owner);
      tree.setDef(f);
      ContextEnv    localEnv = accountant.methodEnv((MethodDecl)tree, env);
      f.type = attribute.attribSignature(params, restype, thrown, localEnv);
      f.type.setDef(f);
      if (checkUnique(tree.pos, f, enclscope))
    enclscope.enter(f);
      localEnv.info.scope.leave();
      def = f;
      //end supercode
      //Definition def = super.memberEnter(tree,env);
      
      System.out.println("Finished Entering Function Name");
      
      if (restype != null)
    switch (restype) {
      // select SIGNAL type and set def modifier to reflect this
    case BasicType(JTypeConst.SIGNAL):
      System.out.println("signal detected: " + def.name);
      def.modifiers |= SIGNAL;
      break;
    }      
      
      return def;
    default:
      return super.memberEnter(tree,env);
    }
  }
}

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

  public boolean jcheckUnique(int pos, Definition def, Scope s,LinkedList md)
  { 
    Definition e = s.lookup(def.name);
    while (e.scope == s)
      {
    if ((e.def != def) &&
        (e.def.kind == def.def.kind) &&
        (def.name != PredefConst.ERROR_N) &&
        ((e.def.kind != FUN) || types.sametype(e.def.type, def.def.type)))
      {
        
        //handle join patterns here
        if (md != null) {
          System.out.println("join detected in JCheck -> JEnterMembers");
          return true;
        }
        
            //name the same
        report.error(pos, "duplicate.def", def + def.location());
        return false;
      }
    e = e.next();
      }
    return true;
  }
  
  public Definition memberEnter(Tree tree, ContextEnv env) {
    Definition def = null;
    switch (tree) {
    case MethodDecl(Name name, int mods, Tree restype,VarDecl[] params, Tree[] thrown, Tree[] stats,_):
      LinkedList md = ((JMethodDecl)tree).joinpatterns;

      //scan through join patterns
      if (((JMethodDecl)tree).joinpatterns != null) {
    
    System.out.println("Found a join pattern");
      }

      int fmods = modifiers.MethMods;

      // if name matches class name and no return type must be constructor
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

      fmods = modifiers.completeMods(tree.pos, mods, env, fmods,modifiers.InterfaceMethMods);
      Scope enclscope = accountant.enterScope(env);

      if ((enclscope.owner.modifiers & STRICTFP) != 0)
    fmods |= STRICTFP;
      Definition f = definitions.make.MethodDef(fmods, name, null, enclscope.owner);
      tree.setDef(f);

      ContextEnv localEnv = accountant.methodEnv((MethodDecl)tree, env);
      f.type = attribute.attribSignature(params, restype, thrown, localEnv);
      f.type.setDef(f);

      //check the name is unique
      if (jcheckUnique(tree.pos, f, enclscope, ((JMethodDecl)tree).joinpatterns))
    //enter the scope
    enclscope.enter(f);
      localEnv.info.scope.leave();

      def = f;

      if (restype != null)
    switch (restype) {
      // select SIGNAL type and set def modifier to reflect this
    case BasicType(JTypeConst.SIGNAL):
      //System.out.println("signal detected :" + def.name);
      def.modifiers |= SIGNAL;
      break;
    }      
      
      return def;
    default:
      return super.memberEnter(tree,env);
    }
  }
}

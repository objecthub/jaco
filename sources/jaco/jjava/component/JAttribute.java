// JAttribute
// Stewart Itzstein

package jaco.jjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.jjava.component.*;
import jaco.jjava.struct.*;
import jaco.jjava.context.*;
import java.util.*;
import Tree.*;
import Type.*;
import Definition.*;


public class JAttribute extends Attribute
   implements JModifierConst
{
  public String getName()
  {
    return "JAttribute";
  }
  
  public void attribDecl(Tree tree, ContextEnv env)
  {
    switch (tree)
      {
    case Tree.MethodDecl(Name name, int mods, Tree restype,VarDecl[] params,
                 Tree[] thrown, Tree[] stats,MethodDef f):
      
      ClassDef    owner = env.enclClass.def;
      checks.checkOverride(tree.pos, f);        //context.TypeChecker();
      tree.type = f.type;
      ContextEnv localEnv = accountant.methodEnv((MethodDecl)tree, env);
      localEnv.info.reported = f.type.thrown();
      checks.checkBound(thrown, types.throwableType);   //context.TypeChecker();
      LinkedList md = ((JMethodDecl)tree).joinpatterns;
            
          // enter parameters of first function in join pattern
      for (int i = 0; i < params.length; i++)
        enterVar(params[i], localEnv);
      
          //NEWSTART
          // enter rest of the parameters
          if (md != null)
              for (int i = 0; i<md.size(); i++) {
                  MethodDecl jpat = (MethodDecl)md.get(i);
                  for (int j = 0; j < jpat.params.length; j++)
                      enterVar(jpat.params[j], localEnv);
              }
          //NEWEND
          
      if ((f.modifiers & SIGNAL) != 0) {
        // make the signal look like an abstract modifier
        ((Tree.MethodDecl)tree).mods |= ABSTRACT;
        //enter into tree
        //commented this line to allow signal type to lead a 
        //method declaration
        //super.attribDecl(tree, env);
        // return the signal type to normal
        ((Tree.MethodDecl)tree).mods &= ~ABSTRACT;
        } 
      else 
      if (stats == null)
        {
          if ((owner.modifiers & INTERFACE) == 0 &&
          ((mods & (ABSTRACT | NATIVE)) == 0))
        report.error(tree.pos, "not.abstract");
        }
          else
          if ((owner.modifiers & INTERFACE) != 0)
        report.error(tree.pos, "has.body", "interface");
      else
      if ((mods & ABSTRACT) != 0)
        report.error(tree.pos, "has.body", "abstract");
      else
      if ((mods & NATIVE) != 0)
        report.error(tree.pos, "has.body", "native");
      else
        {
          if ((f.name == PredefConst.INIT_N) &&
          (owner.type != types.objectType))
        {
          if ((stats.length == 0) || !isSelfCall(stats[0]))
            {
              ((MethodDecl)tree).stats = stats =
            trees.append(trees.at(tree.pos).make(
                                 trees.SuperCall(false, new VarDecl[0])), stats);
            }
          localEnv.info.isSelfCall = true;
        }
          Type    resulttype = f.type.restype();
          attribStats(stats, localEnv, resulttype, resulttype);
        }
      
      
      localEnv.info.scope.leave();
      break;
      
      default:
    super.attribDecl(tree, env);
      }
  }
}





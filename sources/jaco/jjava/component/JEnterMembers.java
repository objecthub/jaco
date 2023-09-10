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
        //System.out.println("def "+ def + " ][ " + def.def);
        //System.out.println("kind "+ e.def.kind + " ][ " + def.def.kind);
        //System.out.println("type "+ e.def.type + " ][ " + def.def.type);
        //System.out.println("name "+ def.name);
        
            //name the same
            if (md == null && md.size() == 0) {
            report.error(pos, "duplicate.def", def + def.location());
            return false;
        }
      }
    if ((e.def != def) &&
        (e.def.kind == def.def.kind) &&
        (def.name != PredefConst.ERROR_N) &&
        ((e.def.kind != FUN) || !types.sametype(e.def.type, def.def.type))) {
            report.error(pos, "join.typemismatch", def.def.type, e.def.type);
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


      if (md != null && md.size() != 0) {
        if (debug(5)) {
            System.out.print("Join Method ");
            System.out.print( new Integer(md.size()) + " patterns | ");
            System.out.print(name);
    }
        for (int i=0;i<md.size();i++) {
                MethodDecl jpat = (MethodDecl)md.get(i);//NEW
                //print out each join part pattern
        if (debug(5)) {
                    System.out.print("&");
                    System.out.print(((JMethodDecl) md.get(i)).name);
        }

                //add each join pattern in as a method definition
                fmods = modifiers.completeMods(jpat.pos, jpat.mods, env, fmods,modifiers.InterfaceMethMods);//MODIFIED

                Definition patdef = definitions.make.MethodDef(fmods, jpat.name, null, enclscope.owner);//MODIFIED
                jpat.setDef(patdef);//MODIFIED

                localEnv = accountant.methodEnv(jpat, env);//MODIFIED
                patdef.type = attribute.attribSignature(jpat.params, jpat.restype, new Tree[0], localEnv);//MODIFIED
                patdef.type.setDef(patdef);

                //check the name is unique
                //FIX THIS FOR REUSE OF PATTERNS
                if (jcheckUnique(jpat.pos, patdef, enclscope, ((JMethodDecl)tree).joinpatterns)) {//MODIFIED
                        //enter the scope
                        enclscope.enter(patdef);//MODIFIED
        }
        }
    if (debug(5)) {
            System.out.println("()");
    }
      } else {
    if (debug(5)) {
                System.out.print("Normal Method ");
                System.out.println(name + "()");
    }
      }


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
    case ClassDecl(Name name, int mods, Tree extending, Tree[] implementing, Tree[] defs, ClassDef c):
      Definition debug_hold = super.memberEnter(tree,env);
      if (debug(4)) {
    System.out.println("Entering " + c + "finished; resulting scope:");
    c.locals.print();
      }
      return debug_hold;
    default:
      return super.memberEnter(tree,env);
    }
  }
}

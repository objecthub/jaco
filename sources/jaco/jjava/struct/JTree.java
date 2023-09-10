package jaco.jjava.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import java.util.*;


public class JMethodDecl extends Tree.MethodDecl {
  public LinkedList joinpatterns = null; 
  
  JMethodDecl(Name name, int mods, Tree restype, Tree.VarDecl[] params,
          Tree[] thrown, Tree[] stats, Definition.MethodDef def,
          LinkedList joinpatterns)
  {
    super(name, mods, restype, params, thrown, stats, def);
    this.joinpatterns = joinpatterns;
  }
}

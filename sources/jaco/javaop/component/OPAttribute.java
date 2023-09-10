//      /   _ _      JaCo
//  \  //\ / / \     - tree attribution and type checking for Java syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.javaop.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;


public class OPAttribute extends Attribute {

/** constructor - none
 */


/** component name
 */
  public String getName() {
    return "OPAttribute";
  }
  
  public Type attribExpr(Tree tree, ContextEnv env, int kind, Type pt) {

    int  ownkind  = VAL;
    Type owntype  = Type.ErrType;
    Type treetype = null;
    
    trees.pushPos(tree.pos);
    
    switch (tree) {
      
    case Binop(int opcode, Tree lhs, Tree rhs,_):
      Type[] argtypes = attribArgs(new Tree[]{lhs, rhs}, env, Type.AnyType);
      Definition operator = namer.resolveOperator(tree.pos, opcode, env, argtypes);  
      
      switch( operator ) {
      case OperatorDef(int opc) :
    trees.popPos();
    return super.attribExpr(tree, env, kind, pt);
    
      case MethodDef(Code code) :
    ((Binop)tree).operator = operator;
    owntype = operator.type.restype();
    // TODO : Should I do some more checking here (a la "case Apply") ?
    // TODO : what about ownkind ?
    break;
      }
      break;

    default:
      trees.popPos();
      return super.attribExpr(tree, env, kind, pt);
      
    }
    
    if ((owntype != Type.ErrType) &&  definitions.checkKind(tree.pos, ownkind, kind))
      tree.type = checks.checkType(tree.pos, owntype, pt);
    else
      tree.type = Type.ErrType;
    
    trees.popPos();
    
    return tree.type;
  }
  
}

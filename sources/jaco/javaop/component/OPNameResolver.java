//      /   _ _      JaCo
//  \  //\ / / \     - name resolution module
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


public class OPNameResolver extends NameResolver {

    boolean active = true;
    
/** component name
 */
  public String getName() {
    return "OPNameResolver";
  }
  
  public Definition resolveOperator(int pos, int opcode,
                                    ContextEnv env, Type[] argtypes)
    {
      if (!active)
          return super.resolveOperator(pos, opcode, env, argtypes);

      // only deals with binary op
      if ( 2 != argtypes.length )
          return super.resolveOperator(pos, opcode, env, argtypes);
      
      Definition def = typeNotFound;
      
      Name name = Name.fromString(operators.toString(opcode));
      
      Type lhs = argtypes[0];
      Type rhs = argtypes[1];

      Definition ld = null;
      Definition rd = null;

      boolean isLhsNum = false;
      boolean isRhsNum = false;

      OPOperators oper = (OPOperators)operators;

      // lets try first arg
      switch( lhs  ) {
      case ClassType( Type outer ) :
        if (lhs.tdef() == types.stringType.tdef())
            ld = super.resolveOperator(pos, opcode, env, argtypes);
        else
            ld = findMethod(env, lhs, oper.mangleName( name, true ), new Type[]{rhs});
        if ( ld.kind >= BAD ) {
                ld = null;
        }
        break;
      case NumType(int tag) :
        isLhsNum = true;
        break;
      }
      
      // lets try second arg
      switch( rhs  ) {
      case ClassType( Type outer ) :
        if (rhs.tdef() == types.stringType.tdef())
            rd = super.resolveOperator(pos, opcode, env, argtypes);
        else
            rd = findMethod(env, rhs, oper.mangleName( name, false ), new Type[]{lhs});     
        if ( rd.kind >= BAD ) {
            rd = null;
        }
        break;
      case NumType(int tag) :
        isRhsNum = true;
        break;
      }      

      // if both args are num
      if ( isLhsNum && isRhsNum ) {
        if ( opcode >= 0 ) { // try the default operators
          return super.resolveOperator(pos, opcode, env, argtypes);  
        } else {
          report.error( pos, "operator.not.found", lhs, name, rhs );
          return access(typeNotFound , pos, null, name, argtypes);
        }
      } 

      if ( (ld != null) && (rd != null) && (ld != rd)) { // check ambiguity          
        report.error( pos, "error.duplicate.def", lhs + " " + name + " " + rhs );
        return access(typeNotFound , pos, null, name, argtypes);
      }
      
      def = ( ld != null ) ? ld : def; // get the right one
      def = ( rd != null ) ? rd : def;
 
      if ( def == typeNotFound  ) {
        report.error( pos, "operator.not.found", lhs, name, rhs );
      }
           
      return access( def , pos, null, name, argtypes);
    }
  
}















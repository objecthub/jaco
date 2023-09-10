//      /   _ _      JaCo
//  \  //\ / / \     - java operator support
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
import java.util.*;


public class OPOperators extends Operators {

  /** user defined operator will be stored in this hashtable
   */  
  HashMap   operators = null;
  ArrayList useropnames = null;

  /** constructor
   */
  public OPOperators() {
    super();
    operators = new HashMap();
    useropnames = new ArrayList();
  }
    
  /** component name
   */
  public String getName() {
    return "OPOperators";
  }

  public String toString(int tag) {
    if ( tag >= 0  ) {
      return opname[tag].toString();
    } else {
      int index = (tag * (-1)) - 1;
      return (String)useropnames.get(index);
    }
  }
  
  public int getopcode(Name name) {
    String s = "" + name;
    if ( operators.containsKey( s ) ) {
      Integer n = (Integer)operators.get( s );
      return n.intValue();
    }
    int nextopcode = (operators.size() + 1) * (-1);
    operators.put( s, new Integer(nextopcode) );
    useropnames.add( s );
    return nextopcode;
  }

  public int prec(int tag){
    if ( tag >= 0 ) {
      return opprec[tag];
    } else {
      return 8; // TODO : this constant should not be hardcoded !
    }
  }


  
  public Name mangleName(Name name, boolean isLeftAssoc) {
      return Name.fromString( "OP$$" + (isLeftAssoc ? "L$" : "R$") + name );
  }

  public Name demangleName(Name name) {
      String s = "" + name;
      return s.startsWith("OP$$") ? Name.fromString(s.substring(6)) : null;
  }

  /**
   *  Should only be used after having verified the name with <code>demangleName</code>
   */
  public boolean isMangledNameLeftAssoc(Name name) {
      return  ("" + name).startsWith("OP$$L$") ? true : false;
  }

}




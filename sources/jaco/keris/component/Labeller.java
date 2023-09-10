//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    Free name administration
//                           
//  [XTrans.java (51763) 18-May-01 12:15 -> 22-Jun-01 23:13]

package jaco.keris.component;

import jaco.framework.*;
import java.util.*;


public class Labeller extends Component {
    
    protected NamePool pool = new NamePool();
    
    public String getName() {
        return "Labeller";
    }
    
    public Name newLabel() {
        return pool.getNew("_lbl");
    }
    
    public Name newTemporary() {
        return pool.getNew("_tmp");
    }
    
    public Name newId() {
        return pool.getNew("_id");
    }
    
    public NamePool newNamePool() {
    	return new NamePool();
    }
	
	public static class NamePool {
		private static final Integer ONE = new Integer(1);
		
		HashMap pool = new HashMap();
		
		public Name getNew() {
			return getNew("tmp");
		}
		
		public Name getNew(String prefix) {
			Integer num = (Integer)pool.get(prefix);
			if (num == null) {
				pool.put(prefix, ONE);
				return Name.fromString(prefix + "$0");
			} else {
				pool.put(prefix, new Integer(num.intValue() + 1));
				return Name.fromString(prefix + "$" + num);
			}
		}
		
		public Name getNew(Name prefix) {
			return getNew(prefix.toString());
		}
	}
}

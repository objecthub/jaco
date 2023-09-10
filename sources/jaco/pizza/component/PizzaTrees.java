//      /   _ _      JaCo
//  \  //\ / / \     - adapted tree functions
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.pizza.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.pizza.struct.*;
import jaco.framework.*;
import java.util.*;


public class PizzaTrees extends Trees {
    Mangler mangler;
    
/** maps tree definition nodes to meta data strings
 */
    public HashMap metaData;
    
    public void setMetaData(Tree tree, Object data) {
        if (data != null)
            metaData.put(tree, data);
    }
    
    public Tree setMetaData(Tree oldt, Tree newt) {
        String data = (String)metaData.get(oldt);
        if (data != null)
            metaData.put(newt, data);
        else
        	switch (oldt) {
        		case VarDecl(_, _, _, _, Definition.VarDef vdef):
        			if ((vdef != null) && (((VDef)vdef).metaData != null))
        				metaData.put(newt, ((VDef)vdef).metaData);
        }
        return newt;
    }
    
    public String getMetaData(Tree tree) {
        return (String)metaData.get(tree);
    }
    
/** definition -> Qualified Base tree
 */
    public Tree QualidBase(CDef c) {
        return QualidThis(c.baseClass);
    }
    
    public Tree QualidThis(Definition def) {
        if ((def.owner == null) || (def.owner.name.length() == 0))
            return IdentThis(def);
        else
            return SelectThis(QualidThis(def.owner), def);
    }
    
    public Tree SelectThis(Tree tree, Definition def) {
        return newdef.Select(tree, mangler.unmangleShort(def.name, def.fullname))
                     .setType(def.type)
                     .setDef(def);
    }

    public Tree IdentThis(Definition def) {
        return newdef.Ident(mangler.unmangleShort(def.name, def.fullname))
                     .setType(def.type)
                     .setDef(def);
    }
    
    public String getName() {
        return "PizzaTrees";
    }
    
    public void init(MainContext context) {
        super.init(context);
        mangler = context.Mangler();
        make = new PizzaTreeFactory();
        newdef = new PizzaTreeCreate();
        redef = new PizzaTreeRedef();
        metaData = new HashMap();
    }
}

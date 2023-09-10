//      /   _ _      JaCo
//  \  //\ / / \     - adapted tree functions
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.stat.struct.*;
import jaco.framework.*;


public class PizzaTrees extends Trees {
    Mangler mangler;
    
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
    }
}

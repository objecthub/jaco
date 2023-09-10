//      /   _ _      JaCo
//  \  //\ / / \     - extended class reader
//   \//  \\_\_/     
//         \         Matthias Zenger, 22/02/00

package jaco.sjava.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.sjava.context.*;
import jaco.sjava.struct.*;
import java.io.IOException;
import java.util.Hashtable;


public class SJClassReader extends ClassReader implements SJAttributeConst, SJModifierConst  {
    
    Hashtable activeMethods;
    
    public String getName() {
        return "SJClassReader";
    }

    public Definition readField()
    {
        Definition res = super.readField();
        if ((res.kind == VAR) &&
            (res.owner.kind == TYP) &&
            ((res.modifiers & (PUBLIC | STATIC | FINAL)) ==
                (PUBLIC | STATIC | FINAL))) {
            if (((res.owner.modifiers & ACTIVE) == 0) &&
                (res.name.equals(TransSJ.ACTMETHCOUNT))) {
                activeMethods.put(res.owner, new Integer(res.type.tconst().intValue()));
                activeMethods.put(res.owner.fullname, new Integer(res.type.tconst().intValue()));
            }
        }
        return res;
    }
    
    protected Definition readClass(Definition c)
    {
        Definition res = super.readClass(c);
        if (((res.modifiers & ACTIVE) == 0) &&
            (activeMethods.get(res) != null)) {
            res.modifiers |= ACTIVE;
            Definition md = res.locals().elems;
            int i = 0;
            while ((md != null) && (md.scope != null)) {
                if ((md.kind == FUN) &&
                    ((md.modifiers & STATIC) == 0) &&
                    !md.name.toString().endsWith("$SJID") &&
                    !md.name.equals(TransSJ.AO))
                    md.modifiers |= ACTIVE;
                md = md.sibling;
            }
        }
        return res;
    }
    
    public void init(MainContext context) {
        super.init(context);
        classAttr |= JACO_ACTIVE_ATTR;
        methodAttr |= JACO_ACTIVE_ATTR;
        activeMethods = ((SJMainContext)context).activeMethods;
    }
    
    protected AttributeReader AttributeReader() {
        return new SJAttributeReader(this);
    }
}

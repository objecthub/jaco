//      /   _ _      JaCo
//  \  //\ / / \     - 
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.context;

import jaco.framework.*;
import jaco.stat.component.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class PizzaSemanticContext extends SemanticContext
{
    public PizzaSemanticContext(PizzaCompilerContext context)
    {
        super(context);
    }
    
    public TypeChecker TypeChecker()
    {
        if (checks == null)
        {
            checks = new PizzaTypeChecker();
            checks.init(this);
        }
        return checks;
    }
    
    public EnterClasses EnterClasses()
    {
        if (classes == null)
        {
            classes = new PizzaEnterClasses();
            classes.init(this);
        }
        return classes;
    }
    
    public ImportClasses ImportClasses()
    {
        if (imports == null)
        {
            imports = new PizzaImportClasses();
            imports.init(this);
        }
        return imports;
    }
    
    public EnterMembers EnterMembers()
    {
        if (members == null)
        {
            members = new PizzaEnterMembers();
            members.init(this);
        }
        return members;
    }
    
    public Attribute Attribute()
    {
        if (attribute == null)
        {
            attribute = new PizzaAttribute();
            attribute.init(this);
        }
        return attribute;
    }
}

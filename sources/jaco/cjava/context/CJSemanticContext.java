//  /   _ _      JaCo
//  \  //\ / / \     - the semantic analyzer context
//   \//  \\_\_/     
//     \         Matthias Zenger, 11/05/99

package jaco.cjava.context;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.cjava.component.*;


public class CJSemanticContext extends jaco.java.context.SemanticContext
{
/** context constructor
 */
    public CJSemanticContext(CJCompilerContext context)
    {
    super(context);
    }

    public TypeChecker TypeChecker()
    {
    if (checks == null)
    {
        checks = new CJTypeChecker();
        checks.init(this);
    }
    return checks;
    }

    public Attribute Attribute()
    {
    if (attribute == null)
    {
        attribute = new CJAttribute();
        attribute.init(this);
    }
    return attribute;
    }

    public EnterMembers EnterMembers()
    {
    if (members == null)
    {
        members = new CJEnterMembers();
        members.init(this);
    }
    return members;
    }

    public EnterClasses EnterClasses()
    {
    if (classes == null)
    {
        classes = new CJEnterClasses();
        classes.init(this);
    }
    return classes;
    }
    


/** factory methods for all components
 
    
    public Accountant Accountant()
    {
    if (accountant == null)
    {
        accountant = new Accountant();
        accountant.init(this);
    }
    return accountant;
    }
    
    public ImportClasses ImportClasses()
    {
    if (imports == null)
    {
        imports = new ImportClasses();
        imports.init(this);
    }
    return imports;
    }
    
    
*/
}

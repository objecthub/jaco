//      /   _ _      JaCo
//  \  //\ / / \     - the semantic analyzer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/05/99

package jaco.wrapper.context;

import jaco.framework.*;
import jaco.wrapper.component.*;


public class WrapperSemanticContext extends jaco.java.context.SemanticContext
{
/** context constructor
 */
    public WrapperSemanticContext(WrapperCompilerContext context)
    {
        super(context);
    }

/** factory methods for all components
 
    public TypeChecker TypeChecker()
    {
        if (checks == null)
        {
            checks = new TypeChecker();
            checks.init(this);
        }
        return checks;
    }
    
    public Accountant Accountant()
    {
        if (accountant == null)
        {
            accountant = new Accountant();
            accountant.init(this);
        }
        return accountant;
    }
    
    public EnterClasses EnterClasses()
    {
        if (classes == null)
        {
            classes = new EnterClasses();
            classes.init(this);
        }
        return classes;
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
    
    public EnterMembers EnterMembers()
    {
        if (members == null)
        {
            members = new EnterMembers();
            members.init(this);
        }
        return members;
    }
    
    public Attribute Attribute()
    {
        if (attribute == null)
        {
            attribute = new Attribute();
            attribute.init(this);
        }
        return attribute;
    }
*/
}

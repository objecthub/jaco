//      /   _ _      JaCo
//  \  //\ / / \     - the semantic analyzer context
//   \//  \\_\_/     
//         \         Matthias Zenger, 16/10/00

package jaco.java.context;

import jaco.framework.*;
import jaco.java.component.*;


public class SemanticContext extends Context
{
/** enclosing context
 */
    public CompilerContext      compilerContext;

/** tool components of the Java semantic analyzer
 */
    protected TypeChecker       checks;
    protected Accountant        accountant;

/** tree processors of the Java semantic analyzer
 */
    protected EnterClasses      classes;
    protected ImportClasses     imports;
    protected EnterMembers      members;
    protected Attribute         attribute;


/** context constructor
 */
    public SemanticContext(CompilerContext context)
    {
        super(context);
        compilerContext = context;
    }

/** factory methods for all components
 */
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
}

package jaco.jjava.context;

import jaco.framework.*;
import jaco.jjava.component.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class JSemanticContext extends SemanticContext
{
    public JSemanticContext(JCompilerContext context)
    {
        super(context);
    }
    /*
    //commented
    public TypeChecker TypeChecker()
    {
        if (checks == null)
        {
            checks = new PizzaTypeChecker();
            checks.init(this);
        }
        return checks;
    }
    //commented
    public EnterClasses EnterClasses()
    {
        if (classes == null)
        {
            classes = new PizzaEnterClasses();
            classes.init(this);
        }
        return classes;
    }
    //commented
    public ImportClasses ImportClasses()
    {
        if (imports == null)
        {
            imports = new PizzaImportClasses();
            imports.init(this);
        }
        return imports;
    }
    */

    public EnterMembers EnterMembers()
    {
        if (members == null)
        {
            members = new JEnterMembers();
            members.init(this);
        }
        return members;
    }
    
    public Attribute Attribute()
    {
        if (attribute == null)
        {
            attribute = new JAttribute();
            attribute.init(this);
        }
        return attribute;
    }
}

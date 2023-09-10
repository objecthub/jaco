//     _    _    Keris -- modular, extensible Java programming
//  |/(_|)|(     (c) 2001 Matthias Zenger
//  |\(_|\|_)    
//               semantic analyzer context

package jaco.keris.context;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.component.*;
import jaco.java.context.*;


public class XSemanticContext extends SemanticContext {
    
    public XSemanticContext(XCompilerContext context) {
        super(context);
    }

    public Accountant Accountant() {
        if (accountant == null) {
            accountant = new XAccountant();
            accountant.init(this);
        }
        return accountant;
    }
    
    public TypeChecker TypeChecker() {
        if (checks == null) {
            checks = new XTypeChecker();
            checks.init(this);
        }
        return checks;
    }
    
    public EnterClasses EnterClasses() {
        if (classes == null) {
            classes = new XEnterClasses();
            classes.init(this);
        }
        return classes;
    }
    
    public ImportClasses ImportClasses() {
        if (imports == null) {
            imports = new XImportClasses();
            imports.init(this);
        }
        return imports;
    }
    
    public XEnterModules EnterModules() {
        XEnterModules entermod = new XEnterModules();
        entermod.init(this);
        return entermod;
    }
    
    public EnterMembers EnterMembers() {
        if (members == null) {
            members = new XEnterMembers();
            members.init(this);
        }
        return members;
    }
    
    public Attribute Attribute() {
        if (attribute == null) {
            attribute = new XAttribute();
            attribute.init(this);
        }
        return attribute;
    }
}

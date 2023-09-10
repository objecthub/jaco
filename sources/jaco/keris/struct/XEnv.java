//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    environments
//                           
//  [XEnv.java (1734) 2-Apr-01 02:22 -> 2-Apr-01 01:37]

package jaco.keris.struct;

import jaco.java.struct.*;
import jaco.framework.*;
import jaco.java.component.*;
import java.util.Vector;
import Tree.*;


public class XContextEnv extends ContextEnv {

    public XContextEnv(Tree tree) {
        this(tree, new XContextInfo());
    }
    
    public XContextEnv(Tree tree, ContextInfo info) {
        super(tree, info);
    }
    
    public XContextEnv(Tree tree, ContextInfo info, Scope scope) {
        super(tree, info, scope);
    }
    
    public XContextEnv(Tree tree, Scope scope) {
        super(tree, scope);
    }
    
    public ContextEnv dup(Tree tree, ContextInfo info) {
        return (ContextEnv)copyTo(new XContextEnv(tree, info));
    }
    
    public ContextEnv dup(Tree tree, ContextInfo info, Scope scope) {
        return (ContextEnv)copyTo(new XContextEnv(tree, info, scope));
    }
}

public class XContextInfo extends ContextInfo {
    public Type moduleType;
    
    XContextInfo() {
    }
    
    XContextInfo(Scope scope) {
        super(scope);
    }
    
    public ContextInfo dup() {
        XContextInfo info = new XContextInfo();
        info.scope = scope;
        info.reported = reported;
        info.isStatic = isStatic;
        info.isSelfCall = isSelfCall;
        info.isSelfArgs = isSelfArgs;
        info.selectSuper = selectSuper;
        info.moduleType = moduleType;
        return info;
    }
}

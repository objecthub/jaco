//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    tree factory
//                           
//  [XTreeFactory.java (5333) 18-Apr-01 20:54 -> 18-Apr-01 01:36]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.keris.component.*;
import XTree.*;
import Definition.*;
import Tree.*;


public class XTreeFactory extends TreeFactory implements XModifierConst, XTree.Factory {
    
    public XTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits) {
        return new XTree.CaseDecl(name, mods, fields, inits, null);
    }

    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                          Tree[] members) {
        return new XTree.XClassDecl(name, mods, extending, implementing, new Tree[0], new Tree[0], null, members, null);
    }
    
    public XTree XClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                            Tree[] required, Tree[] included, Tree[] members) {
        return new XTree.XClassDecl(name, mods, extending, implementing, required, included, null, members, null);
    }
    
    public XTree Overrides(Tree overrides, Tree[] overridden) {
        return new XTree.Overrides(overrides, overridden);
    }
    
    public XTree ModuleFieldDecl(int mods, Tree impl, Tree[] intf) {
        return new XTree.ModuleFieldDecl(mods, impl, intf);
    }
    
    public XTree ClassFieldDecl(int mods, Name name, Tree ext, Tree[] intf,
                                Tree impl, Tree[] included, Tree[] members) {
        return new XTree.XClassDecl(name, mods | CLASSFIELD, ext, intf, new Tree[0], included, impl, members, null);
    }
    
    public XTree ModuleSelect(Tree from, Name selector) {
        return new XTree.ModuleSelect(from, selector, null);
    }
    
    public XTree CompoundType(Tree[] trees) {
        return new XTree.CompoundType(trees);
    }
    
    public XTree FragmentDecl(int mods, Name name, Tree[] provided,
                           Tree[] required, Tree[] included, Tree[] members) {
        return new XTree.XClassDecl(name, mods | FRAGMENT, null, provided, required, included, null, members, null);
    }
    
    public XTree Include(Tree fragment, Tree[] args, Tree[] classes) {
        return new XTree.Include(fragment, args, classes);
    }
}

public class XTreeCreate extends TreeCreate implements XModifierConst, XTree.Factory {
    
    public XTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits) {
        XTree.CaseDecl t = new XTree.CaseDecl(name, mods, fields, inits, null);
        t.pos = protoPos;
        return t;
    }
    
    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                          Tree[] members) {
        Tree t = new XTree.XClassDecl(name, mods, extending, implementing, new Tree[0], new Tree[0], null, members, null);
        t.pos = protoPos;
        return t;
    }
    
    public XTree XClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                            Tree[] required, Tree[] included, Tree[] members) {
        XTree t = new XTree.XClassDecl(name, mods, extending, implementing, required, included, null, members, null);
        t.pos = protoPos;
        return t;
    }
    
    public XTree Overrides(Tree overrides, Tree[] overridden) {
        XTree.Overrides t =  new Overrides(overrides, overridden);
        t.pos = protoPos;
        return t;
    }

    public XTree ModuleFieldDecl(int mods, Tree impl, Tree[] intf) {
        XTree.ModuleFieldDecl t =  new ModuleFieldDecl(mods, impl, intf);
        t.pos = protoPos;
        return t;
    }
    
    public XTree ClassFieldDecl(int mods, Name name, Tree ext, Tree[] intf,
                                Tree impl, Tree[] included, Tree[] members) {
        XTree t = new XTree.XClassDecl(name, mods | CLASSFIELD, ext, intf, new Tree[0], included, impl, members, null);
        t.pos = protoPos;
        return t;
    }
    
    public XTree ModuleSelect(Tree from, Name selector) {
        XTree.ModuleSelect t =  new ModuleSelect(from, selector, null);
        t.pos = protoPos;
        return t;
    }
    
    public XTree CompoundType(Tree[] trees) {
        XTree.CompoundType t =  new XTree.CompoundType(trees);
        t.pos = protoPos;
        return t;
    }
    
    public XTree FragmentDecl(int mods, Name name, Tree[] provided,
                           Tree[] required, Tree[] included, Tree[] members) {
        XTree t = new XTree.XClassDecl(name, mods | FRAGMENT, null, provided, required, included, null, members, null);
        t.pos = protoPos;
        return t;
    }
    
    public XTree Include(Tree fragment, Tree[] args, Tree[] classes) {
        XTree t = new XTree.Include(fragment, args, classes);
        t.pos = protoPos;
        return t;
    }
}

public class XTreeRedef extends TreeRedef implements XModifierConst, XTree.Factory {
    
    public XTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits) {
        XTree.CaseDecl t = (XTree.CaseDecl)protoTree;
        t = new CaseDecl(name, mods, fields, inits, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
        
    public Tree ClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                          Tree[] members) {
        XTree.XClassDecl t = (XTree.XClassDecl)protoTree;
        t = new XTree.XClassDecl(name, mods, extending, implementing, new Tree[0], new Tree[0], null, members, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree XClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                            Tree[] required, Tree[] included, Tree[] members) {
        XTree.XClassDecl t = (XTree.XClassDecl)protoTree;
        t = new XTree.XClassDecl(name, mods, extending, implementing, required, included, null, members, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree Overrides(Tree overrides, Tree[] overridden) {
        XTree.Overrides t = new Overrides(overrides, overridden);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree ModuleFieldDecl(int mods, Tree impl, Tree[] intf)  {
        XTree.ModuleFieldDecl t = (XTree.ModuleFieldDecl)protoTree;
        t =  new ModuleFieldDecl(mods, impl, intf);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }

    public XTree ClassFieldDecl(int mods, Name name, Tree ext, Tree[] intf,
                                Tree impl, Tree[] included, Tree[] members) {
        XTree.XClassDecl t = (XTree.XClassDecl)protoTree;
        t = new XTree.XClassDecl(name, mods | CLASSFIELD, ext, intf, new Tree[0], included, impl, members, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree ModuleSelect(Tree from, Name selector) {
        XTree.ModuleSelect t = (XTree.ModuleSelect)protoTree;
        t = new ModuleSelect(from, selector, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree CompoundType(Tree[] trees) {
        XTree.CompoundType t = new XTree.CompoundType(trees);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree FragmentDecl(int mods, Name name, Tree[] provided,
                           Tree[] required, Tree[] included, Tree[] members) {
        XTree.XClassDecl t = (XTree.XClassDecl)protoTree;
        t = new XTree.XClassDecl(name, mods | FRAGMENT, null, provided, required, included, null, members, t.def);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
    
    public XTree Include(Tree fragment, Tree[] args, Tree[] classes) {
        XTree.Include t = (XTree.Include)protoTree;
        t = new XTree.Include(fragment, args, classes);
        t.pos = protoTree.pos;
        t.type = protoTree.type;
        return t;
    }
}

//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    tree representation
//                           
//  [XTree.java (2445) 19-Apr-01 17:24 -> 22-Jun-01 01:36]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.java.struct.*;
import Definition.*;


/** abstract syntax trees for Keris
 */
public class XTree extends Tree {

    /** algebraic type constructor
     */     
    public case CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits, ClassDef def);
    
    /** module field declaration
     */
    public case ModuleFieldDecl(int mods, Tree impl, Tree[] intf);
    
    /** overrides clause
     */
    public case Overrides(Tree overriding, Tree[] overridden);
    
    /** module select operator
     */
    public case ModuleSelect(Tree from, Name selector, Definition def);
    
    /** compound types
     */
    public case CompoundType(Tree[] fields);
    
    /** include instruction
     */
    public case Include(Tree fragment, Tree[] args, Tree[] classes);
    
    /** class declarations
     */
    public static class XClassDecl extends ClassDecl {
        public Tree[] required;
        public Tree[] included;
        public Tree superimpl;
        
        public XClassDecl(Name name, int mods, Tree extending,
                          Tree[] implementing, Tree[] required,
                          Tree[] included, Tree superimpl,
                          Tree[] members, ClassDef def) {
            super(name, mods, extending, implementing, members, def);
            this.required = required;
            this.included = included;
            this.superimpl = superimpl;
        }
    }
    
    public Tree setDef(Definition def) {
        switch (this) {
            case ModuleSelect(_, _, _):
                ((ModuleSelect)this).def = def;
                break;
            case CaseDecl(_, _, _, _, _):
                ((CaseDecl)this).def = (ClassDef)def;
                break;
            default:
                return super.setDef(def);
        }
        if ((def != null) && (type == null))
            type = def.type;
        return this;
    }
    
    public Definition def() {
        switch (this) {
            case ModuleSelect(_, _, Definition def):
                return def;
            case CaseDecl(_, _, _, _, ClassDef def):
                return def;
            default:
                return super.def();
        }
    }
    
    public int mods() {
        switch (this) {
            case ModuleFieldDecl(int mods, _, _):
                return mods;
            case CaseDecl(_, int mods, _, _, _):
                return mods;
            default:
                return 0;
        }
    }
    
    public static interface Factory extends Tree.Factory {
        XTree CaseDecl(Name name, int mods, VarDecl[] fields, Tree[] inits);
        XTree ModuleFieldDecl(int mods, Tree impl, Tree[] intf);
        XTree ClassFieldDecl(int mods, Name name, Tree ext, Tree[] intf,
                             Tree impl, Tree[] includes, Tree[] members);
        XTree Overrides(Tree overriding, Tree[] overridden);
        XTree ModuleSelect(Tree from, Name selector);
        XTree XClassDecl(Name name, int mods, Tree extending, Tree[] implementing,
                         Tree[] required, Tree[] included, Tree[] members);
        XTree CompoundType(Tree[] fields);
        XTree FragmentDecl(int mods, Name name, Tree[] provided,
                           Tree[] required, Tree[] included, Tree[] members);
        XTree Include(Tree fragment, Tree[] args, Tree[] classes);
    }
}

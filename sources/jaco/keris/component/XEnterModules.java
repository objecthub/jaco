//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    enter modules pass
//                           
//  [XEnterModules.java (20149) 8-May-01 16:06 -> 8-Jul-01 23:35]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.keris.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.keris.context.*;
import java.util.*;
import Type.*;
import Tree.*;
import XTree.*;
import Definition.*;
import XDefinition.*;


public class XEnterModules extends Processor
                           implements XModifierConst, DefinitionConst,
                                      OperatorConst, TreeConst, TypeConst {
    /** helper components
     */
    protected Mangler       mangler;
    protected NameResolver  namer;
    protected ClassReader   reader;
    protected Accountant    accountant;
    protected XAttribute    attribute;
    
    /** language components
     */
    protected Modifiers     modifiers;
    protected Types         types;
    protected Definitions   definitions;
    protected Trees         trees;
    
    /** global intermediate data structures
     */
    protected Map modules = new HashMap();
    protected Map moduleinfo = new HashMap();
    protected Set current = new HashSet();
    protected Set done = new HashSet();
    
    /** delay import attribution
     */
    protected Map imports = new HashMap();
    protected Map starimports = new HashMap();
    
    /** component name
     */
    public String getName() {
        return "XEnterModules";
    }

    public String getDescription() {
        return "entering modules";
    }
    
    public String getDebugName() {
        return "entermod";
    }
    
    /** component initialization
     */
    public void init(XSemanticContext context) {
        super.init(context.compilerContext);
        MainContext mainContext = context.compilerContext.mainContext;
        mangler = mainContext.Mangler();
        reader = mainContext.ClassReader();
        namer = mainContext.NameResolver();
        modifiers = mainContext.Modifiers();
        types = mainContext.Types();
        definitions = mainContext.Definitions();
        trees = mainContext.Trees();
        accountant = context.Accountant();
        attribute = (XAttribute)context.Attribute();
    }
    
    protected boolean needsProcessing(CompilationEnv info) {
        return !info.attributed;
    }
    
    ////
    //// first sub-pass: attribute super modules and implemented module
    ////                 interfaces (do not visit members)
    ////
    
    public TreeList enter(TreeList treelist) throws AbortCompilation {
        TreeList res = super.enter(treelist);
        Tree[] ts = res.toArray();
        for (int i = 0; i < ts.length; i++)
            switch (ts[i]) {
                case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                    report.useEnv(info);
                    ContextEnv localEnv = accountant.ContextEnv(
                        (CompilationUnit)ts[i]);
                    for (int j = 0; j < decls.length; j++)
                        preAttribModuleDecls(decls[j], localEnv);
                    report.usePreviousEnv();
                    break;
                default:
                    throw new Error();
            }
        return res;
    }
    
    protected Tree preAttribModuleDecls(Tree tree, ContextEnv env) {
        switch (tree) {
            case ClassDecl(_, int mods, Tree extending, Tree[] implementing,
                           Tree[] defs, ClassDef def):
                if ((mods & MODULE) == 0)
                    return tree;
                trees.pushPos(tree.pos);
                XClassDef c = (XClassDef)def;
                // attribute super type
                Type supertype = (extending == null) ?
                    types.objectType : attribModuleType(extending, env);
                c.setSupertype(supertype);
                // attribute implemented interfaces
                Type[] is = new Type[implementing.length];
                for (int i = 0; i < is.length; i++) {
                    is[i] = attribModuleType(implementing[i], env);
                    if ((is[i].tdef().modifiers & INTERFACE) == 0)
                        report.error(implementing[i].pos,
                            "expected module interface");
                }
                c.setInterfaces(is);
                // initialize module structure
                c.modIntf = ModuleIntf(c);
                c.modIntf.starImported = new TypeSet(types);
                trees.popPos();
        }
        return tree;
    }
    
    ////
    //// second sub-pass: visit modules and class fields
    ////                  for modules: * check cyclic module inheritance
    ////                               * attribute required modules
    ////                               * attribute contained modules
    ////                               * attribute imports
    ////                               * check correct overriding
    ////
    
    protected Tree process(CompilationUnit tree) throws AbortCompilation {
        ContextEnv localEnv = accountant.ContextEnv(tree);
        for (int i = 0; i < tree.decls.length; i++)
            attribModuleDecls(tree.decls[i], localEnv); 
        return tree;
    }
    
    public Tree attribModuleDecls(Tree tree, ContextEnv env) {
        switch (tree) {
            case ClassDecl(Name nnn, int mods, Tree extending,
                           Tree[] implementing, Tree[] defs, ClassDef def):
                XClassDef xdef = (XClassDef)def;
                XTree.XClassDecl cdecl = (XTree.XClassDecl)tree;
                // fix class field attribution
                if ((mods & CLASSFIELD) != 0) {
                    xdef.vcIntf = new VirtualClassIntf();
                    // attribute class field initialization
                    XClassDef sc = attribClassFieldImpl(
                        (XClassDef)def, cdecl.superimpl, env);
                    // do we initialize the field with an anonymous class
                    // definition?
                    if (defs != null) {
                        Definition owner = def.owner;
                        Definition enclClass = owner.enclClass();
                        Name shortname = mangler.unmangleShort(
                            def.name, def.fullname);
                        Name name = Name.fromString(shortname + "$Impl");
                        Name fullname = mangler.formFullName(name, enclClass);
                        accountant.compiled.put(fullname, fullname);
                        XClassDef c = (XClassDef)definitions.defineClass(fullname);
                        c.completer = null;
                        c.modifiers = def.modifiers & ~(CLASSFIELD | FINAL);
                        if ((owner.modifiers & STRICTFP) != 0)
                            c.modifiers |= STRICTFP;
                        c.owner = owner;
                        c.sourcefile = env.toplevel.info.source.getName();
                        c.setLocals(new Scope(null, c));
                        c.setSupertype(sc.type);
                        env.toplevel.def.locals().enterIfAbsent(c);
                        mangler.put(c.fullname, (ClassDef)owner.enclClass(),
                            name, c.modifiers);
                        ContextEnv localEnv = env.dup(tree, env.info, c.locals());
                        localEnv.enclClass = (ClassDecl)tree;
                        Definition proxy = c.proxy(name);
                        accountant.enterScope(env).enter(proxy);
                        // I guess the next line is wrong!
                        //c.locals().enter(c.proxy(shortname));
                        sc = c;
                        xdef.vcIntf.thisImpl = true;
                    }
                    // do we initialize the class field at all?
                    if ((cdecl.superimpl != null) || (defs != null))
                        xdef.vcIntf.withClass = sc;
                    return tree;
                // skip classes
                } else if ((mods & MODULE) == 0)
                    return tree;
                // enter module interface
                trees.pushPos(tree.pos);
                // check for cyclic module inheritance
                if (cyclicInheritance(def, new HashSet())) {
                    report.error(tree.pos, "cyclic.inheritance", def);
                    def.setSupertype(types.objectType);
                    def.setInterfaces(types.noTypes);
                }
                // register module for post-processing (third sub-pass)
                modules.put(def, new Integer(tree.pos));
                moduleinfo.put(def, env.toplevel.info);
                XClassDef c = (XClassDef)def;
                // attribute required clause
                Tree[] required = cdecl.required;
                for (int i = 0; i < required.length; i++)
                    switch (required[i]) {
                        case Overrides(Tree overriding, Tree[] overridden):
                            Type ltype = attribModuleType(overriding, env);
                            Type rtype = attribModuleType(overridden[0], env);
                            if (!types.subtype(ltype, rtype))
                                report.error(required[i].pos,
                                    "overriding module " + ltype +
                                    " does not refine or implement " + rtype);
                            else if (!c.modIntf.requires(
                                (XClassDef)ltype.tdef(), (XClassDef)rtype.tdef()))
                                report.error(required[i].pos,
                                    "illegal overriding in requires clause");
                            break;
                        default:
                            Type mtype = attribModuleType(required[i], env);
                            if (!c.modIntf.requires((XClassDef)mtype.tdef()))
                                report.error(required[i].pos,
                                    "illegal required module");
                    }
                // attribute submodules and imports
                //namer.fixupScope(tree.pos, c);
                ContextEnv localEnv = env.dup(tree, env.info, new Scope(null, c));
                localEnv.enclClass = (ClassDecl)tree;
                localEnv.outer = env;
                localEnv.info.isStatic = false;
                //localEnv.printscopes();//DEBUG
                //System.out.println("============");//DEBUG
                for (int i = 0; i < defs.length; i++)
                    switch (defs[i]) {
                        case ModuleFieldDecl(int m, Tree impl, Tree[] intf):
                            switch (impl) {
                                case Overrides(Tree overriding, Tree[] overridden):
                                    Type ltype = attribModuleType(overriding, env);
                                    if (((ltype.tdef().modifiers & (ABSTRACT | INTERFACE)) != 0) &&
                                        ((c.modifiers & (ABSTRACT | INTERFACE)) == 0))
                                        report.error(defs[i].pos, "submodules of non-abstract modules have to be concrete");
                                    XClassDef[] ovs = new XClassDef[overridden.length];
                                    for (int k = 0; k < overridden.length; k++) {
                                        Type rtype = attribModuleType(overridden[k], env);
                                        ovs[k] = (XClassDef)rtype.tdef();
                                        if (!types.subtype(ltype, rtype))
                                            report.error(defs[i].pos, "overriding submodule " +
                                                ltype + " does not inherit or implement " + rtype);
                                    }
                                    if (!c.modIntf.contains((XClassDef)ltype.tdef(), ovs, m))
                                        report.error(defs[i].pos, "illegal overriding");
                                    break;
                                default:
                                    Type mtype = attribModuleType(impl, env);
                                    if (((mtype.tdef().modifiers & (ABSTRACT | INTERFACE)) != 0) &&
                                        ((c.modifiers & (ABSTRACT | INTERFACE)) == 0))
                                        report.error(defs[i].pos, "submodules of non-abstract modules have to be concrete");
                                    if ((intf == null) || (intf.length == 0)) {
                                        if (!c.modIntf.contains((XClassDef)mtype.tdef(), m))
                                            report.error(defs[i].pos, "illegal submodule declaration");
                                    } else {
                                        for (int j = 0; j < intf.length; j++) 
                                            switch (intf[j]) {
                                                case Overrides(Tree riding, Tree[] ridden):
                                                    Type ltype = attribModuleType(riding, env);
                                                    Type rtype = attribModuleType(ridden[0], env);
                                                    if (!((XClassDef)mtype.tdef()).refinementOf((XClassDef)ltype.tdef()))
                                                        report.error(defs[i].pos, mtype +   
                                                            " does not implement module " + ltype);
                                                    if (!types.subtype(ltype, rtype))
                                                        report.error(intf[j].pos, "overriding submodule " + ltype +
                                                            " does not refine or implement " + rtype);
                                                    if (!c.modIntf.contains((XClassDef)ltype.tdef(),
                                                                            (XClassDef)rtype.tdef(),
                                                                            m))
                                                        report.error(intf[j].pos, "illegal overriding");
                                                    break;
                                                default:
                                                    Type itype = attribModuleType(intf[j], env);
                                                    if (!((XClassDef)mtype.tdef()).refinementOf((XClassDef)itype.tdef()))
                                                        report.error(defs[i].pos, mtype + " does not implement module " + itype);
                                                    if (!c.modIntf.contains((XClassDef)itype.tdef(), m))
                                                        report.error(intf[j].pos, "illegal submodule declaration");
                                            }
                                        
                                    }
                            }
                            break;
                        case Import(int m, Tree rhs):
                        	if (m == XTreeConst.IMPORTMODULE)
                            	imports.put(rhs, localEnv);
                            else
                            	starimports.put(rhs, localEnv);
                            break;
                        case ClassDecl(_, int fmods, _, _, _, _):
                            if ((fmods & CLASSFIELD) != 0)
                                attribModuleDecls(defs[i], localEnv);
                    }
                trees.popPos();
        }
        return tree;
    }
    
    public boolean cyclicInheritance(Definition c, Set set) {
        // check if we have c already in the set
        if (set.contains(c))
            return true;
        set.add(c);
        // check for cyclic interface inheritance
        if ((c.modifiers & INTERFACE) != 0) {
            Type[] intfs = c.interfaces();
            for (int i = 0; i < intfs.length; i++)
                if (cyclicInheritance(intfs[i].tdef(), set))
                    return true;
        // check for cyclic implementation inheritance
        } else if (c.supertype() != null)
            return cyclicInheritance(c.supertype().tdef(), set);
        return false;
    }
    
    /** attribute the implementation of a class field
     */
    public XClassDef attribClassFieldImpl(XClassDef c, Tree tree, ContextEnv env) {
        XClassDef sc = (XClassDef)types.objectType.tdef();
        if (tree != null) {
            if ((c.modifiers & OPAQUE) != 0)
                report.error(tree.pos,
                    "abstract class fields must not define a concrete implementation");
            else
                sc = (XClassDef)attribute.attribClassType(tree, env).tdef();
        }
        if ((sc.modifiers & MODULE) != 0) {
            report.error(tree.pos, "class fields cannot refer to modules");
            return (XClassDef)types.objectType.tdef();
        }
        return sc;
    }
    
    ////
    //// third sub-pass: inherit required and submodules, check consistency
    ////
    
    public TreeList exit(TreeList treelist) throws AbortCompilation {
        TreeList res = super.exit(treelist);
        XClassDef[] cs = (XClassDef[])modules.keySet().toArray(
            new XClassDef[modules.size()]);
        //int[] ps = new int[cs.length];
        CompilationEnv[] is = new CompilationEnv[cs.length];
        // restore line numbers and compilation environments;
        // inherit requires clauses
        for (int i = 0; i < cs.length; i++) {
            //ps[i] = ((Number)modules.get(cs[i])).intValue();
            is[i] = (CompilationEnv)moduleinfo.get(cs[i]);
            report.useEnv(is[i]);
            inherit(cs[i]);
            report.usePreviousEnv();
        }
        // check submodules
        /* for (int i = 0; i < cs.length; i++) {
            report.useEnv(is[i]);
            inheritContained(cs[i]);
            report.usePreviousEnv();
        } */
        // attribute imports
        Tree[] ts = (Tree[])imports.keySet().toArray(
            new Tree[imports.size()]);
        for (int i = 0; i < ts.length; i++) {
            ContextEnv env = (ContextEnv)imports.get(ts[i]);
            Type mtype = ((XAttribute)attribute).attribFullModuleType(ts[i], env);
            //System.out.println("import " + mtype);
            if (!((XClassDef)env.enclClass.def).modIntf.imports((XClassDef)mtype.tdef(), mtype))
                report.error(ts[i].pos, "illegal import of " + mtype);
        }
        ts = (Tree[])starimports.keySet().toArray(
            new Tree[starimports.size()]);
        for (int i = 0; i < ts.length; i++) {
            ContextEnv env = (ContextEnv)starimports.get(ts[i]);
            Type mtype = ((XAttribute)attribute).attribFullModuleType(ts[i], env);
            //System.out.println("import " + mtype);
            if (!((XClassDef)env.enclClass.def).modIntf.starimports(mtype, ts[i]))
                report.error(ts[i].pos, "illegal import " + mtype + ".*");
        }
        imports.clear();
        starimports.clear();
        return res;
    }
    
    protected void inherit(XClassDef c) {
        // check if we have a module c to attribute here
        if (modules.get(c) == null)
            return;
        int pos = ((Number)modules.get(c)).intValue();
        // check for recursive refinements
        if (current.contains(c))
            report.error(pos, "recursive refinement of module " + c);
        // proceed for pending modules
        else if (!done.contains(c)) {
            current.add(c);
            // check super module
            XClassDef s = (XClassDef)c.supertype().tdef();
            if ((s.modifiers & MODULE) != 0) {
                inherit(s);
                inherit(pos, c, s);
            }
            // check implemented module interfaces
            Type[] intfs = c.interfaces();
            for (int i = 0; i < intfs.length; i++) {
                XClassDef t = (XClassDef)intfs[i].tdef();
                if ((t.modifiers & MODULE) != 0) {
                    inherit(t);
                    inherit(pos, c, t);
                }
            }
            // check overridden modules
            XClassDef[] all = c.modIntf.overriddenContained.keys();
            outer:
            for (int i = 0; i < all.length; i++) {
                if (s.modIntf.contained.contains(all[i]))
                    continue outer;
                for (int j = 0; j < intfs.length; j++) {
                    XClassDef t = (XClassDef)intfs[j].tdef();
                    if (((t.modifiers & MODULE) != 0) &&
                        t.modIntf.contained.contains(all[i]))
                        continue outer;
                }
                report.error(pos, "overridden submodule " + all[i] +
                             " is not declared in super module");
            }
            // we are done
            current.remove(c);
            done.add(c);
        }
    }
    
    protected void inherit(int pos, XClassDef c, XClassDef s) {
        //System.out.println("inherit " + s + " to " + c);
        // get all submodules of super module
        XClassDef[] all = s.modIntf.contained.keys();
        // inherit submodules if not overridden
        for (int i = 0; i < all.length; i++) {
            int mods = s.modIntf.modsOfContained(all[i]);
            if ((mods & PRIVATE) != 0)
                continue;
            else if (c.refinementOf(all[i]))
                report.error(pos, "module " + c +
                             " is more specific than inherited submodule " +
                             all[i] + " from " + s);
            else if (all[i].refinementOf(c))
                report.error(pos, "inherited submodule " + all[i] + " from " +
                             s + " is more specific than module " + c);
            else if (!c.modIntf.contained.contains(all[i]) &&
                     !c.modIntf.overriddenContained.contains(all[i]) &&
                     !c.modIntf.contains(all[i], mods))
                report.error(pos, "cannot inherit submodule " + all[i] +
                             " from " + s + " to " + c);
        }
        // get all modules required from super module
        all = s.modIntf.required.keys();
        for (int i = 0; i < all.length; i++)
            if (!c.modIntf.required.contains(all[i]) &&
                !c.modIntf.overriddenRequired.contains(all[i]) &&
                !c.modIntf.requires(all[i]))
                report.error(pos, "cannot inherit required module " +
                             all[i] + " from " + s + " to " + c);
    }
    
    /** module interface factory method
     */
    public ModuleIntf ModuleIntf(XClassDef c) {
        return c.modIntf = new ModuleIntf(c);
    }
    
    /** attribute a module type
     */
    public Type attribModuleType(Tree tree, ContextEnv env) {
        return ((XAttribute)attribute).attribModuleType(tree, env);
    }
}

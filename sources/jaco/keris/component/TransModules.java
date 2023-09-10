//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    Keris to Java translator
//                           
//  [XTrans.java (51763) 18-May-01 12:15 -> 22-Jun-01 23:13]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.keris.context.*;
import jaco.keris.struct.*;
import java.util.*;
import Tree.*;
import XTree.*;
import Definition.*;


public interface TransModulesConst {
    Name ACCESS_N = Name.fromString("$access");
    Name INIT_N = Name.fromString("$init");
    Name IMPORT_N = Name.fromString("$import");
    Name ENCL_N = Name.fromString("$encl");
    Name PROP_N = Name.fromString("$prop");
    Name SELF_N = Name.fromString("self$0");
    Name CONFIGURATOR_N = Name.fromString("$Configurator");
    Name PROPAGATOR_N = Name.fromString("$Propagator");
    Name CE_N = Name.fromString("$c");
    Name TIME_N = Name.fromString("time");
    Name CONFIGURE_N = Name.fromString("$configure");
    Name ARGS_N = Name.fromString("args");
    Name MAIN_N = Name.fromString("main");
    Name DMAIN_N = Name.fromString("$main");
    Name SUB_N = Name.fromString("$sub");
    Name SUPER_N = Name.fromString("$super");
    Name FACTORY_N = Name.fromString("$Factory");
    Name NEW_N = Name.fromString("new$");
    Name CAST_N = Name.fromString("cast$");
    Name INSTANCEOF_N = Name.fromString("instanceof$");
    Name CREATE_N = Name.fromString("$create$");
    Name IMPLSUF_N = Name.fromString("$Impl");
    Name ENCLMODULE_N = Name.fromString("encl$Module");
    Name MAIN_MESSAGE_N = Name.fromString("module not executable");
    Name CASTEXCEPTION_N = Name.fromString("java.lang.ClassCastException");
    Name ERROR_EXC_N = Name.fromString("java.lang.Error");
    Name TOSTRING_N = Name.fromString("toString");
    Name SUBSTRING_N = Name.fromString("substring");
    Name GETPROPERTY_N = Name.fromString("System.getProperty");
    Name SETPROPERTY_N = Name.fromString("System.setProperty");
    Name TIMEMILLIS_N = Name.fromString("System.currentTimeMillis");
    Name PRINTLN_N = Name.fromString("System.out.println");
    Name MESSAGE_N = Name.fromString("message");
    Name PRINTSTATUS_N = Name.fromString("print$status");
}

public class TransModules extends Translator
                          implements XModifierConst, OperatorConst,
                                     TransModulesConst, TreeConst,
                                     DefinitionConst, TypeConst,
                                     AlgebraicSupportConst {
    /** the context
     */
    protected XMainContext mainContext;
    
    /** other components
     */
    protected NameResolver namer;
    protected Mangler mangler;
    protected XTypes types;
    protected XTrees trees;
    protected XDefinitions definitions;
    protected AlgebraicSupport algebraic;
    protected Labeller labeller;
    protected Constants consts;
    
    protected Type stringArrayParam;
    protected Tree[] constructorMethods = new Tree[0];
    
    protected TreeList setupCode;
    
    protected TreeList moduleAddOns;
    
    protected Set modsAndViews = new HashSet();
    
    protected boolean lhsflag;
    
    protected Labeller.NamePool freeNames;
    
    /** component name
     */
    public String getName() {
        return "TransModules";
    }
    
    /** return descrition of tree processor
     */
    public String getDescription() {
        return "keris translation";
    }
    
    /** default getDebugName method; returns an invalid debug name
     */
    public String getDebugName() {
        return "xtrans";
    }
    
    /** component initialization
     */
    public void init(XTransContext context) {
        super.init(context.compilerContext);
        mainContext = (XMainContext)context.compilerContext.mainContext;
        namer = mainContext.NameResolver();
        mangler = mainContext.Mangler();
        definitions = (XDefinitions)mainContext.Definitions();
        types = (XTypes)mainContext.Types();
        trees = (XTrees)mainContext.Trees();
        algebraic = context.AlgebraicSupport();
        labeller = context.Labeller();
        consts = mainContext.Constants();
        stringArrayParam = types.make.ArrayType(types.stringType);
    }
    
    /** enter code
     */
    public TreeList enter(TreeList treelist) throws AbortCompilation {
        return super.enter(treelist);
    }
    
    /** correct modifiers
     */
    public TreeList exit(TreeList treelist)  throws AbortCompilation {
        TreeList res = super.exit(treelist);
        mainContext.algebraicClasses = false;
        definitions.swapScopes();
        XClassDef[] cs = new XClassDef[modsAndViews.size()];
        modsAndViews.toArray(cs);
        for (int i = 0; i < cs.length; i++) {
            if ((cs[i].modifiers & CLASSFIELD) != 0)
                cs[i].modifiers = (cs[i].modifiers & ~CLASSFIELD) | INTERFACE;
            else if ((cs[i].modifiers & MODULE) != 0)
                cs[i].modifiers &= ~MODULE;
            cs[i].modifiers &= 0xFFFF;
        }
        return res;
    }
    
//
// Handle module definitions
//
    
    /** return a tree for printing an indented message
     */
    public Tree printStatus(String str) {
        return newdef.Exec(
                newdef.Apply(
                    newdef.Ident(PRINTSTATUS_N),
                    new Tree[]{trees.stringLit(str)}));
    }
    
    /** translate declarations
     */
    protected Tree translateDecl(Tree tree, Env env) {
        switch ((XTree)tree) {
            case ClassDecl(Name name, int mods, Tree extending,
                           Tree[] implementing, Tree[] members,
                           ClassDef c):
                XClassDef xc = (XClassDef)c;
                // translate module interfaces
                if (((mods & MODULE) != 0) && ((mods & INTERFACE) != 0)) {
                    freeNames = labeller.newNamePool();
                    modsAndViews.add(c);
                    TreeList fields = new TreeList();
                    setupCode = null;
                    moduleAddOns = new TreeList();
                    createModuleFields(xc, fields, null);
                    return trees.at(tree).make(
                        trees.Container(new Tree[]{
                            redef.ClassDecl(
                                name, mods & ~MODULE,
                                specialTransType(extending, env),
                                //trees.append(
                                //    createModuleFields(xc, fields),
                                    specialTransTypes(implementing, env), //),
                                trees.append(
                                    trees.append(
                                        trees.append(
                                            fields.toArray(),
                                            new Tree[]{
                                                newdef.MethodDecl(
                                                    ACCESS_N, PUBLIC, // | SYNTHETIC,
                                                    trees.classTree(c),
                                                    new VarDecl[0],
                                                    trees.noTrees, null),
                                                newdef.MethodDecl(
                                                    CONFIGURE_N, PUBLIC, // | SYNTHETIC,
                                                    trees.classTree(c),
                                                    new VarDecl[0],
                                                    trees.noTrees, null)
                                            }),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env))),
                                    moduleAddOns.toArray())),
                            moduleAccessorInterface(xc)}));
                // translate modules
                } else if ((mods & MODULE) != 0) {
                    freeNames = labeller.newNamePool();
                    modsAndViews.add(c);
                    moduleAddOns = new TreeList();
                    // new setup code
                    setupCode = new TreeList();
                    // add message output
                    setupCode.append(printStatus("configure "));
                    setupCode.append(trees.incrIndentTree());
                    // initialize self first
                    setupCode.append(
                        trees.at(tree.pos).make(
                            newdef.Exec(
                                newdef.Assign(
                                    newdef.Select(trees.This(), SELF_N),
                                    trees.This()))));
                    // create module prelude
                    TreeList fields = new TreeList(trees.at(tree.pos).make(modulePrelude(xc)));
                    TreeList initCode = new TreeList();
                    createModuleFields(xc, fields, initCode);
                    return trees.at(tree).make(
                        trees.Container(new Tree[]{
                            redef.ClassDecl(
                                name, mods & ~MODULE,
                                specialTransType(extending, env),
                                //trees.append(
                                //    createModuleFields(xc, fields),
                                    specialTransTypes(implementing, env), //),
                                trees.append(
                                    trees.append(
                                        fields.append(propagatorClass(xc))
                                              .append(configuratorClass(xc)).toArray(),
                                        trees.append(
                                            transDecls(
                                                normalizeDecls((XClassDef)c, members, classEnv((ClassDecl)tree, env), initCode),
                                                classEnv((ClassDecl)tree, env)),
                                            moduleInitMethods((XClassDef)c, initCode.toArray()))),
                                    moduleAddOns.toArray())),
                            moduleAccessorInterface((XClassDef)c)
                        }));
                // translate class fields
                } else if ((mods & CLASSFIELD) != 0) {
                    modsAndViews.add(c);
                    TreeList classfieldCode = new TreeList();
                    // add factories to module
                    addClassfieldSupport(tree.pos, xc, classfieldCode);
                    // add new compound interface to module
                    Tree[] ext = trees.at(tree.pos).make(specialTransTypes(implementing, env));
                    if ((xc.vcIntf != null) && (xc.vcIntf.overrides != null)) {
                        Tree[] newext = new Tree[ext.length + xc.vcIntf.overrides.length];
                        System.arraycopy(ext, 0, newext, xc.vcIntf.overrides.length, ext.length);
                        for (int i = 0; i < xc.vcIntf.overrides.length; i++)
                            newext[i] = trees.at(tree.pos).make(trees.classTree(xc.vcIntf.overrides[i]));
                        ext = newext;
                    }
                    classfieldCode.append(
                        trees.at(tree).make(
                            redef.ClassDecl(
                                name, mods & ~CLASSFIELD | INTERFACE,
                                null, ext,
                                trees.noTrees)));
                    // add new implementation class to module
                    if ((xc.vcIntf.withClass != null)) // && xc.vcIntf.thisImpl)
                        classfieldCode.append(trees.at(tree.pos).make(
                                classfieldRepresentation(xc, transDecls(members,
                                                classEnv((ClassDecl)tree, env)))));
                    return trees.Container(trees.at(tree.pos).make(classfieldCode.toArray()));
                // translate algebraic classes
                } else if (definitions.isAlgebraicClass(c)) {
                    return trees.at(tree).make(
                        redef.ClassDecl(name, mods & ~(ALGEBRAIC | CASEDEF),
                                    specialTransType(extending, env),
                                    specialTransTypes(implementing, env),
                                    trees.append(
                                        algebraic.genRuntimeMembers(moduleAddOns, (XClassDef)c),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env)))));
                // translate regular classes
                } else {
                    return trees.at(tree).make(
                        redef.ClassDecl(name, mods,
                                        specialTransType(extending, env),
                                        specialTransTypes(implementing, env),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env))));
                }
            case ModuleFieldDecl(int mods, XTree.Overrides(Tree m, Tree[] n), Tree[] intf):
                XClassDef c = (XClassDef)m.def();
                return ((env.enclClass.def.modifiers & INTERFACE) == 0) ?
                            trees.Container(creatorMethods(c, intf)) :
                            trees.Container(trees.noTrees);
            case ModuleFieldDecl(int mods, Tree m, Tree[] intf):
                XClassDef c = (XClassDef)m.def();
                return ((env.enclClass.def.modifiers & INTERFACE) == 0) ?
                            trees.Container(creatorMethods(c, intf)) :
                            trees.Container(trees.noTrees);
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, MethodDef def):
                // translate main methods in modules
                if (((def.owner.modifiers & MODULE) != 0) &&
                    name.equals(MAIN_N)) {
                    if (stats != null) {
                        stats = transStats(stats, methodEnv((MethodDecl)tree, env));
                        stats = trees.append(algebraic.temporaryDecls(), stats);
                    }
                    Tree res = trees.at(tree).make(
                        redef.MethodDecl(DMAIN_N, mods,
                                        transType(restype, env),
                                        transVarDecls(params, env),
                                        transTypes(thrown, env),
                                        stats));
                        return res;
                // ignore constructors in interfaces
                } else if (((def.owner.modifiers & INTERFACE) != 0) &&
                           name.equals(PredefConst.INIT_N)) {
                    return trees.Container(trees.noTrees);
                // ignore existing constructors in modules
                } else if (((def.owner.modifiers & MODULE) != 0) &&
                           name.equals(PredefConst.INIT_N)) {
                    return trees.Container(trees.noTrees);
                // translate the rest
                } else {
                    return super.translateDecl(tree, env);
                }
            case PackageDecl(Tree qualid):
                return trees.at(tree).make(redef.PackageDecl(trees.copy(qualid)));
            case Import(int tag, Tree qualid):
                if ((tag == IMPORTSTAR) &&
                    definitions.isAlgebraicSubclass(qualid.type.tdef())) {
                    TreeList    imports = new TreeList();
                    Type        type = qualid.type;
                    while (types.isAlgebraicType(type)) {
                        imports.append(
                            trees.at(tree.pos).make(
                                newdef.Import(tag, trees.toTree(type))));
                        type = type.supertype();
                    }
                    return trees.Container(imports.toArray());
                } else if ((qualid.type.tdef().modifiers & MODULE) != 0) {
                    return trees.Container(trees.noTrees);
                } else
                    return trees.at(tree).make(
                            redef.Import(tag, trees.copy(qualid)));
        }
        return super.translateDecl(tree, env);
    }
    
    /** create a module factory method for module c
     */
    protected Tree creatorMethod(XClassDef c, XClassDef init, Name temp) {
        Tree ret = newdef.Return(
                    newdef.NewObj(
                        null, trees.classTree(init),
                        new Tree[]{newdef.Ident(PROP_N)}, null));
        return newdef.MethodDecl(
            moduleCreatorMethodName(c),
            PROTECTED, // | SYNTHETIC,
            trees.classTree(c),
            new VarDecl[0],
            trees.noTrees,
            (temp == null) ?
                new Tree[]{
                    newdef.Return(
                        newdef.NewObj(
                            null, trees.classTree(init),
                            new Tree[]{newdef.Ident(PROP_N)}, null))} :
                new Tree[]{
                        newdef.If(
                            newdef.Binop(EQ, newdef.Ident(temp), trees.Null()),
                            newdef.Return(newdef.Assign(
                                newdef.Ident(temp),
                                newdef.NewObj(null, trees.classTree(init),
                                    new Tree[]{newdef.Ident(PROP_N)}, null))),
                            newdef.Return(newdef.Ident(temp))
                            )
                });
    }
    
    /** create a module creation method for module c
     */
    protected Tree[] creatorMethods(XClassDef c, Tree[] intfs) {
        if ((intfs == null) || (intfs.length == 0))
            return new Tree[]{creatorMethod(c, c, null)};
        Tree[] res = new Tree[intfs.length + 1];
        Name temp = freeNames.getNew();
        res[0] = newdef.VarDecl(temp, PRIVATE, trees.classTree(c), null);
        for (int i = 0; i < intfs.length; i++)
            res[i + 1] = creatorMethod((XClassDef)intfs[i].type.tdef(), c, temp);
        return res;
    }
    
    /** create a module accessor interface for module c
     */
    protected Tree moduleAccessorInterface(XClassDef c) {
        return newdef.ClassDecl(
            moduleAccessorInterfaceName(c),
            (c.modifiers & ~MODULE) | INTERFACE, // | SYNTHETIC,
            null,
            moduleAccessorExtensions(c, true),
            new Tree[]{
                newdef.MethodDecl(
                    moduleAccessorMethodName(c),
                    PUBLIC | ABSTRACT, // | SYNTHETIC,
                    trees.classTree(c),
                    new VarDecl[0],
                    trees.noTrees,
                    null)
            });
    }
    
    /** create the field $encl and a module constructor
     */
    protected Tree[] modulePrelude(XClassDef c) {
        Tree[] body;
        if ((c.supertype().tdef().modifiers & MODULE) != 0)
            return new Tree[]{
                newdef.VarDecl(PROP_N, PRIVATE, newdef.Ident(PROPAGATOR_N),
                    newdef.NewObj(null, newdef.Ident(PROPAGATOR_N), trees.noTrees, null)),
                newdef.MethodDecl(PredefConst.INIT_N,
                    PUBLIC, // | SYNTHETIC,
                    null,
                    new VarDecl[]{(VarDecl)
                        newdef.VarDecl(ENCL_N, 0, trees.toTree(types.objectType), null)},
                    trees.noTrees,
                    new Tree[]{newdef.Exec(
                        newdef.Apply(newdef.Self(null, SUPER),
                                     new Tree[]{newdef.Ident(ENCL_N)}))
                    })};
        else
            return new Tree[]{
                newdef.VarDecl(PROP_N, PRIVATE, newdef.Ident(PROPAGATOR_N),
                    newdef.NewObj(null, newdef.Ident(PROPAGATOR_N), trees.noTrees, null)),
                newdef.VarDecl(ENCL_N, PUBLIC, trees.toTree(types.objectType), null),
                newdef.MethodDecl(PredefConst.INIT_N,
                    PUBLIC, // | SYNTHETIC,
                    null,
                    new VarDecl[]{(VarDecl)
                        newdef.VarDecl(ENCL_N, 0, trees.toTree(types.objectType), null)},
                    trees.noTrees,
                    new Tree[]{
                        newdef.Exec(
                            newdef.Apply(newdef.Self(null, SUPER), trees.noTrees)),
                        trees.FieldInit(ENCL_N),
                        printStatus("create ")
                    })};
    }
    
    /** create a method of the connector class
     */
    protected Tree connectorMethod(XClassDef c, boolean init, Name res) {
        Tree[] body = new Tree[init ? 2 : 1];
        int i = 0;
        if (init)
            body[i++] = newdef.If(
                newdef.Binop(EQ, newdef.Ident(res), trees.Null()),
                newdef.Exec(newdef.Assign(
                    newdef.Ident(res),
                    newdef.NewObj(null, trees.classTree(c),
                        new Tree[]{newdef.Ident(SELF_N)}, null))),
                null);
        body[i] = newdef.Return(newdef.Ident(res));
        return newdef.MethodDecl(
            moduleAccessorMethodName(c),
            PUBLIC, // | SYNTHETIC,
            trees.classTree(c),
            new VarDecl[0],
            trees.noTrees,
            body);
    }
    
    /** create a propagator class
     */
    protected Tree propagatorClass(XClassDef c) {
        XClassDef s = (XClassDef)c.supertype().tdef();
        // create superclass
        Tree t = trees.toTree(types.objectType);
        // create body of class
        TreeList body = new TreeList();
        Set set = new HashSet();
        // map self
        XClassDef[] subs = c.subsumes();
        for (int j = 0; j < subs.length; j++)
            body.append(connectorMethod(subs[j], false, SELF_N));
        // map required from this
        XClassDef[] cs = c.modIntf.required.keys();
        for (int i = 0; i < cs.length; i++) {
            subs = cs[i].subsumes();
            Name name = moduleFieldName(cs[i]);
            for (int j = 0; j < subs.length; j++)
                body.append(connectorMethod(subs[j], false, name));
        }
        // map contained from this
        cs = c.modIntf.contained.keys();
        for (int i = 0; i < cs.length; i++) {
            subs = cs[i].subsumes();
            Name name = moduleFieldName(cs[i]);
            for (int j = 0; j < subs.length; j++)
                body.append(connectorMethod(subs[j], false, name));
        }
        // insert a toString method
        body.append(
            newdef.MethodDecl(
                TOSTRING_N,
                PUBLIC, // | SYNTHETIC,
                trees.toTree(types.stringType),
                new VarDecl[0],
                trees.noTrees,
                new Tree[]{
                    newdef.If(
                        newdef.Binop(EQ,
                            newdef.Ident(ENCL_N),
                            trees.Null()),
                        newdef.Return(trees.stringLit(c.name.toString())),
                        newdef.Return(
                            newdef.Binop(PLUS,
                                newdef.Ident(ENCL_N),
                                trees.stringLit("." + c.name))))
                }));
        // create connector class
        return newdef.ClassDecl(
            PROPAGATOR_N, PUBLIC, t,
            supportedModuleAccessors((XClassDef)c),
            body.toArray());
    }
    
    /** create a configurator class
     */
    protected Tree configuratorClass(XClassDef c) {
        XClassDef s = (XClassDef)c.supertype().tdef();
        // create superclass
        Tree t = ((s.modifiers & MODULE) != 0)
            ? newdef.Select(trees.classTree(s), CONFIGURATOR_N)
            : trees.toTree(types.objectType);
        // create body of class
        TreeList body = new TreeList();
        Set set = new HashSet();
        // map self
        XClassDef[] subs = ((c.modifiers & SPECIALIZES) != 0) ?
            new XClassDef[]{s} : new XClassDef[0];
        subs = c.subsumesAdditional(subs);
        for (int j = 0; j < subs.length; j++) {
            body.append(connectorMethod(subs[j], false, SELF_N));
            set.add(subs[j]);
        }
        XClassDef[] cs;
        if ((s.modifiers & MODULE) != 0) {
            // map required from super
            cs = c.modIntf.required.keys();
            for (int i = 0; i < cs.length; i++) {
                subs = ((ModuleSet)c.modIntf.required.get(cs[i])).keys();
                subs = cs[i].subsumesAdditional(subs);
                Name name = moduleFieldName(cs[i]);
                for (int j = 0; j < subs.length; j++)
                    if (s.modIntf.provides(subs[j]) &&
                        !set.contains(subs[j])) {
                        body.append(connectorMethod(subs[j], false, name));
                        set.add(subs[j]);
                    }
            }
            // map contained from super
            cs = c.modIntf.contained.keys();
            for (int i = 0; i < cs.length; i++) {
                subs = ((ModuleSet)c.modIntf.contained.get(cs[i])).keys();
                subs = cs[i].subsumesAdditional(subs);
                Name name = moduleFieldName(cs[i]);
                for (int j = 0; j < subs.length; j++)
                    if (s.modIntf.provides(subs[j]) &&
                        !set.contains(subs[j])) {
                        body.append(connectorMethod(subs[j], false, name));
                        set.add(subs[j]);
                    }
            }
        }
        // map required from this
        cs = c.modIntf.required.keys();
        for (int i = 0; i < cs.length; i++) {
            subs = ((ModuleSet)c.modIntf.required.get(cs[i])).keys();
            subs = cs[i].subsumesAdditional(subs);
            Name name = moduleFieldName(cs[i]);
            for (int j = 0; j < subs.length; j++)
                if (!set.contains(subs[j]))
                    body.append(connectorMethod(subs[j], false, name));
        }
        // map contained from this
        cs = c.modIntf.contained.keys();
        for (int i = 0; i < cs.length; i++) {
            subs = ((ModuleSet)c.modIntf.contained.get(cs[i])).keys();
            subs = cs[i].subsumesAdditional(subs);
            Name name = moduleFieldName(cs[i]);
            for (int j = 0; j < subs.length; j++)
                if (!set.contains(subs[j]))
                    body.append(connectorMethod(subs[j], false, name));
        }
        // create connector class
        return newdef.ClassDecl(
            CONFIGURATOR_N, PUBLIC, t,
            supportedModuleAccessors((XClassDef)c),
            body.toArray());
    }
    
    /** deal with module accessor methods
     */
    protected Tree[] moduleAccessorExtensions(XClassDef c, boolean excludeSelf) {
        XClassDef[] subs = c.subsumes();
        Tree[] ts = new Tree[subs.length - (excludeSelf ? 1 : 0)];
        int j = 0;
        for (int i = 0; i < subs.length; i++)
            if (!excludeSelf || (subs[i] != c))
                ts[j++] = moduleAccessorInterfaceTree(subs[i]);
        return ts;
    }
    
    /** deal with module accessor methods
     */
    protected Tree[] supportedModuleAccessors(XClassDef c) {
    	TreeList intfs = new TreeList(moduleAccessorInterfaceTree(c));
        XClassDef[] cs = c.modIntf.required.keys();
        for (int i = 0; i < cs.length; i++)
        	intfs.append(moduleAccessorInterfaceTree(cs[i]));
        cs = c.modIntf.contained.keys();
        for (int i = 0; i < cs.length; i++)
            intfs.append(moduleAccessorInterfaceTree(cs[i]));
        return intfs.toArray();
    }
    
    protected Name moduleAccessorInterfaceName(XClassDef c) {
        return Name.fromString("$" + c.name);
    }
    
    protected Name moduleAccessorMethodName(XClassDef c) {
        return Name.fromString("$" + c.fullname.replace((byte)'.', (byte)'_'));
    }
    
    protected Name moduleAccessorMethodName(XClassDef mod, XClassDef c) {
        return Name.fromString(mod.fullname.replace((byte)'.', (byte)'_') + "$" + c.fullname.replace((byte)'.', (byte)'_'));
    }
    
    protected Name moduleCreatorMethodName(XClassDef c) {
        return Name.fromString(CREATE_N + "" + c.fullname.replace((byte)'.', (byte)'_'));
    }
    
    protected Tree moduleAccessorInterfaceTree(XClassDef c) {
        if ((c.owner == null) ||
            (c.owner.fullname == null) ||
            (c.owner.fullname.length() == 0))
            return newdef.Ident(moduleAccessorInterfaceName(c));
        else
            return newdef.Select(
                trees.Qualid(c.owner.fullname),
                moduleAccessorInterfaceName(c));
    }

    /** assign a field name to every contained module
     */
    Map moduleFields;
    Map syncModuleFields;
    Map subsumedBy;
    
    protected Tree[] createModuleFields(XClassDef c, TreeList fields, TreeList initCode) {
        XClassDef[] csubs = c.subsumes();
        TreeList intfs = new TreeList();
        boolean isInterface = (c.modifiers & INTERFACE) != 0;
        // create a new module field name map
        moduleFields = new HashMap();
        syncModuleFields = new HashMap();
        subsumedBy = new HashMap();
        // enter required modules
        XClassDef[] cs = c.modIntf.required.keys();
        for (int i = 0; i < cs.length; i++) {
            XClassDef[] subs = cs[i].subsumes();
            Name name = freeNames.getNew("req");
            if (!isInterface) {
                fields.append(
                    newdef.VarDecl(name, 0/*PRIVATE*/, // | SYNTHETIC,
                        trees.classTree(cs[i]), null));
                setupCode.append(
                    newdef.Exec(
                        newdef.Assign(
                            newdef.Select(trees.This(), name),
                            newdef.Apply(
                                newdef.Select(
                                    trees.castTo(moduleAccessorInterfaceTree(cs[i]), ENCL_N),
                                    moduleAccessorMethodName(cs[i])),
                                trees.noTrees))));
            }
            for (int j = 0; j < subs.length; j++) {
                moduleFields.put(subs[j], name);
                subsumedBy.put(subs[j], cs[i]);
                /*
                    fields.append(
                        newdef.MethodDecl(
                            moduleAccessorMethodName(subs[j]),
                            PUBLIC, // | SYNTHETIC,
                            classTree(subs[j]),
                            new VarDecl[0], trees.noTrees,
                            isInterface ? null :
                                new Tree[]{newdef.Return(
                                    //newdef.Typeop(TYPECAST,
                                        newdef.Apply(
                                            newdef.Select(newdef.Ident(name), ACCESS_N),
                                            trees.noTrees)
                                    //  , classTree(subs[j]))
                                    )})); */
                intfs.append(moduleAccessorInterfaceTree(subs[j]));
            }
        }
        // enter contained modules
        cs = c.modIntf.contained.keys();
        for (int i = 0; i < cs.length; i++) {
            XClassDef[] subs = cs[i].subsumes();
            Name name = freeNames.getNew("sub");
            if (!isInterface) {
                fields.append(
                    newdef.VarDecl(name, 0/*PRIVATE*/, // | SYNTHETIC,
                        trees.classTree(cs[i]), null));
                setupCode.append(
                        newdef.Exec(
                            newdef.Assign(
                                newdef.Ident(name),
                                newdef.Apply(newdef.Ident(moduleCreatorMethodName(cs[i])),
                                             trees.noTrees))));
            }
            for (int j = 0; j < subs.length; j++) {
                moduleFields.put(subs[j], name);
                Integer mods = (Integer)c.modIntf.containedMods.get(subs[j]);
                if ((mods != null) &&
                    ((mods.intValue() & SYNCHRONIZED) != 0)) {
                	syncModuleFields.put(subs[j], subs[j]);
                }
                subsumedBy.put(subs[j], cs[i]);
                for (int k = 0; k < csubs.length; k++)
                    fields.append(
                        newdef.MethodDecl(
                            moduleAccessorMethodName(csubs[k], subs[j]),
                            PUBLIC, // | SYNTHETIC,
                            trees.classTree(subs[j]),
                            new VarDecl[0], trees.noTrees,
                            isInterface ? null :
                                new Tree[]{newdef.Return(
                                    // newdef.Typeop(TYPECAST,
                                    newdef.Select(
                                        newdef.Apply(
                                            newdef.Ident(CONFIGURE_N),
                                            trees.noTrees),
                                        name)
                                    //  , classTree(subs[j]))
                                )}));
                intfs.append(moduleAccessorInterfaceTree(subs[j]));
            }
        }
        // enter self
        if (!isInterface)
            fields.append(
                newdef.VarDecl(SELF_N, 0/*PRIVATE*/, // | SYNTHETIC,
                    trees.classTree(c), null));
        for (int j = 0; j < csubs.length; j++) {
            moduleFields.put(csubs[j], SELF_N);
            subsumedBy.put(csubs[j], c);
            /* fields.append(
                newdef.MethodDecl(
                    moduleAccessorMethodName(subs[j]),
                    PUBLIC, // | SYNTHETIC,
                    classTree(subs[j]),
                    new VarDecl[0], trees.noTrees,
                    isInterface ? null : new Tree[]{
                       */ /* newdef.If(
                            newdef.Binop(EQ, newdef.Ident(SELF_N), trees.Null()),
                            newdef.Block(0, new Tree[]{
                                newdef.Exec(newdef.Assign(
                                    newdef.Ident(SELF_N), trees.This())),
                                newdef.Exec(newdef.Apply(
                                    newdef.Ident(INIT_N), trees.noTrees))}),
                            null), */
                        // newdef.Return(newdef.Ident(SELF_N))}));
            intfs.append(moduleAccessorInterfaceTree(csubs[j]));
        }
        // enter imported modules
        cs = c.modIntf.imported.keys();
        for (int i = 0; i < cs.length; i++) {
            XClassDef[] subs = cs[i].subsumes();
            Name name = freeNames.getNew("imp");
            if (!isInterface) {
                fields.append(
                    newdef.VarDecl(
                        name,
                        PRIVATE, // | SYNTHETIC,
                        trees.classTree(cs[i]),
                        null));
                initCode.append(
                    newdef.Exec(
                        newdef.Assign(
                            newdef.Select(trees.This(), name),
                            selectModule((Type)c.modIntf.imported.get(cs[i])))));
            }
            for (int j = 0; j < subs.length; j++) {
                moduleFields.put(subs[j], name);
                subsumedBy.put(subs[j], cs[i]);
                intfs.append(moduleAccessorInterfaceTree(subs[j]));
            }
        }
        return intfs.toArray();
    }
    
    protected Name moduleFieldName(XClassDef c) {
        return (Name)moduleFields.get(c);
    }
    
    protected boolean isStaticModMember(Definition def) {
    	return (def.kind == VAR) &&
    		   def.type.isBasic() &&
               ((def.modifiers & FINAL) != 0) &&
               ((def.owner.modifiers & MODULE) != 0) &&
               (def.type.isTypeOfConstant() ||
                ((def.modifiers & STATIC) != 0));
   	}
   	
   	protected Tree refStaticModMember(Definition def) {
   		return newdef.Select(trees.ClassName(def.owner),
   		                     def.name);
   	}
    
    /** extract all module initializations and include them in a
     *  method $init(); return a modified list of declarations
     */
    protected Tree[] normalizeDecls(XClassDef c, Tree[] defs, Env env, TreeList initCode) {
        if ((c.supertype().tdef().modifiers & MODULE) != 0)
            initCode.append(
                newdef.Exec(
                    newdef.Apply(
                        newdef.Select(trees.Super(), INIT_N), trees.noTrees)));
       	// access synchronized modules
       	XClassDef[] cs = (XClassDef[])syncModuleFields.keySet().toArray(
       		new XClassDef[syncModuleFields.size()]);
       	for (int i = 0; i < cs.length; i++) {
       		initCode.append(
       			newdef.Exec(
       				newdef.Apply(
       					newdef.Select(selectModule(cs[i]), ACCESS_N),
       					trees.noTrees)));
       	}
        TreeList rest = new TreeList();
        for (int i = 0; i < defs.length; i++) {
            Tree def = defs[i];
            switch (defs[i]) {
                case Block(_, _):
                    initCode.append(transDecl(defs[i], env));
                    break;
                case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                    rest.append(defs[i]);
                    if ((init != null) &&
                        init.type.isTypeOfConstant() &&
                        ((mods & FINAL) != 0) &&
                        vartype.type.isBasic())
                        ((VarDecl)defs[i]).mods |= STATIC;
                    else if ((init != null) &&
                            (((mods & FINAL) == 0) || !init.type.isTypeOfConstant())) {
                        ((VarDecl)defs[i]).init = null;
                        ((VarDecl)defs[i]).mods = ((VarDecl)defs[i]).mods & ~FINAL;
                        if (init != null)
                            initCode.append(
                                trees.at(defs[i].pos).make(
                                    newdef.Exec(
                                        newdef.Assign(
                                            newdef.Ident(name), transExpr(init, env)))));
                    }
                    break;
                default:
                    rest.append(defs[i]);
            }
        }
        return rest.toArray();
    }
    
    /** create module initialization code for the connect method
     */
    protected void moduleInitOthers(TreeList code, XClassDef[] cs) {
        for (int i = 0; i < cs.length; i++) {
            Name name = moduleFieldName(cs[i]);
            code.append(
                newdef.Exec(
                    newdef.Assign(
                        newdef.Select(trees.This(), name),
                        newdef.Apply(
                            newdef.Select(
                                newdef.Ident(CE_N),
                                moduleAccessorMethodName(cs[i])),
                            trees.noTrees))));
        }
    }
    
    /** return implementations for the module initializer methods
     *  setup, connect and init
     */
    protected Tree[] moduleInitMethods(XClassDef c, Tree[] initCode) {
        // generate initialization of module fields
        TreeList code = new TreeList();
        // generate initialization of self
        code.append(
            newdef.Exec(
                newdef.Assign(
                    newdef.Select(trees.This(), SELF_N),
                    trees.This())));
        moduleInitOthers(code, c.modIntf.required.keys());
        moduleInitOthers(code, c.modIntf.contained.keys());
        // call import in super module
        if ((c.supertype().tdef().modifiers & MODULE) != 0) {
            // call connect to super module
            code.append(
                newdef.Exec(
                    newdef.Apply(
                        newdef.Select(
                            trees.Super(), CONFIGURE_N),
                        new Tree[]{
                            newdef.NewObj(null, newdef.Ident(CONFIGURATOR_N),
                                          trees.noTrees, null)})));
            // connect to super module
            setupCode.append(
                newdef.Exec(
                    newdef.Apply(
                        newdef.Select(trees.Super(), CONFIGURE_N),
                        new Tree[]{
                            newdef.NewObj(null, newdef.Ident(CONFIGURATOR_N),
                                          trees.noTrees, null)})));
        }
        //setupCode.append(newdef.Exec(newdef.Apply(newdef.Ident(INIT_N), trees.noTrees)));
        setupCode.append(trees.decrIndentTree());
        setupCode.append(newdef.Return(trees.This()));
        TreeList moduleInits = new TreeList();
        moduleInits.append(
            newdef.MethodDecl(
                ACCESS_N,
                SYNCHRONIZED | PUBLIC, // | SYNTHETIC,
                trees.classTree(c),
                new VarDecl[0],
                trees.noTrees,
                new Tree[]{
                    //newdef.Exec(
                    //  newdef.Apply(
                    //      trees.Qualid(Name.fromString("System.out.println")),
                    //      new Tree[]{trees.This()})),
                    newdef.If(
                        newdef.Binop(NE, newdef.Ident(PROP_N), trees.Null()),
                        newdef.Block(0, new Tree[]{
                            newdef.Exec(newdef.Apply(newdef.Ident(CONFIGURE_N), trees.noTrees)),
                            newdef.Exec(newdef.Assign(newdef.Ident(PROP_N), trees.Null())),
                            printStatus("initialize "),
                            trees.incrIndentTree(),
                            newdef.Exec(newdef.Apply(newdef.Ident(INIT_N), trees.noTrees)),
                            trees.decrIndentTree()
                        }),
                        null),
                    newdef.Return(trees.This())
                }));
        moduleInits.append(
            newdef.MethodDecl(
                CONFIGURE_N,
                SYNCHRONIZED | PUBLIC, // | SYNTHETIC,
                trees.classTree(c),
                new VarDecl[0],
                trees.noTrees,
                new Tree[]{
                    newdef.If(
                        newdef.Binop(EQ, newdef.Ident(SELF_N), trees.Null()),
                        newdef.Block(0, setupCode.toArray()),
                        newdef.Return(trees.This()))
                }));
        moduleInits.append(
            newdef.MethodDecl(
                CONFIGURE_N,
                PROTECTED, // | SYNTHETIC,
                newdef.BasicType(VOID),
                new VarDecl[]{
                    (VarDecl)newdef.VarDecl(CE_N, 0,
                        newdef.Ident(CONFIGURATOR_N), null)
                }, trees.noTrees,
                code.toArray()));
        moduleInits.append(
            newdef.MethodDecl(
                INIT_N,
                PROTECTED, // | SYNTHETIC,
                newdef.BasicType(VOID),
                new VarDecl[0], trees.noTrees, initCode));
        moduleInits.append(
            newdef.MethodDecl(
                PRINTSTATUS_N,
                PROTECTED, // | SYNTHETIC,
                newdef.BasicType(VOID),
                new VarDecl[]{
                    (VarDecl)newdef.VarDecl(MESSAGE_N, 0, trees.toTree(types.stringType), null)
                },
                trees.noTrees,
                new Tree[]{
                    newdef.If(
                        newdef.Binop(NE,
                            newdef.Apply(
                                trees.Qualid(GETPROPERTY_N),
                                new Tree[]{trees.stringLit("keris.verbose")}),
                            trees.Null()),
                        newdef.Exec(
                            newdef.Apply(
                                trees.Qualid(PRINTLN_N),
                                new Tree[]{
                                    newdef.Binop(PLUS,
                                        newdef.Binop(PLUS,
                                            newdef.Binop(PLUS,
                                                trees.stringLit("# "),
                                                newdef.Apply(
                                                    trees.Qualid(GETPROPERTY_N),
                                                    new Tree[]{trees.stringLit("keris.indent")})),
                                            newdef.Ident(MESSAGE_N)),
                                        newdef.If(
                                            newdef.Binop(EQ,
                                                newdef.Ident(ENCL_N),
                                                trees.Null()),
                                            trees.stringLit(c.name.toString()),
                                            newdef.Binop(PLUS,
                                                    newdef.Ident(ENCL_N),
                                                    trees.stringLit("." + c.name))))})),
                                            null)
                }));
        boolean mainGenerated = false;
        // check if we need a static main method
        if (c.modIntf.required.size() == 0) {
            Definition e = null;
            Definition current = c;
            outer: do {
                e = current.locals().lookup(MAIN_N);
                while (e.scope != null) {
                    if (namer.instantiatable(e.def, new Type[]{stringArrayParam}, c.type))
                        break outer;
                    e = e.next();
                }
                current = current.supertype().tdef();
            } while ((current.modifiers & MODULE) != 0);
            if ((e.scope != null) &&
                types.sametype(e.def.type.restype(), types.voidType)) {
                mainGenerated = true;
                moduleInits.append(
                    newdef.MethodDecl(
                        MAIN_N,
                        PUBLIC | STATIC,
                        trees.toTree(types.voidType),
                        new VarDecl[]{
                            (VarDecl)newdef.VarDecl(ARGS_N, 0, trees.toTree(stringArrayParam), null)},
                        trees.toTree(e.def.type.thrown()),
                        new Tree[]{
                            newdef.Exec(
                                newdef.Apply(
                                    trees.Qualid(SETPROPERTY_N),
                                    new Tree[]{
                                        trees.stringLit("keris.indent"),
                                        trees.stringLit("")})),
                            newdef.VarDecl(TIME_N, 0, trees.toTree(types.longType),
                                newdef.Apply(
                                    trees.Qualid(TIMEMILLIS_N), trees.noTrees)),
                            newdef.Exec(
                            newdef.Apply(
                                newdef.Select(
                                    newdef.Apply(
                                        newdef.Select(
                                            newdef.NewObj(null, trees.classTree(c),
                                                new Tree[]{trees.Null()}, null),
                                            ACCESS_N),
                                        trees.noTrees),
                                    DMAIN_N),
                                new Tree[]{newdef.Ident(ARGS_N)})),
                            newdef.If(
                                newdef.Binop(NE,
                                    newdef.Apply(
                                        trees.Qualid(GETPROPERTY_N),
                                        new Tree[]{trees.stringLit("keris.time")}),
                                    trees.Null()),
                                newdef.Exec(
                                    newdef.Apply(
                                        trees.Qualid(PRINTLN_N),
                                        new Tree[] {
                                            newdef.Binop(PLUS,
                                                newdef.Binop(PLUS,
                                                    trees.stringLit("# runtime: "),
                                                    newdef.Binop(MINUS,
                                                        newdef.Apply(
                                                            trees.Qualid(TIMEMILLIS_N),
                                                            trees.noTrees),
                                                        newdef.Ident(TIME_N))),
                                                trees.stringLit("ms"))
                                        })),
                                null)
                        }));
            }
        }
        if (!mainGenerated)
            moduleInits.append(
                newdef.MethodDecl(
                    MAIN_N,
                    PUBLIC | STATIC,
                    trees.toTree(types.voidType),
                    new VarDecl[]{
                        (VarDecl)newdef.VarDecl(ARGS_N, 0, trees.toTree(stringArrayParam), null)},
                    trees.noTrees,
                    new Tree[]{
                        newdef.Throw(newdef.NewObj(null,
                            trees.Qualid(ERROR_EXC_N),
                            new Tree[]{
                                newdef.Literal(consts.make.StringConst(MAIN_MESSAGE_N))
                            },
                            null))}));
        return moduleInits.toArray();
    }

//
// Handle class fields
//

    /** the factory method name for class field c
     */
    protected Name factoryMethodName(XClassDef c) {
        return Name.fromString("" + NEW_N + mangler.unmangleShort(c.name, c.fullname));
    }
    
    /** the cast method name for class field c
     */
    protected Name castMethodName(XClassDef c) {
        return Name.fromString("" + CAST_N + mangler.unmangleShort(c.name, c.fullname));
    }
    
    /** the instanceof method name for class field c
     */
    protected Name instanceofMethodName(XClassDef c) {
        return Name.fromString("" + INSTANCEOF_N + mangler.unmangleShort(c.name, c.fullname));
    }

    /** class field representation name
     */
    protected Name classfieldRepresentationName(XClassDef c) {
        return Name.fromString("" + mangler.unmangleShort(c.name, c.fullname) + IMPLSUF_N);
    }
    
/** find all constructors in class field c
 */
    protected Type[] findConstructors(XClassDef c) {
        Definition e = c.locals().lookup(PredefConst.INIT_N);
        TypeSet ts = new TypeSet(types);
        while (e.scope != null) {
            if ((e.def.owner == c) ||
                ((e.def.owner.modifiers & INTERFACE) != 0))
                ts.add(e.def.type);
            e = e.next();
        }
        return ts.types();
    }
    
/** find all (abstract) interface methods
 */
    protected FunTypeSet findInterfaceMethods(FunTypeSet ts, XClassDef c) {
        Type[] is = c.interfaces();
        for (int i = 0; i < is.length; i++) {
            XClassDef iface = (XClassDef)is[i].tdef();
            for (Definition e = iface.locals().elems; e != null; e = e.sibling)
                if ((e.def.kind == FUN) &&
                    ((e.def.modifiers & STATIC) == 0) &&
                    (e.def.name != PredefConst.INIT_N))
                    ts.add(e.def.name, e.def.type);
            findInterfaceMethods(ts, iface);
        }
        return ts;
    }
    
    protected void addClassfieldSupport(int pos, XClassDef c, TreeList defs) {
        // add factory methods
        Type[] constr = findConstructors(c);
        for (int i = 0; i < constr.length; i++) {
            Type[] pt = constr[i].argtypes();
            Type[] npt = new Type[pt.length - 1];
            System.arraycopy(pt, 1, npt, 0, npt.length);
            VarDecl[] params = trees.Params(npt);
            if (c.vcIntf.withClass != null) // FIX THIS;
            defs.append(trees.at(pos).make(
                newdef.MethodDecl(
                    factoryMethodName(c),
                    ((c.owner.modifiers & ABSTRACT) != 0) ?
                        PUBLIC | ABSTRACT /* | SYNTHETIC */ :
                        PUBLIC /* | SYNTHETIC */,
                    trees.objectType(), //classTree(c),
                    params,
                    trees.toTree(constr[i].thrown()),
                    (((c.owner.modifiers & INTERFACE) != 0) ||
                     ((c.modifiers & ABSTRACT) != 0)) ?
                        null : new Tree[]{
                        newdef.Return(
                            newdef.NewObj(
                                null,
                                //c.vcIntf.thisImpl ?
                                    newdef.Ident(classfieldRepresentationName(c)),
                                //  : trees.classTree(c.vcIntf.withClass),
                                trees.Idents(params),
                                null))
                    })));
        }
        // add a cast method
        if (c.vcIntf.withClass != null)
        defs.append(trees.at(pos).make(
            newdef.MethodDecl(
                castMethodName(c),
                ((c.owner.modifiers & ABSTRACT) != 0) ?
                    PUBLIC | ABSTRACT /* | SYNTHETIC */ :
                    PUBLIC /* | SYNTHETIC */,
                trees.objectType(),
                new VarDecl[]{
                    (VarDecl)newdef.VarDecl(CE_N, 0, trees.objectType(), null)},
                trees.noTrees,
                (((c.owner.modifiers & INTERFACE) != 0) ||
                 ((c.modifiers & ABSTRACT) != 0)) ?
                    null : new Tree[]{
                    newdef.If(
                        newdef.Binop(
                            OperatorConst.NE,
                            newdef.Apply(
                                newdef.Select(
                                    trees.castTo(
                                        newdef.Ident(classfieldRepresentationName(c)),
                                        CE_N),
                                    ENCLMODULE_N),
                                trees.noTrees),
                            selectModule((XClassDef)c.owner)),
                        newdef.Throw(
                            newdef.NewObj(
                                null,
                                trees.Qualid(CASTEXCEPTION_N),
                                trees.noTrees,
                                null)),
                        newdef.Return(newdef.Ident(CE_N)))
                    }
            )));
        // add an instanceof method
        if (c.vcIntf.withClass != null)
        defs.append(trees.at(pos).make(
            newdef.MethodDecl(
                instanceofMethodName(c),
                ((c.owner.modifiers & ABSTRACT) != 0) ?
                    PUBLIC | ABSTRACT /* | SYNTHETIC */ :
                    PUBLIC /* | SYNTHETIC */,
                trees.booleanType(),
                new VarDecl[]{
                    (VarDecl)newdef.VarDecl(CE_N, 0, trees.objectType(), null)},
                trees.noTrees,
                (((c.owner.modifiers & INTERFACE) != 0) ||
                 ((c.modifiers & ABSTRACT) != 0)) ?
                    null : new Tree[]{
                    newdef.Return(
                        newdef.Binop(
                            OperatorConst.AND,
                            newdef.Typeop(TYPETEST, newdef.Ident(CE_N),
                                newdef.Ident(classfieldRepresentationName(c))),
                            newdef.Binop(
                                OperatorConst.EQ,
                                newdef.Apply(
                                    newdef.Select(
                                        trees.castTo(
                                            newdef.Ident(classfieldRepresentationName(c)),
                                            CE_N),
                                        ENCLMODULE_N),
                                    trees.noTrees),
                                selectModule((XClassDef)c.owner)))
                    )}
            )));
    }
    
/** create a representation for a class field implementation
 */
    protected Tree classfieldRepresentation(XClassDef c, Tree[] defs) {
        TreeList members = new TreeList();
        Tree t;
        // add constructors (if not inherited)
        if (!c.vcIntf.thisImpl) {
            Type[] constr = findConstructors(c);
            for (int i = 0; i < constr.length; i++) {
                Type[] pt = constr[i].argtypes();
                Type[] npt = new Type[pt.length - 1];
                System.arraycopy(pt, 1, npt, 0, npt.length);
                VarDecl[] params = trees.Params(npt);
                Tree superQual = null;
                if ((c.vcIntf.withClass.owner.modifiers & MODULE) != 0)
                    superQual = selectModule(c.vcIntf.withClass.owner);
                members.append(newdef.MethodDecl(
                    PredefConst.INIT_N, PUBLIC, // | SYNTHETIC,
                    null, params, trees.toTree(constr[i].thrown()),
                    new Tree[]{
                        newdef.Exec(
                            newdef.Apply(newdef.Self(superQual, SUPER), trees.Idents(params)))
                    }));
            }
            //System.out.println(c + " is this_impl " + c.withClass);
            t = trees.classTree(c.vcIntf.withClass);
        } else {
            members.append(defs);
            //System.out.println(c + " is remote_impl " + c.withClass.supertype());
            t = trees.classTree(c.vcIntf.withClass.supertype().tdef());
        }
        // add a method for querying the enclosing module
        members.append(newdef.MethodDecl(
            ENCLMODULE_N,
            PUBLIC, // | SYNTHETIC,
            trees.classTree(c.owner),
            new VarDecl[0],
            trees.noTrees,
            new Tree[]{
                newdef.Return(selectModule(c.owner))
            }));
        return newdef.ClassDecl(
            classfieldRepresentationName(c), 0,
            t,
            new Tree[]{trees.classTree(c)},
            members.toArray());
    }
    
    protected Tree selectModule(Definition c, Tree encl, Env env) {
        switch (encl) {
            case ModuleSelect(Tree from, _, Definition def):
                if (def == null)
                    def = encl.type.tdef();
                return newdef.Apply(
                            newdef.Select(
                                trees.at(encl).make(selectModule(def, from, env)),
                                moduleAccessorMethodName((XClassDef)def, (XClassDef)c)),
                            trees.noTrees);
            default:
            	Name name = moduleFieldName((XClassDef)encl.type.tdef());
            	if (name.toString().startsWith("self"))
            		return newdef.Self(trees.Qualid(c.topLevelClass()), THIS);
                return newdef.Apply(
                            newdef.Select(
                                newdef.Ident(name),
                                moduleAccessorMethodName((XClassDef)encl.type.tdef(), (XClassDef)c)),
                            trees.noTrees);
        }
    }
    
/** access a module c from module expression encl with static type from
 
    protected Tree selectModule(Definition c, Definition from, Tree encl) {
        return newdef.Apply(
                    newdef.Select(
                        encl,
                        moduleAccessorMethodName((XClassDef)from, (XClassDef)c)),
                    trees.noTrees);
    }
 */
    
/** select a module from the top-level module
 */
    protected Tree selectModule(Definition c) {
    	Name name = moduleFieldName((XClassDef)c);
       	if (name.toString().startsWith("self"))
   			return newdef.Self(trees.Qualid(c.topLevelClass()), THIS);
        return newdef.Ident(moduleFieldName((XClassDef)c)).setDef(c);
    }
    
    protected Tree selectModule(Type t) {
        switch ((XType)t) {
            case ModuleType(Type outer):
                if (outer == null)
                    return selectModule(t.tdef());
                switch ((XType)outer) {
                    case ModuleType(_):
                        return newdef.Apply(
                                newdef.Select(
                                    selectModule(outer),
                                    moduleAccessorMethodName((XClassDef)outer.tdef(),
                                                             (XClassDef)t.tdef())),
                                trees.noTrees);
                    default:
                        return selectModule(t.tdef());
                }
            default:
                throw new Error();
        }
    }
    
/** access a module (which is refered to by 'tree')
 */
    protected Tree accessModule(Tree tree) {
    	switch (tree) {
    		case Self(_, _, _):
    			return tree;
    		case Ident(_, Definition def):
    			if ((def != null) &&
    			    (syncModuleFields.get(def) != null))
    				return tree;
    			else
    				return newdef.Apply(newdef.Select(tree, ACCESS_N), trees.noTrees);
    		default:
        		return newdef.Apply(newdef.Select(tree, ACCESS_N), trees.noTrees);
        }
    }
    
    
    
/** translate expressions (pre/post processing)
 */
/*    public Tree transExpr(Tree tree, Env env) {
        if (!lhsflag && (tree != null) &&
            (tree.type != null) &&
            ((tree.type.tdef().modifiers & CLASSFIELD) != 0)) {
            Tree typeIdent = classTree((XClassDef)tree.type.tdef());
            return newdef.Typeop(TYPECAST, super.transExpr(tree, env), typeIdent);
        } else {
            lhsflag = false;
            return super.transExpr(tree, env);
        }
    } */
    
/** translate expressions
 */
    protected Tree translateExpr(Tree tree, Env env) {
        switch ((XTree)tree) {
            case Select(Tree selected, Name selector, Definition def):
            	if (isStaticModMember(def))
            		return trees.at(tree.pos).make(
            			refStaticModMember(def));
                if (selector == PredefConst.CLASS_N)
                    return super.translateExpr(tree, env);
                else if ((def.modifiers & MODULE) != 0)
                    return trees.at(tree).make(accessModule(selectModule(def)));
                else if (((def.owner.modifiers & MODULE) != 0) && 
                         selector.equals(MAIN_N))
                    return trees.at(tree).make(
                        redef.Select(transExpr(selected, env), DMAIN_N));
                else // for now
                if (selector == PredefConst.CLASS_N)
                    return trees.at(tree).make(
                        redef.Select(selected, selector));
                else if ((def.kind == FUN) &&
                         ((selected.type.tdef().modifiers & CLASSFIELD) != 0))
                    switch (selected) {
                        case Self(Tree clazz, TreeConst.SUPER, _):
                            return trees.at(tree).make(
                                redef.Select(
                                    newdef.Select(
                                        newdef.Self(transType(clazz, env), TreeConst.THIS),
                                        SUPER_N),
                                    Name.fromString("$" + selector)));
                    }
                return trees.at(tree).make(
                        redef.Select(
                            trees.castExpr(
                                def.owner.type,
                                selected.type,
                                transExpr(selected, env)),
                            selector));
            case Ident(Name name, Definition def):
            	if (isStaticModMember(def))
            		return trees.at(tree.pos).make(
            			refStaticModMember(def));
                Tree qual = (Tree)trees.replacements.get(tree);
                if (qual != null) {
                	if (((def.modifiers & MODULE) == 0) &&
                	    ((def.owner.modifiers & MODULE) != 0) &&
                	    name.equals(MAIN_N))
                    	return trees.at(tree).make(
                        	newdef.Select(transExpr(qual, env), DMAIN_N));
                	else if ((def.modifiers & MODULE) == 0)
                		return trees.at(tree).make(
                        	newdef.Select(transExpr(qual, env), name));
                    else
                    	throw new InternalError("not yet implemented");
                }
                if ((def.modifiers & MODULE) != 0)
                    return trees.at(tree).make(accessModule(selectModule(def)));
                else if (((def.modifiers & MODULE) == 0) &&
                    ((def.owner.modifiers & MODULE) != 0) && 
                    name.equals(MAIN_N))
                    return trees.at(tree).make(
                        newdef.Select(accessModule(selectModule(def.owner)), DMAIN_N));
                else if ((def.owner.modifiers & MODULE) != 0) {
                	//System.out.println("IDENT: " + env.enclClass + " / " + name + ": " + def);
                	XClassDef topMod = (XClassDef)env.enclClass.def.topLevelClass();
               		if (topMod.extensionOf((XClassDef)def.owner)) {
               			return trees.at(tree).make(newdef.Ident(name));
               				// newdef.Select(trees.This(), name));
               		} else
                    	return trees.at(tree).make(
                        	newdef.Select(accessModule(selectModule(def.owner)), name));
                } else
                    return super.translateExpr(tree, env);
            case ModuleSelect(Tree from, _, Definition def):
                if (def == null)
                    def = tree.type.tdef();
                if ((def.modifiers & MODULE) == 0)
                    return transExpr(from, env);
                else
                    //return trees.at(tree).make(
                    //    accessModule(def, from.type.tdef(), transExpr(from, env)));
                    return trees.at(tree).make(
                            accessModule(selectModule(def, from, env)));
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def,
                        Definition constructor):
                XClassDef xc = (XClassDef)clazz.type.tdef();
                if ((xc.modifiers & CLASSFIELD) != 0) {
                    return trees.at(tree).make(
                        newdef.Apply(
                            newdef.Select(transExpr(encl, env), factoryMethodName(xc)),
                            transExprs(args, env)));
                } else
                    return super.translateExpr(tree, env);
            case Self(Tree clazz, int tag, _):
                if (tag == TreeConst.THIS) {
                    return trees.at(tree).make(
                            redef.Self(transType(clazz, env), tag));
                } else if ((tree.type.tdef().modifiers & CLASSFIELD) != 0) {
                    return trees.at(tree).make(
                            newdef.Select(
                                redef.Self(transType(clazz, env), TreeConst.THIS),
                                SUPER_N));
                } else {
                    return trees.at(tree).make(
                            redef.Self(transExpr(clazz, env), tag));
                }
            case Apply(Tree fn, Tree[] args):
           		Type[] atypes = new Type[args.length];
           		for (int i = 0; i < atypes.length; i++)
            		atypes[i] = args[i].type;
                switch (fn) {
                    case Self(Tree clazz, TreeConst.SUPER, _):
                        Type supertype = env.enclClass.def.supertype();
                        //System.out.println("constructor: " + fn.type + " of " + supertype);
                        if ((supertype.tdef().modifiers & CLASSFIELD) != 0) {
                            //System.out.println("  classfield!");
                            args = transExprs(args, env);
                            Tree[] newargs = new Tree[args.length + 1];
                            System.arraycopy(args, 0, newargs, 1, args.length);
                            newargs[0] = trees.This();
                            if ((supertype.outer().tdef().modifiers & INTERFACE) != 0)
                                return trees.at(tree).make(newdef.Apply(
                                    newdef.Self(null, TreeConst.SUPER),
                                    args));
                            else
                                return trees.at(tree).make(newdef.Apply(
                                    newdef.Self(
                                        accessModule(
                                            selectModule(supertype.outer())),
                                        TreeConst.SUPER),
                                    args));
                        }
                        if (definitions.isInnerclass(supertype.tdef())) {
                        	//System.out.println("constructor: " + fn.type + " of " + supertype);
                        	Type[] ftypes = fn.type.argtypes();
                        	Type[] gtypes = new Type[ftypes.length - 1];
                        	System.arraycopy(ftypes, 1, gtypes, 0, gtypes.length);
							return trees.at(tree).make(
								redef.Apply(
									transExpr(fn, env),
									trees.castExprs(gtypes, atypes, transExprs(args, env))));
                        }
                        
                }
                return trees.at(tree).make(
                    redef.Apply(
                        transExpr(fn, env),
                        trees.castExprs(fn.type.argtypes(), atypes, transExprs(args, env))));
            case Assign(Tree lhs, Tree rhs):
                if ((lhs.type.tdef().owner.modifiers & STATIC) != 0)
                    return super.translateExpr(tree, env);
                else {
                    lhsflag = true;
                    return trees.at(tree).make(
                        redef.Assign(
                            transExpr(lhs, env),
                            trees.castExpr(lhs.type, rhs.type, transExpr(rhs, env))));
                }
            case Assignop(int opcode, Tree lhs, Tree rhs, _):
                if ((lhs.type.tdef().owner.modifiers & STATIC) != 0)
                    return super.translateExpr(tree, env);
                else {
                    lhsflag = true;
                    return trees.at(tree).make(
                        redef.Assignop(opcode, transExpr(lhs, env),
                                        transExpr(rhs, env)));
                }
            case Typeop(int opcode, Tree expr, Tree clazz):
                Definition  def = clazz.type.tdef();
                // change typecasts and insert checks
                if (definitions.isAlgebraicSubclass(def)) {
                    mainContext.algebraicClasses = false;
                    boolean ignore = types.subtype(expr.type, def.type);
                    mainContext.algebraicClasses = true;
                    if (ignore) {
                        if (opcode == TYPETEST)
                            return trees.at(tree.pos).make(
                                newdef.Binop(NE, transExpr(expr, env), trees.Null()));
                        else
                            return transExpr(expr, env);
                    } else {
                        Tree first = transExpr(expr, env);
                        Tree second;
                        int old = trees.setPos(tree.pos);
                        if (algebraic.needForTemporaryInTypeop(first)) {
                            second = algebraic.temporaryIdent(
                                ((opcode != TYPETEST) ||
                                (((XClassDef)def).baseClass == ((XClassDef)expr.type.tdef()).baseClass)) ?
                                    ((XClassDef)def).baseClass.type :
                                    expr.type);
                            first = newdef.Assign(trees.copy(second),
                                ((opcode != TYPETEST) &&
                                (((XClassDef)def).baseClass != ((XClassDef)expr.type.tdef()).baseClass)) ?
                                    newdef.Typeop(TYPECAST, first, trees.toTree(((XClassDef)def).baseClass.type)) :
                                    first);
                        } else {
                            second = trees.copy(first);
                            if ((opcode != TYPETEST) &&
                                (((XClassDef)def).baseClass != ((XClassDef)expr.type.tdef()).baseClass))
                                second = newdef.Typeop(TYPECAST, second, trees.toTree(((XClassDef)def).baseClass.type));
                        }
                        // translate typecasts
                        if (opcode != TYPETEST) {
                            first = newdef.If(
                                    newdef.Binop(OR,
                                        newdef.Binop(EQ, first, trees.Null()),
                                        newdef.Index(
                                            newdef.Select(
                                                trees.toTree(clazz.type.tdef().owner.type),
                                                algebraic.prefix(CASTABLE_N, clazz.type.tdef())),
                                            newdef.Apply(
                                                newdef.Select(second, GET_SUB_ID_N),
                                                trees.noTrees))),
                                    second,
                                    newdef.Apply(
                                        newdef.Select(
                                            trees.ClassName(definitions.algebraicBase(def).owner),
                                            algebraic.prefix(THROW_CAST_EXC_N, definitions.algebraicBase(def))),
                                        trees.noTrees));
                        // translate instanceof
                        } else {
                            if (((XClassDef)def).baseClass == ((XClassDef)expr.type.tdef()).baseClass) {
                                first = newdef.Binop(AND,
                                            newdef.Binop(NE, first, trees.Null()),
                                            newdef.Index(
                                                newdef.Select(
                                                    trees.toTree(clazz.type.tdef().owner.type),
                                                    algebraic.prefix(CASTABLE_N, clazz.type.tdef())),
                                                newdef.Apply(
                                                    newdef.Select(second, GET_SUB_ID_N),
                                                    trees.noTrees)));
                            } else {
                                first = newdef.Binop(AND,
                                            newdef.Typeop(TYPETEST, first,
                                                trees.toTree(((XClassDef)def).baseClass.type)),
                                            newdef.Index(
                                                newdef.Select(
                                                    trees.toTree(clazz.type.tdef().owner.type),
                                                    algebraic.prefix(CASTABLE_N, clazz.type.tdef())),
                                                newdef.Apply(
                                                    newdef.Select(
                                                        newdef.Typeop(TYPECAST, second,
                                                            trees.toTree(((XClassDef)def).baseClass.type)),
                                                        GET_SUB_ID_N),
                                                    trees.noTrees)));
                            }
                        }
                        trees.setPos(old);
                        return first;
                    }
                // check impossible casts from 'this' to a CASEDEF
                } else if ((opcode != TYPETEST) &&
                    (def != null) &&
                    ((def.modifiers & CASEDEF) != 0)) {
                    expr = algebraic.removeTypecasts(expr);
                    switch (expr) {
                        case Self(_, int tag, Definition d):
                            if (tag == THIS) {
                                mainContext.algebraicClasses = false;
                                boolean possible = types.subtype(tree.type, expr.type);
                                mainContext.algebraicClasses = true;
                                if (!possible)
                                    report.error(tree.pos, "impossible to cast this to type " + tree.type);
                            }
                            break;
                    }
                }
                if (opcode == TYPETEST)
                    return trees.at(tree).make(
                        genInstanceof(
                            expr.type,
                            transExpr(expr, env),
                            clazz.type));
                else
                    return trees.at(tree).make(
                        genCast(
                            expr.type,
                            transExpr(expr, env),
                            clazz.type));
            default:
                return super.translateExpr(tree, env);
        }
    }
    
    public Tree genInstanceof(Type exprType, Tree expr, Type tpe) {
        switch ((XType)tpe) {
            case CompoundType(Type[] ts):
                if (ts.length == 0)
                    return trees.boolLit(true);
                else {
                    // this is not correct: buffering of expr value is required!!!
                    Tree res = genInstanceof(exprType, expr, ts[0]);
                    int i = 1;
                    while (i < ts.length)
                        res = newdef.Binop(AND,
                                genInstanceof(exprType, expr, ts[i++]),
                                res);
                    return res;
                }
            default:
                if ((tpe.tdef().modifiers & CLASSFIELD) != 0)
                    return newdef.Apply(
                        newdef.Select(
                            selectModule(tpe.outer()),
                            instanceofMethodName((XClassDef)tpe.tdef())),
                        new Tree[]{expr});
                else
                    return newdef.Typeop(TYPETEST, expr, trees.toTree(tpe));
        }
    }
    
    public Tree genCast(Type exprType, Tree expr, Type tpe) {
        switch ((XType)tpe) {
            case CompoundType(Type[] ts):
                if (ts.length == 0)
                    return expr;
                else {
                    Tree res = genCast(exprType, expr, ts[0]);
                    int i = 1;
                    while (i < ts.length)
                        res = genCast(exprType, res, ts[i++]);
                    return res;
                }
            default:
                /* if ((tpe.tdef().modifiers & CLASSFIELD) != 0)
                    return newdef.Apply(
                        newdef.Select(
                            selectModule(tpe.outer()),
                            castMethodName((XClassDef)tpe.tdef())),
                        new Tree[]{expr});
                else */
                    return newdef.Typeop(TYPECAST, expr, trees.toTree(tpe));
        }
    }
    
/** translate statements
 */
    protected Tree translateStat(Tree tree, Env env) {
        switch (tree) {
            case Exec(Tree expr):
                lhsflag = true;
                return trees.at(tree).make(redef.Exec(transExpr(expr, env)));
            case Switch(Tree selector, Case[] cases):
                int tag = selector.type.tag();
                if ((tag >= MIN_BASICTYPE_TAG) && (tag <= INT))
                    return super.translateStat(tree, env);
                else {
                    PatternMatcher  pm = new PatternMatcher(
                                                algebraic.PatternNodeFactory(),
                                                algebraic,
                                                trees, pretty, report,
                                                transExpr(selector, env).
                                                    setType(selector.type));
                    pm.printNodes = debug(8);
                    cases = transCases(cases, env);
                    int len = cases.length - 1;
                    for (int i = 0; i < len; i++)
                        pm.addCase(cases[i]);
                    if (len >= 0)
                        pm.addLastCase(cases[len]);
                    if (debug(4))
                        pm.print();
                    return pm.toTree();
                }
            default:
                return super.translateStat(tree, env);
        }
    }
    
/** translate patterns
 */
    public Tree[] transPatterns(Tree[] ts, Env env) {
        if (ts == null)
            return null;
        TreeList exprs = new TreeList();
        Tree tree;
        boolean inclDefault = false;
        for (int i = 0; i < ts.length; i++)
            if (ts[i] == null)
                inclDefault = true;
            else {
                tree = transPat(ts[i]);
                if (tree != null) {
                    Tree[] container = trees.getContainerContent(tree);
                    if (container == null)
                        exprs.append(tree);
                    else
                        exprs.append(container);
                }
            }
        tree = null;
        return inclDefault ? trees.append(tree, exprs.toArray()) :
                             exprs.toArray();
    }
        
	/** make a copy of a tree
	 */
    public Tree[] transPats(Tree[] ts) {
        if ((ts == null) || (ts.length == 0))
            return ts;
        else {
            Tree[] ts2 = new Tree[ts.length];
            for (int i = 0; i < ts.length; i++)
                ts2[i] = transPat(ts[i]);
            return ts2;
        }
    }

    public Tree transPat(Tree tree) {
        if (tree == null)
            return null;
        switch (tree) {
            case Apply(Tree fn, Tree[] args):
                return trees.at(tree).make(redef.Apply(transPat(fn), transPats(args)));
            case Binop(int opc, Tree lhs, Tree rhs, _):
                return trees.at(tree).make(redef.Binop(opc, transPat(lhs), transPat(rhs)));
            case Unop(int opc, Tree operand, _):
                return trees.at(tree).make(redef.Unop(opc, transPat(operand)));
            case Index(Tree indexed, Tree index):
                return trees.at(tree).make(redef.Index(transPat(indexed), transPat(index)));
            case Select(Tree selected, Name selector, _):
                return trees.at(tree).make(redef.Select(transPat(selected), selector));
            case Ident(Name name, Definition def):
            	if (isStaticModMember(def))
            		return trees.at(tree.pos).make(refStaticModMember(def));
            	else
                	return trees.at(tree).make(redef.Ident(name));
            case Self(Tree encl, int tag, _):
                return trees.at(tree).make(redef.Self(transPat(encl), tag));
            case Literal(Constant value):
                return trees.at(tree).make(redef.Literal(value));
            default:
                return trees.copy(tree);
        }
    }

    
/** translate types
 */
    protected Tree translateType(Tree tree, Env env) {
        switch ((XTree)tree) {
            case Select(Tree selected, Name selector, Definition c):
                if ((c.modifiers & CLASSFIELD) != 0)
                    return trees.objectType();
                else if (definitions.isAlgebraicClass(c))
                    return ((XTrees)trees).QualidBase((XClassDef)c);
                else if (definitions.isCase(c))
                    return trees.ClassName(c);
                else
                	return trees.at(tree).make(
                        redef.Select(transType(selected, env), selector));
            case Ident(Name name, Definition c):
                if ((c.modifiers & CLASSFIELD) != 0)
                    return trees.objectType();
                else if (definitions.isAlgebraicClass(c))
                    return ((XTrees)trees).QualidBase((XClassDef)c);
                Tree qual = (Tree)trees.replacements.get(tree);
                if (qual != null)
                	return trees.at(tree.pos).make(
                		newdef.Select(transType(qual, env), name));
                break;
            case ModuleSelect(_, _, _):
                return trees.toTree(tree.type.tdef().type);
        }
        return super.translateType(tree, env);
    }
    
/** special type translation (in declarations)
 */
    protected Tree[] specialTransTypes(Tree[] ts, Env env) {
        if (ts == null)
            return null;
        TreeList types = new TreeList();
        Tree tree;
        for (int i = 0; i < ts.length; i++)
            if ((tree = specialTransType(ts[i], env)) != null) {
                Tree[] container = trees.getContainerContent(tree);
                if (container == null)
                    types.append(tree);
                else
                    types.append(container);
            }
        return types.toArray();
    }
    
    protected Tree specialTransType(Tree tree, Env env) {
        if (tree == null)
            return null;
        int old = trees.setPos(tree.pos);
        Tree result = specialTranslateType(tree, env);
        trees.setPos(old);
        return result;
    }

    protected Tree specialTranslateType(Tree tree, Env env) {
        switch ((XTree)tree) {
            case ModuleSelect(_, _, _):
                return trees.toTree(tree.type.tdef().type);
            case Select(Tree selected, Name selector, Definition c):
                return trees.at(tree).make(
                            redef.Select(specialTransType(selected, env), selector));
            case Ident(Name name, Definition c):
            	Tree qual = (Tree)trees.replacements.get(tree);
                if (qual != null)
                	return trees.at(tree.pos).make(
                		newdef.Select(specialTransType(qual, env), name));
                else
                	return trees.at(tree).make(redef.Ident(name));
            case BasicType(int tag):
                return trees.at(tree).make(redef.BasicType(tag));
            case ArrayTypeTerm(Tree elemtype):
                return trees.at(tree).make(
                        redef.ArrayTypeTerm(specialTransType(elemtype, env)));
            default:
                throw new InternalError("specialTransType(" + tree + ")");
        }
    }
}

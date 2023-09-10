//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    context sensitive type checking support
//                           
//  [ModuleContexts.java (10855) 6-Jun-01 00:19 -> 22-Jun-01 23:13]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.keris.component.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import jaco.java.context.*;


public class ModuleContexts extends Component
                            implements XModifierConst, DefinitionConst {

    /** other components
     */
    protected XDefinitions      definitions;
    protected XTypes            types;
    protected XNameResolver     namer;
    protected ErrorHandler      report;
    protected Mangler           mangler;
    
    /** component name
     */
    public String getName() {
        return "KerisModuleContexts";
    }

    /** component initialization
     */
    public void init(XMainContext context) {
        super.init(context);
        definitions = (XDefinitions)context.Definitions();
        types = (XTypes)context.Types();
        report = context.ErrorHandler();
        mangler = context.Mangler();
        namer = (XNameResolver)context.NameResolver();
    }

    /** factory method for module types
     */
    public Type moduleType(Definition mod, Type in) {
        Type res = ((XTypeFactory)types.make).ModuleType(in);
        res.setDef(mod);
        if ((in != null) && (mod == in.tdef()))
            throw new Error();
        return res;
    }
    
    /** factory method for class types
     */
    public Type classType(Definition mod, Type in) {
        Type res = types.make.ClassType(in);
        res.setDef(mod);
        return res;
    }
    
    /** fix types contained in modules
     */
    public Type modularizeType(Type type) {
        switch (type) {
            case ClassType(Type outer):
                if ((type.tdef().modifiers & MODULE) != 0)
                    return moduleType(type.tdef(), null);
                if ((outer != null) &&
                    (outer.tdef() != null) &&
                    ((outer.tdef().modifiers & MODULE) != 0))
                    return classType(type.tdef(), moduleType(outer.tdef(), null));
                Definition d = type.tdef();
                while (d.owner.kind != PCK) {
                    if ((d.owner.modifiers & MODULE) != 0)
                        return classType(type.tdef(), moduleType(d.owner, null));
                    d = d.owner;
                }
        }
        return type;
    }
    
    /** qualify type with module type mod (possibly replacing some parts
     *  of mod)
     */
    public Type qualifyType(Type type, Type mod) {
        Type res = null;
        try {
        //System.out.println("qualify " + type + " with " + mod);
        if (type == null)
            return null;
        res = type;
        if ((mod == null) ||
            (mod.tdef().kind == PCK) ||
            (!inModule(type.tdef()) && !types.isModule(type)))
            return type;
        XClassDef c = (XClassDef)mod.tdef();
        switch ((XType)type) {
            case PackageType():
                res = mod;
                return mod;
            case ModuleType(Type outer):
                /*if ((outer == null) || (outer.tdef().kind == PCK)) {
                    if (c.modIntf.contained.contains((XClassDef)type.tdef()))
                        res = moduleType(type.tdef(), mod);
                    else
                        res = moduleType(type.tdef(), mod.outer());
                } else
                    res = moduleType(type.tdef(), qualifyType(outer, mod)); */
                if ((outer == null) || (outer.tdef().kind == PCK)) {
                    //System.out.println("c = " + c + "; c.modIntf = " + c.modIntf);
                    if (c.modIntf.contained.contains((XClassDef)type.tdef()))
                        res = moduleType(type.tdef(), mod);
                    else
                        res = qualifyType(type, mod.outer());
                } else
                    res = moduleType(type.tdef(), qualifyType(outer, mod)); 
                return res;
            case ClassType(Type outer):
                Type inner = (outer == null) ? mod : qualifyType(outer, mod);
                //System.out.println("  outer = " + outer + "; inner = " + inner);
                res = types.make.ClassType(inner);
                res.setDef(type.tdef());
                //System.out.println("  qualified type = " + res.toString() + "; " + res.getClass() + "; " + types.make.getClass());
                return res;
            case ArrayType(Type elemtype):
                res = types.make.ArrayType(qualifyType(elemtype, mod));
                res.setDef(type.tdef());
                return res;
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                res = types.make.MethodType(
                    qualifyType(argtypes, mod),
                    qualifyType(restype, mod),
                    qualifyType(thrown, mod));
                res.setDef(type.tdef());
                //System.out.println("  qualified method type = " + res);
                return res;
            default:
                return type;
        }
        } finally {
            //System.out.println("qualify " + type + " with " + mod + " = " + res);
        }
    }
    
    /** qualify types with module mod
     */
    public Type[] qualifyType(Type[] ts, Type mod) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        for (int i = 0; i < ts.length; i++)
            res[i] = qualifyType(ts[i], mod);
        return res;
    }
    
    /** refine type in the context of a derived module
     */
    public Type refineType(Type type, XClassDef mod) {
        //System.out.println("refine " + type + " in " + mod);
        if (type == null)
            return null;
        switch ((XType)type) {
            case ModuleType(Type outer):
                XClassDef c = (XClassDef)type.tdef();
                if ((outer == null) || (outer.tdef() == null) || (outer.tdef().kind == PCK)) {
                    if (mod.refinementOf(c))
                        return moduleType(mod, null);
                    else {
                        XClassDef norm = mod.modIntf.required.key(c);
                        if (norm != null)
                            return moduleType(norm, null);
                        norm = mod.modIntf.contained.key(c);
                        if (norm != null)
                            return moduleType(norm, moduleType(mod, null));
                        else
                            return type;
                    }
                } else {
                    Type outert = refineType(outer, mod);
                    XClassDef ctxt = (XClassDef)outert.tdef();
                    XClassDef norm = ctxt.modIntf.contained.key(c);
                    if (norm != null)
                        return moduleType(norm, outert);
                    else
                        return moduleType(c, outert);
                }
            case ClassType(Type outer):
           		XClassDef c = (XClassDef)type.tdef();
           		if ((c.modifiers & MODULE) != 0)
           			return refineType(moduleType(c, outer), mod);
                Type outert = refineType(outer, mod);
                if ((c.modifiers & CLASSFIELD) != 0) {
                    //System.out.println("classfield " + c + " of " + c.owner + " in " + mod + " with outer type " + outer);
                    c = (XClassDef)namer.findClassField(
                            (XClassDef)outert.tdef(),
                            mangler.unmangleShort(c.name, c.fullname));
                }
                Type res = types.make.ClassType(outert);
                res.setDef(c);
                return res;
            case ArrayType(Type elemtype):
                Type res = types.make.ArrayType(refineType(elemtype, mod));
                res.setDef(type.tdef());
                return res;
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                Type res = types.make.MethodType(
                    refineType(argtypes, mod),
                    refineType(restype, mod),
                    refineType(thrown, mod));
                res.setDef(type.tdef());
                return res;
            default:
                return type;
        }
    }
    
    /** refine types in the context of a derived module
     */
    public Type[] refineType(Type[] ts, XClassDef mod) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        for (int i = 0; i < ts.length; i++)
            res[i] = refineType(ts[i], mod);
        return res;
    }
    
    /** is thiz type a subtype of that type; seen within the module of thiz
     */
    public boolean subtype(Type thiz, Type that) {
        XClassDef top = (XClassDef)thiz.tdef().topLevelClass();
        return types.subtype(thiz,
                             ((top.modifiers & MODULE) != 0) ?
                                refineType(that, top) : that);
    }
    
    /** migrate a type definition 'type' from the module context 'from' to
     *  the module context 'to' where it is qualified with type 'qualifier'
     */
    public Type migrateType(Type type, XClassDef from, Type qualifier, XClassDef context) {
        //System.out.println("### migrate " + type + " coming from " + from + " as qualified from " + qualifier + " in " + context);
        // if we are outside of a module
        if ((context == null) ||
            ((context.modifiers & MODULE) == 0) ||
            type.isBasic() ||
            ((type.tdef().topLevelClass().modifiers & MODULE) == 0))
            //((type.tdef().modifiers & MODULE) != 0))
            return type;
        // find innermost module of 'qualifier'
        Type tomod = innermostModule(qualifier);
        if (tomod == null)
            return type;
        //System.out.println("### migrate " + type + " coming from " + from + " as qualified from " + qualifier + " in " + context);
        XClassDef to = (XClassDef)tomod.tdef();
        // inherit type
        if ((from != null) && ((from.modifiers & MODULE) != 0))
            type = inheritType(type, from, to);
        // qualify type
        type = qualifyType(type, tomod); // was: qualifier
        // refine type
        if (inModule(type.tdef()) || ((type.tdef().modifiers & MODULE) != 0))
            type = refineType(type, context);
        else {
            // in what definition did we select?
            XClassDef in = (XClassDef)qualifier.tdef();
            // are we in a class (or directly inside the module)?
            if (((in.modifiers & MODULE) == 0) && inModule(in))
                type = updateType(type, in, qualifier, context);
            else
                type = refineType(type, context);
        }
        //System.out.println("######### results in " + type);
        return type;
    }
    
    public Type migrateType(Type type, Definition def, Type qualifier, ContextEnv from) {
        return migrateType(
                    type,
                    moduleOf(def),
                    modularizeType(qualifier),
                    (XClassDef)from.enclClass.def.topLevelClass());
    }
    
    public Type[] migrateType(Type[] ts, Definition def, Type qualifier, ContextEnv from) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        XClassDef mod = moduleOf(def);
        XClassDef context = (XClassDef)from.enclClass.def.topLevelClass();
        qualifier = modularizeType(qualifier);
        for (int i = 0; i < ts.length; i++)
            res[i] = migrateType(ts[i], mod, qualifier, context);
        return res;
    }
    
    public Type[] printTypes(Type[] ts) {
        for (int i = 0; i < ts.length; i++)
            System.out.println("      " + ts[i] + " with supertype " + ts[i].tdef().supertype());
        return ts;
    }
    

    /** update type as qualified from site in the current context (module)
     */
    public Type updateType(Type type, Type site, ContextEnv env) {
        return updateType(type, site, (XClassDef)env.enclClass.def().topLevelClass());
    }
    
    /** update type as qualified from site in the context of module mod
     */
    public Type updateType(Type type, Type site, XClassDef mod) {
        site = modularizeType(site);
        //System.out.println("### update " + type + " as qualified from " + site + " in " + mod);
        // find innermost module of 'site'
        Type inMod = innermostModule(site);
        if ((mod == null) ||
            ((mod.modifiers & MODULE) == 0))
            return inheritType(type, inMod);
        else if (inModule(type.tdef()) || ((type.tdef().modifiers & MODULE) != 0)) {
            type = refineType(inheritType(type, inMod), mod);
            return type;
        } else {
            // find owner of type
            // Definition owner = type.tdef().owner;
            // in what definition did we select?
            XClassDef in = (XClassDef)site.tdef();
            // are we in a class (or directly inside the module)?
            if (((in.modifiers & MODULE) == 0) && inModule(in))
                return updateType(type, in, site, mod);
            return refineType(inheritType(type, inMod), mod);
        }
    }
    
    public Type updateType(Type type, XClassDef in, Type site, XClassDef mod) {
        // find superclass of 'in' where 'def' was found?
        Type inMod = innermostModule(site);
        Definition owner = type.tdef().owner;
        while (owner != in) {
            // try interfaces
            Type[] intfs = in.interfaces();
            for (int i = 0; i < intfs.length; i++) {
                XClassDef newin = (XClassDef)intfs[i].tdef();
                if (!inModule(newin))
                    return null;
                Type res = updateType(type, newin, inheritType(intfs[i], inMod), mod);
                if (res != null)
                    return res;
            }
            // try supertype
            Type superIn = in.supertype();
            if (superIn == null)
                return null;
            in = (XClassDef)superIn.tdef();
            if (!inModule(in))
                return inheritType(type, inMod);
            site = inheritType(superIn, inMod);
            inMod = innermostModule(site);
        }
        return refineType(inheritType(type, inMod), mod);
    }

    /** requalify type in new module context
     */
    public Type[] updateType(Type[] ts, Type site, XClassDef mod) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        for (int i = 0; i < ts.length; i++)
            res[i] = updateType(ts[i], site, mod);
        return res;
    }
    
    public Type[] updateType(Type[] ts, Type site, ContextEnv env) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        for (int i = 0; i < ts.length; i++)
            res[i] = updateType(ts[i], site, env);
        return res;
    }
    
    public Type inheritType(Definition def, ContextEnv env) {
        if (def.kind >= BAD)
            return def.type;
        // find innermost module of 'def'
        XClassDef defmod = moduleOf(def);
        if (defmod == null)
            return modularizeType(def.type);
        // find current module
        XClassDef topmod = (XClassDef)env.enclClass.def().topLevelClass();
        // map type
        Type res = inheritType(modularizeType(def.type),
                               defmod,
                               topmod);
        //System.out.println("inherit(" + def + ", " + topmod + ") = " + res);
        return res;
    }
    
    public Type inheritType(Type type, Type qualifier) {
        if ((type == Type.ErrType) || (qualifier == Type.ErrType))
            return Type.ErrType;
        //System.out.println("inherit(" + type + ", " + qualifier + ") = ?");
        if (qualifier == null)
            throw new InternalError();
        // find innermost module of 'def'
        Type defmod = innermostModule(type);
        // find innermost module of 'qualifier'
        Type qualmod = innermostModule(qualifier);
        // don't inherit if one is not a module
        if ((defmod == null) || (qualmod == null) ||
            (defmod == Type.ErrType) || (qualmod == Type.ErrType))
            return qualifyType(type, qualifier);
        // map type
        Type res = qualifyType(
                    inheritType(type,
                                (XClassDef)defmod.tdef(),
                                (XClassDef)qualmod.tdef()),
                    qualifier);
        //System.out.println("inherit(" + type + ", " + qualifier + ") = " + res);
        return res;
    }
    
    public Type inheritType(Definition def, Type qualifier) {
        if (def.kind >= BAD)
            return def.type;
        // find innermost module of 'def'
        XClassDef defmod = moduleOf(def);
        if (defmod == null)
            return modularizeType(def.type);
        // find innermost module of 'qualifier'
        Type qualmod = innermostModule(qualifier);
        if (qualmod == Type.ErrType)
            return qualmod;
        // map type
        Type res = qualifyType(
                    inheritType(modularizeType(def.type),
                                defmod,
                                (XClassDef)qualmod.tdef()),
                    qualifier);
        //System.out.println("inherit(" + def + ", " + qualifier + ") = " + res);
        return res;
    }
    
    /** this function inherits a type 'raw' from module 'owner' to
     *  module 'current' (applying specialization mappings)
     */
    public Type inheritType(Type raw, XClassDef owner, XClassDef current) {
        //System.out.println("inheritType(" + raw + ", " + owner + ", " + current + ")");
        report.assert ((owner.modifiers & MODULE) != 0);
        report.assert ((current.modifiers & MODULE) != 0);
        if (current == owner)
            return raw;
        if ((owner.modifiers & INTERFACE) != 0) {
            Type[] intf = current.interfaces();
            for (int i = 0; i < intf.length; i++) {
                Type tpe = inheritType(raw, owner, (XClassDef)intf[i].tdef());
                if (tpe != null) {
                    if ((current.modifiers & INTERFACE) != 0)
                        return mapType(tpe, current.modIntf);
                    else
                        return tpe;
                }
            }
            if ((current.modifiers & INTERFACE) != 0)
                return null;
        }
        return mapType(
            inheritType(raw, owner, (XClassDef)current.supertype().tdef()),
            current.modIntf);
    }
    
    /** mapType specializes type according to the mapping specified by map
     */
    public Type mapType(Type type, ModuleIntf map) {
        if (type == null)
            return null;
        if ((map.self.modifiers & SPECIALIZES) == 0)
            return type;
        switch ((XType)type) {
            case ModuleType(Type outer):
                XClassDef c = (XClassDef)type.tdef();
                if ((outer == null) || (outer.tdef().kind == PCK)) {
                    if (((XClassDef)map.self.supertype().tdef()).refinementOf(c))
                        return moduleType(map.self, null);
                    XClassDef d = (XClassDef)map.overriddenRequired.get(c);
                    if (d != null)
                        return moduleType(d, null);
                    d = (XClassDef)map.overriddenContained.get(c);
                    if (d != null)
                        return moduleType(d, moduleType(map.self, null));
                    else
                        return refineType(type, map.self);
                } else {
                    Type outertpe = mapType(outer, map);
                    type = inheritType(moduleType(c, null),
                                       (XClassDef)outer.tdef(),
                                       (XClassDef)outertpe.tdef());
                    return moduleType(type.tdef(), outertpe);
                }
            case ClassType(Type outer):
                XClassDef c = (XClassDef)type.tdef();
                Type outertpe = mapType(outer, map);
                if ((c.modifiers & CLASSFIELD) != 0)
                    c = (XClassDef)namer.findClassField(
                            (XClassDef)outertpe.tdef(),
                            mangler.unmangleShort(c.name, c.fullname));
                type = types.make.ClassType(outertpe);
                type.setDef(c);
                return type;
            case ArrayType(Type elemtype):
                Type res = types.make.ArrayType(mapType(elemtype, map));
                res.setDef(type.tdef());
                return res;
            case MethodType(Type[] argtypes, Type restype, Type[] thrown):
                Type res = types.make.MethodType(mapType(argtypes, map),
                                                 mapType(restype, map),
                                                 mapType(thrown, map));
                res.setDef(type.tdef());
                return res;
            default:
                return type;
        }
    }
    
    public Type[] mapType(Type[] ts, ModuleIntf map) {
        if (ts == null)
            return null;
        Type[] res = new Type[ts.length];
        for (int i = 0; i < ts.length; i++)
            res[i] = mapType(ts[i], map);
        return res;
    }
    
    /** return the innermost module of this type
     */
    public Type innermostModule(Type type) {
        while (type != null)
            switch ((XType)type) {
                case ClassType(Type outer):
                    type = outer;
                    break;
                case ModuleType(_):
                    return type;
                default:
                    return null;
            }
        return null;
    }
    
    /** return the outermost module of this type
     */
    public Type outermostModule(Type type) {
        while (type != null)
            switch ((XType)type) {
                case ClassType(Type outer):
                    type = outer;
                    break;
                case ModuleType(Type outer):
                    if ((outer == null) || (outer.tdef().kind == PCK))
                        return type;
                    type = outer;
                    break;
                default:
                    return null;
            }
        return null;
    }
    
    /** return module in which 'def' is defined; return null if
     *  'def' is not defined inside of a module
     */
    public XClassDef moduleOf(Definition def) {
        if (def == null)
            return null;
        while (def.owner != null) {
            if (((def.owner.modifiers & XModifierConst.MODULE) != 0) &&
                (def.owner.kind == TYP)) {
            	// System.out.println("DEF = " + def + " OWNED BY " + def.owner + " / " + def.owner.getClass());
                return (XClassDef)def.owner;
          	}
            def = def.owner;
        }
        return null;
    }
    
    /** is this definition inside a module?
     */
    public boolean inModule(Definition def) {
        if (def == null)
            return false;
        while (def.owner != null) {
            if ((def.owner.modifiers & XModifierConst.MODULE) != 0)
                return true;
            def = def.owner;
        }
        return false;
    }
        
    /** convert def to type
     */
    public Type defToType(XClassDef def) {
        XClassDef mod = (XClassDef)def.topLevelClass();
        if (((mod.modifiers & MODULE) == 0) ||
            ((def.modifiers & MODULE) != 0))
            return def.type;
        return classType(def, myDefToType((XClassDef)def.enclClass()));
    }
    
    private Type myDefToType(XClassDef def) {
        if ((def.modifiers & MODULE) != 0)
            return moduleType(def, null);
        else
            return classType(def, myDefToType((XClassDef)def.enclClass()));
    }
}

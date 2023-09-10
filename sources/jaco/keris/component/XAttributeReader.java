//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    classfile attribute reader
//                           
//  [XAttributeReader.java (3231) 16-May-01 17:11 -> 8-Jun-01 23:22]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.keris.component.*;
import jaco.keris.struct.*;
import Type.*;


public class XAttributeReader extends AttributeReader
                              implements XAttributeConst, XModifierConst {
    
    protected XSignatures sigs;
    protected Types types;
    
    public XAttributeReader(ClassReader reader) {
        super(reader);
        sigs = (XSignatures)((XClassReader)reader).sigs;
        types = sigs.getTypesModule();
    }

    public int nameToId(Name name) {
        if (name == JACO_MODULE_N)
            return JACO_MODULE_ATTR;
        if (name == JACO_CLASSFIELD_N)
            return JACO_CLASSFIELD_ATTR;
        if (name == JACO_ALGEBRAIC_N)
            return JACO_ALGEBRAIC_ATTR;
        if (name == JACO_SCOPE_N)
            return JACO_SCOPE_ATTR;
        return super.nameToId(name);
    }
    
    protected XClassDef readClassRef() {
        int index = in.nextChar();
        if (index == 0)
            return null;
        else
            return (XClassDef)reader.definitions.defineClass(
                        reader.readClassName(index));
    }
    
    protected ModuleSet readModuleSet() {
        XClassDef[] cs = new XClassDef[in.nextChar()];
        for (int i = 0; i < cs.length; i++)
            cs[i] = readClassRef();
        return new ModuleSet(cs);
    }
    
    protected ModuleMap readModuleMapToSet() {
        XClassDef[] cs = new XClassDef[in.nextChar()];
        Object[] os = new Object[cs.length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = readClassRef();
            os[i] = readModuleSet();
        }
        return new ModuleMap(cs, os);
    }
    
    protected ModuleMap readModuleMapToDef() {
        XClassDef[] cs = new XClassDef[in.nextChar()];
        Object[] os = new Object[cs.length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = readClassRef();
            os[i] = readClassRef();
        }
        return new ModuleMap(cs, os);
    }
    
    protected ModuleMap readModuleMapToType() {
        XClassDef[] cs = new XClassDef[in.nextChar()];
        Object[] os = new Object[cs.length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = readClassRef();
            os[i] = readType();
        }
        return new ModuleMap(cs, os);
    }
    
    protected TypeMap readTypeMap() {
        int n = in.nextChar();
        TypeMap map = new TypeMap(types);
        for (int i = 0; i < n; i++)
            map.add(readType(), readTypeSet());
        return map;
    }
    
    protected TypeSet readTypeSet() {
        int n = in.nextChar();
        TypeSet set = new TypeSet(types);
        for (int i = 0; i < n; i++)
            set.add(readType());
        return set;
    }
    
    protected Type readType() {
        return reader.readType(in.nextChar());
    }
    
    public void readAttribute(Definition def, int attr, int attrLen) {
        switch (attr) {
            case JACO_MODULE_ATTR:
                ((XClassDef)def).ensureCache();
                ((XClassDef)def).cache.modifiers |= MODULE;
                ModuleSet specialized = readModuleSet();
                if (specialized.size() == 0)
                    specialized = null;
                ModuleMap required = readModuleMapToSet();
                ModuleMap contained = readModuleMapToSet();
                ModuleMap overriddenRequired = readModuleMapToDef();
                ModuleMap overriddenContained = readModuleMapToDef();
                ModuleMap imported = readModuleMapToType();
                TypeMap maxInterfaces = readTypeMap();
                ((XClassDef)def).modIntf = new ModuleIntf(
                    (XClassDef)def,
                    specialized,
                    required,
                    contained,
                    null,
                    overriddenRequired,
                    overriddenContained,
                    imported,
                    maxInterfaces);
                break;
            case JACO_CLASSFIELD_ATTR:
                ((XClassDef)def).ensureCache();
                //System.out.println("###" + def + " is interface " + (def.modifiers & INTERFACE));
                XClassDef c = (XClassDef)def;
                c.cache.modifiers |= CLASSFIELD;
                c.vcIntf = new VirtualClassIntf();
                c.vcIntf.withClass = readClassRef();
                c.vcIntf.thisImpl = (in.nextChar() == 1);
                c.vcIntf.depends = new Type[in.nextChar()];
                for (int i = 0; i < c.vcIntf.depends.length; i++)
                    c.vcIntf.depends[i] = readType();
                c.vcIntf.overrides = new XClassDef[in.nextChar()];
                for (int i = 0; i < c.vcIntf.overrides.length; i++)
                    c.vcIntf.overrides[i] = (XClassDef)reader.definitions.defineClass(
                        reader.readClassName(in.nextChar()));
                break;
            case INNERCLASSES_ATTR:
                int n = in.nextChar();
                for (int i = 0; i < n; i++) {
                    Definition  inner = (Definition)reader.readPool(in.nextChar());
                    Definition  outer = (Definition)reader.readPool(in.nextChar());
                    Name        name = (Name)reader.readPool(in.nextChar());
                    int         flags = in.nextChar();
                    if (name != null) {
                        mangler.put(inner.fullname, outer, name, flags);
                        if (outer == def) {
                            def.locals().enterIfAbsent(inner.proxy(name));
                            ((XClassDef)def).cacheLocals().enterIfAbsent(inner.proxy(name));
                            inner.owner = def;
                            if (definitions.isInnerclass(inner))
                                ((ClassType)inner.type).outer = def.type;
                        }
                    }
                }
                return;
            case JACO_ALGEBRAIC_ATTR:
                int tag = in.nextChar();
                switch (def) {
                    case MethodDef(_):
                        ((XMethodDef)def).tag = tag;
                        def.modifiers |= CASEDEF;
                        break;
                    
                    case VarDef(_, _, _):
                        ((XVarDef)def).tag = tag;
                        def.modifiers |= CASEDEF;
                        break;
                }
                break;
            case JACO_SCOPE_ATTR:
                readScopeAttr((XClassDef)def);
                break;
            default:
                super.readAttribute(def, attr, attrLen);
        }
    }
    
    protected void readScopeAttr(XClassDef c) {
        int mods = (c.cache != null) ? c.cache.modifiers : 0;
        c.ensureCache();
        c.cache.modifiers = in.nextChar();
        c.cache.modifiers |= (in.nextChar() << 16);
        c.cache.modifiers |= mods;
        c.tag = in.nextChar();
        c.baseClass = (XClassDef)reader.definitions.defineClass(
                            reader.readClassName(in.nextChar()));
        c.cache.supertype = readType();
        Type[] is = new Type[in.nextChar()];
        for  (int i = 0; i < is.length; i++)
            is[i] = readType();
        c.cache.interfaces = is;
        reader.methodAttr |= JACO_ALGEBRAIC_ATTR;
        reader.fieldAttr |= JACO_ALGEBRAIC_ATTR;
        char fieldCount = in.nextChar();
        for (int i = 0; i < fieldCount; i++)
            c.cache.locals.enter(reader.readField());
        char    methodCount = in.nextChar();
        for (int i = 0; i < methodCount; i++)
            c.cache.locals.enter(reader.readMethod());
        reader.methodAttr &= ~JACO_ALGEBRAIC_ATTR;
        reader.fieldAttr &= ~JACO_ALGEBRAIC_ATTR;
        readAttributes(c, reader.classAttr & ~JACO_SCOPE_ATTR);
    }
}

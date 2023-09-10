//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    classfile attribute writer
//                           
//  [XAttributeWriter.java (3874) 16-May-01 17:13 -> 8-Jun-01 00:01]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.keris.component.*;
import jaco.keris.struct.*;
import java.util.*;


public interface XAttributeConst {
    int JACO_MODULE_ATTR = 0x00200;
    int JACO_CLASSFIELD_ATTR = 0x00400;
    int JACO_ALGEBRAIC_ATTR = 0x0100;
    int JACO_SCOPE_ATTR = 0x4000;
    
    Name JACO_MODULE_N = Name.fromString("KERIS_MODULE");
    Name JACO_CLASSFIELD_N = Name.fromString("KERIS_CLASS_FIELD");
    Name JACO_ALGEBRAIC_N = Name.fromString("KERIS_ALGEBRAIC");
    Name JACO_SCOPE_N = Name.fromString("KERIS_SCOPE");
}

public class XAttributeWriter extends AttributeWriter
                              implements XAttributeConst, XModifierConst {
    
    protected XSignatures sigs;
    
    
    public XAttributeWriter(ClassWriter parent) {
        super(parent);
        sigs = (XSignatures)((XClassWriter)parent).sigs;
    }
    
    protected void writeClassRef(XClassDef current, XClassDef c) {
        if (c == null)
            out.writeChar(0);
        else
            out.writeChar(current.pool.put(c));
    }
    
    protected void writeClassRefs(XClassDef current, XClassDef[] cs) {
        if (cs == null)
            out.writeChar(0);
        else {
            out.writeChar(cs.length);
            for (int i = 0; i < cs.length; i++)
                writeClassRef(current, cs[i]);
        }
    }
    
    protected void writeModuleSet(XClassDef c, ModuleSet set) {
        if (set == null)
            out.writeChar(0);
        else
            writeClassRefs(c, set.keys());
    }
    
    protected void writeModuleMapToSet(XClassDef c, ModuleMap map) {
        if (map == null)
            out.writeChar(0);
        else {
            XClassDef[] cs = map.keys();
            out.writeChar(cs.length);
            for (int i = 0; i < cs.length; i++) {
                writeClassRef(c, cs[i]);
                writeModuleSet(c, (ModuleSet)map.content[i]);
            }
        }
    }
    
    protected void writeModuleMapToDef(XClassDef c, ModuleMap map) {
        if (map == null)
            out.writeChar(0);
        else {
            XClassDef[] cs = map.keys();
            out.writeChar(cs.length);
            for (int i = 0; i < cs.length; i++) {
                writeClassRef(c, cs[i]);
                writeClassRef(c, (XClassDef)map.content[i]);
            }
        }
    }
    
    protected void writeModuleMapToType(XClassDef c, ModuleMap map) {
        if (map == null)
            out.writeChar(0);
        else {
            XClassDef[] cs = map.keys();
            out.writeChar(cs.length);
            for (int i = 0; i < cs.length; i++) {
                writeClassRef(c, cs[i]);
                writeType((Type)map.content[i]);
            }
        }
    }
    
    protected void writeTypeMap(TypeMap map) {
        if (map == null)
            out.writeChar(0);
        else {
            Type[] ts = map.keys();
            out.writeChar(ts.length);
            for (int i = 0; i < ts.length; i++) {
                writeType(ts[i]);
                writeTypeSet(map.values[i]);
            }
        }
    }
    
    protected void writeTypeSet(TypeSet set) {
        if (set == null)
            out.writeChar(0);
        else {
            Type[] ts = set.types();
            out.writeChar(ts.length);
            for (int i = 0; i < ts.length; i++)
                writeType(ts[i]);
        }
    }
    
    protected void writeType(Type type) {
        out.writeChar(pool.put(sigs.typeToSig(type)));
    }
    
    public boolean writeAttribute(int attr, Definition def) {
        switch (attr) {
            case JACO_MODULE_ATTR:
                switch (def) {
                    case ClassDef(_, _, _, _, _):
                        XClassDef c = (XClassDef)def;
                        if (c.cache == null)
                            return false;
                        if ((c.cache.modifiers & MODULE) != 0) {
                            int adr = writeAttr(JACO_MODULE_N);
                            writeModuleSet(c, c.modIntf.specialized);
                            writeModuleMapToSet(c, c.modIntf.required);
                            writeModuleMapToSet(c, c.modIntf.contained);
                            writeModuleMapToDef(c, c.modIntf.overriddenRequired);
                            writeModuleMapToDef(c, c.modIntf.overriddenContained);
                            writeModuleMapToType(c, c.modIntf.imported);
                            writeTypeMap(c.modIntf.maxInterfaces);
                            return endAttr(adr);
                        } else
                            return false;
                }
                break;
            case JACO_CLASSFIELD_ATTR:
                switch (def) {
                    case ClassDef(_, _, _, _, _):
                        XClassDef c = (XClassDef)def;
                        if (c.cache == null)
                            return false;
                        if ((c.cache.modifiers & CLASSFIELD) != 0) {
                            int adr = writeAttr(JACO_CLASSFIELD_N);
                            writeClassRef(c, c.vcIntf.withClass);
                            out.writeChar(c.vcIntf.thisImpl ? 1 : 0);
                            out.writeChar(c.vcIntf.depends.length);
                            for (int i = 0; i < c.vcIntf.depends.length; i++)
                                writeType(c.vcIntf.depends[i]);
                            writeClassRefs(c, c.vcIntf.overrides);
                            return endAttr(adr);
                        } else
                            return false;
                }
                break;
            case JACO_ALGEBRAIC_ATTR:
                int tag = -1;
                switch (def) {
                    case MethodDef(_):
                        if ((def.modifiers & CASEDEF) != 0)
                            tag = ((XMethodDef)def).tag;
                        break;
                    case VarDef(_, _, _):
                        if ((def.modifiers & CASEDEF) != 0)
                            tag = ((XVarDef)def).tag;
                        break;
                }
                if (tag >= 0) {
                    int adr = writeAttr(JACO_ALGEBRAIC_N);
                    out.writeChar(tag);
                    return endAttr(adr);
                } else
                    return false;
            case JACO_SCOPE_ATTR:
                switch (def) {
                    case ClassDef(_, _, _, _, _):
                        return writeScopeAttr((XClassDef)def);
                }
                break;
        }
        return super.writeAttribute(attr, def);
    }
    
    public boolean writeScopeAttr(XClassDef c) {
        if (c.cache == null)
            return false;
        sigs.extended = true;
        int adr = writeAttr(JACO_SCOPE_N);
        // write original modifiers
        out.writeChar((c.cache.modifiers & LANGUAGE_MODS) | ACC_SUPER);
        out.writeChar(c.cache.modifiers >>> 16);
        // write algebraic type information
        out.writeChar(c.tag);
        out.writeChar(c.pool.put(c.baseClass));
        // write supertype
        writeType(c.cache.supertype);
        // write implemented interfaces
        Type[] is = c.cache.interfaces;
        out.writeChar(is.length);
        for (int i = 0; i < is.length; i++)
            writeType(is[i]);
        // write original scope
        Definition  elems = c.cache.locals.elems;
        int classAttr = parent.classAttr;
        int methodAttr = parent.methodAttr;
        int fieldAttr = parent.fieldAttr;
        parent.classAttr = DEPRECATED_ATTR;
        parent.methodAttr = EXCEPTIONS_ATTR | DEPRECATED_ATTR | JACO_ALGEBRAIC_ATTR;
        parent.fieldAttr = CONSTANT_VALUE_ATTR | DEPRECATED_ATTR | JACO_ALGEBRAIC_ATTR;
        parent.writeDefinitions(elems, VAR);
        parent.writeDefinitions(elems, FUN);
        parent.writeAttributes(c);
        parent.classAttr = classAttr;
        parent.methodAttr = methodAttr;
        parent.fieldAttr = fieldAttr;
        sigs.extended = false;
        return endAttr(adr);
    }
}

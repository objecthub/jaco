//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    keris types
//                           
//  [XType.java (6980) 3-May-01 14:31 -> 6-Jul-01 01:35]

package jaco.keris.struct;

import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.keris.component.*;


public class XType extends Type {
    
    public case ModuleType(Type outer);
    
    public case CompoundType(Type[] components);
    
    public static class XClassType extends ClassType {
        public XClassType(Type outer) {
            super(outer);
        }
        
        public String toString() {
            switch (outer) {
                case ClassType(_):
                    return outer + "." + tdef.name;
                case ModuleType(_):
                    return outer + "." + tdef.name;
                default:
                    return outer + "->" + tdef.fullname;
            }
        }
        
        /** returns the supertype of this type.
         */
        public Type supertype() {
            return reformulate(tdef.supertype(), outer);
        }

        /** returns the types of implemented interfaces
         */
        public Type[] interfaces() {
            Type[] intfs = tdef.interfaces();
            Type[] res = new Type[intfs.length];
            for (int i = 0; i < intfs.length; i++)
                res[i] = reformulate(intfs[i], outer);
            return res;
        }
        
        /** returns the types of dependent class fields
         */
        public Type[] depends() {
            Type[] intfs = ((XClassDef)tdef).vcIntf.depends;
            if (intfs == null)
                return null;
            Type[] res = new Type[intfs.length];
            for (int i = 0; i < intfs.length; i++)
                res[i] = reformulate(intfs[i], outer);
            return res;
        }
    }
    
    
	/** copy a type
	 */
    public Type dup() {
        switch (this) {
            case ModuleType(Type outer):
                Type res = ModuleType(outer);
                res.setDef(tdef);
                res.setConst(tconst);
                return res;
            case CompoundType(Type[] components):
                Type res = CompoundType(components);
                res.setDef(tdef);
                res.setConst(tconst);
                return res;
            default:
                return super.dup();
        }
    }

	/** return a string representation of the type
	 */
    public String toString() {
        switch (this) {
            case ModuleType(Type outer):
                if (outer == null)
                    return "_::" + tdef.fullname.toString();
                else
                    return outer + "::" + tdef.fullname;
            case CompoundType(Type[] components):
                if (components.length == 0)
                    return "[]";
                String res = "[" + components[0];
                for (int i = 1; i < components.length; i++)
                    res += ", " + components[i];
                return res + "]";
            default:
                return super.toString();
        }
    }

	/** for class/module types; returns the type of the outer class
	 */
    public Type outer() {
        switch (this) {
            case ModuleType(Type outer):
                return outer;
            default:
                return super.outer();
        }
    }

	/** is it a module type?
	 */
    private static boolean isModule(Type type) {
        if (type == null)
            return false;
        switch (type) {
            case ModuleType(_):
                return true;
            default:
                return false;
        }
    }
    
	/** is this type nested in a module?
	 */
    private static boolean inModule(Definition def) {
        while (def.owner != null) {
            if ((def.owner.modifiers & XModifierConst.MODULE) != 0)
                return true;
            def = def.owner;
        }
        return false;
    }
    
    private static boolean inModule(Type tpe) {
        if (tpe.tdef() == null) // tpe is a package
            return false;
        return inModule(tpe.tdef());
    }
    
    private static Type transform(Type thiz, Type context) {
    	//System.out.println("  transform(" + thiz + ", " + context + ")");
        if ((thiz == null) ||
            (context == null) ||
            (!inModule(thiz) && ((thiz.tdef().modifiers & XModifierConst.MODULE) == 0)))
            return thiz;
        XClassDef thizdef = (XClassDef)thiz.tdef();
        switch (thiz) {
            case ModuleType(Type outer):
                if (isModule(outer)) {
                    Type res = ModuleType(transform(outer, context));
                    res.setDef(thizdef);
                    res.setConst(thiz.tconst());
                    return res;
                } else {
                    while (isModule(context) &&
                           !((XClassDef)context.tdef()).modIntf.contained.contains(thizdef))
                        context = context.outer();
                    Type res = ModuleType(context);
                    res.setDef(thizdef);
                    res.setConst(thiz.tconst());
                    return res;
                }
            case ClassType(Type outer):
                if (((thizdef.modifiers & XModifierConst.MODULE) == 0) || isModule(outer)) {
                    Type res = new XClassType(transform(outer, context));
                    res.setDef(thizdef);
                    res.setConst(thiz.tconst());
                    return res;
                } else {
                    while (isModule(context) &&
                           !((XClassDef)context.tdef()).modIntf.contained.contains(thizdef))
                        context = context.outer();
                    Type res = new XClassType(context);
                    res.setDef(thizdef);
                    res.setConst(thiz.tconst());
                    return res;
                }
            case CompoundType(Type[] fields):
                Type[] fs = new Type[fields.length];
                for (int i = 0; i < fields.length; i++)
                    fs[i] = transform(fields[i], context);
                return CompoundType(fs);
            default:
                return thiz;
        }
    }
    
    private static Type reformulate(Type thiz, Type context) {
        // find the module containing the base class definition
        loop:
        while (context != null)
            switch (context) {
                case ClassType(Type outer):
                    context = outer;
                    break;
                case ModuleType(Type outer):
                    break loop;
                default:
                    return thiz; // the base class is not in a module
            }
        // now context refers to the module of the base class
        return transform(thiz, context);
    }
    
	/** returns the supertype of this type.
	 */
    public Type supertype() {
        switch (this) {
            case ModuleType(Type outer):
                return tdef.type.supertype();
            case ClassType(Type outer):
                return reformulate(tdef.supertype(), outer);
            default:
                return null;
        }
    }

	/** returns the types of implemented interfaces
	 */
    public Type[] interfaces() {
        switch (this) {
            case ModuleType(Type outer):
                return tdef.type.interfaces();
            case ClassType(Type outer):
                Type[] intfs = tdef.interfaces();
                Type[] res = new Type[intfs.length];
                for (int i = 0; i < intfs.length; i++)
                    res[i] = reformulate(intfs[i], outer);
                return res;
            case CompoundType(Type[] components):
                return components;
            default:
                return null;
        }
    }

	/** return the tag of a type
	 */
    public int tag() {
        switch (this) {
            case ModuleType(_):
            case CompoundType(_):
                return TypeConst.REF;
            default:
                return super.tag();
        }
    }

	/** does this type contain "error" elements?
	 */
    public boolean erroneous() {
        switch (this) {
            case ModuleType(Type outer):
                if (outer == null)
                    return false;
                else
                    return outer.erroneous();
            case CompoundType(Type[] components):
                for (int i = 0; i < components.length; i++)
                    if (components[i].erroneous())
                            return true;
                return false;
            default:
                return super.erroneous();
        }
    }
    
    /** the type factory
     */
    public static interface Factory extends Type.Factory {
        Type ModuleType(Type outer);
        Type CompoundType(Type[] components);
    }
}

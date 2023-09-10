//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    types module
//                           
//  [XTypes.java (4303) 27-Apr-01 16:41 -> 6-Jun-01 00:13]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.keris.struct.*;
import jaco.keris.context.*;
import java.util.*;


public class XTypes extends Types implements XModifierConst {
    
    protected XMainContext mainContext;
    protected Map classimpls = new HashMap();
    
    
    public String getName() {
        return "XTypes";
    }
    
    public void init(MainContext context) {
        make = new XTypeFactory(this);
        mainContext = (XMainContext)context;
        super.init(context);
    }
    
    ///
    /// Support for algebraic types
    /// 
    
    public boolean isAlgebraicType(Type type) {
        return (type != null) && ((type.tdef().modifiers & ALGEBRAIC) != 0);
    }
    
    public boolean isClassField(Type type) {
        return (type != null) && ((type.tdef().modifiers & CLASSFIELD) != 0);
    }
    
    public boolean isCaseType(Type type) {
        return (type != null) && ((type.tdef().modifiers & CASEDEF) != 0);
    }
    
    public Type algebraicSupertype(Type type) {
        if (type == null)
            return null;
        else if ((type.tdef().complete().modifiers & ALGEBRAIC) != 0)
            return type;
        else
            return algebraicSupertype(type.supertype());
    }
   
    ///
    /// Support for modules
    /// 
    
    /** is this a module type?
     */
    public boolean isModule(Type type) {
        switch (type) {
            case XType.ModuleType(_):
                return true;
            default:
                return false;
        }
    }
    
    /** extract the qualifying module part
     */
    public Type qualifyingModule(Type type) {
        while (type != null)
            switch (type) {
                case ClassType(Type outer):
                    type = outer;
                    break;
                case XType.ModuleType(Type outer):
                    return type;
                default:
                    return null;
            }
        return type;
    }
    
    /** return an array of all dependent types
     */
    public Type[] dependentTypes(Type type) {
        if (type == null)
            return new Type[0];
        else if ((type.tdef().modifiers & CLASSFIELD) != 0) {
            Type[] res = ((XType.XClassType)type).depends();
           	if (res != null)
        		return res;
        }
        return new Type[0];
    }
    
    /** the subtype relationship
     */
    public boolean subtype(Type thiz, Type that) {
        if (!mainContext.algebraicClasses)
            return super.subtype(thiz, that);
        if (thiz == that)
            return true;
        else if ((thiz == null) || (that == null))
            return false;
        switch ((XType)that) {
            case ErrType:
            case AnyType:
                return true;
            case CompoundType(Type[] components):
                for (int i = 0; i < components.length; i++)
                    if (!subtype(thiz, components[i]))
                        return false;
                return true;
        }
        switch ((XType)thiz) {
            case ModuleType(_):
                return sametype(thiz, that);
            case ClassType(_):
           		//System.out.println("checking subtype " + thiz + " (" + thiz.supertype() + ") < " + that);
                if (sametype(thiz, that))
                    return true;
                if (isAlgebraicType(that)) {
                    if (isAlgebraicType(thiz))
                        return subtype(thiz.supertype(), that);
                    do {
                        thiz = thiz.supertype();
                    } while ((thiz != null) && !isAlgebraicType(thiz));
                    return subtype(that, thiz) || subtype(thiz, that);
                } else if (isClassField(thiz)) {
                    Type[] is = dependentTypes(thiz);
                    if (is != null)
                        for (int i = 0; i < is.length; i++)
                            if (subtype(is[i], that))
                                return true;
                } else if (subtype(thiz.supertype(), that))
                    return true;
                Type[] is = thiz.interfaces();
                for (int i = 0; i < is.length; i++)
                    if (subtype(is[i], that))
                        return true;
                return false;
            case CompoundType(Type[] components):
                for (int i = 0; i < components.length; i++)
                    if (subtype(components[i], that))
                        return true;
                return false;
        }
        return super.subtype(thiz, that);
    }
    
    /** the sametype relationship
     */
    public boolean sametype(Type thiz, Type that) {
        if (!mainContext.algebraicClasses)
            return super.sametype(thiz, that);
        if (thiz == that)
            return true;
        else if ((thiz == null) || (that == null))
            return false;
        switch (that) {
            case ErrType:
                return true;
        }
        switch ((XType)thiz) {
            case ModuleType(Type outer):
                switch ((XType)that) {
                    case ModuleType(Type thatouter):
                        XClassDef thizc = (XClassDef)thiz.tdef();
                        XClassDef thatc = (XClassDef)that.tdef();
                        return (thizc.refinementOf(thatc) ||
                                thatc.refinementOf(thizc)) &&
                               sametype(outer, thatouter);
                }
                break;
            case ClassType(Type thizo):
                if (isModule(thizo))
                    switch (that) {
                        case ClassType(Type thato):
                            XClassDef thizc = (XClassDef)thiz.tdef();
                            XClassDef thatc = (XClassDef)that.tdef();
                            if (((thizc.modifiers & CLASSFIELD) == 0) &&
                                ((thatc.modifiers & CLASSFIELD) == 0))
                                return super.sametype(thiz, that);
                            else if (sametype(thizo, thato))
                                return
                                    (mangler.unmangleShort(thizc.name, thizc.fullname) ==
                                     mangler.unmangleShort(thatc.name, thatc.fullname));
                    }
                else
                    return super.sametype(thiz, that);
                break;
            case CompoundType(_):
                return subtype(thiz, that) && subtype(that, thiz);
            default:
                return super.sametype(thiz, that);
        }
        return false;
    }
	
	/** the castable relationship
	 */
    public boolean castable(Type thiz, Type that) {
        if (!mainContext.algebraicClasses)
            return super.castable(thiz, that);
        switch ((XType)thiz) {
            case ErrType:
            case AnyType:
                return true;
            case CompoundType(Type[] components):
                for (int i = 0; i < components.length; i++)
                    if (!castable(components[i], that))
                        return false;
                return true;
        }
        switch ((XType)that) {
            case ModuleType(_):
                return sametype(thiz, that);
            case CompoundType(Type[] components):
                for (int i = 0; i < components.length; i++)
                    if (!castable(thiz, components[i]))
                        return false;
                return true;
            default:
                return super.castable(thiz, that);
        }
    }
    
    /** the original Java subtype relationship
     */
    public boolean javaSubtype(Type thi, Type that) {
    	boolean old = mainContext.algebraicClasses;
    	mainContext.algebraicClasses = false;
        boolean res = super.subtype(thi, that);
        mainContext.algebraicClasses = old;
        return res;
    }
}

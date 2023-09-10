//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    class definition
//                           
//  [XClassDef.java (5009) 9-May-01 18:14 -> 6-Jul-01 01:24]

package jaco.keris.struct;

import jaco.framework.*;
import jaco.keris.component.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import java.util.*;
import Definition.*;


public class XClassDef extends ClassDef implements XModifierConst {

    /** module data; this field is null for non-modules
     */
    public ModuleIntf modIntf;
    
    /** virtual class data; this field is null for other classes/modules
     */
    public VirtualClassIntf vcIntf;
    
    /** included fragments; this field is null for non-(classes | fragments)
     */
    public XClassDef[] includes;
    
    /** this is the base class of the definition; == this, if it is
     *  not an algebraic class
     */
    public XClassDef baseClass;

    /** the number of tags of an algebraic class or the tag of a case def
     */
    public int tag;

    /** the alternative class definition data
     */
    public Cache cache;
    public Scope defaultScope;
    
    
    public static class Cache {
        public int modifiers;
        public Type supertype;
        public Type[] interfaces;
        public Scope locals;
        
        Cache(XClassDef c) {
            this.modifiers = c.modifiers;
            this.supertype = null;//c.supertype;
            this.interfaces = new Type[0];//c.interfaces;
            if ((this.locals = c.defaultScope) == null)
                this.locals = new Scope(null, c);
            else
                c.defaultScope = null;
            //System.out.println("=== creating cache for " + c + " ===");
        }
    }
    
    /** constructor
     */
    public XClassDef(Name name, int mods, Type type, Definition owner) {
        super(null, new Type[0], null, null, null);
        this.kind = DefinitionConst.TYP;
        this.modifiers = mods;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.def = this;
        this.baseClass = this;
    }
    
    /** exchange current class definition data with the cache content
     */
    public void swap() {
        if (cache != null) {
            int mods = modifiers;
            Type supert = supertype;
            Type[] intfs = interfaces;
            Scope scope = locals;
            supertype = cache.supertype;
            interfaces = cache.interfaces;
            locals = cache.locals;
            modifiers = cache.modifiers;
            cache.supertype = supert;
            cache.interfaces = intfs;
            cache.locals = scope;
            cache.modifiers = mods;
            //System.out.println("--- swapping cache of " + this + ": " + modifiers + "/" + mods + ", " + supertype + "/" + supert + ", " + locals + "/" + scope + " ---");
        }
    }
    
    /** ensure that there is a cache
     */
    public void ensureCache() {
        if (cache == null)
            cache = new Cache(this);
    }
    
    /** ensure that there is a default scope cache
     */
    public Scope cacheLocals() {
        if (cache != null)
            return cache.locals;
        if (defaultScope == null)
            defaultScope = new Scope(null, this);
        return defaultScope;
    }
    
    /** return a set of all modules for which this is more specific
     */
    public XClassDef[] subsumes() {
        HashSet set = new HashSet();
        subsumes(set);
        XClassDef[] res = new XClassDef[set.size()];
        set.toArray(res);
        return res;
    }
    
    public XClassDef[] subsumesAdditional(XClassDef[] cs) {
        HashSet set = new HashSet();
        subsumes(set);
        for (int i = 0; i < cs.length; i++)
            cs[i].subsumes(set);
        XClassDef[] res = new XClassDef[set.size()];
        set.toArray(res);
        return res;
    }
    
    private void subsumes(Set all) {
        Definition c = this;
        while (((c.modifiers & MODULE) != 0) && !all.contains(c)) {
            all.add(c);
            if ((c.modifiers & (INTERFACE | SPECIALIZES)) == (INTERFACE | SPECIALIZES))
                break;
            Type[] intf = c.interfaces();
            for (int i = 0; i < intf.length; i++)
                ((XClassDef)intf[i].tdef().complete()).subsumes(all);
            if ((c.modifiers & SPECIALIZES) != 0)
                break;
            c = c.supertype().tdef().complete();
        }
    }
    
    /** are the interfaces of this module and that module disjoint?
     */
    public boolean disjoint(XClassDef that) {
        return disjoint(this, collect(that, new HashSet()));
    }
    
    private static Set collect(Definition c, Set that) {
        c = c.complete();
        while ((c.modifiers & MODULE) != 0) {
            that.add(c);
            if ((c.modifiers & (INTERFACE | SPECIALIZES)) == (INTERFACE | SPECIALIZES))
                break;
            Type[] intf = c.interfaces();
            for (int i = 0; i < intf.length; i++)
                collect((XClassDef)intf[i].tdef(), that);
            if ((c.modifiers & SPECIALIZES) != 0)
                break;
            c = c.supertype().tdef().complete();
        }
        return that;
    }
    
    private static boolean disjoint(Definition c, Set that) {
        c = c.complete();
        while ((c.modifiers & MODULE) != 0) {
            if (that.contains(c))
                return false;
            if ((c.modifiers & (INTERFACE | SPECIALIZES)) == (INTERFACE | SPECIALIZES))
                return true;
            Type[] intf = c.interfaces();
            for (int i = 0; i < intf.length; i++)
                if (!disjoint(intf[i].tdef(), that))
                    return false;
            if ((c.modifiers & SPECIALIZES) != 0)
                return true;
            c = c.supertype().tdef().complete();
        }
        return true;
    }
    
    /** is this module a refinement of that module?
     */
    public boolean refinementOf(XClassDef that) {
        that = (XClassDef)that.complete();
        if (((this.modifiers & MODULE) == 0) ||
            ((that.modifiers & MODULE) == 0))
            return false;
        Definition c = this;
        while ((c.modifiers & MODULE) != 0) {
            if (c == that)
                return true;
            if ((c.modifiers & (INTERFACE | SPECIALIZES)) == (INTERFACE | SPECIALIZES))
                return false;
            if ((that.modifiers & INTERFACE) != 0) {
                Type[] intf = c.interfaces();
                for (int i = 0; i < intf.length; i++)
                    if (((XClassDef)intf[i].tdef().complete()).refinementOf(that))
                        return true;
                if ((c.modifiers & INTERFACE) != 0)
                    return false;
            }
            if ((c.modifiers & SPECIALIZES) != 0)
                return false;
            c = c.supertype().tdef().complete();
        }
        return false;
    }
    
    /** is this module a specialized version of that module?
     */
    public boolean specializationOf(XClassDef that) {
        that = (XClassDef)that.complete();
        // filter out identity and non-module cases
        if ((this == that) ||
            ((this.modifiers & MODULE) == 0) ||
            ((that.modifiers & MODULE) == 0))
            return false;
        Definition c = this;
        // handle interfaces
        if ((this.modifiers & INTERFACE) != 0) {
            Type[] intf = this.interfaces();
            if ((this.modifiers & SPECIALIZES) == 0)
                for (int i = 0; i < intf.length; i++)
                    if (((XClassDef)intf[i].tdef().complete()).specializationOf(that))
                        return true;
            else
                for (int j = 0; j < intf.length; j++)
                    if (((XClassDef)intf[j].tdef().complete()).extensionOf(that))
                        return true;
            return false;
        // handle true modules
        } else {
            while (((c.modifiers & MODULE) != 0) &&
                   ((c.modifiers & SPECIALIZES) == 0))
                c = c.supertype().tdef().complete();
            if (((c.modifiers & MODULE) == 0) || (c == that))
                return false;
            return ((XClassDef)c).extensionOf(that);
        }
    }
    
    /** is this module an extension of that module?
     */
    public boolean extensionOf(XClassDef that) {
        that = (XClassDef)that.complete();
        if (((this.modifiers & MODULE) == 0) ||
            ((that.modifiers & MODULE) == 0))
            return false;
        Definition c = this;
        while ((c.modifiers & MODULE) != 0) {
            if (c == that)
                return true;
            if ((that.modifiers & INTERFACE) != 0) {
                Type[] intf = c.interfaces();
                for (int i = 0; i < intf.length; i++)
                    if (((XClassDef)intf[i].tdef().complete()).extensionOf(that))
                        return true;
                if ((c.modifiers & INTERFACE) != 0)
                    return false;
            }
            c = c.supertype().tdef().complete();
        }
        return false;
    }
    
}

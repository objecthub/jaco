//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    module relevant part of XClassDefs
//                           
//  [ModuleIntf.java (8802) 16-May-01 17:11 -> 8-Jun-01 01:38]

package jaco.keris.struct;

import java.util.*;
import jaco.java.struct.*;
import jaco.keris.component.*;
import jaco.framework.*;


public class ModuleIntf implements XModifierConst {
    
    /** reference to the own class definition
     */
    public XClassDef self;
    
    /** the super module if self specializes it
     */
    public ModuleSet specialized;
    
    /** all required modules (maps required modules to overridden modules)
     */
    public ModuleMap required;

    /** all contained modules (maps contained modules to overridden modules)
     */
    public ModuleMap contained;
    
    /** all contained modules (maps contained modules to modifiers)
     */
    public ModuleMap containedMods;
    
    /** maps overridden modules to overriding modules
     */
    public ModuleMap overriddenRequired;

    /** maps overridden modules to overriding modules
     */
    public ModuleMap overriddenContained;
    
    /** all imported modules
     */
    public ModuleMap imported;
    public TypeSet starImported;
    public HashMap starImportedTrees;
    
    /** the modules structure of the supermodule
     */
    public ModuleIntf parent;
    
    /** constraints on types (maximal set of implemented interfaces)
     */
    public TypeMap maxInterfaces;
    
    
    public ModuleIntf(XClassDef self) {
        this.self = self;
        this.required = new ModuleMap();
        this.contained = new ModuleMap();
        this.containedMods = new ModuleMap();
        this.overriddenRequired = new ModuleMap();
        this.overriddenContained = new ModuleMap();
        this.imported = new ModuleMap();
        this.starImportedTrees = new HashMap();
        if ((self.modifiers & SPECIALIZES) != 0) {
            this.specialized = new ModuleSet();
            if ((self.modifiers & INTERFACE) == 0)
                this.specialized.add((XClassDef)self.supertype().tdef());
            else {
                Type[] intf = self.interfaces();
                for (int i = 0; i < intf.length; i++)
                    this.specialized.add((XClassDef)intf[i].tdef());
            }
        }
        self.modIntf = this;
    }
    
    public ModuleIntf(XClassDef self,
                      ModuleSet specialized,
                      ModuleMap required,
                      ModuleMap contained,
                      ModuleMap containedMods,
                      ModuleMap overriddenRequired,
                      ModuleMap overriddenContained,
                      ModuleMap imported,
                      TypeMap maxInterfaces) {
        this.self = self;
        this.specialized = specialized;
        this.required = required;
        this.contained = contained;
        this.containedMods = containedMods;
        this.overriddenRequired = overriddenRequired;
        this.overriddenContained = overriddenContained;
        this.imported = imported;
        this.maxInterfaces = maxInterfaces;
        if (specialized != null)
            self.modifiers |= SPECIALIZES;
        self.modIntf = this;
    }
    
    public void setParent(ModuleIntf modules) {
        this.parent = modules;
    }
    
    /** add a requirement to the module
     */
    public boolean requires(XClassDef mod) {
        // check if self captures already mod
        if (self.refinementOf(mod))
            return true;
        if (!self.disjoint(mod))
            return false;
        // check if mod is already contained
        if (contained.contains(mod) ||
            contained.subsumes(mod) ||
            !contained.consistent(mod))
            return false;
        // we are finished if mod is already required
        if (!required.contains(mod)) {
            // check if adding the module would keep consistency
            if (!required.consistent(mod))
                return false;
            // make a union of all subsumed overridden modules
            Object[] overridden = required.getSubsumed(mod);
            ModuleSet res = new ModuleSet();
            for (int i = 0; i < overridden.length; i++)
                res.add((ModuleSet)overridden[i]);
            // remove all subsumed modules
            required.put(mod, res);
        }
        return true;
    }
    
    /** add a rewiring requirement to the module
     */
    public boolean requires(XClassDef mod, XClassDef over) {
        // check if self captures already mod
        if (!self.refinementOf(mod) && !self.disjoint(mod))
            return false;
        // check if over is more specific than mod or vice versa
        if (over.refinementOf(mod) || mod.refinementOf(over))
            return false;
        // check if mod is already contained
        if (contained.contains(mod) ||
            contained.subsumes(mod) ||
            !contained.consistent(mod))
            return false;
        // check if over is already overridden with different
        // module c
        XClassDef c = (XClassDef)overriddenRequired.get(over);
        if (c == null) {
            Object[] overriding = overriddenRequired.getSubsumed(over);
            ModuleSet overridingSet = new ModuleSet();
            for (int i = 0; i < overriding.length; i++)
                overridingSet.add((XClassDef)overriding[i]);
            overridingSet.add(mod);
            if (overridingSet.size() != 1)
                return false;
            overriddenRequired.put(over, overridingSet.get(mod));
        } else if (mod.refinementOf(c))
            overriddenRequired.put(over, mod);
        else if (!c.refinementOf(mod))
            return false;
        // add required module
        if (required.contains(mod)) {
            ModuleSet overridden = (ModuleSet)required.get(mod);
            overridden.add(over);
        } else {
            // check if adding the module would keep consistency
            if (!required.consistent(mod))
                return false;
            // make a union of all subsumed overridden modules
            Object[] overridden = required.getSubsumed(mod);
            ModuleSet res = new ModuleSet();
            for (int i = 0; i < overridden.length; i++)
                res.add((ModuleSet)overridden[i]);
            // add newly overridden module
            res.add(over);
            // remove all subsumed modules
            required.put(mod, res);
        }
        return true;
    }
    
    private Integer normalizeMods(int mods) {
        if ((mods & (PRIVATE | PROTECTED)) == 0)
            mods |= PUBLIC;
        return new Integer(mods);
    }
    
    /** add a submodule
     */
    public boolean contains(XClassDef mod, int mods) {
        // check if mod is compatible to the self module
        if (!self.disjoint(mod))
            return false;
        // check if mod is already required
        if (required.contains(mod) ||
            required.subsumes(mod) ||
            !required.consistent(mod))
            return false;
        // check that mod is not already (partly) contained
        if (contained.contains(mod) ||
            contained.subsumes(mod) ||
            !contained.consistent(mod))
            return false;
        // enter mod to contained set
        contained.put(mod, new ModuleSet());
        containedMods.put(mod, normalizeMods(mods));
        return true;
    }
    
    /** add a rewiring submodule
     */
    public boolean contains(XClassDef mod, XClassDef[] over, int mods) {
        // check if mod is compatible to the self module
        if (!self.disjoint(mod))
            return false;
        // check if overs are not more specific than mod
        for (int i = 0; i < over.length; i++)
            if (over[i].refinementOf(mod) || mod.refinementOf(over[i]))
                return false;
        // check if mod is already required
        if (required.contains(mod) ||
            required.subsumes(mod) ||
            !required.consistent(mod))
            return false;
        // check if over is already overridden
        for (int i = 0; i < over.length; i++) {
            if (overriddenContained.contains(over[i]) ||
                overriddenContained.subsumes(over[i]))
                return false;
            overriddenContained.put(over[i], mod);
        }
        // check if mod is already contained
        if (contained.contains(mod) ||
            contained.subsumes(mod) ||
            !contained.consistent(mod))
            return false;
        // add mod to contained set
        ModuleSet res = new ModuleSet();
        for (int i = 0; i < over.length; i++)
            res.add(over[i]);
        contained.put(mod, res);
        containedMods.put(mod, normalizeMods(mods));
        return true;
    }
    
    public boolean contains(XClassDef mod, XClassDef over, int mods) {
        return contains(mod, new XClassDef[]{over}, mods);
    }
    
    /** get the modifiers of a submodule
     */
    public int modsOfContained(XClassDef mod) {
        //return ((Integer)containedMods.get(mod)).intValue();
        return 0;
    }
    
    /** set the modifiers of a submodule
     */
    public void setModsOfContained(XClassDef mod, int mods) {
        containedMods.put(mod, normalizeMods(mods));
    }
    
    /** import deeply nested modules
     */
    public boolean imports(XClassDef mod, Type tpe) {
        // check if mod is compatible to the self module
        if (!self.disjoint(mod))
            return false;
        // check if mod is already required
        if (required.contains(mod) ||
            required.subsumes(mod) ||
            !required.consistent(mod))
            return false;
        // check if mod is already contained
        if (contained.contains(mod) ||
            contained.subsumes(mod) ||
            !contained.consistent(mod))
            return false;
        // check if mod is already imported
        if (imported.contains(mod) ||
            imported.subsumes(mod) ||
            !imported.consistent(mod))
            return false;
        // add mod to imported set
        imported.put(mod, tpe);
        return true;
    }
    
    /** import deeply nested modules
     */
    public boolean starimports(Type tpe, Object obj) {
    	// check that the same type does not get imported twice
    	if (starImported.contains(tpe))
    		return false;
        // add mod to imported set
        starImported.add(tpe);
        starImportedTrees.put(tpe, obj);
        return true;
    }
    
    /** check if the context defines module 'mod'
     */
    public boolean provides(XClassDef mod) {
        return self.refinementOf(mod) ||
               required.contains(mod) ||
               contained.contains(mod);
    }
    
    /** check if the context defines all modules of  'reqs'
     */
    public XClassDef provides(ModuleSet reqs) {
        XClassDef[] rcs = reqs.keys();
        for (int i = 0; i < rcs.length; i++)
            if (!self.refinementOf(rcs[i]) &&
                !required.contains(rcs[i]) &&
                !contained.contains(rcs[i]))
                return rcs[i];
        return null;
    }
    
    /** check if all modules of  'reqs' are submodules
     */
    public XClassDef containsSome(ModuleSet reqs) {
        XClassDef[] rcs = reqs.keys();
        for (int i = 0; i < rcs.length; i++)
            if (contained.contains(rcs[i]))
                return rcs[i];
        return null;
    }
    
    public String toString() {
        return self + " requires " + required + ", contains " + contained;
    }
}

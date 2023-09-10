//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    module sets
//                           
//  [ModuleSet.java (3119) 16-May-01 16:58 -> 9-Jul-01 01:39]

package jaco.keris.struct;

import java.util.*;
import jaco.java.struct.*;


/** a class for representing sets of modules
 */
public class ModuleSet {

    public XClassDef[] modules;
    int num;
    
    /** constructors
     */
    public ModuleSet() {
        this.modules = new XClassDef[8];
        this.num = 0;
    }
    
    public ModuleSet(XClassDef[] modules) {
        this.modules = modules;
        this.num = modules.length;
    }
    
    public ModuleSet(ModuleSet set) {
        this.modules = new XClassDef[(set.num == 0) ? 2 : set.num];
        this.num = set.num;
        System.arraycopy(set.modules, 0, this.modules, 0, this.num);
    }
    
    protected void extend() {
        if (num == modules.length) {
            if (num == 0)
                modules = new XClassDef[2];
            else {
                XClassDef[] newmodules = new XClassDef[modules.length * 2];
                System.arraycopy(modules, 0, newmodules, 0, modules.length);
                modules = newmodules;
            }
        }
    }
    
    /** return the (normalized) module for 'mod' or null if there is no
     *  module that subsumes 'mod'
     */
    public Object get(XClassDef mod) {
        for (int i = 0; i < num; i++)
             if (modules[i].refinementOf(mod))
                return modules[i];
        return null;
    }
    
    /** check if 'mod' is contained in this set
     */
    public boolean contains(XClassDef mod) {
        for (int i = 0; i < num; i++)
             if (modules[i].refinementOf(mod))
                return true;
        return false;
    }
    
    /** check if 'mod' subsumes a module of this set
     */
    public boolean subsumes(XClassDef mod) {
        for (int i = 0; i < num; i++)
             if (mod.refinementOf(modules[i]))
                return true;
        return false;
    }
    
    /** check that the given module 'mod' does not get subsumed
     *  by this set, that it does not subsume a module of this set
     *  and that it does not share an implemented module interface
     *  with another module of this set
     */
    public boolean consistent(XClassDef mod) {
        for (int i = 0; i < num; i++)
            if (!mod.refinementOf(modules[i]) &&
                !modules[i].refinementOf(mod) &&
                !mod.disjoint(modules[i]))
                return false;
        return true;
    }
    
    /** include the given module in this set
     */
    public void add(XClassDef mod) {
        if (mod == null)
            return;
        int del = 0;
        for (int i = 0; i < num; i++)
            if (modules[i].refinementOf(mod))
                return;
            else if (mod.refinementOf(modules[i])) {
                modules[i] = mod;
                del++;
            }
        if (del == 1)
            return;
        if (del > 1) {
            int j = 0;
            for (int i = 0; i < num; i++)
                if (modules[i] != mod) {
                    modules[j] = modules[i];
                    j++;
                }
            num = j;
        } else
            extend();
        modules[num++] = mod;
    }
    
    /** include the given set in this modules
     */
    public void add(ModuleSet modSet) {
        for (int i = 0; i < modSet.num; i++)
            add(modSet.modules[i]);
    }
    
    /** create a new set representing the union of this set with
     *  the given set
     */
    public ModuleSet union(ModuleSet set) {
        set = new ModuleSet(set);
        set.add(this);
        return set;
    }
    
    /** return the intersection of the two sets; the intersection
     *  contains the most refined version of common modules
     */
    public ModuleSet intersect(ModuleSet other) {
        ModuleSet res = new ModuleSet();
        for (int i = 0; i < num; i++)
            if (other.contains(modules[i]))
                res.add(modules[i]);
        for (int i = 0; i < other.num; i++)
            if (this.contains(other.modules[i]))
                res.add(other.modules[i]);
        return res;
    }
    
    /** return a set of modules that are required by at least
     *  one of the modules of this set
     */
    public ModuleSet required() {
        ModuleSet res = new ModuleSet();
        for (int i = 0; i < num; i++)
            res.add(modules[i].modIntf.required);
        return res;
    }
    
    /** return a set of modules that are required by at least
     *  one of the modules of this set
     */
    public ModuleSet modulesOverridingRequired(XClassDef c) {
        ModuleSet res = new ModuleSet();
        for (int i = 0; i < num; i++)
            res.add((XClassDef)modules[i].modIntf.overriddenRequired.get(c));
        return res;
    }
    
    /** return a set representing the closure of this module
     */
    public ModuleSet closure() {
        return closureUntil(null);
    }
    
    /** return a set representing the closure of this module
     *  excluding modules in set 'barrier'
     */
    public ModuleSet closureUntil(ModuleSet barrier) {
        ModuleSet res = new ModuleSet();
        for (int i = 0; i < num; i++)
            closure(modules[i], res, barrier);
        return res;
    }
    
    protected void closure(XClassDef c, ModuleSet res, ModuleSet barrier) {
        if (((barrier != null) && barrier.contains(c)) || res.contains(c))
            return;
        res.add(c);
        ModuleSet reqSet = c.modIntf.required;
        for (int i = 0; i < reqSet.num; i++)
            closure(reqSet.modules[i], res, barrier);
    }
    
    /** return the first element
     */
    public XClassDef first() {
        return modules[0];
    }
    
    /** the number of modules in this set
     */
    public int size() {
        return num;
    }
    
    /** checks if the set is empty
     */
    public boolean isEmpty() {
        return num == 0;
    }
    
    /** return the content of this set in form of an array
     */
    public XClassDef[] keys() {
        XClassDef[] keys = new XClassDef[num];
        System.arraycopy(modules, 0, keys, 0, num);
        return keys;
    }
    
    /** return a string representation of this set
     */
    public String toString() {
        if (num == 0)
            return "{}";
        if (num == 1)
            return "{" + modules[0] + "}";
        String res = "{" + modules[0];
        for (int i = 1; i < num; i++)
            res += ", " + modules[i];
        return res + "}";
    }
}

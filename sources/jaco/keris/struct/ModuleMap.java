//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    module maps
//                           
//  [ModuleMap.java (3562) 16-May-01 16:59 -> 8-Jun-01 01:37]

package jaco.keris.struct;

import java.util.*;
import jaco.java.struct.*;


public class ModuleMap extends ModuleSet {

    public Object[] content = new Object[8];
    
    public ModuleMap() {
        super();
        this.content = new Object[modules.length];
    }
    
    public ModuleMap(XClassDef[] modules, Object[] content) {
        super(modules);
        this.content = content;
    }
    
    
    protected void extend() {
        if (num == modules.length) {
            XClassDef[] newmodules = new XClassDef[modules.length * 2];
            System.arraycopy(modules, 0, newmodules, 0, modules.length);
            Object[] newcontent = new Object[modules.length * 2];
            System.arraycopy(content, 0, newcontent, 0, modules.length);
            modules = newmodules;
            content = newcontent;
        }
    }
    
    public XClassDef key(XClassDef mod) {
        for (int i = 0; i < num; i++)
             if (modules[i].refinementOf(mod))
                return modules[i];
        return null;
    }
    
    public Object get(XClassDef mod) {
        for (int i = 0; i < num; i++)
             if (modules[i].refinementOf(mod))
                return content[i];
        return null;
    }
    
    public Object[] getSubsumed(XClassDef mod) {
        Set set = new HashSet();
        for (int i = 0; i < num; i++)
             if (mod.refinementOf(modules[i]))
                set.add(content[i]);
        return set.toArray();
    }
    
    public void put(XClassDef mod, Object cont) {
        int del = 0;
        for (int i = 0; i < num; i++)
            if (modules[i].refinementOf(mod))
                return;
            else if (mod.refinementOf(modules[i])) {
                modules[i] = mod;
                content[i] = cont;
                del++;
            }
        if (del == 1)
            return;
        if (del > 1) {
            int j = 0;
            for (int i = 0; i < num; i++)
                if (modules[i] != mod) {
                    modules[j] = modules[i];
                    content[j] = content[i];
                    j++;
                }
            num = j;
        } else
            extend();
        modules[num] = mod;
        content[num++] = cont;
    }
    
    public void put(ModuleSet modSet, Object cont) {
        for (int i = 0; i < modSet.num; i++)
            put(modSet.modules[i], cont);
    }
    
    public void remove(XClassDef mod) {
        for (int i = 0; i < num; i++)
            if (modules[i].refinementOf(mod)) {
                System.arraycopy(modules, i + 1, modules, i, num - i - 1);
                System.arraycopy(content, i + 1, content, i, num - i - 1);
                num--;
                return;
            }
    }
    
    public void remove(ModuleSet modSet) {
        for (int i = 0; i < modSet.num; i++)
            remove(modSet.modules[i]);
    }
    
    public Object[] content() {
    	return content(new Object[num]);
    }
    
    public Object[] content(Object[] res) {
    	System.arraycopy(content, 0, res, 0, num);
    	return res;
    }
    
    public String toString() {
        if (num == 0)
            return "[]";
        if (num == 1)
            return "[" + modules[0] + " -> " + content[0] + "]";
        String res = "[" + modules[0] + " -> " + content[0];
        for (int i = 1; i < num; i++)
            res += ", " + modules[i] + " -> " + content[i];
        return res + "]";
    }
}

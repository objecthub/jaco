//      /   _ _      JaCo
//  \  //\ / / \     - statistics module
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.stat.context.*;
import java.util.*;


public class Stat extends jaco.java.component.Compiler implements DefinitionConst, PizzaModifierConst
{
    public Name packname;
    public int numlines;
    public int numbytes;
    public int numparserlines;
    public int numparserbytes;
    public int numsources;
    public int numclasses;
    public int extclasses;
    public int numalgebraic;
    public int numvariants;
    public int numoveralgebraic;
    public int numovervariants;
    public int numnewvariants;
    public int nummethods;
    public int numovermethods;
    public int numswitches;
    public int numintswitches;
    public int numoverswitches;
    public int numswitchmeth;
    public int numoverswitchmeth;
    public int numoverswitchnew;
    
    public int extendedswitches;
    public int superswitches;
    public int oversuperswitches;
    public int extendedmethods;
    public int supermethods;
    public int oversupermethods;
    
    public Hashtable extended = new Hashtable();
    public Hashtable semethods = new Hashtable();
    
    
    Stat(String packname) {
        this.packname = Name.fromString(packname);
    }
    
    public static Stat[] stats = {
        new Stat("jaco.java"),
        new Stat("jaco.pizza"),
        new Stat("jaco.cjava"),
        new Stat("jaco.sjava"),
        new Stat("jaco.framework")
    };
    
    public static Hashtable classes = new Hashtable();
    public static Hashtable cases = new Hashtable();
    public static Hashtable algebraics = new Hashtable();
    public static Hashtable methods = new Hashtable();
    public static Hashtable smethods = new Hashtable();
    public static Hashtable overrides = new Hashtable();
    public static boolean algebraicSetup = false;
    
    static Stat getStat(Definition def) {
        boolean cancel = false;
        while (def.kind != PCK) {
            if (def.fullname.toString().endsWith("Parser"))
                cancel = true;
            def = def.owner;
        }
        for (int i = 0; i < stats.length; i++)
            if (def.fullname.toString().startsWith(
                stats[i].packname.toString())) {
                if ((i == stats.length - 1) && cancel)
                    return null;
                else
                    return stats[i];
            }
        return null;
    }
    
    public static void addLines(Definition def, String sourcename, int lines, int bytes) {
        Stat s = getStat(def);
        if (s != null) {
            s.numlines += lines;
            s.numbytes += bytes;
            s.numsources++;
            if (sourcename.endsWith("Parser.java")) {
                s.numparserlines = lines;
                s.numparserbytes = bytes;
            }
        }
    }
    
    public static void addClass(Definition def) {
        Stat s = getStat(def);
        if (s != null) {
            if (classes.get(def) == null) {
                s.numclasses++;
                System.out.println("** " + def);//DEBUG
            }
        }
        classes.put(def, def);
    }
    
    public static void addCase(Definition def) {
        Stat s = getStat(def);
        if (s != null) {
            if (cases.get(def) == null) {
                s.numvariants++;
                Integer val = (Integer)algebraics.get(def.owner);
                if (val == null)
                    algebraics.put(def.owner, new Integer(1));
                else
                    algebraics.put(def.owner, new Integer(1 + val.intValue()));
            }
        }
        cases.put(def, def);
    }
    
    public static void addMethod(Definition def) {
        Stat s = getStat(def);
        if (s != null) {
            if (methods.get(def) == null)
                s.nummethods++;
        }
        methods.put(def, def);
    }
    
    public static void addSwitch(Definition def, Definition tdef) {
        if (!algebraicSetup)
            setupAlgebraic();
        Stat s = getStat(def);
        if (s != null) {
            if ((tdef.modifiers & ALGEBRAIC) == 0) {
                s.numintswitches++;
                return;
            }
            boolean newmeth = (smethods.get(def) == null);
            if (newmeth)
                s.numswitchmeth++;
            s.numswitches++;
            if (s.extended.get(tdef) != null) {
                s.extendedswitches++;
                if (newmeth)
                    s.extendedmethods++;
            }
            if (s == stats[0])
                for (int i = 1; i < stats.length; i++)
                    if (stats[i].extended.get(tdef) != null) {
                        stats[i].superswitches++;
                        if (newmeth)
                            stats[i].supermethods++;
                        Integer val = (Integer)stats[i].semethods.get(def);
                        if (val == null)
                            stats[i].semethods.put(def, new Integer(1));
                        else
                            stats[i].semethods.put(def, new Integer(val.intValue() + 1));
                    }
        }
        Integer val = (Integer)smethods.get(def);
        if (val == null)
            smethods.put(def, new Integer(1));
        else
            smethods.put(def, new Integer(val.intValue() + 1));
    }
    
    public static void addOverridden(Types types, Definition f, Definition overridden) {
        Stat sf = getStat(f);
        if (sf != null) {
            Stat so = getStat(overridden);
            if (so != null) {
                Definition old = (Definition)overrides.get(f);
                if (old == null)
                    overrides.put(f, overridden);
                else if (overridden.owner == old.owner) {
                } else if (types.subtype(overridden.owner.type, old.owner.type)) {
                    overrides.put(f, overridden);
                    addOverridden(types, overridden, old);
                } else
                    addOverridden(types, old, overridden);
            }
        }
    }
    
    public static void setupAlgebraic() {
        Definition[] cs = (Definition[])classes.keySet().toArray(
            new Definition[classes.size()]);
        for (int i = 0; i < cs.length; i++) {
            Stat s = getStat(cs[i]);
            if ((s != null) && ((cs[i].modifiers & ALGEBRAIC) != 0)) {
                s.numalgebraic++;
                s.numclasses--;
                if ((cs[i].type.supertype().tdef().modifiers & ALGEBRAIC) != 0) {
                    s.numoveralgebraic++;
                    s.extended.put(cs[i].type.supertype().tdef(), cs[i]);
                    s.extended.put(cs[i], cs[i]);
                    Integer val = (Integer)algebraics.get(cs[i].type.supertype().tdef());
                    if (val != null) {
                        s.numovervariants += val.intValue();
                        val = (Integer)algebraics.get(cs[i]);
                        if (val != null)
                            s.numnewvariants += val.intValue();
                    }
                }
            } else if (s != null) {
                Stat s1 = getStat(cs[i].type.supertype().tdef());
                if (s1 != null)
                    s.extclasses++;
            }
        }
        algebraicSetup = true;
    }
    
    public static void finish() {
        Definition[] fs = (Definition[])overrides.keySet().toArray(
            new Definition[overrides.size()]);
        for (int i = 0; i < fs.length; i++) {
            Definition o = (Definition)overrides.get(fs[i]);
            Stat sf = getStat(fs[i]);
            Stat so = getStat(o);
            Integer val = (Integer)smethods.get(o);
            sf.numovermethods++;
            if (val != null) {
                sf.numoverswitches += val.intValue();
                sf.numoverswitchmeth++;
            }
            val = (Integer)sf.semethods.get(o);
            if (val != null) {
                sf.oversuperswitches += val.intValue();
                sf.oversupermethods++;
            }
            /* if (sf == stats[2]) {
                if ((smethods.get(fs[i]) != null) || (smethods.get(o) != null))
                System.out.println(fs[i] + " of " + fs[i].owner + " (" + smethods.get(fs[i]) +
                                   ") overrides " + o + " of " + o.owner + " (" + smethods.get(o) + ")");
            } */
        }
    }
    
    public static void printStatistics() {
        finish();
        for (int i = 0; i < stats.length; i++)
            if (stats[i].numsources > 0) {
                System.out.println("=== " + stats[i].packname + " ==========================================");
                System.out.println("  sourcefiles = " + stats[i].numsources);
                System.out.println("  lines = " + stats[i].numlines + " (" + (stats[i].numlines - stats[i].numparserlines) + ")");
                System.out.println("  bytes = " + stats[i].numbytes + " (" + (stats[i].numbytes - stats[i].numparserbytes) + ")");
                System.out.println("  classes = " + stats[i].numclasses);
                System.out.println("  extended classes = " + stats[i].extclasses);
                System.out.println("  algebraic classes = " + stats[i].numalgebraic);
                System.out.println("  variants = " + stats[i].numvariants);
                System.out.println("  extended algebraic classes = " + stats[i].numoveralgebraic);
                System.out.println("  variants of extended algebraic classes = " + stats[i].numovervariants);
                System.out.println("  new variants of extended algebraic classes = " + stats[i].numnewvariants);
                System.out.println("  methods = " + stats[i].nummethods);
                System.out.println("  overridden methods = " + stats[i].numovermethods);
                System.out.println("  methods with switches = " + stats[i].numswitchmeth);
                System.out.println("  overridden methods with switches = " + stats[i].numoverswitchmeth);
                System.out.println("  int switches = " + stats[i].numintswitches);
                System.out.println("  switches = " + stats[i].numswitches);
                System.out.println("  overridden switches = " + stats[i].numoverswitches);
                System.out.println("  methods with switches of extended types = " + stats[i].extendedmethods);
                System.out.println("  methods with switches of extended types in super system = " + stats[i].supermethods);
                System.out.println("  overridden methods with switches of extended types = " + stats[i].oversupermethods);
                System.out.println("  switches of extended types = " + stats[i].extendedswitches);
                System.out.println("  switches of extended types in super system = " + stats[i].superswitches);
                System.out.println("  overridden switches of extended types = " + stats[i].oversuperswitches);

            }
    }
}

//      /   _ _      JaCo
//  \  //\ / / \     - java modifiers support
//   \//  \\_\_/     
//         \         Matthias Zenger, 19/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;


public interface ModifierConst
{
/** modifier masks
 */
    int LANGUAGE_MODS   = 0xffff;
    int INTERNAL_MODS   = 0xffff0000;

/** java modifiers
 */
    int PUBLIC          = 0x0001;
    int PRIVATE         = 0x0002;
    int PROTECTED       = 0x0004;
    int STATIC          = 0x0008;
    int FINAL           = 0x0010;
    int SYNCHRONIZED    = 0x0020;
    int VOLATILE        = 0x0040;
    int TRANSIENT       = 0x0080;
    int NATIVE          = 0x0100;
    int INTERFACE       = 0x0200;
    int ABSTRACT        = 0x0400;
    int STRICTFP        = 0x0800;
    
/** additional classfile access flag
 */
    int ACC_SUPER       = 0x0020;

/** non-standard modifiers
 */
    /** symbol is compiler generated */
    int SYNTHETIC       = 0x10000;
    
    /** symbol is deprecated */
    int DEPRECATED      = 0x20000;
    
    /** method represents class-level block */
    int BLOCK           = 0x40000;
    
    /** block represents a sequence of trees
     */
    int CONTAINER       = 0x40000;

/** non-standard modifiers for variables
 */
    /** variable is captured by inner class */
    int CAPTURED        = 0x40000;
    
    /** variable is used */
    int USED            = 0x80000;
    
    int BRIDGE          = 0x800000;
}


public class Modifiers extends Component implements ModifierConst
{
/** other components
 */
    ErrorHandler    report;
    
/** modifier masks
 */
    public int AccessMods           = PUBLIC | PROTECTED | PRIVATE;
    public int LocalClassMods       = FINAL | INTERFACE | ABSTRACT | STRICTFP;
    public int InnerClassMods       = LocalClassMods | AccessMods | STATIC;
    public int ClassMods            = LocalClassMods | PUBLIC;
    public int LocalVarMods         = FINAL;
    public int InterfaceVarMods     = FINAL | STATIC | PUBLIC;
    public int VarMods              = AccessMods | FINAL | STATIC | VOLATILE
                                    | TRANSIENT;
    public int InterfaceMethMods    = ABSTRACT | PUBLIC;
    public int ConstrMods           = AccessMods;
    public int MethMods             = AccessMods | ABSTRACT | STATIC | NATIVE
                                    | SYNCHRONIZED | FINAL | STRICTFP;

/** number of modifiers
 */
    public int      modcount = 12;

/** modifier strings
 */
    public String[] modstring;


/** component name
 */
    public String getName()
    {
        return "JavaModifiers";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        report = context.ErrorHandler();

        modstring = new String[modcount];
        modstring[0]  = "public";
        modstring[1]  = "private";
        modstring[2]  = "protected";
        modstring[3]  = "static";
        modstring[4]  = "final";
        modstring[5]  = "synchronized";
        modstring[6]  = "volatile";
        modstring[7]  = "transient";
        modstring[8]  = "native";
        modstring[9]  = "interface";
        modstring[10] = "abstract";
        modstring[11] = "strictfp";
    }

/** return all modifiers in mods as a string, separated by whitespaces
 *  if nonempty
 */
    public String toString(int mods)
    {
        String  str = "";
        int     mask = 1, i = 0;
        
        mods &= LANGUAGE_MODS;
        while (mods != 0)
        {
            if ((mods & mask) != 0)
            {
                if (i < modcount)
                    str += modstring[i] + " ";
                else
                    str += "<MODIFIER#" + mask + "> ";
                mods ^= mask;
            }
            mask <<= 1;
            i++;
        }
        return (str == null) ? "" : str;
    }
    
/** return first modifier in mods, or 0 if mods is empty
 */
    public int firstMod(int mods)
    {
        if (mods == 0)
            return 0;
        else
        {
            int i = 0;
            while ((i < modcount) && ((mods & (1 << i)) == 0))
                i++;
            return i;
        }
    }

/** the level of access protection given by modifier mods.
 */
    public int protection(int mods)
    {
        switch (mods & AccessMods)
        {
            case PRIVATE:
                return 3;
            
            case 0:
                return 2;
            
            case PROTECTED:
                return 1;
            
            case PUBLIC:
                return 0;
            
            default:
                throw new InternalError();
        }
    }

/** the name of access protection given by modifier mods.
 */
    public String protectionString(int mods)
    {
        switch (mods & AccessMods)
        {
            case PRIVATE:
                return "private";
            
            case 0:
                return "package";
            
            case PROTECTED:
                return "protected";
            
            case PUBLIC:
                return "public";
            
            default:
                throw new InternalError();
        }
    }


/** check that mods does not contain elements of both set1 and set2;
 *  return true if it doesn't
 */
    public boolean checkDisjoint(int pos, int mods, int set1, int set2)
    {
        if (((mods & set1) != 0) && ((mods & set2) != 0))
        {
            report.error(pos, "illegal.mod.combination",
                         modstring[firstMod(mods & set1)],
                         modstring[firstMod(mods & set2)]);
            return false;
        }
        else
            return true;
    }

/** check that mods is contained in mask and does not contain illegal
 *  combinations of modifiers; return mods & mask.
 */
    public int checkMods (int pos, int mods, int mask, boolean classMods)
    {
        if ((mods & LANGUAGE_MODS & ~mask) != 0)
            report.error(pos, "mod.not.allowed", modstring[firstMod(mods & LANGUAGE_MODS & ~mask)]);
        else
        if (checkDisjoint(pos, mods, ABSTRACT | INTERFACE, FINAL | NATIVE | SYNCHRONIZED) &&
            (classMods || checkDisjoint(pos, mods, ABSTRACT, STATIC)) &&
            checkDisjoint(pos, mods, PUBLIC, PRIVATE | PROTECTED) &&
            checkDisjoint(pos, mods, PRIVATE, PUBLIC | PROTECTED) &&
            checkDisjoint(pos, mods, FINAL, VOLATILE))
        {}
        return mods & (mask | INTERNAL_MODS);
    }

/** complete member modifiers mods after checking them.
 *  required modidifiers are either imods or cmods, depending on
 *  whether we are in an interface or not.
 */
    public int completeMods(int pos, int mods, ContextEnv env, int cmods, int imods)
    {
        if ((env.enclClass.mods & INTERFACE) != 0)
        {
            int     dmods = checkMods(pos, mods, imods | PUBLIC, false);
            return (dmods | imods | (env.enclClass.mods & PUBLIC));
        }
        else
            return checkMods(pos, mods, cmods, true);
    }


    public static class Wrapper
    {
        int mods;
    
        public Wrapper(int mods)
        {
            this.mods = mods;
        }
    
        public Wrapper add(Wrapper m)
        {
            mods |= m.mods;
            return this;
        }
        
        public Wrapper add(int ms)
        {
            mods |= ms;
            return this;
        }
        
        public int intValue()
        {
            return mods;
        }
    }
}

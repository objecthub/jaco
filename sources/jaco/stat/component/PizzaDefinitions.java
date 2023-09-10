//      /   _ _      JaCo
//  \  //\ / / \     - library for pizza definitions
//   \//  \\_\_/     
//         \         Matthias Zenger, 11/12/00

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.stat.struct.*;
import jaco.stat.context.*;
import java.util.*;
import Definition.*;


public class PizzaDefinitions extends Definitions
                              implements PizzaModifierConst
{
/** other components
 */
    protected PizzaTypes    types;
    
    
    public String getName()
    {
        return "PizzaDefinitions";
    }
    
    protected DefinitionFactory DefinitionFactory(Types types)
    {
        return new PizzaDefinitionFactory(this, this.types = (PizzaTypes)types);
    }
    
    
    public Definition algebraicBase(Definition def)
    {
        Type    type;
        while (types.isAlgebraicType(type = def.supertype()))
            def = type.tdef();
        return def;
    }
    
    public Definition algebraicClass(Definition def)
    {
        Type    type = def.type;
        while (!types.isAlgebraicType(type))
            if (type == null)
                return null;
            else
            {
                def = type.tdef();
                type = def.supertype();
            }
        return def;
    }
    
    public boolean isAlgebraicClass(Definition def)
    {
        return (def.kind == TYP) && ((def.modifiers & ALGEBRAIC) != 0);
    }
    
    public boolean isAlgebraicSubclass(Definition def)
    {
        return (def.kind == TYP) && ((def.modifiers & ALGEBRAIC) != 0)
                && (((CDef)def).baseClass != def);
    }
    
    public boolean isCase(Definition def)
    {
        return (def.kind == TYP) && ((def.modifiers & CASEDEF) != 0);
    }
    
    public int getCaseTag(Definition def)
    {
        switch (def)
        {
            case ClassDef(_, _, _, _, _):
                return ((CDef)def).tag;
            
            case MethodDef(_):
                return ((MDef)def).tag;
            
            case VarDef(_, _, _):
                return ((VDef)def).tag;
        
            default:
                throw new InternalError();
        }
    }
    
    public Definition getCaseDefFromConstr(Definition constr)
    {
        return constr.type.restype().tdef();
    }
    
    public Definition[] getCaseMembers(Definition def)
    {
        if ((def.modifiers & CASEDEF) == 0)
            throw new InternalError();
        Definition      base = def;
        Definition[]    vars = def.members(VAR);
        int start = 0;
        while ((start < vars.length) &&
               (((vars[start].modifiers & SYNTHETIC) == 0) ||
                ((vars[start].modifiers & (STATIC | PRIVATE)) != 0)))
            start++;
        int i = start;
        while ((i < vars.length) &&
               ((vars[i].modifiers & SYNTHETIC) != 0) &&
               ((vars[i].modifiers & (STATIC | PRIVATE)) == 0))
            i++;
        Definition[]    fields = new Definition[i - start];
        for (int k = 1; k <= fields.length; k++)
            fields[i - k - start] = vars[start + k - 1];
        return fields;
    }
    
    public Definition getCaseConstr(Definition site, int tag)
    {
        Definition[]    defs = site.members(FUN);
        for (int i = 0; i < defs.length; i++)
            if (((defs[i].modifiers & CASEDEF) != 0) &&
                (((MDef)defs[i]).tag == tag))
                return defs[i];
        defs = site.members(VAR);
        for (int i = 0; i < defs.length; i++)
            if (((defs[i].modifiers & CASEDEF) != 0) &&
                (((MDef)defs[i]).tag == tag))
                return defs[i];
        return null;
    }
    
    public int[] getCaseArgNums(Definition site)
    {
        int[]   args = new int[((CDef)site).tag];
        do
        {
            Definition[]    caseDefs = site.members(TYP);
            int             tag;
            for (int i = 0; i < caseDefs.length; i++)
                if (((caseDefs[i].modifiers & CASEDEF) != 0) &&
                    ((tag = ((CDef)caseDefs[i]).tag) >= 0))
                    args[tag] = getCaseMembers(caseDefs[i]).length;
        } while ((site != ((CDef)site).baseClass) &&
                ((site = site.supertype().tdef()) == site)); // don't think about that :-)
        return args;
    }
    
    public Definition[][] getCaseArgDefs(Definition site)
    {
        Definition[][]  argdefs = new Definition[((CDef)site).tag][];
        do
        {
            Definition[]    caseDefs = site.members(TYP);
            int             tag;
            for (int i = 0; i < caseDefs.length; i++)
                if (((caseDefs[i].modifiers & CASEDEF) != 0) &&
                    ((tag = ((CDef)caseDefs[i]).tag) >= 0))
                    argdefs[tag] = getCaseMembers(caseDefs[i]);
        } while ((site != ((CDef)site).baseClass) &&
                ((site = site.supertype().tdef()) == site)); // don't think about that :-)
        for (int i = 0; i < argdefs.length; i++)
            if (argdefs[i] == null)
                argdefs[i] = new Definition[0];
        return argdefs;
    }
    
/** does method 'f' overload method definition 'other' with same
 *  number of arguments?
 *  PRE: f.name == other.name
 */
    public boolean overloads(Definition f, Definition other)
    {
        if (!f.isConstructor() &&
            (other.kind == FUN) &&
            (((other.modifiers & PRIVATE) == 0) ||
            (f.owner == other.owner)))
        {
            Type[]  fargs = f.type.argtypes();
            Type[]  oargs = other.type.argtypes();
            return (fargs.length == oargs.length) &&
                    !types.sametypes(fargs, oargs);
        }
        return false;
    }
   
/** swap the scopes of class definitions; used to toggle between
 *  java and pizza scopes
 */
    public void swapScopes()
    {
        for (Enumeration e = classes.elements(); e.hasMoreElements();)
            ((CDef)e.nextElement()).swap();
    }
}

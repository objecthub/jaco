package jaco.sjava.component;

import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.sjava.component.*;
import jaco.sjava.struct.*;
import Tree.*;
import Definition.*;
import jaco.framework.*;


public class SJEnterMembers extends EnterMembers implements SJModifierConst
{
    public String getName()
    {
        return "SJavaEnterMembers";
    }

    public Definition memberEnter(Tree tree, ContextEnv env)
    {
        trees.pushPos(tree.pos);
        Definition result;
        switch (tree) {
            case MethodDecl(_, _, Tree restype, _, _, _, _):
            result = super.memberEnter(tree, env);
            MethodDef f = (MethodDef)((MethodDecl)tree).def;
            if (((f.owner.modifiers & ACTIVE) != 0) &&
                            ((f.modifiers & STATIC) == 0) &&
                            (restype != null) &&
                            !f.name.toString().endsWith("$SJID") &&
                            !f.name.equals(TransSJ.AO)) {
                f.modifiers |= ACTIVE;
                Definition c = f.owner;
                for (Definition e = c.locals().lookup(f.name);
                 e.scope != null; e = e.next())
                if (((f != e.def) && definitions.overrides(f, e.def)) &&
                    ((e.def.owner.modifiers & ACTIVE) == 0)) {
                    f.modifiers &= ~ACTIVE;
                    break;
                }
            }
            break;
            
            default:
            result = super.memberEnter(tree, env);
            break;
        }
        trees.popPos();
        return result;
    }

    public void implementInterfaceMethods(ClassDecl cd, Definition c, ContextEnv env)
    {
        Type[]  is = c.interfaces();
        for (int i = 0; i < is.length; i++)
        {
            Definition  iface = is[i].tdef();
            for (Definition e = iface.locals().elems; e != null; e = e.sibling)
            {
                if ((e.def.kind == FUN) &&
                    ((e.def.modifiers & STATIC) == 0) &&
                    !(((cd.def.modifiers & ACTIVE) != 0) && e.name.equals(TransSJ.AO)))
                {
                    Definition  absfun = e.def;
                    Definition  implfun = definitions.implementation(absfun, cd.def);
                    if (implfun == null)
                        addAbstractMethod(cd, absfun, env);
                }
            }
            implementInterfaceMethods(cd, iface, env);
        }
    }
}

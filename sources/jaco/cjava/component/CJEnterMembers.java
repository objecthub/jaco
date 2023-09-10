//      /   _ _      JaCo
//  \  //\ / / \     - third pass of semantic analysis
//   \//  \\_\_/     
//         \         Matthias Zenger, 24/01/99

package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.cjava.struct.*;
import jaco.cjava.component.*;
import jaco.cjava.context.*;
import java.util.*;
import CJType.*;
import CJTree.*;
import CJDefinition.*;
import Definition.*;

/*
 * Used for letting components containing interfaces be used as
 * one interface extending the other interfaces in the compound.
 */
public class CJEnterMembers extends EnterMembers 
{
    /* components */
    CJCompounds cjcompounds;

    public void init(SemanticContext context)
    {
    super.init(context);
    cjcompounds = ((CJMainContext) context.compilerContext.mainContext).CJCompounds();
    }

 /** component name
 */
    public String getName()
    {
        return "CJEnterMembers";
    }
    
    public String getDescription()
    {
        return "CJava entering class members";
    }

    /**
     * Dirty patch.
     *
     * TODO: remove ? I don't think it's necessary any longer, because
     * AliasType does it automatically when tdef() is called.
     */
    public Type attribSuper(Tree tree, ContextEnv env, boolean interfaceExpected)
    {
    Type t = attribute.attribClassType(tree, env);
    switch ((CJType)t)
        {
        case CompoundType(_):
        Definition def = t.tdef();
        memberEnter(def);
        if( interfaceExpected )
            {
            if(def.supertype() != null)
                report.error(tree.pos, "no.interface");
            }
        else
            {
            if(def.supertype() == null)
                report.error(tree.pos, "no.interface.allowed");
            }
        break;
        
        case ClassType(_):
        memberEnter(t.tdef());
        if (interfaceExpected)
            {
            if ((t.tdef().modifiers & INTERFACE) == 0)
                report.error(tree.pos, "no.interface");
            }
        else
            {
            if ((t.tdef().modifiers & INTERFACE) != 0)
                report.error(tree.pos, "no.interface.allowed");
            else
                if ((t.tdef().modifiers & FINAL) != 0)
                report.error(tree.pos,"final.superclass", t.tdef());
            }
        break;
        }
    return t;
    }
    
    /**
     * Complete alias declaration, and set the alias real type.
     */
    public Definition memberEnter(Tree tree, ContextEnv env)
    {
    switch((CJTree)tree)
        {
        case AliasDecl(Name name, int mods, Tree realtype, Definition def):
        if(accountant.todo.get(def)!=null)
            {
            trees.pushPos(tree.pos);
            Type t = attribute.attribClassType(realtype, env);
            t = t.tdef().type; /* in case it's an alias and it was not entered */
            tree.type = t;

            /* check protection.
             *
             * aliases cannot be used to extend the scope
             * of the classes they represent.
             */
            Type[] tlist = cjcompounds.typeList(t);
            int protection = modifiers.protection(mods);
            int modst=0;
            for(int i=0; i<tlist.length; i++)
                {
                if(protection<modifiers.protection(mods=tlist[i].tdef().modifiers))
                    {
                    report.error(tree.pos, 
                             "alias.less.protected", 
                             tlist[i].tdef().name,
                             modifiers.protectionString(modst));
                    t = Type.ErrType;
                    }
                }


            CJClassDef d = (CJClassDef)def;
            d.aliasTo(t);
            d.setStandardType( types.make.ClassType(null) );
            d.setSpecialType( t );
            d.special();
            def.completer = null;

            accountant.todo.remove(def);

            trees.popPos();

            }
        return def;
        
        default:
        return super.memberEnter(tree, env);
        }
    }

}

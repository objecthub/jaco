// JAttribute
// Stewart Itzstein

package jaco.jjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.jjava.component.*;
import jaco.jjava.struct.*;
import jaco.jjava.context.*;
import java.util.*;
import Tree.*;
import Type.*;
import Definition.*;


public class JAttribute extends Attribute
                        implements JModifierConst
{
    public String getName()
    {
        return "JAttribute";
    }
    
    public void attribDecl(Tree tree, ContextEnv env)
    {
        switch (tree)
        {
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                                                        Tree[] thrown, Tree[] stats, MethodDef f):

            //case Tree.MethodDecl(_, _, _, _, _, _, MethodDef f):
                if ((f.modifiers & SIGNAL) != 0) {
                    // make the signal look like an abstract modifier
                    ((Tree.MethodDecl)tree).mods |= ABSTRACT;
                    super.attribDecl(tree, env);
                    // return the signal type to normal
                    ((Tree.MethodDecl)tree).mods &= ~ABSTRACT;
                } else {

                            ContextEnv localEnv = accountant.methodEnv((MethodDecl)tree, env);
                            localEnv.info.reported = f.type.thrown();
                                checks.checkBound(thrown, types.throwableType);
                                
                                for (int i = 0; i < params.length; i++)
                                        enterVar(params[i], localEnv);


                //insert asynch function parameters into current scope

                    //super.attribDecl(tree, env);
                    JMethodDecl md = ((JMethodDecl)tree).join;
                    while (md != null) {
                        // add check for names here
                        System.out.println(md.name+"Has "+md.params.length);
                                        for (int i = 0; i < md.params.length; i++)
                                                enterVar(md.params[i], localEnv);
                        localEnv.printscopes();
                        env.printscopes();
                        md = md.join;
                    }


                //from super class
                            ClassDef    owner = env.enclClass.def;
                            checks.checkOverride(tree.pos, f);
                            tree.type = f.type;
                                
 
                                if (stats == null)
                                {
                                        if ((owner.modifiers & INTERFACE) == 0 &&
                                                ((mods & (ABSTRACT | NATIVE)) == 0))
                                                        report.error(tree.pos, "not.abstract");
                                }
                                else
                                if ((owner.modifiers & INTERFACE) != 0)
                                        report.error(tree.pos, "has.body", "interface");
                                else
                                if ((mods & ABSTRACT) != 0)
                                        report.error(tree.pos, "has.body", "abstract");
                                else
                                if ((mods & NATIVE) != 0)
                                        report.error(tree.pos, "has.body", "native");
                                else
                                {
                                        if ((f.name == PredefConst.INIT_N) &&
                                                (owner.type != types.objectType))
                                        {
                                                if ((stats.length == 0) || !isSelfCall(stats[0]))
                                                {
                                                        ((MethodDecl)tree).stats = stats =
                                                                trees.append(trees.at(tree.pos).make(
                                                                        trees.SuperCall(false, new VarDecl[0])), stats);
                                                }
                                                localEnv.info.isSelfCall = true;
                                        }
                                        Type    resulttype = f.type.restype();
                                        attribStats(stats, localEnv, resulttype, resulttype);
                                }
                                localEnv.info.scope.leave();
                }
                break;
            
            default:
                super.attribDecl(tree, env);
        }
    }
}

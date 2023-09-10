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
import Type.*;
import Tree.*;
import CJTree.*;
import CJDefinition.*;
import Definition.*;


/**
 * Enter aliases in the class list.
 */
public class CJEnterClasses extends EnterClasses implements CJType.AliasCompleter
{
    /**
     * Valid modifiers for local aliases (owner by methods).
     */
    static final int LocalAliasMods = 0;

    /**
     * Valid modifiers for aliases that are owner by packages.
     */
    static final int AliasMods = Modifiers.PUBLIC;

    /**
     * Valid modifiers for aliases that are owner by classes.
     */
    static final int InnerAliasMods = Modifiers.PUBLIC|Modifiers.PRIVATE|Modifiers.PROTECTED;
    
    public String getName()
    {
    return "CJavaEnterClasses";
    }
    
    public String getDescription()
    {
    return "entering classes";
    }
    
    CJEnterMembers members;
    public void init(SemanticContext context)
    {
    super.init(context);
    members = (CJEnterMembers)context.EnterMembers();
    }

    /**
     * @see CJType.CompleteAlias
     * @see CJType.AliasType
     */
    public Type completeAlias(Definition def)
    {
    members.memberEnter(def);
    return def.type;
    }
    
    /**
     * Enter aliases 
     */
    public Tree classEnter(Tree tree, ContextEnv env)
    {
    switch((CJTree)tree)
        {
        /* mostly unchanged ClassDecl */
        case AliasDecl(Name name, int mods, _, Definition def):
        trees.pushPos(tree.pos);
        
        Definition  owner = (env.info.scope == null) ?
            env.toplevel.def :
            env.info.scope.owner;
        Definition  enclClass = owner.enclClass();
        Name        fullname = ((owner.kind == TYP) || (owner.kind == PCK)) ?
            mangler.formFullName(name, enclClass) :
            formAnonFullName(name, enclClass);
        if (classesNest && (accountant.compiled.get(fullname) != null))
            {
            report.error(tree.pos, "duplicate.class", mangler.unmangle(fullname));
            Name    newname = Name.fromString(name + "$_$" + accountant.compiled.size());
            ((ClassDecl)tree).name = newname;
            fullname = mangler.formFullName(newname, enclClass);
            }
        accountant.compiled.put(fullname, fullname);
        CJClassDef  c = (CJClassDef)def;
        if (def == null)
            {
            c = (CJClassDef)definitions.defineClass(fullname);
            c.completer = null;
            c.modifiers = modifiers.checkMods(
                              tree.pos, mods, c.isLocal() ?
                              LocalAliasMods :
                              (owner.kind == TYP) ?
                              InnerAliasMods :
                              AliasMods, true);
            if ((owner.modifiers & STRICTFP) != 0)
                c.modifiers |= STRICTFP;
            /*
             * Create a temporary type. Using this type causes
             * the real type of the alias to be read from the
             * tree by EnterMembers.entermemember(def)
             */
            Type aliastype = ((CJType.Factory)types.make).AliasType(this);
                aliastype.setDef(c);
            c.type = aliastype;
            }
        c.owner = owner;
        
        c.sourcefile = env.toplevel.info.source.getName();
        //c.setLocals(new Scope(null, c));
        //env.toplevel.def.locals().enterIfAbsent(c);
        if (owner.kind != PCK)
            {
            mangler.put(c.fullname, (ClassDef)owner.enclClass(), name, c.modifiers);
            Definition  proxy = c.proxy(name);
            Scope enclscope = accountant.enterScope(env);
            if (checkUnique(tree.pos, proxy, enclscope))
                {
                enclscope.enter(proxy);
                checkNoDuplicate(tree.pos, c);
                }
            }
        else
            {
            env.toplevel.def.locals().enterIfAbsent(c);
            
            if (!classesNest)
                {
                Mangler.Mangle  info = mangler.get(c.fullname);
                if (info != null)
                    info.owner.locals().enterIfAbsent(c.proxy(info.name));
                }
            }
        tree.setDef(c);
        accountant.todo.put(c, tree);
        
        trees.popPos();
        return tree;
        
        default:
        return super.classEnter(tree, env);
        }
    }
}


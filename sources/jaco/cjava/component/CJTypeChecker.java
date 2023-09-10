package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import CJType.*;
import CJTree.*;
import java.util.*;

/**
 * Add specific checks for compounds.
 */
public class CJTypeChecker extends TypeChecker
{
    /**
     * other component
     */
    protected NameResolver namer;


    /** component name
     */
    public String getName()
    {
    return "CJTypeChecker";
    }

    /**
     * Component CJCompounds.
     */
    protected CJCompounds cjcompounds;
    /**
     * Initialization
     *
     * @param context SemantiContext, with CJMainContext as its main context.
     */
    public void init(SemanticContext context)
    {
    super.init(context);
    cjcompounds = ((CJMainContext)context.compilerContext.mainContext).CJCompounds();
    namer = context.compilerContext.mainContext.NameResolver();
    }
    
    /**
     * Hashtable that stores the result of previous checks in checkCompound.
     *
     * Key: compound.toString() (after the fake definition's been created)
     * Value: Boolean (true=> compound valid, false=> compound invalid)
     */
    protected Hashtable checkedCompounds = new Hashtable();
    
    /**
     * Check whether compound type is valid.
     *
     * It checks that there is at most one class in the
     * compound, following aliases and getting into
     * nested compounds if necessary.
     *
     * It also sets the definition.
     *
     * @param pos position of the compound definition 
     * @param compound the compound type
     * @param components the compounents in the componud 
     */
    public Type checkCompound(int pos, ContextEnv env, Type compound, Type[] components)
    {
    try
        {
        String str;
        boolean valid = true;
        Definition def = cjcompounds.fakeDefinition(compound, components);
        compound.setDef(def);

        str = compound.toString();

        Boolean validB = (Boolean)checkedCompounds.get(str);
        if(validB!=null)
            valid = validB.booleanValue();
        else
            {
            valid = fullCheck(def, pos, env);
            checkedCompounds.put(str, new Boolean(valid));
            }
        return valid ? compound:types.errorType;
        }
    catch(CJCompounds.InvalidCompoundException e)
        {
        report.error(pos, e.getMessageKey());
        return types.errorType;
        }
    }
    
    /**
     * Perform a full check for a compound type, and display any error.
     *
     * It's SLOW !
     *
     * @param def definition of compound type
     * @param pos position in the source file
     * @param env context env (for accessibility checks)
     * @retval true if the compound is o.k.
     * @see CJCompounds.InvalidCompoundException
     */
    protected boolean fullCheck(Definition def, int pos, ContextEnv env) throws CJCompounds.InvalidCompoundException
    {
    Type superclass = def.supertype();
    Type[] interfaces = def.interfaces();

    if(superclass!=null && (superclass.tdef().modifiers&ModifierConst.FINAL)!=0)
        {
        report.error(pos, "no.final.in.compound");
        }

    if(interfaces!=null)
        {
        for(int i=0; i<interfaces.length; i++)
            {
            if(superclass!=null)
                {
                if(methodConflict(superclass, interfaces[i], pos, env))
                    return false;
                }
            for(int j=0; j<i; j++)
                {
                if(methodConflict(interfaces[j], interfaces[i], pos, env))
                    return false;
                }
            }
        }
    return true;
    }

    /**
     * Check for method conflicts between two (compound) types.
     *
     * There's a conflict when two types define the same method
     * with the same signature, but with different return types.
     *
     * If an error is found, it is reported.
     *
     * This method works on compound types as well as on
     * standard class or interface types.
     *
     * @param type the first type 
     * @param type2 the second type 
     * @param pos position in the source file
     * @param env context env (for accessibility checks)
     * @return true if the two types have an incompatible method
     */
    public boolean methodConflict(Type t1, Type t2, int pos, ContextEnv env)
    {
    if(t1==null || t2==null)
        return false;

    return methodConflict(t1.tdef(), t2.tdef(), pos, env);
    }

    /**
     * @see CJTypeChecker#methodConflict
     */
    protected boolean methodConflict(Definition def1, Definition def2, int pos, ContextEnv env)
    {
    if(def1.completer!=null)
        def1.complete();
    if(def2.completer!=null)
        def2.complete();
    Scope scope = def1.locals();
    while(scope!=null)
        {
        Definition e = scope.elems;
        while(e!=null)
            {
            if(e.scope!=null && e.def.kind==DefinitionConst.FUN && namer.accessible(env, e.def.owner, e.def))
                {
                switch(e.def.type)
                    {
                    case MethodType(Type[] argtypes, Type restype, _):
                    if(conflictIn(def2, e.def, argtypes, restype, pos, env))
                        return true;
                    }
                }
            e=e.sibling;
            }
        scope=scope.next; /* next outer scope */
        }
    /* check the interfaces, too */
    Type[] interfaces = def1.interfaces();
    if(interfaces!=null)
        for(int i=0; i<interfaces.length; i++)
        {
            if(methodConflict(interfaces[i].tdef(), def2, pos, env))
            return true;
        }
    return false;
    }

    /**
     * Check that there exists no method in the type
     * that has the same name and signature, but
     * not the same return value.
     *
     * If a conflict is found, it is reported
     * 
     * @param def the definition to look for methods
     * @param methodef definition of the method
     * @param argtypes arguments of the method
     * @param restype return type of the method
     * @param pos position in the source file
     * @param env context env (for accessibility checks)
     * @result true if a conflict was found
     */
    protected boolean conflictIn(Definition def, Definition methoddef,  Type[] argtypes, Type restype, int pos, ContextEnv env)
    {
    Name methodname = methoddef.name;
    if(def==null)
        return false;
    Scope scope = def.locals();
    if(scope==null)
        return false;
    Definition e = scope.lookup(methodname);
    while(e.scope!=null)
        {
        if(e.def.kind==DefinitionConst.FUN)
            {
            Definition c = e.def;
            if(namer.accessible(env, c.owner, c))
            {
                switch(c.type)
                {
                case MethodType(Type[] argtypes2, Type restype2, _):
                    if(types.sametypes(argtypes, argtypes2) && !types.sametype(restype, restype2))
                    {
                        report.error(pos, "method.conflict", 
                             methodname.toString(), 
                             def.fullname.toString(), 
                             methoddef.owner.fullname.toString());
                        return true;
                    }
                    break;
                }
            }
            }
        e=e.next();
        }
    Type[] is = def.interfaces();
    if(is!=null)
        for(int i=0; i<is.length; i++)
        {
            if(conflictIn(is[i].tdef(), methoddef, argtypes, restype, pos, env))
            return true;
        }
    return false;
    }

       
}

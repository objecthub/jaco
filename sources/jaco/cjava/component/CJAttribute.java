package jaco.cjava.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import jaco.java.component.*;
import jaco.java.context.*;
import jaco.cjava.struct.*;
import jaco.cjava.context.*;
import CJType.*;
import CJTree.*;
import Tree.*;
import Definition.*;
import java.util.*;

public class CJAttribute extends Attribute
{
    static final Name SEPARATOR = Name.fromString("$cj$");
    /* other components */
    CJCompounds cjcompounds;
    CJSignatures cjsignatures;

    /** component name
     */
    public String getName()
    {
    return "CJAttribute";
    }

    public void init(SemanticContext context)
    {
    super.init(context);
    cjcompounds = ((CJMainContext)context.compilerContext.mainContext).CJCompounds();
    cjsignatures = ((CJMainContext)context.compilerContext.mainContext).CJSignatures();
    }


    /**
     * attribSelect version that uses CompoundType as a ClassType (or ArrayType). 
     * 
     * Woaw ! CUT&PASTE ! 
     * Pretty bad, but I couldn't find any cleaner way to do this.
     */
    protected Definition attribSelect(Select tree, Type stype, ContextEnv env, int kind, Type pt)
    {
    Name        name = tree.selector;
    int         pos = tree.pos;
    Definition  c = stype.tdef();
    Definition  def;
    
    switch ((CJType)stype.deref())
        {
        case CompoundType(_):
        if(stype.tdef()==null)
            throw new InternalError("Definition not set for " + stype);
        if(stype.tdef().locals()==null)
            throw new InternalError("Scope not set for " + stype);

        namer.fixupScope(pos, c);
        def = namer.resolveSelectFromType(stype, name, env, kind & ~PCK, pt);
        if (def.kind >= BAD)
            def = namer.access(def, pos, stype, name, namer.protoArgs(pt));
        break;
        default:
        return super.attribSelect(tree, stype, env, kind, pt);
        }
    return def;
    }
    

    /**
     * Attribute an exprension.
     *
     * If the expression contains a compound type declaration, a new
     * compound type is created and then checked.
     */
    public Type attribExpr(Tree tree, ContextEnv env, int kind, Type pt)
    {
    switch ((CJTree)tree)
        {

        case NewArray(Tree elemtype, _):
        Type t = super.attribExpr(tree, env, kind, pt);
        switch((CJType)elemtype.type)
            {
            case CompoundType(_):
            tree.type = Type.ErrType;
            t = Type.ErrType;
            report.error(tree.pos, "no.array.of.compound");
            break;
            }
        return t;
        /*
        {
            
            Type owntype = Type.ErrType;
            int ownkind = VAL;
            trees.pushPos(tree.pos);
            attribArgs(dims, env, types.intType);
            owntype = attribType(elemtype, env);
            switch((CJType)owntype)
            {
            case CompoundType(_):
                owntype = Type.ErrType;

                break;

            default:
                for (int i = 0; i < dims.length; i++)
                owntype = types.make.ArrayType(owntype);
                break;
            }
            if ((owntype != Type.ErrType) &&
            definitions.checkKind(tree.pos, ownkind, kind))
            {
                tree.type = checks.checkType(tree.pos, owntype, pt);
            }
            else
            tree.type = Type.ErrType;
            trees.popPos();
            return tree.type;
        }
        */



        case ArrayTypeTerm(Tree elemtype):
        trees.pushPos(tree.pos);
        Type owntype = null;
        int ownkind = TYP;
        Type    etype = attribNonVoidType(elemtype, env);
        switch((CJType)etype)
            {
            /*
            case CompoundType(_):

            owntype = Type.ErrType;
            break;
            */
            default:
            owntype = types.make.ArrayType(etype);
            break;
            }
        if ((owntype != Type.ErrType) &&
            definitions.checkKind(tree.pos, ownkind, kind))
            {
            tree.type = checks.checkType(tree.pos, owntype, pt);
            }
        else
            tree.type = Type.ErrType;
        trees.popPos();
        return tree.type;
        
        case CompoundType(Tree[] names):
            {
            Type    owntype;
                Type[] components = new Type[names.length];

            trees.pushPos(tree.pos);
            
            for(int i=0; i<names.length; i++)
                {
                components[i] = attribClassType(names[i], env);
                }
            owntype = ((CJType.Factory)types.make).CompoundType( components );
            owntype = ((CJTypeChecker)checks).checkCompound(tree.pos, env, owntype, components);

            /* if possible, convert it to a single, standard type */
            Definition def = owntype.tdef();
            switch((CJDefinition)def)
                {
                case CompoundDef(Type superclass, Type[] interfaces):
                if(superclass!=null && (interfaces==null || interfaces.length==0))
                    {
                    owntype = superclass;
                    }
                else if(superclass==null && interfaces!=null && interfaces.length==1)
                    {
                    owntype = interfaces[0];
                    }
                }

            if(definitions.checkKind(tree.pos, TYP, kind))
                {
                tree.type = checks.checkType(tree.pos, owntype, pt);
                }
            else
                tree.type = Type.ErrType;


            trees.popPos();
            return tree.type;
            }

        case Typeop(OperatorConst.TYPECAST, Tree expr, Tree type):
        Type retval = super.attribExpr(tree, env, kind, pt);
        ((CJTypeChecker)checks).methodConflict(expr.type, type.type, tree.pos, env);
        return retval;
 
        default:
        return super.attribExpr(tree, env, kind, pt);
        }
    }

    /**
     * Add compounds to the list of 'class types'.
     */
    public Type attribClassType(Tree tree, ContextEnv env)
    {
    attribType(tree, env);
    switch((CJType)tree.type.deref())
        {
        case ClassType(_):
        case ErrType:
        case CompoundType(_):
        break;

        default:
        report.error(tree.pos, "requires.class.type", tree.type);
        tree.type = Type.ErrType;
        }
    return tree.type;
    }

    /**
     * Attribute a local alias declaration.
     *
     * It's a simpler version of the work done for other alias
     * declarations in CJEnterClasses and CJEnterMembers.
     *
     * The AliasDef is not put into the Definitions hash table, just
     * in the current scope.
     *
     * It is not legal to define an alias with the same name in
     * the same scope.
     *
     * Local aliases don't accept modifiers (public, final, ...) even
     * if the grammar allows it. (I had to do that for the
     * grammar to be accepted.)
     */
    public Type attribStat(Tree tree, ContextEnv env, Type pt, Type sofar)
    {
    switch((CJTree)tree)
        {
        case AliasDecl(Name name, int mods, Tree realtypeTree, _):
        ContextInfo info = env.info;
        trees.pushPos(tree.pos);
        tree.type = sofar;

        modifiers.checkMods(tree.pos, mods, 0, false); /* no modifiers allowed */

        Definition owner = env.enclMethod.type.tdef();
        Type realtype = attribClassType(realtypeTree, env);
        CJClassDef def = (CJClassDef) ((CJDefinition.Factory)definitions.make).ClassDef(0, name, realtype, owner);
        def.aliasTo(realtype);
        //tree.type = realtype;
        tree.setDef(def);

        /* enter into local scope */
        if(members.checkUnique(tree.pos, def, info.scope))
            {
            info.scope.enter(def);
            }
        
        trees.popPos();
        return tree.type;

        default:
        return super.attribStat(tree, env, pt, sofar);
        }
    }

    /**
     * Attribute an alias declaration.
     *
     * Most of the work is done in CJEnterClasses and CJEnterMembers.
     *
     * Methods that should be renamed are also detected here, and
     * a new name is chosen for them. (These are methods that would
     * have the same name and the same signature after compound types
     * have been converted to standard types.)
     */
    public void attribDecl(Tree tree, ContextEnv env)
    {
    
    switch ((CJTree)tree)
        {
        case AliasDecl(Name name, int mods, Tree realtype, Definition def):
        trees.pushPos(tree.pos);
        if(realtype.type==null || realtype.type.tdef()==null || def==null)
            throw new InternalError();
        
        tree.type = def.type;
        
        trees.popPos();
        return;

        case ClassDecl(_, _, _, _, Tree[] defs, ClassDef c):
        /* ClassDecl: detect methods that should be renamed */
            CJClassDef cjc = (CJClassDef)c;
            for(int i = 0; i<defs.length; i++)
            {
                switch(defs[i])
                {
                case MethodDecl(_, _, Tree restype, VarDecl[] params, Tree[] thrown, _, MethodDef def):
                    boolean compounds = false;
                    if(params!=null && params.length>0)
                    {
                        compounds = cjcompounds.usesCompounds( ((Type.MethodType)def.type).argtypes );
                        if(compounds)
                        {
                            /* rename */
                            TwoFacedMethodDef m = (TwoFacedMethodDef)def;
                            Name altname = m.getSpecialName().append(SEPARATOR);
                            altname=altname.append(cjsignatures.typeToSig(def.type));

                            /* replace '/' with '_', ';' with '$', and '(' and ')' with 'X' */
                            byte[] ascii = new byte[altname.length()];
                            altname.copyAscii(ascii, 0);
                            for(int j=0; j<ascii.length; j++)
                            {
                                if(ascii[j]==((byte)'/'))
                                ascii[j]=(byte)'_';
                                else if(ascii[j]==((byte)';'))
                                ascii[j]=(byte)'$';
                                else if(ascii[j]==((byte)'(') || ascii[j]==((byte)')'))
                                ascii[j]=(byte)'X';
                            }
                            m.special();
                            m.setStandardName(Name.fromAscii(ascii, 0, ascii.length));
                        }
                    }
                    
                    Type.MethodType t = (Type.MethodType)def.type;
                    if(!compounds)
                    compounds = (cjcompounds.usesCompounds(t.argtypes) 
                             || cjcompounds.usesCompounds(t.restype) 
                             || cjcompounds.usesCompounds(t.thrown));
                    if(compounds)
                    {
                        /* real signature */
                        TwoFacedMethodDef m = (TwoFacedMethodDef)def;

                        Type altt = types.make.MethodType(cjcompounds.convertArrayToDefault(t.argtypes), 
                                          cjcompounds.convertToDefault(t.restype),
                                          cjcompounds.convertArrayToDefault(t.thrown));
                        m.special();
                        m.setStandardType(altt);
                        
                        ((CJClassDef)def.owner).addDefinition(m);
                    }
                    break;
                    
                case VarDecl(_, _, _, _, VarDef def):
                    if(cjcompounds.usesCompounds(def.type))
                    {
                        TwoFacedVarDef v = (TwoFacedVarDef)def;
                        v.special();
                        v.setStandardType(cjcompounds.defaultTypeFor(def.type));
                        cjc.addDefinition(v);
                    }
                    break;
                }
            }

        super.attribDecl(tree, env);
        return;
        
        default:
        super.attribDecl(tree, env);
        return;
        }
    }
}

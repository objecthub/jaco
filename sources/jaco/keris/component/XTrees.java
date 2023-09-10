//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    tree management
//                           
//  [XTrees.java (966) 19-Apr-01 17:05 -> 23-Jun-01 00:11]

package jaco.keris.component;

import jaco.java.component.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import jaco.keris.struct.*;
import jaco.framework.*;
import XTree.*;
import java.util.*;


public interface XTreeConst extends TreeConst {
    final int IMPORTMODULE = 112;
    final int IMPORTMODULESTAR = 113;
    final int INCLUDEMODULE = 114;
    final int FRAGMENT = 123;
}

public class XTrees extends Trees implements XTreeConst, XModifierConst {
    
    public final static Name TOSTRING_N = Name.fromString("toString");
    public final static Name SUBSTRING_N = Name.fromString("substring");
    public final static Name GETPROPERTY_N = Name.fromString("java.lang.System.getProperty");
    public final static Name SETPROPERTY_N = Name.fromString("java.lang.System.setProperty");
    public final static Name OBJECT_N = Name.fromString("java.lang.Object");
    public final static Name STRING_N = Name.fromString("java.lang.String");
    
    protected Mangler mangler;
    protected Constants consts;
    protected Types types;
    
    
    public HashMap replacements = new HashMap();
    
    
    public String getName() {
        return "XTrees";
    }
    
    public void init(MainContext context) {
        super.init(context);
        make = new XTreeFactory();
        newdef = new XTreeCreate();
        redef = new XTreeRedef();
        mangler = context.Mangler();
        consts = context.Constants();
        types = context.Types();
    }
    
    public Tree QualidBase(XClassDef c) {
        return QualidThis(c.baseClass);
    }
    
    public Tree QualidThis(Definition def) {
        if ((def.owner == null) || (def.owner.name.length() == 0))
            return IdentThis(def);
        else
            return SelectThis(QualidThis(def.owner), def);
    }
    
    public Tree SelectThis(Tree tree, Definition def) {
        return newdef.Select(tree, mangler.unmangleShort(def.name, def.fullname))
                     .setType(def.type)
                     .setDef(def);
    }

    public Tree IdentThis(Definition def) {
        return newdef.Ident(mangler.unmangleShort(def.name, def.fullname))
                     .setType(def.type)
                     .setDef(def);
    }
    
	/** return a syntax tree representing type 't'
	 */
    public Tree toTree(Type t) {
        switch (t) {
        	case XType.ModuleType(_):
        		return ClassName(t.tdef());
        	case XType.CompoundType(Type[] ts):
        		return super.toTree(types.objectType);
        	case ClassType(_):
                if ((t.tdef().modifiers & CLASSFIELD) != 0)
                	return super.toTree(types.objectType);
                return super.toTree(t);
            default:
                return super.toTree(t);
        }
    }
    
    /** cast name to type tree
     */
    protected Tree castTo(Tree tree, Name name) {
        return newdef.Typeop(TYPECAST, newdef.Ident(name), tree);
    }
    
    /** cast a tree if it's type does not match the required type
     */
    public Tree castExpr(Type required, Type current, Tree expr) {
    	switch (expr) {
    		case Self(_, _, _):
    			return expr;
    	}
    	// simulate erasure
        switch ((XType)current) {
            case CompoundType(Type[] ts):
                if (ts.length == 1)
                    return castExpr(required, ts[0], expr);
                current = types.objectType;
                break;
            case ArrayType(_):
            	return expr;
            default:
                if ((current.tdef().modifiers & MODULE) != 0)
                    return expr;
                if ((current.tdef().modifiers & CLASSFIELD) != 0)
                    current = types.objectType;
        }
        // compare types
        switch ((XType)required) {
            case CompoundType(Type[] ts):
                if (ts.length == 1)
                    return castExpr(ts[0], current, expr);
                return expr;
                /*
                Type[] nts = new Type[ts.length - 1];
                System.arraycopy(ts, 1, nts, 0, nts.length);
                return castExpr(XType.CompoundType(ts),
                                ts[0],
                                castExpr(ts[0], current, expr)); */
            case ModuleType(_):
            	return expr;
            default:
                if ((required.tdef().modifiers & CLASSFIELD) != 0)
                    return expr;
                if (((XTypes)types).javaSubtype(current, required))
                    return expr;
                return newdef.Typeop(TYPECAST, expr, toTree(required));
        }
    }
    
    public Tree[] castExprs(Type[] required, Type[] current, Tree[] exprs) {
        Tree[] res = new Tree[exprs.length];
        for (int i = 0; i < exprs.length; i++)
            res[i] = castExpr(required[i], current[i], exprs[i]);
        return res;
    }
    
    /** return the tree representation of common types
     */
    public Tree objectType() {
        return Qualid(OBJECT_N);
    }
    
    public Tree stringType() {
        return Qualid(STRING_N);
    }
    
    public Tree booleanType() {
        return toTree(types.booleanType);
    }
    
    /** transform class definition into class name tree
     */
    public Tree classTree(Definition c) {
        if (c.owner.kind == TYP)
            return newdef.Select(
                    classTree(c.owner),
                    mangler.unmangleShort(c.name, c.fullname)).setDef(c);
        else
            return Qualid(Name.fromString(mangler.unmangle(c.fullname))).setDef(c);
    }
    
    /** return a string literal
     */
    public Tree stringLit(String str) {
        return newdef.Literal(consts.make.StringConst(Name.fromString(str)));
    }
    
    /** return an integer literal
     */
    public Tree intLit(int val) {
        return newdef.Literal(consts.make.IntConst(val));
    }
    
    /** return an boolean literal
     */
    public Tree boolLit(boolean val) {
        if (val)
            return newdef.Ident(PredefConst.TRUE_N);
        else
            return newdef.Ident(PredefConst.FALSE_N);
    }
    
    /** copy a tree
     */
    public Tree copy(Tree tree) {
        if (tree == null)
            return null;
        switch ((XTree)tree) {
            case ModuleFieldDecl(int mods, Tree impl, Tree[] intf):
                return at(tree).make(
                        ((XTree.Factory)redef).ModuleFieldDecl(mods, copy(impl), copy(intf)));
            case Overrides(Tree overriding, Tree[] overridden):
                return at(tree).make(
                        ((XTree.Factory)redef).Overrides(copy(overriding), copy(overridden)));
            case ModuleSelect(Tree from, Name selector, Definition def):
                return at(tree).make(
                        ((XTree.Factory)redef).ModuleSelect(copy(from), selector).setDef(def));
            case CompoundType(Tree[] fields):
                return at(tree).make(
                        ((XTree.Factory)redef).CompoundType(copy(fields)));
            default:
                return super.copy(tree);
        }
    }
    
    //
    // Handling of status messages
    //
    
    /** return an indentation tree
     */
    public Tree indentTree(String str) {
        return newdef.Binop(PLUS,
            newdef.Binop(PLUS,
                stringLit("# "),
                newdef.Apply(Qualid(GETPROPERTY_N), new Tree[]{stringLit("keris.indent")})),
            stringLit(str));
    }
    
    /** return a tree for incrementing the indentation
     */
    public Tree incrIndentTree() {
        return newdef.Exec(
                newdef.Apply(
                    Qualid(SETPROPERTY_N),
                    new Tree[]{
                        stringLit("keris.indent"),
                        newdef.Binop(PLUS,
                            newdef.Apply(
                                Qualid(GETPROPERTY_N),
                                new Tree[]{stringLit("keris.indent")}),
                            stringLit("  "))}));
    }
    
    /** return a tree for decrementing the indentation
     */
    public Tree decrIndentTree() {
        return newdef.Exec(
                newdef.Apply(
                    Qualid(SETPROPERTY_N),
                    new Tree[]{
                        stringLit("keris.indent"),
                        newdef.Apply(
                            newdef.Select(
                                newdef.Apply(
                                    newdef.Select(
                                        newdef.Apply(
                                            Qualid(GETPROPERTY_N),
                                            new Tree[]{stringLit("keris.indent")}),
                                        TOSTRING_N),
                                    noTrees),
                                SUBSTRING_N),
                            new Tree[]{intLit(2)})}));
    }
}

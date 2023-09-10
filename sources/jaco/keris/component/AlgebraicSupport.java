//                 _
//   _  _____ _ __(_)___     Keris -- modular, object-oriented programming
//  | |/ / _ \ '__| / __|    (c) 2001-2003 Matthias Zenger
//  |   <  __/ |  | \__ \
//  |_|\_\___|_|  |_|___/    Keris to Java translator
//                           
//  [XTrans.java (51763) 18-May-01 12:15 -> 22-Jun-01 23:13]

package jaco.keris.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.keris.context.*;
import jaco.keris.struct.*;
import java.util.*;
import Tree.*;
import XTree.*;
import Definition.*;


public interface AlgebraicSupportConst {
    
	/** names in algebraic types
	 */
    Name    TAG_N = Name.fromString("$tag");
    Name    SUB_ID_N = Name.fromString("$subID");
    Name    SUB_IDS_N = Name.fromString("$subIDs");
    Name    EXTENSIONS_N = Name.fromString("$extensions");
    Name    CASTABLE_N = Name.fromString("$castable");
    Name    REGISTER_N = Name.fromString("$register");
    Name    EXTEND_N = Name.fromString("$extend");
    Name    GET_SUB_ID_N = Name.fromString("$getSubID");
    Name    THROW_CAST_EXC_N = Name.fromString("$throw$cast$exception");
    Name    TABLE_N = Name.fromString("table");
    Name    I_N = Name.fromString("i");
    Name    LENGTH_N = Name.fromString("length");
    Name    NEWARRAY_N = Name.fromString("new$array");
    Name    VAR_CASE_N = Name.fromString("$Var");
    Name    TO_STRING_N = Name.fromString("toString");
    
	/** names used to construct EACs
	 */
    Name    CLASS_CAST_EXC_N = Name.fromString("ClassCastException");
    Name    ARRAYCOPY_N = Name.fromString("System.arraycopy");
    Name    EXT_LENGTH_N = Name.fromString("$extensions.length");

	/** names used by pattern matcher
	 */
    Name    EQUALS_N = Name.fromString("equals");
    Name    ERROR_N = Name.fromString("Error");
}

public class AlgebraicSupport extends Component
                              implements XModifierConst, OperatorConst,
                                       TreeConst, AlgebraicSupportConst,
                                       DefinitionConst, TypeConst {

	/** the context
	 */
    protected XMainContext mainContext;
    
	/** other components
	 */
    protected XTrees trees;
    protected Constants constants;
    protected Mangler mangler;
    protected XTypes types;
    protected XDefinitions definitions;
    public Labeller labeller;
    
    protected Tree.Factory  newdef;
    protected Tree.Factory  redef;
    
	/** list of all base types involved in cast expressions within the
	 *  current method
	 */
    protected Type[] temporaryVarTypes;
    
    
	/** component initialization
	 */
    public void init(XTransContext context) {
        super.init(context);
        mainContext = (XMainContext)context.compilerContext.mainContext;
        mangler = mainContext.Mangler();
        constants = mainContext.Constants();
        definitions = (XDefinitions)mainContext.Definitions();
        types = (XTypes)mainContext.Types();
        trees = (XTrees)mainContext.Trees();
        labeller = context.Labeller();
        newdef = trees.newdef;
        redef = trees.redef;
    }
    
    public String getName() {
        return "AlgebraicSupport";
    }
    
	//////////// temporary variable management
    
    protected Name temporaryName(int i) {
        return Name.fromString("tmp$" + i);
    }
    
    protected Tree temporaryIdent(Type type) {
        if (temporaryVarTypes == null) {
            temporaryVarTypes = new Type[]{type};
            return newdef.Ident(temporaryName(0));
        }
        for (int i = 0; i < temporaryVarTypes.length; i++)
            if (types.sametype(type, temporaryVarTypes[i]))
                return newdef.Ident(temporaryName(i));
        temporaryVarTypes = types.append(temporaryVarTypes, type);
        return newdef.Ident(temporaryName(temporaryVarTypes.length - 1));
    }
    
    protected Tree[] temporaryDecls() {
        if ((temporaryVarTypes == null) || (temporaryVarTypes.length == 0))
            return trees.noTrees;
        else {
            Tree[]  ts = new Tree[temporaryVarTypes.length];
            for (int i = 0; i < ts.length; i++)
                ts[i] = newdef.VarDecl(temporaryName(i), 0,
                                    trees.toTree(temporaryVarTypes[i]), null);
            temporaryVarTypes = null;
            return ts;
        }
    }

	//////////// support for EAC translation 

    public Tree genBool(boolean val) {
        return trees.Ident(val ? constants.trueConst : constants.falseConst);
    }
    
    public Tree genInt(int i) {
        return newdef.Literal(constants.make.IntConst(i));
    }
    
    public Tree Null(Type type) {
        if (type.isBasic()) {
            switch (type.tag()) {
                case BOOLEAN:
                    return genBool(false);
                case BYTE:
                case CHAR:
                case SHORT:
                case INT:
                    return trees.newdef.Literal(constants.make.IntConst(0, type.tag()));
                case LONG:
                    return trees.newdef.Literal(constants.make.LongConst(0l));
                case FLOAT:
                    return trees.newdef.Literal(constants.make.FloatConst(0.0f));
                case DOUBLE:
                    return trees.newdef.Literal(constants.make.DoubleConst(0.0));
                default:
                    throw new InternalError();
            }
        } else
            return trees.Null();
    }
    
    public Tree genIndexed(Name array, Name index) {
        return newdef.Index(trees.Qualid(array), trees.Qualid(index));
    }
    
    public Tree genIndexed(Name array, Definition c, Name index) {
        return newdef.Index(trees.Qualid(array), newdef.Select(trees.ClassName(c.owner), prefix(index, c)));
    }
    
    public Tree genIndexed(Definition c, Name array, Name index) {
        return newdef.Index(
        	newdef.Select(trees.ClassName(c.owner), prefix(array, c)), trees.Qualid(index));
    }
    
    public Tree genLocalUpdate(Definition c, Definition owner, Tree rhs) {
        if (definitions.isAlgebraicClass(c))
            return genLocalUpdate(c.supertype().tdef(), owner,
                newdef.Assign(genIndexed(prefix(CASTABLE_N, owner), c, SUB_ID_N), rhs));
        else
            return rhs;
    }
    
    public Tree genOtherUpdate(Definition c, Definition owner, Tree rhs) {
        if (definitions.isAlgebraicClass(c))
            return genOtherUpdate(c.supertype().tdef(), owner,
                        newdef.Assign(genIndexed(c, CASTABLE_N, prefix(SUB_ID_N, owner)), rhs));
        else
            return rhs;
    }
    
    public Tree[] genExtendStats(Tree atype, Tree atree) {
        return new Tree[]{
            newdef.VarDecl(NEWARRAY_N, 0, newdef.ArrayTypeTerm(trees.copy(atype)),
                newdef.NewArray(atype, new Tree[]{
                    newdef.Binop(OperatorConst.TIMES,
                        newdef.Select(trees.copy(atree), LENGTH_N), genInt(2))})),
            newdef.Exec(newdef.Apply(trees.Qualid(ARRAYCOPY_N),
                new Tree[]{trees.copy(atree), genInt(0),
                            newdef.Ident(NEWARRAY_N), genInt(0),
                            newdef.Select(trees.copy(atree), LENGTH_N)})),
            newdef.Exec(newdef.Assign(atree, newdef.Ident(NEWARRAY_N)))};
    }
    
    public Tree genCallExtends(XClassDef owner) {
        return newdef.ForLoop(
            new Tree[]{newdef.VarDecl(I_N, 0, trees.toTree(types.intType), genInt(0))},
            newdef.Binop(OperatorConst.LT, newdef.Ident(I_N), newdef.Ident(prefix(SUB_IDS_N, owner))),
            new Tree[]{newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(I_N)))},
            newdef.Exec(newdef.Apply(
                newdef.Select(genIndexed(prefix(EXTENSIONS_N, owner), I_N), TO_STRING_N),
                trees.noTrees)));
    }
    
    public Name prefix(Name name, Definition def) {
        return Name.fromString(mangler.unmangleShort(def.name,
                               def.fullname) + "" + name);
    }
    
    public Tree[] genRuntimeMembers(TreeList moduleMembers, XClassDef owner) {
        Name shortName = mangler.unmangleShort(owner.name, owner.fullname);
        boolean base = (owner == owner.baseClass);
        
        Tree    extensions = base ? newdef.VarDecl(prefix(EXTENSIONS_N, owner),
                                PUBLIC | STATIC | SYNTHETIC,
                                newdef.ArrayTypeTerm(trees.toTree(types.objectType)),
                                newdef.Aggregate(
                                    new Tree[]{trees.Null(), trees.Null(), trees.Null(), trees.Null()},
                                    null)) : null;
        Tree    subIds = base ? newdef.VarDecl(prefix(SUB_IDS_N, owner),
                                    PUBLIC | STATIC | SYNTHETIC,
                                    trees.toTree(types.intType),
                                    null) : null;
        Tree    castable = newdef.VarDecl(prefix(CASTABLE_N, owner), PUBLIC | STATIC | SYNTHETIC,
                                newdef.ArrayTypeTerm(trees.toTree(types.booleanType)),
                                base ? newdef.Aggregate(
                                        new Tree[]{genBool(true), genBool(false), genBool(false), genBool(false)},
                                        null) : null);
        Tree    extend = newdef.MethodDecl(
                                TO_STRING_N, PUBLIC | SYNTHETIC,
                                trees.toTree(types.stringType),
                                new VarDecl[0],
                                trees.noTrees,
                                trees.append(
                                    genExtendStats(trees.toTree(types.booleanType), newdef.Ident(prefix(CASTABLE_N, owner))),
                                    newdef.Return(trees.Null())));
        Tree    subId = newdef.VarDecl(prefix(SUB_ID_N, owner), PUBLIC | STATIC | FINAL | SYNTHETIC,
                                trees.toTree(types.intType),
                                newdef.Apply(
                                    newdef.Select(
                                        trees.ClassName(owner.baseClass.owner),
                                        prefix(REGISTER_N, owner.baseClass)),
                                    new Tree[]{
                                        newdef.NewObj(null,
                                            trees.toTree(types.objectType),
                                            trees.noTrees,
                                            newdef.ClassDecl(null, 0, null, trees.noTrees, new Tree[]{
                                                extend
                                        }))}));
        Tree    staticblock = base ? null : newdef.Block(STATIC,
                    new Tree[]{newdef.Exec(newdef.Assign(
                                newdef.Ident(prefix(CASTABLE_N, owner)),
                                newdef.NewArray(trees.toTree(types.booleanType),
                                    new Tree[]{
                                        newdef.Select(
                                            newdef.Select(
                                            	trees.ClassName(owner.baseClass.owner),
                                                //trees.Qualid(owner.baseClass),//MZ
                                                //trees.ClassName(owner.owner),
                                                prefix(CASTABLE_N, owner.baseClass)),
                                            LENGTH_N)}))),
                               newdef.Exec(genLocalUpdate(owner, owner, genBool(true))),
                               newdef.Exec(genOtherUpdate(owner, owner, genBool(true)))});
        Tree    register = base ? newdef.MethodDecl(
                                prefix(REGISTER_N, owner), PUBLIC | STATIC | SYNTHETIC,
                                trees.toTree(types.intType),
                                trees.Params(new Type[]{types.objectType}),
                                trees.noTrees,
                                new Tree[]{
                                    newdef.If(newdef.Binop(OperatorConst.EQ,
                                        newdef.Ident(prefix(SUB_IDS_N, owner)),
                                        newdef.Select(newdef.Ident(prefix(EXTENSIONS_N, owner)), LENGTH_N)),
                                        newdef.Block(0, trees.append(
                                            genExtendStats(//trees.Qualid(owner),//MZ
                                                           trees.QualidThis(owner),
                                                           newdef.Ident(prefix(EXTENSIONS_N, owner))),
                                            genCallExtends(owner))),
                                        null),
                                    newdef.Exec(newdef.Assign(
                                        genIndexed(prefix(EXTENSIONS_N, owner), prefix(SUB_IDS_N, owner)),
                                        newdef.Ident(trees.paramName(0)))),
                                    newdef.Return(newdef.Unop(
                                        OperatorConst.POSTINC,
                                        newdef.Ident(prefix(SUB_IDS_N, owner))))}) : null;
        Tree    getSubId = newdef.MethodDecl(
                                GET_SUB_ID_N, PUBLIC | SYNTHETIC,
                                trees.toTree(types.intType),
                                new VarDecl[0],
                                trees.noTrees,
                                new Tree[]{
                                    newdef.Return(newdef.Ident(prefix(SUB_ID_N, owner)))});
        Tree    throwException = newdef.MethodDecl(
                                prefix(THROW_CAST_EXC_N, owner),
                                PUBLIC | STATIC | SYNTHETIC,
                                newdef.Ident(shortName),
                                new VarDecl[0],
                                trees.noTrees,
                                new Tree[]{
                                    newdef.Throw(newdef.NewObj(null,
                                        newdef.Ident(CLASS_CAST_EXC_N),
                                        trees.noTrees,
                                        null))});
        if (base) {
            moduleMembers.append(extensions);
            moduleMembers.append(subIds);
            moduleMembers.append(castable);
            moduleMembers.append(subId);
            moduleMembers.append(register);
            moduleMembers.append(throwException);
        } else {
            moduleMembers.append(castable);
            moduleMembers.append(subId);
            moduleMembers.append(staticblock);
        }
        return new Tree[]{getSubId};
    }
    
    protected Tree removeTypecasts(Tree tree) {
        switch (tree) {
            case Typeop(SAVECAST, Tree expr, _):
                return removeTypecasts(expr);
            case Typeop(TYPECAST, Tree expr, _):
                return removeTypecasts(expr);
            default:
                return tree;
        }
    }
    
    protected boolean needForTemporaryInTypeop(Tree expr) {
        switch (expr) {
            case Ident(_, _):
                return false;
            case Select(Ident(_, _), _, _):
                return false;
            default:
                return true;
        }
    }

    protected PatternNode.Factory PatternNodeFactory() {
        return new PatternNodeFactory();
    }
}

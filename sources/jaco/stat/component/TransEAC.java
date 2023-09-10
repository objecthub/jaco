//      /   _ _      JaCo
//  \  //\ / / \     - translation of extensible algebraic types to regular Java
//   \//  \\_\_/     
//         \         Matthias Zenger, 08/04/98

package jaco.stat.component;

import jaco.framework.*;
import jaco.java.component.*;
import jaco.java.struct.*;
import jaco.stat.context.*;
import jaco.stat.struct.*;
import Tree.*;
import PizzaTree.*;
import Definition.*;


public interface EACConst
{
/** names in EACs
 */
    Name    TAG_N = Name.fromString("$tag");
    Name    SUB_ID_N = Name.fromString("$subID");
    Name    SUB_IDS_N = Name.fromString("$subIDs");
    Name    EXTENSIONS_N = Name.fromString("$extensions");
    Name    CASTABLE_N = Name.fromString("$castable");
    Name    REGISTER_N = Name.fromString("$register");
    Name    EXTEND_N = Name.fromString("$extend");
    Name    GET_SUB_ID_N = Name.fromString("$getSubID");
    Name    THROW_CAST_EXC_N = Name.fromString("throw$cast$exception");
    Name    TABLE_N = Name.fromString("table");
    Name    I_N = Name.fromString("i");
    Name    LENGTH_N = Name.fromString("length");
    Name    NEWARRAY_N = Name.fromString("new$array");
    Name    VAR_CASE_N = Name.fromString("$Var");
    
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


public class TransEAC extends Translator
                      implements PizzaModifierConst, OperatorConst, EACConst,
                                 TreeConst, TreeProcessor, TypeConst
{
/** the context
 */
    protected MainPizzaContext      mainContext;
    
/** other components
 */
    protected Constants             constants;
    protected PizzaTypes            types;
    protected PizzaDefinitions      definitions;

/** list of all base types involved in cast expressions within the
 *  current method
 */
    protected Type[]                temporaryVarTypes;
    
/** object that provides labels
 */
    public Labeller                 labeller = new Labeller();
    
    
/** component name
 */
    public String getName()
    {
        return "TransEAC";
    }
    
/** return descrition of tree processor
 */
    public String getDescription()
    {
        return "translating extensible algebraic types";
    }
    
/** default getDebugName method; returns an invalid debug name
 */
    public String getDebugName()
    {
        return "eac";
    }
    
/** component initialization
 */
    public void init(PizzaCompilerContext context)
    {
        super.init(context);
        mainContext = (MainPizzaContext)context.mainContext;
        constants = mainContext.Constants();
        definitions = (PizzaDefinitions)mainContext.Definitions();
        types = (PizzaTypes)mainContext.Types();
    }
    
/** enter code
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        definitions.swapScopes();
        return super.enter(treelist);
    }
    
/** exit code
 */
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        mainContext.algebraicClasses = false;
        return super.exit(treelist);
    }
    
    
//////////// temporary variable management
    
    protected Name temporaryName(int i)
    {
        return Name.fromString("tmp$" + i);
    }
    
    protected Tree temporaryIdent(Type type)
    {
        if (temporaryVarTypes == null)
        {
            temporaryVarTypes = new Type[]{type};
            return newdef.Ident(temporaryName(0));
        }
        for (int i = 0; i < temporaryVarTypes.length; i++)
            if (types.sametype(type, temporaryVarTypes[i]))
                return newdef.Ident(temporaryName(i));
        temporaryVarTypes = types.append(temporaryVarTypes, type);
        return newdef.Ident(temporaryName(temporaryVarTypes.length - 1));
    }
    
    protected Tree[] temporaryDecls()
    {
        if ((temporaryVarTypes == null) || (temporaryVarTypes.length == 0))
            return trees.noTrees;
        else
        {
            Tree[]  ts = new Tree[temporaryVarTypes.length];
            for (int i = 0; i < ts.length; i++)
                ts[i] = newdef.VarDecl(temporaryName(i), 0,
                                    trees.toTree(temporaryVarTypes[i]), null);
            temporaryVarTypes = null;
            return ts;
        }
    }
    

//////////// support for EAC translation 

    public Tree genBool(boolean val)
    {
        if (val)
            return trees.Ident(constants.trueConst);
        else
            return trees.Ident(constants.falseConst);
    }
    
    public Tree genInt(int i)
    {
        return newdef.Literal(constants.make.IntConst(i));
    }
    
    public Tree Null(Type type)
    {
        if (type.isBasic())
        {
            switch (type.tag())
            {
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
        }
        else
            return trees.Null();
    }
    
    public Tree genIndexed(Name array, Name index)
    {
        return newdef.Index(trees.Qualid(array), trees.Qualid(index));
    }
    
    public Tree genIndexed(Name array, Definition c, Name index)
    {
        return newdef.Index(trees.Qualid(array), newdef.Select(trees.ClassName(c), index));
    }
    
    public Tree genIndexed(Definition c, Name array, Name index)
    {
        return newdef.Index(newdef.Select(trees.ClassName(c), array), trees.Qualid(index));
    }
    
    public Tree genLocalUpdate(Definition c, Tree rhs)
    {
        if (definitions.isAlgebraicClass(c))
            return genLocalUpdate(c.supertype().tdef(),
                        newdef.Assign(genIndexed(CASTABLE_N, c, SUB_ID_N), rhs));
        else
            return rhs;
    }
    
    public Tree genOtherUpdate(Definition c, Tree rhs)
    {
        if (definitions.isAlgebraicClass(c))
            return genOtherUpdate(c.supertype().tdef(),
                        newdef.Assign(genIndexed(c, CASTABLE_N, SUB_ID_N), rhs));
        else
            return rhs;
    }
    
    public Tree[] genExtendStats(Tree atype, Tree atree)
    {
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
    
    public Tree genCallExtends()
    {
        return newdef.ForLoop(
            new Tree[]{newdef.VarDecl(I_N, 0, trees.toTree(types.intType), genInt(0))},
            newdef.Binop(OperatorConst.LT, newdef.Ident(I_N), newdef.Ident(SUB_IDS_N)),
            new Tree[]{newdef.Exec(newdef.Unop(OperatorConst.POSTINC, newdef.Ident(I_N)))},
            newdef.Exec(newdef.Apply(
                newdef.Select(genIndexed(EXTENSIONS_N, I_N), EXTEND_N),
                trees.noTrees)));
    }
    
    public Tree[] genRuntimeMembers(CDef owner)
    {
        boolean base = (owner == owner.baseClass);
        
        Tree    extensions = base ? newdef.VarDecl(EACConst.EXTENSIONS_N,
                                PUBLIC | STATIC | SYNTHETIC,
                                newdef.ArrayTypeTerm(trees.Ident(owner)),
                                newdef.Aggregate(
                                    new Tree[]{trees.Null(), trees.Null(), trees.Null(), trees.Null()},
                                    null)) : null;
        
        Tree    subIds = base ? newdef.VarDecl(EACConst.SUB_IDS_N,
                                    PUBLIC | STATIC | SYNTHETIC,
                                    trees.toTree(types.intType),
                                    null) : null;
        
        Tree    castable = newdef.VarDecl(EACConst.CASTABLE_N, PUBLIC | STATIC | SYNTHETIC,
                                newdef.ArrayTypeTerm(trees.toTree(types.booleanType)),
                                base ? newdef.Aggregate(
                                        new Tree[]{genBool(true), genBool(false), genBool(false), genBool(false)},
                                        null) : null);
        
        Tree    subId = newdef.VarDecl(EACConst.SUB_ID_N, PUBLIC | STATIC | FINAL | SYNTHETIC,
                                trees.toTree(types.intType),
                                newdef.Apply(newdef.Ident(REGISTER_N),
                                    new Tree[]{trees.New(owner.name, genInt(-1))}));
        
        Tree    staticblock = base ? null : newdef.Block(STATIC,
                    new Tree[]{newdef.Exec(newdef.Assign(
                                newdef.Ident(CASTABLE_N),
                                newdef.NewArray(trees.toTree(types.booleanType),
                                    new Tree[]{
                                        newdef.Select(
                                            newdef.Select(
                                                //trees.Qualid(owner.baseClass),//MZ
                                                ((PizzaTrees)trees).QualidBase((CDef)owner),
                                                CASTABLE_N),
                                            LENGTH_N)}))),
                               newdef.Exec(genLocalUpdate(owner, genBool(true))),
                               newdef.Exec(genOtherUpdate(owner, genBool(true)))});
        
        Tree    register = base ? newdef.MethodDecl(
                                REGISTER_N, PUBLIC | STATIC | SYNTHETIC,
                                trees.toTree(types.intType),
                                trees.Params(new Type[]{owner.type}),
                                trees.noTrees,
                                new Tree[]{
                                    newdef.If(newdef.Binop(OperatorConst.EQ,
                                        newdef.Ident(SUB_IDS_N),
                                        newdef.Select(newdef.Ident(EXTENSIONS_N), LENGTH_N)),
                                        newdef.Block(0, trees.append(
                                            genExtendStats(//trees.Qualid(owner),//MZ
                                                           ((PizzaTrees)trees).QualidThis(owner),
                                                           newdef.Ident(EXTENSIONS_N)),
                                            genCallExtends())),
                                        null),
                                    newdef.Exec(newdef.Assign(
                                        genIndexed(EXTENSIONS_N, SUB_IDS_N),
                                        newdef.Ident(trees.paramName(0)))),
                                    newdef.Return(newdef.Unop(
                                        OperatorConst.POSTINC,
                                        newdef.Ident(SUB_IDS_N)))}) : null;
                                        
        Tree    extend = newdef.MethodDecl(
                                EXTEND_N, PUBLIC | SYNTHETIC,
                                trees.toTree(types.voidType),
                                new VarDecl[0],
                                trees.noTrees,
                                genExtendStats(trees.toTree(types.booleanType), newdef.Ident(CASTABLE_N)));

        Tree    getSubId = newdef.MethodDecl(
                                GET_SUB_ID_N, PUBLIC | SYNTHETIC,
                                trees.toTree(types.intType),
                                new VarDecl[0],
                                trees.noTrees,
                                new Tree[]{
                                    newdef.Return(newdef.Ident(SUB_ID_N))});
        
        Tree    throwException = newdef.MethodDecl(
                                THROW_CAST_EXC_N,
                                PUBLIC | STATIC | SYNTHETIC,
                                trees.Ident(owner),
                                new VarDecl[0],
                                trees.noTrees,
                                new Tree[]{
                                    newdef.Throw(newdef.NewObj(null,
                                        newdef.Ident(CLASS_CAST_EXC_N),
                                        trees.noTrees,
                                        null))});
        if (base)
            return new Tree[]{extensions, subIds, castable, subId,
                                register, throwException, extend, getSubId};
        else
            return new Tree[]{castable, subId, staticblock,
                                extend, getSubId};
    }
    
    protected Tree removeTypecasts(Tree tree)
    {
        switch (tree)
        {
            case Typeop(SAVECAST, Tree expr, _):
                return removeTypecasts(expr);
                
            case Typeop(TYPECAST, Tree expr, _):
                return removeTypecasts(expr);
            
            default:
                return tree;
        }
    }
    
    protected boolean needForTemporaryInTypeop(Tree expr)
    {
        switch (expr)
        {
            case Ident(_, _):
                return false;
            
            case Select(Ident(_, _), _, _):
                return false;
            
            default:
                return true;
        }
    }
    
    protected Tree translateDecl(Tree tree, Env env)
    {
        switch (tree)
        {
            case PackageDecl(Tree qualid):
                return trees.at(tree).make(
                        redef.PackageDecl(trees.copy(qualid)));
            
            case Import(int tag, Tree qualid):
                if ((tag == IMPORTSTAR) &&
                    definitions.isAlgebraicSubclass(qualid.type.tdef()))
                {
                    TreeList    imports = new TreeList();
                    Type        type = qualid.type;
                    while (types.isAlgebraicType(type))
                    {
                        imports.append(
                            trees.at(tree.pos).make(
                                newdef.Import(tag, trees.toTree(type))));
                        type = type.supertype();
                    }
                    return trees.Container(imports.toArray());
                }
                else
                    return trees.at(tree).make(
                            redef.Import(tag, trees.copy(qualid)));
            
            case ClassDecl(Name name, int mods, Tree extending,
                           Tree[] implementing, Tree[] members, ClassDef c):
                if (definitions.isAlgebraicClass(c))
                    return trees.at(tree).make(
                        redef.ClassDecl(name, mods & ~(ALGEBRAIC | CASEDEF),
                                    trees.copy(extending),
                                    trees.copy(implementing),
                                    trees.append(
                                        genRuntimeMembers((CDef)c),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env)))));
                else
                    return trees.at(tree).make(
                        redef.ClassDecl(name, mods,
                                        trees.copy(extending),
                                        trees.copy(implementing),
                                        transDecls(members,
                                            classEnv((ClassDecl)tree, env))));
            
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):
                if (stats != null)
                {
                    stats = transStats(stats, methodEnv((MethodDecl)tree, env));
                    stats = trees.append(temporaryDecls(), stats);
                }
                return trees.at(tree).make(
                        redef.MethodDecl(name, mods & ~CASEDEF,
                                        transType(restype, env),
                                        transVarDecls(params, env),
                                        transTypes(thrown, env),
                                        stats));
            
            case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                if ((env.enclMethod == null) && (init != null))
                {
                    Tree    tinit = transExpr(init, env);
                    if (temporaryVarTypes != null)
                        return trees.at(tree).make(
                                trees.Container(
                                    new Tree[]{
                                        redef.VarDecl(name, mods & ~CASEDEF,
                                            transType(vartype, env), null),
                                        newdef.Block(0, trees.append(
                                            temporaryDecls(), tinit))}));
                    else
                        return trees.at(tree).make(
                                redef.VarDecl(name, mods & ~CASEDEF,
                                transType(vartype, env), tinit));
                }
                break;
        }
        return super.translateDecl(tree, env);
    }
    
    protected Tree reduceSelected(Tree tree)
    {
        switch (tree)
        {
            case Select(Tree selected, _, Definition def):
                if (def.kind == DefinitionConst.TYP)
                    return null;
                else
                    return reduceSelected(selected);
            
            case Ident(_, _):
                return null;
        }
        return tree;
    }
    
    protected Tree patchStaticAccess(Tree select, Tree tree, Env env)
    {
        switch (select)
        {
            case Select(Tree selected, Name selector, Definition def):
                if (((def.modifiers & STATIC) != 0) &&
                    definitions.isAlgebraicSubclass(def.owner))
                {
                    ((Select)select).selected = trees.ClassName(def.owner);
                    if ((selected = reduceSelected(selected)) != null)
                    {
                        Tree    temp = temporaryIdent(types.objectType);
                        return newdef.If(
                                newdef.Binop(EQ,
                                    newdef.Assign(temp, transExpr(selected, env)),
                                    trees.copy(temp)),
                                tree,
                                Null(tree.type));
                    }
                } else
                    ((Select)select).selected = transExpr(selected, env);
                break;
        }
        return tree;
    }
    
    protected Tree transExprIgnoreSelect(Tree tree, Env env)
    {
        if (tree != null)
            switch (tree)
            {
                case Select(Tree selected, Name selector, _):
                    return trees.at(tree).make(
                            redef.Select(selected, selector));
            }
        return transExpr(tree, env);
    }
    
    protected Tree translateExpr(Tree tree, Env env)
    {
        switch (tree)
        {
            case Typeop(int opcode, Tree expr, Tree clazz):
                Definition  def = clazz.type.tdef();
                
                // change typecasts and insert checks
                if (definitions.isAlgebraicSubclass(def))
                {
                    mainContext.algebraicClasses = false;
                    boolean ignore = types.subtype(expr.type, def.type);
                    mainContext.algebraicClasses = true;
                    if (ignore)
                    {
                        if (opcode == TYPETEST)
                            return trees.at(tree.pos).make(
                                    newdef.Binop(NE, transExpr(expr, env), trees.Null()));
                        else
                            return transExpr(expr, env);
                    }
                    else
                    {
                        Tree    first = transExpr(expr, env);
                        Tree    second;
                        int     old = trees.setPos(tree.pos);
                        if (needForTemporaryInTypeop(first))
                        {
                            second = temporaryIdent(
                                ((opcode != TYPETEST) ||
                                (((CDef)def).baseClass == ((CDef)expr.type.tdef()).baseClass)) ?
                                    ((CDef)def).baseClass.type :
                                    expr.type);
                            first = newdef.Assign(trees.copy(second),
                                ((opcode != TYPETEST) &&
                                (((CDef)def).baseClass != ((CDef)expr.type.tdef()).baseClass)) ?
                                    newdef.Typeop(TYPECAST, first, trees.toTree(((CDef)def).baseClass.type)) :
                                    first);
                        }
                        else
                        {
                            second = trees.copy(first);
                            if ((opcode != TYPETEST) &&
                                (((CDef)def).baseClass != ((CDef)expr.type.tdef()).baseClass))
                                second = newdef.Typeop(TYPECAST, second, trees.toTree(((CDef)def).baseClass.type));
                        }
                        // translate typecasts
                        if (opcode != TYPETEST)
                        {
                            first = newdef.If(
                                    newdef.Binop(OR,
                                        newdef.Binop(EQ, first, trees.Null()),
                                        newdef.Index(
                                            newdef.Select(
                                                trees.toTree(clazz.type),
                                                CASTABLE_N),
                                            newdef.Apply(
                                                newdef.Select(second, GET_SUB_ID_N),
                                                trees.noTrees))),
                                    second,
                                    newdef.Apply(
                                        newdef.Select(
                                            trees.ClassName(((CDef)def).baseClass),
                                            THROW_CAST_EXC_N),
                                        trees.noTrees));
                        }
                        // translate instanceof
                        else
                        {
                            if (((CDef)def).baseClass == ((CDef)expr.type.tdef()).baseClass)
                            {
                                first = newdef.Binop(AND,
                                            newdef.Binop(NE, first, trees.Null()),
                                            newdef.Index(
                                                newdef.Select(
                                                    trees.toTree(clazz.type),
                                                    CASTABLE_N),
                                                newdef.Apply(
                                                    newdef.Select(second, GET_SUB_ID_N),
                                                    trees.noTrees)));
                            }
                            else
                            {
                                first = newdef.Binop(AND,
                                            newdef.Typeop(TYPETEST, first,
                                                trees.toTree(((CDef)def).baseClass.type)),
                                            newdef.Index(
                                                newdef.Select(
                                                    trees.toTree(clazz.type),
                                                    CASTABLE_N),
                                                newdef.Apply(
                                                    newdef.Select(
                                                        newdef.Typeop(TYPECAST, second,
                                                            trees.toTree(((CDef)def).baseClass.type)),
                                                        GET_SUB_ID_N),
                                                    trees.noTrees)));
                            }
                        }
                        trees.setPos(old);
                        return first;
                    }
                }
                else
                // check impossible casts from 'this' to a CASEDEF
                if ((opcode != TYPETEST) &&
                    (def != null) &&
                    ((def.modifiers & CASEDEF) != 0))
                {
                    expr = removeTypecasts(expr);
                    switch (expr)
                    {
                        case Self(_, int tag, Definition d):
                            if (tag == THIS)
                            {
                                mainContext.algebraicClasses = false;
                                boolean possible = types.subtype(tree.type, expr.type);
                                mainContext.algebraicClasses = true;
                                if (!possible)
                                    report.error(tree.pos, "impossible to cast this to type " + tree.type);
                            }
                            break;
                    }
                }
                return trees.at(tree).make(
                        redef.Typeop(opcode,
                                     transExpr(expr, env),
                                     transType(clazz, env)));
            
            case Apply(Tree fn, Tree[] args):
                return trees.at(tree).make(patchStaticAccess(
                        fn = transExprIgnoreSelect(fn, env),
                        redef.Apply(fn, transExprs(args, env)), env));
            
            case Assign(Tree lhs, Tree rhs):
                return trees.at(tree).make(patchStaticAccess(
                        lhs = transExprIgnoreSelect(lhs, env),
                        redef.Assign(lhs, transExpr(rhs, env)), env));
            
            case Assignop(int opcode, Tree lhs, Tree rhs, _):
                return trees.at(tree).make(patchStaticAccess(
                        lhs = transExprIgnoreSelect(lhs, env),
                        redef.Assignop(opcode, lhs,
                                        transExpr(rhs, env)), env));
            
            case Binop(int opcode, Tree lhs, Tree rhs, _):
                return trees.at(tree).make(patchStaticAccess(
                        lhs = transExprIgnoreSelect(lhs, env),
                        redef.Binop(opcode, lhs,
                                    transExpr(rhs, env)), env));
            
            case Unop(int opcode, Tree operand, _):
                return trees.at(tree).make(patchStaticAccess(
                        operand = transExprIgnoreSelect(operand, env),
                        redef.Unop(opcode, operand), env));
            
            case Select(Tree selected, Name selector, Definition def):
                if (selector == PredefConst.CLASS_N)
                    return trees.at(tree).make(
                            redef.Select(selected, selector));
                else
                    return trees.at(tree).make(patchStaticAccess(
                            selected = transExprIgnoreSelect(selected, env),
                            redef.Select(selected, selector), env));
        }
        return super.translateExpr(tree, env);
    }
    
    protected Tree translateStat(Tree tree, Env env)
    {
        switch (tree)
        {
            case Exec(Apply(Select(Tree selected, Name fn, Definition def), Tree[] args)):
                if (((def.modifiers & STATIC) != 0) &&
                    definitions.isAlgebraicSubclass(def.owner))
                {
                    Tree    call = trees.at(tree).make(
                                redef.Exec(newdef.Apply(
                                    newdef.Select(
                                        trees.ClassName(def.owner), fn),
                                    transExprs(args, env))));
                    if ((selected = reduceSelected(selected)) != null)
                        call = trees.at(tree.pos).make(
                                newdef.Block(0, new Tree[]{
                                    newdef.Exec(newdef.Assign(
                                        temporaryIdent(types.objectType),
                                        transExpr(selected, env))),
                                    call}));
                    return call;
                }
                break;
            
            case Switch(Tree selector, Case[] cases):
                int tag = selector.type.tag();
                if ((tag >= MIN_BASICTYPE_TAG) && (tag <= INT))
                    return super.translateStat(tree, env);
                else
                {
                    PatternMatcher  pm = new PatternMatcher(
                                                PatternNodeFactory(),
                                                this, trees, pretty, report,
                                                transExpr(selector, env).
                                                    setType(selector.type));
                    pm.printNodes = debug(8);
                    cases = transCases(cases, env);
                    int len = cases.length - 1;
                    for (int i = 0; i < len; i++)
                        pm.addCase(cases[i]);
                    if (len >= 0)
                        pm.addLastCase(cases[len]);
                    if (debug(4))
                        pm.print();
                    return pm.toTree();
                }
        }
        return super.translateStat(tree, env);
    }
    
    protected Tree translateType(Tree tree, Env env)
    {
        switch (tree)
        {
            case Select(_, _, Definition c):
                if (definitions.isAlgebraicClass(c))
                    //return trees.Qualid(((CDef)c).baseClass);//MZ
                    return ((PizzaTrees)trees).QualidBase((CDef)c);
                else
                if (definitions.isCase(c))
                    return trees.ClassName(c);
                else
                    return trees.copy(tree);
                
            case Ident(_, Definition c):
                if (definitions.isAlgebraicClass(c))
                    //return trees.Qualid(((CDef)c).baseClass);
                    return ((PizzaTrees)trees).QualidBase((CDef)c);
                else
                    return trees.copy(tree);
        }
        return super.translateType(tree, env);
    }
    
    protected PatternNode.Factory PatternNodeFactory()
    {
        return new PatternNodeFactory();
    }
}

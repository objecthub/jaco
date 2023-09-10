//      /   _ _      JaCo
//  \  //\ / / \     - translation of pattern matching to plain Java
//   \//  \\_\_/     
//         \         Matthias Zenger, 24/01/99

package jaco.stat.component;

import jaco.framework.*;
import jaco.stat.struct.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import Tree.*;
import PatternNode.*;
import Definition.*;


public class PatternMatcher implements DefinitionConst, PizzaModifierConst,
                                       TypeConst, OperatorConst, EACConst, TreeConst
{
    protected PrettyPrinter         pretty;
    protected PatternNode.Factory   make;
    protected Trees                 trees;
    protected TreeCreate            newdef;
    protected PizzaTypes            types;
    protected PizzaDefinitions      definitions;
    protected ErrorHandler          report;
    protected Labeller              labeller;
    protected SwitchHeader          root;
    protected Tree                  selector;
    protected Name                  switchLabel;
    protected TransEAC              transEAC;
    protected Mangler               mangler;
    protected int                   errors = 0;
    protected boolean               thisSwitch = false;
    public boolean                  printNodes = false;
    
    
//////////// builder methods

    public PatternMatcher(PatternNode.Factory make,
                          TransEAC transEAC,
                          Trees trees,
                          PrettyPrinter pretty,
                          ErrorHandler report,
                          Tree selector)
    {
        this.make = make;
        this.types = transEAC.types;
        this.definitions = transEAC.definitions;
        this.report = report;
        this.selector = selector;
        this.labeller = transEAC.labeller;
        this.switchLabel = labeller.newLabel();
        this.trees = trees;
        this.newdef = trees.newdef;
        this.pretty = pretty;
        this.transEAC = transEAC;
        Tree    switchTree;
        switch (selector)
        {
            case Self(_, _, _):
                switchTree = selector;
                thisSwitch = true;
                break;
                
            case Ident(_, _):
            case Select(Ident(_, _), _, _):
                switchTree = selector;
                break;
            
            default:
                switchTree = trees.make.Ident(labeller.newTemporary()).at(selector.pos);
                break;
        }
        this.root = (SwitchHeader)make.SwitchHeader(
                                    selector.pos, types.algebraicSupertype(selector.type),
                                    switchTree, labeller.newTemporary());
    }
    
    public void print()
    {
        print(root, "");
    }
    
    public void print(PatternNode patNode, String indent)
    {
        if (patNode == null)
            System.out.println(indent + "NULL");
        else
            switch (patNode)
            {
                case SwitchHeader(Tree tree, Name casted, SwitchHeader next):
                    System.out.print(indent + "SWITCH(" + patNode.type + ", ");
                    pretty.printExpr(tree);
                    System.out.println(", " + casted + ")");
                    print(patNode.or, indent + "|");
                    if (next != null)
                        print(next, indent);
                    break;
                
                case ConstrPat(int tag, _):
                    String  s = "-- " + patNode.type.tdef().name + "(" + tag + ") -> ";
                    String  ind = indent;
                    indent = (patNode.or != null) ?
                                    indent :
                                    indent.substring(0, indent.length() - 1) + " ";
                    for (int i = 0; i < s.length(); i++)
                        indent += " ";
                    System.out.println(ind + s);
                    print(patNode.and, indent);
                    if (patNode.or != null)
                        print(patNode.or, ind);
                    break;
                
                case DefaultPat():
                    System.out.println(indent + "-- _ -> ");
                    print(patNode.and, indent.substring(0, indent.length() - 1) +
                                        "         ");
                    if (patNode.or != null)
                        print(patNode.or, indent);
                    break;
                
                case ConstantPat(Constant value):
                    String  s = "-- CONST(" + value + ") -> ";
                    String  ind = indent;
                    indent = (patNode.or != null) ?
                                    indent :
                                    indent.substring(0, indent.length() - 1) + " ";
                    for (int i = 0; i < s.length(); i++)
                        indent += " ";
                    System.out.println(ind + s);
                    print(patNode.and, indent);
                    if (patNode.or != null)
                        print(patNode.or, ind);
                    break;
                
                case BodyNode(_, _):
                    System.out.println(indent + "BODY");
                    break;
            }
    }
    
    protected void reportError(int pos, String message)
    {
        report.error(pos, message);
        errors++;
    }
    
    public void addCase(Case c)
    {
        if (c.pat.length > 0)
        {
            fixBreaks(c.stats);
            int i = 0;
            if (thisSwitch)
                while ((i < c.pat.length) && (c.pat[i] != null) &&
                        !types.javaSubtype(c.pat[i].type, root.type))
                    i++;
            if (i == c.pat.length)
                return;
            CaseEnv env = enter(c.pos, c.pat[i], c.stats, null);
            while ((++i) < c.pat.length)
                if (!thisSwitch || types.javaSubtype(c.pat[i].type, root.type))
                    enter((c.pat[i] != null) ? c.pat[i].pos : c.pos,
                          c.pat[i], trees.copy(c.stats), env);
        }
    }
    
    public void addLastCase(Case c)
    {
        addCase((Case)trees.at(c).make(trees.redef.Case(c.pat,
                    trees.append(c.stats,
                        trees.make.Break(null).at(Position.NOPOS)))));
    }
    
    protected CaseEnv enter(int pos, Tree pat, Tree[] stats, CaseEnv env)
    {
        CaseEnv cenv = new CaseEnv();
        if (env == null)
            env = cenv;
        if (match(pat, root) != null)
            reportError(pat.pos, "case not reachable or redundant");
        else
        {
            PatternNode target = enter(pat, null, root, root, env);
            if (target.and != null)
                reportError(pat.pos, "duplicate case");
            else
            if (!cenv.equals(env))
                reportError(pat.pos, "illegal combination of cases");
            target.and = make.BodyNode(pos, cenv.boundVars(), stats);
        }
        return env;
    }
    
    public PatternNode match(Tree pat, PatternNode target)
    {
        PatternNode     next = target.and;
        Tree[]          args = patternArgs(pat);
        
        if (next == null)
            return null;
        switch (next)
        {
            case BodyNode(_, _):
                return next;
        }
        SwitchHeader    header = (SwitchHeader)next;
        PatternNode     patn = patternNode(pat, header, null);
        do
        {
            next = header;
            while ((next = next.or) != null)
                if (samePat(next, patn) && ((target = match(args, next)) != null))
                    return target;
                else
                if (isDefaultPat(next))
                    return next;
            header = header.next;
        } while (header != null);
        return null;
    }
    
    public PatternNode match(Tree[] pats, PatternNode target)
    {
        for (int i = 0; i < pats.length; i++)
            if ((target = match(pats[i], target)) == null)
                return null;
        return target;
    }
    
    public PatternNode enter(Tree pat, Definition field, PatternNode target,
                             SwitchHeader header, CaseEnv env)
    {
        PatternNode     next = target.and;
        Tree[]          patArgs = patternArgs(pat);
        Definition[]    caseMem = caseMembers(pat);
        int             pos = (pat == null) ? header.switchTree.pos : pat.pos;
        
        if (next == null)
        {
            target.and = (header = (SwitchHeader)make.SwitchHeader(pat.pos, field.type,
                                    trees.make.Select(
                                        trees.make.Ident(header.casted).at(pat.pos),
                                        field.name).at(pos),
                                    labeller.newTemporary()));
            return enter(patArgs, caseMem, header.or = patternNode(pat, header, env),
                         header, env);
        }
        header = (SwitchHeader)next;
        SwitchHeader    curHeader = header;
        while (curHeader.next != null)
            curHeader = curHeader.next;
        PatternNode     patNode = patternNode(pat, curHeader, env);
        next = curHeader;
        while (true)
            if (samePat(next, patNode))
                return enter(patArgs, caseMem, next, curHeader, env);
            else
            if (isDefaultPat(next) ||
                ((next.or == null) && types.isAlgebraicType(header.type) &&
                 isDefaultPat(patNode)))
                return enter(patArgs, caseMem,
                        (curHeader = (curHeader.next =
                            (SwitchHeader)make.SwitchHeader(patNode.pos, header.type,
                                                header.switchTree, header.casted)))
                        .or = patNode, curHeader, env);
            else
            if (next.or == null)
                return enter(patArgs, caseMem, next.or = patNode, curHeader, env);
            else
                next = next.or;
    }
    
    public PatternNode enter(Tree[] pats, Definition[] fields, PatternNode target,
                             SwitchHeader header, CaseEnv env)
    {
        for (int i = 0; i < pats.length; i++)
            target = enter(pats[i], fields[i], target, header, env);
        return target;
    }
    
    private Type algebraicBasetype(Type type) {
        if (type == null)
        return null;
    else if ((type.tdef().modifiers & ALGEBRAIC) == 0)
        return type;
    else
        return algebraicBasetype(type.supertype());
    }
    
    protected PatternNode patternNode(Tree tree, SwitchHeader header, CaseEnv env)
    {
        if (tree == null)                               // default:
            return make.DefaultPat(header.pos, header.type);
        else                                            // constant
        if (tree.type.isTypeOfConstant())
            return make.ConstantPat(tree.pos, tree.type);
        else
            switch (tree)
            {
                case Apply(Tree fn, _):                 // pattern with args
                    return make.ConstrPat(tree.pos,
                            definitions.getCaseDefFromConstr(fn.def()).type,
                            definitions.getCaseTag(fn.def()),
                            fn.type.argtypes().length);
                
                case VarDecl(Name name, _, Tree vartype, _, VarDef v):
                    if (name == null)                   // blank
                        return make.DefaultPat(tree.pos, header.type);
                    else                                // (type v)
                    if (types.sametype(algebraicBasetype(vartype.type),
                                       algebraicBasetype(header.type)))
                    {
                        if ((env != null) && ((v.modifiers & USED) != 0))
                            env.newBoundVar(tree.pos, name, vartype.type, header);
                        return make.DefaultPat(tree.pos, vartype.type);
                    }
                    else                                // (Case v)
                    if (definitions.isCase(vartype.type.tdef()))
                    {
                        int tag;
                        if ((env != null) && ((v.modifiers & USED) != 0))
                            env.newBoundVar(tree.pos, name, vartype.type, header);
                        return make.ConstrPat(tree.pos,
                            vartype.type,
                            tag = definitions.getCaseTag(vartype.type.tdef()),
                            definitions.getCaseConstr(header.type.tdef(), tag).type.argtypes().length);
                    }
                    else
                        throw new InternalError("cannot match " + vartype.type +
                                                " with " + header.type);
                case Select(_, _, Definition def):      // pattern without args
                    return make.ConstrPat(
                            tree.pos, def.type,
                            definitions.getCaseTag(def), 0);
                
                case Ident(_, Definition def):          // pattern without.args
                    return make.ConstrPat(
                            tree.pos, def.type,
                            definitions.getCaseTag(def), 0);
                
                default:
                    throw new InternalError();
            }
    }
    
    protected Tree[] patternArgs(Tree tree)
    {
        if (tree != null)
            switch (tree)
            {
                case Apply(_, Tree[] args):
                    return args;
            }
        return new Tree[0];
    }
    
    protected Definition[] caseMembers(Tree tree)
    {
        if (tree != null)
            switch (tree)
            {
                case Apply(Tree fn, _):
                    return definitions.getCaseMembers(definitions.
                                    getCaseDefFromConstr(fn.def()));
            }
        return new Definition[0];
    }
    
    protected boolean isDefaultPat(PatternNode p)
    {
        switch (p)
        {
            case DefaultPat():
                return true;
            
            default:
                return false;
        }
    }
    
    protected boolean samePat(PatternNode p, PatternNode q)
    {
        switch (p)
        {
            case ConstrPat(int ptag, _):
                switch (q)
                {
                    case ConstrPat(int qtag, _):
                        return ptag == qtag;
                }
                return false;
            
            case ConstantPat(Constant pval):
                switch (q)
                {
                    case ConstantPat(Constant qval):
                        return pval.equals(qval);
                }
                return false;
            
            case DefaultPat():
                switch (q)
                {
                    case DefaultPat():
                        return true;
                }
                return false;
        }
        return false;
    }


//////////// generator methods
    
    protected Tree typeToTree(Type t)
    {
        if (types.isAlgebraicType(t))
            t = ((CDef)t.tdef()).baseClass.type;
        return trees.toTree(t);
    }
    
    protected Tree genVarDecl(Name name, Type type, Tree init, Type initType)
    {
        if (!types.subtype(initType, type))
            init = newdef.Typeop(TYPECAST, init, typeToTree(type));
        return newdef.VarDecl(name, FINAL, typeToTree(type), init);
    }
    
    protected Tree genBlock(Tree[] stats)
    {
        if (stats.length == 1)
            switch (stats[0])
            {
                case Block(_, _):
                case Switch(_, _):
                case If(_, _, _):
                    return stats[0];
            }
        return newdef.Block(0, stats);
    }
    
    protected Tree[] markExhaustive()
    {
        Node    exhaustGraph = new Node();
        
        toGraph(root, exhaustGraph);
        if (printNodes)
            exhaustGraph.print();
        if (exhaustGraph.reduce().complete())
            return new Tree[]{
                    newdef.Throw(newdef.NewObj(null,
                        newdef.Ident(ERROR_N), new Tree[0], null)).
                    at(Position.NOPOS)};
        else
            return new Tree[0];
    }
    
    public Tree toTree()
    {
        if (errors > 0)
            return trees.errorTree;
        else
        if (root.switchTree == selector)
            return checkCode(trees.at(selector.pos).make(
                    newdef.Labelled(switchLabel,
                        genBlock(trees.append(toTree(root),
                                              markExhaustive())))));
        else
            return checkCode(trees.at(selector.pos).make(
                newdef.Labelled(switchLabel,
                    genBlock(trees.append(
                            genVarDecl(((Ident)root.switchTree).name, root.type,
                                       trees.copy(selector), root.type),
                            trees.append(toTree(root),
                                         markExhaustive()))))));
    }
    
    protected Tree[] toTree(PatternNode patNode)
    {
        Tree[]  result = trees.noTrees;
        while (patNode != null)
            switch (patNode)
            {
                case SwitchHeader(Tree sel, _, SwitchHeader next):
                    int tag = patNode.type.tag();
                    if ((tag >= MIN_BASICTYPE_TAG) && (tag <= INT))
                        result = trees.append(result, toSwitch((SwitchHeader)patNode, sel));
                    else
                    if (types.isAlgebraicType(patNode.type))
                        result = trees.append(result, toSwitch((SwitchHeader)patNode,
                                        trees.at(patNode.pos).make(
                                            newdef.Select(sel, TAG_N))));
                    else
                    if ((tag >= MIN_BASICTYPE_TAG) && (tag <= MAX_BASICTYPE_TAG))
                        result = trees.append(result, toIf(patNode.or,
                                                        (SwitchHeader)patNode, sel));
                    else
                        result = trees.append(result, toIfEquals(patNode.or, sel));
                    patNode = next;
                    break;
                    
                case BodyNode(VarDecl[] decls, Tree[] stats):
                    result = new Tree[]{
                                newdef.Block(CASEBLOCK,
                                    trees.append(result,
                                        trees.append(decls, stats)))};
                    patNode = null;
                    break;
                
                default:
                    throw new InternalError();
            }
        return result;
    }
    
    protected Case toCase(PatternNode patNode, SwitchHeader header)
    {
        switch (patNode)
        {
            case ConstrPat(int tag, int args):
                return (Case)trees.at(patNode.pos).make(
                        newdef.Case(new Tree[]{transEAC.genInt(tag)},
                            new Tree[]{
                                genBlock(trees.append(
                                    (args > 0) ?
                                        new Tree[]{newdef.VarDecl(
                                            header.casted, FINAL,
                                            typeToTree(patNode.type),
                                            newdef.Typeop(SAVECAST,
                                                trees.copy(header.switchTree),
                                                typeToTree(patNode.type)))} :
                                        null,
                                    toTree(patNode.and))),
                                newdef.Break(null).at(Position.NOPOS)}));
            
            case ConstantPat(Constant value):
                return (Case)trees.at(patNode.pos).make(
                        newdef.Case(new Tree[]{transEAC.genInt(value.intValue())},
                                    new Tree[]{genBlock(toTree(patNode.and)),
                                               newdef.Break(null).at(Position.NOPOS)}));
            
            case DefaultPat():
                return (Case)trees.at(patNode.pos).make(
                        newdef.Case(new Tree[]{null},
                                    new Tree[]{genBlock(toTree(patNode.and))}));
            
            default:
                throw new InternalError();
        }
    }
    
    protected Tree toIf(PatternNode patNode, SwitchHeader header, Tree selector)
    {
        if (patNode == null)
            return null;
        switch (patNode)
        {
            case ConstrPat(int tag, int args):
                return trees.at(patNode.pos).make(
                        newdef.If(
                            newdef.Binop(EQ, trees.copy(selector), transEAC.genInt(tag)),
                            genBlock(trees.append(
                                    (args > 0) ?
                                        new Tree[]{newdef.VarDecl(
                                            header.casted, FINAL,
                                            typeToTree(patNode.type),
                                            newdef.Typeop(SAVECAST,
                                                trees.copy(header.switchTree),
                                                typeToTree(patNode.type)))} :
                                        null,
                                    toTree(patNode.and))),
                            toIf(patNode.or, header, selector)));
            
            case ConstantPat(Constant value):
                return trees.at(patNode.pos).make(
                        newdef.If(
                            newdef.Binop(EQ, trees.copy(selector),
                                newdef.Literal(value)),
                            genBlock(toTree(patNode.and)),
                            toIf(patNode.or, header, selector)));
            
            case DefaultPat():
                return trees.at(patNode.pos).make(
                        genBlock(toTree(patNode.and)));
            
            default:
                throw new InternalError();
        }
    }
    
    protected Tree toIfEquals(PatternNode patNode, Tree selector)
    {
        if (patNode == null)
            return null;
        switch (patNode)
        {
            case ConstantPat(Constant value):
                return trees.at(patNode.pos).make(
                        newdef.If(
                            newdef.Apply(
                                newdef.Select(trees.copy(selector), EQUALS_N),
                                new Tree[]{newdef.Literal(value)}),
                            genBlock(toTree(patNode.and)),
                            toIfEquals(patNode.or, selector)));
            
            case DefaultPat():
                return trees.at(patNode.pos).make(genBlock(toTree(patNode.and)));
            
            default:
                throw new InternalError();
        }
    }
    
    protected int numCases(SwitchHeader header, boolean full, boolean withDefault)
    {
        int num = 0;
        do
        {
            PatternNode patNode = header;
            while ((patNode = patNode.or) != null)
                switch (patNode)
                {
                    case DefaultPat():
                        if (withDefault)
                            num++;
                        break;
                    
                    default:
                        num++;
                        break;
                }
            header = header.next;
        } while (full && (header != null));
        return num;
    }
    
    protected Tree toSwitch(SwitchHeader header, Tree selector)
    {
        int n = numCases(header, false, false);
        if (n == 0)
        {
            if (header.or == null)
                return trees.at(header.pos).make(
                        newdef.Block(0, new Tree[0]));
            else
                return trees.at(header.pos).make(
                        genBlock(toTree(header.or.and)));
        }
        else if (n > 1)
        {
            PatternNode patNode = header;
            Case[]      cases = new Case[numCases(header, false, true)];
            for (int i = 0; i < cases.length; i++)
                cases[i] = toCase(patNode = patNode.or, header);
            return trees.at(header.pos).make(
                    newdef.Switch(selector, cases));
        }
        else
            return toIf(header.or, header, selector);
    }
    
    
//////////// exhaustive check
    
    protected PatternNode defaultPatterns(Definition[] defs, PatternNode succ)
    {
        if (defs.length > 0)
        {
            PatternNode start = make.SwitchHeader(succ.pos, defs[0].type, null, null);
            PatternNode node = start;
            node = (node.or = make.DefaultPat(succ.pos, defs[0].type));
            for (int i = 1; i < defs.length; i++)
            {
                node.and = make.SwitchHeader(succ.pos, defs[i].type, null, null);
                node = (node.and.or = make.DefaultPat(succ.pos, defs[i].type));
            }
            node.and = succ;
            return start;
        }
        else
            return succ;
    }
    
    protected void toGraph(PatternNode patNode, Node last)
    {
        if (patNode == null)
            return;
        switch (patNode)
        {
            case BodyNode(_, _):
                if (last.next() != null)
                    throw new InternalError();
                break;
                
            case SwitchHeader(_, _, _):
                if (types.isAlgebraicType(patNode.type) &&
                    ((patNode.type.tdef().modifiers & ALG_FINAL) != 0))
                {
                    Node    composite;
                    SwitchHeader    header = (SwitchHeader)patNode;
                    if (numCases((SwitchHeader)patNode, true, false) == 0)
                        composite = last.append(new Node());
                    else
                        composite = last.expandNext(
                                        definitions.getCaseArgNums(patNode.type.tdef()));
                    do
                    {
                        while ((patNode = patNode.or) != null)
                            toGraph(patNode, composite);
                        patNode = (header = header.next);
                    } while (header != null);
                }
                else
                {
                    int             num = 0;
                    SwitchHeader    header = (SwitchHeader)patNode;
                    do
                    {
                        while ((patNode = patNode.or) != null)
                            switch (patNode)
                            {
                                case DefaultPat():
                                    if ((num++) == 0)
                                        last = last.append(new Node());
                                    toGraph(patNode.and, last);
                            }
                        patNode = (header = header.next);
                    } while (header != null);
                    if (num == 0)
                        last.append(new Node(-1));
                }
                break;
                
            case ConstrPat(int tag, int args):
                toGraph(patNode.and, last.insert(tag, args));
                break;
                
            case DefaultPat():
                if (last.complete())
                    toGraph(patNode.and, last);
                else
                {
                    Definition[][]  argdefs = definitions.getCaseArgDefs(patNode.type.tdef());
                    for (int i = 0; i < argdefs.length; i++)
                        toGraph(defaultPatterns(argdefs[i], patNode.and),
                                last.insert(i, argdefs[i].length));
                }
                break;
                
            default:
                throw new InternalError();
        }
    }   


//////////// generated code analysis
    
    public static class CheckEnv
    {
        CheckEnv    outer = null;
        int         level;
        boolean     alive = true;
        int         dead = 0; // 0 = not dead, 1 = dead by default, 2 = dead by throw
        Name        label;
        boolean     referenced = false;
        boolean     breakable = false;
        
        public CheckEnv(CheckEnv outer)
        {
            this(outer, outer.alive);
            this.dead = outer.dead;
        }
        
        public CheckEnv(CheckEnv outer, boolean alive)
        {
            this.outer = outer;
            this.alive = alive;
            if (outer != null)
                this.level = outer.level + 1;
        }
        
        public int refEncl()
        {
            CheckEnv    env = this;
            while (env != null)
                if (env.breakable)
                {
                    env.referenced = true;
                    return env.level;
                }
                else
                    env = env.outer;
            return -1;
        }
        
        public int refLabel(Name label)
        {
            CheckEnv    env = this;
            while (env != null)
                if (env.label == label)
                {
                    env.referenced = true;
                    return env.level;
                }
                else
                    env = env.outer;
            return -1;
        }
    }
    
    public Tree checkCode(Tree tree)
    {
        return checkCode(tree, new CheckEnv(null, true));
    }
    
    public Tree[] checkCode(Tree[] trees, CheckEnv env)
    {
        if (trees == null)
            return null;
        int j = 0;
        for (int i = 0; i < trees.length; i++)
            if ((trees[j] = checkCode(trees[i], env)) != null)
                j++;
        if (j < trees.length)
        {
            Tree[]  newTrees = new Tree[j];
            System.arraycopy(trees, 0, newTrees, 0, j);
            return newTrees;
        }
        return trees;
    }
    static int num = 0;
    
    public Tree checkCode(Tree tree, CheckEnv env)
    {
        if (tree == null)
            return tree;
        switch (tree)
        {
            case Block(int mods, Tree[] stats):
                ((Block)tree).stats = checkCode(stats, env);
                if ((mods & CASEBLOCK) != 0)
                {
                    ((Block)tree).mods ^= CASEBLOCK;
                    if (env.alive)
                        report.error(tree.pos, "possible fall-through to another " +
                                     "case pattern");
                }
                break;
                
            case Loop(int tag, Tree cond, Tree body):
                CheckEnv    cenv = new CheckEnv(env);
                cenv.breakable = true;
                if (cond.type.isTypeOfConstant())
                {
                    boolean     value = (cond.type.tconst().intValue() != 0);
                    if ((tag == WHILE) && !value)
                    {
                        cenv.alive = false;
                        ((Loop)tree).body = checkCode(body, cenv);
                    }
                    else
                    {
                        ((Loop)tree).body = checkCode(body, cenv);
                        env.alive = (cenv.alive && !value) | cenv.referenced;
                    }
                }
                else
                if (tag == WHILE)
                    ((Loop)tree).body = checkCode(body, cenv);
                else
                {
                    ((Loop)tree).body = checkCode(body, cenv);
                    env.alive = cenv.alive | cenv.referenced;
                }
                env.dead = cenv.dead;
                break;
                
            case ForLoop(_, Tree cond, _, Tree body):
                CheckEnv    cenv = new CheckEnv(env);
                cenv.breakable = true;
                if ((cond != null) && cond.type.isTypeOfConstant())
                {
                    if (cond.type.tconst().intValue() == 0)
                    {
                        cenv.alive = false;
                        ((ForLoop)tree).body = checkCode(body, cenv);
                    }
                    else
                    {
                        ((ForLoop)tree).body = checkCode(body, cenv);
                        env.alive = false;
                    }
                }
                else
                {
                    ((ForLoop)tree).body = checkCode(body, cenv);
                    env.alive = (cond == null);
                }
                env.alive |= cenv.referenced;
                env.dead = cenv.dead;
                break;
                
            case Labelled(Name label, Tree body):
                CheckEnv    cenv = new CheckEnv(env);
                cenv.label = label;
                Tree    t = checkCode(body, cenv);
                if (!cenv.referenced)
                {
                    env.alive = cenv.alive;
                    env.dead = cenv.dead;
                    tree = t;
                }
                else
                {
                    ((Labelled)tree).body = t;
                    env.dead = cenv.dead;
                }
                break;
                
            case Switch(_, Case[] cases):
                CheckEnv    defaultEnv = null;
                CheckEnv    cenv = null;
                boolean     dead = true;
                int         deadCode = 0;
                for (int i = 0; i < cases.length; i++)
                {
                    if (defaultEnv == null)
                        cenv = new CheckEnv(env);
                    cases[i].stats = checkCode(cases[i].stats, cenv);
                    dead &= (cenv.dead > 0);
                    deadCode |= cenv.dead;
                    for (int j = 0; j < cases[i].pat.length; j++)
                        if (cases[i].pat[j] == null)
                            defaultEnv = cenv;
                }
                if (defaultEnv != null)
                {
                    if (dead)
                    {
                        env.alive = false;
                        env.dead = deadCode;
                    }
                    else
                        env.alive = defaultEnv.alive;
                }
                break;
                
            case Synchronized(Tree lock, Tree body):
                ((Synchronized)tree).body = checkCode(body, env);
                break;
                
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                CheckEnv    cenv = new CheckEnv(env);
                ((Try)tree).body = checkCode(body, cenv);
                if (cenv.dead == 2)
                {
                    cenv.alive = true;
                    cenv.dead = 0;
                }
                CheckEnv    caenv = new CheckEnv(env);
                caenv.dead = cenv.dead;
                for (int i = 0; i < catchers.length; i++)
                {
                    caenv.alive = true;
                    catchers[i].body = checkCode(catchers[i].body, caenv);
                }
                CheckEnv    fenv = new CheckEnv(env, true);
                if (finalizer != null)
                {
                    ((Try)tree).finalizer = checkCode(finalizer, fenv);
                    env.alive &= fenv.alive;
                }
                env.dead |= cenv.dead | fenv.dead;
                break;
                
            case Catch(VarDecl exception, Tree body):
                ((Catch)tree).body = checkCode(body, env);
                break;
                
            case If(_, Tree thenpart, Tree elsepart):
                CheckEnv    env1 = new CheckEnv(env);
                CheckEnv    env2 = new CheckEnv(env);
                Tree    t = checkCode(thenpart, env1);
                Tree    s = checkCode(elsepart, env2);
                env.alive = env1.alive || env2.alive;
                env.dead = env1.dead & env2.dead;
                ((If)tree).thenpart = t;
                ((If)tree).elsepart = s;
                break;
            
            case Break(Name label, _):
                if (tree.pos == Position.NOPOS)
                {
                    if (!env.alive)
                        return null;
                }
                if (label == null)
                    env.refEncl();
                else
                    env.refLabel(label);
                env.alive = false;
                break;
            
            case Continue(Name label, _):
                if (label == null)
                    env.refEncl();
                else
                    env.refLabel(label);
                env.alive = false;
                break;
                
            case Return(_, _):
                if (env.alive)
                    env.dead = 1;
                env.alive = false;
                break;
                
            case Throw(_):
                if ((tree.pos == Position.NOPOS) && !env.alive)
                    return null;
                if (env.alive)
                    env.dead = 2;
                env.alive = false;
                break;
        }
        return tree;
    }
    
    protected void fixBreaks(Tree stat)
    {
        if (stat == null)
            return;
        switch (stat)
        {
            case Block(_, Tree[] stats):
                fixBreaks(stats);
                break;
            
            case Labelled(Name label, Tree body):
                fixBreaks(body);
                break;
                
            case Synchronized(_, Tree body):
                fixBreaks(body);
                break;
                
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                fixBreaks(body);
                for (int i = 0; i < catchers.length; i++)
                    fixBreaks(catchers[i].body);
                fixBreaks(finalizer);
                break;
    
            case If(_, Tree thenpart, Tree elsepart):
                fixBreaks(thenpart);
                fixBreaks(elsepart);
                break;
            
            case Break(Name label, Tree target):
                if (label == null)
                    ((Break)stat).label = switchLabel;
                break;
            
            default:
                break;
        }
    }

    protected void fixBreaks(Tree[] stats)
    {
        for (int i = 0; i < stats.length; i++)
            fixBreaks(stats[i]);
    }
    
    
//////////// environment for cases
    
    protected class CaseEnv
    {
        protected VarDecl[]     boundVars = new VarDecl[4];
        protected int           numVars = 0;
        
        public void newBoundVar(int pos, Name name, Type type, SwitchHeader sh)
        {
            if (numVars == boundVars.length)
            {
                VarDecl[]   newVars = new VarDecl[numVars * 2];
                System.arraycopy(boundVars, 0, newVars, 0, numVars);
                boundVars = newVars;
            }
            boundVars[numVars++] = (VarDecl)trees.make.VarDecl(name,
                                        0, typeToTree(type),
                                        sh.switchTree).at(pos).setType(sh.type);
        }
        
        public VarDecl[] boundVars()
        {
            VarDecl[]   newVars = new VarDecl[numVars];
            System.arraycopy(boundVars, 0, newVars, 0, numVars);
            return newVars;
        }
        
        public boolean equals(Object obj)
        {
            if (!(obj instanceof CaseEnv))
                return false;
            CaseEnv env = (CaseEnv)obj;
            if (env.numVars != numVars)
                return false;
            for (int i = 0; i < numVars; i++)
                if ((boundVars[i].name != env.boundVars[i].name) ||
                    !types.sametype(boundVars[i].type, env.boundVars[i].type) ||
                    (boundVars[i].init != env.boundVars[i].init))
                    return false;
            return true;
        }
    }
}

public class Labeller
{
    protected int   labels = 0;
    protected int   temporaries = 0;
    
    public Name newLabel()
    {
        return Name.fromString("label$" + labels++);
    }
    
    public Name newTemporary()
    {
        return Name.fromString("temp$" + temporaries++);
    }
}

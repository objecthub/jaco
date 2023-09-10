//      /   _ _      JaCo
//  \  //\ / / \     - library for abstract syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 18/02/00

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.grammar.*;
import jaco.java.context.*;
import Tree.*;


public interface TreeConst
{
/** opcodes
 */
    /** dereference operation */
    final int   DEREF = 0;

/** operator opcodes range from 1 to 99
 */

/** opcodes that range from 100 to 199
 */
    /** do-loop */
    final int   DO = 100;
    /** while-loop */
    final int   WHILE = 101;
    
    /** single type import */
    final int   IMPORT = 110;
    /** import on demand */
    final int   IMPORTSTAR = 111;
    
    /** super keyword */
    final int   SUPER = 121;
    /** this keyword */
    final int   THIS = 122;
}

public class Trees extends Component
                   implements TreeConst, PredefConst, OperatorConst,
                              DefinitionConst, ModifierConst
{
/** other components
 */
    Mangler                 mangler;
    
/** plain Tree factory
 */
    public Tree.Factory     make;

/** factory with predefined positions
 */
    public TreeCreate       newdef;
    public TreeRedef        redef;
    
    public Tree[]           noTrees = new Tree[0];
    public Tree             errorTree = Tree.Bad();

/** maintanance of positions for tree constructors
 */
    protected int[]         genPosStack = new int[256];
    protected int           genSp = 0;
    protected PresetNewDef  presetNewDef;

/** maintanance of prototype trees for tree redefinition
 */
    protected Tree[]        genTreeStack = new Tree[256];
    protected int           genTreeSp = 0;
    protected PresetReDef   presetReDef;
    
    
    public class PresetNewDef
    {
        public Tree make(Tree tree)
        {
            newdef.protoPos = genPosStack[--genSp];
            return tree;
        }
        
        public Tree[] make(Tree[] trees)
        {
            newdef.protoPos = genPosStack[--genSp];
            return trees;
        }
    }
    
    public class PresetReDef
    {
        public Tree make(Tree tree)
        {
            newdef.protoPos = genPosStack[--genSp];
            redef.protoTree = genTreeStack[--genTreeSp];
            return tree;
        }
        
        public Tree[] make(Tree[] trees)
        {
            newdef.protoPos = genPosStack[--genSp];
            redef.protoTree = genTreeStack[--genTreeSp];
            return trees;
        }
    }
    
    public PresetNewDef at(int pos)
    {
        genPosStack[genSp++] = newdef.protoPos;
        newdef.protoPos = pos;
        return presetNewDef;
    }
    
    public PresetReDef at(Tree tree)
    {
        genPosStack[genSp++] = newdef.protoPos;
        newdef.protoPos = tree.pos;
        genTreeStack[genTreeSp++] = redef.protoTree;
        redef.protoTree = tree;
        return presetReDef;
    }
    
    public int setPos(int pos)
    {
        int old = newdef.protoPos;
        newdef.protoPos = pos;
        return old;
    }
    
/** wrap an array of trees into one tree
 */
    public Tree Container(Tree[] trees)
    {
        return make.Block(CONTAINER, trees);
    }
    
/** return the array of trees stored in a container; return null, iff
 *  'tree' is not a container
 */
    public Tree[] getContainerContent(Tree tree)
    {
        switch (tree)
        {
            case Block(int mods, Tree[] trees):
                if ((mods & CONTAINER) != 0)
                    return trees;
                break;
        }
        return null;
    }
    
/** component name
 */
    public String getName()
    {
        return "JavaTrees";
    }

/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        make = new TreeFactory();
        newdef = new TreeCreate();
        redef = new TreeRedef();
        presetNewDef = new PresetNewDef();
        presetReDef = new PresetReDef();
        
        mangler = context.Mangler();
    }
    
/** current position maintanance
 */
    protected int   sp = 0;
    protected int[] posStack = new int[16];
    
    public void pushPos(int pos)
    {
        if (sp == posStack.length)
        {
            int[]   newStack = new int[2 * sp];
            System.arraycopy(posStack, 0, newStack, 0, sp);
            posStack = newStack;
        }
        posStack[sp++] = pos;
    }
    
    public void popPos()
    {
        sp--;
    }
    
    public int getPos()
    {
        return posStack[sp - 1];
    }
    

/** builder methods
 */
    public TreeList TreeList()
    {
        return new TreeList();
    }
    
    public TreeList TreeList(Tree t)
    {
        return new TreeList(t);
    }
    
    public Modifiers.Wrapper Modifiers(int mods)
    {
        return new Modifiers.Wrapper(mods);
    }
    
    public Tree[] append(Tree[] asts, Tree ast)
    {
        if (asts == null)
            return new Tree[]{ast};
        Tree[]  newTrees = new Tree[asts.length + 1];
        System.arraycopy(asts, 0, newTrees, 0, asts.length);
        newTrees[asts.length] = ast;
        return newTrees;
    }
    
    public Tree[] append(Tree[] asts0, Tree[] asts1)
    {
        if (asts0 == null)
            return asts1;
        else
        if (asts1 == null)
            return asts0;
        Tree[]  newTrees = new Tree[asts0.length + asts1.length];
        System.arraycopy(asts0, 0, newTrees, 0, asts0.length);
        System.arraycopy(asts1, 0, newTrees, asts0.length, asts1.length);
        return newTrees;
    }
    
    public Tree[] append(Tree ast, Tree[] asts)
    {
        if (asts == null)
            return new Tree[]{ast};
        Tree[]  newTrees = new Tree[asts.length + 1];
        System.arraycopy(asts, 0, newTrees, 1, asts.length);
        newTrees[0] = ast;
        return newTrees;
    }
    
    public Tree[] extract(Tree[] xs, int start, Tree[] target)
    {
        System.arraycopy(xs, start, target, 0, target.length);
        return target;
    }
    
    
/** ast tools
 */
    public Name fullName(Tree tree)
    {
        switch (tree)
        {
            case Select(Tree selected, Name selector, _):
                Name sname = fullName(selected);
                if (sname == PredefConst.ERROR_N)
                    return PredefConst.ERROR_N;
                else
                    return sname.append(PredefConst.PERIOD_N).append(selector);
            
            case Ident(Name idname, _):
                return idname;

            default:
                return PredefConst.ERROR_N;
        }
    }

/** helper constructors
 */ 
    public Tree This()
    {
        return newdef.Self(null, THIS);
    }
    
    public Tree Super()
    {
        return newdef.Self(null, SUPER);
    }
    
    public Tree Null()
    {
        return newdef.Ident(PredefConst.NULL_N);
    }
    
    public Tree Class(Tree type)
    {
        return newdef.Select(type, CLASS_N);
    }
    
/** some name constructors
 */
    public Name paramName(int i)
    {
        return Name.fromString("x$" + i);
    }

/** fully qualified name -> Qualid
 */
    public Tree Qualid(Name name)
    {
        int p = name.lastPos((byte)'.');
        if (p >= 0)
            return newdef.Select(Qualid(name.subName(0, p)),
                                    name.subName(p+1, name.length()));
        else
            return newdef.Ident(name);
    }
    
/** definition -> Qualid
 */
    public Tree Qualid(Definition def)
    {
        if ((def.owner == null) || (def.owner.name.length() == 0))
            return Ident(def);
        else
        {
            Definition  owner = def.owner;
            return Select(Qualid(owner), def);
        }
    }

/** tree, definition -> Select
 */
    Tree Select(Tree tree, Definition def)
    {
        return newdef.Select(tree, def.name).setType(def.type).setDef(def);
    }

/** make thrown clause from a list of types
 */
    public Tree[] Thrown(Type[] ttypes)
    {
        Tree[] thr = new Tree[ttypes.length];
        for (int i = 0; i < thr.length; i++)
            thr[i] = toTree(ttypes[i]);
        return thr;
    }
    
/** make parameters from a list of types
 */
    public VarDecl[] Params(Type[] ptypes)
    {
        VarDecl[]   params = new VarDecl[ptypes.length];
        for (int i = 0; i < params.length; i++)
            params[i] = (VarDecl)newdef.VarDecl(paramName(i), 0, toTree(ptypes[i]), null);
        return params;
    }
    
/** make parameters from variables
 */
    public VarDecl[] Params(VarDecl[] ps)
    {
        VarDecl[]   params = new VarDecl[ps.length];
        for (int i = 0; i < params.length; i++)
            params[i] = (VarDecl)newdef.VarDecl(ps[i].name, ps[i].mods & FINAL,
                                                ps[i].vartype, null);
        return params;
    }

/** name -> 'this.name = name';
 */
    public Tree FieldInit(Name name)
    {
        return newdef.Exec(newdef.Assign(newdef.Select(
                        This(), name), newdef.Ident(name)));
    }
    
    public Tree[] FieldInits(VarDecl[] decls)
    {
        Tree[]  inits = new Tree[decls.length];
        
        for (int i = 0; i < decls.length; i++)
            inits[i] = FieldInit(decls[i].name);
        return inits;
    }
    
/** definition -> Ident
 */
    public Tree Ident(Definition def)
    {
        return newdef.Ident(def.name).setType(def.type).setDef(def);
    }

/** default constructors
 */
    public Tree Ident(VarDecl def)
    {
        return newdef.Ident(def.name).setDef(def.def).setType(def.vartype.type);
    }
    
    public Tree[] Idents(VarDecl[] defs)
    {
        Tree[]  ids = new Tree[defs.length];
        for (int i = 0; i < defs.length; i++)
            ids[i] = Ident(defs[i]);
        return ids;
    }
    
    public Tree SuperCall(boolean based, VarDecl[] params)
    {
        Tree    base = null;
        if (based)
        {
            base = newdef.Ident(params[0].name);
            params = (VarDecl[])extract(params, 1, new VarDecl[params.length - 1]);
        }
        return newdef.Exec(newdef.Apply(newdef.Self(base, SUPER),
                            Idents(params)));
    }
    
    public Tree Call(Tree app)
    {
        return (app.type == Type.VoidType) ?
                    newdef.Exec(app) :
                    newdef.Return(app).setType(app.type);
    }

    public Tree App(Tree fn, Tree[] args)
    {
        Tree    tree = newdef.Apply(fn, args);
        if (fn.type != null)
            tree.setType(fn.type.restype());
        return tree;
    }
    
    public Tree DefaultConstructor(int cmods, boolean based, Type[] argtypes)
    {
        VarDecl[] params = Params(argtypes);
        return newdef.MethodDecl(PredefConst.INIT_N, cmods,
                                    null, params, noTrees,
                                    new Tree[]{SuperCall(based, params)});
    }

/** a way to make a method declaration
 */
    public Tree MethodDecl(Definition f, Tree[] stats)
    {
        return newdef.MethodDecl(f.name, f.modifiers, toTree(f.type.restype()),
                                Params(f.type.argtypes()),
                                toTree(f.type.thrown()), stats).setDef(f);
    }
    
/** class definition -> class name
 */
    public Tree ClassName(Definition c)
    {
        if (c.owner.kind == TYP)
                return newdef.Select(ClassName(c.owner),
                                mangler.unmangleShort(c.name, c.fullname))
                            .setDef(c);
        else
            return Qualid(c.fullname);
    }

/** make an object instantiation via new
 */
    public Tree New(Name classname, Tree arg)
    {
        return newdef.NewObj(null, Qualid(classname), new Tree[]{arg}, null);
    }

/** return a syntax tree representing type 't'
 */
    public Tree toTree(Type t)
    {
        switch (t)
        {
            case NumType(_):
            case VoidType:
                return newdef.BasicType(t.tag()).setType(t);
            
            case ClassType(_):
                /* if (unqualifiable(t.tsym()))//TODO
                    return Ident(t.tsym());
                    else */
                return ClassName(t.tdef());
            
            case ArrayType(Type et):
                    return newdef.ArrayTypeTerm(toTree(et)).setType(t);

            default:
                throw new InternalError("bad type: " + t.getClass());
        }
    }
    
    public Tree[] toTree(Type[] ts)
    {
        Tree[]  trees = new Tree[ts.length];
        for (int i = 0; i < ts.length; i++)
            trees[i] = toTree(ts[i]);
        return trees;
    }

/** make a copy of a tree
 */
    public Tree[] copy(Tree[] ts)
    {
        if ((ts == null) || (ts.length == 0))
            return ts;
        else
        {
            Tree[]  ts2 = new Tree[ts.length];
            for (int i = 0; i < ts.length; i++)
                ts2[i] = copy(ts[i]);
            return ts2;
        }
    }

    public Tree[] copy(Tree[] ts, Tree[] ts2)
    {
        for (int i = 0; i < ts.length; i++)
            ts2[i] = copy(ts[i]);
        return ts2;
    }
    
    public Tree copy(Tree tree)
    {
        if (tree == null)
            return null;
        PresetReDef pr = at(tree);
        Tree        res = null;
        
        switch (tree)
        {
            case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                res = redef.CompilationUnit(copy(decls), info);
                break;
            
            case PackageDecl(Tree qualid):
                res = redef.PackageDecl(copy(qualid));
                break;
            
            case Import(int tag, Tree qualid):
                res = redef.Import(tag, copy(qualid));
                break;
            
            case ClassDecl(Name name, int mods, Tree ext, Tree[] impl, Tree[] members, _):
                res = redef.ClassDecl(name, mods, copy(ext), copy(impl), copy(members));
                break;
            
            case MethodDecl(Name name, int mods, Tree rest, VarDecl[] par, Tree[] thrown, Tree[] stats, _):
                res = redef.MethodDecl(name, mods, copy(rest),
                        (VarDecl[])copy(par, new VarDecl[par.length]),
                        copy(thrown), copy(stats));
                break;
            
            case VarDecl(Name name, int mods, Tree vtype, Tree init, _):
                res = redef.VarDecl(name, mods, copy(vtype), copy(init));
                break;
            
            case Block(int mods, Tree[] stats):
                res = redef.Block(mods, copy(stats));
                break;
            
            case Loop(int tag, Tree cond, Tree body):
                res = redef.Loop(tag, copy(cond), copy(body));
                break;
            
            case ForLoop(Tree[] inits, Tree cond, Tree[] steps, Tree body):
                res = redef.ForLoop(copy(inits), copy(cond), copy(steps), copy(body));
                break;
            
            case Labelled(Name label, Tree body):
                res = redef.Labelled(label, copy(body));
                break;
            
            case Switch(Tree selector, Case[] cases):
                res = redef.Switch(copy(selector),
                        (Case[])copy(cases, new Case[cases.length]));
                break;
            
            case Case(Tree[] pat, Tree[] stats):
                res = redef.Case(copy(pat), copy(stats));
                break;
            
            case Synchronized(Tree lock, Tree body):
                res = redef.Synchronized(copy(lock), copy(body));
                break;
            
            case Try(Tree body, Catch[] catchers, Tree finalizer):
                res = redef.Try(copy(body),
                        (Catch[])copy(catchers, new Catch[catchers.length]),
                        finalizer);
                break;
            
            case Catch(VarDecl exc, Tree body):
                res = redef.Catch((VarDecl)copy(exc), copy(body));
                break;
            
            case If(Tree cond, Tree thenp, Tree elsep):
                res = redef.If(copy(cond), copy(thenp), copy(elsep));
                break;
            
            case Exec(Tree expr):
                res = redef.Exec(copy(expr));
                break;
            
            case Break(Name label, Tree target):
                res = redef.Break(label);
                break;
            
            case Continue(Name label, Tree target):
                res = redef.Continue(label);
                break;
            
            case Return(Tree expr, Tree target):
                res = redef.Return(copy(expr));
                break;
            
            case Throw(Tree expr):
                res = redef.Throw(copy(expr));
                break;
            
            case Aggregate(Tree[] elems, Tree arrtype):
                res = redef.Aggregate(elems, arrtype);
                break;
            
            case Apply(Tree fn, Tree[] args):
                res = redef.Apply(copy(fn), copy(args));
                break;
            
            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def, _):
                res = redef.NewObj(encl, clazz, args, def);
                break;
            
            case NewArray(Tree elemtype, Tree[] dims):
                res = redef.NewArray(copy(elemtype), copy(dims));
                break;
            
            case Assign(Tree lhs, Tree rhs):
                res = redef.Assign(copy(lhs), copy(rhs));
                break;
            
            case Assignop(int opc, Tree lhs, Tree rhs, _):
                res = redef.Assignop(opc, copy(lhs), copy(rhs));
                break;
            
            case Binop(int opc, Tree lhs, Tree rhs, _):
                res = redef.Binop(opc, copy(lhs), copy(rhs));
                break;
            
            case Unop(int opc, Tree operand, _):
                res = redef.Unop(opc, copy(operand));
                break;
            
            case Typeop(int opc, Tree expr, Tree clazz):
                res = redef.Typeop(opc, copy(expr), copy(clazz));
                break;
            
            case Index(Tree indexed, Tree index):
                res = redef.Index(copy(indexed), copy(index));
                break;
            
            case Select(Tree selected, Name selector, _):
                res = redef.Select(copy(selected), selector);
                break;
            
            case Ident(Name name, _):
                res = redef.Ident(name);
                break;
            
            case Self(Tree encl, int tag, _):
                res = redef.Self(copy(encl), tag);
                break;
            
            case Literal(Constant value):
                res = redef.Literal(value);
                break;
            
            case BasicType(int tag):
                res = redef.BasicType(tag);
                break;
            
            case ArrayTypeTerm(Tree elemtype):
                res = redef.ArrayTypeTerm(copy(elemtype));
                break;
            
            case Bad():
                res = Tree.Bad();
                res.pos = tree.pos;
                break;
                
            case Assert(Tree cond, Tree message):
            	res = redef.Assert(copy(cond), copy(message));
            	break;
            	
            default:
                throw new InternalError("copy(" + tree.getClass() + ")");
        }
        return pr.make(res);
    }

/** has this tree any side-effects?
 *  this is *not* a conservative approach. any exceptions, like NullPointerExceptions
 *  or ArrayIndexOutOfBoundsExceptions are not regarded as side-effects.
 */
    public boolean hasSideEffects(Tree tree)
    {
        if (tree == null)
            return false;
        switch (tree)
        {
            case If(Tree cond, Tree thenp, Tree elsep):
                return hasSideEffects(cond) || hasSideEffects(thenp) || hasSideEffects(elsep);
            
            case Binop(_, Tree lhs, Tree rhs, _):
                return hasSideEffects(lhs) || hasSideEffects(rhs);
            
            case Unop(_, _, _):
                return true;
            
            case Typeop(int opc, Tree expr, Tree clazz):
                return hasSideEffects(expr) || hasSideEffects(clazz);
            
            case Index(Tree indexed, Tree index):
                return hasSideEffects(indexed) || hasSideEffects(index);
            
            case Select(Tree selected, _, _):
                return hasSideEffects(selected);
            
            case Ident(Name name, _):
                return false;
            
            case Self(_, _, _):
                return false;
            
            case Literal(_):
                return false;
            
            case BasicType(_):
                return false;
            
            case ArrayTypeTerm(Tree elemtype):
                return false;
            
            default:
                return true;
        }
    }
}

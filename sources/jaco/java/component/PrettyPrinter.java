//      /   _ _      JaCo
//  \  //\ / / \     - pretty printer for abstract syntax trees
//   \//  \\_\_/     
//         \         Matthias Zenger, 29/03/98

package jaco.java.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.context.*;
import Tree.*;
import Type.*;
import Constant.*;
import java.io.*;
import java.util.*;


public class PrettyPrinter extends Component
                           implements ModifierConst, OperatorConst, TreeConst,
                                      TreeProcessor
{
/** language components
 */
    protected ErrorHandler  report;
    protected Modifiers     modifiers;
    protected Operators     operators;
    protected Types         types;


/** the print writer
 */
    protected PrintWriter   out;

/** suffix of output file (= null iff out-output)
 */
    protected String        suffix;
    
/** indentaion width
 */
    protected int           width = 3;

/** the current left margin
 */
    protected int           lmargin = 0;

/** current classname
 */
    protected Name          classname = Name.fromString("<top level>");


    public PrettyPrinter()
    {
        out = new PrintWriter(System.out, true);
    }
    
    public PrettyPrinter(PrintWriter out)
    {
        this.out = out;
    }
    
    public PrettyPrinter(String suffix)
    {
        this.suffix = suffix;
    }


/** component name
 */
    public String getName()
    {
        return "JavaPrettyPrinter";
    }
    
/** component initialization
 */
    public void init(MainContext context)
    {
        super.init(context);
        report = context.ErrorHandler();
        modifiers = context.Modifiers();
        operators = context.Operators();
        types = context.Types();
    }

/** flush buffer
 */
    public void flush() {
        out.flush();
    }
    
/** align to lmargin
 */
    protected void align()
    {
        for (int i = 1; i < lmargin; i += 2)
            print("  ");
        if ((lmargin % 2) == 1)
            print(" ");
    }

/** indent left margin
 */
    protected void indent()
    {
        lmargin += width;
    }

/** reverse indentation
 */
    protected void undent()
    {
        lmargin -= width;
    }

/** enter new precedence level
 */
    protected void open(int contextPrec, int ownPrec)
    {
        if (ownPrec < contextPrec)
            print("(");
    }

/** leave precedence level
 */
    protected void close(int contextPrec, int ownPrec)
    {
        if (ownPrec < contextPrec)
            print(")");
    }


/** basic print methods
 */
    public PrettyPrinter print(String str)
    {
        out.print(str);
        return this;
    }
    
    public PrettyPrinter print(Object obj)
    {
        out.print(obj);
        return this;
    }

    public PrettyPrinter println(String str)
    {
        out.println(str);
        return this;
    }

    public PrettyPrinter println()
    {
        out.println();
        return this;
    }


/** output methods
 */
    protected String toString(Tree tree)
    {
        switch (tree)
        {
            case Select(Tree selected, Name selector, _): 
                return toString(selected) + "." + selector;
            
            case Ident(Name idname, _): 
                return idname.toString();
            
            default:
                throw new InternalError();
        }
    }
    
    public void outputProlog(Vector prolog, String filename)
    {
        println("/* generated by JaCo").
        println(" *   content: " + filename).
        println(" *   date   : " + new Date()).
        println(" */").println();
        if (prolog != null)
        {
            for (int i = 1; i < prolog.size(); i++)
                printDecl((Tree)prolog.elementAt(i)).println();
            println();
        }
    }
    
    public PrettyPrinter outputUnit(Tree tree, String suffix, boolean forbidOverwrite)
    {
        switch (tree)
        {
            case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                report.useEnv(info);
                String      dest = exchangeSuffix(info.source.toString(), suffix);
                PrintWriter oldOut = out;
                try
                {
                    File        outfile = new File(dest);
                    if (forbidOverwrite && outfile.exists())
                        report.error(Position.NOPOS, "cannot overwrite; " +
                                     "target file " + dest + " already exists");
                    else
                    {
                        out = new PrintWriter(new BufferedOutputStream(
                                                    new FileOutputStream(outfile)));
                        outputProlog(null, dest);
                        printDecls(decls).println();
                        out.close();
                        report.note("[wrote " + outfile.getPath() + "]");
                    }
                }
                catch (IOException e)
                {
                    report.error(Position.NOPOS, "error while writing " + dest + ": " + e);
                }
                out = oldOut;
                report.usePreviousEnv();
                return this;
            
            case Bad():
                return this;
            
            default:
                throw new InternalError();
        }
    }
    
    public PrettyPrinter outputClasses(Tree tree)
    {
        switch (tree)
        {
            case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                report.useEnv(info);
                File        dir = new File(new File(info.source.toString()).getParent());
                PrintWriter oldOut = out;
                Vector      prolog = new Vector();
                String      pckge = "";
                for (int i = 0; i < decls.length; i++)
                    switch (decls[i])
                    {
                        case Bad():
                            break;
                        
                        case PackageDecl(Tree qualid):
                            pckge = toString(qualid) + ".";
                            prolog.addElement(decls[i]);
                            break;
                        
                        case Import(_, _):
                            prolog.addElement(decls[i]);
                            break;

                        case ClassDecl(Name name, _, _, _, _, _):
                            File    outfile;
                            try
                            {
                                out = new PrintWriter(
                                        new BufferedOutputStream(
                                            new FileOutputStream(outfile =
                                                VirtualFile.create(dir, pckge + name, ".java"))));
                                outputProlog(prolog, name.toString());
                                printDecl(decls[i]).println();
                                out.close();
                                report.note("[wrote " + outfile.getPath() + "]");
                            }
                            catch (IOException e)
                            {
                                report.error(Position.NOPOS, "error while sourcefile " + name + ": " + e);
                            }
                            break;
                        
                        default:
                            throw new InternalError();
                    }
                out = oldOut;
                report.usePreviousEnv();
                return this;
            
            case Bad():
                return this;
            
            default:
                throw new InternalError();
        }
    }
    
    private String exchangeSuffix(String sourcefile, String suffix)
    {
        return sourcefile.substring(0, sourcefile.lastIndexOf('.')) +
                "." + suffix;
    }

/** print methods
 */
    public PrettyPrinter printDecls(Tree[] trees)
    {
        for (int i = 0; i < trees.length; i++)
            printDecl(trees[i]).println();
        return this;
    }

    public PrettyPrinter printDecl(Tree tree)
    {
        switch (tree)
        {
            case Bad():
                return println("<bad def>");
            
            case PackageDecl(Tree qualid):
                return print("package ").printExpr(qualid).println(";");

            case Import(int tag, Tree qualid):
                    print("import ").printExpr(qualid);
                    if (tag == IMPORTSTAR)
                        return println(".*;");
                    else
                        return println(";");

            case ClassDecl(Name name, int mods, Tree extending,
                            Tree[] implementing, Tree[] members, _):
                Name    prev = classname;
                classname = name;
                print(modifiers.toString(mods & ~INTERFACE));
                if ((mods & INTERFACE) != 0)
                {
                    print("interface " + name);
                    if (implementing.length > 0)
                        print(" extends ").printExprs(implementing);
                }
                else
                {
                    print("class " + name);
                    if (extending != null)
                        print(" extends ").printExpr(extending);
                    if (implementing.length > 0)
                        print(" implements ").printExprs(implementing);
                }
                printClassBlock(members);
                classname = prev;
                return this;
                
            case MethodDecl(Name name, int mods, Tree restype, VarDecl[] params,
                            Tree[] thrown, Tree[] stats, _):
                print(modifiers.toString(mods));
                if (name == PredefConst.INIT_N)
                    print(classname);
                else
                if (restype == null)
                    print(name);
                else
                    printExpr(restype).print(" " + name);
                print("(").printExprs(params).print(")");
                if (thrown.length > 0)
                    print(" throws ").printExprs(thrown);
                if (stats != null)
                    return print(" ").printBlock(stats);
                else
                    return print(";");
            
            case VarDecl(_, _, _, _, _):
                return printExpr(tree).print(";");
            
            case Block(int mods, Tree[] stats):
                return print(modifiers.toString(mods)).printBlock(stats);
            
            default:
                throw new InternalError("printDecl(" + tree + ")");
        }
    }

    public PrettyPrinter printBlock(Tree[] stats)
    {
        println("{").indent();
        printStats(stats).undent();
        align();
        return print("}");
    }

    public PrettyPrinter printClassBlock(Tree[] defs)
    {
        println(" {").indent();
        for (int i = 0; i < defs.length; i++)
        {
            align();
            printDecl(defs[i]).println();
        }
        undent();
        align();
        return print("}");
    }
    

    public PrettyPrinter printStats(Tree[] stats)
    {
        for (int i = 0; i < stats.length; i++)
        {
            align();
            printStat(stats[i]).println();
        }
        return this;
    }
    
    boolean printBody(Tree stat)
    {
        switch (stat)
        {
            case Block(_, Tree[] stats):
                printBlock(stats);
                return true;
            
            default:
                println();
                indent();
                align();
                printStat(stat);
                undent();
                return false;
        }
    }

    public PrettyPrinter printStat(Tree tree)
    {
        switch (tree)
        {
            case Bad():
                return println("<bad stat>");
            
            case Block(_, Tree[] stats):
                return printBlock(stats);
            
            case ClassDecl(_, _, _, _, _, _):
                return printDecl(tree);
            
            case VarDecl(_, _, _, _, _):
                return printExpr(tree).print(";");
            
            case Loop(DO, Tree cond, Tree body):
                print("do ").printStat(body).align();
                return print(" while (").printExpr(cond).print(");");
            
            case Loop(WHILE, Tree cond, Tree body):
                print("while (").printExpr(cond).print(") ").printBody(body);
                return this;
            
            case ForLoop(Tree[] init, Tree cond, Tree[] steps, Tree body):
                print("for (");
                if (init.length > 0)
                {
                    switch (init[0])
                    {
                        case VarDecl(Name n, int mods, Tree vartype, Tree ini, _):
                            print(modifiers.toString(mods));
                            printExpr(vartype).print(" " + n);
                            if (ini != null)
                                print(" = ").printExpr(ini);
                            for (int i = 1; i < init.length; i++)
                            {
                                switch (init[i])
                                {
                                    case VarDecl(Name name, _, _, Tree initexpr, _):
                                        print(", " + name + " = ");
                                        printExpr(initexpr);
                                        break;
                                    
                                    default:
                                        throw new InternalError();
                                }
                            }
                            break;
                        
                        default:
                            printExprs(init);
                    }
                }
                print("; ");
                if (cond != null)
                    printExpr(cond);
                print("; ").printExprs(steps).print(") ").printBody(body);
                return this;

            case Labelled(Name label, Tree body):
                return print(label + ": ").printStat(body);

            case Switch(Tree selector, Case[] cases):
                print("switch (").printExpr(selector).print(") {\n");
                indent();
                // align();
                printStats(cases);
                undent();
                align();
                return print("}");

            case Case(Tree[] pats, Tree[] stats):
                for (int i = 0; i < pats.length; i++)
                {
                    if (i > 0)
                        align();
                    if (pats[i] == null)
                        println("default:");
                    else
                        print("case ").printExpr(pats[i]).println(":");
                }
                indent();
                printStats(stats);
                undent();
                align();
                return this;

            case Synchronized(Tree lock, Tree body):
                print("synchronized (").printExpr(lock);
                return print(") ").printStat(body);

            case Try(Tree body, Catch[] catchers, Tree finalizer):
                print("try ").printStat(body).print(" ");
                for (int i = 0; i < catchers.length; i++)
                    printStat(catchers[i]);
                if (finalizer != null)
                    print(" finally ").printStat(finalizer);
                return this;

            case Catch(VarDecl p, Tree body):
                return print(" catch (").printExpr(p).print(") ").printStat(body);

            case If(Tree cond, Tree thenpart, Tree elsepart):
                if (print("if (").printExpr(cond).print(") ").printBody(thenpart))
                {
                    if (elsepart != null)
                        print(" else ").printBody(elsepart);
                }
                else
                if (elsepart != null)
                {
                    println().align();
                    print("else ").printBody(elsepart);
                }
                return this;

            case Exec(Tree expr):
                return printExpr(expr).print(";");

            case Break(Name label, Tree target):
                print("break");
                if (label != null)
                    print(" " + label);
                print(";");
                if (tree.pos == Position.NOPOS)
                    print("/* inserted for safety */");
                return this;

            case Continue(Name label, Tree target):
                print("continue");
                if (label != null)
                    print(" " + label);
                return print(";");

            case Return(Tree expr, Tree target):
                print("return");
                if (expr != null)
                    print(" ").printExpr(expr);
                return print(";");

            case Throw(Tree expr):
                print("throw ").printExpr(expr).print(";");
                if (tree.pos == Position.NOPOS)
                    out.print("/* inserted for safety */");
                return this;
            
            case Assert(Tree cond, Tree message):
                if (message == null)
                    print("assert ").printExpr(cond).print(";");
                else
                    print("assert(").printExpr(cond).print(", ").
                        printExpr(message).print(");");
                return this;
                
            default:
                throw new InternalError("printStat(" + tree + ")");
        }
    }
    
    protected Name className(Tree tree)
    {
        switch (tree)
        {
            case Select(_, Name selector, _):
                return selector;
                
            case Ident(Name idname, _):
                return idname;
            
            default:
                return classname;
        }
    }
    
    public PrettyPrinter printExpr(Tree tree, int prec)
    {
        switch (tree)
        {
            case Bad():
                return print("<bad expr>");
            
            case VarDecl(Name name, int mods, Tree vartype, Tree init, _):
                print(modifiers.toString(mods));
                printExpr(vartype).print(" " + name);
                if (init != null)
                    print(" = ").printExpr(init);
                    return this;
            
            case Exec(Tree expr):
                return printExpr(expr);
            
            case If(Tree cond, Tree thenpart, Tree elsepart):
                open(prec, operators.condPrec);
                printExpr(cond, operators.condPrec).print(" ? ");
                printExpr(thenpart, operators.condPrec).print(" : ");
                printExpr(elsepart, operators.condPrec);
                close(prec, operators.condPrec);
                return this;

            case Aggregate(Tree[] elems, Tree arrtype):
                if (arrtype != null)
                    print("new ").printExpr(arrtype, operators.postfixPrec);
                return print("{").printExprs(elems).print("}");

            case Apply(Tree fn, Tree[] args):
                return printExpr(fn, operators.postfixPrec).print("(").
                        printExprs(args).print(")");

            case NewObj(Tree encl, Tree clazz, Tree[] args, Tree def, _):
                if (encl != null)
                    printExpr(encl).print(".");
                print("new ").printExpr(clazz).print("(").printExprs(args).print(")");
                if (def != null)
                {
                    Name    prev = classname;
                    classname = className(clazz);
                    printClassBlock(((ClassDecl)def).members);
                    classname = prev;
                }
                return this;
            
            case NewArray(Tree elemtype, Tree[] dims):
                print("new ");
                int n = 0;
                Tree lastelem = null;
                Tree etype = elemtype;
                while (etype != lastelem)
                {
                    lastelem = etype;
                    switch (etype)
                    {
                        case ArrayTypeTerm(Tree etype1):
                            n++;
                            etype = etype1;
                        break;
                    }
                }
                printExpr(etype);
                for (int i = 0; i < dims.length; i++)
                    print("[").printExpr(dims[i]).print("]");
                for (int i = 0; i < n; i++)
                    print("[]");
                return this;

            case Assign(Tree lhs, Tree rhs):
                open(prec, operators.prec(ASSIGN) - 1);
                printExpr(lhs, operators.assignPrec).print(" = ");
                printExpr(rhs, operators.assignPrec);
                close(prec, operators.prec(ASSIGN) - 1);
                return this;

            case Assignop(int opcode, Tree lhs, Tree rhs, _):
                open(prec, operators.prec(ASSIGN) - 1);
                printExpr(lhs, operators.assignPrec).print(" " + operators.toString(opcode) + "= ");
                printExpr(rhs, operators.assignPrec);
                close(prec, operators.prec(ASSIGN) - 1);
                return this;

            case Binop(int opcode, Tree lhs, Tree rhs, _):
                open(prec, operators.prec(opcode));
                printExpr(lhs, operators.prec(opcode)).print(" " + operators.toString(opcode) + " ");
                printExpr(rhs, operators.prec(opcode) + 1);
                close(prec, operators.prec(opcode));
                return this;

            case Unop(int opcode, Tree operand, _):
                int opprec = operators.prec(opcode);
                open(prec, opprec);
                if (opcode < POSTINC)
                    print(operators.toString(opcode) + " ");
                printExpr(operand, opprec);
                if (opcode >= POSTINC)
                    print(" " + operators.toString(opcode));
                close(prec, opprec);
                return this;

            case Typeop(int opcode, Tree expr, Tree clazz):
                open (prec, operators.prec(opcode));
                if (opcode == TYPETEST)
                    printExpr(expr, operators.prefixPrec).print(" instanceof ").
                    printExpr(clazz);
                else
                    print("(").printExpr(clazz, operators.postfixPrec).print(")").
                    printExpr(expr, operators.postfixPrec);
                close(prec, operators.prec(opcode));
                return this;

            case Index(Tree indexed, Tree index):
                return printExpr(indexed, operators.postfixPrec).print("[").
                        printExpr(index).print("]");

            case Select(Tree selected, Name selector, _):
                return printExpr(selected, operators.postfixPrec).print("." + selector);

            case Ident(Name idname, _):
                return print(idname);

            case Self(Tree clazz, int tag, _):
                if (clazz != null)
                    printExpr(clazz, operators.postfixPrec).print(".");
                if (tag == SUPER)
                    return print("super");
                else
                    return print("this");

            case Literal(Constant value):
                return print(value);

            case BasicType(int tag):
                return print(types.toString(tag));

            case ArrayTypeTerm(Tree elemtype):
                return printExpr(elemtype).print("[]");
            
            default:
                throw new InternalError("printExpr(" + tree + ")");
        }
    }
    
    public PrettyPrinter printExpr(Tree tree)
    {
        return printExpr(tree, operators.noPrec);
    }

    public PrettyPrinter printExprs(Tree[] trees)
    {
        if (trees != null && trees.length >= 1)
        {
            printExpr(trees[0]);
            for (int i = 1; i < trees.length; i++)
                print(", ").printExpr(trees[i]);
        }
        return this;
    }
    
/** the tree processor methods
 */
    public TreeList enter(TreeList treelist) throws AbortCompilation
    {
        return treelist;
    }
    
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        return treelist;
    }
    
    public Tree process(Tree tree) throws AbortCompilation
    {
        if (tree == null)
            println("<null tree>");
        else
            switch (tree)
            {
                case CompilationUnit(Tree[] decls, _, _, CompilationEnv info):
                    println("////// " + info.source + " //////");
                    printDecls(decls);
                    break;
                    
                case Bad():
                    println("<bad tree>");
                    break;
                    
                default:
                    throw new InternalError();
            }
        return tree;
    }
}

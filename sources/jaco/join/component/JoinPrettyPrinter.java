package jaco.join.component;

import jaco.framework.*;
import jaco.java.struct.*;
import jaco.java.component.*;
import jaco.join.struct.*;
import jaco.join.context.*;
import JTree.*;
import Type.*;
import Constant.*;
import java.io.*;
import java.util.*;


public class JoinPrettyPrinter extends Component implements JoinConst
{
/** language components
 */
    protected ErrorHandler  report;


/** the print writer
 */
    protected PrintWriter   out;

/** suffix of output file (= null iff out-output)
 */
    protected String        suffix;
    
/** indentation width
 */
    protected int           width = 3;

/** the current left margin
 */
    protected int           lmargin = 0;

/** the current precedence level
 */
    protected int           prec = 0;

/** current classname
 */
    protected Name          classname = Name.fromString("<top level>");


    public JoinPrettyPrinter()
    {
        out = new PrintWriter(System.out, true);
    }
    
    public JoinPrettyPrinter(PrintWriter out)
    {
        this.out = out;
    }
    
    public JoinPrettyPrinter(String suffix)
    {
        this.suffix = suffix;
    }


/** component name
 */
    public String getName()
    {
        return "JoinPrettyPrinter";
    }
    
/** component initialization
 */
    public void init(JoinSyntacticContext context)
    {
        super.init(context);
        report = context.compilerContext.mainContext.ErrorHandler();
    }

/** basic print methods
 */
    public JoinPrettyPrinter print(String str)
    {
        out.print(str);
        return this;
    }
    
    public JoinPrettyPrinter print(Object obj)
    {
        out.print(obj.toString());
        return this;
    }

    public JoinPrettyPrinter println(String str)
    {
        out.println(str);
        
        return this;
    }

    public JoinPrettyPrinter println()
    {
        out.println();
        for (int i = 1; i < lmargin; i++)
            out.print(' ');
        return this;
    }
    
    public JoinPrettyPrinter newleft()
    {
        lmargin += width;
        return this;
    }
    
    public JoinPrettyPrinter oldleft()
    {
        lmargin -= width;
        return this;
    }
    
    public JoinPrettyPrinter indent()
    {
        lmargin += width;
        out.println();
        for (int i = 1; i < lmargin; i++)
            out.print(' ');
        return this;
    }
    
    public JoinPrettyPrinter undent()
    {
        lmargin -= width;
        out.println();
        for (int i = 1; i < lmargin; i++)
            out.print(' ');
        return this;
    }
    
    private String exchangeSuffix(String sourcefile, String suffix)
    {
        return sourcefile.substring(0, sourcefile.lastIndexOf('.')) +
                "." + suffix;
    }

    public int precedence(JTree tree) {
        switch(tree) {
            case Bad():
                return 0;
            case TopLevel(_, _):
                return 0;
            case Literal(_):
                return 12;
            case Ident(_):
                return 12;
            case Select(_, _):
                return 12;
            case Apply(_, _):
                return 10;
            case Oper(int opcode, _, _):
                switch (opcode) {
                    case PLUS:
                        return 7;
                    case MINUS:
                        return 7;
                    case NEG:
                        return 11;
                    case TIMES:
                        return 8;
                    case DIV:
                        return 8;
                    case AND:
                        return 4;
                    case OR:
                        return 3;
                    case EQ:
                        return 5;
                    case NOTEQ:
                        return 5;
                    case GT:
                        return 6;
                    case LT:
                        return 6;
                    case GTEQ:
                        return 6;
                    case LTEQ:
                        return 6;
                    case NOT:
                        return 11;
                    default:
                        return 1;
                }
            case If(_, _, _):
                return 2;
            case Let(_, _):
                return 1;
            case Join(_, _):
                return 1;
            case Tupel(_):
                return 12;
            case Decl(_, _, _):
            case Decls(_, _):
            case Rec(_, _):
                return 1;
            default:
                return 13;
        }
    }
    
/** print methods
 */
    public void print(JTree tree) {
        printExpr(tree);
        out.flush();
    }
    
    public JoinPrettyPrinter printExprs(JTree[] trees) {
        for (int i = 0; i < trees.length; i++)
            printExpr(trees[i]).println();
        return this;
    }
    
    public JoinPrettyPrinter printExpr(JTree tree) {
        if (tree == null) {
            System.out.println("<null>");
            return this;
        }
        int oldprec = prec;
        prec = precedence(tree);
        if (prec < oldprec)
            print("(");
        switch(tree) {
            case Bad():
                break;
                
            case TopLevel(JTree[] trees, Tree.CompilationUnit cu):
                report.useEnv(cu.info);
                lmargin = 0;
                prec = 0;
                if (suffix != null)
                {
                    String dest = exchangeSuffix(cu.info.source.toString(), suffix);
                    PrintWriter oldOut = out;
                    try
                    {
                        File outfile = new File(dest);
                        if (outfile.exists())
                            report.error(Position.NOPOS, "cannot overwrite; " +
                                     "target file " + dest + " already exists");
                        else
                        {
                            out = new PrintWriter(new BufferedOutputStream(
                                                    new FileOutputStream(outfile)));
                            printExprs(trees).println();
                            out.close();
                            report.note("[wrote " + outfile.getPath() + "]");
                        }
                    }
                    catch (IOException e)
                    {
                        report.error(Position.NOPOS, "error while writing " + dest + ": " + e);
                    }
                    out = oldOut;
                }
                else
                    printExprs(trees).println();
                report.usePreviousEnv();
                break;
                
            case Literal(Constant lit):
                print(lit);
                break;
                        
            case SpecialLiteral(int tag):
                switch (tag) {
                    case ERROR:
                        print("error");
                        break;
                    case FALSE:
                        print("false");
                        break;
                    case TRUE:
                        print("true");
                        break;
                    default:
                        print("<speciallit " + tag + ">");
                }
                break;
            
            case Ident(Name name):
                print(name);
                break;
    
            case Select(JTree selected, Name selector):
                printExpr(selected).print(".").print(selector);
                break;
                
            case Apply(JTree fun, JTree arg):
                printExpr(fun).print(" ").printExpr(arg);
                break;
            
            case Oper(int opcode, JTree left, JTree right):
                printExpr(left);
                switch (opcode) {
                    case PLUS:
                        print(" + ");
                        break;
                    case MINUS:
                        print(" - ");
                        break;
                    case NEG:
                        print("-");
                        break;
                    case TIMES:
                        print(" * ");
                        break;
                    case DIV:
                        print(" / ");
                        break;
                    case AND:
                        print(" && ");
                        break;
                    case OR:
                        print(" || ");
                        break;
                    case EQ:
                        print(" == ");
                        break;
                    case NOTEQ:
                        print(" != ");
                        break;
                    case GT:
                        print(" > ");
                        break;
                    case LT:
                        print(" < ");
                        break;
                    case GTEQ:
                        print(" >= ");
                        break;
                    case LTEQ:
                        print(" <= ");
                        break;
                    case NOT:
                        print("!");
                        break;
                    default:
                        print(" <?> ");
                }
                printExpr(right);
                break;
                
            case If(JTree cond, JTree ifterm, JTree elseterm):
                println().print("if (").printExpr(cond).print(")").indent();
                int p = prec;
                prec = 1;
                printExpr(ifterm).undent();
                prec = p;
                print("else").indent();
                printExpr(elseterm).oldleft();
                break;
            
            case Let(JTree head, JTree body):
                printExpr(head).print(" ;").indent();
                int p = prec;
                prec = 1;
                printExpr(body).oldleft();
                prec = p;
                break;
                
            case Join(JTree left, JTree right):
                printExpr(left).print(" & ").printExpr(right);
                break;
            
            case Tupel(JTree[] trees):
                if (trees.length == 0)
                    print("()");
                else {
                    int p = prec;
                    prec = 1;
                    newleft().print("(").printExpr(trees[0]);
                    for (int i = 1; i < trees.length; i++)
                        print(", ").printExpr(trees[i]);
                    print(")").oldleft();
                    prec = p;
                }
                break;
            
            case Decl(JTree lhs, JTree rhs, _):
                printExpr(lhs).print(" = ").newleft().printExpr(rhs).oldleft();
                break;
            
            case Decls(JTree[] decls, _):
                if (decls.length == 0)
                    print("{}");
                else if (decls.length == 1)
                    print("{ ").newleft().printExpr(decls[0]).print(" }").oldleft();
                else {
                    print("{ ").indent().printExpr(decls[0]);
                    for (int i = 1; i < decls.length; i++)
                        print(",").println().printExpr(decls[i]);
                    print(" }").oldleft();
                }
                break;
            
            case Rec(JTree[] decls, _):
                print("rec { ").indent().printExpr(decls[0]);
                for (int i = 1; i < decls.length; i++)
                    print(",").println().printExpr(decls[i]);
                print(" }").oldleft();
                break;
        }
        if (prec < oldprec)
            print(")");
        prec = oldprec;
        return this;
    }
}

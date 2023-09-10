package jaco.join.component;

import jaco.join.grammar.*;
import jaco.join.struct.*;
import jaco.join.context.*;
import jaco.java.struct.*;
import jaco.framework.*;
import jaco.framework.parser.*;
import java.util.Hashtable;


public class JoinSyntacticAnalyzer extends jaco.java.component.SyntacticAnalyzer
{
/** the current context
 */
    protected JoinSyntacticContext context;

/** a hashtable mapping CompilationUnits to structure trees of join
 *  programs
 */
    protected Hashtable joinTrees = new Hashtable();
    

/** component name
 */
    public String getName()
    {
        return "JoinSyntacticAnalyzer";
    }

/** component initialization
 */
    public void init(JoinSyntacticContext context)
    {
        super.init(context);
        this.context = context;
    }

/** compile join programs after parsing
 */
    public TreeList exit(TreeList treelist) throws AbortCompilation
    {
        TreeList.Cons   trees = treelist.chain();
        while (trees != null)
        {
            JTree jtree = (JTree)joinTrees.get(trees.head);
            if (jtree != null)
            {
                // build a linked jtree list
                context.JPrettyPrinter().printExpr(jtree);
                String s = ((JTree.TopLevel)jtree).unit.info.source.shortname;
                if (s.endsWith(".join"))
                    s = s.substring(0, s.length() - 5);
                new JoinTypeChecker(context.JPrettyPrinter()).typeCheck(jtree);
                Program prog = new JoinCodeGenerator().generateCode(jtree);
                prog.dump();
                ((JTree.TopLevel)jtree).unit.decls = prog.translate(
                        s,
                        context.compilerContext.mainContext.Trees(),
                        context.compilerContext.mainContext.Constants());
                context.compilerContext.mainContext.PrettyPrinter().process(((JTree.TopLevel)jtree).unit);
            }
            trees = trees.next();
        }
        //throw new AbortCompilation(AbortCompilation.TOPLEVEL);
        // apply context.JoinCompiler to jtree list
        return super.exit(treelist);
    }

/** the tree processor
 */
    protected Tree process(Tree.CompilationUnit tree) throws AbortCompilation
    {
        if (tree.decls != null)
            return tree;
        else if (tree.info.source.toString().endsWith(".java"))
            return super.process(tree);
        else
        {
            long        msec    = System.currentTimeMillis();
            Scanner     scanner = context.JoinScanner(tree.info.source);
            LRParser    parser  = context.JoinParser(scanner);
            try
            {
                Symbol  result;
                if (context.debug.debugSwitchSet(getDebugName(), 4))
                    result = parser.debug_parse();
                else
                    result = parser.parse();
                if (result.value instanceof JTree.TopLevel) {
                    ((JTree.TopLevel)result.value).unit = tree;
                    joinTrees.put(tree, result.value);
                    return tree;
                } else
                    return trees.errorTree;
            }
            catch (Exception e)
            {
                e.printStackTrace();//DEBUG
                report.error(Position.NOPOS, e.toString());
                return trees.errorTree;
            }
            finally
            {
                report.operation("parsed " + tree.info.source, msec);
            }
        }
    }
}

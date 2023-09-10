//      /   _ _      JaCo
//  \  //\ / / \     - the main compiler component
//   \//  \\_\_/     
//         \         Matthias Zenger, 25/01/99

package jaco.java.component;

import jaco.framework.*;
import jaco.java.context.*;
import jaco.java.struct.*;
import java.io.IOException;


public class JavaCompiler extends Component implements Runnable
{
/** the switches
 */
    protected JavaSettings  settings;
    
/** other components
 */
    protected ErrorHandler  report;
    protected Trees         trees;
    protected ClassReader   reader;
    
/** the syntax trees to compile
 */
    protected TreeList      treelist;
    
    
/** component name (here, this is the name of the compiler)
 */
    public String getName()
    {
        return "jaco";
    }

/** version of the compiler
 */
    public String getVersion()
    {
        return "1.3, 29-Aug-2003";
    }
    
/** author of the compiler
 */
    public String getAuthor()
    {
        return "(c) Matthias Zenger [software@zenger.org]";
    }
    
/** component initialization
 */
    public void init(MainContext context)
    {
        // call initializer of super class
        super.init(context);
        
        // get settings
        settings = (JavaSettings)context.settings;
        
        // import other components
        report = context.ErrorHandler();
        reader = context.ClassReader();
        trees = context.Trees();
        
        // initialize this component
        treelist = makeTreeList(context);
        reportSkipped();
    }
    
    protected CompilationEnv makeTopLevelEnv(MainContext context, String filename)
    {
        try
        {
            Sourcefile  source = new Sourcefile(filename);
            if (settings.encoding != null)
                source.setEncoding(settings.encoding);
            return context.CompilationEnv(source);
        }
        catch (IOException e)
        {
            report.print("error: " + e.getMessage());
            return null;
        }
    }
    
    protected TreeList makeTreeList(MainContext context)
    {
        TreeList    ts = new TreeList();
        for (int i = 0; i < settings.sources.size(); i++)
        {
            CompilationEnv  info = makeTopLevelEnv(context,
                                    (String)settings.sources.elementAt(i));
            if (info != null)
                ts.append(trees.make.CompilationUnit(null, info));
        }
        return ts;
    }
    
    protected void reportSkipped()
    {
        if (settings.skipped.size() > 0)
        {
            String  skipList = "skipping '" +
                                    settings.skipped.elementAt(0) + "'";
            for (int i = 1; i < settings.skipped.size(); i++)
                skipList += ", '" + settings.skipped.elementAt(i) + "'";
            report.print("error: " + skipList);
        }
    }
    
    public boolean compile()
    {
        long    msec = System.currentTimeMillis();
        try
        {
            if (settings.version)
            {
                report.print(getName() + " " + getVersion());
                report.print(getAuthor());
            }
            process(treelist);
            return (report.nerrors == 0);
        }
        catch (AbortCompilation e)
        {
            e.printStackTrace();//DEBUG
            return false;
        }
        finally
        {
        	if (settings.stats)
        		report.printStats();
        	report.printSummary();
            if (settings.verbose)
                report.print("[total " +
                    (System.currentTimeMillis() - msec) + "ms]");
        }
    }
    
    public TreeList process(TreeList treelist) throws AbortCompilation
    {
        treelist = treelist.process(((MainContext)context).Compiler());
        while (true)
        {
            TreeList    tl = reader.fetchNewTrees();
            if (tl.length() == 0)
                break;
            else
                treelist.append(tl.process(((MainContext)context).Compiler()));
        }
        return treelist;
    }
    
    public void run()
    {
        compile();
    }
}

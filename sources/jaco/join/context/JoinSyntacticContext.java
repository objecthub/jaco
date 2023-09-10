package jaco.join.context;

import jaco.join.grammar.*;
import jaco.join.component.*;
import jaco.framework.*;
import jaco.framework.parser.*;


public class JoinSyntacticContext extends jaco.java.context.SyntacticContext
{
/** components
 */
    protected JoinPrettyPrinter jpretty;
    
    
    public JoinSyntacticContext(JoinJavaCompilerContext context)
    {
        super(context);
    }
    
    public LRParser JoinParser(Scanner scanner)
    {
        return new jaco.join.grammar.Parser(scanner);
    }
    
    public Scanner JoinScanner(Sourcefile source)
    {
        return new Scanner(this, source);
    }
    
    public JoinPrettyPrinter JPrettyPrinter() {
        if (jpretty == null) {
            jpretty = new JoinPrettyPrinter();
            jpretty.init(this);
        }
        return jpretty;
    }
    
    public JoinCompiler JoinCompiler() {
        JoinCompiler comp = new JoinCompiler();
        comp.init(JoinCompilerContext());
        return comp;
    }
    
    protected JoinCompilerContext JoinCompilerContext() {
        return new JoinCompilerContext(this);
    }
}

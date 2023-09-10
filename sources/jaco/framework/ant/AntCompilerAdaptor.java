//      /   _ _      JaCo
//  \  //\ / / \     - a compiler adaptor for using jaco extensions with ant
//   \//  \\_\_/     
//         \         Matthias Zenger, 01/03/2002

package jaco.framework.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import java.io.IOException;


public abstract class AntCompilerAdaptor extends DefaultCompilerAdapter {
    
    private String source;
    private String target;

    
    public abstract boolean runCompiler(String[] args);
    
    public String compilerName() {
        return "JaCo extension";
    }
    
    public void setJavac(Javac attributes) {
        super.setJavac(attributes);
        AntCompilerTask myattribs = (AntCompilerTask)attributes;
        source = myattribs.getSource();
        target = myattribs.getTarget();
    }

    public boolean execute() throws BuildException {
        attributes.log("Using " + compilerName() + " as Java compiler",
                       Project.MSG_VERBOSE);
        return runCompiler(setupJacoCommand().getArguments());
    }
    
    public Commandline setupJacoCommand() {
        Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches(cmd);
        setupJacoCommandlineSwitches(cmd);
        logAndAddFilesToCompile(cmd);
        return cmd;
    }
    
    public void setupJacoCommandlineSwitches(Commandline cmd) {
        if (source != null) {
            cmd.createArgument().setValue("-source");
            cmd.createArgument().setValue(source);
        }
        if (target != null) {
            cmd.createArgument().setValue("-target");
            cmd.createArgument().setValue(target);
        }
    }
}

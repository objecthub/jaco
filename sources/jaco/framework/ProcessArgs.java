//      /   _ _     JaCo
//  \  //\ / / \    - command line parsing
//   \//  \\_\_/
//         \


package jaco.framework;

import java.io.*;
import java.util.*;


public final class ProcessArgs {
	
    public static String[] expand(String[] args) {
        if (args == null)
        	return new String[0];
        Vector newArgs = new Vector();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.length() > 1 && arg.charAt(0) == '@') {
                arg = arg.substring(1);
                if (arg.charAt(0) == '@')
                    newArgs.add(arg);
                else
                	try {
                    	process(arg, newArgs);
                    } catch (IOException e) {
                    	newArgs.add(arg);
                    }
            } else
                newArgs.add(arg);
        }
        return (String[])newArgs.toArray(new String[newArgs.size()]);
    }
	
    private static void process(String name, Vector args) throws IOException {
        Reader r = new BufferedReader(new FileReader(name));
        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.wordChars(' ', 255);
        st.whitespaceChars(0, ' ');
        st.commentChar('#');
        st.quoteChar('"');
        st.quoteChar('\'');
        while (st.nextToken() != st.TT_EOF)
            args.add(st.sval);
        r.close();
    }
    
}

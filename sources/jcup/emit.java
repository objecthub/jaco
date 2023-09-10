package jcup;

import java.io.*;
import java.util.Stack;
import java.util.Enumeration;
import java.util.Date;

/** 
 * This class handles emitting generated code for the resulting parser.
 * The various parse tables must be constructed, etc. before calling any 
 * routines in this class.<p>  
 *
 * Three classes are produced by this code:
 *   <dl>
 *   <dt> symbol constant class
 *   <dd>   this contains constant declarations for each terminal (and 
 *          optionally each non-terminal).
 *   <dt> action class
 *   <dd>   this non-public class contains code to invoke all the user actions 
 *          that were embedded in the parser specification.
 *   <dt> parser class
 *   <dd>   the specialized parser class consisting primarily of some user 
 *          supplied general and initialization code, and the parse tables.
 *   </dl><p>
 *
 *  Three parse tables are created as part of the parser class:
 *    <dl>
 *    <dt> production table
 *    <dd>   lists the LHS non terminal number, and the length of the RHS of 
 *           each production.
 *    <dt> action table
 *    <dd>   for each state of the parse machine, gives the action to be taken
 *           (shift, reduce, or error) under each lookahead symbol.<br>
 *    <dt> reduce-goto table
 *    <dd>   when a reduce on a given production is taken, the parse stack is 
 *           popped back a number of elements corresponding to the RHS of the 
 *           production.  This reveals a prior state, which we transition out 
 *           of under the LHS non terminal symbol for the production (as if we
 *           had seen the LHS symbol rather than all the symbols matching the 
 *           RHS).  This table is indexed by non terminal numbers and indicates 
 *           how to make these transitions. 
 *    </dl><p>
 * 
 * In addition to the method interface, this class maintains a series of 
 * public global variables and flags indicating how misc. parts of the code 
 * and other output is to be produced, and counting things such as number of 
 * conflicts detected (see the source code and public variables below for
 * more details).<p> 
 *
 * This class is "static" (contains only static data and methods).<p> 
 *
 * @see java_cup.main
 * @version last update: 11/25/95
 * @author Scott Hudson
 */

/* Major externally callable routines here include:
     symbols               - emit the symbol constant class 
     parser                - emit the parser class

   In addition the following major internal routines are provided:
     emit_package          - emit a package declaration
     emit_action_code      - emit the class containing the user's actions 
     emit_production_table - emit declaration and init for the production table
     do_action_table       - emit declaration and init for the action table
     do_reduce_table       - emit declaration and init for the reduce-goto table

   Finally, this class uses a number of public instance variables to communicate
   optional parameters and flags used to control how code is generated,
   as well as to report counts of various things (such as number of conflicts
   detected).  These include:

   prefix                  - a prefix string used to prefix names that would 
                 otherwise "pollute" someone else's name space.
   package_name            - name of the package emitted code is placed in 
                 (or null for an unnamed package.
   symbol_const_class_name - name of the class containing symbol constants.
   parser_class_name       - name of the class for the resulting parser.
   action_code             - user supplied declarations and other code to be 
                 placed in action class.
   parser_code             - user supplied declarations and other code to be 
                 placed in parser class.
   init_code               - user supplied code to be executed as the parser 
                 is being initialized.
   scan_code               - user supplied code to get the next Symbol.
   start_production        - the start production for the grammar.
   import_list             - list of imports for use with action class.
   num_conflicts           - number of conflicts detected. 
   nowarn                  - true if we are not to issue warning messages.
   not_reduced             - count of number of productions that never reduce.
   unused_term             - count of unused terminal symbols.
   unused_non_term         - count of unused non terminal symbols.
   *_time                  - a series of symbols indicating how long various
                 sub-parts of code generation took (used to produce
                 optional time reports in main).
*/

public class emit {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Only constructor is private so no instances can be created. */
  private emit() { }

  /*-----------------------------------------------------------*/
  /*--- Static (Class) Variables ------------------------------*/
  /*-----------------------------------------------------------*/

  /** The prefix placed on names that pollute someone else's name space. */
  public static String prefix = "jcup$";

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Package that the resulting code goes into (null is used for unnamed). */
  public static String package_name = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Name of the generated class for symbol constants. */
  public static String symbol_const_class_name = "Symbols";
  
  /** Name of the generated parser class. */
  public static String parser_class_name = "Parser";
  
  /** the output path */
  public static String out_path = "";

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** User declarations for direct inclusion in user action class. */
  public static String action_code = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** User declarations for direct inclusion in parser class. */
  public static String parser_code = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** User code for user_init() which is called during parser initialization. */
  public static String init_code = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** User code for scan() which is called to get the next Symbol. */
  public static String scan_code = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The start production of the grammar. */
  public static production start_production = null;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** List of imports (Strings containing class names) to go with actions. */
  public static Stack import_list = new Stack();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Number of conflict found while building tables. */
  public static int num_conflicts = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Do we skip warnings? */
  public static boolean nowarn = false;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Count of the number on non-reduced productions found. */
  public static int not_reduced = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Count of unused terminals. */
  public static int unused_term = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Count of unused non terminals. */
  public static int unused_non_term = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /* Timing values used to produce timing report in main.*/

  /** Time to produce symbol constant class. */
  public static long symbols_time          = 0;

  /** Time to produce parser class. */
  public static long parser_time           = 0;

  /** Time to produce action code class. */
  public static long action_code_time      = 0;

  /** Time to produce the production table. */
  public static long production_table_time = 0;

  /** Time to produce the action table. */
  public static long action_table_time     = 0;

  /** Time to produce the reduce-goto table. */
  public static long goto_table_time       = 0;

  /* frankf 6/18/96 */
  protected static boolean _lr_values;

  /** whether or not to emit code for left and right values */
  public static boolean lr_values() {return _lr_values;}
  protected static void set_lr_values(boolean b) { _lr_values = b;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Build a string with the standard prefix. 
   * @param str string to prefix.
   */
  protected static String specialPre(String str)
  {
    return parser_class_name + str;
  }

  protected static String pre(String str)
  {
    return prefix + str;
  }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit a package spec if the user wants one. 
   * @param out stream to produce output on.
   */
  protected static void emit_package(PrintStream out)
    {
      /* generate a package spec if we have a name for one */
      if (package_name != null) 
    out.println("package " + package_name + ";\n");

    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit code for the symbol constant class, optionally including non terms,
   *  if they have been requested.  
   * @param out            stream to produce output on.
   * @param emit_non_terms do we emit constants for non terminals?
   */
  public static void symbols(PrintStream out, boolean emit_non_terms)
    {
      terminal term;
      non_terminal nt;

      long start_time = System.currentTimeMillis();

      /* top of file */
      out.println("//----------------------------------------------------"); 
      out.println("// code was generated by " + version.title_str);
      out.println("// " + new Date());
      out.println("//----------------------------------------------------"); 
      out.println();
      emit_package(out);

      /* class header */
      out.println(
        "/** jcup generated interface containing symbol constants */");
// MZ: classes containing only constants are typically interfaces in Java
      out.println("public interface " + symbol_const_class_name + " {");

      out.println("  /* terminals */");

      /* walk over the terminals */              /* later might sort these */
      for (Enumeration e = terminal.all(); e.hasMoreElements(); )
    {
      term = (terminal)e.nextElement();

      /* output a constant decl for the terminal */
      out.println("  public final int " + term.name() + " = " + 
              term.index() + ";");
    }

      /* do the non terminals if they want them (parser doesn't need them) */
      if (emit_non_terms)
    {
          out.println("\n  /* non terminals */");

          /* walk over the non terminals */       /* later might sort these */
          for (Enumeration e = non_terminal.all(); e.hasMoreElements(); )
        {
          nt = (non_terminal)e.nextElement();
    
          /* output a constant decl for the terminal */
          out.println("  public final int " + nt.name() + " = " + 
                  nt.index() + ";");
        }
    }

      /* end of class */
      out.println("};\n");

      symbols_time = System.currentTimeMillis() - start_time;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit code for the non-public class holding the actual action code. 
   * @param out        stream to produce output on.
   * @param start_prod the start production of the grammar.
   */
  protected static void emit_action_code(PrintStream out, production start_prod)
    throws internal_error
    {
      production prod;

      long start_time = System.currentTimeMillis();

      /* class header */
      out.println();
      out.println(
       "/** jcup generated class to encapsulate user supplied action code.*/"
      );  
      out.println("class " +  specialPre("Actions") + " implements jaco.framework.parser.ActionInterface {");
      out.println("  protected jaco.framework.parser.LRParser   " + pre("parser") + ";");
      out.println("  protected jaco.framework.parser.ParseStack " + pre("stack") + ";");
      out.println("  private final " + parser_class_name + " parser;");
      
      /* user supplied code */
      if (action_code != null)
    {
      out.println();
          out.println(action_code);
    }

      /* constructor */
      out.println();
      out.println("  /** constructor */");
      out.println("  " + specialPre("Actions") + "(");
      out.println("    " + parser_class_name + " parser,");
      out.println("    jaco.framework.parser.ParseStack stack)");
      out.println("  {");
      out.println("      this.parser = parser;");
      out.println("      " + pre("parser") + " = parser;");
      out.println("      " + pre("stack") + " = stack;");
      out.println("  }");
      
      /* action method head */
      out.println();
      out.println("  /** method with the actual generated action code. */");
      out.println("  public final jaco.framework.parser.Symbol doAction(");
      out.println("    int " + pre("act_num,"));
      out.println("    int " + pre("top)"));
      out.println("    throws java.lang.Exception");
      out.println("    {");
    
      /* declaration of result symbol */
      /* New declaration!! now return Symbol
     6/13/96 frankf */
      out.println("      /* the parse stack */");
      out.println("      jaco.framework.parser.Symbol[] $stack = " + pre("stack") + ".stack;");
      out.println();

      /* switch top */
      out.println("      /* select the action based on the action number */");
      out.println("      switch (" + pre("act_num") + ")");
      out.println("        {");

      /* emit action code for each production as a separate case */
      for (Enumeration p = production.all(); p.hasMoreElements(); )
    {
      prod = (production)p.nextElement();

      /* case label */
          out.println("          /*. . . . . . . . . . . . . . . . . . . .*/");
          out.println("          case " + prod.index() + ": // " + prod.to_simple_string());

      /* give them their own block to work in */
      out.println("            {");

      /* create the result symbol */
      /*make the variable RESULT which will point to the new Symbol (see below)
        and be changed by action code
        6/13/96 frankf */
      if (prod == start_prod)
        out.println("      jaco.framework.parser.Symbol " + pre("result") + ";");
      
      /*
       * Add code to propagate RESULT assignments that occur in
       * action code embedded in a production (ie, non-rightmost
       * action code). 24-Mar-1998 CSA
       *
      for (int i=0; i<prod.rhs_length(); i++) {
        // only interested in non-terminal symbols.
        if (!(prod.rhs(i) instanceof symbol_part)) continue;
        symbol s = ((symbol_part)prod.rhs(i)).the_symbol();
        if (!(s instanceof non_terminal)) continue;
        // skip this non-terminal unless it corresponds to
        // an embedded action production.
        if (((non_terminal)s).is_embedded_action == false) continue;
        // OK, it fits.  Make a conditional assignment to RESULT.
        int index = prod.rhs_length() - i - 1; // last rhs is on top.
        out.println("              " + "// propagate RESULT from " +
            s.name());
        out.println("              " + "if ( " +
          "((java_cup.runtime.Symbol) " + emit.pre("stack") + ".elementAt("
              + emit.pre("top") + "-" + index + ")).value != null )");
        out.println("                " + "RESULT = " +
          "(" + prod.lhs().the_symbol().stack_type() + ") " +
          "((java_cup.runtime.Symbol) " + emit.pre("stack") + ".elementAt("
              + emit.pre("top") + "-" + index + ")).value;");
      }
      */
      
      boolean propagate = false;
        /* if there is an action string, emit it */
       if ((prod.action() != null) && (prod.action().code_string() != null) && !prod.action().equals(""))
       {
         String codestr = prod.action().code_string().trim();
         if (codestr.equals("ACCEPT;"))
           propagate = true;
         else
         {
           out.println("\t\t" + prod.lhs().the_symbol().stack_type() + " RESULT = null;");
           out.println("\t\t" + codestr);
         }
       }
       else
         out.println("              " +  prod.lhs().the_symbol().stack_type() + " RESULT = null;");
      /* here we have the left and right values being propagated.
        must make this a command line option.
         frankf 6/18/96 */

         /* Create the code that assigns the left and right values of
            the new Symbol that the production is reducing to */
      if (propagate)
      {
        if (prod == start_prod)
          out.println("              " + pre("result") + " = new jaco.framework.parser.Symbol(" + 
              prod.lhs().the_symbol().index() + "/*" +
              prod.lhs().the_symbol().name() + "*/" + ", $stack[" + pre("top") +
              "].left, $stack[" + pre("top") + "].right, $stack[" + pre("top") + "].value);");
        else
          out.println("              return " + pre("stack") + ".reuse(" +
              prod.lhs().the_symbol().index() + "/*" + 
              prod.lhs().the_symbol().name() + "*/);");
      }
      else
      if (emit.lr_values())
      {     
        int     loffset;
        String  leftstring, rightstring, both;
        int     roffset = 0;
        
        rightstring = emit.pre("top");    
        if (prod.rhs_length() == 0)
        {
          rightstring = "$stack[" + rightstring + "].left";
          if (prod == start_prod)
            out.println("              " + pre("result") + " = new jaco.framework.parser.Symbol(" + 
                prod.lhs().the_symbol().index() + "/*" +
                prod.lhs().the_symbol().name() + "*/" + ", " + rightstring + ", " + rightstring + ", RESULT);");
          else
            out.println("              return new jaco.framework.parser.Symbol(" + 
                prod.lhs().the_symbol().index() + "/*" +
                prod.lhs().the_symbol().name() + "*/" + ", " + rightstring + ", " + rightstring + ", RESULT);");
        }
        else
        {
          loffset = prod.rhs_length() - 1;
          leftstring = emit.pre("top") + ((loffset != 0) ? (" - " + loffset) : "");
          both = leftstring + ((loffset != 0) ? (", " + rightstring) : "");
          if (prod == start_prod)
            out.println("              " + pre("result") + " = new jaco.framework.parser.Symbol(" + 
              prod.lhs().the_symbol().index() + "/*" +
              prod.lhs().the_symbol().name() + "*/" + ", " + both + ", RESULT);");
          else
            out.println("              return " + pre("stack") + ".reuse(" +
              prod.lhs().the_symbol().index() + "/*" + 
              prod.lhs().the_symbol().name() + "*/" + ", " + both + ", RESULT);");
        }
      }
      else
      {
        if (prod == start_prod)
          out.println("              " + pre("result") + " = new jaco.framework.parser.Symbol(" + 
            prod.lhs().the_symbol().index() + "/*" +
            prod.lhs().the_symbol().name() + "*/" + 
            ", RESULT);");
        else
          out.println("              return new jaco.framework.parser.Symbol(" + 
            prod.lhs().the_symbol().index() + "/*" +
            prod.lhs().the_symbol().name() + "*/" + 
            ", RESULT);");
      }

      /* if this was the start production, do action for accept */
      if (prod == start_prod)
        {
          out.println("          /* ACCEPT */");
          out.println("          " + pre("parser") + ".done_parsing();");
          
          /* code to return lhs symbol */
          out.println("          return " + pre("result") + ";");
        }
      
      /* end of their block */
      out.println("            }");
      out.println();
    }

      /* end of switch */
      out.println("          /* . . . . . .*/");
      out.println("          default:");
      out.println("            throw new Exception(");
      out.println("               \"Invalid action number found in " +
                  "internal parse table\");");
      out.println();
      out.println("        }");

      /* end of method */
      out.println("    }");

      /* end of class */
      out.println("};\n");

      action_code_time = System.currentTimeMillis() - start_time;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit the production table. 
   * @param out stream to produce output on.
   */
  protected static void emit_production_table(PrintStream out)
    {
      production all_prods[];
      production prod;

      long start_time = System.currentTimeMillis();

      /* do the top of the table */
      out.println();
      out.println("  /** production table */");
      out.println("  protected static final short _production_table[][] = {"); 

      /* collect up the productions in order */
      all_prods = new production[production.number()];
      for (Enumeration p = production.all(); p.hasMoreElements(); )
    {
      prod = (production)p.nextElement();
      all_prods[prod.index()] = prod;
    }

      /* do one entry per production */
      out.print("    ");
      for (int i = 0; i<production.number(); i++)
    {
      prod = all_prods[i];

      /* make the table entry */
      out.print("    {");
      out.print(/* lhs symbol # */ prod.lhs().the_symbol().index() + ", ");
      out.print(/* rhs size */     prod.rhs_length() + "}");

      /* put in a comma if we aren't at the end */
      if (i < production.number()-1) out.print(", ");

      /* 5 entries per line */
      if ((i+1) % 5 == 0) 
        {
          out.println();
          out.print("    ");
        }
    }

      /* finish off the table initializer */
      out.println("  };");

      /* do the public accessor method */
      out.println();
      out.println("  /** access to production table */");
      out.println("  public short[][] production_table() " + 
                         "{return _production_table;}");

      production_table_time = System.currentTimeMillis() - start_time;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit the action table. 
   * @param out             stream to produce output on.
   * @param act_tab         the internal representation of the action table.
   * @param compact_reduces do we use the most frequent reduce as default?
   */
  protected static void do_action_table(PrintStream out, ObjectOutputStream oout, parse_action_table act_tab, boolean compact_reduces) throws internal_error
    {
      parse_action_row row;
      parse_action     act;
      int              red;

      long start_time = System.currentTimeMillis();

      out.println();
      out.println("  /** parse action table */");
      // out.println("  protected static final short[][] _action_table = {"); 
      out.println("  protected static short[][] _action_table;"); 
      short[][] _action_table = new short[act_tab.num_states()][];

      /* do each state (row) of the action table */
      for (short i = 0; i < act_tab.num_states(); i++)
      {
      /* get the row */
      row = act_tab.under_state[i];
    
      /* determine the default for the row */
      if (compact_reduces)
        row.compute_default();
      else
        row.default_reduce = -1;

      // out.print("    /*" + i + "*/{");

    /* do each column */
    int k = 2;
    for (short j = 0; j < row.size(); j++)
    {
        act = row.under_term[j];
        if ((act.kind() == parse_action.SHIFT) ||
            ((act.kind() == parse_action.REDUCE) &&
            (((reduce_action)act).reduce_with().index() != row.default_reduce)))
            k += 2;
    }
    _action_table[i] = new short[k];
    k = 0;
    for (short j = 0; j < row.size(); j++)
    {
          /* extract the action from the table */
          act = row.under_term[j];

          /* skip error entries these are all defaulted out */
        if (act.kind() != parse_action.ERROR)
        {
            /* first put in the symbol index, then the actual entry */
            /* shifts get positive entries of state number + 1 */
            if (act.kind() == parse_action.SHIFT)
            {
                _action_table[i][k++] = j;
                _action_table[i][k++] = (short)(((shift_action)act).shift_to().index() + 1);
                // out.print(j + "," + _action_table[i][k - 1] + ",");
            }
            /* reduce actions get negated entries of production# + 1 */
            else
            if (act.kind() == parse_action.REDUCE)
            {
                /* if its the default entry let it get defaulted out */
                red = ((reduce_action)act).reduce_with().index();
                if (red != row.default_reduce)
                {
                    _action_table[i][k++] = j;
                    _action_table[i][k++] = (short)-(red + 1);
                    // out.print(j + "," + (-(red+1)) + ",");
                }
            }
            else
            if (act.kind() == parse_action.NONASSOC)
            {
                /* do nothing, since we just want a syntax error */
            }
            /* shouldn't be anything else */
            else
                throw new internal_error("Unrecognized action code " + 
                                act.kind() + " found in parse table");
        }
    }
    
      /* finish off the row with a default entry */
      if (row.default_reduce != -1)
      {
        _action_table[i][k++] = -1;
        _action_table[i][k++] = (short)-(row.default_reduce + 1);
        // out.println("-1," + (-(row.default_reduce+1)) + "},");
      }
      else
      {
        _action_table[i][k++] = -1;
        _action_table[i][k++] = 0;
        // out.println("-1,0},");
      }
    }
    try
    {
        oout.writeObject(_action_table);
        oout.flush();
    }
    catch (IOException e)
    {
        System.out.println("unable to write action table");
    }
      /* finish off the init of the table */
      // out.println("  };");

      /* do the public accessor method */
      out.println();
      out.println("  /** access to parse action table */");
      out.println("  public short[][] action_table() {return _action_table;}");

      action_table_time = System.currentTimeMillis() - start_time;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit the reduce-goto table. 
   * @param out     stream to produce output on.
   * @param red_tab the internal representation of the reduce-goto table.
   */
  protected static void do_reduce_table(PrintStream out, ObjectOutputStream oout, parse_reduce_table red_tab)
    {
      lalr_state       goto_st;
      parse_action     act;

      long start_time = System.currentTimeMillis();

      out.println();
      out.println("  /** reduce_goto table */");
      // out.println("  protected static final short[][] _reduce_table = {"); 
      out.println("  protected static short[][] _reduce_table;"); 
      short[][] _reduce_table = new short[red_tab.num_states()][];
      
      /* do each row of the reduce-goto table */
        for (int i=0; i<red_tab.num_states(); i++)
        {
            // out.print("    /*" + i + "*/{");
            int k = 2;
            for (int j=0; j<red_tab.under_state[i].size(); j++)
            {
                if (red_tab.under_state[i].under_non_term[j] != null)
                    k += 2;
            }
            _reduce_table[i] = new short[k];
            k = 0;
            /* do each entry in the row */
            for (short j=0; j<red_tab.under_state[i].size(); j++)
            {
                /* get the entry */
                goto_st = red_tab.under_state[i].under_non_term[j];

                /* if we have none, skip it */
                if (goto_st != null)
                {
                    /* make entries for the index and the value */
                    _reduce_table[i][k++] = j;
                    _reduce_table[i][k++] = (short)goto_st.index();
                    // out.print(j + "," + goto_st.index() + ",");
                }
            }
            
            /* end row with default value */
            // out.println("-1,-1},");
            _reduce_table[i][k++] = -1;
            _reduce_table[i][k++] = -1;
        }
        try
        {
            oout.writeObject(_reduce_table);
            oout.flush();
        }
        catch (IOException e)
        {
            System.out.println("unable to write action table");
        }
      /* finish off the init of the table */
      // out.println("  };");

      /* do the public accessor method */
      out.println();
      out.println("  /** access to reduce_goto table */");
      out.println("  public short[][] reduce_table() {return _reduce_table;}");
      out.println();

      goto_table_time = System.currentTimeMillis() - start_time;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit the parser subclass with embedded tables. 
   * @param out             stream to produce output on.
   * @param action_table    internal representation of the action table.
   * @param reduce_table    internal representation of the reduce-goto table.
   * @param start_st        start state of the parse machine.
   * @param start_prod      start production of the grammar.
   * @param compact_reduces do we use most frequent reduce as default?
   */
  public static void parser(
    PrintStream        out, 
    parse_action_table action_table,
    parse_reduce_table reduce_table,
    int                start_st,
    production         start_prod,
    boolean            compact_reduces)
    throws internal_error
    {
      long start_time = System.currentTimeMillis();

      /* top of file */
      out.println("//----------------------------------------------------"); 
      out.println("// code was generated by " + version.title_str);
      out.println("// " + new Date());
      out.println("//----------------------------------------------------"); 
      out.println();
      emit_package(out);

      /* user supplied imports */
      for (int i = 0; i < import_list.size(); i++)
    out.println("import " + import_list.elementAt(i) + ";");

      /* class header */
      out.println();
      out.println("public final class " + parser_class_name + " extends jaco.framework.parser.LRParser {");

      /* constructor */
      out.println();
      out.println("  /** constructor */");
      out.println("  public " + parser_class_name + "() { super(); }");

      /* emit the various tables */
      try
      {
         FileOutputStream       ostream = new FileOutputStream(out_path + parser_class_name + ".tables");
         ObjectOutputStream p = new ObjectOutputStream(ostream);
         emit_production_table(out);
         do_action_table(out, p, action_table, compact_reduces);
         do_reduce_table(out, p, reduce_table);
         ostream.close();
      }
      catch (IOException e)
      {
        System.out.println("parser is corrupt; unable to write parse tables");
      }
      /* write parse table loader */
      out.println("  static {");
      out.println("    try {");
      out.println("      java.io.ObjectInputStream p = new java.io.ObjectInputStream(find_tables(\"" + package_name + "." + parser_class_name + "\"));");
      out.println("      _action_table = (short[][])p.readObject();");
      out.println("      _reduce_table = (short[][])p.readObject();");
      out.println("    }");
      out.println("    catch (Exception e) {");
      out.println("      throw new RuntimeException(\"unable to load parser tables: \" + e);");
      out.println("    }");
      out.println("  }");

      /* instance of the action encapsulation class */
      out.println("  /** instance of action encapsulation class */");
      out.println("  protected " + specialPre("Actions") + " action_obj;");
      out.println();

      /* action object initializer */
      out.println("  /** action encapsulation object initializer */");
      out.println("  protected void init_actions()");
      out.println("  {");
      out.println("      actObj = (action_obj = new " + specialPre("Actions") + "(this, stack));");
      out.println("  }");
      out.println();

      /* method to tell the parser about the start state */
      out.println("  /** start state */");
      out.println("  public final int start_state() { return " + start_st + "; }");

      /* method to indicate start production */
      out.println("  /** start production */");
      out.println("  public final int start_production() { return " + start_production.index() + "; }");
      out.println();

      /* methods to indicate EOF and error symbol indexes */
      out.println("  /** EOF Symbol index */");
      out.println("  public final int EOF_sym() { return " + terminal.EOF.index() + "; }");
      out.println();
      out.println("  /** error Symbol index */");
      out.println("  public final int error_sym() { return " + terminal.error.index() + "; }");
      out.println();

      /* user supplied code for user_init() */
      if (init_code != null)
      {
        out.println();
        out.println("  /** user initialization */");
        out.println("  public final void user_init() throws java.lang.Exception");
        out.println("  {");
        out.println(init_code);
        out.println("  }");
      }

      /* user supplied code for scan */
      if (scan_code != null)
    {
          out.println();
      out.println("  /** scan to get the next Symbol */");
      out.println("  public final jaco.framework.parser.Symbol scan()");
      out.println("    throws java.lang.Exception");
      out.println("  {");
      out.println(scan_code);
      out.println("  }");
    }

      /* user supplied code */
      if (parser_code != null)
    {
      out.println();
          out.println(parser_code);
    }

      /* end of class */
      out.println("};");

      /* put out the action code class */
      emit_action_code(out, start_prod);

      parser_time = System.currentTimeMillis() - start_time;
    }

    /*-----------------------------------------------------------*/
}


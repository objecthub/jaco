package jcup;

import java.util.Enumeration;
import java.io.*;
import java.awt.*;

/** This class serves as the main driver for the JavaCup system.
 *  It accepts user options and coordinates overall control flow.
 *  The main flow of control includes the following activities: 
 *  <ul>
 *    <li> Parse user supplied arguments and options.
 *    <li> Open output files.
 *    <li> Parse the specification from standard input.
 *    <li> Check for unused terminals, non-terminals, and productions.
 *    <li> Build the state machine, tables, etc.
 *    <li> Output the generated code.
 *    <li> Close output files.
 *    <li> Print a summary if requested.
 *  </ul>
 *
 *  Options to the main program include: <dl>
 *   <dt> -package name  
 *   <dd> specify package generated classes go in [default none]
 *   <dt> -parser name   
 *   <dd> specify parser class name [default "parser"]
 *   <dt> -symbols name  
 *   <dd> specify name for symbol constant class [default "sym"]
 *   <dt> -nonterms      
 *   <dd> put non terminals in symbol constant class
 *   <dt> -expect #      
 *   <dd> number of conflicts expected/allowed [default 0]
 *   <dt> -compact_red   
 *   <dd> compact tables by defaulting to most frequent reduce
 *   <dt> -nowarn        
 *   <dd> don't warn about useless productions, etc.
 *   <dt> -nosummary     
 *   <dd> don't print the usual summary of parse states, etc.
 *   <dt> -progress      
 *   <dd> print messages to indicate progress of the system
 *   <dt> -time          
 *   <dd> print time usage summary
 *   <dt> -dump_grammar  
 *   <dd> produce a dump of the symbols and grammar
 *   <dt> -dump_states   
 *   <dd> produce a dump of parse state machine
 *   <dt> -dump_tables   
 *   <dd> produce a dump of the parse tables
 *   <dt> -dump          
 *   <dd> produce a dump of all of the above
 *   <dt> -debug         
 *   <dd> turn on debugging messages within JavaCup 
 *   <dt> -nopositions
 *   <dd> don't generate the positions code
 *   </dl>
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */

public class Main {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/
  /** Only constructor is private, so we do not allocate any instances of this
      class. */
  private Main() { }

  /*-------------------------*/
  /* Options set by the user */
  /*-------------------------*/
  /** User option -- do we print progress messages. */
  protected static boolean print_progress   = true;
  /** User option -- do we produce a dump of the state machine */
  protected static boolean opt_dump_states  = false;
  /** User option -- do we produce a dump of the parse tables */
  protected static boolean opt_dump_tables  = false;
  /** User option -- do we produce a dump of the grammar */
  protected static boolean opt_dump_grammar = false;
  /** User option -- do we show timing information as a part of the summary */
  protected static boolean opt_show_timing  = false;
  /** User option -- do we run produce extra debugging messages */
  protected static boolean opt_do_debug     = false;
  /** User option -- do we compact tables by making most common reduce the 
      default action */
  protected static boolean opt_compact_red  = false;
  /** User option -- should we include non terminal symbol numbers in the 
      symbol constant class. */
  protected static boolean include_non_terms = false;
  /** User option -- do not print a summary. */
  protected static boolean no_summary = false;
  /** User option -- number of conflicts to expect */
  protected static int expect_conflicts = 0;

  /* frankf added this 6/18/96 */
  /** User option -- should generator generate code for left/right values? */
  protected static boolean lr_values = true;

  /*----------------------------------------------------------------------*/
  /* Timing data (not all of these time intervals are mutually exclusive) */
  /*----------------------------------------------------------------------*/
  /** Timing data -- when did we start */
  protected static long start_time       = 0;
  /** Timing data -- when did we end preliminaries */
  protected static long prelim_end       = 0;
  /** Timing data -- when did we end parsing */
  protected static long parse_end        = 0;
  /** Timing data -- when did we end checking */
  protected static long check_end        = 0;
  /** Timing data -- when did we end dumping */
  protected static long dump_end         = 0;
  /** Timing data -- when did we end state and table building */
  protected static long build_end        = 0;
  /** Timing data -- when did we end nullability calculation */
  protected static long nullability_end  = 0;
  /** Timing data -- when did we end first set calculation */
  protected static long first_end        = 0;
  /** Timing data -- when did we end state machine construction */
  protected static long machine_end      = 0;
  /** Timing data -- when did we end table construction */
  protected static long table_end        = 0;
  /** Timing data -- when did we end checking for non-reduced productions */
  protected static long reduce_check_end = 0;
  /** Timing data -- when did we finish emitting code */
  protected static long emit_end         = 0;
  /** Timing data -- when were we completely done */
  protected static long final_time       = 0;

  /* Additional timing information is also collected in emit */

  /*-----------------------------------------------------------*/
  /*--- Main Program ------------------------------------------*/
  /*-----------------------------------------------------------*/

  /** The main driver for the system. 
   * @param argv an array of strings containing command line arguments.
   */
  public static void main(String argv[]) throws internal_error, IOException, Exception
    {
      boolean did_output = false;

      start_time = System.currentTimeMillis();

      /* process user options and arguments */
      String    fname = parse_args(argv);
      if (fname == null)
      {
            String message = "Where is the grammar?";
            DataInputStream dataStream = null;
            FileInputStream fileStream;
            FileDialog inputFileDialog;
            Frame theFrame = new Frame("jcup");
            inputFileDialog = new FileDialog(theFrame, message, FileDialog.LOAD);
            inputFileDialog.show();
            inputFileDialog.pack();
            if (inputFileDialog.getFile() == null)
                System.exit(0);
            File inputFile = new File(inputFileDialog.getDirectory(), inputFileDialog.getFile());
            if (emit.out_path.length() == 0)
                emit.out_path = inputFileDialog.getDirectory();
            fname = inputFile.getAbsolutePath();
      }
      /* frankf 6/18/96
     hackish, yes, but works */
      emit.set_lr_values(lr_values);
      /* open output files */
      if (print_progress)
        System.out.println("[opening files]");
      open_files(fname);
    
      prelim_end = System.currentTimeMillis();

      /* parse spec into internal data structures */
      if (print_progress) 
        System.out.println("[parsing specification]");
      parse_grammar_spec();

      parse_end = System.currentTimeMillis();

      /* don't proceed unless we are error free */
      if (lexer.error_count == 0)
    {
      /* check for unused bits */
        if (print_progress)
            System.out.println("[checking specification]");
          check_unused();

          check_end = System.currentTimeMillis();

      /* build the state machine and parse tables */
          if (print_progress)
              System.out.println("[building parse tables]");
          build_parser();

          build_end = System.currentTimeMillis();
          
      /* output the generated code, if # of conflicts permits */
      if (lexer.error_count != 0) {
          // conflicts! don't emit code, don't dump tables.
          opt_dump_tables = false;
      } else { // everything's okay, emit parser.
          if (print_progress) System.out.println("[writing parser]");
          emit_parser();
          did_output = true;
      }
    }

      /* fix up the times to make the summary easier */
      emit_end = parse_end;

      /* do requested dumps */
      if (opt_dump_grammar) dump_grammar();
      if (opt_dump_states)  dump_machine(); 
      if (opt_dump_tables)  dump_tables(); 

      dump_end = System.currentTimeMillis();

      /* close output files */
      if (print_progress)
          System.out.println("[closing files]");
      close_files();

      /* produce a summary if desired */
      if (!no_summary) emit_summary(did_output);
      
      /* If there were errors during the run,
       * exit with non-zero status (makefile-friendliness). --CSA */
      if (lexer.error_count != 0)
      System.exit(100);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Print a "usage message" that described possible command line options, 
   *  then exit.
   * @param message a specific error message to preface the usage message by.
   */
  protected static void usage(String message)
    {
      System.out.println();
      System.out.println(message);
      System.out.println();
      System.out.println(
"usage: " + version.program_name + " [options]\n" +
"  and expects a specification file on standard input\n" +
"  legal options include:\n" +
"    -package name  specify package generated classes go in [default none]\n" +
"    -parser name   specify parser class name [default \"parser\"]\n" +
"    -symbols name  specify name for symbol constant interface [default \"sym\"]\n"+
"    -o <outdir>    specify directory for jcup output\n" +
"    -f <file>      parse <file> instead of standard input\n" +
"    -nonterms      put non terminals in symbol constant class\n" + 
"    -expect #      number of conflicts expected/allowed [default 0]\n" + 
"    -compact_red   compact tables by defaulting to most frequent reduce\n" +
"    -nowarn        don't warn about useless productions, etc.\n" +
"    -nosummary     don't print the usual summary of parse states, etc.\n" +
"    -nopositions   don't propagate the left and right token position values\n" +
"    -progress      print messages to indicate progress of the system\n" +
"    -time          print time usage summary\n" +
"    -dump_grammar  produce a human readable dump of the symbols and grammar\n"+
"    -dump_states   produce a dump of parse state machine\n"+
"    -dump_tables   produce a dump of the parse tables\n"+
"    -dump          produce a dump of all of the above\n"
      );
      System.exit(1);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Parse command line options and arguments to set various user-option
   *  flags and variables. 
   * @param argv the command line arguments to be parsed.
   */
  protected static String parse_args(String argv[])
    {
      int len = argv.length;
      int i;
      String res = null;
      
      /* parse the options */
      for (i=0; i<len; i++)
    {
      /* try to get the various options */
      if (argv[i].equals("-package"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || 
                argv[i].endsWith(".cup")) 
        usage("-package must have a name argument");

          /* record the name */
          emit.package_name = argv[i];
        }
      else if (argv[i].equals("-parser"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || 
                argv[i].endsWith(".cup")) 
        usage("-parser must have a name argument");

          /* record the name */
          emit.parser_class_name = argv[i];
        }
      else if (argv[i].equals("-symbols"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || 
                argv[i].endsWith(".cup")) 
        usage("-symbols must have a name argument");

          /* record the name */
          emit.symbol_const_class_name = argv[i];
        }
      else if (argv[i].equals("-o"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || 
                argv[i].endsWith(".cup")) 
        usage("-o must have a path argument");

          /* record the path */
          emit.out_path = argv[i];
        }
      else if (argv[i].equals("-nonterms"))
        {
          include_non_terms = true;
        }
      else if (argv[i].equals("-f"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || !argv[i].endsWith(".cup")) 
            usage("-f must have a .cup file as argument");
          res = argv[i];
        }
      else if (argv[i].equals("-expect"))
        {
          /* must have an arg */
          if (++i >= len || argv[i].startsWith("-") || 
                argv[i].endsWith(".cup")) 
        usage("-expect must have a name argument");

          /* record the number */
          try {
            expect_conflicts = Integer.parseInt(argv[i]);
          } catch (NumberFormatException e) {
        usage("-expect must be followed by a decimal integer");
          }
        }
      else if (argv[i].equals("-compact_red"))  opt_compact_red = true;
      else if (argv[i].equals("-nosummary"))    no_summary = true;
      else if (argv[i].equals("-nowarn"))       emit.nowarn = true;
      else if (argv[i].equals("-dump_states"))  opt_dump_states = true;
      else if (argv[i].equals("-dump_tables"))  opt_dump_tables = true; 
      else if (argv[i].equals("-progress"))     print_progress = true;
      else if (argv[i].equals("-dump_grammar")) opt_dump_grammar = true;
      else if (argv[i].equals("-dump")) 
            opt_dump_states = opt_dump_tables = opt_dump_grammar = true; 
      else if (argv[i].equals("-time"))         opt_show_timing = true; 
      else if (argv[i].equals("-debug"))        opt_do_debug = true;
      /* frankf 6/18/96 */
      else if (argv[i].equals("-nopositions"))  lr_values = false;
      else
        {
          usage("Unrecognized option \"" + argv[i] + "\"");
        }
    }
      return res;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /*-------*/
  /* Files */
  /*-------*/

  /** Input file.  This is a buffered version of System.in. */
  public static BufferedInputStream input_file;
  
  /** Output file for the parser class. */
  protected static PrintStream parser_class_file;

  /** Output file for the symbol constant class. */
  protected static PrintStream symbol_class_file;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Open various files used by the system. */
  protected static void open_files(String fname)
    {
      File fil;
      String out_name;

      /* use a buffered version of standard input */
      try
      {
      input_file = new BufferedInputStream(new FileInputStream(fname));
      }
      catch (FileNotFoundException e)
      {
          System.out.println("can't open \"" + fname + "\" for input");
          System.exit(-1);
      }
      /* open each of the output files */

      /* parser class */
      out_name = emit.out_path + emit.parser_class_name + ".java";
      fil = new File(out_name);
      try {
        parser_class_file = new PrintStream(
         new BufferedOutputStream(new FileOutputStream(fil), 4096));
      } catch(Exception e) {
    System.out.println("can't open \"" + out_name + "\" for output");
    System.exit(3);
      }

      /* symbol constants class */
      out_name = emit.out_path + emit.symbol_const_class_name + ".java";
      fil = new File(out_name);
      try {
        symbol_class_file = new PrintStream(
         new BufferedOutputStream(new FileOutputStream(fil), 4096));
      } catch(Exception e) {
    System.out.println("can't open \"" + out_name + "\" for output");
    System.exit(4);
      }
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Close various files used by the system. */
  protected static void close_files() throws java.io.IOException
    {
      if (input_file != null) input_file.close();
      if (parser_class_file != null) parser_class_file.close();
      if (symbol_class_file != null) symbol_class_file.close();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Parse the grammar specification from standard input.  This produces
   *  sets of terminal, non-terminals, and productions which can be accessed
   *  via static variables of the respective classes, as well as the setting
   *  of various variables (mostly in the emit class) for small user supplied
   *  items such as the code to scan with.
   */
  protected static void parse_grammar_spec() throws java.lang.Exception
    {
      parser parser_obj;

      /* create a parser and parse with it */
      parser_obj = new parser();
      try {
    if (opt_do_debug)
          parser_obj.debug_parse();
    else
          parser_obj.parse();
      } catch (Exception e)
      {
    /* something threw an exception.  catch it and emit a message so we 
       have a line number to work with, then re-throw it */
    lexer.emit_error("Internal error: Unexpected exception");
    throw e;
      }
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Check for unused symbols.  Unreduced productions get checked when
   *  tables are created.
   */
  protected static void check_unused()
    {
      terminal term;
      non_terminal nt;

      /* check for unused terminals */
      for (Enumeration t = terminal.all(); t.hasMoreElements(); )
    {
      term = (terminal)t.nextElement();

      /* don't issue a message for EOF */
      if (term == terminal.EOF) continue;

      /* or error */
      if (term == terminal.error) continue;

      /* is this one unused */
      if (term.use_count() == 0)
        {
          /* count it and warn if we are doing warnings */
          emit.unused_term++;
          if (!emit.nowarn) 
        {
          System.out.println("warning: terminal \"" + term.name() + 
                     "\" was declared but never used");
          lexer.warning_count++;
        }
        }
    }

      /* check for unused non terminals */
      for (Enumeration n = non_terminal.all(); n.hasMoreElements(); )
    {
      nt = (non_terminal)n.nextElement();

      /* is this one unused */
      if (nt.use_count() == 0)
        {
          /* count and warn if we are doing warnings */
          emit.unused_term++;
          if (!emit.nowarn) 
        {
          System.out.println("warning: non terminal \"" + nt.name() + 
                     "\" was declared but never used");
          lexer.warning_count++;
        }
        }
    }

    }

  /* . . . . . . . . . . . . . . . . . . . . . . . . .*/
  /* . . Internal Results of Generating the Parser . .*/
  /* . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Start state in the overall state machine. */
  protected static lalr_state start_state;

  /** Resulting parse action table. */
  protected static parse_action_table action_table;

  /** Resulting reduce-goto table. */
  protected static parse_reduce_table reduce_table;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Build the (internal) parser from the previously parsed specification.
   *  This includes:<ul>
   *    <li> Computing nullability of non-terminals.
   *    <li> Computing first sets of non-terminals and productions.
   *    <li> Building the viable prefix recognizer machine.
   *    <li> Filling in the (internal) parse tables.
   *    <li> Checking for unreduced productions.
   *  </ul>
   */
  protected static void build_parser() throws internal_error
    {
      /* compute nullability of all non terminals */
      if (opt_do_debug || print_progress) 
    System.out.println("[  computing non-terminal nullability]");
      non_terminal.compute_nullability();

      nullability_end = System.currentTimeMillis();

      /* compute first sets of all non terminals */
      if (opt_do_debug || print_progress) 
    System.out.println("[  computing first sets]");
      non_terminal.compute_first_sets();

      first_end = System.currentTimeMillis();

      /* build the LR viable prefix recognition machine */
      if (opt_do_debug || print_progress) 
    System.out.println("[  building state machine]");
      start_state = lalr_state.build_machine(emit.start_production);

      machine_end = System.currentTimeMillis();

      /* build the LR parser action and reduce-goto tables */
      if (opt_do_debug || print_progress) 
    System.out.println("[  filling in tables]");
      action_table = new parse_action_table();
      reduce_table = new parse_reduce_table();
      for (Enumeration st = lalr_state.all(); st.hasMoreElements(); )
    {
      lalr_state lst = (lalr_state)st.nextElement();
      lst.build_table_entries(
                                  action_table, reduce_table);
    }

      table_end = System.currentTimeMillis();

      /* check and warn for non-reduced productions */
      if (opt_do_debug || print_progress) 
    System.out.println("[  checking for non-reduced productions]");
      action_table.check_reductions();

      reduce_check_end = System.currentTimeMillis();

      /* if we have more conflicts than we expected issue a message and die */
      if (emit.num_conflicts > expect_conflicts)
    {
      System.out.println("error: more conflicts encountered than expected; " +
                 "parser generation aborted");
      lexer.error_count++;
      build_end = System.currentTimeMillis();
      
      /* do dumps and summary as needed */
          if (opt_dump_grammar) dump_grammar();
          if (opt_dump_states)  dump_machine(); 
      if (!no_summary) emit_summary(false);

      System.exit(100);
    }
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Call the emit routines necessary to write out the generated parser. */
  protected static void emit_parser() throws internal_error
    {
      emit.symbols(symbol_class_file, include_non_terms);
      emit.parser(parser_class_file, action_table, reduce_table, 
          start_state.index(), emit.start_production, opt_compact_red);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Helper routine to optionally return a plural or non-plural ending. 
   * @param val the numerical value determining plurality.
   */
  protected static String plural(int val)
    {
      if (val == 1)
    return "";
      else
    return "s";
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit a long summary message to standard error (System.err) which 
   *  summarizes what was found in the specification, how many states were
   *  produced, how many conflicts were found, etc.  A detailed timing 
   *  summary is also produced if it was requested by the user.
   * @param output_produced did the system get far enough to generate code.
   */
  protected static void emit_summary(boolean output_produced)
    {
      final_time = System.currentTimeMillis();

      if (no_summary) return;

      System.out.println("------- " + version.title_str + 
             " parser generation summary -------");

      /* error and warning count */
      System.out.println("  " + lexer.error_count + " error" + 
     plural(lexer.error_count) + " and " + lexer.warning_count + 
     " warning" + plural(lexer.warning_count));

      /* basic stats */
      System.out.print("  " + terminal.number() + " terminal" + 
             plural(terminal.number()) + ", ");
      System.out.print(non_terminal.number() + " non terminal" + 
             plural(non_terminal.number()) + ", and ");
      System.out.println(production.number() + " production" + 
             plural(production.number()) + " declared, ");
      System.out.println("  producing " + lalr_state.number() + 
             " unique parse states");

      /* unused symbols */
      System.out.println("  " + emit.unused_term + " terminal" + 
             plural(emit.unused_term) + " declared but not used");
      System.out.println("  " + emit.unused_non_term + " non terminal" + 
             plural(emit.unused_term) + " declared but not used");

      /* productions that didn't reduce */
      System.out.println("  " + emit.not_reduced + " production" + 
             plural(emit.not_reduced) + " never reduced");

      /* conflicts */
      System.out.println("  " + emit.num_conflicts + " conflict" +
             plural(emit.num_conflicts) + " detected" +
                     " (" + expect_conflicts + " expected)");

      /* code location */
      if (output_produced)
    System.out.println("  code written to \"" + emit.parser_class_name + 
            ".java\", and \"" + emit.symbol_const_class_name + ".java\"");
      else
    System.out.println("  no code produced");

      if (opt_show_timing) show_times();

      System.out.println(
    "---------------------------------------------------- (" + 
     version.version_str + ")");
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce the optional timing summary as part of an overall summary. */
  protected static void show_times()
    {
      long total_time = final_time - start_time;
      System.out.println("[total time: "
        + timestr(final_time-start_time, total_time) + "ms]");
      System.out.println("[startup time: "
        + timestr(prelim_end-start_time, total_time) + "ms]");
      System.out.println("[parse time: "
        + timestr(parse_end-prelim_end, total_time) + "ms]");
      if (check_end != 0)
        System.out.println("[checking time: "
        + timestr(check_end-parse_end, total_time) + "ms]");
      if (check_end != 0 && build_end != 0)
        System.out.println("[parser building time: "
        + timestr(build_end-check_end, total_time) + "ms]");
      if (nullability_end != 0 && check_end != 0)
        System.out.println("[nullability check time: "
        + timestr(nullability_end-check_end, total_time) + "ms]");
      if (first_end != 0 && nullability_end != 0)
        System.out.println("[computing first sets: "
            + timestr(first_end-nullability_end, total_time) + "ms]");
      if (machine_end != 0 && first_end != 0)
        System.out.println("[state building time: " 
        + timestr(machine_end-first_end, total_time) + "ms]"); 
      if (table_end != 0 && machine_end != 0)
        System.out.println("[table building time: " 
        + timestr(table_end-machine_end, total_time) + "ms]"); 
      if (reduce_check_end != 0 && table_end != 0)
        System.out.println("[checking time: " 
        + timestr(reduce_check_end-table_end, total_time) + "ms]");
      if (emit_end != 0 && build_end != 0)
        System.out.println("[code output: "
        + timestr(emit_end-build_end, total_time) + "ms]");
      if (emit.symbols_time != 0)
    System.out.println("[symbols interface: "
        + timestr(emit.symbols_time, total_time) + "ms]");
      if (emit.parser_time != 0)
    System.out.println("[parser class "
        + timestr(emit.parser_time, total_time) + "ms]");
      if (emit.action_code_time != 0)
    System.out.println("[actions innerclass: "
        + timestr(emit.action_code_time, total_time) + "ms]");
      if (emit.production_table_time != 0)
    System.out.println("[production table: "
        + timestr(emit.production_table_time, total_time) + "ms]");
      if (emit.action_table_time != 0)
    System.out.println("[action table: "
        + timestr(emit.action_table_time, total_time) + "ms]");
      if (emit.goto_table_time != 0)
    System.out.println("[reduce table: "
        + timestr(emit.goto_table_time, total_time) + "ms]");
      System.out.println("[dump output: "
    + timestr(dump_end-emit_end, total_time) + "ms]");
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Helper routine to format a decimal based display of seconds and
   *  percentage of total time given counts of milliseconds.   Note: this
   *  is broken for use with some instances of negative time (since we don't 
   *  use any negative time here, we let if be for now).
   * @param time_val   the value being formatted (in ms).
   * @param total_time total time percentages are calculated against (in ms).
   */
  protected static String timestr(long time_val, long total_time)
    {
      boolean neg;
      long    ms = 0;
      long    sec = 0;
      long    percent10;
      String  pad;

      /* work with positives only */
      neg = time_val < 0;
      if (neg) time_val = -time_val;

      /* pull out seconds and ms */
      ms = time_val % 1000;
      sec = time_val / 1000;

      /* construct a pad to blank fill seconds out to 4 places */
      if (sec < 10)   
    pad = "   ";
      else if (sec < 100)  
    pad = "  ";
      else if (sec < 1000) 
    pad = " ";
      else
    pad = "";

      /* calculate 10 times the percentage of total */
      percent10 = (time_val*1000)/total_time;

      /* build and return the output string */
      return (neg ? "-" : "") + pad + sec + "." + 
         ((ms%1000)/100) + ((ms%100)/10) + (ms%10) + "sec" +
         " (" + percent10/10 + "." + percent10%10 + "%)";
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a human readable dump of the grammar. */
  public static void dump_grammar() throws internal_error
    {
      int cnt;
      Enumeration t, n, p;
      production prod;

      System.out.println("===== Terminals =====");
      for (t = terminal.all(), cnt=0; t.hasMoreElements(); cnt++)
    {
      System.out.print(((terminal)t.nextElement()).name() + " ");
      if ((cnt+1) % 5 == 0) System.out.println();
    }
      System.out.println();
      System.out.println();

      System.out.println("===== Non terminals =====");
      for (n=non_terminal.all(), cnt=0; n.hasMoreElements(); cnt++)
    {
      System.out.print(((non_terminal)n.nextElement()).name() + " ");
      if ((cnt+1) % 5 == 0) System.out.println();
    }
      System.out.println();
      System.out.println();


      System.out.println("===== Productions =====");
      for (p=production.all(); p.hasMoreElements(); )
    {
      prod = (production)p.nextElement();
      System.out.print(prod.lhs().the_symbol().name() + " ::= ");
      for (int i=0; i<prod.rhs_length(); i++)
        if (prod.rhs(i).is_action())
          System.out.print("{action} ");
        else
          System.out.print(
             ((symbol_part)prod.rhs(i)).the_symbol().name() + " ");
      System.out.println();
    }
      System.out.println();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a (semi-) human readable dump of the complete viable prefix 
   *  recognition state machine. 
   */
  public static void dump_machine()
    {
      lalr_state ordered[] = new lalr_state[lalr_state.number()];

      /* put the states in sorted order for a nicer display */
      for (Enumeration s = lalr_state.all(); s.hasMoreElements(); )
    {
      lalr_state st = (lalr_state)s.nextElement();
      ordered[st.index()] = st;
    }

      System.out.println("===== Viable Prefix Recognizer =====");
      for (int i = 0; i<lalr_state.number(); i++)
    {
      if (ordered[i] == start_state) System.out.print("START ");
          System.out.println(ordered[i]);
      System.out.println("-------------------");
    }
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a (semi-) human readable dumps of the parse tables */
  public static void dump_tables()
    {
      System.out.println(action_table);
      System.out.println(reduce_table);
    }

  /*-----------------------------------------------------------*/

};


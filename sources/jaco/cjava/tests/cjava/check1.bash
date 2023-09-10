#!/bin/bash
#
# Run the first set of checks
#
# CHECK 1: the parser
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="compounds: check parser"


TESTNAME "${mainname} (ClassDecl)"
SEND <<END
    import java.io.*;
    class ClassDecl extends Throwable implements Serializable
    {
    }
	boo // cause an error; I don't want it to compile
END

EXPECT <<END
${tempfile}-send.java:5: syntax error (103)
	boo // cause an error; I don't want it to compile
        ^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
END

RUNTEST $command
ENDTEST

TESTNAME "${mainname} (ClassDecl2)"
SEND <<EOF
    import java.io.*
    class ClassDecl2 extends [Object, Cloneable] implements Serializable, Cloneable
	//                  ^ syntax error
    {
    }
EOF

EXPECT <<EOF
${tempfile}-send.java:2: syntax error (19)
    class ClassDecl2 extends [Object, Cloneable] implements Serializable, Cloneable
    ^
EOF

RUNTEST $command
ENDTEST

TESTNAME "${mainname} (ClassImplementsCompound)"
SEND <<END
    import java.io.*;
    class ClassDecl3 implements [Serializable, Cloneable]
    {
    }
    x // syntax error
END

EXPECT <<END
${tempfile}-send.java:5: syntax error (103)
    x // syntax error
    ^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
END

RUNTEST $command
ENDTEST


TESTNAME "${mainname} (InterfaceExtendsCompound)"
SEND <<END
    import java.io.*;
    interface Decl4 extends [Serializable, Cloneable]
    {
    }
    x // syntax error
END

EXPECT <<END
${tempfile}-send.java:5: syntax error (103)
    x // syntax error
    ^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
END

RUNTEST $command
ENDTEST


TESTNAME "${mainname} (AsReference)"
SEND <<END
	import java.io.*;
	public class AsReference 
	{
	    int dummy4;
	    [Serializable, Cloneable] dammit;
	    [Object, Serializable, Cloneable] boo;
    
 	   public void main(int argc, String[] argv)
	    {
		[Serializable, Cloneable] sc = ([Serializable, Cloneable])this;
		if( this instanceof [Serializable, Object] )
		    System.out.println("woops");
		if( this instanceof [Serializable, Cloneable] )
		    System.out.println("no");	

		[Serializable, Object, AsReference] wrong = null;
		//            ^ error: two classes in a compound
 	   }
	}
END

EXPECT <<END
${tempfile}-send.java:16: compound types cannot contain more than one class.
		[Serializable, Object, AsReference] wrong = null;
                ^
END

RUNTEST $command
ENDTEST


TESTNAME "${mainname} (AsArrays)"
SEND <<END
	import java.io.*;
	public class AsArrays implements Serializable, Cloneable
	{
	    boolean dummy(Object o)
	    {
		[Serializable, Cloneable][] beurk;
		beurk = new AsArrays[12];
                beurk[0] = ([Serializable, Cloneable])o;
                beurk = ([Serializable, Cloneable][])o;
		return beurk instanceof [Object, Cloneable][];
	    }

	    public Object clone() { return this; }
	}
END

EXPECT <<END
////// ${tempfile}-send.java //////
import java.io.*;

public class AsArrays implements Serializable, Cloneable {
   boolean dummy(Object o) {
      [Serializable, Cloneable][] beurk;
      beurk = new AsArrays[12];
      beurk[0] = ([Serializable, Cloneable])o;
      beurk = ([Serializable, Cloneable][])o;
      return beurk instanceof [Object, Cloneable][];
   }
   public Object clone() {
      return this;
   }
}
END

RUNTEST $command -debug parser@2
ENDTEST

TESTNAME "${mainname} (NoCompoundCreation)"
SEND <<EOF

interface A1 { 
}
interface A2 { 
}
class NoCompoundCreation
{
    [A1, A2] dummy()
    {
	return new NoCompoundCreation(); // ok
	return new [A1, A2]; // syntax error
	return new [NoCompoundCreation](); // syntax error
    }
}

EOF

EXPECT <<EOF
${tempfile}-send.java:11: syntax error (73)
	return new [A1, A2]; // syntax error
                           ^
${tempfile}-send.java:12: syntax error (134)
	return new [NoCompoundCreation](); // syntax error
                    ^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
EOF

RUNTEST $command 

ENDTEST

TESTNAME "${mainname} (Nested)"
SEND <<EOF

interface A1 { 
}
interface A2 { 
}
class NoCompoundCreation
{
    void dummy()
    {
	[A1, [A2, NoCompoundCreation]] a;
	[[A1, A2, NoCompoundCreation]] b;
	[Object, [A1, NoCompoundCreation]] c;
    }
}
w // causes a syntax error and stops compiler
EOF

EXPECT <<EOF
${tempfile}-send.java:15: syntax error (103)
w // causes a syntax error and stops compiler
^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
EOF

RUNTEST $command 

ENDTEST


exit $failure






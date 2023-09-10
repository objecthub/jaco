#!/bin/bash
#
# Run the second set of checks
#
# CHECK 2: semantic analyzer
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="compounds: check semantic analyzer"

TESTNAME "${mainname} (General)"
SEND <<EOF
    import java.io.*;
interface A1
{
    void a1();
}

interface A2
{
    boolean a2();
}

interface A3
{
    int a3();
}

class Test implements Cloneable, A1, A3
{
    [Cloneable, A1] oo;
    boolean isOk()
    {
	return true;
    }

    boolean isNotOk()
    {
	return false;
    }

    [A1, Cloneable] bonobo()
    {
	oo = this;
	[A1, A3] a1a3 = this;
	[Test, Cloneable] testcloneable = this;
	if(this instanceof [A1, A3])
	    return this;
        return null;
    }

    int toto([A1, A2] a1a2)
    {
	a1a2.a1();

	if(a1a2.getClass()!=null && a1a2.a2())
	    return (([A1, A3])a1a2).a3();
	return 0;
    }

    public void a1()
    {
    }

    public boolean a2()
    {
	return false;
    }

    public int a3()
    {
	return -1;
    }

    public Object clone()
    {
	return null;
    }
}
EOF

EXPECT <<EOF
EOF

# no result; 
# the translator fails and  throws an exception, which is removed by the ignorepattern
RUNTEST $command
ENDTEST

TESTNAME "${mainname} (NoNewArrays)"
SEND <<EOF
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
    [A1, A2][] dummy()
    {
	[A1][] w = new [A1][3];
	return new [A1, A2][3];
    }
}
EOF

EXPECT <<EOF
${tempfile}-send.java:11: cannot create arrays of compound types
	return new [A1, A2][3];
               ^
EOF

RUNTEST $command 
ENDTEST


TESTNAME "${mainname} (WithArrays)"
SEND <<EOF

interface A1 { 
    void a1();
}
interface A2 { 
    boolean a2();
}
interface A3 { 
    int a3();
}

class WithArrays implements A1, A2, A3
{
    [A1, A2][] objects;
    [A1][] toto([A1, A2] one, [A1, A2, A3] two)
    {
	objects = new WithArrays[12];
	objects[0] = this;
	objects[1] = one;
	objects[2] = two;
	if(two.a3()>0)
	  return objects;
	else
	  return null;
    }
   
    public void a1() { }
    public boolean a2() { return true; } 
    public int a3() { return 3; }
}

EOF

EXPECT <<EOF
EOF


RUNTEST $command
ENDTEST

TESTNAME "${mainname} (Exceptions)"
SEND <<EOF

interface A1 { 
    void a1();
}

interface A2 { 
    boolean a2();
}
interface A3 { 
    int a3();
}

class TestException extends Exception implements A1, A2 {
    public TestException() {} 
    public void a1() { }
    public boolean a2() { return true; } 
}

class TestExceptions 
{
    void dummy()
    {
	try
	{
	    [Exception, A2] ea2 = new TestException();
	    throw new TestException();
	    throw ea2;
	}
	catch([Exception, A1] ea1)
	{
	    ea1.a1();
	}
    }
}

EOF

EXPECT <<EOF
${tempfile}-send.java:27: exception [java.lang.Exception, A2] must be caught or declared to be thrown.
	    throw ea2;
            ^
EOF

RUNTEST $command 
ENDTEST

TESTNAME "${mainname} (CompoundInDeclaration)"
SEND <<EOF

interface A1 { 
 void a1();
}

interface A2 { 
 void a2();
}
interface A3 extends [ A1, A2 ] { 
}

class CompoundInDeclaration implements [ A1, A2 ]
{
    void dummy()
    {
        A1 a1 = this;
        A2 a2 = this;
        this.a1();
        this.a2();
        A3 a3 = this; // error
    }
    public void a1() {} 
    public void a2() {} 
}

EOF

EXPECT <<EOF
${tempfile}-send.java:20: incompatible types
  found   : CompoundInDeclaration
  required: A3
        A3 a3 = this; // error
                ^
EOF

RUNTEST $command 
ENDTEST


TESTNAME "${mainname} (Nested)"
SEND <<EOF

interface A1 { 
}

interface A2 { 
}
interface A3 { 
}

class TestNested extends java.util.Vector implements A1, A2, A3
{
    void dummy()
    {
	[A1, [A2, java.util.Vector]] a = this;
	[[A1, A2], java.util.Vector] o = this;
	[[A1, A2], java.util.Vector] b = a;
	[java.util.Vector, [A1, A2, A3]] c = this;
	[[A1, A2, java.util.Vector], A3, TestNested] w; // error, 2 classes
	[[A1, A2, Object], Object] wx = this; // special case, [Object, Interface1, ...] = [Interface1, ...]
    }
}

EOF

EXPECT <<EOF
${tempfile}-send.java:18: compound types cannot contain more than one class.
	[[A1, A2, java.util.Vector], A3, TestNested] w; // error, 2 classes
        ^
EOF

RUNTEST $command 
ENDTEST

exit $failure





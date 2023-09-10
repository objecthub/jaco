#!/bin/bash 
#
# Run the second set of checks
#
# CHECK 2: semantic analyzer
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="compounds: check translator"

TESTNAME "${mainname} (General)"
SEND <<EOF
interface A1 
{
    void a1();
}

interface A2
{
    void a2();
}

interface A3
{
    void a3();
}

class Test implements A1, A2, A3
{
    public void a1() {} 
    public void a2() {}
    public void a3() {}
    [A2, A1] dummy()
    {
	[A1, A2] a1a2 = this;
	A2 justa1 = a1a2;
	/*
	[A1, A2] array[] = new [A1, A2][3];
	array[0] = this;
	array[1] = a1a2;
	*/
	justa1 = a1a2;
	a1a2.a1();
	a1a2.a2();
	if( a1a2 instanceof [A1, A2, A3] )
	   justa1 = a1a2;
	return a1a2;
    }
}

EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   [A2, A1] dummy() {
      [A1, A2] a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      a1a2.a1();
      a1a2.a2();
      if (a1a2 instanceof [A1, A2, A3]) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   A2 dummy() {
      A2 a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      ((A1)a1a2).a1();
      a1a2.a2();
      if (a1a2 instanceof A3 && a1a2 instanceof A2 && a1a2 instanceof A1) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   A2 dummy() {
      A2 a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      ((A1)a1a2).a1();
      a1a2.a2();
      if (a1a2 instanceof A3 && a1a2 instanceof A2 && a1a2 instanceof A1) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
EOF


RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (BestMatch)"
SEND <<EOF
interface A1 {
    void moreThanOnce(Object o);
}
interface A2 {
    void moreThanOnce(A2 o);
}
class TestBestMatch 
{
    void dummy([A1, A2] a1a2)
    {
	a1a2.moreThanOnce(this);
	a1a2.moreThanOnce(a1a2);
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A1 {
   void moreThanOnce(Object o);
}
interface A2 {
   void moreThanOnce(A2 o);
}
class TestBestMatch {
   void dummy([A1, A2] a1a2) {
      a1a2.moreThanOnce(this);
      a1a2.moreThanOnce(a1a2);
   }
   TestBestMatch() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void moreThanOnce(Object o);
}
interface A2 {
   void moreThanOnce(A2 o);
}
class TestBestMatch {
   void dummy\$cj\$XPLA1\$LA2\$\$XV(A2 a1a2) {
      ((A1)a1a2).moreThanOnce(this);
      a1a2.moreThanOnce(a1a2);
   }
   TestBestMatch() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void moreThanOnce(Object o);
}
interface A2 {
   void moreThanOnce(A2 o);
}
class TestBestMatch {
   void dummy\$cj\$XPLA1\$LA2\$\$XV(A2 a1a2) {
      ((A1)a1a2).moreThanOnce(this);
      a1a2.moreThanOnce(a1a2);
   }
   TestBestMatch() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
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
    void dummy(A3 a3)
    {
        A1 a1 = this;
        A2 a2 = this;
        this.a1();
        this.a2();
	a3.a1();
	a3.a2();
	a1 = a3;
	a2 = a3;
    }
    public void a1() {} 
    public void a2() {} 
}

EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 extends [A1, A2] {
}
class CompoundInDeclaration implements [A1, A2] {
   void dummy(A3 a3) {
      A1 a1 = this;
      A2 a2 = this;
      this.a1();
      this.a2();
      a3.a1();
      a3.a2();
      a1 = a3;
      a2 = a3;
   }
   public void a1() {
   }
   public void a2() {
   }
   CompoundInDeclaration() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 extends A2, A1 {
}
class CompoundInDeclaration implements A2, A1 {
   void dummy(A3 a3) {
      A1 a1 = this;
      A2 a2 = this;
      this.a1();
      this.a2();
      a3.a1();
      a3.a2();
      a1 = a3;
      a2 = a3;
   }
   public void a1() {
   }
   public void a2() {
   }
   CompoundInDeclaration() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 extends A2, A1 {
}
class CompoundInDeclaration implements A2, A1 {
   void dummy(A3 a3) {
      A1 a1 = this;
      A2 a2 = this;
      this.a1();
      this.a2();
      a3.a1();
      a3.a2();
      a1 = a3;
      a2 = a3;
   }
   public void a1() {
   }
   public void a2() {
   }
   CompoundInDeclaration() {
      super();
   }
}
EOF

RUNTEST $command  -debug semantic@2 
ENDTEST


TESTNAME "${mainname} (Nested)"
SEND <<EOF
interface A1 
{
    void a1();
}

interface A2
{
    void a2();
}

interface A3
{
    void a3();
}

class Test implements A1, A2, A3
{
    public void a1() {} 
    public void a2() {}
    public void a3() {}
    [A2, [A1]] dummy()
    {
	[[A1], A2] a1a2 = this;
	A2 justa1 = a1a2;
	justa1 = a1a2;
	a1a2.a1();
	a1a2.a2();
	if( a1a2 instanceof [A1, [A2, A3]] )
	   justa1 = a1a2;
	return a1a2;
    }
}

EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   [A2, [A1]] dummy() {
      [[A1], A2] a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      a1a2.a1();
      a1a2.a2();
      if (a1a2 instanceof [A1, [A2, A3]]) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   A2 dummy() {
      A2 a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      ((A1)a1a2).a1();
      a1a2.a2();
      if (a1a2 instanceof A3 && a1a2 instanceof A2 && a1a2 instanceof A1) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
   void a1();
}
interface A2 {
   void a2();
}
interface A3 {
   void a3();
}
class Test implements A1, A2, A3 {
   public void a1() {
   }
   public void a2() {
   }
   public void a3() {
   }
   A2 dummy() {
      A2 a1a2 = this;
      A2 justa1 = a1a2;
      justa1 = a1a2;
      ((A1)a1a2).a1();
      a1a2.a2();
      if (a1a2 instanceof A3 && a1a2 instanceof A2 && a1a2 instanceof A1) 
         justa1 = a1a2;
      return a1a2;
   }
   Test() {
      super();
   }
}
EOF


RUNTEST $command -debug semantic@2
ENDTEST




TESTNAME "${mainname} (Exceptions1)"
SEND <<EOF
import java.io.*;
interface A1 {
 void a1();
 Object obj();
}
interface A2 {
}
interface A3
{
}

class Test 
{
    void boo() throws Throwable, IOException, EOFException
    {
    }

    void dummy() throws Throwable
    {
	try
	    {
		boo();
	    }
	catch([NullPointerException, A1] npea1)
	    {
		System.out.println("npea1");
	    }
	catch([EOFException, A2] iieo)
	    {
		System.out.println("ioe");
		try
		{
		  boo();
		}
		catch([Exception, A1] ea1)
                {
		  ea1.a1();
		  if(ea1.obj() instanceof [Exception, A1])
		    ea1.printStackTrace();
                }

	    }
	catch(NumberFormatException nfe)
	    {
		System.out.println("nfe");
	    }
	catch(NullPointerException npe)
	    {
		System.out.println("npe");
	    }
	catch(IOException ioe)
	    {
		System.out.println("ioe");
	    }
	catch(RuntimeException rte)
	    {
		System.out.println("rte");
	    }
	catch([Exception, A1] ea1)
	    {
		System.out.println("ea1");
		}

    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
   void a1();
   Object obj();
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      try {
         boo();
      }  catch ([NullPointerException, A1] npea1) {
         System.out.println("npea1");
      } catch ([EOFException, A2] iieo) {
         System.out.println("ioe");
         try {
            boo();
         }  catch ([Exception, A1] ea1) {
            ea1.a1();
            if (ea1.obj() instanceof [Exception, A1]) 
               ea1.printStackTrace();
         }
      } catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch (NullPointerException npe) {
         System.out.println("npe");
      } catch (IOException ioe) {
         System.out.println("ioe");
      } catch (RuntimeException rte) {
         System.out.println("rte");
      } catch ([Exception, A1] ea1) {
         System.out.println("ea1");
      }
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
   void a1();
   Object obj();
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      java.lang.Object __cjava_t\$0;
      try {
         boo();
      }  catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch (java.lang.NullPointerException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A1) {
            java.lang.NullPointerException npea1 = __cjava_t\$1;
            System.out.println("npea1");
         } else {
            java.lang.NullPointerException npe = __cjava_t\$1;
            System.out.println("npe");
         }
      } catch (java.io.IOException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.io.EOFException && __cjava_t\$1 instanceof A2) {
            java.io.EOFException iieo = (java.io.EOFException)__cjava_t\$1;
            System.out.println("ioe");
            try {
               boo();
            }  catch (java.lang.Exception __cjava_t\$2) {
               if (__cjava_t\$2 instanceof A1) {
                  java.lang.Exception ea1 = __cjava_t\$2;
                  ((A1)ea1).a1();
                  if ((__cjava_t\$0 = ((A1)ea1).obj()) instanceof java.lang.Exception && __cjava_t\$0 instanceof A1) 
                     ea1.printStackTrace();
               } else 
                  throw __cjava_t\$2;
            }
         } else {
            java.io.IOException ioe = __cjava_t\$1;
            System.out.println("ioe");
         }
      } catch (RuntimeException rte) {
         System.out.println("rte");
      } catch (java.lang.Exception __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A1) {
            java.lang.Exception ea1 = __cjava_t\$1;
            System.out.println("ea1");
         } else 
            throw __cjava_t\$1;
      }
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
   void a1();
   Object obj();
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      java.lang.Object __cjava_t\$0;
      try {
         boo();
      }  catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch (java.lang.NullPointerException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A1) {
            java.lang.NullPointerException npea1 = __cjava_t\$1;
            System.out.println("npea1");
         } else {
            java.lang.NullPointerException npe = __cjava_t\$1;
            System.out.println("npe");
         }
      } catch (java.io.IOException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.io.EOFException && __cjava_t\$1 instanceof A2) {
            java.io.EOFException iieo = (java.io.EOFException)__cjava_t\$1;
            System.out.println("ioe");
            try {
               boo();
            }  catch (java.lang.Exception __cjava_t\$2) {
               if (__cjava_t\$2 instanceof A1) {
                  java.lang.Exception ea1 = __cjava_t\$2;
                  ((A1)ea1).a1();
                  if ((__cjava_t\$0 = ((A1)ea1).obj()) instanceof java.lang.Exception && __cjava_t\$0 instanceof A1) 
                     ea1.printStackTrace();
               } else 
                  throw __cjava_t\$2;
            }
         } else {
            java.io.IOException ioe = __cjava_t\$1;
            System.out.println("ioe");
         }
      } catch (RuntimeException rte) {
         System.out.println("rte");
      } catch (java.lang.Exception __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A1) {
            java.lang.Exception ea1 = __cjava_t\$1;
            System.out.println("ea1");
         } else 
            throw __cjava_t\$1;
      }
   }
   Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST


TESTNAME "${mainname} (Exceptions2)"
SEND <<EOF
import java.io.*;
interface A1 
{
}

interface A2
{
}

interface A3
{
}

class Test 
{
    void boo() throws Throwable, IOException, EOFException
    {
    }

    void dummy() throws Throwable
    {
	try
	    {
		boo();
	    }
	catch(NumberFormatException nfe)
	    {
		System.out.println("nfe");
	    }
	catch([NullPointerException, A1] npea1)
	    {
		System.out.println("npea1");
	    }
	catch([EOFException, A2] iieo)
	    {
		System.out.println("ioe");
	    }
	catch([IOException, A1] ioe)
	    {
		System.out.println("ioe");
	    }
	catch(RuntimeException rte)
	    {
		System.out.println("rte");
	    }
	catch([Throwable, A1] ea1)
	    {
		System.out.println("ea1");
		}

    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      try {
         boo();
      }  catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch ([NullPointerException, A1] npea1) {
         System.out.println("npea1");
      } catch ([EOFException, A2] iieo) {
         System.out.println("ioe");
      } catch ([IOException, A1] ioe) {
         System.out.println("ioe");
      } catch (RuntimeException rte) {
         System.out.println("rte");
      } catch ([Throwable, A1] ea1) {
         System.out.println("ea1");
      }
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      try {
         boo();
      }  catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch (java.lang.RuntimeException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.lang.NullPointerException && __cjava_t\$1 instanceof A1) {
            java.lang.NullPointerException npea1 = (java.lang.NullPointerException)__cjava_t\$1;
            System.out.println("npea1");
         } else {
            java.lang.RuntimeException rte = __cjava_t\$1;
            System.out.println("rte");
         }
      } catch (java.lang.Throwable __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.io.EOFException && __cjava_t\$1 instanceof A2) {
            java.io.EOFException iieo = (java.io.EOFException)__cjava_t\$1;
            System.out.println("ioe");
         } else 
            if (__cjava_t\$1 instanceof java.io.IOException && __cjava_t\$1 instanceof A1) {
               java.io.IOException ioe = (java.io.IOException)__cjava_t\$1;
               System.out.println("ioe");
            } else 
               if (__cjava_t\$1 instanceof A1) {
                  java.lang.Throwable ea1 = __cjava_t\$1;
                  System.out.println("ea1");
               } else 
                  throw __cjava_t\$1;
      }
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.io.*;
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test {
   void boo() throws Throwable, IOException, EOFException {
   }
   void dummy() throws Throwable {
      try {
         boo();
      }  catch (NumberFormatException nfe) {
         System.out.println("nfe");
      } catch (java.lang.RuntimeException __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.lang.NullPointerException && __cjava_t\$1 instanceof A1) {
            java.lang.NullPointerException npea1 = (java.lang.NullPointerException)__cjava_t\$1;
            System.out.println("npea1");
         } else {
            java.lang.RuntimeException rte = __cjava_t\$1;
            System.out.println("rte");
         }
      } catch (java.lang.Throwable __cjava_t\$1) {
         if (__cjava_t\$1 instanceof java.io.EOFException && __cjava_t\$1 instanceof A2) {
            java.io.EOFException iieo = (java.io.EOFException)__cjava_t\$1;
            System.out.println("ioe");
         } else 
            if (__cjava_t\$1 instanceof java.io.IOException && __cjava_t\$1 instanceof A1) {
               java.io.IOException ioe = (java.io.IOException)__cjava_t\$1;
               System.out.println("ioe");
            } else 
               if (__cjava_t\$1 instanceof A1) {
                  java.lang.Throwable ea1 = __cjava_t\$1;
                  System.out.println("ea1");
               } else 
                  throw __cjava_t\$1;
      }
   }
   Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST


TESTNAME "${mainname} (InstanceOf)"
SEND <<EOF
interface A1  {}
interface A2  {} 
interface A3  {}
class Test implements A1, A2, A3
{
    Object toto;
    boolean dummy(Test o)
    {
	boolean retval = w(o) instanceof [A1, A2, A3];
	retval = o instanceof [Test, A1, A2] || retval;
	retval = toto instanceof [Test, A1, A2] || retval;
	retval = w(this) instanceof [A1, A2] || retval;
	return retval;
    }
    Object w(Object a)
    {
	return a;
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test implements A1, A2, A3 {
   Object toto;
   boolean dummy(Test o) {
      boolean retval = w(o) instanceof [A1, A2, A3];
      retval = o instanceof [Test, A1, A2] || retval;
      retval = toto instanceof [Test, A1, A2] || retval;
      retval = w(this) instanceof [A1, A2] || retval;
      return retval;
   }
   Object w(Object a) {
      return a;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test implements A1, A2, A3 {
   Object toto;
   boolean dummy(Test o) {
      java.lang.Object __cjava_t\$0;
      boolean retval = (__cjava_t\$0 = w(o)) instanceof A3 && __cjava_t\$0 instanceof A2 && __cjava_t\$0 instanceof A1;
      retval = o instanceof Test && o instanceof A2 && o instanceof A1 || retval;
      retval = (__cjava_t\$0 = toto) instanceof Test && __cjava_t\$0 instanceof A2 && __cjava_t\$0 instanceof A1 || retval;
      retval = (__cjava_t\$0 = w(this)) instanceof A2 && __cjava_t\$0 instanceof A1 || retval;
      return retval;
   }
   Object w(Object a) {
      return a;
   }
   Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A1 {
}
interface A2 {
}
interface A3 {
}
class Test implements A1, A2, A3 {
   Object toto;
   boolean dummy(Test o) {
      java.lang.Object __cjava_t\$0;
      boolean retval = (__cjava_t\$0 = w(o)) instanceof A3 && __cjava_t\$0 instanceof A2 && __cjava_t\$0 instanceof A1;
      retval = o instanceof Test && o instanceof A2 && o instanceof A1 || retval;
      retval = (__cjava_t\$0 = toto) instanceof Test && __cjava_t\$0 instanceof A2 && __cjava_t\$0 instanceof A1 || retval;
      retval = (__cjava_t\$0 = w(this)) instanceof A2 && __cjava_t\$0 instanceof A1 || retval;
      return retval;
   }
   Object w(Object a) {
      return a;
   }
   Test() {
      super();
   }
}
EOF


RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (MethodArguments)"
SEND <<EOF
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   Object toto;
   public void dummy([A1, A2] a1a2, [A2, A3] a2a3) {
   }
   public void d() {
      [A1, A2, A3] a1a2a3 = this;
      this.dummy(a1a2a3, a1a2a3);
   }
   public static void main(String[] argv) {
   }
   public Test() {
      super();
   }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   Object toto;
   public void dummy([A1, A2] a1a2, [A2, A3] a2a3) {
   }
   public void d() {
      [A1, A2, A3] a1a2a3 = this;
      this.dummy(a1a2a3, a1a2a3);
   }
   public static void main(String[] argv) {
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   Object toto;
   public void dummy\$cj\$XPLA1\$LA2\$\$PLA2\$LA3\$\$XV(A2 a1a2, A3 a2a3) {
   }
   public void d() {
      A3 a1a2a3 = this;
      this.dummy\$cj\$XPLA1\$LA2\$\$PLA2\$LA3\$\$XV((A2)a1a2a3, a1a2a3);
   }
   public static void main(String[] argv) {
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   Object toto;
   public void dummy\$cj\$XPLA1\$LA2\$\$PLA2\$LA3\$\$XV(A2 a1a2, A3 a2a3) {
   }
   public void d() {
      A3 a1a2a3 = this;
      this.dummy\$cj\$XPLA1\$LA2\$\$PLA2\$LA3\$\$XV((A2)a1a2a3, a1a2a3);
   }
   public static void main(String[] argv) {
   }
   public Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2


TESTNAME "${mainname} (OneLevelArrays)"
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
	[A1, A2, A3][] a = new Test[3];
	[A1, A3][] b = a;
	Object[] c = b;
	[A1, A2][] d = ([A1, A2, A3][])a;
	return a;
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   [A1, A2][] dummy() {
      [A1, A2, A3][] a = new Test[3];
      [A1, A3][] b = a;
      Object[] c = b;
      [A1, A2][] d = ([A1, A2, A3][])a;
      return a;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   A2[] dummy() {
      A3[] a = new Test[3];
      A3[] b = a;
      Object[] c = b;
      A2[] d = (A2[])((A3[])((A2[])((A1[])a)));
      return (A2[])a;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   A2[] dummy() {
      A3[] a = new Test[3];
      A3[] b = a;
      Object[] c = b;
      A2[] d = (A2[])((A3[])((A2[])((A1[])a)));
      return (A2[])a;
   }
   public Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (DeepArrays)"
SEND <<EOF
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
    [A1, A2][][] dummy()
    {
	[A1, A2, A3][][] a = null;
	[A1, A3][][] b = a;
	Object[] c = b;
	[A1, A2][][] d = ([A1, A2, A3][][])a;
	return a;
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   [A1, A2][][] dummy() {
      [A1, A2, A3][][] a = null;
      [A1, A3][][] b = a;
      Object[] c = b;
      [A1, A2][][] d = ([A1, A2, A3][][])a;
      return a;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   A2[][] dummy() {
      A3[][] a = null;
      A3[][] b = a;
      Object[] c = b;
      A2[][] d = (A2[][])((A3[][])((A2[][])((A1[][])a)));
      return (A2[][])a;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
public interface A1 {
}
public interface A2 {
}
public interface A3 {
}
public class Test implements A1, A2, A3 {
   A2[][] dummy() {
      A3[][] a = null;
      A3[][] b = a;
      Object[] c = b;
      A2[][] d = (A2[][])((A3[][])((A2[][])((A1[][])a)));
      return (A2[][])a;
   }
   public Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (Import)"

cat >${tempfile}-setup.java <<EOF
import java.io.*;
interface A{ }
interface B{  }
interface C{ }

public class Test 
{
    public A a;
    public [A, B] ab;
    public [B, C] bc;
    public Test()
    { 
    }
    void dummy(C c) 
    {
    }
    void dummy([A, C, B] ab)
    {
	if(ab!=null)
	    dummy(ab);
	else
	    dummy((C)ab);
    }

    void dummyAgain([Exception, Serializable] toto) {}
    void dummyAgain(Exception toto) {}
    void dummyAgain([Exception, Serializable, A] toto) {}

    void dummy2()
    {
    }
    void dummy2([B, C] b) throws [Exception, A]
    {
    }
    void dummy2([C, A] a)
    {
    }

    [A, B] returnAB() { return null; }
}
EOF


SEND <<EOF
import java.lang.Exception;
public class Test2 
{
    void dummy() 
   {
	Test test = new Test();
	[A, C, B] abc = null;
	test.a = abc;
	test.ab = abc;
	test.bc = abc;
	[A, B] ab2 = test.ab;
	[B, C] bc2 = test.bc;
	C c = abc;
	test.dummy(abc);
	test.dummy(c);
	test.dummyAgain(new Exception());
	[A, B] ohoh = test.returnAB();
	try
	{
	  test.dummy2(bc2);
	}
	catch([Exception, A] a)
        {
        }
   }
}
EOF

EXPECT <<EOF
////// ${tempfile}-setup.java //////
import java.io.*;

interface A {
}
interface B {
}
interface C {
}
public class Test {
   public A a;
   public [A, B] ab;
   public [B, C] bc;
   public Test() {
      super();
   }
   void dummy(C c) {
   }
   void dummy([A, C, B] ab) {
      if (ab != null) 
         dummy(ab);
      else 
         dummy((C)ab);
   }
   void dummyAgain([Exception, Serializable] toto) {
   }
   void dummyAgain(Exception toto) {
   }
   void dummyAgain([Exception, Serializable, A] toto) {
   }
   void dummy2() {
   }
   void dummy2([B, C] b) throws [Exception, A] {
   }
   void dummy2([C, A] a) {
   }
   [A, B] returnAB() {
      return null;
   }
}
////// ${tempfile}-setup.java //////
import java.io.*;

interface A {
}
interface B {
}
interface C {
}
public class Test {
   public A a;
   public B ab;
   public C bc;
   public Test() {
      super();
   }
   void dummy(C c) {
   }
   void dummy\$cj\$XPLA\$LB\$LC\$\$XV(C ab) {
      if (ab != null) 
         dummy\$cj\$XPLA\$LB\$LC\$\$XV(ab);
      else 
         dummy((C)ab);
   }
   void dummyAgain\$cj\$XPLjava_lang_Exception\$Ljava_io_Serializable\$\$XV(java.lang.Exception toto) {
   }
   void dummyAgain(Exception toto) {
   }
   void dummyAgain\$cj\$XPLjava_lang_Exception\$LA\$Ljava_io_Serializable\$\$XV(java.lang.Exception toto) {
   }
   void dummy2() {
   }
   void dummy2\$cj\$XPLB\$LC\$\$XV(C b) throws java.lang.Exception {
   }
   void dummy2\$cj\$XPLA\$LC\$\$XV(C a) {
   }
   B returnAB() {
      return null;
   }
}
////// ${tempfile}-setup.java //////
import java.io.*;

interface A {
}
interface B {
}
interface C {
}
public class Test {
   public A a;
   public B ab;
   public C bc;
   public Test() {
      super();
   }
   void dummy(C c) {
   }
   void dummy\$cj\$XPLA\$LB\$LC\$\$XV(C ab) {
      if (ab != null) 
         dummy\$cj\$XPLA\$LB\$LC\$\$XV(ab);
      else 
         dummy((C)ab);
   }
   void dummyAgain\$cj\$XPLjava_lang_Exception\$Ljava_io_Serializable\$\$XV(java.lang.Exception toto) {
   }
   void dummyAgain(Exception toto) {
   }
   void dummyAgain\$cj\$XPLjava_lang_Exception\$LA\$Ljava_io_Serializable\$\$XV(java.lang.Exception toto) {
   }
   void dummy2() {
   }
   void dummy2\$cj\$XPLB\$LC\$\$XV(C b) throws java.lang.Exception {
   }
   void dummy2\$cj\$XPLA\$LC\$\$XV(C a) {
   }
   B returnAB() {
      return null;
   }
}
////// ${tempfile}-send.java //////
import java.lang.Exception;

public class Test2 {
   void dummy() {
      Test test = new Test();
      [A, C, B] abc = null;
      test.a = abc;
      test.ab = abc;
      test.bc = abc;
      [A, B] ab2 = test.ab;
      [B, C] bc2 = test.bc;
      C c = abc;
      test.dummy(abc);
      test.dummy(c);
      test.dummyAgain(new Exception());
      [A, B] ohoh = test.returnAB();
      try {
         test.dummy2(bc2);
      }  catch ([Exception, A] a) {
      }
   }
   public Test2() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.lang.Exception;

public class Test2 {
   void dummy() {
      Test test = new Test();
      C abc = null;
      test.a = (A)abc;
      test.ab = (B)abc;
      test.bc = abc;
      B ab2 = test.ab;
      C bc2 = test.bc;
      C c = abc;
      test.dummy\$cj\$XPLA\$LB\$LC\$\$XV(abc);
      test.dummy(c);
      test.dummyAgain(new Exception());
      B ohoh = test.returnAB();
      try {
         test.dummy2\$cj\$XPLB\$LC\$\$XV(bc2);
      }  catch (java.lang.Exception __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A) {
            java.lang.Exception a = __cjava_t\$1;
         } else 
            throw __cjava_t\$1;
      }
   }
   public Test2() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.lang.Exception;

public class Test2 {
   void dummy() {
      Test test = new Test();
      C abc = null;
      test.a = (A)abc;
      test.ab = (B)abc;
      test.bc = abc;
      B ab2 = test.ab;
      C bc2 = test.bc;
      C c = abc;
      test.dummy\$cj\$XPLA\$LB\$LC\$\$XV(abc);
      test.dummy(c);
      test.dummyAgain(new Exception());
      B ohoh = test.returnAB();
      try {
         test.dummy2\$cj\$XPLB\$LC\$\$XV(bc2);
      }  catch (java.lang.Exception __cjava_t\$1) {
         if (__cjava_t\$1 instanceof A) {
            java.lang.Exception a = __cjava_t\$1;
         } else 
            throw __cjava_t\$1;
      }
   }
   public Test2() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2 ${tempfile}-setup.java ';' $command -debug semantic@2



exit $failure





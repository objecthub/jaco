#!/bin/bash 
#
# 
# CHECK 6: translator (for Aliases)
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="alias: check translator"

TESTNAME "${mainname} (Basic)"
SEND <<EOF
import java.util.*;
public interface A1 
{
    void a1();
}
public interface A2 
{
    void a2();
}
public interface A3 
{
    void a3();
}

public class AliasA2 = A2;
public class AliasA2C = [A2];
public interface AliasA1 = A1;
public interface AliasA1A2 = [A1, A2];

public class BasicCheck6
{
    public class AliasA3 = A3;
    public interface AliasA3_2 = AliasA3;
    public class AliasA2_2 = AliasA2;
    class AliasA3_3 = AliasA3;

    public class AliasStringTok = StringTokenizer;

    void dummy(AliasA1A2 a2_1)
    {
        AliasA2_2 a2_2 = a2_1;
	AliasA2C a2_3 = a2_1;
	AliasA2 a2_4 = (A2)this;
	a2_1.a1();
	a2_1.a2();
	((AliasA3_2)a2_1).a3();

	if(a2_1 instanceof AliasA1A2)
	  a2_1.a1();

	if(a2_1 instanceof AliasA3_2)
	   a2_1.a2();
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
import java.util.*;

public interface A1 {
   void a1();
}
public interface A2 {
   void a2();
}
public interface A3 {
   void a3();
}
public class AliasA2 = A2;
public class AliasA2C = [A2];
public class AliasA1 = A1;
public class AliasA1A2 = [A1, A2];
public class BasicCheck6 {
   public class AliasA3 = A3;
   public class AliasA3_2 = AliasA3;
   public class AliasA2_2 = AliasA2;
   class AliasA3_3 = AliasA3;
   public class AliasStringTok = StringTokenizer;
   void dummy(AliasA1A2 a2_1) {
      AliasA2_2 a2_2 = a2_1;
      AliasA2C a2_3 = a2_1;
      AliasA2 a2_4 = (A2)this;
      a2_1.a1();
      a2_1.a2();
      ((AliasA3_2)a2_1).a3();
      if (a2_1 instanceof AliasA1A2) 
         a2_1.a1();
      if (a2_1 instanceof AliasA3_2) 
         a2_1.a2();
   }
   public BasicCheck6() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.util.*;

public interface A1 {
   void a1();
}
public interface A2 {
   void a2();
}
public interface A3 {
   void a3();
}
public class AliasA2 {
   public AliasA2() {
      super();
   }
}
public class AliasA2C {
   public AliasA2C() {
      super();
   }
}
public class AliasA1 {
   public AliasA1() {
      super();
   }
}
public class AliasA1A2 {
   public AliasA1A2() {
      super();
   }
}
public class BasicCheck6 {
   public static class AliasA3 {
      public AliasA3() {
         super();
      }
   }
   public static class AliasA3_2 {
      public AliasA3_2() {
         super();
      }
   }
   public static class AliasA2_2 {
      public AliasA2_2() {
         super();
      }
   }
   static class AliasA3_3 {
      AliasA3_3() {
         super();
      }
   }
   public static class AliasStringTok {
      public AliasStringTok() {
         super();
      }
   }
   void dummy\$cj\$XPLA1\$LA2\$\$XV(A2 a2_1) {
      A2 a2_2 = a2_1;
      A2 a2_3 = a2_1;
      A2 a2_4 = (A2)this;
      ((A1)a2_1).a1();
      a2_1.a2();
      ((A3)a2_1).a3();
      if (a2_1 instanceof A2 && a2_1 instanceof A1) 
         ((A1)a2_1).a1();
      if (a2_1 instanceof A3) 
         a2_1.a2();
   }
   public BasicCheck6() {
      super();
   }
}
////// ${tempfile}-send.java //////
import java.util.*;

public interface A1 {
   void a1();
}
public interface A2 {
   void a2();
}
public interface A3 {
   void a3();
}
public class AliasA2 {
   public AliasA2() {
      super();
   }
}
public class AliasA2C {
   public AliasA2C() {
      super();
   }
}
public class AliasA1 {
   public AliasA1() {
      super();
   }
}
public class AliasA1A2 {
   public AliasA1A2() {
      super();
   }
}
public class BasicCheck6 {
   void dummy\$cj\$XPLA1\$LA2\$\$XV(A2 a2_1) {
      A2 a2_2 = a2_1;
      A2 a2_3 = a2_1;
      A2 a2_4 = (A2)this;
      ((A1)a2_1).a1();
      a2_1.a2();
      ((A3)a2_1).a3();
      if (a2_1 instanceof A2 && a2_1 instanceof A1) 
         ((A1)a2_1).a1();
      if (a2_1 instanceof A3) 
         a2_1.a2();
   }
   public BasicCheck6() {
      super();
   }
}
public class BasicCheck6\$AliasA3 {
   public BasicCheck6\$AliasA3() {
      super();
   }
}
public class BasicCheck6\$AliasA3_2 {
   public BasicCheck6\$AliasA3_2() {
      super();
   }
}
public class BasicCheck6\$AliasA2_2 {
   public BasicCheck6\$AliasA2_2() {
      super();
   }
}
class BasicCheck6\$AliasA3_3 {
   BasicCheck6\$AliasA3_3() {
      super();
   }
}
public class BasicCheck6\$AliasStringTok {
   public BasicCheck6\$AliasStringTok() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (LocalAlias)"
SEND <<EOF
interface A{ void a();}
interface B{ void dummy(); }
interface C{ void c();}
public class Test implements A, B, C
{
    public void a(){}
    public void c(){}
    public void dummy()
    {
	
        class AB = [A, B], CA = [C, A];
	int AB = 0;
	AB ab = this;

	ab.dummy();

	class ABC = [AB, C], BC = [B, C], A2 = A; 
	ABC abc = this;
	BC bc = abc;
	abc.a();
	abc.c();
	bc.c();
	bc.dummy();
	A2 a = abc;
	a.a();
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A {
   void a();
}
interface B {
   void dummy();
}
interface C {
   void c();
}
public class Test implements A, B, C {
   public void a() {
   }
   public void c() {
   }
   public void dummy() {
      class AB = [A, B];
      class CA = [C, A];
      int AB = 0;
      AB ab = this;
      ab.dummy();
      class ABC = [AB, C];
      class BC = [B, C];
      class A2 = A;
      ABC abc = this;
      BC bc = abc;
      abc.a();
      abc.c();
      bc.c();
      bc.dummy();
      A2 a = abc;
      a.a();
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A {
   void a();
}
interface B {
   void dummy();
}
interface C {
   void c();
}
public class Test implements A, B, C {
   public void a() {
   }
   public void c() {
   }
   public void dummy() {
      int AB = 0;
      B ab = this;
      ab.dummy();
      C abc = this;
      C bc = abc;
      ((A)abc).a();
      abc.c();
      bc.c();
      ((B)bc).dummy();
      A a = (A)abc;
      a.a();
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A {
   void a();
}
interface B {
   void dummy();
}
interface C {
   void c();
}
public class Test implements A, B, C {
   public void a() {
   }
   public void c() {
   }
   public void dummy() {
      int AB = 0;
      B ab = this;
      ab.dummy();
      C abc = this;
      C bc = abc;
      ((A)abc).a();
      abc.c();
      bc.c();
      ((B)bc).dummy();
      A a = (A)abc;
      a.a();
   }
   public Test() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (AliasesUsedOutOfOrder)"
SEND <<EOF
interface A{ void a();}
public interface B{ void dummy(); }
interface C{ void c();}

class aA = [aB];
public class aB = B;
public class Test 
{
    class bA = aA;
    class bB = C;
    [B] dummy()
    {
          bA w = null; // bA->aA->[aB], aB->B => bA=[B]
	  return w;
    }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
interface A {
   void a();
}
public interface B {
   void dummy();
}
interface C {
   void c();
}
class aA = [aB];
public class aB = B;
public class Test {
   class bA = aA;
   class bB = C;
   [B] dummy() {
      bA w = null;
      return w;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A {
   void a();
}
public interface B {
   void dummy();
}
interface C {
   void c();
}
class aA {
   aA() {
      super();
   }
}
public class aB {
   public aB() {
      super();
   }
}
public class Test {
   static class bA {
      bA() {
         super();
      }
   }
   static class bB {
      bB() {
         super();
      }
   }
   B dummy() {
      B w = null;
      return w;
   }
   public Test() {
      super();
   }
}
////// ${tempfile}-send.java //////
interface A {
   void a();
}
public interface B {
   void dummy();
}
interface C {
   void c();
}
class aA {
   aA() {
      super();
   }
}
public class aB {
   public aB() {
      super();
   }
}
public class Test {
   B dummy() {
      B w = null;
      return w;
   }
   public Test() {
      super();
   }
}
class Test\$bA {
   Test\$bA() {
      super();
   }
}
class Test\$bB {
   Test\$bB() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST


exit $failure


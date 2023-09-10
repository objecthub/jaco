#!/bin/bash 
#
# 
# CHECK 5: semantic analyzer (for Aliases)
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="alias: check semantic analyzer"

# global aliases, inner aliases & indirections
TESTNAME "${mainname} (BasicAliases)"
SEND <<EOF
import java.util.*;
public interface A1 {}
public interface A2 {}
public interface A3 {}

public class AliasA2 = A2;
public class AliasA2C = [A2];
public interface AliasA1 = A1;
public interface AliasA1A2 = [A1, A2];

public class BasicGrammar
{
    public class AliasA3 = A3;
    public interface AliasA3_2 = AliasA3;
    public class AliasA2_2 = AliasA2;
    public class AliasA3_3 = AliasA3;

    public class AliasStringTok = StringTokenizer;

    void dummy(AliasA1A2 a2_1)
    {
	BasicGrammar.AliasStringTok tok = new AliasStringTok("a b c d" , " ");
	StringTokenizer tok2 = tok;

	A2 a2_2 = a2_1; 
	AliasA2_2 a2_3 = a2_1;
	AliasA3_3 a3 = a2_1; // error
    }
}
EOF

EXPECT <<EOF
${tempfile}-send.java:27: incompatible types
  found   : [A1, A2]
  required: A3
	AliasA3_3 a3 = a2_1; // error
                       ^
EOF

RUNTEST $command 
ENDTEST


TESTNAME "${mainname} (Protection1)"
SEND <<EOF
interface OkA1{}
protected interface WrongA2 {} // error
private interface WrongA3 {} // error
interface PackageA{}
class ProtectedAliases
{
    interface OkA1{}
    public interface A1 {}
    protected interface A2 {}
    private interface A3 {}

    public class PublicOnPublic = A1;
    public class PublicOnPrivate = A3; // should issue error
    protected class ProtectedOnPublic = A1;
    public class PublicOnPackage = PackageA; // error
}
EOF

EXPECT <<EOF
${tempfile}-send.java:2: modifier 'protected' not allowed here.
protected interface WrongA2 {} // error
^
${tempfile}-send.java:3: modifier 'private' not allowed here.
private interface WrongA3 {} // error
^
${tempfile}-send.java:13: cannot make an alias to ProtectedAliases\$A3 with access privileges weaker than 'package'.
    public class PublicOnPrivate = A3; // should issue error
                 ^
${tempfile}-send.java:15: cannot make an alias to PackageA with access privileges weaker than 'package'.
    public class PublicOnPackage = PackageA; // error
                 ^
EOF

RUNTEST $command 
ENDTEST

TESTNAME "${mainname} (Protection2)"
SEND <<EOF
interface I_package{}
public interface I_public{}
class alias_package_on_public = I_public;
class alias_package_on_package = I_package;
class ProtectedAliases
{
    interface I_inner_package{}
    public interface I_inner_public{}
    protected interface I_inner_protected{}
    private interface I_inner_private{}

    class alias_inner_package_on_package = I_inner_package;
    public class alias_public_on_public = I_inner_public;
    public class alias_public_on_outer_public = I_public;
    private class alias_private_on_outer_public = I_public;
    private class alias_private_on_public = I_inner_public;
    protected class alias_protected_on_public = I_inner_public;
    private class alias_private_on_protected = I_inner_protected;
    private class alias_private_on_private = I_inner_private;
    protected class alias_protected_on_protected = I_inner_protected;

    void dummy()
    {
	    alias_private_on_private a;
	    alias_private_on_protected b;
	    alias_protected_on_public c;
	    alias_private_on_public d;
	    alias_inner_package_on_package e;
	    alias_public_on_public f;
	    alias_public_on_outer_public g;
	    alias_private_on_outer_public h;
    }
}
public class ProtectedAliasesE extends ProtectedAliases
{

    void dummy()
	{
	    alias_private_on_private a; // error
	    alias_private_on_protected b; // error
	    alias_protected_on_public c; // ok
	    alias_protected_on_protected d; // ok
	    alias_private_on_public e; // error 
	    alias_inner_package_on_package f; // ok
	    alias_public_on_public g; // ok
	    alias_public_on_outer_public h; // ok
	    alias_private_on_outer_public i; // error
	}
}
public class Another
{
    void dummy()
	{

	    ProtectedAliases.alias_private_on_private a; // error
	    ProtectedAliases.alias_private_on_protected b; // error
	    ProtectedAliases.alias_private_on_public e; // error 
	    ProtectedAliases.alias_private_on_outer_public i; // error

	    /*
	    ProtectedAliasesE.alias_protected_on_public c; // error
	    ProtectedAliasesE.alias_protected_on_protected d; // error
	    */

	    ProtectedAliases.alias_inner_package_on_package f; // ok
	    ProtectedAliases.alias_public_on_public g; // ok
	    ProtectedAliases.alias_public_on_outer_public h; // ok
	}

}
EOF

EXPECT <<EOF
${tempfile}-send.java:39: ProtectedAliases\$alias_private_on_private has private access in ProtectedAliases
	    alias_private_on_private a; // error
            ^
${tempfile}-send.java:40: ProtectedAliases\$alias_private_on_protected has private access in ProtectedAliases
	    alias_private_on_protected b; // error
            ^
${tempfile}-send.java:43: ProtectedAliases\$alias_private_on_public has private access in ProtectedAliases
	    alias_private_on_public e; // error 
            ^
${tempfile}-send.java:47: ProtectedAliases\$alias_private_on_outer_public has private access in ProtectedAliases
	    alias_private_on_outer_public i; // error
            ^
${tempfile}-send.java:55: ProtectedAliases\$alias_private_on_private has private access in ProtectedAliases
	    ProtectedAliases.alias_private_on_private a; // error
            ^
${tempfile}-send.java:56: ProtectedAliases\$alias_private_on_protected has private access in ProtectedAliases
	    ProtectedAliases.alias_private_on_protected b; // error
            ^
${tempfile}-send.java:57: ProtectedAliases\$alias_private_on_public has private access in ProtectedAliases
	    ProtectedAliases.alias_private_on_public e; // error 
            ^
${tempfile}-send.java:58: ProtectedAliases\$alias_private_on_outer_public has private access in ProtectedAliases
	    ProtectedAliases.alias_private_on_outer_public i; // error
            ^
EOF

RUNTEST $command 
ENDTEST

TESTNAME "${mainname} (Protection3)"
SEND <<EOF
interface I {}
public interface pI {}
class aliasI = [I, pI]; // ok
public class aliaspI = [I, pI]; // not ok
public class aliaspI2 = aliasI; // not ok
class aliasI2 = aliasI; // ok
public class Test
{
  private interface privI {}
  public interface pubI {}
  interface packI {}
  protected interface protI {}

  public class alias1 = [pubI];
  private class alias2 = [pubI];
  protected class alias3 = [packI]; // error
  class alias4 = [packI];
  public class alias5 = [protI]; // error
  public class alias6 = [pubI, pI, protI]; // error
  class alias7 = [pI];
  class alias8 = [aliaspI2];
}
EOF

EXPECT <<EOF
${tempfile}-send.java:4: cannot make an alias to I with access privileges weaker than 'package'.
public class aliaspI = [I, pI]; // not ok
             ^
${tempfile}-send.java:5: cannot make an alias to I with access privileges weaker than 'package'.
public class aliaspI2 = aliasI; // not ok
             ^
${tempfile}-send.java:16: cannot make an alias to Test\$packI with access privileges weaker than 'package'.
  protected class alias3 = [packI]; // error
                  ^
${tempfile}-send.java:18: cannot make an alias to Test\$protI with access privileges weaker than 'package'.
  public class alias5 = [protI]; // error
               ^
${tempfile}-send.java:19: cannot make an alias to Test\$protI with access privileges weaker than 'package'.
  public class alias6 = [pubI, pI, protI]; // error
               ^
EOF

RUNTEST $command 
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
	
        class AB = [A, B], CA = [A];
	int AB = 0;
	AB ab = this;

	ab.dummy();

	class AB = A; /* error; duplicate definition */
	public class ABC = [AB, C], BC = [B, C]; /* error; 'public' not allowed */
	ABC abc = this;
	abc.a();
	abc.c();
    }
}
EOF

EXPECT <<EOF
${tempfile}-send.java:17: duplicate definition of AB in dummy.
	class AB = A; /* error; duplicate definition */
              ^
${tempfile}-send.java:18: modifier 'public' not allowed here.
	public class ABC = [AB, C], BC = [B, C]; /* error; 'public' not allowed */
                     ^
EOF

RUNTEST $command 
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
    class bA = bB;
    class bB = C;
}
EOF

EXPECT <<EOF
EOF

RUNTEST $command 
ENDTEST



exit $failure


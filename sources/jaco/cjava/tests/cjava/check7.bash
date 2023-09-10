#!/bin/bash 
#
# 
# CHECK 7: translator & import (for Aliases)
#
source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="alias: check import"
do_setup="yes"

export CLASSPATH=.:$CLASSPATH

function SETUP
{
#not safe  [ -f fake/check7/Setup.class -a fake/check7/Setup.class -nt $0 ] && return

cat >/var/tmp/check7-$PPID.java <<EOF
package fake.check7;
public interface A1 { void a1(); }
public interface A2 { void a2(); }
public interface A3 { void a3(); }

public class aliasA1 = A1;
public class aliasA2 = A2;
class aliasA3 = A3;

public class aliasA1A2 = [aliasA1, aliasA2];

public class Setup
{
    public class aliasA1 = A1;
    public class aliasA2 = A2;
    public class aliasA3 = A3;
    protected class aliasA4 = A1;
    
    public class AliasA1A2 = [aliasA1, aliasA2];
}
EOF
$command /var/tmp/check7-$PPID.java || exit 10
}

TESTNAME "${mainname} (Import)"


SEND <<EOF
package fake.check7;
class Toto {
   boolean dummy(aliasA1A2 a1a2) {
      aliasA1 a1 = a1a2;
      aliasA2 a2 = a1a2;
      a1a2.a1();
      a1a2.a2();
      a1.a1();
      a2.a2();
      return a1 instanceof [aliasA1A2, Setup.aliasA3];
   }
   Toto() {
      super();
   }
}
EOF

EXPECT <<EOF
////// ${tempfile}-send.java //////
package fake.check7;

class Toto {
   boolean dummy(aliasA1A2 a1a2) {
      aliasA1 a1 = a1a2;
      aliasA2 a2 = a1a2;
      a1a2.a1();
      a1a2.a2();
      a1.a1();
      a2.a2();
      return a1 instanceof [aliasA1A2, Setup.aliasA3];
   }
   Toto() {
      super();
   }
}
////// ${tempfile}-send.java //////
package fake.check7;

class Toto {
   boolean dummy\$cj\$XPLfake_check7_A1\$Lfake_check7_A2\$\$XZ(fake.check7.A2 a1a2) {
      fake.check7.A1 a1 = (fake.check7.A1)a1a2;
      fake.check7.A2 a2 = a1a2;
      ((fake.check7.A1)a1a2).a1();
      a1a2.a2();
      a1.a1();
      a2.a2();
      return a1 instanceof fake.check7.A3 && a1 instanceof fake.check7.A2 && a1 instanceof fake.check7.A1;
   }
   Toto() {
      super();
   }
}
////// ${tempfile}-send.java //////
package fake.check7;

class Toto {
   boolean dummy\$cj\$XPLfake_check7_A1\$Lfake_check7_A2\$\$XZ(fake.check7.A2 a1a2) {
      fake.check7.A1 a1 = (fake.check7.A1)a1a2;
      fake.check7.A2 a2 = a1a2;
      ((fake.check7.A1)a1a2).a1();
      a1a2.a2();
      a1.a1();
      a2.a2();
      return a1 instanceof fake.check7.A3 && a1 instanceof fake.check7.A2 && a1 instanceof fake.check7.A1;
   }
   Toto() {
      super();
   }
}
EOF

RUNTEST $command -debug semantic@2
ENDTEST

TESTNAME "${mainname} (Protection2)"
SEND <<EOF
package fake.check7_2;
import fake.check7.*;
class Toto
{
    boolean dummy(aliasA1A2 a1a2)
    {
	aliasA1 a1 = a1a2;
	aliasA2 a2 = a1a2;
	Setup.aliasA4 a4 = a1; /* doesn't work; aliasA4 protected */
	return a1 instanceof [aliasA1A2, aliasA3]; /* aliasA3 not accessible; package local */
    }
}
class Toto2 extends Setup
{
    void dummy()
    {
	Setup.aliasA4 a4 = (A1)this; /* works */
    }
}
EOF

EXPECT <<EOF
${tempfile}-send.java:9: Setup\$aliasA4 has protected access in Setup
	Setup.aliasA4 a4 = a1; /* doesn't work; aliasA4 protected */
        ^
${tempfile}-send.java:10: aliasA3 is not public in check7. it cannot be accessed from outside the package.
	return a1 instanceof [aliasA1A2, aliasA3]; /* aliasA3 not accessible; package local */
                                         ^
EOF

RUNTEST $command 
ENDTEST


exit $failure


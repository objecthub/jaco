#!/bin/bash 
#
# 
# CHECK 4: grammar (for Aliases)
#

source functions.bash

PARSEARGS "$@"

command=cjava 
mainname="alias: check grammar"

TESTNAME "${mainname} (BasicGrammar)"
SEND <<EOF
interface A1 {}
interface A2 {}
interface A3 {}

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
}

x // error; stop compiler
EOF

EXPECT <<EOF
${tempfile}-send.java:18: syntax error (103)
x // error; stop compiler
^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
EOF

RUNTEST $command 
ENDTEST


TESTNAME "${mainname} (LocalAliases)"
SEND <<EOF
interface A1 {}
interface A2 {}
interface A3 {}
public class BasicGrammar
{
	void dummy()
	{
	  class A1A2 = [A1, A2];
	  class CA1 = [A1], CA2 = A2;
	  int i;
	}
}
x // error; stop compiler
EOF

EXPECT <<EOF
${tempfile}-send.java:13: syntax error (103)
x // error; stop compiler
^
${tempfile}-send.java: fatal error: Couldn't repair and continue parse
EOF

RUNTEST $command 
ENDTEST

exit $failure





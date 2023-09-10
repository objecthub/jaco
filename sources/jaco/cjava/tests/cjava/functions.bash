#
# Functions used by test scripts
#
# Only tested on bash (1.14)
#
#

###
# Flags to be changed by the calling script afterwards.
# 
# by default, it's set for java

# extension of the file passed to the command by RUNTEST
send_ext='java' 

# lines matching this regexp will be removed from the command output
ignorepattern='\(^ *$\)\|\(^.*at .*(.*)$\)\|\(^Exception in.*$\)' # ignore exception stack traces and blank lines
               
###
# Global variables
tempfile=/var/tmp/sh-$PPID
success=0
failure=0

###
# flags, set in PARSEARGS

verbose="no"
testpattern='.*'
do_setup="no"

# SETUP
#
# This function is called before a test is really
# executed. 
#
# Redefine it if you need to do something before executing 
# a test. This way, if no tests are actually run, no setup
# will be done either.
#
function SETUP {
	return
}

# PARSEARGS
#
# Set some flags, based on command-line arguments.
#
# USAGE: PARSEARGS "$@"
#
# SYNTAX: PARSEARGS [-v|-verbose|--verbose] [testname-regexp]
#         PARSEARGS [-h|--help|-help|-?]
#
function PARSEARGS {
  for i in "$@"
  do
	if [ "$i" = "-?" -o "$i" = "-h" -o "$i" = "--help" -o "$i" = "-help" ]
        then
	  echo "USAGE: $0 [-v|--verbose] [testname-regexp]"
	  exit 1
        fi

	if [ "$i" = "-v" -o "$i" = "-verbose" -o "$i" = "--verbose" ]
        then
          verbose="yes"
	  continue
        fi

        testpattern="${testpattern}${i}"
  done 
}

# ENDTEST
#
# Call this function to exit the script if
# one test failed.
#
function ENDTEST
{
    [ "$failure" -gt 0 ] && exit 1
}


# TESTNAME
#
# Set the name of the current test. 
#
# The name may be displayed to the user.
# It is also used to decide whether to run
# the test, based on the regexp set by PARSEARGS.
#
# USAGE: TESTNAME [name]
#
function TESTNAME
{
	testname="$*"
	if expr "$*" : "$testpattern" >/dev/null
	then
	 dotest=yes
	else
	 dotest=no
	fi
}

# SEND
#
# Set the content of the file to pass to the command
# run by the test.
#
# It must be called before RUNTEST
#
# EXAMPLES: SEND <filename
#           SEND <<EOF
#             ... file content ...
#            EOF
#
function SEND
{
	[ "$dotest" = "yes" ] && cat >${tempfile}-send.${send_ext}
}

# EXPECT
#
# Set what the output of the command should be.
#
# It must be called before RUNTEST.
#
# EXAMPLES: EXPECT <filename
#           EXPECT <<EOF
#             ... file content ...
#           EOF
#
function EXPECT
{
	[ "$dotest" = "yes" ] && grep -v "${ignorepattern}" >${tempfile}-expect.out
}

# VERBOSE
#
# works like echo, but only if the verbose flag is
# set by PARSEARGS
#
# USAGE: VERBOSE [-n] {strings}
#
function VERBOSE
{
  [ "$verbose" = "yes" ] && echo "$@"
}

# RUNTEST: Really run the test.
#
# SEND and EXPECT must always be called before RUNTEST.
#
# It passes the temporary file set by SEND to the command
# and runs it. The output of the command (stdout and stderr)
# is captured and compared to what was set by EXPECT.
# 
# If the output is different from what was expected, an
# error message is printed to stderr.
#
# success is incremented if the test succeeds,
# otherwise failure is incremented.
#
# USAGE: RUNTEST command {command-arguments}
#
function RUNTEST
{
     if [ "$dotest" = "no" ]
     then
        VERBOSE $testname '... skipped' 
     else
	if [ -f ${tempfile}-send.${send_ext} -a -f ${tempfile}-expect.out ]
	then
		if [ "${do_setup}" = "yes" ]
		then
	       	 	VERBOSE -n "$mainname setup"  '...'
	        	SETUP
	 		VERBOSE  'ok'
			do_setup="no"
                fi
		VERBOSE -n $testname '... '
		eval "$@" ${tempfile}-send.${send_ext} >${tempfile}-got.out 2>&1 
		if grep -v "${ignorepattern}" ${tempfile}-got.out | diff ${tempfile}-expect.out - >${tempfile}-diff.out
		then
			success=$(($success + 1))
			VERBOSE "ok"
		else
			VERBOSE "failed"
			echo "$testname: FAILED (details follow)" >&2
			failure=$(($failure + 1))

			echo >&2
			echo "COMMAND: $*" >&2
			echo "RESULT: " >&2
			cat ${tempfile}-got.out >&2
			echo "DIFF: " >&2
			cat ${tempfile}-diff.out >&2
			echo >&2
		fi
	else
		echo "runtest: call expect and send before runtest!"
		exit 10
	fi
	rm -f ${tempfile}-* >/dev/null
    fi
}

# generated makefile for jaco extension OP
# Matthias Zenger, 30/05/99

JC = java -ms24m -mx40m jaco.pizza.Main
CLASSES = ../../../classes
NEWCLASSES = ../../../classes
OPTIONS = -g

note:
	@echo ''
	@echo '---- OP -- extension of JACO'
	@echo '---- generated makefile'
	@echo '-- '
	@echo '-- make compiler        generates the compiler in ./new'
	@echo '-- make cup             updates the parser sources'
	@echo '-- make parser          compiles and installs the parser in ./classes'
	@echo '-- make resources       updates the resources in ./classes'
	@echo '-- make swap            installs the generated compiler'
	@echo ''

./grammar/Parser.tables: ./grammar/Grammar.cup
	java -ms16m -mx64m jcup.Main -package jaco.javaop.grammar -parser Parser -symbols Symbols \
         -o ./grammar/ -f ./grammar/Grammar.cup

cup: ./grammar/Parser.tables

parser: cup
	$(JC) $(OPTIONS) -d $(CLASSES) ./grammar/*.java
	cp ./grammar/Parser.tables $(CLASSES)/jaco/javaop/grammar

resources:
	cp -r ./resources $(CLASSES)/jaco/java

compiler:
	-mkdir -p $(NEWCLASSES)/jaco/javaop/component
	$(JC) $(OPTIONS) -d $(NEWCLASSES) ./struct/*.java ./component/*.java ./context/*.java ./grammar/*.java \
          ./Main.java
	cp ./grammar/Parser.tables $(NEWCLASSES)/jaco/javaop/grammar
	cp -r ./resources $(NEWCLASSES)/jaco/javaop

swap:
	-mkdir -p $(NEWCLASSES)/jaco/javaop/component
	-mkdir -p $(CLASSES)/jaco/javaop/component
	mv $(CLASSES)/jaco/javaop $(NEWCLASSES)/tempclasses
	mv $(NEWCLASSES)/jaco/javaop $(CLASSES)/jaco
	mv $(NEWCLASSES)/tempclasses $(NEWCLASSES)/jaco/javaop

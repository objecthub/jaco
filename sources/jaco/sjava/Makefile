# generated makefile for jaco extension sjavac
# Matthias Zenger, 30/05/99

JC = java -ms24m -mx40m jaco.pizza.Main
CLASSES = ../../../classes
# NEWCLASSES = ../../../new
NEWCLASSES = ../../../classes

note:
	@echo ''
	@echo '---- SJAVAC -- extension of JACO'
	@echo '---- generated makefile'
	@echo '-- '
	@echo '-- make compiler        generates the compiler in ../../../classes'
	@echo '-- make cup             updates the parser sources'
	@echo '-- make parser          compiles and installs the parser in ../../../classes'
	@echo '-- make rsrc            updates the resources in ../../../classes'
	@echo '-- make swap            installs the generated compiler'
	@echo ''

./grammar/Parser.tables: ./grammar/Grammar.cup
	java -ms16m -mx64m jcup.Main -package jaco.sjava.grammar -parser Parser -symbols Symbols \
         -o ./grammar/ -f ./grammar/Grammar.cup

cup: ./grammar/Parser.tables

parser: cup
	$(JC) -d $(CLASSES) ./grammar/*.java
	cp ./grammar/Parser.tables $(CLASSES)/jaco/sjava/grammar

rsrc:
	cp -r ./resources $(CLASSES)/jaco/sjava

compiler:
	-mkdir -p $(NEWCLASSES)/jaco/sjava/component
	$(JC) -d $(NEWCLASSES) ./struct/*.java ./component/*.java ./context/*.java ./grammar/*.java \
          ./Main.java
	cp ./grammar/Parser.tables $(NEWCLASSES)/jaco/sjava/grammar
	cp -r ./resources $(NEWCLASSES)/jaco/sjava

swap:
	-mkdir -p $(NEWCLASSES)/jaco/sjava/component
	-mkdir -p $(CLASSES)/jaco/sjava/component
	mv $(CLASSES)/jaco/sjava $(NEWCLASSES)/tempclasses
	mv $(NEWCLASSES)/jaco/sjava $(CLASSES)/jaco
	mv $(NEWCLASSES)/tempclasses $(NEWCLASSES)/jaco/sjava

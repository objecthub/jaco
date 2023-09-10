# generated makefile for jaco extension cjavac
# Matthias Zenger, 30/05/99

JC = java -ms24m -mx40m jaco.pizza.Main
CLASSES = ../../../classes

note:
	@echo ''
	@echo '---- cjavac -- extension of JACO'
	@echo '---- generated makefile'
	@echo '-- '
	@echo '-- make compiler        generates the compiler in ./new'
	@echo '-- make cup             updates the parser sources'
	@echo '-- make parser          compiles and installs the parser in ./classes'
	@echo '-- make rsrc            updates the resources in ./classes'
	@echo ''

./grammar/Parser.tables: ./grammar/Grammar.cup
	java -ms16m -mx64m jcup.Main -package jaco.cjava.grammar -parser Parser -symbols Symbols \
         -o ./grammar/ -f ./grammar/Grammar.cup

cup: ./grammar/Parser.tables

parser: cup
	$(JC) -d $(CLASSES) ./grammar/*.java
	cp ./grammar/Parser.tables $(CLASSES)/jaco/cjava/grammar

rsrc:
	cp -r ./resources $(CLASSES)/jaco/cjava

compiler:
	-mkdir -p $(CLASSES)/jaco/cjava/component
	$(JC) -d $(CLASSES) ./struct/*.java ./component/*.java ./context/*.java ./grammar/*.java \
          ./Main.java
	cp ./grammar/Parser.tables $(CLASSES)/jaco/cjava/grammar
	cp -r ./resources $(CLASSES)/jaco/cjava

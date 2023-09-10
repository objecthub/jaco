# generated makefile for jaco extension statc
# Matthias Zenger, 30/05/99

JC = java -ms24m -mx40m jaco.pizza.Main
CLASSES = ../../../classes

note:
	@echo ''
	@echo '---- statc -- extension of JACO'
	@echo '---- generated makefile'
	@echo '-- '
	@echo '-- make compiler        generates the compiler'
	@echo '-- make cup             updates the parser sources'
	@echo '-- make parser          compiles and installs the parser in ./classes'
	@echo '-- make rsrc            updates the resources in ./classes'
	@echo ''

./grammar/XParser.tables: ./grammar/Grammar.cup
	java -ms16m -mx64m jcup.Main -package jaco.stat.grammar -parser PizzaParser -symbols PizzaSymbols \
                   -o ./grammar/ -f ./grammar/Grammar.cup

cup: ./grammar/XParser.tables

parser: cup
	$(JC) -d $(CLASSES) ./grammar/*.java
	cp ./grammar/XParser.tables $(CLASSES)/jaco/stat/grammar

rsrc:
	cp -r ./resources $(CLASSES)/jaco/stat

compiler:
	-mkdir -p $(CLASSES)/jaco/stat/component
	$(JC) -d $(CLASSES) ./struct/*.java ./component/*.java ./context/*.java ./grammar/*.java \
          ./Main.java
	cp ./grammar/PizzaParser.tables $(CLASSES)/jaco/stat/grammar
	cp -r ./resources $(CLASSES)/jaco/stat

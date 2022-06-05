# Makefile for Network Security Encrypted Group Chat Application

JC=/usr/bin/javac
BIN=bin
SRC=src
JARS=lib/bcmail-jdk18on-171.jar:lib/bcpkix-jdk18on-171.jar:lib/bcprov-jdk18on-171.jar:lib/bctls-jdk18on-171.jar:lib/bcutil-jdk18on-171.jar
BINJARS=$(BIN):$(JARS)
JAVA_FILES=$(SRC)/*.java
JFLAGS=-d $(BIN)/ -sourcepath $(SRC) -cp $(JARS)

build: $(JAVA_FILES)
	$(JC) $(JFLAGS) $?

clean:
	@rm $(BIN)/*.class
	@echo "Clean!"

docs:
	@javadoc -d doc $(JAVA_FILES)

cleanDocs:
	@rm -r doc

server: $(SRC)/Server.java $(SRC)/ServerThread.java
	$(JC) $(JFLAGS) $?
	java -cp $(BINJARS) Server

serverPort: $(SRC)/Server.java $(SRC)/ServerThread.java
	$(JC) $(JFLAGS) $?
	java -cp $(BINJARS) Server $(PORT)

user: build
	java -cp $(BINJARS) User

userPort: build
	java -cp $(BINJARS) User $(PORT)
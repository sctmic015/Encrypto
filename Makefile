# Makefile for Network Security Encrypted Group Chat Application

JC=/usr/bin/javac
BIN=bin
SRC=src
JAVA_FILES=$(SRC)/*.java
JFLAGS=-d $(BIN)/ -sourcepath $(SRC) 

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
	java -cp $(BIN) Server

serverPort: $(SRC)/Server.java $(SRC)/ServerThread.java
	$(JC) $(JFLAGS) $?
	java -cp $(BIN) Server $(PORT)

user: build
	java -cp $(BIN) User

userPort: build
	java -cp $(BIN) User $(PORT)
# Makefile to build the new frontend

all: StreamItLex.java StreamItParser.java StreamItParserFE.java 
	jikes -g ToJava.java


# compile .g files to .java files
%.java: %.g
	java antlr.Tool $<

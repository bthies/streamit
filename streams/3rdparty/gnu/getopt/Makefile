# Makefile for Java port of GNU getopt

all:	LongOpt.class Getopt.class GetoptDemo.class

LongOpt.class:	LongOpt.java
	jikes LongOpt.java

Getopt.class:	Getopt.java
	jikes Getopt.java

GetoptDemo.class:	GetoptDemo.java
	jikes GetoptDemo.java

docs: 
	javadoc -author -version -public Getopt.java LongOpt.java


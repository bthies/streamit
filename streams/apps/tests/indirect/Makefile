all: IndirectTest.java
	make -C $(STREAMIT_HOME)/compiler
	java -Xmx1700M  at.dms.kjc.Main -s --linearreplacement2 IndirectTest.java > IndirectTest.c

IndirectTest.java:
	java streamit.frontend.ToJava IndirectTest.str > IndirectTest.java
clean:
	rm -f *.java *.dot *.ps *.c
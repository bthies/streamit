# $Id&
all: clean java streamit dot

java:
	jikes FieldPropTest.java
	jikes FieldPropTest2.java

streamit:
	java -Xmx256M  at.dms.kjc.Main -s FieldPropTest.java > FieldPropTest.c
	java -Xmx256M  at.dms.kjc.Main -s FieldPropTest2.java > FieldPropTest2.c
	gcc -o FieldPropTest -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c FieldPropTest.c
	gcc -o FieldPropTest2 -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c FieldPropTest2.c

dot: java
	java FieldPropTest -norun -printgraph > FieldPropTest.dot
	java FieldPropTest2 -norun -printgraph > FieldPropTest2.dot
	dot -Tps FieldPropTest.dot > FieldPropTest.ps
	dot -Tps FieldPropTest2.dot > FieldPropTest2.ps

clean:
	rm -rf *~
	rm -rf reg-out*
	rm -rf *.class
	rm -rf *.c
	rm -rf *.dot
	rm -rf *.ps
	rm -rf FieldPropTest
	rm -rf FieldPropTest2

STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc

clean:
	rm -f *.html 
	rm -f *.class
	rm -f *.dot
	rm -f *.java
	rm -f *.c

verify: clean
	strc DCTverify.str DCT.str -library -i1

compare: clean
	strc iDCTcompare.str DCT.str -library -i1

docs: ${STREAMITDOC}
	${STREAMITDOC} DCT.str -o DCT.html








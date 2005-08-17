STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc

default: fast

clean:
	rm -f DCTexternal.str
	rm -f *.html 
	rm -f *.class
	rm -f *.dot
	rm -f *.java

reference:	clean
	ln -s DCTreference.str DCTexternal.str

fast:	clean
	ln -s DCTfast.str DCTexternal.str

comparison:	clean
	ln -s DCTcomparison.str DCTexternal.str

check_correct: comparison
	strc DCTcorrectness.str DCTinternal.str DCTexternal.str -library -i 1

docs: DCTinternal.str ${STREAMITDOC}
	${STREAMITDOC} DCTinternal.str -o DCTinternal.html
	find ./DCTexternal.str && ${STREAMITDOC} DCTexternal.str -o DCTexternal.html








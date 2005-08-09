STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc

default: fast

clean:
	rm -f DCTexternal.str
	rm -f *.html 

reference:	clean
	ln -s DCTreference.str DCTexternal.str

fast:	clean
	ln -s DCTfast.str DCTexternal.str

comparison:	clean
	ln -s DCTcomparison.str DCTexternal.str

docs: DCTinternal.str ${STREAMITDOC}
	${STREAMITDOC} DCTinternal.str -o DCTinternal.html
	find ./DCTexternal.str && ${STREAMITDOC} DCTexternal.str -o DCTexternal.html








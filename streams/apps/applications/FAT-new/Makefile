all:
	cat *.str > LinkedFAT-test
	mv LinkedFAT-test LinkedFAT_test.str
	perl -pi -e 's/FAT_new/LinkedFAT_test/g' LinkedFAT_test.str
	${STREAMIT_HOME}/knit LinkedFAT_test.str

clean:
	rm *.c *.java *~ LinkedFAT_test.*
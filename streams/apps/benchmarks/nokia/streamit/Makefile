# Makefile links all java files into a single "linked" java file in order
# to make the job of gathering numbers easier.
IMPORTS   = 	"import streamit.library.*; \
	         import streamit.library.io.*; \
		 import java.lang.Math;"

LIB_FILES =	AdapTest.java \
		forw.java \
		multvect.java \
		backs.java \
		FirFilter.java \
		GenA.java \
		RowCol.java \
		chold.java \
		FloatIdentity.java \
		multiply.java \
		SelfProd.java \
		DelayPipeline.java

MAIN	  = 	dcalc

MAIN_FILE = 	${MAIN}.java
LINKED_FILE =   Linked${MAIN_FILE};

STREAMIT_MAKEFILE = Makefile.streamit 

all: link

clean:
	#remove liked file from dir
	rm -f $(LINKED_FILE)
	#remove old class files
	rm -f *.class

link: clean
	# put all the libraries in one file
	cat ${LIB_FILES} > ${MAIN_FILE}.tmp
	# make all library classes non-public
	perl -pi -e "s/public class/class/g" ${MAIN_FILE}.tmp
	# add the main file
	cat ${MAIN_FILE}.tmp ${MAIN_FILE} > ${MAIN_FILE}.1.tmp
	# remove all the import statements
	perl -pi -e "s/import .*;//g" ${MAIN_FILE}.1.tmp
	# convert main file name into linked file name
	perl -pi -e "s/class ${MAIN}/class Linked${MAIN}/g" ${MAIN_FILE}.1.tmp
	perl -pi -e "s/new ${MAIN}/new Linked${MAIN}/g" ${MAIN_FILE}.1.tmp
	# add the imports that we care about into the final file
	echo ${IMPORTS} > $(LINKED_FILE)
	# add the source code to the final file
	cat ${MAIN_FILE}.1.tmp >> $(LINKED_FILE)
	# remove temporaries
	rm -f ${MAIN_FILE}.tmp ${MAIN_FILE}.1.tmp
	# compile
	javac $(LINKED_FILE)

more-cycles:
	# change the simulator to run for 5Million cycles
	perl -pi -e "s/SIM-CYCLES = .*/SIM-CYCLES = 5000000/g" $(STREAMIT_MAKEFILE); 


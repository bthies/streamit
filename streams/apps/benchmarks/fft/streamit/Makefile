# Copied terribly from the streams/apps/FMRadio Makefile
# Assembles all the java sources in this directory into a 
# single "linked" file that can be compiled with streamit 
# (used for regression testing)
LIBRARIES = 	"../libraries"
IMPORTS   = 	"import streamit.library.*; \
	         import streamit.library.io.*; \
		 import java.lang.Math;"

LIB_FILES =	FFTTestSource.java \
		FloatPrinter.java

MAIN	  = 	FFT2

MAIN_FILE = 	${MAIN}.java

all:
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
	echo ${IMPORTS} > Linked${MAIN_FILE}
	# add the source code to the final file
	cat ${MAIN_FILE}.1.tmp >> Linked${MAIN_FILE}
	# remove temporaries
	rm -f ${MAIN_FILE}.tmp ${MAIN_FILE}.1.tmp


# replace all references in Makefile.streamit of
# SIM-CYCLES = 50000 to SIM-CYCLES = 5Million
more-cycles:
	perl -pi -e "s/SIM-CYCLES = \d*/SIM-CYCLES = 5000000/g" Makefile.streamit


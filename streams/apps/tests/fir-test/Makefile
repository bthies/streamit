LIBRARIES = 	"../../libraries"
IMPORTS   = 	"import streamit.library.*; "

LIB_FILES = 	${LIBRARIES}/FloatOneSource.java \
		${LIBRARIES}/FloatPrinter.java \
		${LIBRARIES}/FirFilter.java

MAIN	  = 	FirTest

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
	# compile
	javac Linked${MAIN_FILE}



LIBRARIES = 	"../libraries"
IMPORTS   = 	"import streamit.library.*; \
	         import streamit.library.io.*; \
		 import java.lang.Math;"

LIB_FILES =	ComplexFIRFilter.java \
		QuadratureDemod.java \
		RealFIRFilter.java

MAIN	  = 	perftest4_inout
MAIN_FILE = 	${MAIN}.java

MAIN2	  = 	perftest4
MAIN_FILE2 = 	${MAIN2}.java

all:
	# MAKE 1
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
	#
	# MAKE 2
	# put all the libraries in one file
	cat ${LIB_FILES} > ${MAIN_FILE2}.tmp
	# make all library classes non-public
	perl -pi -e "s/public class/class/g" ${MAIN_FILE2}.tmp
	# add the main file
	cat ${MAIN_FILE2}.tmp ${MAIN_FILE2} > ${MAIN_FILE2}.1.tmp
	# remove all the import statements
	perl -pi -e "s/import .*;//g" ${MAIN_FILE2}.1.tmp
	# convert main file name into linked file name
	perl -pi -e "s/class ${MAIN2}/class Linked${MAIN2}/g" ${MAIN_FILE2}.1.tmp
	perl -pi -e "s/new ${MAIN2}/new Linked${MAIN2}/g" ${MAIN_FILE2}.1.tmp
	# add the imports that we care about into the final file
	echo ${IMPORTS} > Linked${MAIN_FILE2}
	# add the source code to the final file
	cat ${MAIN_FILE2}.1.tmp >> Linked${MAIN_FILE2}
	# remove temporaries
	rm -f ${MAIN_FILE2}.tmp ${MAIN_FILE2}.1.tmp
	# compile
	javac Linked${MAIN_FILE2}



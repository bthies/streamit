# Command to run the streamit compiler
STREAMIT-COMPILER = java -Xmx512M at.dms.kjc.Main -s 
# command to compile uniprocessor streamit programs
UNI-COMPILER      = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 
# front end for converting new syntax to java
NEW-FRONTEND = java streamit.frontend.ToJava

# options to use for linear analysis
ANALYSIS-OPTIONS  = --constprop --linearanalysis --debug 
# options to use for linear transformation
TRANSFORMATION-OPTIONS =  --constprop --linearanalysis --linearreplacement

# regression testing perl script
LINEAR-REG-TEST = linear_regtest.pl

# current test
CURRENT-TEST = regtests/LinearTest11

all: clean java-compile uni-compile raw-compile


linear-compile:
	jikes /u/aalamb/streams/compiler/kopi/kjc/sir/linear/*.java


test-current-old: linear-compile
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) test/simple.java > test/simple.c
	$(UNI-COMPILER) test/simple.c -o test/simple.exe
	$(STREAMIT-COMPILER) $(TRANSFORMATION-OPTIONS) test/simple.java > test/simple-linear.c
	$(UNI-COMPILER) test/simple-linear.c -o test/simple-linear.exe

test-current: linear-compile
	$(NEW-FRONTEND) < $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output



linear-regtest: linear-compile
	# just run our regression testing script
	$(LINEAR-REG-TEST)

clean:
	rm -f *.class
	rm -f $(UNI-SOURCE)
	rm -f $(UNI-EXE)
	rm -f Makefile.streamit
	rm -f sw*.s sw*.o
	rm -f tile*.o tile*.c tile*.lnk
	rm -f tile.*
	rm -f fileio.bc kernel
	rm -f *.dot
	rm -f *.output
	rm -f regtests/*.output
	rm -f regtests/*.parsed
	rm -f regtests/*~

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
CURRENT-TEST = regtests/LinearTest18

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
	rm -f *.dot *.dot.ps *.output
	rm -f regtests/*.output regtests/*.parsed  regtests/*.dot
	rm -f regtests/*~ regtests/*.exe regtests/*.run regtests/*.c
	rm -f regtests/LinearTest10.java regtests/LinearTest11.java regtests/LinearTest12.java regtests/LinearTest13.java 
	rm -f regtests/LinearTest14.java regtests/LinearTest15.java regtests/LinearTest16.java regtests/LinearTest17.java 
	rm -f regtests/LinearTest18.java regtests/LinearTest19.java regtests/LinearTest20.java regtests/LinearTest21.java 

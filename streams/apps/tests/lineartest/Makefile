# Command to run the streamit compiler
STREAMIT-COMPILER = java -Xmx512M at.dms.kjc.Main -s 
# command to compile uniprocessor streamit programs
UNI-COMPILER      = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 
# front end for converting new syntax to java
NEW-FRONTEND = java streamit.frontend.ToJava --full 

# options to use for linear analysis
ANALYSIS-OPTIONS  = --unroll 100000 --linearanalysis --debug 
# options to use for linear transformation
LINTRANS-OPTIONS =  --unroll 100000 --linearanalysis --linearreplacement --debug
# options to use for frquency transformation
FREQTRANS-OPTIONS =  --unroll 100000 --linearanalysis --frequencyreplacement 1 --targetFFTSize 20 --debug
# options to use for both transformations
BOTH-OPTIONS      =  --unroll 100000 --linearanalysis --linearreplacement --frequencyreplacement 1 --debug

# regression testing perl script
LINEAR-REG-TEST = linear_regtest.pl

# current test
CURRENT-TEST = regtests/LinearTest28

all: clean java-compile uni-compile raw-compile


linear-compile:
	jikes /u/aalamb/streams/compiler/kopi/kjc/sir/linear/*.java


test-current-replace: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(LINTRANS-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output

test-current-freq: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(FREQTRANS-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output
	# convert to .c files, and run compiler without any fancy options to see what we get.
	cp current.output test-freq.c
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) $(CURRENT-TEST).java > test-normal.c
	$(UNI-COMPILER) test-freq.c -o test-freq.exe
	$(UNI-COMPILER) test-normal.c -o test-normal.exe
	./test-freq.exe -i 10 > freq.out
	./test-normal.exe -i 1000 > norm.out

test-current-both: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(BOTH-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output



test-current: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
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
	rm -f test-freq* test-normal*
	rm -f regtests/LinearTest9.java
	rm -f regtests/LinearTest10.java regtests/LinearTest11.java regtests/LinearTest12.java regtests/LinearTest13.java 
	rm -f regtests/LinearTest14.java regtests/LinearTest15.java regtests/LinearTest16.java regtests/LinearTest17.java 
	rm -f regtests/LinearTest18.java regtests/LinearTest19.java regtests/LinearTest20.java regtests/LinearTest21.java 
	rm -f regtests/LinearTest22.java regtests/LinearTest23.java regtests/LinearTest24.java regtests/LinearTest25.java 
	rm -f regtests/LinearTest26.java regtests/LinearTest27.java regtests/LinearTest28.java regtests/LinearTest29.java 

## This is in a state of flux between old StreamIt and new StreamIt. I
## stopped trying to get it to work when I realized that
## linear_regtest does all the important stuff.  -BFT.

# Command to run the streamit compiler
STREAMIT-COMPILER = strc
# front end for converting new syntax to java
NEW-FRONTEND = java streamit.frontend.ToJava --full 

# options to use for linear analysis
ANALYSIS-OPTIONS  = --unroll 100000 --linearanalysis --debug 
# options to use for linear transformation
LINTRANS-OPTIONS =  --unroll 100000 --linearanalysis --linearreplacement --debug
# options to use for frquency transformation
FREQTRANS-OPTIONS =  --unroll 100000 --linearanalysis --frequencyreplacement 3 --debug
# options to use for both transformations
BOTH-OPTIONS      =  --unroll 100000 --linearanalysis --linearreplacement --frequencyreplacement 1 --debug

# options to use for frquency transformation
REDUNDANT-OPTIONS =  --unroll 100000 --linearanalysis --redundantreplacement --debug


# regression testing perl script
LINEAR-REG-TEST = linear_regtest.pl

# output comparison script
COMPARE = perl -I$(STREAMIT_HOME)/regtest/tools $(STREAMIT_HOME)/regtest/tools/compare_uni.pl 

# current test
CURRENT-TEST = regtests/LinearTest37


all: clean java-compile uni-compile raw-compile


linear-compile-old:
	jikes $(STREAMIT_HOME)/src/at/dms/kjc/sir/linear/*.java 
	jikes $(STREAMIT_HOME)/src/at/dms/kjc/sir/linear/transform/*.java
	jikes $(STREAMIT_HOME)/src/at/dms/kjc/sir/linear/frequency/*.java
linear-compile:
	make -C $(STREAMIT_HOME)/src
library-compile:
	make -C $(STREAMIT_HOME)/library/c


test-current-replace: linear-compile
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) $(CURRENT-TEST).str
	cp a.out test-normal.exe
	$(STREAMIT-COMPILER) $(LINTRANS-OPTIONS) $(CURRENT-TEST).str
	cp a.out test-replace.exe

test-current-freq: linear-compile library-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(FREQTRANS-OPTIONS) $(CURRENT-TEST).java > test-freq.c
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) $(CURRENT-TEST).java > test-normal.c
	$(UNI-COMPILER) test-freq.c -o test-freq.exe
	$(UNI-COMPILER) test-normal.c -o test-normal.exe
	./test-freq.exe -i 100 > freq.out
	./test-normal.exe -i 1000 > norm.out
	./parse_linear_output.pl test-freq.c
	$(COMPARE) freq.out norm.out



test-current-both: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(BOTH-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output



test-current: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS) $(CURRENT-TEST).java > current.output
	./parse_linear_output.pl current.output


test-current-redundant: linear-compile
	$(NEW-FRONTEND) $(CURRENT-TEST).str > $(CURRENT-TEST).java
	$(STREAMIT-COMPILER) $(REDUNDANT-OPTIONS) $(CURRENT-TEST).java > test-redund.c
	$(STREAMIT-COMPILER) $(ANALYSIS-OPTIONS)  $(CURRENT-TEST).java > test-normal.c
	$(UNI-COMPILER) test-redund.c -o test-redund.exe
	$(UNI-COMPILER) test-normal.c -o test-normal.exe
	./test-redund.exe -i 100 > redu.out
	./test-normal.exe -i 100 > norm.out
	./parse_redundant_output.pl test-redund.c
	$(COMPARE) redu.out norm.out





test-freq: linear-compile library-compile
	sl test-freq.c -o test-freq.exe
	./test-freq.exe -i 1



linear-regtest: linear-compile 
	# just run our regression testing script
	$(LINEAR-REG-TEST)


clean:
	rm -f *.class *.out
	rm -f *.dot *.dot.ps *.output
	rm -f regtests/*.output regtests/*.parsed  regtests/*.dot
	rm -f regtests/*~ regtests/*.exe regtests/*.run regtests/*.c
	rm -f test-freq* test-normal* test-replace* test-redund.*
	rm -f regtests/LinearTest9.java
	rm -f regtests/LinearTest10.java regtests/LinearTest11.java regtests/LinearTest12.java regtests/LinearTest13.java 
	rm -f regtests/LinearTest14.java regtests/LinearTest15.java regtests/LinearTest16.java regtests/LinearTest17.java 
	rm -f regtests/LinearTest18.java regtests/LinearTest19.java regtests/LinearTest20.java regtests/LinearTest21.java 
	rm -f regtests/LinearTest22.java regtests/LinearTest23.java regtests/LinearTest24.java regtests/LinearTest25.java 
	rm -f regtests/LinearTest26.java regtests/LinearTest27.java regtests/LinearTest28.java regtests/LinearTest29.java 
	rm -f regtests/LinearTest30.java regtests/LinearTest31.java regtests/LinearTest32.java regtests/LinearTest33.java 
	rm -f regtests/LinearTest34.java regtests/LinearTest35.java regtests/LinearTest36.java regtests/LinearTest37.java

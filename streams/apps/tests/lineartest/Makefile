# Command to run the streamit compiler
STREAMIT-COMPILER = java -Xmx512M  at.dms.kjc.Main -s 
# command to compile uniprocessor streamit programs
UNI-COMPILER      = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 

# various names of intermediary files.
SOURCES = LinearTest.java
UNI-SOURCE = LinearTest.java.c
UNI-EXE    = LinearTest.exe

all: clean java-compile uni-compile raw-compile



java-run: java-compile
	java LinearTest -i 100
java-compile:
	jikes $(SOURCES)

uni-run: uni-compile
	$(UNI-EXE) -i 100
uni-compile:
	$(STREAMIT-COMPILER) $(SOURCES) > $(UNI-SOURCE)
	$(UNI-COMPILER) $(UNI-SOURCE) -o $(UNI-EXE)

raw-run:
	make -f Makefile.streamit run
raw-compile:
	$(STREAMIT-COMPILER) --raw 4 --partition $(SOURCES)
	make -f Makefile.streamit


linear-compile:
	jikes /u/aalamb/streams/compiler/kopi/kjc/sir/linear/*.java
linear-test: linear-compile
	$(STREAMIT-COMPILER) --linearanalysis $(SOURCES)

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

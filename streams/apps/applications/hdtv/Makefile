COMPILER = java -Xmx512M  at.dms.kjc.Main -s
LINKER   = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 

all: clean java uni raw

clean:
	rm -rf *.class
	rm -rf *~
	rm -rf *.dot
	rm -rf *.ps
	rm -rf *.c
	rm -rf *.o
	rm -rf *.s
	rm -rf *.lnk
	rm -rf *.exe
	rm -rf *.rexe
	rm -rf *.out
	rm -rf Makefile.streamit
	rm -rf kernel
	rm -rf tile.rbf

java:
	jikes HDTV.java
	java HDTV -norun -printgraph > out.dot
	java HDTV -i 1000

uni:
	$(COMPILER) HDTV.java > HDTV.c
	$(LINKER) HDTV.c -o HDTV.exe
	HDTV.exe -i 1000

raw:
	$(COMPILER) --raw 8 HDTV.java
	make -f Makefile.streamit
	make -f Makefile.streamit run


COMPILER = java -Xmx512M  at.dms.kjc.Main -s
LINKER   = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 

all: clean java uni raw

clean:
	rm -rf *.class *~
	rm -rf *.dot *.ps *.c *.o *.s
	rm -rf *.lnk *.exe *.rexe *.out
	rm -rf Makefile.streamit
	rm -rf kernel
	rm -rf tile.rbf
	rm -rf hdtv_writeup.aux hdtv_writeup.dvi hdtv_writeup.log 
	rm -rf hdtv_writeup.ps hdtv_writeup.pdf
	rm -rf hdtv_writeup.bbl hdtv_writeup.blg

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

doc:
	latex hdtv_writeup.tex
	bibtex hdtv_writeup
	latex hdtv_writeup.tex
	latex hdtv_writeup.tex
	dvips hdtv_writeup.dvi -o hdtv_writeup.ps
	gv hdtv_writeup.ps



SOURCES  = HDTV.java TrellisEncoder.java TrellisDecoder.java PreCoder.java PreDecoder.java UngerboeckEncoder.java UngerboeckDecoder.java ReedSolomonEncoder.java ReedSolomonDecoder.java ConvolutionalInterleaver.java ConvolutionalDeinterleaver.java TrellisDecoderPipeline.java TrellisEncoderPipeline.java SymbolMapper.java SymbolUnMapper.java DelayPipeline.java DataReorder.java DataUnReorder.java

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
	rm -rf *.aux 
	rm -rf hdtv_writeup.dvi hdtv_writeup.log 
	rm -rf hdtv_writeup.ps hdtv_writeup.pdf
	rm -rf hdtv_writeup.bbl hdtv_writeup.blg
	rm -rf texput.log
	rm -rf hdtv-stream-graph.*;
	rm -rf images/hdtv-stream-graph.*;

java-compile: 
	jikes HDTV.java

java:
	java HDTV -i 1000

uni-compile:
	$(COMPILER) $(SOURCES) > HDTV.c
	$(LINKER) HDTV.c -o HDTV.exe
uni: uni-compile
	./HDTV.exe -i 1

#HDTV.exe -i 1000

raw-compile:
	$(COMPILER) --raw 8 -partition --fusion $(SOURCES)
raw: raw-compile
	make -f Makefile.streamit run

stream-graph-generate: java-compile
	java HDTV -norun -printgraph > hdtv.dot
	dot -Tps hdtv.dot > hdtv-stream-graph.ps
stream-graph: stream-graph-generate
	gv hdtv-stream-graph.ps

doc: stream-graph-generate
	ps2epsi hdtv-stream-graph.ps hdtv-stream-graph.eps
	cp hdtv-stream-graph.eps images
	latex hdtv_writeup.tex
	bibtex hdtv_writeup
	latex hdtv_writeup.tex
	latex hdtv_writeup.tex
	dvips hdtv_writeup.dvi -o hdtv_writeup.ps
	gv hdtv_writeup.ps



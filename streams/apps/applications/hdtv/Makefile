LIB_FILES = TrellisEncoder.java TrellisDecoder.java PreCoder.java PreDecoder.java UngerboeckEncoder.java UngerboeckDecoder.java ReedSolomonEncoder.java ReedSolomonDecoder.java ConvolutionalInterleaver.java ConvolutionalDeinterleaver.java TrellisDecoderPipeline.java TrellisEncoderPipeline.java SymbolMapper.java SymbolUnMapper.java DelayPipeline.java DataReorder.java Bitifier.java UnBitifier.java NoiseSource.java

SOURCES = HDTV.java $(LIB_FILES)

COMPILER = java -Xmx512M  at.dms.kjc.Main -s
LINKER   = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c 

IMPORTS   = 	"import streamit.library.*; \
	         import streamit.library.io.*; \
		 import java.lang.Math;"

MAIN	  = 	HDTV

MAIN_FILE = 	${MAIN}.java



all: clean java uni raw

clean:
	rm -rf *.class *~
	rm -rf streamgraphs/*.class streamgraphs/*~
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
	rm -rf streamgraphs/*.eps
	rm -rf images/*.tiff

java-compile: 
	jikes HDTV.java

java: java-compile
	java HDTV -i 1000

uni-compile:
	$(COMPILER) $(SOURCES) > HDTV.c
	$(LINKER) HDTV.c -o HDTV.exe
uni: uni-compile
	./HDTV.exe -i 1

#HDTV.exe -i 1000

raw-compile:
	$(COMPILER) --raw 8 -partition $(SOURCES)
raw: raw-compile
	make -f Makefile.streamit run

link:
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
	perl -pi -e "s/${MAIN}\./Linked${MAIN}\./g" ${MAIN_FILE}.1.tmp
	# add the imports that we care about into the final file
	echo ${IMPORTS} > Linked${MAIN_FILE}
	# add the source code to the final file
	cat ${MAIN_FILE}.1.tmp >> Linked${MAIN_FILE}
	# remove temporaries
	rm -f ${MAIN_FILE}.tmp ${MAIN_FILE}.1.tmp
	# compile
	#javac Linked${MAIN_FILE}


# convert all of the eps files we have in the images directory to tiff files
tiff-files:
	./images/convert_to_tiff.pl

individual-stream-graphs:
	./make_stream_graphs.pl

hdtv-stream-graph: java-compile
	java HDTV -norun -printgraph > hdtv.dot
	dot -Tps hdtv.dot > hdtv-stream-graph.ps
	ps2epsi hdtv-stream-graph.ps hdtv-stream-graph.eps
	cp hdtv-stream-graph.eps images

stream-graphs: hdtv-stream-graph individual-stream-graphs


dvi: stream-graphs
	latex hdtv_writeup.tex
	bibtex hdtv_writeup
	latex hdtv_writeup.tex
	latex hdtv_writeup.tex


ps: dvi
	dvips hdtv_writeup.dvi -o hdtv_writeup.ps
pdf: ps
	dvipdf hdtv_writeup.dvi


doc: ps pdf

ps-view: clean ps
	gv hdvt_writeup.ps &

pdf-view: clean pdf
	acroread hdtv_writeup.pdf &

STREAMIT_HOME = ~/streams
PREPROCESS = ${STREAMIT_HOME}/misc/scripts/preprocess.perl
STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc
STRC_OPTIONS = -library -nosched

DCT_HOME = ${STREAMIT_HOME}/apps/benchmarks/dct_ieee/streamit
LIBDCT = ${DCT_HOME}/DCTinternal.str ${DCT_HOME}/DCTexternal.str

LIBMPEG2 = MPEGdecoder.str ColorSpace.str Misc.str 


default: player

clean:	
	rm -f MPEGdecoder.str
	rm -f BinaryFile.str
	rm -f BMP.str
	rm -f *.java
	rm -f *.class
	rm -f *.dot
	rm -f *.html
	rm -f *.bmp

# NOTE: BinaryFile.str is actually only needed for compiling outside the library
# where one must use the FileReader<int>

player:	MPEGplayer.str BinaryFile.str ${LIBMPEG2} BMP.str ${LIBDCT}
	strc MPEGplayer.str BinaryFile.str ${LIBMPEG2} BMP.str ${LIBDCT} ${STRC_OPTIONS}

mpeg2bmp:	MPEGtoBMP.str BinaryFile.str ${LIBMPEG2} BMP.str ${LIBDCT}
	strc MPEGtoBMP.str BinaryFile.str ${LIBMPEG2} BMP.str ${LIBDCT} ${STRC_OPTIONS}

MPEGdecoder.str: MPEGdecoder.str.pre ${PREPROCESS}
	${PREPROCESS} MPEGdecoder.str.pre -o MPEGdecoder.str

BinaryFile.str: BinaryFile.str.pre ${PREPROCESS}
	${PREPROCESS} BinaryFile.str.pre -o BinaryFile.str

BMP.str: BMP.str.pre ${PREPROCESS}
	${PREPROCESS} BMP.str.pre -o BMP.str

docs:	${STREAMITDOC}
	${STREAMITDOC} BinaryFile.str.pre -o BinaryFile.str.pre.html
	${STREAMITDOC} BMP.str.pre -o BMP.str.pre.html
	${STREAMITDOC} ColorSpace.str -o ColorSpace.str.html
	${STREAMITDOC} Misc.str -o Misc.str.html
	${STREAMITDOC} MPEGdecoder.str.pre -o MPEGdecoder.str.pre.html
	${STREAMITDOC} MPEGplayer.str -o MPEGplayer.str.html
	${STREAMITDOC} MPEGtoBMP.str -o MPEGtoBMP.str.html








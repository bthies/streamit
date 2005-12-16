PREPROCESS = ${STREAMIT_HOME}/misc/scripts/preprocess.perl
STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc
STRC_OPTIONS = -library -nosched
#STRC_OPTIONS = -cluster 1

DCT_HOME = ${STREAMIT_HOME}/apps/benchmarks/dct_ieee/streamit
LIBDCT = ${DCT_HOME}/DCTinternal.str ${DCT_HOME}/DCTexternal.str

# NOTE: BinaryFile.str is actually only needed for compiling outside the library
# where one must use the FileReader<int>

LIBMPEG2 = MPEGglobal.str ColorSpace.str Misc.str BinaryFile.str

default: player

clean:	
	rm -f MPEGdecoder.str
	rm -f MPEGencoder.str
	rm -f MPEGglobal.str
	rm -f BinaryFile.str
	rm -f BMP.str
	rm -f *.java
	rm -f *.class
	rm -f *.dot
	rm -f *.html
	rm -f *.bmp
	rm -f *.m2v

player:	MPEGplayer.str MPEGdecoder.str ${LIBMPEG2} ${LIBDCT}
	strc MPEGplayer.str MPEGdecoder.str ${LIBMPEG2} ${LIBDCT} ${STRC_OPTIONS}

nomessage: MPEGdecoder_nomessage.str $(LIBMPEG2) $(LIBDCT)
	strc MPEGdecoder_nomessage.str $(LIBMPEG2) $(LIBDCT) $(STRC_OPTIONS)

mpeg2bmp:	MPEGtoBMP.str MPEGdecoder.str ${LIBMPEG2} BMP.str ${LIBDCT}
	strc MPEGtoBMP.str MPEGdecoder.str ${LIBMPEG2} BMP.str ${LIBDCT} ${STRC_OPTIONS}

bmp2mpeg: BMPtoMPEG.str MPEGencoder.str MPEGdecoder.str ${LIBMPEG2} BMP.str ${LIBDCT}
	strc BMPtoMPEG.str MPEGencoder.str MPEGdecoder.str ${LIBMPEG2} BMP.str ${LIBDCT} ${STRC_OPTIONS}

${LIBDCT}:
	pushd ${DCT_HOME} && make && popd

%.str: %.str.pre ${PREPROCESS}
	${PREPROCESS} $@.pre -o $@

docs:	${STREAMITDOC}
	${STREAMITDOC} BinaryFile.str.pre -o BinaryFile.str.pre.html
	${STREAMITDOC} BMP.str.pre -o BMP.str.pre.html
	${STREAMITDOC} ColorSpace.str -o ColorSpace.str.html
	${STREAMITDOC} Misc.str -o Misc.str.html
	${STREAMITDOC} MPEGdecoder.str.pre -o MPEGdecoder.str.pre.html
	${STREAMITDOC} MPEGplayer.str -o MPEGplayer.str.html
	${STREAMITDOC} MPEGtoBMP.str -o MPEGtoBMP.str.html












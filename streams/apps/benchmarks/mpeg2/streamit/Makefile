# --------------------------------------------------------------------
# Performance / Modes / Options Variables
# --------------------------------------------------------------------

COLOR_CHANNEL_PROCESSING = ColorChannelProcessing.str
# COLOR_CHANNEL_PROCESSING = ColorChannelProcessing_opt_splitjoin.str

DESCRAMBLE_AND_MOTION_COMPENSATE = DescrambleAndMotionCompensate.str
# DESCRAMBLE_AND_MOTION_COMPENSATE = DescrambleAndMotionCompensate_opt_handfused.str

MOTION_PREDICTION = MotionPrediction.str
# MOTION_PREDICTION = MotionPrediction_alt_blocked.str
# MOTION_PREDICTION = MotionPrediction_alt_blocked_parallel.str

BLOCK_DESCRAMBLER = BlockDescrambler.str
# BLOCK_DESCRAMBLER = BlockDescrambler_opt_handfused_nopeek.str

# --------------------------------------------------------------------
# Make and Compilation Variables
# --------------------------------------------------------------------

PREPROCESS = ${STREAMIT_HOME}/misc/scripts/preprocess.perl
STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc
STRC_OPTIONS = -library -nosched

DCT_HOME = ${STREAMIT_HOME}/apps/benchmarks/dct_ieee/streamit
LIBDCT = ${DCT_HOME}/DCT.str

# NOTE: BinaryFile.str is actually only needed for compiling outside the library
# where one must use the FileReader<int>

# General Purpose Streams Used By Both the Encoders and Decoders
LIBMPEG2 = MPEGglobal.str ColorSpace.str Misc.str BinaryFile.str ZigZag.str ${LIBDCT}

# Streams Used By the Decoders
LIBMPEG2_DECODE = MPEGdecoder.str Parser.str ${COLOR_CHANNEL_PROCESSING} ${DESCRAMBLE_AND_MOTION_COMPENSATE} ${MOTION_PREDICTION} ${BLOCK_DESCRAMBLER}

# Streams Used By the Encoders
LIBMPEG2_ENCODE = MPEGencoder.str

default: player

# --------------------------------------------------------------------
# User Executed Targets
# --------------------------------------------------------------------

clean:	
	rm -f MPEGdecoder.str
	rm -f MPEGencoder.str
	rm -f MPEGglobal.str
	rm -f BinaryFile.str
	rm -f MPEGdecoder_nomessage.str
	rm -f BMP.str
	rm -f Parser.str
	rm -f MotionPrediction.str
	rm -f MotionPrediction_alt_blocked.str
	rm -f MotionPrediction_alt_blocked_parallel.str
	rm -f *.java
	rm -f *.class
	rm -f *.dot
	rm -f *.html
	rm -f *.bmp
	rm -f *.m2v
	rm -f *.int

player:	MPEGplayer.str ${LIBMPEG2_DECODE} ${LIBMPEG2}
	strc MPEGplayer.str ${LIBMPEG2_DECODE} ${LIBMPEG2} ${STRC_OPTIONS}

mpeg2bmp:	MPEGtoBMP.str ${LIBMPEG2_DECODE} ${LIBMPEG2} BMP.str 
	strc MPEGtoBMP.str ${LIBMPEG2_DECODE} ${LIBMPEG2} BMP.str ${STRC_OPTIONS}

bmp2mpeg: BMPtoMPEG.str ${LIBMPEG2_ENCODE} ${LIBMPEG2} BMP.str 
	strc BMPtoMPEG.str ${LIBMPEG2_ENCODE} ${LIBMPEG2} BMP.str ${STRC_OPTIONS}

nomessage: MPEGdecoder_nomessage.str ${LIBMPEG2}
	strc MPEGdecoder_nomessage.str ${LIBMPEG2} ${STRC_OPTIONS}

docs:	${STREAMITDOC}
	${STREAMITDOC} BinaryFile.str.pre -o BinaryFile.str.pre.html
	${STREAMITDOC} BMP.str.pre -o BMP.str.pre.html
	${STREAMITDOC} ColorSpace.str -o ColorSpace.str.html
	${STREAMITDOC} Misc.str -o Misc.str.html
	${STREAMITDOC} MPEGdecoder.str.pre -o MPEGdecoder.str.pre.html
	${STREAMITDOC} MPEGencoder.str.pre -o MPEGencoder.str.pre.html
	${STREAMITDOC} MPEGplayer.str -o MPEGplayer.str.html
	${STREAMITDOC} MPEGtoBMP.str -o MPEGtoBMP.str.html
	${STREAMITDOC} Parser.str.pre -o Parser.str.pre.html

# --------------------------------------------------------------------
# Make Executed Targets
# --------------------------------------------------------------------

${LIBDCT}:
	pushd ${DCT_HOME} && make && popd

%.str: %.str.pre ${PREPROCESS}
	${PREPROCESS} $@.pre -o $@












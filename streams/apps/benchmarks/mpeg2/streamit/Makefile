include $(STREAMIT_HOME)/misc/Makefile.vars

# --------------------------------------------------------------------
# Performance / Modes / Options Variables
# --------------------------------------------------------------------

PARSER = Parser_alt_preserved_state.str

#COLOR_CHANNEL_PROCESSING = ColorChannelProcessing.str
COLOR_CHANNEL_PROCESSING = ColorChannelProcessing_opt_splitjoin.str
# the previous line is needed while StreamIt fails to handle splitter selection
# inside a conditional
# COLOR_CHANNEL_PROCESSING = 
# Note - this last (null) option is for the alternate decoder

DESCRAMBLE_AND_MOTION_COMPENSATE = DescrambleAndMotionCompensate.str
# DESCRAMBLE_AND_MOTION_COMPENSATE = DescrambleAndMotionCompensate_opt_handfused.str

MOTION_PREDICTION = MotionPrediction.str
# change to alt_blocked for messaging version
# MOTION_PREDICTION = MotionPrediction_alt_blocked.str
# MOTION_PREDICTION = MotionPrediction_alt_blocked_parallel.str

BLOCK_DESCRAMBLER = BlockDescrambler.str
# BLOCK_DESCRAMBLER = BlockDescrambler_opt_handfused_nopeek.str

DECODER_PIPELINE = MPEGdecoder.str
# DECODER_PIPELINE = MPEGdecoder_alt_parallel.str
# NOTE: Using _alt_parallel pipeline requires that:
#       MOTION_PREDICTION use alt_blocked_parallel
#       COLOR_CHANNEL_PROCESSING use (null)

# --------------------------------------------------------------------
# Make and Compilation Variables
# --------------------------------------------------------------------

PREPROCESS = ${STREAMIT_HOME}/misc/scripts/preprocess.perl
STREAMITDOC = ${STREAMIT_HOME}/misc/scripts/streamitdoc
STRC_OPTIONS = -library -nosched

DCT_HOME = ${STREAMIT_HOME}/apps/benchmarks/dct_ieee/streamit
LIBDCT = ${DCT_HOME}/DCT.str

MACROBLOCK_DESCRAMBLER = MacroBlockDescrambler.str
DATA_REORDERING = ${BLOCK_DESCRAMBLER} ${MACROBLOCK_DESCRAMBLER}

# General Purpose Streams Used By Both the Encoders and Decoders
LIBMPEG2 = MPEGglobal.str ColorSpace.str Misc.str BinaryFile.str ZigZag.str InverseQuantization.str  ${LIBDCT}
LIBMPEG2_NOMESSAGE = MPEGglobal.str ColorSpace.str Misc.str BinaryFile.str ${LIBDCT}

# Streams Used By the Decoders
LIBMPEG2_DECODE = ${DECODER_PIPELINE} ${PARSER} ChannelUpsampling.str MotionVectorDecode.str PictureReorder.str  BlockDecode.str ${COLOR_CHANNEL_PROCESSING} ${DESCRAMBLE_AND_MOTION_COMPENSATE}  ${MOTION_PREDICTION} ${DATA_REORDERING}

# Streams Used By the Encoders
LIBMPEG2_ENCODE = MPEGencoder.str Generator.str

default: player

# --------------------------------------------------------------------
# User Executed Targets
# --------------------------------------------------------------------

clean:	
	rm -f MPEGdecoder.str
	rm -f MPEGdecoder_alt_parallel.str
	rm -f MPEGencoder.str
	rm -f MPEGglobal.str
	rm -f BinaryFile.str
	rm -f MPEGdecoder_nomessage.str
	rm -f BMP.str
	rm -f Parser.str
	rm -f Parser_alt_preserved_state.str
	rm -f MotionPrediction.str
	rm -f MotionPrediction_alt_blocked.str
	rm -f MotionPrediction_alt_blocked_parallel.str
	rm -f InverseQuantization.str
	rm -f Generator.str
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

nomessage: MPEGdecoder_nomessage.str ${LIBMPEG2_NOMESSAGE}
	strc MPEGdecoder_nomessage.str ${LIBMPEG2_NOMESSAGE} ${STRC_OPTIONS}

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

MPEGdecoder_nomessage.str: MPEGdecoder_nomessage.str.pre ${PREPROCESS}
	${PREPROCESS} $@.pre -o $@
	perl -p -i -e 's/blocks_per_macroblock\[the_chroma_format\]/\/\*blocks_per_macroblock\[the_chroma_format\]\*\/6/' $@

%.str: %.str.pre ${PREPROCESS}
	${PREPROCESS} $@.pre -o $@












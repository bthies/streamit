STREAMIT_HOME = ~/streams

default: run

clean:	
	rm -f MPEGdecoder.str && rm -f *.java && rm -f *.class && rm -f *.dot && rm -f *.html && rm -f *.bmp

run:	MPEGdecoder.str
	strc MPEGdecoder.str -library -nosched

MPEGdecoder.str: MPEGdecoder.str.pre ${STREAMIT_HOME}/misc/scripts/preprocess.perl DCT.str
	${STREAMIT_HOME}/misc/scripts/preprocess.perl MPEGdecoder.str.pre -o MPEGdecoder.str

docs:	DCT.str ${STREAMIT_HOME}/misc/scripts/streamitdoc
	${STREAMIT_HOME}/misc/scripts/streamitdoc DCT.str -o DCT.html

TARGET  = sar
SOURCES = SAR.str genRawSAR.str FFT.str Statics.str Utils.str
BACKEND = 
# -asciifileio -simpleC

OUTFILE = image
RESULTS = image.txt

all: ${TARGET}

${TARGET}: ${SOURCES}
	strc ${SOURCES} -o ${TARGET} ${BACKEND}

run: ${TARGET}
	./${TARGET} -i 1

lib: ${SOURCES}
	strc ${SOURCES} --library -i1

check:
#for ascii files uses
	spiff -q -r .01 ../output/${RESULTS} ${OUTFILE} | head
# for binary files uses
#	@cmp -l ../output/${RESULTS} ${OUTFILE} | less	
	@echo "expected output can be found in ../output/"

clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out ${TARGET} ${OUTFILE}

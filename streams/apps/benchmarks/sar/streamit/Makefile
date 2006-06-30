TARGET  = sar
SOURCES = SAR.str genRawSAR.str FFT.str Statics.str Utils.str
BACKEND = -simpleC

all: ${TARGET}

${TARGET}: ${SOURCES}
	strc ${SOURCES} -o ${TARGET} ${BACKEND}

run: ${TARGET}
	./${TARGET} -i1

lib: ${SOURCES}
	strc ${SOURCES} --library -i1

check:
	@cmp -l ../output/image image | less
	@echo "expected output can be found in ../output/image[.txt (for ascii output)]"

clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out ${TARGET} image

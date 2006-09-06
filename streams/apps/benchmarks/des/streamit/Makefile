SOURCES = DES.str Sboxes.str Utils.str Keys.str Source.str Statics.str
BACKEND = 
#-simpleC

all: des

des: ${SOURCES}
	strc ${SOURCES} -o des ${BACKEND}

run: des
	./des -i1

lib: ${SOURCES}
	strc ${SOURCES} --library -i1

check:
	@head -8 ../output/output.txt | tail -1
	@echo "assuming test vector #7 is used"
	@echo "expected output can be found in ../output/output.txt"

clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out des

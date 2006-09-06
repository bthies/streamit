SOURCES = Serpent.str L.str Utils.str Keys.str Source.str Statics.str
BACKEND = 
#-simpleC

all: serpent

serpent: ${SOURCES}
	strc ${SOURCES} -o serpent ${BACKEND}

run: serpent
	./serpent -i1

lib: ${SOURCES}
	strc ${SOURCES} --library -i1

check:
	@head -3 ../output/output.txt | tail -1
	@echo "assuming test vector #2 is used"
	@echo "expected output can be found in ../output/output.txt"

clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out serpent

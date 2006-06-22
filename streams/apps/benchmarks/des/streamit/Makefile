SOURCES = DES.str Sboxes.str Utils.str Keys.str Source.str
BACKEND = --simpleC

all: des

des: ${SOURCES}
	strc DES.str Sboxes.str Utils.str Keys.str Source.str -o des ${BACKEND}

run: des
	./des -i1

lib: ${SOURCES}
	strc DES.str Sboxes.str Utils.str Keys.str Source.str --library -i1

check:
	@head -4 ../data/output | tail -1
	@echo "assuming test vector #3 is used"
	@echo "expected output can be found in ../data/output"


clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out des
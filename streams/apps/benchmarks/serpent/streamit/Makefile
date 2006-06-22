SOURCES = Serpent.str L.str Utils.str Keys.str Source.str
BACKEND = 

all: serpent

serpent: ${SOURCES}
	strc Serpent.str L.str Utils.str Keys.str Source.str -o serpent ${BACKEND}

run: serpent
	./serpent -i1

lib: ${SOURCES}
	strc Serpent.str L.str Utils.str Keys.str Source.str --library -i1

check:
	@echo "7CA73BB083C31E69EC52BD8227A46120"
	@echo "assuming default test vector is used"

clean:
	@rm -f *cpp *h *c *txt *java *class *dot Makefile.cluster *.o *~ a.out serpent
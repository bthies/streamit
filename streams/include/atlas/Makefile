#
# This requires ATLAS_HOME to be defined.  At the time this was
# written, ATLAS_HOME on CAG was at:
#
# /home/bits7/NO_BACKUP/streamit/ATLAS/lib
#

all: atlas-test

atlas-test: atlas-test.c atlas-interface.c
	gcc -I${ATLAS_HOME}/include -L${ATLAS_HOME}/lib/Linux_P4SSE2/ atlas-test.c -lcblas -latlas -o atlas-test

FILES = Tester DataStruct lqHouse WienerHopfSolution qr matlabFunctions \
	AdaptiveBeamform ABsteeringMatrix Stap DopplerFilter Cfar \
	Detection Pulse DFT
STRS = $(FILES:%=%.str)
STRCFLAGS =

all: library

library:
	strc --library -i 1 $(STRCFLAGS) $(STRS)

Tester:
	strc $(STRCFLAGS) $(STRS)

raw:
	strc --raw 4 $(STRCFLAGS) $(STRS)

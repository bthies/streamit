###############################################################################
# Make the GMTI application or its fragments.
#
# make               makes a library version (Tester.java)
# make raw           makes a raw version
# make make_files    makes a library version that outputs intermediate data
#                    files at several points, for verifying fragments.
# make fragments_lib makes fragments of GMTI for the library version 
# make fragments_raw makes fragments of GMTI for the raw backends.
#
# Variables:
# STRCFLAGS  default ""   flags to pass to strc
# TILESQRT   default "4"  square root of number of tiles on a raw processor
# FRAGMENT   default "1"  code fragment.
#
# Example:  to make only the AdaptiveBeamform fragment for a 8x8 raw machine
#  using the spacedynamic back end:
#
# make 'TILESQRT=8' 'FRAGMENT=4' 'STRCFLAGS=--spacedynamic' fragments_raw
#
# See GMTI_Fragments.str header for more info about making fragments of the 
# application
###############################################################################
# override for optimizations.
STRCFLAGS =

# override to select phases in GMTI_Fragments
FRAGMENT = 1

# override for different raw tile configurations
TILESQRT = 4

FILES = DataStruct lqHouse WienerHopfSolution qr matlabFunctions \
	AdaptiveBeamform ABsteeringMatrix Stap DopplerFilter Cfar \
	Detection Pulse DFT
TESTERFILES = Tester $(FILES)

STRS = $(TESTERFILES:%=%.str)


all: library

library:
	strc --library -i 1 $(STRCFLAGS) $(STRS)

Tester:
	strc $(STRCFLAGS) $(STRS)

raw:
	strc --raw $(TILESQRT) $(STRCFLAGS) $(STRS)

# make intermediate files for GMTI_Fragments from data.bin
make_files:
	strc --library -i 1 $(STRCFLAGS) Tester_Intermediate_Results.str $(FILES:%=%.str)

# make fragments of GMTI: override $(FRAGMENTS) if you don't want the 
# whole program
fragments_lib:
	(cat GMTI_Fragments.str | sed -e 's/FRAGMENT/$(FRAGMENT)/g' > GMTI_Fragment$(FRAGMENT).str)
	strc --library -i 1 $(STRCFLAGS) GMTI_Fragment$(FRAGMENT).str $(FILES:%=%.str)

fragments_raw:
	(cat GMTI_Fragments.str | sed -e 's/FRAGMENT/$(FRAGMENT)/g' > GMTI_Fragment$(FRAGMENT).str)
	strc --raw $(TILESQRT)  $(STRCFLAGS) GMTI_Fragment$(FRAGMENT).str $(FILES:%=%.str)

fragments_cluster:
	(cat GMTI_Fragments.str | sed -e 's/FRAGMENT/$(FRAGMENT)/g' > GMTI_Fragment$(FRAGMENT).str)
	strc -cluster 1 -standalone $(STRCFLAGS) GMTI_Fragment$(FRAGMENT).str $(FILES:%=%.str)


fragments_simpleC:
	(cat GMTI_Fragments.str | sed -e 's/FRAGMENT/$(FRAGMENT)/g' > GMTI_Fragment$(FRAGMENT).str)
	strc -simpleC $(STRCFLAGS) GMTI_Fragment$(FRAGMENT).str $(FILES:%=%.str)

fragments_uni:
	(cat GMTI_Fragments.str | sed -e 's/FRAGMENT/$(FRAGMENT)/g' > GMTI_Fragment$(FRAGMENT).str)
	strc $(STRCFLAGS) GMTI_Fragment$(FRAGMENT).str $(FILES:%=%.str)

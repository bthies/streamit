CXX       = gcc
CXXFLAGS  = -O3 

CORE_SRCS = perfTestOpt.cc serpentkeygen.cc output.cc
CORE_OBJS = $(CORE_SRCS:%.cc=%.o)

ALL_SRCS  = $(CORE_SRCS)
ALL_OBJS  = $(CORE_OBJS)
DEPS      =  Serpent.str Serpent.opt KeySched.str

all: serpent

serpent: output.cc $(CORE_OBJS)
	$(CXX) $(CXXFLAGS) $(CORE_OBJS) -lstdc++ -o serpent

#serpentkeygen.cc: KeySched.str
#	compilesbit.sh KeyGen.str
#	mv output.cc serpentkeygen.cc
#	mv output.h  serpentkeygen.h

serpentkeygen.o: serpentkeygen.cc serpentkeygen.h
	$(CXX) -o $@ -c serpentkeygen.cc

output.cc: $(DEPS)
	compilesbit.sh Serpent.str

%.o: %.cc $(DEPS)
	$(CXX) $(CXXFLAGS) -o $@ -c $*.cc

clean:
	@rm -f \#* *~ *.o *.mat *.dot
	@rm -rf OUTPUT
	@rm -f serpent

spotless: clean
#	@rm -f serpentkeygen.cc serpentkeygen.h
	@rm -f output.cc output.h

CXX       = gcc
CXXFLAGS  = -O3 

CORE_SRCS = perfTestOpt.cc keygen.cc output.cc
CORE_OBJS = $(CORE_SRCS:%.cc=%.o)

ALL_SRCS  = $(CORE_SRCS)
ALL_OBJS  = $(CORE_OBJS)
DEPS      =  DES.str DES.opt 

all: des

des: output.cc $(CORE_OBJS)
	$(CXX) $(CXXFLAGS) $(CORE_OBJS) -lstdc++ -o des

output.cc: $(DEPS)
	compilesbit.sh DES.str

%.o: %.cc $(DEPS)
	$(CXX) $(CXXFLAGS) -o $@ -c $*.cc

clean:
	@rm -f \#* *~ *.o *.mat *.dot
	@rm -rf OUTPUT
	@rm -f des

spotless: clean
	@rm -f output.h output.cc

#  
#  Copyright 1997 Massachusetts Institute of Technology
# 
#  Permission to use, copy, modify, distribute, and sell this software and
#  its documentation for any purpose is hereby granted without fee, provided
#  that the above copyright notice appear in all copies and that both that
#  copyright notice and this permission notice appear in supporting
#  documentation, and that the name of M.I.T. not be used in advertising or
#  publicity pertaining to distribution of the software without specific,
#  written prior permission.  M.I.T. makes no representations about the
#  suitability of this software for any purpose.  It is provided "as is"
#  without express or implied warranty.
# 

CC = g++
RM = rm -f
CFLAGS =  -Wall -g -pg
DEBUG_CFLAGS =  -g -Wall
SRCS = $(shell ls *.cc)
CLASSES = $(shell ls *.java | mawk 'gsub ( ".java$$", ".class" )')
OBJS = $(shell ls *.cc | mawk 'gsub ( ".cc$$", ".o" )')  
EXECS = $(shell ls *.cc | mawk 'gsub ( ".cc$$", "" )' | grep -v specui)
DEBUG_EXECS = $(shell ls *.cc | mawk 'gsub ( ".cc$$", ".debug" )' | grep -v specui)
INC	= -I$(SPECTRA_HOME)/lib/vr -I$(SPECTRA_HOME)/lib/vrio -I$(SPECTRA_HOME)/lib/vrp -I$(SPECTRA_HOME)/lib/vrvoice -I$(SPECTRA_HOME)/lib/vrgui -I$(SPECTRA_HOME)/lib/fftw/src -I/usr/local/jdk/include -I/usr/local/jdk/include/genunix -I$(SPECTRA_HOME)/lib/libpperf
LIBS	= -lm  -L$(SPECTRA_HOME)/lib/ -lspectra -lspecvoice -L$(SPECTRA_HOME)/lib/fftw/src -lfftw #$(SPECTRA_HOME)/lib/libsls.a $(SPECTRA_HOME)/lib/libsls_util.a
DEBUG_LIBS = $(SPECTRA_HOME)/lib/libspectra.a $(SPECTRA_HOME)/lib/libspecvoice.a $(SPECTRA_HOME)/lib/fftw/src/libfftw.a $(SPECTRA_HOME)/lib/libsls.a $(SPECTRA_HOME)/lib/libsls_util.a
LD_FLAGS = -Xlinker -rpath -Xlinker $(SPECTRA_HOME)/lib/vr -Xlinker -rpath -Xlinker $(SPECTRA_HOME)/lib/vrgui

all: $(EXECS)

world: $(EXECS) 

$(EXECS): % : %.cc
	$(CC) $(CFLAGS) $< -o $@ $(LIBS) $(INC)

debug: $(DEBUG_EXECS) 

$(DEBUG_EXECS): %.debug : %.cc
	$(CC) $(DEBUG_CFLAGS) $< -o $@ $(DEBUG_LIBS) $(INC)

java: $(CLASSES) $(EXECS) 

tags:
	find . -name CVS -prune -o -name "*.[cc,h]" -print | etags -

%.class: %.java
	javac $<

cleantags:
	$(RM) TAGS

clean:
	$(RM) $(CLASSES) $(EXECS) $(DEBUG_EXECS) TAGS

allclean:
	$(RM) $(CLASSES) $(EXECS) $(DEBUG_EXECS) TAGS







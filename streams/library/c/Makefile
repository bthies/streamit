#
# Makefile: Build the StreaMIT C runtime library
# $Id: Makefile,v 1.2 2001-10-11 14:04:21 dmaze Exp $
#

CC =		cc
CFLAGS =	-g
AR =		ar
RANLIB =	ranlib
LIBRARY =	libstreamit.a
OBJS = 		stream_context.o streamit_run.o streamit_splitjoin.o \
		streamit_tape.o

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	$(AR) r $@ $^

%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

# Explicit file dependencies:
stream_context.o: streamit.h
streamit_run.o: streamit.h streamit_internal.h
streamit_splitjoin.o: streamit.h streamit_internal.h
streamit_tape.o: streamit.h streamit_internal.h

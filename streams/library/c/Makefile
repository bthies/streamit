#
# Makefile: Build the StreaMIT C runtime library
# $Id: Makefile,v 1.3 2001-10-12 19:30:34 dmaze Exp $
#

CC =		cc
CFLAGS =	-g
AR =		ar
RANLIB =	ranlib
LIBRARY =	libstreamit.a
OBJS = 		stream_context.o streamit_run.o streamit_splitjoin.o \
		streamit_tape.o

all: $(LIBRARY)

clean:
	rm -f $(LIBRARY) $(OBJS)
	rm -f *~

$(LIBRARY): $(OBJS)
	$(AR) r $@ $^

%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

# Explicit file dependencies:
stream_context.o: streamit.h
streamit_run.o: streamit.h streamit_internal.h
streamit_splitjoin.o: streamit.h streamit_internal.h
streamit_tape.o: streamit.h streamit_internal.h

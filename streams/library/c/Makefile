#
# Makefile: Build the StreaMIT C runtime library
# $Id: Makefile,v 1.7 2001-11-02 19:36:41 dmaze Exp $
#

CC =		cc
CFLAGS =	-O2
AR =		ar
RANLIB =	ranlib
LIBRARY =	libstreamit.a
LIBRARYP =	libstreamit_p.a
NAMES =		stream_context streamit_message streamit_run \
		streamit_splitjoin streamit_tape
OBJS = 		$(NAMES:%=%.o)
OBJSP =		$(NAMES:%=%_p.o)

all: $(LIBRARY)

profile: $(LIBRARYP)

clean:
	rm -f $(LIBRARY) $(OBJS)
	rm -f $(LIBRARYP) $(OBJSP)
	rm -f *~

$(LIBRARY): $(OBJS)
	$(AR) r $@ $^
	$(RANLIB) $@

$(LIBRARYP): $(OBJSP)
	$(AR) r $@ $^
	$(RANLIB) $@

%.o: %.c
	$(CC) $(CFLAGS) -g -c -o $@ $<

%_p.o: %.c
	$(CC) $(CFLAGS) -g -pg -a -c -o $@ $<

# Explicit file dependencies:
stream_context.o: streamit.h
streamit_message.o: streamit.h streamit_internal.h
streamit_run.o: streamit.h streamit_internal.h
streamit_splitjoin.o: streamit.h streamit_internal.h
streamit_tape.o: streamit.h streamit_internal.h

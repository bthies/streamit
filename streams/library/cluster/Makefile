
CC =		gcc
CFLAGS = 	-O2 -I.
AR =            ar
RANLIB =        ranlib
LIBRARY =       libcluster.a
LIBRARYP =      libcluster_p.a

NAMES =		sock_dscr \
		thread_info \
		connection_info \
		timer \
		sdep \
		mysocket \
		data_consumer \
		data_producer \
		open_socket \
		message \
		init_instance \
		node_server \
		master_server \
		ccp \
		ccp_session \
		object_write_buffer \
		save_state \
		save_manager \
		delete_chkpts \
		service \
		read_setup

OBJS = 		$(NAMES:%=%.o)
OBJSP =         $(NAMES:%=%_p.o)

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


%.o: %.cpp
	$(CC) $(CFLAGS) -g -c -o $@ $<

%_p.o: %.c
	$(CC) $(CFLAGS) -g -pg -a -c -o $@ $<


# Explicit file dependencies:
init_instance.o: init_instance.h sock_dscr.h open_socket.h mysocket.h
int_pair.o: int_pair.h
mysocket.o: mysocket.h
open_socket.o: open_socket.h mysocket.h
timer.o: timer.h



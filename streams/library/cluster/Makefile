
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
		read_setup \
		mysocket \
		netsocket \
		memsocket \
		producer2 \
		consumer2

#               data_consumer
#               data_producer
#               network_connection
#               network_manager

SOURCES = 	$(NAMES:%=%.cpp)
DEPS = 		$(NAMES:%=%.d)
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
	$(CC) $(CFLAGS) -c -o $@ $<

%_p.o: %.c
	$(CC) $(CFLAGS) -g -pg -a -c -o $@ $<

deps:	
	$(CC) $(CFLAGS) -MD -E $(SOURCES) > /dev/null


-include $(DEPS)

# Explicit file dependencies:
#init_instance.o: init_instance.h sock_dscr.h open_socket.h netsocket.h
#int_pair.o: int_pair.h
#netsocket.o: netsocket.h
#open_socket.o: open_socket.h netsocket.h
#timer.o: timer.h



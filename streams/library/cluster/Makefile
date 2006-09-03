# build the cluster library

include $(STREAMIT_HOME)/misc/Makefile.vars

CFLAGS = 	-O2
CFLAGS += 	-I.
LIBRARY =       libcluster.a
LIBRARYP =      libcluster_p.a

# unsupported versions
CC_IA64 =       ecc
CC_ARM = 	arm343 #arm-linux-gcc
LIBRARY_IA64 =  libcluster_ia64.a
LIBRARY_ARM =   libcluster_arm.a
# end unsupported versions

# End of parameterizable stuff.

NAMES =	profiler \
		sock_dscr \
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
		FileReader \
		FileWriter

#               data_consumer
#               data_producer
#               network_connection
#               network_manager

ifeq ($(HAVE_FFTW), 1)
NAMES +=	fft
endif

SOURCES = 	$(NAMES:%=%.cpp)
DEPS = 		$(NAMES:%=%.d)
OBJS = 		$(NAMES:%=%.o)
OBJS_IA64 = 	$(NAMES:%=%_ia64.o)
OBJS_ARM = 	$(NAMES:%=%_arm.o)
OBJSP =         $(NAMES:%=%_p.o)

all: $(LIBRARY)

ia64: $(LIBRARY_IA64)

arm: $(LIBRARY_ARM)

profile: $(LIBRARYP)

clean:
	rm -f $(LIBRARY) $(OBJS)
	rm -f $(LIBRARY_IA64) $(OBJS_IA64)
	rm -f $(LIBRARY_ARM) $(OBJS_ARM)
	rm -f $(LIBRARYP) $(OBJSP)
	rm -f *~

$(LIBRARY): $(OBJS)
	$(AR) r $@ $^
	$(RANLIB) $@

$(LIBRARY_IA64): $(OBJS_IA64)
	$(AR) r $@ $^
	$(RANLIB) $@

$(LIBRARY_ARM): $(OBJS_ARM)
	$(AR) r $@ $^
	$(RANLIB) $@

$(LIBRARYP): $(OBJSP)
	$(AR) r $@ $^
	$(RANLIB) $@


%.o: %.cpp
	$(CXX) $(CFLAGS) -c -o $@ $<

%_ia64.o: %.cpp
	$(CC_IA64) $(CFLAGS) -c -o $@ $<

%_arm.o: %.cpp
	$(CC_ARM) $(CFLAGS) -DARM -c -o $@ $<

%_p.o: %.c
	$(CXX) $(CFLAGS) -g -pg -a -c -o $@ $<

deps:	
	$(CXX) $(CFLAGS) -MD -E $(SOURCES) > /dev/null


-include $(DEPS)

# Explicit file dependencies:
#init_instance.o: init_instance.h sock_dscr.h open_socket.h netsocket.h
#int_pair.o: int_pair.h
#netsocket.o: netsocket.h
#open_socket.o: open_socket.h netsocket.h
#timer.o: timer.h



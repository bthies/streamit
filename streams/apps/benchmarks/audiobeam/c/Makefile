CC = gcc
CFLAGS = -g -I/usr/local/include
LIBPATH = -L/usr/lib -L/usr/local/lib 
LDLIBS = -lm # -ldmalloc
PROG = delay_and_sum 
SRCS = main.c
OBJS = ${SRCS:.c=.o}
LDFLAGS = ${LDLIBS}
LD = $(CC)

all: $(PROG)

${PROG}: ${OBJS}
	${LD} ${LIBPATH} ${LDFLAGS} ${OBJS} -o $@

clean: 	
	rm -f *.o *~ $(PROG)

run: 	all
	./delay_and_sum -data_file ../../from_kevin_farfield/eugene2 -output_file blah2 -search_far_field


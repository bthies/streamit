CC = gcc
CFLAGS = -I/usr/local/include -O3 #-g
LIBPATH = -L/usr/lib -L/usr/local/lib 
LDLIBS = -lm # -ldmalloc
PROG = delay_and_sum 
SRCS = optimized.c 
OBJS = ${SRCS:.c=.o}
LDFLAGS = ${LDLIBS}
LD = $(CC)

all: $(PROG)

${PROG}: ${OBJS}
	${LD} ${LIBPATH} ${LDFLAGS} ${OBJS} -o $@

clean: 	
	rm -f *.o *~ $(PROG)

run1: all
	./delay_and_sum -data_file ../data/c-data/near-field/data1 -output_file data1.out
run2: all
	./delay_and_sum -data_file ../data/c-data/near-field/data2 -output_file data2.out


CC = gcc
CFLAGS = -O2
INCLUDES = 
LIBS = -pthread -lstdc++ -lm
OBJS = main.o barrier.o core0.o 

all: $(OBJS)
	$(CC) $(CFLAGS) $(LIBS) $(OBJS) -o smp1

clean:
	rm *.o smp1

main.o: main.c
	$(CC) $(CFLAGS) $(INCLUDES) -c main.c

barrier.o: barrier.c
	$(CC) $(CFLAGS) $(INCLUDES) -c barrier.c

core0.o: core0.c
	$(CC) $(CFLAGS) $(INCLUDES) -c core0.c


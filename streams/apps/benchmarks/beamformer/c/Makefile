VSIPL_DIR = vsipl/TASP_VSIPL_Core_Plus
SRC = src
INC = include
INCLUDE_DIRS = -I$(VSIPL_DIR)/include -I$(INC)
LIB_DIRS = -L$(VSIPL_DIR)/lib

CC = gcc
CC_FLAGS = -O6 $(INCLUDE_DIRS) -c -DPC_IN_TIME  ##-DPRINT_RESULT  ##-DDEBUG_0 -DDEBUG_1 -DDEBUG_2 -DPC_IN_TIME

LINKER = gcc
LINK_FLAGS = -O6 $(LIB_DIRS)

MPIRUN		= mpirun
RUN_FLAGS	=
NUM_PROCS 	= 1
RUN_ARGS	=





PCADemo: vsipl $(SRC)/main.o $(SRC)/LPF.o $(SRC)/DIT.o \
		$(SRC)/BF.o $(SRC)/PC.o $(SRC)/Utils.o
	$(LINKER) $(LINK_FLAGS) $(SRC)/main.o $(SRC)/LPF.o \
			$(SRC)/DIT.o $(SRC)/BF.o $(SRC)/PC.o $(SRC)/Utils.o \
	-lvsip_c -lm -o PCADemo

run: PCADemo
	PCADemo $(RUN_ARGS)

$(SRC)/main.o: $(SRC)/main.c $(INC)/LPF.h $(INC)/DIT.h \
		$(INC)/BF.h $(INC)/PC.h $(INC)/Utils.h $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/main.c -o $(SRC)/main.o

$(SRC)/LPF.o: $(INC)/LPF.h $(SRC)/LPF.c $(INC)/Utils.h $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/LPF.c -o $(SRC)/LPF.o

$(SRC)/DIT.o: $(INC)/DIT.h $(SRC)/DIT.c $(INC)/Utils.h $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/DIT.c -o $(SRC)/DIT.o

$(SRC)/BF.o: $(INC)/BF.h $(SRC)/BF.c $(INC)/Utils.h $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/BF.c -o $(SRC)/BF.o

$(SRC)/PC.o: $(INC)/PC.h $(SRC)/PC.c $(INC)/Utils.h $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/PC.c -o $(SRC)/PC.o

$(SRC)/Utils.o: $(INC)/Utils.h $(SRC)/Utils.c $(INC)/Globals.h
	$(CC) $(CC_FLAGS) $(SRC)/Utils.c -o $(SRC)/Utils.o

clean:
	rm -f $(SRC)/*.o
	rm -f PCADemo
	rm -rf vsipl

vsipl:
	cp vsipl.tar.gz temp.tar.gz
	gzip -d temp.tar.gz
	tar xvf temp.tar
	rm -rf temp.tar
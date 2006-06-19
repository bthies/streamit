CFLAGS = -O

all: test ecb_vt ecb_vk ecb_e_m ecb_d_m cbc_e_m cbc_d_m katmct
katmct: test ecb_vt.txt ecb_vk.txt ecb_e_m.txt ecb_d_m.txt cbc_e_m.txt cbc_d_m.txt

ecb_vt.txt: ecb_vt
	ecb_vt > ecb_vt.txt

ecb_vk.txt: ecb_vk
	ecb_vk > ecb_vk.txt

ecb_e_m.txt: ecb_e_m
	ecb_e_m > ecb_e_m.txt

ecb_d_m.txt: ecb_d_m
	ecb_d_m > ecb_d_m.txt

cbc_e_m.txt: cbc_e_m
	cbc_e_m > cbc_e_m.txt

cbc_d_m.txt: cbc_d_m
	cbc_d_m > cbc_d_m.txt


test: test.o serpent.o
	cc -o test test.o serpent.o


ecb_vt: ecb_vt.o serpent.o
	cc -o ecb_vt  ecb_vt.o serpent.o


ecb_vk: ecb_vk.o serpent.o
	cc -o ecb_vk  ecb_vk.o serpent.o


ecb_e_m: ecb_e_m.o serpent.o
	cc -o ecb_e_m  ecb_e_m.o serpent.o


ecb_d_m: ecb_d_m.o serpent.o
	cc -o ecb_d_m  ecb_d_m.o serpent.o


cbc_e_m: cbc_e_m.o serpent.o
	cc -o cbc_e_m  cbc_e_m.o serpent.o


cbc_d_m: cbc_d_m.o serpent.o
	cc -o cbc_d_m  cbc_d_m.o serpent.o

clean:
	@rm -f cbc_d_m cbc_e_m ecb_d_m ecb_e_m ecb_vk ecb_vt test
	@rm -f *.o
	@rm -f *.txt
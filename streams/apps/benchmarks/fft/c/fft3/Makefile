all: fft compare dcompare  

# fft implementation  
fft: driver.c fft.c 
	gcc -lm driver.c fft.c -o fft 

# utilities for comparing two files with floating point numbers 
compare: compare.c 
	gcc -lm compare.c -o compare 

dcompare: dcompare.c 
	gcc -lm dcompare.c -o dcompare 

clean: 
	rm -f *.o dcompare compare fft core 

run:
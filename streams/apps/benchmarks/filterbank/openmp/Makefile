all:
	gcc -O3 filterbank.c -o ./filterbank

openmp:
	gcc -DOPENMP -O3 -fopenmp filterbank.c -o ./filterbank 

clean:
	rm -rf ./filterbank

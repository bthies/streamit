#include <stdlib.h>
#include <math.h>

//const int columns1  =  3;
//const int rows1     =  4;
//const int columns2  =  5;
//const int rows2     =  3;

const int columns1  =  10;
const int rows1     =  10;
const int columns2  =  10;
const int rows2     =  10;


const int mod_max   = 100;

/* The source of all floats */
float floatSource();

/* This file calculates C = A * B, and then prints out the elements of C */
int doCalculation() {
  float A[columns1][rows1];
  float B[columns2][rows2];
  float C[columns2][rows1];
  int i,j;

  if (columns1 != rows2)
    return -1;

  /*    while(1) { */ /* uncomment this while to make it run forever */

  /** initialize A **/
    for(j=0; j < rows1; j++) {
      for(i=0; i < columns1; i++) {
	A[i][j] = floatSource();
      }
    }
      
  /** initialize B **/
    for(j=0; j < rows2; j++) {
      for(i=0; i < columns2; i++) {
	B[i][j] = floatSource();
      }
    }

    for(j = 0; j < rows1; j++) {
      for(i = 0; i < columns2; i++) {
	int k;
	float val = 0;

	/** calculate C_{i,j} **/
	for(k = 0; k < columns1; k++) {
	  val += A[k][j] * B[i][k];
	}
	C[i][j] = val;
      }
    }

/* these print statements print out the matrix in a nicer fashion, but
 *  were commented out to leave the output compatible with the
 *  streamit output */ 
    for(j = 0; j < rows1; j++) {
      for(i = 0; i < columns2; i++) {
	//printf("%f\n", C[i][j]);
	/*        printf("%f\t", C[i][j]); */ 
      }
      /*      printf("\n"); */
    }
    /*    } */ /* end of commented while */
    print_string("done.\n");
    return 0;
}

/** currently floatSource mimics the floatSource for the streamit
 * version of MatrixMult.  That is, it repeats the sequence [0...9]
 * forever **/
float floatSource() {
  static int n = 0;
  if (n == mod_max) {
    n = 0;
  }
  return n++;
}


/** 
 * main entry point -- runs the matrix multiplication two times
 * so that we can get a fair shot at the numbers.
 **/
int main(int argc, char **argv) {
  doCalculation();
  doCalculation();
  doCalculation();
}

   

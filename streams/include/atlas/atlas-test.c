#include <stdio.h>
#include <string.h>
#include "atlas-interface.c"

int testMatrixVectorProduct() {
  int i, M=2, N=3;
  float A[6] = {1.0, 2.0, 3.0, 
		-1.0, -2.0, -3.0};
  float x[3] = {3.0,
		4.0,
		5.0};
  float b[3] = {10.0,
		11.0};
  float* y = (float*)malloc(N*sizeof(float));
  atlasMatrixVectorProduct(A, x, b, M, N, y);
  // print result -- should be 26, -26
  printf("Result:\n");
  for (i=0; i<M; i++) {
    printf("  %f\n", y[i]);
  }
  if (y[0]==36.0 && y[1]==-15.0) {
    printf("\nCorrect!\n\n");
    return 0;
  } else {
    printf("ERROR:  should have returned:\n  26.0\n  -26.0");
    return 1;
  }
}

int main() {
  return testMatrixVectorProduct();
}

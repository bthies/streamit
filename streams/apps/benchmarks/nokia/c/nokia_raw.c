#include <math.h>
// This functio performes the cholesky decomposition, the Cholesky
void choldc(float **A,float **L,int n)
{
  int i,j,k;
  float sum;

  for (i=0; i<n;i++) {
    for (j=i;j<n;j++) {
      for (sum=A[i][j],k=i-1;k>=0;k--)
	sum -= L[i][k]*L[j][k];
      if (i==j) {
	L[j][i]=sqrt(sum);
      }
      else L[j][i]=sum/p[i];
    }
  }
}

	  
  

#include <math.h>
#include <stdio.h>
#include <iostream.h>

void Decode(int Q,int N, int W, int K, float **C, float **h, float *r);
void AddSigma(int n,float sigma,float **AhA,float **AhAsig);
void MatchFilt(int m, int n, float *r, float *Ahr, float **A);
void SelfMult(int m, int n,float **A,float **AhA);
void Forw(int n, float *Ahr, float *u, float **L);
void Backs(int n, float *v, float *u, float **L);
void CompSigma(int n,float sigma, float *a, float *b);
void DelMat(int K, int W, int Q, int N, float **B, float **A);
void chold(float **A,float **L,int n);
void ConvMat(int K,int W, int Q,  float* C[], float* B[], float* h[]);
void PrintD(int n, float *d);

main() {
	int K;
	int N;
	int Q;
	int W;
	K=2;
	N=2;
	Q=2;
	W=2;
	float h[W][K];
	float C[Q][K];
	float  r[Q*N+W-1];    
	h[0][0]=1;
	h[0][1]=3;
	h[1][0]=2;
	h[1][1]=5;
	C[0][0]=1;
	C[0][1]=0;
	C[1][0]=1;
	C[1][1]=2;
	r[0]=1;
	r[1]=2;
	r[2]=3;
	r[3]=4;
	r[4]=5;
	Decode(Q,N,W,K,C,h,r);


  return 0;
}







// This function performes the complete decoding.
void Decode(int Q,int N, int W, int K, float **C, float **h, float *r)
{
  int m=N*Q+W-1;
  int n=K*N;
  
  float B[Q+W-1][K];
  float A[Q*N+W-1][K*N];
  float Ahr[K*N];
  float AhA[K*N][K*N];
  float L[K*N][K*N];
  float u[K*N];
  float v[K*N];
  float sigma;
  float AhAsig[n][n];
  
  ConvMat(K,W,Q,C,B,h);

  DelMat(K,W,Q,N,B,A);

  MatchFilt(m,n,r,Ahr,A);

  SelfMult(m,n,A,AhA);

  chold(AhA,L,n);

  Forw(n,Ahr,u,L);

  Backs(n,v,u,L);

  CompSigma(n,sigma,v,Ahr);

  AddSigma(n,sigma,AhA,AhAsig);

  chold(AhAsig,L,n);

  Forw(n,Ahr,u,L);

  Backs(n,v,u,L);

  PrintD(n,v);
}

  





//Prints elements of a vector of size n;

void PrintD(int n, float *d){
  int i;
  for (i=0 ; i <n ;i++){
    cout <<i<<":"<< d[i] << endl;
  }
}
  


//Adds sigma to the diagonal elements
void AddSigma(int n,float sigma,float **AhA,float **AhAsig)
{
  int i,j;

  for(i=0;i<n;i++)
    for(j=0;j<n;j++)
      {
	if (i==j) {
	  AhAsig[i][j]=AhA[i][j]+sigma;}
	else {AhAsig[i][j]=AhA[i][j];
	}
      }
}
	





// this does the match filtering, clear from its name!
void MatchFilt(int m, int n, float *r, float *Ahr, float **A)
{
  int i,j,k;

  for (i=0;i<n;i++)
    {
      Ahr[i]=0;
      for (j=0; j<m;j++)
	Ahr[i]+=A[j,i]*r[j];
    }
}


// This multiplies the matrix A by itself
void SelfMult(int m, int n,float **A,float **AhA)
{
  int i,j,k;

  for (i=0; i<n;i++)
    for (j=0; j<=i ;j++)
      {
	AhA[i][j]=0;
	for ( k=0; k<m;k++)
	  AhA[i][j]+=A[i][k]*A[j][k];
	AhA[j][i]=AhA[i][j];
      }
}


// This performs the Forward subtituion
void Forw(int n, float *Ahr, float *u, float **L)
{
  int i,j;
  float sum;

  for (i=0; i <n; i++){
    sum=0;
    for (j=0; j<i;j++)
      sum+=L[i][j]*u[j];
    u[i]=(Ahr[i]-sum)/L[i][i];
  }
}

// This performes the Back substituiton
void Backs(int n, float *v, float *u, float **L)
{
  int i,j;
  float sum;
  for (i=n-1;i>=0;i--){
    sum=0;
    for (j=i+1;j<n;j++)
      sum+=L[j][i]*v[j];
    v[i]=(u[i]-sum)/L[i][i];
  }
}


// this function calculates the avarage distance between two vectors
void CompSigma(int n,float sigma, float *a, float *b)
{
  int i;
  float sum=0;
  for (i=0; i<n;i++)
    sum+=(a[i]-b[i])*(a[i]-b[i]);
  sigma=sqrt(sum)/n;
}

      




// this funtion performes the convolution of matrix h and matrix C,
// h is a W by K matrix of channel responses,
// B is a W+Q-1 by K matrix of channel responses,
// C is a Q by K matirx of Channel Signitures
void ConvMat(int K,int W, int Q,  float* C[], float* B[], float* h[])
{
  int i,j,l;

  for (i=0; i < K ; i++)
    for (j=0; j < W+Q-1; j++){
      B[j][i]=0;
      for (l=0;((l < W) & ((j-l)>0) );l++)
	B[j][i]+=h[l][i]*C[j-l][i];
    }
}

//B is a Q+W-1 By K matrix
//A is a N*Q+W-1 By N*K matrix
void DelMat(int K, int W, int Q, int N, float **B, float **A)
{
  int i,j,l;
  for (i=0; i <K; i++)
    for (j=0; j<N; j++)
      for (l=0; l<Q+W-1; l++)
	A[j*Q+l][i*N+j]=B[l][i];
}

//A is an n by n matrix, so is L
void chold(float **A,float **L,int n)
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
      else L[j][i]=sum/L[i][i];
    }
  }
}

	  
  

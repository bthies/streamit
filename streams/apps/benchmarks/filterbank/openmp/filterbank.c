#include <math.h>
#ifdef raw
#include <raw.h>
#else
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#endif

//const int N_sim=2*1024;
const int N_sim=2048;
const int N_samp=8;
//const int N_ch=N_samp;
const int N_ch=8;
const int N_col=32;

void begin(void);
void FBCore(int N_samp,int N_ch, int N_col,float r[N_sim],float y[N_sim], float H[N_ch][N_col],float F[N_ch][N_col]);

static int numiters = -1;

#ifndef raw
int main(int argc, char **argv)
{
  int option;

  while ((option = getopt(argc, argv, "i:")) != -1)
  {
    switch(option)
    {
    case 'i':
      numiters = atoi(optarg);
    }
  }

  begin();
  return 0;
}
#endif

void begin(void){

  float r[N_sim];
  float y[N_sim];
  float H[N_ch][N_col];
  float F[N_ch][N_col];

		
  int i,j;

  for (i=0;i<N_sim;i++)
      r[i]=i+1;


  for (i=0;i<N_col;i++) {

      for (j=0;j<N_ch;j++){
	  H[j][i]=i*N_col+j*N_ch+j+i+j+1;

	  F[j][i]=i*j+j*j+j+i;
	
      }
  }

  while (numiters == -1 || numiters-- > 0) {
    FBCore(N_samp,N_ch,N_col,r,y,H,F);
    for (i=0;i<N_sim;i++) {
#ifdef raw
      print_float(y[i]);
#else
      printf("%f\n", y[i]);
#endif
    }
  }
}


// the FB core gets the input vector (r) , the filter responses H and F and generates the output vector(y)
void FBCore(int N_samp,int N_ch, int N_col,float r[N_sim],float y[N_sim], float H[N_ch][N_col],float F[N_ch][N_col])
{
  int i,j,k;
  for (i=0; i < N_sim;i++)
    y[i]=0;

  for (i=0; i< N_ch; i++)
    {
      float Vect_H[N_sim]; //(output of the H)
      float Vect_Dn[(int) N_sim/N_samp]; //output of the down sampler;
      float Vect_Up[N_sim]; // output of the up sampler;
      float Vect_F[N_sim];// this is the output of the 

      //convolving H
      for (j=0; j< N_sim; j++)
	{
	  Vect_H[j]=0;
	  for (k=0; ((k<N_col) & ((j-k)>=0)); k++)
	    Vect_H[j]+=H[i][k]*r[j-k];
	}

      //Down Sampling
      for (j=0; j < N_sim/N_samp; j++)
	Vect_Dn[j]=Vect_H[j*N_samp];

      //Up Sampling
      for (j=0; j < N_sim;j++)
	Vect_Up[j]=0;
      for (j=0; j < N_sim/N_samp;j++)
	Vect_Up[j*N_samp]=Vect_Dn[j];

      //convolving F
      for (j=0; j< N_sim; j++)
	{
	  Vect_F[j]=0;
	  for (k=0; ((k<N_col) & ((j-k)>=0)); k++)
	    Vect_F[j]+=F[i][k]*Vect_Up[j-k];
	}

      //adding the results to the y matrix

      for (j=0; j < N_sim; j++)
	y[j]+=Vect_F[j];
    }
}
      


      

	  

  
  



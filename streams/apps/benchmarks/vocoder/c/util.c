#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"

#define  COSNUM 15
#define  EPSILON 0.05

/* Low Pass Filter to extract Spectral Envelop. It's actually 17-point, but the first and last points have values zero. */
/*
float CosWin[COSNUM] = {0.1736, 0.3420, 0.5000, 0.6428, 0.766, 0.866, 0.9397, 0.9848, 1, 0.9848, 0.9397, 0.866, 0.766, 0.6428, 0.5, 0.342, 0.1736};
*/
float CosWin[COSNUM] = {0.1951, 0.3827, 0.5556, 0.7071, 0.8315, 0.9239, 0.9808, 1, 0.9808, 0.9239, 0.8315, 0.7071, 0.5556, 0.3827, 0.1951};

/*
float CosWin[COSNUM] = {0.3827, 0.7071, 0.9239, 1.000, 0.9239, 0.7071, 0.3827};
*/

/*Float FIR Filter Routine*/
float *FIRFilter1 (float *filter, float *data, int filterlen, int datalen)
     /* Float FIR filter routine 
	where the system is characterized by:
	y(n) = a(1)*x(n)+a(2)*x(n-1)+...+a(k)*x(n-k+1)
	and the corresponding system function:
        H(Z) = a(1)+a(2)*Z^(-1)+......+a(k)*Z^(-k+1)
	

	filter: the sequence of a(1), a(2), ...., a(k)
	data: x(n), the data that will be filtered
	filterlen: length of acoeff, should be k+1 in the previous example.
	datalen: length of data to be filtered.
	
	return: the filter result

	Implemented by convolution. Padding (filterlen-1) 0s before and after the data.
	The return has a length of datalen+filterlen-1.
     */
{
  int i, j, k ;
  float *filtrlt, *pTemp;
  
  assert(filter);
  assert(data);
  assert(filterlen > 0);
  assert(datalen >0);
  
  pTemp = filtrlt  = (float *) malloc ((datalen+filterlen-1)*sizeof(float));
 
  for (i = 0; i < datalen+filterlen-1; i++)
    {
      (*pTemp) = 0;

      for ( j = filterlen - 1 ; j >= 0 ; j--)
      {
	if ((i-j >= 0) && (i-j < datalen))
	  (*pTemp) += data[i-j]*filter[j];

      };
      pTemp++;
    }

  return filtrlt;
};



float * Spec2Env (float *Spectral, int length)
     /* Spectral to Spectral Envelop by lowpassing using CosWin defined.  
	The input is a sequence of DFT coefficients, before filtering it, 
	We make replicate the last filterlen/2 points in reverse order.
     */
{
  int i, j, k;
  float *pTemp, *envelop, *pBuf;

  assert(Spectral);
  
  envelop = (float *)malloc(length*sizeof(float));
  pTemp = pBuf = (float *)malloc((length+COSNUM/2)*sizeof(float));

  memcpy (pBuf, Spectral, length*sizeof(float));
  pTemp += length;
  for (i = 0; i < COSNUM/2; i++)
    (*pTemp++) = pBuf[length-1-i];

  pTemp = FIRFilter1(CosWin, pBuf, COSNUM, length+COSNUM/2);
  free(pBuf);
  memcpy(envelop, pTemp+COSNUM/2, length*sizeof(float));
  free(pTemp);

  return envelop;
}

/*
float *DFT2Spectral (Complex *dft, int length)
{
  float *Spectral, *pTemp;
  int i;
  
  assert(dft);
  pTemp = Spectral = (float *) malloc ( length * sizeof(float));
  assert(pTemp);
  for (i = 0; i < length ; i++)
    {
      (*pTemp) = sqrt ( (dft->rl)*(dft->rl)+(dft->im)*(dft->im));
      pTemp++;
      dft++;
      
    }

  return Spectral;
}
      
float *DFT2Phase (Complex *dft, int length)
{
  float *Phase, *pTemp;
  int i;
  
  assert(dft);

  pTemp = Phase =  (float*) malloc ( length * sizeof(float));

  for (i = 0 ; i < length ; i++)
    {
      (*pTemp++) = atan2 (dft->im, dft->rl);
      dft ++;
    }

  return Phase;
}

*/
void Rectangular2Polar ( Complex *dft, float * Magnitude, float *Phase, int length)
{

  int i;
  for ( i = 0 ; i < length; i++)
    {
      (*Phase++) = atan2(dft->im, dft->rl);
      (*Magnitude++) = sqrt ( (dft->rl)*(dft->rl)+(dft->im)*(dft->im)); 
      dft++;
    };
}
      
  

Complex* Polar2Rectangular ( float * Magnitude, float *Phase, int length)
     /* Spectral: Spectral Envelop of DFT \
	Phase: Phase sequence \
	length: length of data */
{
  Complex *dft, *pTemp;
  int i;

  pTemp = dft =  (Complex*) malloc ( length * sizeof(Complex));

  for (i = 0 ; i < length ; i++)
    {
      pTemp->rl = (*Magnitude)*cos(*Phase);
      pTemp->im = (*Magnitude++)*sin(*Phase++);
      pTemp ++;
    }
  return dft;
}


float * short2float ( short * data, int length)
{
  float * result;
  int i;

  result = (float *)malloc (length * sizeof(float));
  for (i=0; i<length; i++)
    result[i] = data[i];
  return result;
};


float * char2float ( unsigned char * data, int length)
{
  float * result;
  int i;

  result = (float *)malloc (length * sizeof(float));
  for (i=0; i<length; i++)
    result[i] = data[i];
  return result;
};

short * float2short ( float * data, int length)
{
  short * result;
  int i;

  result = (short*)malloc (length * sizeof(short));
  for (i=0; i<length; i++)
    result[i] = (short)data[i];
  return result;
};

unsigned char* float2char ( float * data, int length)
{
  unsigned char * result;
  int i;

  result = (unsigned char*)malloc (length * sizeof(char));
  for (i=0; i<length; i++)
    result[i] = (unsigned char)data[i];
  return result;
};



/* For simplicity, here is a linear interpolation routine */
float* interp(float* xn, int length,  int l)
     /* xn: the sequence to be upsampled. 
	length: the length of xn.
	l:  the factor of upsampling 

	for linear interpolation, 
                      1-|n|/l   |n| <=l
	h_lin[n] = 
	              0         otherwise
        x_lin[n] = sum (x(k)*h_lin(n-kl)), for all k.
	
	So, by interploating with a factor of l, generate (l-1) points equally spaced between x(i) and x(i+1).
	In this implementation, when i==length-1, use x(0) as x(i+1).
	The major consideration is that, when we are interpolating frequencies, this would force the interpolated frequencies add
	up to exactly l*(uniterpolated frequency sum), and hence maitains the phase continuity.
	
     */

{
  int i,j, k;
  float *x, *pTemp;

  //pTemp = x = (float *) malloc (length*l*sizeof(float));
 
  //for (i = 0 ; i < length*l; i++)
  //{
  //  (*pTemp) = 0;
  //  if (i%l) k = i/l; else k=i/l-1;
  //  for ( j = max(k, 0); j <= i/l+1; j++)
  //*pTemp += xn[j]*(1-(abs(i-j*l))/((float)l));
  //  pTemp++;
  //}

  pTemp = x = (float *) malloc (((length-1)*l+1)*sizeof(float));
  for ( i = 0 ; i < ((length-1)*l+1); i ++)
    {
      if ( i%l ) 
	(*pTemp) = xn[i/l] + (i%l)*(xn[i/l+1]-xn[i/l])/((float)l);
      else
	(*pTemp) = xn[i/l];
      
      pTemp++;
    }
  return x;
}

     
float *decimate (float *xn, int length, int l)
     /* as in interp, this routine performs a downsampling at a factor of l */
{

  float *x;
  int i, j;

  x = (float *)malloc(length/l*sizeof(float));
  for (i = 0; i< length/l; i++)
    x[i] = xn[i*l];
  return x;
}


int max(int x , int y )
{
  return (x>y)? x : y;
};


void SaveData(float *data, int datalen, char *filename)
     /* just for the sake of debugging......... */
{
  FILE *f;
  int i;

  f = fopen (filename, "a");
  assert(f);
  fseek(f, 0L, SEEK_END);

  fwrite(data, sizeof(float), datalen, f);

  fclose(f);
}
    
float average(float *data, int length)
{
  float f= 0;
  int i;

  
  for ( i = 0 ; i < length; i++)
    f += data[i];
  
  return f/length;
}


int *ratio2int ( float ratio)
{
  int *result;
  int interpfactor, decimatefactor, x, y;

  /* Round up to 1 digit behind ., and convert to ratio */
  interpfactor = floor(ratio);
  ratio = ratio - interpfactor;
  if ( ratio == 0)
    decimatefactor = 1; 
  else 
    {
      interpfactor = interpfactor * 10 + ((int)(ratio*10+0.5));
      decimatefactor = 10;
    }

  /* Find GCD */
  x = interpfactor;
  y = decimatefactor;

  while ( y > 0) 
    {
      x = x % y;
      x = x + y;
      y = x - y;
      x = x - y;
    }

  /* prepare final result and return */
  interpfactor /= x;
  decimatefactor /= x;
  
  result = ( int * ) malloc ( 2 * sizeof (int));

  result[0] = interpfactor;
  result[1] = decimatefactor;
  
  return result;
}
  













#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"

float angle;
float r = 0.998781;
float rn;

Complex * DFT ( float *data, int SampleNum, int *DataIdx, float *Buf, int BUFSIZE, int fftlen, Complex *LastSample, int BufCount)
{
  int fftlen2, i, j, idx, in;
  Complex *dftrlt;
  int BufIdx = 0;
  Complex W;


  fftlen2 = fftlen/2;
  dftrlt = (Complex *)malloc(BUFSIZE*fftlen2*sizeof(Complex));
  rn = exp(fftlen*log(r));
  angle = 2*PI/fftlen;
  
  Buf[BufIdx] = ((*DataIdx) < SampleNum ) ? data[(*DataIdx)]:0;
  
  if (BufCount == 0 )
    {
      /* Computing the First DFT Outputs, X[fftlen/2], use Equation 1 */
      /* Initialization Step of iterative process                     */
      for ( i = 0; i < fftlen2 ; i++)
	{
	  dftrlt[i].rl = dftrlt[i].im = 0;
	  idx = BufIdx - fftlen +1 < 0 ? BufIdx - fftlen +1 + BUFSIZE : BufIdx - fftlen+1;
	  for (j = fftlen2-fftlen+1; j <= fftlen2; j ++)
	    {
	      W.rl = cos(i*(j-fftlen2+fftlen-1)*angle);
	      W.im = sin(i*(j-fftlen2+fftlen-1)*angle);
	     
	      
	      dftrlt[i].rl += Buf[idx]*(W.rl);
	      dftrlt[i].im += Buf[idx]*(W.im);
	      idx ++;
	      if (idx == BUFSIZE)
		idx = 0;
	    }
	} // end of computing the first X[fftlen/2]
    }
  else /* Compute initial value based on LastSample and currently, continue iterative step */
    {
      for (i = 0 ; i < fftlen2; i++)
	{
	  W.rl = cos(i*angle);
	  W.im = -sin(i * angle);
	  dftrlt[i].rl = r*(W.rl*(LastSample[i].rl) - W.im*(LastSample[i].im));
	  dftrlt[i].im = r*(W.rl*(LastSample[i].im) + W.im*(LastSample[i].rl));
	  
	  
	  dftrlt[i].rl += W.rl*Buf[BufIdx];
	  dftrlt[i].im += W.im*Buf[BufIdx];
	      
	  in = BufIdx - fftlen < 0 ? BufIdx -fftlen+BUFSIZE: BufIdx-fftlen;
	  dftrlt[i].rl -= W.rl*rn*Buf[in];
	  dftrlt[i].im -= W.im*rn*Buf[in];
	}
    }
     
  /* Intial Value has been set, iteratively computing till the buffer filled up */


  BufIdx++; (*DataIdx)++;

  while ((BufIdx < BUFSIZE) & ((*DataIdx) < SampleNum+fftlen2))
    {
      Buf[BufIdx] = ((*DataIdx) < SampleNum ) ? data[(*DataIdx)]:0;
      for (j = 0 ; j < fftlen2; j++)
	{
	  W.rl = cos(j*angle);
	  W.im = -sin(j * angle);
	  dftrlt[BufIdx*fftlen2+j].rl = r*(W.rl*dftrlt[(BufIdx-1)*fftlen2+j].rl - W.im*dftrlt[(BufIdx-1)*fftlen2+j].im);
	  dftrlt[BufIdx*fftlen2+j].im = r*(W.rl*dftrlt[(BufIdx-1)*fftlen2+j].im + W.im*dftrlt[(BufIdx-1)*fftlen2+j].rl);
	  
	  dftrlt[BufIdx*fftlen2+j].rl += W.rl*Buf[BufIdx];
	  dftrlt[BufIdx*fftlen2+j].im += W.im*Buf[BufIdx];
	  
	  in = BufIdx - fftlen < 0 ? BufIdx -fftlen+BUFSIZE: BufIdx-fftlen; 
	  dftrlt[BufIdx*fftlen2+j].rl -= W.rl*rn*Buf[in];
	  dftrlt[BufIdx*fftlen2+j].im -= W.im*rn*Buf[in];
	}
	     
      BufIdx++; (*DataIdx)++;
    };
  memcpy ( LastSample, dftrlt+(BUFSIZE-1)*fftlen2, fftlen2*sizeof(Complex));
  return dftrlt;
}
  

Complex  *AddCosWin (Complex * dftrlt1,  int fftlen, int datalen)
{
  Complex *pTemp, *dftrlt;
  int i, j, fftlen2; 

  fftlen2 = fftlen/2;
  dftrlt = (Complex *)malloc(datalen*fftlen2*sizeof(Complex));
   /*Windowing: By convolve the DFT sequence of each sample with the (-1/4, 1/2, -1/4) sequence */
  pTemp = dftrlt;
  for (i = 0 ; i < datalen; i++)
    {
      
      for ( j = 0 ; j < fftlen2; j++)
	{
	  if (j == 0 )
	    {
	      pTemp->rl = (dftrlt1[i*fftlen2+j].rl - dftrlt1[i*fftlen2+j+1].rl)/4;
	      pTemp->im = (dftrlt1[i*fftlen2+j].im - dftrlt1[i*fftlen2+j+1].im)/4;
	    }
	    else 
	  if (j < fftlen2 - 1)
	    {
	      pTemp->rl = ((dftrlt1+i*fftlen2+j)->rl)/2 - (((dftrlt1+i*fftlen2+j-1)->rl)+((dftrlt1+i*fftlen2+j+1)->rl))/4;
	      pTemp->im = ((dftrlt1+i*fftlen2+j)->im)/2 - (((dftrlt1+i*fftlen2+j-1)->im)+((dftrlt1+i*fftlen2+j+1)->im))/4;
	    }
	  
	  /* Note: here the convolution requires coefficients DFT[fftlen/2..fftlen/2+1] */
	  /* We have to look BACK a little as we stored only DFT[0..fftlen/2-1], and we know that the sequence is symmetric. */
	  else if (j == fftlen2 - 1)
	    {
	      pTemp->rl = (pTemp-1)->rl;
	      pTemp->im = (pTemp-1)->im;
	    }
	  pTemp ++;
	}
    };
 
  return dftrlt;
}

/*Inverse DFT, reconstruct speech data using FFT result*/
float * InvDFT1 (Complex *dftrlt, int fftlen, int datalen)
     /* Inverse DFT by adding signed real part, compute the central point */
{
  int i, j, k;
  float *data;
  
  data  = (float *)malloc (datalen*sizeof(float));
  for (i = 0; i < datalen; i++)
    {
      data[i] = 0;
      k = 1;
      for (j = 0; j < fftlen; j++)
	{
	  if (k)
	    {
	      data[i] += dftrlt->rl;
	      k = 0;
	    }
	  else 
	    { 
	      data[i] -= dftrlt->rl;
	      k = 1;
	    }
	  dftrlt++;
	}
      data[i] /= fftlen;
    }
  return data;
}

float * InvDFT2 (Complex *dftrlt, int fftlen, int datalen)
     /* Inverse DFT by summing up all real part, corresponds to a rotated cosine window */
{
  int i, j, k;
  float *data;

  data = (float *)malloc (datalen*sizeof(float));
  assert(data);
  
  for (i = 0; i < datalen; i++)
    {
      data[i]  = 0;
      for (j = 0; j < fftlen; j++)
	{
	  data[i] += dftrlt->rl;
	  dftrlt++;
	}
      data[i] /= fftlen;
    }

  return data;
}


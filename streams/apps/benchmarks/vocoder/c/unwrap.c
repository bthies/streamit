#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"

void unwrap1(float *phase, int datalen, int fftnum, int fftlen, int BufCount, float *lastpha, int DownSample)
{
  int i, j, k , startidx; 
  float f, offset, discontinuity;
  float step, preestimate, estimate , newphase;
  
  offset = 2*PI;
  for ( i = 0 ; i < fftnum; i++)
    {
      if (DownSample)
	{
	  step = -2*PI*(i+0.5)/fftlen;
	  discontinuity = i > fftlen/4 ? 2*PI : PI;
	}
      else 
	{
	  step = -2*PI*(2*i+1.5)/fftlen;
	  discontinuity = i > fftlen/2 ? 2*PI : PI;
	};
      
      if (BufCount ==0 )
	{
	  estimate = phase[i];
	  startidx = 1;
	}
      else 
	{
	  estimate = lastpha[i];
	  startidx = 0;
	}
      
      for ( j = startidx; j<datalen; j++)
	{
	  estimate += step;
	  k = (int)((estimate - phase[j*fftnum+i]) / offset);
	  phase[j*fftnum+i] += k*offset;
	  while ( phase[j*fftnum+i] - estimate > discontinuity)
	    phase[j*fftnum+i] -= offset;
	  while ( estimate - phase[j*fftnum+i] > discontinuity )
	    phase[j*fftnum+i] += offset;
	}
    };
  
  for ( i = 0 ; i < fftnum; i++)
    {
      
      if (DownSample)
	discontinuity = i > fftlen/4 ? 2*PI : PI;
      else 
	discontinuity = i > fftlen/2 ? 2*PI : PI;

      
      for ( j = 1; j < datalen; j++)
	{
	  while ( phase[j*fftnum+i] -phase[(j-1)*fftnum+i] > discontinuity )
	    phase[j*fftnum+i] -= offset;

	  while ( phase[j*fftnum+i] - phase[(j-1)*fftnum+i] < -discontinuity )
	    phase[j*fftnum +i] += offset;
      }
    }
};
/*
void unwrap1 (float *phase, int datalen, int fftnum, int fftlen, int BufCount, float *lastpha, int DownSample)
{
  int i, j, k, startidx;
  float f, offset, discontinuity;
  float step, preestimate, estimate, newphase, newestimate;

  offset = 2*PI;

  for (i = 0; i < fftnum; i++)
    {
      if (DownSample)
	step = -offset*(i+0.5)/fftlen;
      else
	step = -offset*(2*i+1.5)/fftlen;
    
      if (BufCount > 0 )
	{
	  estimate = lastpha[i] + step;
	  k = (int)((estimate - phase[i])/offset) ;
	  phase[i] += k*offset;
	  while ( phase[i] - estimate > PI )
	    phase[i] -= offset;
	  while ( estimate - phase[i] > PI )
	    phase[i] += offset;
	}
      else 
	estimate = phase[i];
      
      for ( j = 1 ; j < datalen; j++)
	{
	  estimate += step;
	  k = (int) ((estimate - phase[j*fftnum+i])/offset) ;
	  phase[j*fftnum+i] += k*offset;
	  while (phase[j*fftnum+i]  - estimate > PI)
	    phase[j*fftnum+i] -= offset;
	  while (estimate - phase[j*fftnum+i] > PI)
	    phase[j*fftnum+i] += offset;
	}
    };
  
  
  for (i = 0; i < fftnum ; i++)
    for ( j = 1; j < datalen; j++)
      {
	while (phase[j*fftnum+i] - phase[(j-1)*fftnum+i] > PI)
	  phase[j*fftnum+i] -= offset;
	
	while (phase[(j-1)*fftnum+i] - phase[j*fftnum+i] > PI)
	  phase[j*fftnum+i] += offset;
      }

}
*/

void unwrap2 (float *phase, int datalen, int fftnum, int fftlen, int DownSampleFlag)
{
  int i, j, k;
  float discontinuity, offset, f;
  
  offset = 2*PI;

  if (DownSampleFlag)
    k = fftlen/4;
  else k = fftlen/2;

  for (i = 0; i < fftnum ; i++)
    {
      if ( i < k)
	discontinuity = PI;
      else 
	discontinuity = offset;
      
      f = 0;
      for ( j = 1; j < datalen; j++)
	{
	  
	  phase[j*fftnum+i] += f;
	  while (phase[j*fftnum+i] -phase[(j-1)*fftnum+i] > discontinuity)
	    {
	      phase[j*fftnum+i] -= offset;
	       f -= offset;
	    }

	  while (phase[(j-1)*fftnum+i] -phase[j*fftnum+i] > discontinuity)
	    {
	      phase[j*fftnum+i] += offset;
	       f += offset;
	    }
	}
    };
}











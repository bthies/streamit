#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"
#include "extern.h"

float *pitchlastpha1, *pitchlastpha2;

void InitPitch (int fftnum)
{
  
  pitchlastpha1 = malloc ( fftnum*sizeof(float));
  pitchlastpha2 = malloc ( fftnum*sizeof(float));

}

void CleanPitch ()
{
  free(pitchlastpha1);
  free(pitchlastpha2);
}

float *Pitch ( float *magnitude, float *phase, int fftnum, int fftlen, int datalen, int BufCount, int DownSample)
{

  Complex *dftrlt;
  float *phase1, *magnitude1;
  float *envelop;
  float *excite;

  int i, j, k;
  float *pTemp, *pTemp1;
  float *result, *phasediff;
  int newfftnum, interpnum, decimatenum;
  float frequencyfactor;
  int idx;

  if (BufCount <=13 ) 
    frequencyfactor = 1;
  else 
    {
      frequencyfactor = 1.0/(1+(BufCount-12)*0.02);
      frequencyfactor = 1.0/(1+(BufCount-13)*0.02);
    }

  
  phase1 = (float *)malloc ( datalen*fftnum*sizeof(float));
  assert(phase1);
/*    printf ("BufCount = %d, DATALEN = %d, frequencefactor = %5.3f , fftnum = %d \n", BufCount, datalen, frequencyfactor , fftnum); */
  /*
  for (i=0; i<datalen; i++)
    {
      pTemp = Spec2Env(magnitude+i*fftnum, fftnum);
      memcpy (envelop+i*fftnum, pTemp, fftnum*sizeof(float));
      free(pTemp);
    };
  
  for (i = 0; i<datalen; i++)
    for (j = 0; j<newfftnum; j++)
      {
	if (envelop[i*fftnum+j] == 0 )
	  excite[i*newfftnum+j] = 0;
	else
	  excite[i*newfftnum+j] = magnitude[i*fftnum+j] / envelop[i*fftnum+j];
	phase1[i*newfftnum+j] = phase[i*fftnum+j];

      };
  */
  for (i = 0; i<datalen; i++)
    for (j = 0; j<fftnum; j++)
      {
	phase1[i*fftnum+j] = phase[i*fftnum+j];
	
      };

  /*
  printf("OK\n");
  
  printf("BufCount = %d" , BufCount);
  */
  phasediff = (float *)malloc (datalen*fftnum*sizeof(float)); 
  if (BufCount == 0)
    {
      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phasediff[(i-1)*fftnum+j] = (phase1[i*fftnum+j]-phase1[(i-1)*fftnum+j])*frequencyfactor;

/*        printf("OK1, "); */
      for (i = 0 ; i< fftnum; i++)
	pitchlastpha1[i] = phase1[(datalen-1)*fftnum+i];

/*        printf("OK2, "); */

      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phase1[i*fftnum+j] = phase1[(i-1)*fftnum+j]+phasediff[(i-1)*fftnum+j];

/*        printf("OK3, "); */
      for (i = 0 ; i< fftnum; i++)
	pitchlastpha2[i] = phase1[(datalen-1)*fftnum+i];
      
/*        printf("OK4,\n"); */

    }
  else 
    {
      for (i = 0; i < fftnum; i++)
	phasediff[i] = ( phase1[i]-pitchlastpha1[i])*frequencyfactor;

      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phasediff[i*fftnum+j] = (phase1[i*fftnum+j]-phase1[(i-1)*fftnum+j])*frequencyfactor;

      for (i = 0 ; i< fftnum; i++)
	pitchlastpha1[i] = phase1[(datalen-1)*fftnum+i];

      for (i = 0; i< fftnum; i++)
	phase1[i] = pitchlastpha2[i]+phasediff[i];
      
      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phase1[i*fftnum+j] = phase1[(i-1)*fftnum+j]+phasediff[i*fftnum+j];
      
      for (i = 0 ; i< fftnum; i++)
	pitchlastpha2[i] = phase1[(datalen-1)*fftnum+i];
   
    }


  /*****************************************************/
  /**********if we downsampled the DFT coefficients, newfftlen should be 43*/

  /*
  magnitude1 = (float *) malloc ( datalen*fftnum*sizeof(float));
  for (i = 0; i<datalen; i++)
    {
      pTemp = interp (envelop+i*fftnum, fftnum, interpnum);
      pTemp1 = decimate ( pTemp, fftnum*interpnum, decimatenum);
      memcpy (magnitude1+i*fftnum, pTemp1, fftnum*sizeof(float));
      free(pTemp);
      free(pTemp1);
      
      for (j = 0; j<fftnum; j++)
	magnitude1[i*fftnum+j] *= excite[i*fftnum+j];
    }

  free(excite);
  free(envelop);
  free(phasediff);
  */
  /*
  printf ("OK\n");
  */
  dftrlt = Polar2Rectangular(magnitude, phase1, datalen*fftnum);
  free(phase1);


  if (DownSample)
    result = InvDFT2(dftrlt, fftnum, datalen);
  else
    result = InvDFT1(dftrlt, fftnum, datalen);

  free(dftrlt);
  return (result);
}








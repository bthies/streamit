#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"
#include "extern.h"

float *male2female (float * magnitude, float * phase, int fftnum, int fftlen, int datalen, int BufCounnt, int DownSample);
float *female2male (float * magnitude, float * phase, int fftnum, int fftlen, int datalen, int BufCounnt, int DownSample);

float *flastpha1, *flastpha2;
float *mlastpha1, *mlastpha2;

void InitConv (int  fftnum)
{
   int newfftnum, interpnum, decimatenum;
   float frequencyfactor;

  interpnum = 2; decimatenum = 3; frequencyfactor = 1.8;
  newfftnum = fftnum*interpnum/decimatenum;
  
  mlastpha1 = (float *) malloc ( (newfftnum)*sizeof(float));
  mlastpha2 = (float *) malloc ( (newfftnum)*sizeof(float));

  interpnum = 25; decimatenum = 18; frequencyfactor = 0.6;

  newfftnum = (int)((float)fftnum/(float)frequencyfactor);
  newfftnum = fftnum*interpnum/decimatenum;
  flastpha1 = (float *) malloc ( (newfftnum)*sizeof(float));
  flastpha2 = (float *) malloc ( (newfftnum)*sizeof(float));
}
  

void CleanConv()
{
  free(mlastpha1);
  free(mlastpha2);
  free(flastpha1);
  free(flastpha2);
}
  
  
  

float *Conv ( float *magnitude, float *phase, int fftnum, int fftlen, int datalen, char Command, int BufCount, int DownSample)
{
 
  if (Command == 'm')
    return male2female(magnitude, phase, fftnum, fftlen, datalen, BufCount, DownSample);
  else
    return female2male(magnitude, phase, fftnum, fftlen, datalen, BufCount, DownSample);
 

}



/*******************************************************************************************************************************/
float *male2female (float * magnitude, float * phase, int fftnum, int fftlen, int datalen, int BufCount, int DownSample)
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


  interpnum = 2; decimatenum = 3; frequencyfactor = 1.8;
  newfftnum = fftnum*interpnum/decimatenum;
  envelop = (float *)malloc (datalen*fftnum*sizeof(float));
/*    printf("datalen = %d fftnum = %d \n", datalen, fftnum); */
  /*  excite = (float *)malloc ( datalen*newfftnum*sizeof(float)); */
  excite = (float *)malloc ( datalen*fftnum*sizeof(float));
  phase1 = (float *)malloc ( datalen*newfftnum*sizeof(float));

/*    printf ("fftnum = %d, newfftnum = %d, \n", fftnum, newfftnum); */
  for (i=0; i<datalen; i++)
    {
      pTemp = Spec2Env(magnitude+i*fftnum, fftnum);
      memcpy (envelop+i*fftnum, pTemp, fftnum*sizeof(float));
      free(pTemp);
    };
  
  for (i = 0; i<datalen; i++)
    for (j = 0; /* j<newfftnum; */ j < fftnum;  j++)
      {
	if (envelop[i*fftnum+j] == 0 )
	  /*excite[i*newfftnum+j] = 0; */
	  excite[i*fftnum+j] = 0;
	else
	  /*excite[i*newfftnum+j] = magnitude[i*fftnum+j] / envelop[i*fftnum+j];*/
	  excite[i*fftnum+j] = magnitude[i*fftnum+j] / envelop[i*fftnum+j];
	if ( j < newfftnum ) phase1[i*newfftnum+j] = phase[i*fftnum+j];

      };
  
/*    SaveData(envelop, datalen*fftnum, "env.dat"); */
/*    SaveData(excite, datalen*fftnum, "exc.dat"); */

  
  
  phasediff = (float *)malloc (datalen*newfftnum*sizeof(float)); 
  if (BufCount == 0)
    {
      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phasediff[(i-1)*newfftnum+j] = (phase1[i*newfftnum+j]-phase1[(i-1)*newfftnum+j])*frequencyfactor;

      for (i = 0 ; i< newfftnum; i++)
	flastpha1[i] = phase1[(datalen-1)*newfftnum+i];


      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phase1[i*newfftnum+j] = phase1[(i-1)*newfftnum+j]+phasediff[(i-1)*newfftnum+j];

      for (i = 0 ; i< newfftnum; i++)
	flastpha2[i] = phase1[(datalen-1)*newfftnum+i];

    }
  else 
    {
      for (i = 0; i < newfftnum; i++)
	phasediff[i] = ( phase1[i]-flastpha1[i])*frequencyfactor;

      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phasediff[i*newfftnum+j] = (phase1[i*newfftnum+j]-phase1[(i-1)*newfftnum+j])*frequencyfactor;

      for (i = 0 ; i< newfftnum; i++)
	flastpha1[i] = phase1[(datalen-1)*newfftnum+i];

      for (i = 0; i< newfftnum; i++)
	phase1[i] = flastpha2[i]+phasediff[i];
      
      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phase1[i*newfftnum+j] = phase1[(i-1)*newfftnum+j]+phasediff[i*newfftnum+j];
      
      for (i = 0 ; i< newfftnum; i++)
	flastpha2[i] = phase1[(datalen-1)*newfftnum+i];
   
    }


  /*****************************************************/
  /**********if we downsampled the DFT coefficients, newfftlen should be 43*/

  magnitude1 = (float *) malloc ( datalen*newfftnum*sizeof(float));
  for (i = 0; i<datalen; i++)
    {
      pTemp = interp (envelop+i*fftnum, fftnum, interpnum);
      pTemp1 = decimate ( pTemp, fftnum*interpnum, decimatenum);
      memcpy (magnitude1+i*newfftnum, pTemp1, newfftnum*sizeof(float));
      free(pTemp);
      free(pTemp1);
      
      for (j = 0; j<newfftnum; j++)
	/*magnitude1[i*newfftnum+j] *= excite[i*newfftnum+j];*/
	magnitude1[i*newfftnum+j] *= excite[i*fftnum+j];
    }
  

  free(excite);
  free(envelop);
  free(phasediff);

  dftrlt = Polar2Rectangular(magnitude1, phase1, datalen*newfftnum);
  free(phase1);
  free(magnitude1);

/*    SaveData(magnitude, datalen*fftnum, "mag0.dat"); */




  if (DownSample)
    result = InvDFT2(dftrlt, newfftnum, datalen);
  else
    result = InvDFT1(dftrlt, newfftnum, datalen);

  free(dftrlt);
  return (result);
}



float *female2male (float * magnitude, float * phase, int fftnum, int fftlen, int datalen, int BufCount, int DownSample)
{
  Complex *dftrlt;
  float *phase1;
  float *envelop;
  float *excite;

  int i, j, k;
  float *pTemp, *pTemp1;
  float *result, *phasediff;
  int newfftnum, interpnum, decimatenum;
  float frequencyfactor;
  float *magnitude1;
  int idx;

  interpnum = 25; decimatenum = 18; frequencyfactor = 0.6;
  newfftnum = fftnum*interpnum/decimatenum;
  envelop = (float *)malloc (datalen*fftnum*sizeof(float));
  excite = (float *)malloc ( datalen*newfftnum*sizeof(float));
  phase1 = (float *)malloc ( datalen*newfftnum*sizeof(float));

  for (i=0; i<datalen; i++)
    {
      pTemp = Spec2Env(magnitude+i*fftnum, fftnum);
      memcpy (envelop+i*fftnum, pTemp, fftnum*sizeof(float));
      free(pTemp);
    };

  /*calculating the first pi/2 excitation and copy them 3 times */
  /*Also duplicate the phase  */
  for (i = 0; i<datalen; i++)
    for (j = 0; j<fftnum/2; j++)
      {
	if (envelop[i*fftnum+j] == 0 )
	  excite[i*newfftnum+j] = 0;
	else
	  excite[i*newfftnum+j] = magnitude[i*fftnum+j] / envelop[i*fftnum+j];
	
	excite[i*newfftnum+j+fftnum/2] = excite[i*newfftnum+j];
	phase1[i*newfftnum+j] = phase[i*fftnum+j];
	phase1[i*newfftnum+j+fftnum/2] = phase1[i*newfftnum+j]+phase[i*fftnum+fftnum/2-1];
      };
  
  for ( i = 0 ; i < datalen ; i++)
    for (j = 0; j < newfftnum - fftnum; j++)
      excite[i*newfftnum + j + fftnum ] = excite[i*newfftnum+j%fftnum];
  
  for ( i = 0 ; i < datalen ; i++)
    {
      idx = 0;
      k = 2;
      j = fftnum;
      while (j < newfftnum )
	{
	  phase1[i*newfftnum +j ] = phase1[i*newfftnum + idx ] + k*phase[i*fftnum+fftnum/2-1];
	  idx ++, j++;
	  if (idx == fftnum ) 
	    {
	      k ++; 
	      idx = 0 ;
	    }
	}
    }

  /*
  unwrap2(phase1, datalen, newfftnum, fftlen, DownSample);
  SaveData ( phase1, newfftnum*datalen, "pha1.dat");
  */
  
  unwrap1(phase1, datalen, newfftnum, fftlen, BufCount, flastpha1, DownSample);
  
  phasediff = (float *)malloc (datalen*newfftnum*sizeof(float)); 
  if (BufCount == 0)
    {
      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phasediff[(i-1)*newfftnum+j] = (phase1[i*newfftnum+j]-phase1[(i-1)*newfftnum+j])*frequencyfactor;

      for (i = 0 ; i< newfftnum; i++)
	flastpha1[i] = phase1[(datalen-1)*newfftnum+i];


      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phase1[i*newfftnum+j] = phase1[(i-1)*newfftnum+j]+phasediff[(i-1)*newfftnum+j];

      for (i = 0 ; i< newfftnum; i++)
	flastpha2[i] = phase1[(datalen-1)*newfftnum+i];

    }
  else 
    {
      for (i = 0; i < newfftnum; i++)
	phasediff[i] = ( phase1[i]-flastpha1[i])*frequencyfactor;

      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phasediff[i*newfftnum+j] = (phase1[i*newfftnum+j]-phase1[(i-1)*newfftnum+j])*frequencyfactor;

      for (i = 0 ; i< newfftnum; i++)
	flastpha1[i] = phase1[(datalen-1)*newfftnum+i];

      for (i = 0; i< newfftnum; i++)
	phase1[i] = flastpha2[i]+phasediff[i];
      
      for (i = 1; i<datalen; i++)
	for (j = 0; j<newfftnum; j++)
	  phase1[i*newfftnum+j] = phase1[(i-1)*newfftnum+j]+phasediff[i*newfftnum+j];
      
      for (i = 0 ; i< newfftnum; i++)
	flastpha2[i] = phase1[(datalen-1)*newfftnum+i];
   
    }

  k = fftnum*interpnum/decimatenum;
  magnitude1 = (float *) malloc ( datalen*newfftnum*sizeof(float));
  for (i = 0; i<datalen; i++)
    {
      
      pTemp = interp (envelop+i*fftnum, fftnum, interpnum);
      pTemp1 = decimate ( pTemp, fftnum*interpnum, decimatenum);
      memcpy (magnitude1+i*newfftnum, pTemp1, k*sizeof(float));
      free(pTemp);
      free(pTemp1);
    
      for (j = 0; j<newfftnum; j++)
	magnitude1[i*newfftnum+j] *= excite[i*newfftnum+j];
    }

  
  free(excite);
  free(envelop);
  free(phasediff);
   
  dftrlt = Polar2Rectangular(magnitude1, phase1, datalen*newfftnum);
  free(phase1);
  free(magnitude1);

  if (DownSample)
    result = InvDFT2(dftrlt, newfftnum, datalen);
  else
    result = InvDFT1(dftrlt, newfftnum, datalen);

  free(dftrlt);
  return (result);

}








#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "dft.h"
#include "extern.h"

float *lastinterppha, *lastinterpmag;

void InitSpeed(int fftnum)
{
  lastinterppha = (float *)malloc(fftnum*sizeof(float));
  lastinterpmag = (float *)malloc(fftnum*sizeof(float));
  memset ( lastinterpmag, 0, fftnum*sizeof(float));
  memset ( lastinterppha, 0, fftnum*sizeof(float));
}

void CleanSpeed()
{
  free(lastinterppha);
  free(lastinterpmag);
}

float *speedup ( float* magnitude, float *phase, int fftnum, int fftlen, int* length,  float  speedfactor, int BufCount, float *lastmag, float * lastpha, int DownSample)
{
 
  Complex *dftrlt;
  float *phasediff, *result;
  int i, j, k, m,datalen, decimafactor, interpfactor, *param;
  float *magnitude1, *phase1, *pTemp, *pTemp1, *pTemp2 ;
  
  datalen = (*length);
  assert(magnitude);
  assert(phase);

  //intf("%d \n" , datalen);
  if (speedfactor == 1)
    {
      dftrlt = Polar2Rectangular(magnitude, phase, (*length)*fftnum);
      result = InvDFT2(dftrlt, fftnum, (*length));
      free(dftrlt);
      return result;
    };

  param = ratio2int(speedfactor);
  interpfactor = param[0];
  decimafactor = param[1];
  free(param);

  phasediff = (float *) malloc (datalen*fftnum*sizeof(float));
  if (BufCount == 0 )
    {
      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phasediff[j*datalen+i-1] = (phase[i*fftnum+j]-phase[(i-1)*fftnum+j]);
      for (j = 0 ; j < fftnum; j++)
	phasediff[j*datalen+i-1] = average(phasediff+j*datalen, datalen-1);
    }
  else 
    {
      for (i = 1; i<datalen; i++)
	for (j = 0; j<fftnum; j++)
	  phasediff[j*datalen+i] = (phase[i*fftnum+j]-phase[(i-1)*fftnum+j]);
      for (j = 0; j < fftnum; j++)
	phasediff[j*datalen] = (phase[j] - lastpha[j]);
    }

 
  pTemp = (float * ) malloc((datalen+1)*sizeof(float));

  /*
  for (i = 0; i<fftnum; i++)
    for (j = 0; j<datalen; j++)
      phasediff[i*datalen+j] = phasediff[i*datalen+j]*interpfactor/decimafactor;
  */
  if (BufCount == 0 )
    (*length) = (datalen*interpfactor-interpfactor+1);
  else (*length) =  datalen*interpfactor;

  if (decimafactor > 1 ) 
    (*length) = (*length)/decimafactor+(*length)%decimafactor;

  
  magnitude1 = (float *) malloc ((*length)*fftnum*sizeof(float));
  phase1 = (float *) malloc ((*length)*fftnum*sizeof(float));
  
  if (BufCount == 0 )
    for (i = 0; i< fftnum; i++)
      {
	
	for (j = 0; j < datalen; j++)
	  pTemp[j] = magnitude[j*fftnum+i];
	pTemp1 = interp(pTemp, datalen, interpfactor);
	if (decimafactor > 1)
	  pTemp2 = decimate(pTemp1, datalen*interpfactor-interpfactor+1, decimafactor);
	else pTemp2 = pTemp1;
	
	for ( j = 0 ; j <  (datalen*interpfactor-interpfactor+1)/decimafactor; j++)
	  magnitude1[j*fftnum+i] = pTemp2[j];
	k = (datalen*interpfactor-interpfactor+1)%decimafactor;
	for ( m = 0; m < k ; m++)
	  magnitude1[(m+j)*fftnum+i] = pTemp1[j*decimafactor+m];

	//for (j = 0; j < (*length); j++)
	//magnitude1[j*fftnum+i] = pTemp2[j];
	free(pTemp1);
	if (decimafactor >1)
	  free(pTemp2);
	
	pTemp1 = interp(phasediff+i*datalen, datalen, interpfactor);
	if (decimafactor > 1)
	  pTemp2 = decimate(pTemp1, datalen*interpfactor-interpfactor+1, decimafactor);
	else pTemp2 = pTemp1;
	
	phase1[i] = phase[i];
	for ( j = 1 ; j <  (datalen*interpfactor-interpfactor+1)/decimafactor; j++)
	  phase1[j*fftnum+i] = phase1[(j-1)*fftnum+i]+pTemp2[j-1];
	k = (datalen*interpfactor-interpfactor+1)%decimafactor;
	for ( m = 0; m < k ; m++)
	  phase1[(m+j)*fftnum+i] = phase1[(m+j-1)*fftnum+i]+pTemp1[j*decimafactor+m];
	//for (j = 1; j< (*length); j++)
	//phase1[j*fftnum+i] = phase1[(j-1)*fftnum+i]+pTemp2[j-1];
	
	
	free(pTemp1);
	if (decimafactor >1)
	  free(pTemp2);
	
	lastinterpmag[i] = magnitude1[((*length)-1)*fftnum+i];
	lastinterppha[i] = phase1[((*length)-1)*fftnum+i];
      }
  else 
    for (i = 0; i<fftnum; i++)
      {


	pTemp[0] = lastinterpmag[i];
	for (j = 1; j <= datalen; j++)
	  pTemp[j] = magnitude[(j-1)*fftnum+i];
	
	pTemp1 = interp(pTemp, datalen+1, interpfactor);
	if ( decimafactor > 1 )
	  pTemp2 = decimate(pTemp1+1, datalen*interpfactor, decimafactor);
	else 	pTemp2 = pTemp1;
	
	for ( j = 0 ; j <  datalen*interpfactor/decimafactor; j++)
	  magnitude1[j*fftnum+i] = pTemp2[j];
	k = (datalen*interpfactor)%decimafactor;
	for ( m = 0; m < k ; m++)
	  magnitude1[(m+j)*fftnum+i] = pTemp1[j*decimafactor+m+1];

	//for (j = 0; j < (*length); j++)
	//magnitude1[j*fftnum+i] = pTemp2[j+1];
	free(pTemp1);
	if (decimafactor > 1) 
	  free(pTemp2);
	
	pTemp1 = interp(phasediff+i*datalen, datalen, interpfactor);
	if ( decimafactor > 1 )
	  pTemp2 = decimate(pTemp1, (datalen-1)*interpfactor+1, decimafactor);
	else 	pTemp2 = pTemp1;
	
	phase1[i] = lastinterppha[i]+pTemp2[0];
	

	
	for (j = 1; j< ((datalen-1)*interpfactor+1)/decimafactor; j++)
	  phase1[j*fftnum+i] = phase1[(j-1)*fftnum+i]+pTemp2[j];
	

	k = ((datalen-1)*interpfactor+1)%decimafactor;
	for ( m = 0; m < k ; m++)
	  phase1[(m+j)*fftnum+i] = phase1[(m+j-1)*fftnum+i]+pTemp1[j*decimafactor+m];
	
	

	//intf("%d, %d\n", ((datalen-1)*interpfactor+1)/decimafactor+k, (*length));
	for ( m = ((datalen-1)*interpfactor+1)/decimafactor+k ; m < (*length) ; m++)
	  phase1[m*fftnum+i] = 2*phase1[(m-1)*fftnum+i]-phase1[(m-2)*fftnum+i];

	


	free(pTemp1);
	if (decimafactor > 1)
	  free(pTemp2);
	
	lastinterpmag[i] = magnitude1[((*length)-1)*fftnum+i];
	lastinterppha[i] = phase1[((*length)-1)*fftnum+i];


      }

  free(pTemp);
  free(phasediff);

  dftrlt = Polar2Rectangular(magnitude1, phase1, (*length)*fftnum);
  //printf("here\n");
  
  if (magnitude1) free(magnitude1);
  if (phase1) free(phase1);

  if (DownSample)
    result = InvDFT2(dftrlt, fftnum, (*length));
  else
    result = InvDFT1(dftrlt, fftnum, (*length));

  /*  result = InvDFT2(dftrlt, fftnum, (*length));*/
  free(dftrlt);


  return result;
}















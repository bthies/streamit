#include "Globals.h"
#include "PC.h"
#include <stdio.h>

void PC_create(PC_Data* this)
{
  vsip_cvview_f* pulseShape = vsip_cvcreate_f(PULSE_SIZE, VSIP_MEM_NONE);
  vsip_cvview_f* predecPulseShape = vsip_cvcreate_f(PREDEC_PULSE_SIZE, VSIP_MEM_NONE);

  this->time = 0;
  this->numPulses     = 0;
  this->matchedFilter = vsip_cvcreate_f((NUM_RANGES/(FINE_DECIMATION_RATIO*
						     COARSE_DECIMATION_RATIO))*NUM_SEGMENTS, VSIP_MEM_NONE);
  this->concatData    = vsip_cmcreate_f(NUM_BEAMS,
					(NUM_RANGES/(FINE_DECIMATION_RATIO*
						     COARSE_DECIMATION_RATIO))*NUM_SEGMENTS,
					VSIP_ROW, VSIP_MEM_NONE);
  this->fstHalfData   = vsip_cmsubview_f(this->concatData, 0, 0, NUM_BEAMS,
					 (NUM_RANGES/(FINE_DECIMATION_RATIO*
						      COARSE_DECIMATION_RATIO)));
  this->sndHalfData   = vsip_cmsubview_f(this->concatData, 0, (NUM_RANGES/(FINE_DECIMATION_RATIO*
								     COARSE_DECIMATION_RATIO)),
					 NUM_BEAMS,
					 (NUM_RANGES/(FINE_DECIMATION_RATIO*
						      COARSE_DECIMATION_RATIO)));
  this->result        = vsip_mcreate_f(NUM_BEAMS,
					(NUM_RANGES/(FINE_DECIMATION_RATIO*
						     COARSE_DECIMATION_RATIO))*NUM_SEGMENTS,
					VSIP_ROW, VSIP_MEM_NONE);
  this->concatRow     = vsip_cmrowview_f(this->concatData, 0);
  this->rsltRow       = vsip_mrowview_f(this->result, 0);
  this->filteredRow   = vsip_cvcreate_f((NUM_RANGES/(FINE_DECIMATION_RATIO*
						   COARSE_DECIMATION_RATIO))*NUM_SEGMENTS, VSIP_MEM_NONE);
  /**  createMf(pulseShape, predecPulseShape, this->matchedFilter); **/
  createLpf1(this->matchedFilter);
#ifndef PC_IN_TIME
  this->fft           = vsip_ccfftop_create_f(MF_SIZE, 1.0,
					      VSIP_FFT_FWD,
					      SEMI_INFINITE,
					      VSIP_ALG_TIME);
  this->ifft          = vsip_ccfftip_create_f(MF_SIZE,
					      1.0/MF_SIZE,
					      VSIP_FFT_INV,
					      SEMI_INFINITE,
					      VSIP_ALG_TIME);
#else
  this->pcFir = vsip_cfir_create_f(this->matchedFilter,
				   VSIP_NONSYM,
				   vsip_cvgetlength_f(this->concatRow),
				   MF_DECIMATION_RATIO,
				   VSIP_STATE_NO_SAVE,
				   0,
				   0);

#endif

  vsip_cvalldestroy_f(pulseShape);
  vsip_cvalldestroy_f(predecPulseShape);
}

void PC_processPulse(PC_Data* this, vsip_cmview_f* inputMat)
{
  int i,j;
  float value;
  unsigned short success = 1;
  clock_t start, stop;

  /* Initialize - this could be moved to the PC_create() method for a minor speed up
   * for now I will keep it here for simplicity.
   */
  int concatDataOrigOffset = vsip_cmgetoffset_f(this->concatData);    /* Offset to first concat row          */
  int concatDataOrigStride = vsip_cmgetcolstride_f(this->concatData); /* Distance between rows of concatData */
  int resultDataOrigOffset = vsip_mgetoffset_f(this->result);         /* Offset to first result row          */
  int resultDataOrigStride = vsip_mgetcolstride_f(this->result);      /* Distance between rows of result     */
  int nRows                = vsip_cmgetcollength_f(this->concatData); /* Number of rows to process           */
  int nCols                = vsip_cmgetrowlength_f(this->concatData); /* Number of cols to process           */
  vsip_block_f* rBlock     = vsip_mgetblock_f(this->result);
  float* rData;

  start = clock();
#ifdef DEBUG_0
  checkIfAllZeroCf(inputMat, __FILE__, __LINE__);
#endif

/* Store the incoming matrix in the second half of the working Matrix. */
  vsip_cmcopy_f_f(inputMat, this->concatData);

  /* Only do this if we have at least two pulses of data */
  if(this->numPulses >= NUM_SEGMENTS-1)
  {
    /* Note that this loop implements a mild optimization.
     * It would be simpler to implement with some calls from the mfft family,
     * but that would cause multiple accesses of rows of the concatData and generally screws the cache.
     */
    for(i = 0; i < nRows; i++)
    {
      /* get the right row */
      vsip_cvputoffset_f(this->concatRow, i*concatDataOrigStride+concatDataOrigOffset);
      vsip_vputoffset_f(this->rsltRow, i*resultDataOrigStride+resultDataOrigOffset);

#ifndef PC_IN_TIME
      /* convert to frequency domain */
      vsip_ccfftop_f(this->fft, this->concatRow, this->filteredRow);

      /* multiply by the weights */
      vsip_cvmul_f(this->filteredRow, this->matchedFilter, this->filteredRow);

      /* back to time domain */
      vsip_ccfftip_f(this->ifft, this->filteredRow);
#else
      /* Do it all in the time domain */
      vsip_cfirflt_f(this->pcFir, this->concatRow, this->filteredRow);
#endif
      /* and take the magnitude */
      vsip_cvmag_f(this->filteredRow, this->rsltRow);
    }

    /* Pulse Compression is done, now do detection. For efficiency - move detection
     * into above loop.  For clarity, we put it here.
     */
    /**    for(i = 0; i < nRows; i++) **/
    /**    { **/
    /**      for(j = 0; j < nCols; j++) **/
	     /**      {**/
	     /**	value = vsip_mget_f(this->result, i, j);**/
#ifdef PRINT_RESULT
	printf("%f\n", value);
#endif
/* 	if( value >= CFAR_THRESHOLD ) */
/* 	{ */
/* 	  if( i != TARGET_BEAM && j != TARGET_SAMPLE ) */
/* 	  { */
/* #ifdef PRINT_RESULT */
/* 	    printf( "ERROR: Target found in wrong location!!!!\n"); */
/* #endif */
/* 	    success = success && 0; */
/* 	  } */
/* 	  else */
/* 	    success = success && 1; */
/* 	} */
/* 	if( value < CFAR_THRESHOLD ) */
/* 	{ */
/* 	  if( i == TARGET_BEAM && j == TARGET_SAMPLE ) */
/* 	  { */
/* #ifdef PRINT_RESULT */
/* 	    printf( "ERROR: Target not found in the proper location!!!!!\n"); */
/* 	    printf( "       Target value was: %f, Threshold value was: %f\n", value, CFAR_THRESHOLD); */
/* #endif */
/* 	    success = success && 0; */
/* 	  } */
/* 	  else */
/* 	    success = success && 1;; */
/* 	} */
	/**    } **/
	       /** } **/
  }
  /**if( success )**/
  /*  printf( "SUCCESS!!!!\n"); **/
  this->numPulses++;
  /**vsip_cmcopy_f_f(this->sndHalfData, this->fstHalfData);**/
  stop = clock();
  this->time += ((double)(stop-start))/CLOCKS_PER_SEC;
}

void PC_destroy(PC_Data* this)
{
  vsip_cvalldestroy_f(this->matchedFilter);
  vsip_cmalldestroy_f(this->concatData);
  vsip_cmdestroy_f(this->fstHalfData);
  vsip_cmdestroy_f(this->sndHalfData);
  vsip_malldestroy_f(this->result);
  vsip_cvdestroy_f(this->concatRow);
  vsip_vdestroy_f(this->rsltRow);
  vsip_cvalldestroy_f(this->filteredRow);
#ifndef PC_IN_TIME
  vsip_fft_destroy_f(this->fft);
  vsip_fft_destroy_f(this->ifft);
#else
  vsip_cfir_destroy_f(this->pcFir);
#endif
}

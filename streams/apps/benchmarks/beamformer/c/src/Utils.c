/**
 * Utils.c
 *
 * This file contains a number of utility functions that are used to make the application
 *  auto-verify. To do this, the DIT will actually perform the reverse of the signal
 *  processing chain to create raw data that has a target in the position specified by Globals.h.
 *
 * All other modules "agree" on the automatic verification by calling
 *  these same functions internally to initialize there specific
 *  portion of the chain.
 */

#include "Globals.h"
#include <math.h>

/* Utils for the Utils... */
void reverse(vsip_cvview_f* a, vsip_cvview_f* b)
{
  int i;
  unsigned int length = vsip_cvgetlength_f(a);
  for(i = 0; i < length; i++)
  {
    vsip_cvput_f(b, i, vsip_cvget_f(a, (length - (i+1))));
  }
}

void splitCopy(vsip_cvview_f* a, vsip_cvview_f* b)
{
  unsigned int aLength = vsip_cvgetlength_f(a);
  unsigned int bLength = vsip_cvgetlength_f(b);
  vsip_cvview_f* a0 = vsip_cvsubview_f(a, 0, aLength/2);
  vsip_cvview_f* a1 = vsip_cvsubview_f(a, aLength/2, aLength/2);
  vsip_cvview_f* b0 = vsip_cvsubview_f(b, 0, aLength/2);
  vsip_cvview_f* b1 = vsip_cvsubview_f(b, bLength-aLength/2, aLength/2);

  vsip_cvcopy_f_f(a0, b0);
  vsip_cvcopy_f_f(a1, b1);

  vsip_cvdestroy_f(a0);
  vsip_cvdestroy_f(a1);
  vsip_cvdestroy_f(b0);
  vsip_cvdestroy_f(b1);
}

void checkIfAllZeroCf( vsip_cmview_f* a, char* file, char* line )
{
  int i, j;
  unsigned short success = 0;
  float valueR;
  float valueI;
  int nRows = vsip_cmgetcollength_f(a);
  int nCols = vsip_cmgetrowlength_f(a);

  for(i = 0; i < nRows; i++)
  {
    for(j = 0; j < nCols; j++)
    {
      valueR = vsip_cmget_f(a, i, j).r;
      valueI = vsip_cmget_f(a, i, j).i;

      if( valueR > 0 && valueR < 1 && valueI > 0 && valueI < 1 )
      {
	success = success || 0;
      }
      else
	success = success || 1;
      printf( "M(%d, %d) = (%f, %f)\n", i, j, valueR, valueI);
    }
  }
  if( !success )
    printf( "Matrix is all zeros in %s on line %s", file, line);
}



/* The next two fns are used to set the weights of the firs in the LPF.
 *  For simplicity, the filter coefficients are set to identity values.
 *  Just use the LPF for demo and performance reasons - can be changed later.
 */
void createLpf1(vsip_cvview_f* coarseFilterWeights)
{
  vsip_cvfill_f(COMPLEX_ZERO, coarseFilterWeights);

  vsip_cvput_f(coarseFilterWeights, 0, COMPLEX_ONE);
}

void createLpf2(vsip_cvview_f* fineFilterWeights)
{
  vsip_cvfill_f(COMPLEX_ZERO, fineFilterWeights);

  vsip_cvput_f(fineFilterWeights, 0, COMPLEX_ONE);
}

/* This method is used to create the beam-forming weights. */
void createBf(vsip_cmview_f* steeringVectors,
	      vsip_cmview_f* bfWeights)
{
  vsip_vview_f* phase = vsip_vcreate_f(NUM_BEAMS, VSIP_MEM_NONE);
  vsip_vview_f* channel = vsip_vcreate_f(NUM_CHANNELS, VSIP_MEM_NONE);
  vsip_mview_f* steeringPhases = vsip_mcreate_f(NUM_CHANNELS,
						NUM_BEAMS,
						VSIP_ROW,
						VSIP_MEM_NONE);

  /* Compute angles using a ramp. */
  vsip_vramp_f((float)-M_PI/4,
	       (float)(M_PI/2)/(NUM_BEAMS-1),
	       phase);

  /* Compute sin of angle and multiply by element spacing. */
  vsip_vsin_f(phase, phase);
  vsip_svmul_f(2.0*M_PI*D_OVER_LAMBDA, phase, phase);

  /* Compute channel index. */
  vsip_vramp_f(0.0, 1.0, channel);

  /* Compute phases for every channel. */
  vsip_vouter_f(1.0, channel, phase, steeringPhases);

  /* Compute complex phases. */
  vsip_meuler_f(steeringPhases, steeringVectors);

  /* Copy and conjugate the steering vectors */
  vsip_cmconj_f(steeringVectors, bfWeights);

  vsip_valldestroy_f(phase);
  vsip_valldestroy_f(channel);
  vsip_malldestroy_f(steeringPhases);
}

/* This method creates the filter coeffs for the matched filter used by the
 * pulse compressor.
 */
void createMf(vsip_cvview_f* pulseShape,
	      vsip_cvview_f* predecPulseShape,
	      vsip_cvview_f* mfWeights)
{
  vsip_cvview_f* pulseShape0;
  vsip_vview_f*  pulsePhase;
  vsip_cvview_f* pulseShapeFreq;
  vsip_fft_f*    mfFft;
  vsip_fft_f*    mfIfft;
  vsip_fft_f*    pulseFft;
  vsip_fft_f*    predecPulseIfft;

#ifdef DEBUG_2
  printf("        Creating matched filter...\n");
#endif
  pulsePhase = vsip_vcreate_f(PULSE_SIZE, VSIP_MEM_NONE);
  pulseShapeFreq = vsip_cvcreate_f(PULSE_SIZE, VSIP_MEM_NONE);

  vsip_cvfill_f(COMPLEX_ZERO, predecPulseShape);
  vsip_cvfill_f(COMPLEX_ZERO, mfWeights);

#ifdef DEBUG_2
  printf("        filled pulse shape and weights...\n");
#endif

  /* Create a sub-view of mf_weights. */
  pulseShape0 = vsip_cvsubview_f(mfWeights, 0, PULSE_SIZE);

#ifdef DEBUG_2
  printf("        got subview of weights...\n");
#endif

  /* Create MF FFTs. */
  /* Create in-place fft and ifft. */
  mfFft  = vsip_ccfftip_create_f(MF_SIZE, 1.0, VSIP_FFT_FWD, 0, VSIP_ALG_TIME);
  mfIfft = vsip_ccfftip_create_f(MF_SIZE, 1.0/MF_SIZE, VSIP_FFT_INV, 0, VSIP_ALG_TIME);

#ifdef DEBUG_2
  printf("        Created matched filter ffts ...\n");
#endif

  /* Create local in place FFTs for Pulse. */
  pulseFft        = vsip_ccfftip_create_f(PULSE_SIZE, 1.0, VSIP_FFT_FWD, 0, VSIP_ALG_TIME);
  predecPulseIfft = vsip_ccfftip_create_f(PREDEC_PULSE_SIZE,
					  1.0/MF_SIZE, VSIP_FFT_INV, 0, VSIP_ALG_TIME);


#ifdef DEBUG_2
  printf("        Created pulse ffts ...\n");
#endif

  /* Compute base pulse shape. */
  /* Compute ramp. */
  vsip_vramp_f( -0.5,
	        1.0/PULSE_SIZE,
		pulsePhase);

#ifdef DEBUG_2
  printf("        Computed base pulse shape...\n");
#endif

  /* Rescale and Square. */
  vsip_svmul_f(PULSE_SIZE, pulsePhase, pulsePhase);
  vsip_vmul_f(pulsePhase, pulsePhase, pulsePhase);

#ifdef DEBUG_2
  printf("        Rescaled and Squared...\n");
#endif

  /* Compute complex phase. */
  vsip_veuler_f(pulsePhase, pulseShape);

#ifdef DEBUG_2
  printf("        Computed Complex Phase...\n");
#endif

  /* Compute matched filter weights. */
  /* Copy data and time reverse view. */
  reverse(pulseShape, pulseShape0);

#ifdef DEBUG_2
  printf("        reversed pulse shape...\n");
#endif

  /* Conjugate. */
  vsip_cvconj_f(pulseShape0, pulseShape0);

#ifdef DEBUG_2
  printf("        conjugated pulse shape...\n");
#endif

  /* Convert to frequency domain. */
  vsip_ccfftip_f(mfFft, mfWeights);

#ifdef DEBUG_2
  printf("        converted weights to freq domain...\n");
#endif

  /* Compute pre-decimaated pulse shape. */
  /* Copy Pulse Shape. */
  vsip_cvcopy_f_f(pulseShape, pulseShapeFreq);

#ifdef DEBUG_2
  printf("        copied pulse shape to pulse shap in freq...\n");
#endif

  /* Convert to frequency domain. */
  vsip_ccfftip_f(pulseFft, pulseShapeFreq);

#ifdef DEBUG_2
  printf("        converted pulse shape to pulse shape in freq...\n");
#endif

  /* Copy to PredecPulseShape. */
  splitCopy(pulseShapeFreq, predecPulseShape);

#ifdef DEBUG_2
  printf("        did the split copy...\n");
#endif

  /* Convert back to time domain. */
  vsip_ccfftip_f(predecPulseIfft, predecPulseShape);

#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_cvdestroy_f(pulseShape0);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_valldestroy_f(pulsePhase);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_cvalldestroy_f(pulseShapeFreq);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_fft_destroy_f(mfFft);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_fft_destroy_f(mfIfft);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_fft_destroy_f(pulseFft);
#ifdef DEBUG_2
  printf("        converted back to frequency domain...\n");
#endif

  vsip_fft_destroy_f(predecPulseIfft);
}

/**
 * Currently, no noise in data -> no CFAR needed */
void createCfar(vsip_cvview_f* cfarWeights)
{
}

void createRawData(const vsip_cmview_f* steeringVectors,
                   const vsip_cvview_f* predecPulseShape,
                   vsip_cmview_f*       rawData)
{
  vsip_cvview_f* beam;
  vsip_cmview_f* rawData0;

  vsip_cmfill_f(COMPLEX_ZERO, rawData);

  beam = vsip_cmcolview_f(steeringVectors, TARGET_BEAM);

  rawData0 = vsip_cmsubview_f(rawData, 0, TARGET_SAMPLE, NUM_CHANNELS, PREDEC_PULSE_SIZE);

  vsip_cvouter_f(COMPLEX_ONE, beam, predecPulseShape, rawData0);

  vsip_cvdestroy_f(beam);
  vsip_cmdestroy_f(rawData0);
}

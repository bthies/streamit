#ifndef _GLOBALS_H_
#define _GLOBALS_H_

#include <vsip.h>
#include "Utils.h"

enum Status
{
  DONE,
  RUNNING
};

/**
 * Atomic Constants - all of these can be changed to and the self-verification will still work
 * properly.
 */
static const unsigned long 		NUM_CHANNELS = 12;	        /*  # of input rows */
static const unsigned long		NUM_RANGES = 64;	        /*  # of output rows */
static const unsigned long    		NUM_BEAMS = 4;  		/*  # of beams for beam forming step */
static const unsigned long    		COARSE_FILTER_SIZE = 16;	/*  # of taps in coarse filter */
static const unsigned long    		FINE_FILTER_SIZE = 32;	        /*  # of taps in fine filter */
static const unsigned long 		COARSE_DECIMATION_RATIO = 2;	/*  Decimation ratio after coarse filter */
static const unsigned long 	       	FINE_DECIMATION_RATIO = 2;	/*  Decimation ratio after fine filter */
static const unsigned long 	       	NUM_SEGMENTS = 1;  		/*  # of segments to concatenate */
static const float			FFT_SCALE = 1.0;		/*  Scale factor for forward fft */
static const float			NOISE_LEVEL = 1.0;		/*  Noise level */
static const vsip_cscalar_f             COMPLEX_ZERO = {0.0, 0.0};      /*  Useful static const */
static const vsip_cscalar_f 		COMPLEX_ONE = {1.0, 0.0};	/*  Useful static const */
static const unsigned long 	       	CFAR_SIZE = 32; 		/*  Size of CFAR filter kernel */
static const unsigned long 	       	CFAR_GUARD_SIZE = 8; 		/*  # of cfar guard gates */
static const unsigned long	       	MF_DECIMATION_RATIO = 1;	/*  Decimation ratio of matched filter */

static const unsigned int 		ROW_DIM = 0;			/*  dimension 0 is the row dim */
static const unsigned int 		COL_DIM = 1;			/*  dimension 1 is the col dim */

static const int                       SEMI_INFINITE = 0;              /* for some VSIPL routines */

static const float D_OVER_LAMBDA = 0.5;

/**
 * Derived constants - more care should be taken in changing these.
 * Only the literal values in the following expressions should be modified.
 */
#define	NUM_POSTDEC1          (NUM_RANGES/COARSE_DECIMATION_RATIO)
#define NUM_POSTDEC2          (NUM_POSTDEC1/FINE_DECIMATION_RATIO)
#define MF_SIZE               (NUM_SEGMENTS * NUM_POSTDEC2)
#define PULSE_SIZE            (NUM_POSTDEC2/2)
#define PREDEC_PULSE_SIZE     (PULSE_SIZE*COARSE_DECIMATION_RATIO*FINE_DECIMATION_RATIO)
#define TARGET_BEAM           (NUM_BEAMS/4)
#define TARGET_SAMPLE         (NUM_RANGES/4)
#define TARGET_SAMPLE_POSTDEC (1+TARGET_SAMPLE/COARSE_DECIMATION_RATIO/FINE_DECIMATION_RATIO)
#define CFAR_THRESHOLD        (0.95*D_OVER_LAMBDA*NUM_CHANNELS*(0.5*PULSE_SIZE))

#endif

/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

/* 
 * contains all of the code necessary to do implement multiplication in frequency
 * and then do a reverse FFT.
 * $Id: streamit_fft.c,v 1.4 2003-10-09 20:42:06 dmaze Exp $
 */

#include<stdio.h>
#include<stdlib.h>
#include<math.h>
#include<assert.h>


#define TRUE  1
#define FALSE 0
#define BITS_PER_WORD   (sizeof(unsigned) * 8)
#define FORWARD 0
#define REVERSE 1
#define DDC_PI  (3.14159265358979323846)


/* Implementation */

/* multiply two float vectors(a and b), pairwise, consisting of real,im pairs of floats. */
static void element_multiply(float* a_r, float* a_i, 
			     float* b_r, float* b_i, 
			     float* result_r, float* result_i, 
			     int size) {
  int i;
  for (i=0; i<size; i++) {
    /* result: real part = a.re*b.re - a.im*b.im
       imag part = a.re*b.im + a.im*b.re */
    result_r[i] = (a_r[i]*b_r[i]-
		   a_i[i]*b_i[i]);
    result_i[i] = (a_r[i]*b_i[i]+
		   a_i[i]*b_r[i]);
  }
}

static int IsPowerOfTwo ( unsigned x )
{
    if ( x < 2 )
        return FALSE;

    if ( x & (x-1) )        /* Thanks to 'byang' for this cute trick! */
        return FALSE;

    return TRUE;
}


static unsigned NumberOfBitsNeeded ( unsigned PowerOfTwo )
{
    unsigned i;

    if ( PowerOfTwo < 2 )
    {
        fprintf (
            stderr,
            ">>> Error in fftmisc.c: argument %d to NumberOfBitsNeeded is too small.\n",
            PowerOfTwo );

        exit(1);
    }

    for ( i=0; ; i++ )
    {
        if ( PowerOfTwo & (1 << i) )
            return i;
    }
}



static unsigned ReverseBits ( unsigned index, unsigned NumBits )
{
    unsigned i, rev;

    for ( i=rev=0; i < NumBits; i++ )
    {
        rev = (rev << 1) | (index & 1);
        index >>= 1;
    }

    return rev;
}


#define CHECKPOINTER(p)  CheckPointer(p,#p)

static void CheckPointer ( void *p, char *name )
{
    if ( p == NULL )
    {
        fprintf ( stderr, "Error in fft_float():  %s == NULL\n", name );
        exit(1);
    }
}


static void fft_float (unsigned  NumSamples,
		       int       InverseTransform,
		       float    *RealIn,
		       float    *ImagIn,
		       float    *RealOut,
		       float    *ImagOut )
{
    unsigned NumBits;    /* Number of bits needed to store indices */
    unsigned i, j, k, n;
    unsigned BlockSize, BlockEnd;

    double angle_numerator = 2.0 * DDC_PI;
    double tr, ti;     /* temp real, temp imaginary */

    if ( !IsPowerOfTwo(NumSamples) )
    {
        fprintf (
            stderr,
            "Error in fft():  NumSamples=%u is not power of two\n",
            NumSamples );

        exit(1);
    }

    if ( InverseTransform )
        angle_numerator = -angle_numerator;

    CHECKPOINTER ( RealIn );
    CHECKPOINTER ( RealOut );
    CHECKPOINTER ( ImagOut );

    NumBits = NumberOfBitsNeeded ( NumSamples );

    /*
    **   Do simultaneous data copy and bit-reversal ordering into outputs...
    */

    for ( i=0; i < NumSamples; i++ )
    {
        j = ReverseBits ( i, NumBits );
        RealOut[j] = RealIn[i];
        ImagOut[j] = (ImagIn == NULL) ? 0.0 : ImagIn[i];
    }

    /*
    **   Do the FFT itself...
    */

    BlockEnd = 1;
    for ( BlockSize = 2; BlockSize <= NumSamples; BlockSize <<= 1 )
    {
        double delta_angle = angle_numerator / (double)BlockSize;
        double sm2 = sin ( -2 * delta_angle );
        double sm1 = sin ( -delta_angle );
        double cm2 = cos ( -2 * delta_angle );
        double cm1 = cos ( -delta_angle );
        double w = 2 * cm1;
        double ar[3], ai[3];

        for ( i=0; i < NumSamples; i += BlockSize )
        {
            ar[2] = cm2;
            ar[1] = cm1;

            ai[2] = sm2;
            ai[1] = sm1;

            for ( j=i, n=0; n < BlockEnd; j++, n++ )
            {
                ar[0] = w*ar[1] - ar[2];
                ar[2] = ar[1];
                ar[1] = ar[0];

                ai[0] = w*ai[1] - ai[2];
                ai[2] = ai[1];
                ai[1] = ai[0];

                k = j + BlockEnd;
                tr = ar[0]*RealOut[k] - ai[0]*ImagOut[k];
                ti = ar[0]*ImagOut[k] + ai[0]*RealOut[k];

                RealOut[k] = RealOut[j] - tr;
                ImagOut[k] = ImagOut[j] - ti;

                RealOut[j] += tr;
                ImagOut[j] += ti;
            }
        }

        BlockEnd = BlockSize;
    }

    /*
    **   Need to normalize if inverse transform...
    */

    if ( InverseTransform )
    {
        double denom = (double)NumSamples;

        for ( i=0; i < NumSamples; i++ )
        {
            RealOut[i] /= denom;
            ImagOut[i] /= denom;
        }
    }
}

/* 
 * takes an input array of floats (in the time domain) 
 * and produces an output array of floats in the same array.
 *
 * This uses the fft implementation found on the web from: 
 * Don Cross (dcross@intersrv.com)
 */
void do_fast_convolution_std(float* x, float* H_r, float* H_i, int size) {
  float *X_r,  *X_i;
  float *Y_r, *Y_i;
  
  float* y_r;
  float* y_i;

  /* allocate some memory for the frequency domain stuff. */
  X_r  = malloc(sizeof(float) * size);
  X_i  = malloc(sizeof(float) * size);
  Y_r = malloc(sizeof(float) * size);
  Y_i = malloc(sizeof(float) * size);
  y_r      = x;
  y_i      = malloc(sizeof(float) * size);

  /* convert the input to the frequency domain. */
  fft_float(size, FORWARD, x, NULL, X_r, X_i);
  
  /* do the multiplication element wise in frequency. Y(w)=X(w)H(w)*/
  element_multiply(X_r, X_i,
		   H_r, H_i,
		   Y_r, Y_i, 
		   size);
  
  /* do the inverse fft to recover the output frequency spectrum. */
  fft_float(size, REVERSE, Y_r, Y_i, y_r, y_i);

  /* free the memory we allocated */
  free(X_r);
  free(X_i);
  free(Y_r);
  free(Y_i);
  free(y_i);
}


/*--- end of file fourierf.c ---*/


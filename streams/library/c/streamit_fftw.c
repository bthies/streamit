/*
 * interface to the fftw library to replace streamit_fft.c (which 
 * contians a simple non optimized implementation of fft)
 * $Id: streamit_fftw.c,v 1.5 2003-02-04 17:36:12 aalamb Exp $
 */

#include <sfftw.h>
#include <srfftw.h>

/* Linked list of the FFTW plans we know about. */
struct rfftw_plan_list 
{
  int size;
  rfftw_plan rtoc_plan;
  rfftw_plan ctor_plan;
  float *buff;
  struct rfftw_plan_list *next;
};
static struct rfftw_plan_list *plans;

static struct rfftw_plan_list *get_plan(int size)
{
  struct rfftw_plan_list *plan;
  float *spare_storage;

  /* 1. Look for a plan; if we have it, return it. */
  for (plan = plans; plan; plan = plan->next)
    if (plan->size == size)
      return plan;
  
  /* 2. We lose.  Create a new plan. */
  plan = malloc(sizeof(struct rfftw_plan_list));
  plan->size = size;
  plan->next = plans;
  plans = plan;
  plan->buff = malloc(size * sizeof(float));
  spare_storage = malloc(size * sizeof(float));
  plan->rtoc_plan = rfftw_create_plan_specific
    (size, FFTW_REAL_TO_COMPLEX,
     FFTW_ESTIMATE | FFTW_OUT_OF_PLACE | FFTW_USE_WISDOM,
     spare_storage, 1, plan->buff, 1);
  plan->ctor_plan = rfftw_create_plan_specific
    (size, FFTW_COMPLEX_TO_REAL,
     FFTW_ESTIMATE | FFTW_OUT_OF_PLACE | FFTW_USE_WISDOM,
     plan->buff, 1, spare_storage, 1);
  free(spare_storage);
  return plan;
}


/* Multiplies an FFTW halfcomplex array by a known set of
 * constants.
 * output: Y (float array of size size)
 * input1: X (float array of size size)
 * input2: H (float array of size size)
 *
 * All arrays are  Hermitian, meaning that for all
 * i 0<=i<n, x[i] = conj(x[n-i]).  FFTW then stores this in a single
 * array, where for 0<=i<=n/2, x[i] is the real part of X[i] (and also
 * the real part of X[n-i]), and for 0<i<n/2, x[n-i] is the complex part
 * of X[i] (and the negated complex part of X[n-i]).  It appears to
 * follow from the documentation that X[0] is strictly real (which is
 * due to the math of the FFT.
 *
 * The output can be safely set to be one of the inputs if desired.
 */
void do_halfcomplex_multiply(float *Y, float *X, float *H, int size)
{
  int i;
  
  /* We get to ignore the last half of H.  Whee!  :-) */
  /* First off, calculate buff[0], which apparently is strictly real. */
  Y[0] = X[0] * H[0];
  
  /* Now go through the complex parts of the array: */
  for (i = 1; i < size/2; i++)
    {
      /* (a+bi)(c+di) = (ac-bd)+(ad+bc)i */
      float X_real = X[i];
      float X_imag = X[size-i];
      float H_real = H[i];
      float H_imag = H[size-i];
      Y[i]      = X_real * H_real - X_imag * H_imag;
      Y[size-i] = X_real * H_imag + X_imag * H_real;
    }
  
  /* If size is even, we also need to deal with the middle (real) element. */
  if (!(size & 1)) {
    Y[size/2] = X[size/2] * H[size/2];
  }
}

/* 
 * takes an input array of floats (in the time domain) 
 * and produces an output array of floats in the same array.
 */
//void do_fast_convolution_fftw(float* x, float* H, int size) {
//  int i;
//  struct rfftw_plan_list *plan;

  /* Start off by finding the plan pair, or creating one. */
//  plan = get_plan(size);
  
  /* Run the forward FFT. */
//  rfftw_one(plan->rtoc_plan, (fftw_real *)x, (fftw_real *)plan->buff);

  /* Do the multiplication element-wise in frequency.  Note that this
   * is a little weird because of the ordering; see the FFTW documentation.
   * This happens in place. */
  //do_halfcomplex_multiply(plan->buff, H, size);

  //_debug_print_halfcomplex("Y", plan->buff, size);

  /* Run the backward FFT (trashing storage). */
//  rfftw_one(plan->ctor_plan, (fftw_real *)plan->buff, (fftw_real  *)x);

//}


/**
 * Replaces the contents of input_buff with the value of its FFT
 * scaled by a factor of 1/size (because fftw does not do 
 * this scaling in the reverse FFT, we precompute the scaling now).
 * Since buff is a completly realarray, the corresponding complex
 * valued FFT(buff) is stored in the "half complex array" format of
 * fftw (see http://www.fftw.org/doc/fftw_2.html#SEC5)
 **/
void convert_to_freq(float* input_buff, int size) 
{
  struct rfftw_plan_list *plan;
  int i;

  /* Start off by finding the plan pair, or creating one. */
  plan = get_plan(size);

  /* Run the forward FFT on the input buffer. */
  rfftw_one(plan->rtoc_plan, (fftw_real *)input_buff, (fftw_real *)plan->buff);

  /* copy the values from the plan buffer (eg the output) into the 
   * input buffer (return value is passed via input). **/
  for (i=0; i<size; i++) {
    input_buff[i] = plan->buff[i];
  }

  /** and we are done. Return value is the input_buffer parameter. **/
}

/** 
 * Scales the passed buffer by 1/size. Used to renormalize the
 * filter coefficients when they have been converted into the
 * frequency domain (fftw does not do the scaling automatically).
 **/
void scale_by_size(float* buffer, int size) 
{
  int i;
  for (i=0; i<size; i++) {
    buffer[i] = buffer[i]/size;
  }
}


/**
 * Replaces the contents of input_buff with the value of its IFFT
 * (doesn't include the 1/size scaling factor in the definition
 * -- that is an FFTW thing.
 * Since input_buff is in "half complex array" which corresponds to
 * a completely real valued inverse FFT.
 **/
void convert_from_freq(float* input_buff, int size) 
{
  struct rfftw_plan_list *plan;

  /* Start off by finding the plan pair, or creating one. */
  plan = get_plan(size);

  /* Run the backward FFT (trashing storage). */
  // reverse is specified by the plan. Then comes input followed by output.
  rfftw_one(plan->ctor_plan, (fftw_real *)input_buff, (fftw_real *)input_buff);
  
  /** and we are done. Return value is the input_buffer parameter. **/
}




/** debugging routine that prints a halfcomplex array. **/
void _debug_print_halfcomplex(char* prefix, float* complex_arr, int size) {
  int i;
  // the first element is purely real
  printf("%s[%d]:%f\n", prefix, 0, complex_arr[0]);
  for (i=1; i<(size/2); i++) {
    printf("%s[%d]:%f+%fi\n", prefix, i, complex_arr[i], complex_arr[size-i]);
  }
  // if size even, also print out the middle element which is purely real.
  if ((size % 2) == 0) {
    printf("%s[%d]:%f\n", prefix, (size/2), complex_arr[size/2]);
  }
}

/** debugging routine that prints a real array. **/
void _debug_print_real(char* prefix, float* arr, int size) {
  int i;
  for (i=0; i<size; i++) {
    printf("%s[%d]:%f\n", prefix, i, arr[i]);
  }
}

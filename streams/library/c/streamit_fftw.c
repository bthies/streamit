/*
 * interface to the fftw library to replace streamit_frequency.c
 * $Id: streamit_fftw.c,v 1.1 2002-11-16 02:04:17 dmaze Exp $
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

/* Multiplies (in place) an FFTW halfcomplex array by a known set of
 * constants, in place.  The array is Hermitian, meaning that for all
 * i 0<=i<n, x[i] = conj(x[n-i]).  FFTW then stores this in a single
 * array, where for 0<=i<=n/2, x[i] is the real part of X[i] (and also
 * the real part of X[n-i]), and for 0<i<n/2, x[n-i] is the complex part
 * of X[i] (and the negated complex part of X[n-i]).  It appears to
 * follow from the documentation that X[0] is strictly real. */
static void do_halfcomplex_multiply(float *buff, float *H_r, float *H_i,
                                    int size)
{
  int i;
  /* We get to ignore the last half of H.  Whee!  :-) */
  /* First off, calculate buff[0], which apparently is strictly real. */
  buff[0] = buff[0] * H_r[0];
  
  /* Now go through the complex parts of the array: */
  for (i = 1; i < size/2; i++)
  {
    /* (a+bi)(c+di) = (ac-bd)+(ad+bc)i */
    float real = buff[i];
    float imag = buff[size-i];
    buff[i]      = real * H_r[i] - imag * H_i[i];
    buff[size-i] = real * H_i[i] + imag * H_r[i];
  }
  
  /* If size is even, we also need to deal with the middle (real) element. */
  if (!(size & 1))
    buff[size/2] = buff[size/2] * H_r[size/2];
}

/* 
 * takes an input array of floats (in the time domain) 
 * and produces an output array of floats in the same array.
 */
do_fast_convolution(float* x, float* H_r, float* H_i, int size) {
  struct rfftw_plan_list *plan;

  /* Start off by finding the plan pair, or creating one. */
  plan = get_plan(size);
  
  /* Run the forward FFT. */
  rfftw_one(plan->rtoc_plan, (fftw_real *)x, (fftw_real *)plan->buff);
  
  /* Do the multiplication element-wise in frequency.  Note that this
   * is a little weird because of the ordering; see the FFTW documentation.
   * This happens in place. */
  do_halfcomplex_multiply(plan->buff, H_r, H_i, size);

  /* Run the backward FFT (trashing storage). */
  rfftw_one(plan->ctor_plan, (fftw_real *)plan->buff, (fftw_real  *)x);
}

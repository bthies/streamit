/*
 * cfar.c: C implementation of the CFAR kernel
 * From "PCA Kernel-Level Benchmarks", item 3.3
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: cfar.c,v 1.2 2003-10-28 20:57:49 dmaze Exp $
 */

/* Implementation of the CFAR algorithm.  The documentation talks
 * about a data cube of cells C(i,j,k), but on a given iteration only
 * cells with fixed i and k are considered.  Our goal here is compatibility
 * with the StreamIt version, rather than strict correctness as regards
 * the Lincoln Labs document.  Thus, we assume that the cube has
 * been transformed to run over range gates.  Then each row is
 * independent, and the processing comes down to these three
 * parameters.  N_rg is the length of a row.  Around a particular
 * element, the first G gates either way are ignored, and then the
 * next N_cfar gates are considered. */

#ifdef raw
#include <raw.h>
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#endif
#include <math.h>

/* Parameters: */
#define N_rg 64
#define N_cfar 5
#define G 4

void begin(void);
void get_input(float *in);
void do_cfar(float *in, float *out);
void print_output(float *out);

static int numiters = -1;

#ifndef raw
int main(int argc, char **argv)
{
  int option;

  while ((option = getopt(argc, argv, "i:")) != -1)
  {
    switch(option)
    {
    case 'i':
      numiters = atoi(optarg);
    }
  }

  begin();
  return 0;
}
#endif

void begin(void)
{
  float in[N_rg * 2];
  float out[N_rg];

  while (numiters == -1 || numiters-- > 0) {
    get_input(in);
    do_cfar(in, out);
    print_output(out);
  }
}

/* The input array is pairs of real and imaginary numbers.  Helpers: */
#define REAL(v, i) (v)[(i) * 2]
#define IMAG(v, i) (v)[(i) * 2 + 1]
#define MAG(v, i) (REAL((v),(i))*REAL((v),(i)) + IMAG((v),(i))*IMAG((v),(i)))

void get_input(float *in)
{
  /* Same (somewhat arbitrary) input source as the StreamIt code. */
  static float theta;
  float mag;
  int i;
  for (i = 0; i < N_rg; i++)
  {
    theta += M_PI / 16;
    mag = sin(theta);
    REAL(in, i) = mag * cos(theta);
    IMAG(in, i) = mag * sin(theta);
  }
}

void do_cfar(float *in, float *out)
{
  float value;
  int i;

  /* Start by calculating the initial value of T.  This is the sum of
   * the magnitudes of items G+1 to G+N_cfar, divided by 2*N_cfar. */
  value = 0;
  for (i = G+1; i <= G+N_cfar; i++)
  {
    value += MAG(in, i);
  }
  out[0] = value / (2 * N_cfar);
  
  /* For the first G items, update the value and push; we won't need items
   * from behind us. */
  for (i = 1; i < G + 1; i++)
  {
    value += MAG(in, i + G + N_cfar);
    value -= MAG(in, i + G);
    out[i] = value / (2 * N_cfar);
  }

  /* For the next N_cfar items, update the value and add in item zero. */
  for (i = G + 1; i < G + N_cfar + 1; i++)
  {
    value += MAG(in, i + G + N_cfar);
    value -= MAG(in, i + G);
    value += MAG(in, i - G - 1);
    out[i] = value / (2 * N_cfar);
  }

  /* Until we reach the end, now, update on both ends. */
  for (i = G + N_cfar + 1; i < N_rg - G - N_cfar; i++)
  {
    value += MAG(in, i + G + N_cfar);
    value -= MAG(in, i + G);
    value += MAG(in, i - G - 1);
    value -= MAG(in, i - G - N_cfar - 1);
    out[i] = value / (2 * N_cfar);
  }

  /* For the penultimate N_cfar items, update the value and subtract out
   * the last item. */
  for (i = N_rg - G - N_cfar; i < N_rg - G; i++)
  {
    value -= MAG(in, i + G);
    value += MAG(in, i - G - 1);
    value -= MAG(in, i - G - N_cfar - 1);
    out[i] = value / (2 * N_cfar);
  }
  
  /* For the last G items, update the value and push; we won't need items
   * from ahead of us. */
  for (i = N_rg - G; i < N_rg; i++)
  {
    value += MAG(in, i - G - 1);
    value -= MAG(in, i - G - N_cfar - 1);
    out[i] = value / (2 * N_cfar);
  }
}

void print_output(float *out)
{
  int i;
  for (i = 0; i < N_rg; i++)
#ifdef raw
    print_float(out[i]);
#else
    printf("%f\n", out[i]);
#endif
}

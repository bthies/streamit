/*
 * firref.c: C reference implementation of FIR
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: firref.c,v 1.2 2002-07-30 01:34:22 dmaze Exp $
 */

#ifdef raw
#include <raw.h>
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#endif
#include <math.h>

void begin(void);
float calc_fir(const float *a, float *b, float W, float last);

/* Globals: */
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

#define DEPTH 128
#define LENGTH 12

void begin(void)
{
  int val = 0;
  const int limit = 10000;
  int i;
  float W[DEPTH], last[DEPTH];
  float f1[LENGTH], f2[LENGTH];
  float *a, *b, *c;
  
  for (i = 0; i < DEPTH; i++)
  {
    W[i] = (float)(2*i*i) / (float)(i+1);
    last[i] = 0;
  }
  a = f1;
  b = f2;
  
  /* Main loop: */
  while (numiters == -1 || numiters-- > 0)
  {
    for (i = 0; i < LENGTH; i += 2)
    {
      a[i] = 0;
      a[i+1] = val++;
      if (val >= limit) val = 0;
    }
    for (i = 0; i < DEPTH; i++)
    {
      last[i] = calc_fir(a, b, W[i], last[i]);
      c = a;
      a = b;
      b = c;
    }
      
    for (i = 0; i < LENGTH; i += 2)
#ifdef raw
      print_float(a[i]);
#else
      printf("%f\n", a[i]);
#endif
  }
}

float calc_fir(const float *a, float *b, float W, float last)
{
  int i;
  
  for (i = 0; i < LENGTH/2; i++)
  {
    float s = a[i*2];
    b[i*2] = s + last * W;
    b[i*2+1] = last;
    last = a[i*2+1];
  }
  return last;
}

/*
 * firref.c: C reference implementation of FIR
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: firref.c,v 1.3 2002-07-30 19:11:40 dmaze Exp $
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
#define COUNT 6

void begin(void)
{
  int val = 0;
  const int limit = 10000;
  int i, j, base;
  float W[DEPTH];
  float buff[DEPTH];
  int bufflast;
  
  // Calculate weights:
  for (i = 0; i < DEPTH; i++)
    W[i] = (float)(2*i*i) / (float)(i+1);
  // Prefill buffer:
  for (i = 0; i < DEPTH; i++)
    buff[i] = 0;
  bufflast = 0;
  base = 0;
  
  /* Main loop: */
  while (numiters == -1 || numiters-- > 0)
  {
    // Calculate a sliding window over the (circular) buffer.
    float s = 0;
    for (i = 0; i < DEPTH; i++)
        s += W[i] * buff[(base + i) % DEPTH];

    // Calculate the next elements.
    base--;
    if (base < 0) base += DEPTH;
    buff[base] = bufflast++;
      
    // Print the result.
#ifdef raw
    print_float(s);
#else
    printf("%f\n", s);
#endif
  }
}

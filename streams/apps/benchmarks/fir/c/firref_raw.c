/*
 * firref.c: C reference implementation of FIR
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: firref_raw.c,v 1.3 2002-07-30 19:50:27 aalamb Exp $
 */

#include <raw.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

void begin(void);

/* Globals: */
static int numiters = -1;

int main(int argc, char **argv)
{
  numiters = 100;
  begin();
  return 0;
}

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
      
    print_float(s);
  }
}

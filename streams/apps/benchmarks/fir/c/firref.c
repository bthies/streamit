/*
 * firref.c: C reference implementation of FIR
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: firref.c,v 1.1 2002-07-30 00:35:54 dmaze Exp $
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

void begin(void)
{
  int val = 0;
  const int limit = 10000;
  float c, s;
  int i;
  float W[64];
  
  for (i = 0; i < 64; i++)
    W[i] = (float)(2*i*i) / (float)(i+1);
  
  /* Main loop: */
  while (numiters == -1 || numiters-- > 0)
  {
    c = val++;
    if (val >= limit) val = 0;
    s = 0.0;
    for (i = 0; i < 64; i++)
      s = s + c * W[i];
#ifdef raw
    print_float(s);
#else
    printf("%f\n", s);
#endif
  }
}

/*
 * mergeref.c: a reference C implementation of merge-sort
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: mergeref.c,v 1.1 2002-05-06 15:24:03 dmaze Exp $
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/* Parameters: */
/* Number of inputs; should be power of 2 */
#define NUM_INPUTS 16
/* Number of times to break up sequence (for testing) */
#define MULT 4
/* And, for convenience later, length of a sequence segment: */
#define MULT_SEG (NUM_INPUTS/MULT)

int inBuffer[NUM_INPUTS];
int outBuffer[NUM_INPUTS];

void getNums(int *buff, int len);
void doSort(const int *in, int *out, int len);
void doMerge(const int *in1, const int *in2, int inlen, int *out);
void printNums(const int *buff, int len);

int main(int argc, char **argv)
{
  int numiters = -1;
  int option;

  while ((option = getopt(argc, argv, "i:")) != -1)
  {
    switch(option)
    {
    case 'i':
      numiters = atoi(optarg);
    }
  }

  /* Main loop: */
  while (numiters == -1 || numiters-- > 0)
  {
    getNums(inBuffer, NUM_INPUTS);
    doSort(inBuffer, outBuffer, NUM_INPUTS);
    printNums(outBuffer, NUM_INPUTS);
  }
}

void getNums(int *buff, int len)
{
  /* This version does deal correctly with the case where MULT doesn't
   * divide NUM_INPUTS; we could do better (with a nested loop and no
   * mod) in the case where it is. */
  static int cycle;
  int i;
  
  for (i = 0; i < len; i++)
  {
    buff[i] = MULT_SEG - cycle;
    cycle++;
    cycle = cycle % MULT_SEG;
  }
}

void doSort(const int *in, int *out, int len)
{
  int i;

  /* Do a recursive sort if we have more than two items. */
  if (len > 2)
  {
    int buff[len];
    doSort(in, buff, len/2);
    doSort(in + len/2, buff + len/2, len/2);
    doMerge(buff, buff + len/2, len/2, out);
  }
  else
    doMerge(in, in + len/2, len/2, out);
}

void doMerge(const int *in1, const int *in2, int inlen, int *out)
{
  /* Merge sorted streams of length inlen from in1 and in2 into a single
   * sorted stream of length 2*inlen in out. */
  int count1, count2, countout;
  
  for (count1 = 0, count2 = 0, countout = 0;
       count1 < inlen || count2 < inlen;
       countout++)
  {
    int item;
    /* Figure out which item to get, remembering that at least one of count1
     * and count2 is strictly less than inlen: */
    if (count1 == inlen)
      item = in2[count2++];
    else if (count2 == inlen)
      item = in1[count1++];
    else if (in1[count1] < in2[count2])
      item = in1[count1++];
    else
      item = in2[count2++];
    out[countout] = item;
  }
}

void printNums(const int *buff, int len)
{
  int i;
  for (i = 0; i < len; i++)
    printf("%d\n", buff[i]);
}

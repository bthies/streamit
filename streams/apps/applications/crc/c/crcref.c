/*
 * crcref.c: reference implementation of 32-bit CRC
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: crcref.c,v 1.2 2002-05-07 19:28:42 dmaze Exp $
 */

#include <stdint.h>
#include <stdio.h>
#include <unistd.h>

/* This is intended to implement exactly the code in CrcEncoder32Test.java.
 * It seems like a real C implementation of this would use bitwise
 * operations for speed.  We'll see how we do here... */

int getInput(void);
int doCRC(int in);

int main(int argc, char **argv)
{
  int numiters = -1;
  int option;
  FILE *fp;

  while ((option = getopt(argc, argv, "i:")) != -1)
  {
    switch(option)
    {
    case 'i':
      numiters = atoi(optarg);
    }
  }

  fp = fopen("bleh", "w");

  /* Main loop: */
  while (numiters == -1 || numiters-- > 0)
  {
    int in = getInput();
    int out = doCRC(in);
    fwrite(&out, sizeof(out), 1, fp);
  }
  fclose(fp);
}

int getInput(void)
{
  /* StreamIt version has an unused option for file input. */
  static int first = 1;
  if (first)
  {
    first = 0;
    return 0;
  }
  else
    return 1;
}

int doCRC(int in)
{
  static int regs[32];
  static int lastout = 0;
  int first, val, temp;
  
  /* This is just a duplication of the StreamIt code. */
  first = val = in ^ lastout;
#define SHIFT(n) temp = regs[n]; regs[n] = val; val = temp;
#define ADD val = val ^ first;
  SHIFT(1);
  ADD;
  SHIFT(2);
  ADD;
  SHIFT(3);
  SHIFT(4);
  ADD;
  SHIFT(5);
  ADD;
  SHIFT(6);
  SHIFT(7);
  ADD;
  SHIFT(8);
  ADD;
  SHIFT(9);
  SHIFT(10);
  ADD;
  SHIFT(11);
  ADD;
  SHIFT(12);
  ADD;
  SHIFT(13);
  SHIFT(14);
  SHIFT(15);
  SHIFT(16);
  ADD;
  SHIFT(17);
  SHIFT(18);
  SHIFT(19);
  SHIFT(20);
  SHIFT(21);
  SHIFT(22);
  ADD;
  SHIFT(23);
  ADD;
  SHIFT(24);
  SHIFT(25);
  SHIFT(26);
  ADD;
  SHIFT(27);
  SHIFT(28);
  SHIFT(29);
  SHIFT(30);
  SHIFT(31);
  
  lastout = val;
  return val;
}

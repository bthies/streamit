/*****************************
 * 
 *
 * Multiplication and Arithmetic on Galois Field GF(256)
 *
 * From Mee, Daniel, "Magnetic Recording, Volume III", Ch. 5 by Patel.
 * 
 * (c) 1991 Henry Minsky
 *
 *
 ******************************/
 
 
#include <stdio.h>
#include <stdlib.h>
#include "ecc.h"

/* This is one of 14 irreducible polynomials
 * of degree 8 and cycle length 255. (Ch 5, pp. 275, Magnetic Recording)
 * The high order 1 bit is implicit */
/* x^8 + x^4 + x^3 + x^2 + 1 */
#define PPOLY 0x1D 


int gexp[512];
int glog[256];


static void init_exp_table (void);


void
init_galois_tables (void)
{	
  /* initialize the table of powers of alpha */
  init_exp_table();
}


static void
init_exp_table (void)
{
  int i, z;
  int pinit,p1,p2,p3,p4,p5,p6,p7,p8;

  pinit = p2 = p3 = p4 = p5 = p6 = p7 = p8 = 0;
  p1 = 1;
	
  gexp[0] = 1;
  gexp[255] = gexp[0];
  glog[0] = 0;			/* shouldn't log[0] be an error? */
	
  for (i = 1; i < 256; i++) {
    pinit = p8;
    p8 = p7;
    p7 = p6;
    p6 = p5;
    p5 = p4 ^ pinit;
    p4 = p3 ^ pinit;
    p3 = p2 ^ pinit;
    p2 = p1;
    p1 = pinit;
    gexp[i] = p1 + p2*2 + p3*4 + p4*8 + p5*16 + p6*32 + p7*64 + p8*128;
    gexp[i+255] = gexp[i];
  }
	
  for (i = 1; i < 256; i++) {
    for (z = 0; z < 256; z++) {
      if (gexp[z] == i) {
	glog[i] = z;
	break;
      }
    }
  }
}

/* multiplication using logarithms */
int gmult(int a, int b)
{
  int i,j;
  if (a==0 || b == 0) return (0);
  i = glog[a];
  j = glog[b];
  return (gexp[i+j]);
}
		

int ginv (int elt) 
{ 
  return (gexp[255-glog[elt]]);
}


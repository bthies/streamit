/* code borrowed from fdctref.c, idctref.c, and idct.c of MPEG reference code 
 * which has the following copyright and disclaimer
 *
 * Copyright (C) 1996, MPEG Software Simulation Group. All Rights Reserved. 
 *
 * Disclaimer of Warranty
 *
 * These software programs are available to the user without any license fee or
 * royalty on an "as is" basis.  The MPEG Software Simulation Group disclaims
 * any and all warranties, whether express, implied, or statuary, including any
 * implied warranties or merchantability or of fitness for a particular
 * purpose.  In no event shall the copyright-holder be liable for any
 * incidental, punitive, or consequential damages of any kind whatsoever
 * arising from the use of these programs.
 *
 * This disclaimer of warranty extends to the user of these programs and user's
 * customers, employees, agents, transferees, successors, and assigns.
 *
 * The MPEG Software Simulation Group does not represent or warrant that the
 * programs furnished hereunder are free of infringement of any third-party
 * patents.
 *
 * Commercial implementations of MPEG-1 and MPEG-2 video, including shareware,
 * are subject to royalty fees to patent holders.  Many of these patents are
 * general enough such that they are unavoidable regardless of implementation
 * design.
 *
 */

#include <stdio.h>
#include <math.h>
#include <stdlib.h>

#ifndef PI
# ifdef M_PI
#  define PI M_PI
# else
#  define PI 3.14159265358979323846
# endif
#endif
#define W1 2841 /* 2048*sqrt(2)*cos(1*pi/16) */
#define W2 2676 /* 2048*sqrt(2)*cos(2*pi/16) */
#define W3 2408 /* 2048*sqrt(2)*cos(3*pi/16) */
#define W5 1609 /* 2048*sqrt(2)*cos(5*pi/16) */
#define W6 1108 /* 2048*sqrt(2)*cos(6*pi/16) */
#define W7 565  /* 2048*sqrt(2)*cos(7*pi/16) */

static double c[8][8]; /* transform coefficients */

void init_dct()
{
  int i, j;
  double s;

  for (i=0; i<8; i++) {
    s = (i==0) ? sqrt(0.125) : 0.5;
    
    for (j=0; j<8; j++)
      c[i][j] = s * cos((PI/8.0)*i*(j+0.5));
  }
}

void dct(int* block)
{
  int i, j, k;
  double s;
  double tmp[64];
  
  for (i=0; i<8; i++)
    for (j=0; j<8; j++) {
      s = 0.0;
	
      for (k=0; k<8; k++)
        s += c[j][k] * block[8*i+k];
	
      tmp[8*i+j] = s;
    }
  
  for (j=0; j<8; j++)
    for (i=0; i<8; i++) {
      s = 0.0;
	
      for (k=0; k<8; k++)
        s += c[i][k] * tmp[8*k+j];
	
      block[8*i+j] = (int)floor(s+0.499999);
    }
}


void init_idct()
{
  int freq, time;
  double scale;

  for (freq=0; freq < 8; freq++)
  {
    scale = (freq == 0) ? sqrt(0.125) : 0.5;
    for (time=0; time<8; time++)
      c[freq][time] = scale*cos((PI/8.0)*freq*(time + 0.5));
  }
}

void idct(int* block)
{
  int i, j, k, v;
  double partial_product;
  double tmp[64];

  for (i=0; i<8; i++)
    for (j=0; j<8; j++) {
      partial_product = 0.0;
	
      for (k=0; k<8; k++)
        partial_product+= c[k][j]*block[8*i+k];
	
      tmp[8*i+j] = partial_product;
    }
  
  for (j=0; j<8; j++)
    for (i=0; i<8; i++) {
      partial_product = 0.0;
	
      for (k=0; k<8; k++)
        partial_product+= c[k][i]*tmp[8*k+j];
	
      v = (int) floor(partial_product+0.50);
      block[8*i+j] = v;
    }
}

static void idctrow(int* blk)
{
  int x0, x1, x2, x3, x4, x5, x6, x7, x8;

  /* shortcut */
  if (!((x1 = blk[4]<<11) | (x2 = blk[6]) | (x3 = blk[2]) |
        (x4 = blk[1]) | (x5 = blk[7]) | (x6 = blk[5]) | (x7 = blk[3])))
  {
    blk[0]=blk[1]=blk[2]=blk[3]=blk[4]=blk[5]=blk[6]=blk[7]=blk[0]<<3;
    return;
  }

  x0 = (blk[0]<<11) + 128; /* for proper rounding in the fourth stage */

  /* first stage */
  x8 = W7*(x4+x5);
  x4 = x8 + (W1-W7)*x4;
  x5 = x8 - (W1+W7)*x5;
  x8 = W3*(x6+x7);
  x6 = x8 - (W3-W5)*x6;
  x7 = x8 - (W3+W5)*x7;
  
  /* second stage */
  x8 = x0 + x1;
  x0 -= x1;
  x1 = W6*(x3+x2);
  x2 = x1 - (W2+W6)*x2;
  x3 = x1 + (W2-W6)*x3;
  x1 = x4 + x6;
  x4 -= x6;
  x6 = x5 + x7;
  x5 -= x7;
  
  /* third stage */
  x7 = x8 + x3;
  x8 -= x3;
  x3 = x0 + x2;
  x0 -= x2;
  x2 = (181*(x4+x5)+128)>>8;
  x4 = (181*(x4-x5)+128)>>8;
  
  /* fourth stage */
  blk[0] = (x7+x1)>>8;
  blk[1] = (x3+x2)>>8;
  blk[2] = (x0+x4)>>8;
  blk[3] = (x8+x6)>>8;
  blk[4] = (x8-x6)>>8;
  blk[5] = (x0-x4)>>8;
  blk[6] = (x3-x2)>>8;
  blk[7] = (x7-x1)>>8;
}

void idctcol(int* blk)
{
  int x0, x1, x2, x3, x4, x5, x6, x7, x8;

  /* shortcut */
  if (!((x1 = (blk[8*4]<<8)) | (x2 = blk[8*6]) | (x3 = blk[8*2]) |
        (x4 = blk[8*1]) | (x5 = blk[8*7]) | (x6 = blk[8*5]) | (x7 = blk[8*3])))
  {
    blk[8*0]=blk[8*1]=blk[8*2]=blk[8*3]=blk[8*4]=blk[8*5]=blk[8*6]=blk[8*7]=(blk[8*0]+32)>>6;
    return;
  }

  x0 = (blk[8*0]<<8) + 8192;

  /* first stage */
  x8 = W7*(x4+x5) + 4;
  x4 = (x8+(W1-W7)*x4)>>3;
  x5 = (x8-(W1+W7)*x5)>>3;
  x8 = W3*(x6+x7) + 4;
  x6 = (x8-(W3-W5)*x6)>>3;
  x7 = (x8-(W3+W5)*x7)>>3;
  
  /* second stage */
  x8 = x0 + x1;
  x0 -= x1;
  x1 = W6*(x3+x2) + 4;
  x2 = (x1-(W2+W6)*x2)>>3;
  x3 = (x1+(W2-W6)*x3)>>3;
  x1 = x4 + x6;
  x4 -= x6;
  x6 = x5 + x7;
  x5 -= x7;
  
  /* third stage */
  x7 = x8 + x3;
  x8 -= x3;
  x3 = x0 + x2;
  x0 -= x2;
  x2 = (181*(x4+x5)+128)>>8;
  x4 = (181*(x4-x5)+128)>>8;
  
  /* fourth stage */
  blk[8*0] = (x7+x1)>>14;
  blk[8*1] = (x3+x2)>>14;
  blk[8*2] = (x0+x4)>>14;
  blk[8*3] = (x8+x6)>>14;
  blk[8*4] = (x8-x6)>>14;
  blk[8*5] = (x0-x4)>>14;
  blk[8*6] = (x3-x2)>>14;
  blk[8*7] = (x7-x1)>>14;
}

void fidct(int* block)
{
  int i;

  for (i=0; i<8; i++) {
    idctrow(block+8*i);
  }

  for (i=0; i<8; i++) {
    idctcol(block+i);
  }
}

int main(int argc, char* argv[])
{
  int k;
  volatile int b[8][8];
  int* p = &(b[0][0]);
  int  mode = atoi(argv[1]);

  if (mode == 0) {
    init_dct();
    init_idct();

    for (k=0; k<64; k++) {
	float x = pow(3, k);
	int   y = (int) x;
	int   m = y % 75;
	
	p[k] = m;
    }
    
    dct(p);
    fidct(p);

    for (k=0; k<64; k++) {
	printf("%d\n", p[k]);
    }
  }
  else {
    FILE* in  = fopen(argv[2], "r");
    FILE* out = fopen(argv[3], "w");
    int n     = atoi(argv[4]);

    if (mode == 1) {
	init_dct();
	for (k=0; k<n;k++) {
	  fread(p, sizeof(int), 64, in);
	  dct(p);
	  fwrite(p, sizeof(int), 64, out);
	}
    }
    else if (mode == 2) {
	init_idct();
	for (k=0; k<n;k++) {
	  fread(p, sizeof(int), 64, in);
	  idct(p);
	  fwrite(p, sizeof(int), 64, out);
	}
    }
    else if (mode == 3) {
	for (k=0; k<n;k++) {
	  fread(p, sizeof(int), 64, in);
	  fidct(p);
	  fwrite(p, sizeof(int), 64, out);
	}
    }

    fclose(in);
    fclose(out);
  }
}

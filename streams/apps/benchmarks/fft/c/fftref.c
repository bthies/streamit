#if !(defined(FFT2)||defined(FFT3))
#define FFT2
#endif
#ifdef FFT2
#undef FFT3
#endif

#ifdef raw
#include <raw.h>
#else
#include <stdio.h> 
#include <stdlib.h>
#include <unistd.h>
#endif
#include <math.h>  

/************************************************************************/

void init_array(float *A);
void compute_W(float *W_re, float *W_im); 
void output_array(float *A); 
void permute_bitrev(float *A);
int  bitrev(int inp, int numbits); 
int  log_2(int n);  
void fft(float *A, float *W_re, float *W_im);
void begin();

/************************************************************************/

static int n = 64;
static int log2n = 6;
static int numiters = -1;

/* gets no. of points from the user, initialize the points and roots
 * of unity lookup table and lets fft go. finally bit-reverses the
 * results and outputs them into a file. n should be a power of 2.
 */ 
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

void begin()
{
  int i;
  float *A, *W_re, *W_im; 

  A = (float*)malloc(sizeof(float)*2*n);
  W_re = (float*)malloc(sizeof(float)*n/2); 
  W_im = (float*)malloc(sizeof(float)*n/2); 
  /* assert(A_re != NULL && A_im != NULL && W_re != NULL && W_im != NULL);  */
  compute_W(W_re, W_im); 
  
  while (numiters == -1 || numiters-- > 0) {
    init_array(A); 
    fft(A, W_re, W_im);
#ifdef FFT2
    permute_bitrev(A);
#endif
    output_array(A);
  }

  free(A);
  free(W_re); 
  free(W_im); 
}


/* initializes the array with some function of n */  
void init_array(float *A)
{
  int NumPoints, i;
  NumPoints     = 0;

  #ifdef COMMENT_ONLY 
  for(i=0; i < n*2 ; i+=2)
  {
    A_re[NumPoints] = (float)input_buf[i];  
    A_im[NumPoints] = (float)input_buf[i+1];  
    NumPoints++;
  }
  #endif 

  for (i=0; i<n; i++)
  {
      if (i==1) 
      {
        A[2*i] = 1.0f;
        A[2*i+1] = 0.0f;
      }  
      else
      {
        A[2*i] = 0.0f;
        A[2*i+1] = 0.0f;
      } 
      #ifdef COMMENT_ONLY 
      A_re[i] = sin_lookup[i];  /* sin((float)i*2*M_PI/(float)n); */  
      A_im[i] = sin_lookup[i];  /* sin((float)i*2*M_PI/(float)n); */  
      #endif 
  } 
  //A_re[255] = 1.0;  
   
} 


/* W will contain roots of unity so that W[bitrev(i,log2n-1)] = e^(2*pi*i/n)
 * n should be a power of 2
 * Note: W is bit-reversal permuted because fft(..) goes faster if this is
 *       done.  see that function for more details on why we treat 'i' as a
 *       (log2n-1) bit number.
 */
void compute_W(float *W_re, float *W_im)
{
  int i, br;

  for (i=0; i<(n/2); i++)
  {
    br = bitrev(i,log2n-1); 
    W_re[br] = cos(((float)i*2.0*M_PI)/((float)n));  
    W_im[br] = sin(((float)i*2.0*M_PI)/((float)n));  
  }
  #ifdef COMMENT_ONLY 
  for (i=0;i<(n/2);i++)
  { 
    br = i; //bitrev(i,log2n-1); 
    printf("(%g\t%g)\n", W_re[br], W_im[br]);
  }  
  #endif 
}


/* permutes the array using a bit-reversal permutation */ 
void permute_bitrev(float *A)
{ 
  int i, j, i2, bri;
  float t_re, t_im;
  static int* bitrev;
  
  if (!bitrev)
  {
    bitrev = malloc(n * sizeof(int));
    for (i = 0; i < n; i++)
    {
      i2 = i;
      bri = 0;
      for (j=0; j < log2n; j++)
      {
        bri = (bri << 1) | (i2 & 1);
        i2 >>= 1;
      }
      bitrev[i] = bri;
    }
  }

  for (i=0; i<n; i++)
  {
    bri = bitrev[i];
    /* skip already swapped elements */
    if (bri <= i) continue;

    t_re = A[2*i];
    t_im = A[2*i+1];
    A[2*i] = A[2*bri];
    A[2*i+1] = A[2*bri+1];
    A[2*bri] = t_re;
    A[2*bri+1] = t_im;
  }  
} 


/* treats inp as a numbits number and bitreverses it. 
 * inp < 2^(numbits) for meaningful bit-reversal
 */ 
int bitrev(int inp, int numbits)
{
  int i, rev=0;
  for (i=0; i < numbits; i++)
  {
    rev = (rev << 1) | (inp & 1);
    inp >>= 1;
  }
  return rev;
}


/* returns log n (to the base 2), if n is positive and power of 2 */ 
int log_2(int n) 
{
  int res; 
  for (res=0; n >= 2; res++) 
    n = n >> 1; 
  return res; 
}
 
/* fft on a set of n points given by A_re and A_im. Bit-reversal permuted roots-of-unity lookup table
 * is given by W_re and W_im. More specifically,  W is the array of first n/2 nth roots of unity stored
 * in a permuted bitreversal order.
 *
 * FFT - Decimation In Time FFT with input array in correct order and output array in bit-reversed order.
 *
 * REQ: n should be a power of 2 to work. 
 *
 * Note: - See www.cs.berkeley.edu/~randit for her thesis on VIRAM FFTs and other details about VHALF section of the algo
 *         (thesis link - http://www.cs.berkeley.edu/~randit/papers/csd-00-1106.pdf)
 *       - See the foll. CS267 website for details of the Decimation In Time FFT implemented here.
 *         (www.cs.berkeley.edu/~demmel/cs267/lecture24/lecture24.html)
 *       - Also, look "Cormen Leicester Rivest [CLR] - Introduction to Algorithms" book for another variant of Iterative-FFT
 */

void fft(float *A, float *W_re, float *W_im) 
{
  float w_re, w_im, u_re, u_im, t_re, t_im;
  int m, g, b;
  int i, mt, k;

  /* for each stage */  
  for (m=n; m>=2; m=m>>1) 
  {
    /* m = n/2^s; mt = m/2; */
    mt = m >> 1;

    /* for each group of butterfly */ 
    for (g=0,k=0; g<n; g+=m,k++) 
    {
      /* each butterfly group uses only one root of unity. actually, it is the bitrev of this group's number k.
       * BUT 'bitrev' it as a log2n-1 bit number because we are using a lookup array of nth root of unity and
       * using cancellation lemma to scale nth root to n/2, n/4,... th root.
       *
       * It turns out like the foll.
       *   w.re = W[bitrev(k, log2n-1)].re;
       *   w.im = W[bitrev(k, log2n-1)].im;
       * Still, we just use k, because the lookup array itself is bit-reversal permuted. 
       */
      w_re = W_re[k];
      w_im = W_im[k];

      /* for each butterfly */ 
      for (b=g; b<(g+mt); b++) 
      {

        /* t = w * A[b+mt] */
        t_re = w_re * A[2*(b+mt)] - w_im * A[2*(b+mt)+1];
        t_im = w_re * A[2*(b+mt)+1] + w_im * A[2*(b+mt)];

        /* u = A[b]; in[b] = u + t; in[b+mt] = u - t; */
        u_re = A[2*b];
        u_im = A[2*b+1];
        A[2*b] = u_re + t_re;
        A[2*b+1] = u_im + t_im;
        A[2*(b+mt)] = u_re - t_re;
        A[2*(b+mt)+1] = u_im - t_im;
      }
    }
  }
}

void output_array(float *A)
{
#ifdef AVOID_PRINTF
  volatile float result;
#endif
  int i;
  for (i = 0; i < 2*n; i++)
  {
#ifdef AVOID_PRINTF
    result = A[i];
#else
#ifdef raw
    print_float(A[i]);
#else
    printf("%f\n", A[i]);
#endif
#endif
  }
}

#include <stdio.h> 
#include <math.h>   
#include <assert.h> 

/************************************************************************************/

void init_array(int n, double *A_re, double *A_im); 
void compute_W(int n, double *W_re, double *W_im); 
void output_array(int n, double *A_re, double *A_im, char *outfile); 
void permute_bitrev(int n, double *A_re, double *A_im); 
int  bitrev(int inp, int numbits); 
int  log_2(int n);  
void fft(int n, double *A_re, double *A_im, double *W_re, double *W_im);

/************************************************************************************/



/* gets no. of points from the user, initialize the points and roots of unity lookup table 
 * and lets fft go. finally bit-reverses the results and outputs them into a file. 
 * n should be a power of 2. 
 */ 
int main(int argc, char *argv[])
{
  int n;
  int i;
  double *A_re, *A_im, *W_re, *W_im; 
  
  n = 64;

  A_re = (double*)malloc(sizeof(double)*n); 
  A_im = (double*)malloc(sizeof(double)*n); 
  W_re = (double*)malloc(sizeof(double)*n/2); 
  W_im = (double*)malloc(sizeof(double)*n/2); 
  assert(A_re != NULL && A_im != NULL && W_re != NULL && W_im != NULL); 
  
  for (i=0; i<3; i++) {
    init_array(n, A_re, A_im); 
    compute_W(n, W_re, W_im); 
    fft(n, A_re, A_im, W_re, W_im);
    permute_bitrev(n, A_re, A_im);        
    //output_array(n, A_re, A_im, argv[2]);  
    
    print_string("done");
  }
    
  free(A_re); 
  free(A_im); 
  free(W_re); 
  free(W_im); 
  exit(0);

}


/* initializes the array with some function of n */  
void init_array(int n, double *A_re, double *A_im) 
{
  int NumPoints, i;
  NumPoints     = 0;

  #ifdef COMMENT_ONLY 
  for(i=0; i < n*2 ; i+=2)
  {
    A_re[NumPoints] = (double)input_buf[i];  
    A_im[NumPoints] = (double)input_buf[i+1];  
    /* printf("%d,%d -> %g,%g\n", input_buf[i], input_buf[i+1], A_re[NumPoints], A_im[NumPoints]); */  
    /* printf("%g %g\n", A_re[NumPoints], A_im[NumPoints]);  */  
    NumPoints++;
  }
  #endif 

  for (i=0; i<n; i++)
  {
      if (i==1) 
      {
        A_re[i]=1.0; 
        A_im[i]=0.0; 
      }  
      else
      {
        A_re[i]=0.0; 
        A_im[i]=0.0; 
      } 
      #ifdef COMMENT_ONLY 
      A_re[i] = sin_lookup[i];  /* sin((double)i*2*M_PI/(double)n); */  
      A_im[i] = sin_lookup[i];  /* sin((double)i*2*M_PI/(double)n); */  
      #endif 
  } 
  //A_re[255] = 1.0;  
   
} 


/* W will contain roots of unity so that W[bitrev(i,log2n-1)] = e^(2*pi*i/n)
 * n should be a power of 2
 * Note: W is bit-reversal permuted because fft(..) goes faster if this is done.
 *       see that function for more details on why we treat 'i' as a (log2n-1) bit number.
 */
void compute_W(int n, double *W_re, double *W_im)
{
  int i, br;
  int log2n = log_2(n);

  for (i=0; i<(n/2); i++)
  {
    br = bitrev(i,log2n-1); 
    W_re[br] = cos(((double)i*2.0*M_PI)/((double)n));  
    W_im[br] = sin(((double)i*2.0*M_PI)/((double)n));  
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
void permute_bitrev(int n, double *A_re, double *A_im) 
{ 
  int i, bri, log2n;
  double t_re, t_im;

  log2n = log_2(n); 
  
  for (i=0; i<n; i++)
  {
      bri = bitrev(i, log2n);

      /* skip already swapped elements */
      if (bri <= i) continue;

      t_re = A_re[i];
      t_im = A_im[i];
      A_re[i]= A_re[bri];
      A_im[i]= A_im[bri];
      A_re[bri]= t_re;
      A_im[bri]= t_im;
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

void fft(int n, double *A_re, double *A_im, double *W_re, double *W_im) 
{
  double w_re, w_im, u_re, u_im, t_re, t_im;
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
        /* printf("bf %d %d %d %f %f %f %f\n", m, g, b, A_re[b], A_im[b], A_re[b+mt], A_im[b+mt]);
         */ 
        //printf("bf %d %d %d (u,t) %g %g %g %g (w) %g %g\n", m, g, b, A_re[b], A_im[b], A_re[b+mt], A_im[b+mt], w_re, w_im);

        /* t = w * A[b+mt] */
        t_re = w_re * A_re[b+mt] - w_im * A_im[b+mt];
        t_im = w_re * A_im[b+mt] + w_im * A_re[b+mt];

        /* u = A[b]; in[b] = u + t; in[b+mt] = u - t; */
        u_re = A_re[b];
        u_im = A_im[b];
        A_re[b] = u_re + t_re;
        A_im[b] = u_im + t_im;
        A_re[b+mt] = u_re - t_re;
        A_im[b+mt] = u_im - t_im;

        /*  printf("af %d %d %d %f %f %f %f\n", m, g, b, A_re[b], A_im[b], A_re[b+mt], A_im[b+mt]);
         */         
        //printf("af %d %d %d (u,t) %g %g %g %g (w) %g %g\n", m, g, b, A_re[b], A_im[b], A_re[b+mt], A_im[b+mt], w_re, w_im);
      }
    }
  }
}

#include <stdio.h> 
#include <math.h> 

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


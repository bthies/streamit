/** 
 * FFT3.java - Standard FFT implementation with bit-reversed output.  
 *             Works only for power-of-2 sizes starting from 4. 
 * 
 * Note: 
 * 1. Each input element is also referred to as a point in the comments in this file.  
 * 2. Most of the multiply_by_2 and 2 that you see in this file are for handling  
 *    the real and imag parts of each complex point 
 * 
 * For a quick explanation/diagram, you can see "Review of the FFT" at 
 * www.cs.berkeley.edu/~demmel/cs267-1995/lecture24/lecture24.html
 */ 

import streamit.library.*; 
import java.lang.*; 
import java.util.*; 


/**
 * If u and t are complex points, then an in-place butterfly is:  
 * u = u + Wt;  
 * t = u - Wt; 
 * where, W is a root of unity. 
 */  
class Butterfly extends Filter  
{ 
  float w_re, w_im; 

  public Butterfly(float w_re, float w_im) 
  { 
    super(w_re, w_im); 
  } 
  public void init(final float w_re, final float w_im) 
  {
    input = new Channel(Float.TYPE, 4); 
    output = new Channel(Float.TYPE, 4); 
    this.w_re = w_re; 
    this.w_im = w_im; 
  } 
  public void work() 
  {
    float u_re, u_im, t_re, t_im, wt_re, wt_im;
    u_re = input.popFloat(); u_im = input.popFloat(); 
    t_re = input.popFloat(); t_im = input.popFloat(); 

    /* compute Wt */ 
    wt_re = w_re * t_re - w_im * t_im; 
    wt_im = w_re * t_im + w_im * t_re; 
    /* the butterfly computation: u = u + Wt; and t = u - Wt; */ 
    /* note the order: t is computed first */ 
    t_re = u_re - wt_re; 
    t_im = u_im - wt_im; 
    u_re = u_re + wt_re; 
    u_im = u_im + wt_im; 

    output.pushFloat(u_re); output.pushFloat(u_im); 
    output.pushFloat(t_re); output.pushFloat(t_im); 
  } 
} 

/**  
 * A butterfly group of a particular fft stage is a set 
 * of butterflies that use the same root of unity W. Or 
 * graphically, each butterfly group is a bunch of  
 * butterflies that are clustered together in the FFT's 
 * butterfly graph for that particular stage 
 */     
class ButterflyGroup extends SplitJoin 
{
  public ButterflyGroup(float w_re, float w_im, int numbflies) 
  { 
    super(w_re, w_im, numbflies); 
  } 
  public void init(final float w_re, final float w_im, final int numbflies) 
  {
    /* The splitjoin routes the complex points in a round-robin fashion,  
     * one each to/from the constituent butterflies.  
     */
    this.setSplitter(ROUND_ROBIN(2)); 
    /* for each butterfly in this group */  
    for (int b=0; b<numbflies; b++)
      this.add(new Butterfly(w_re, w_im));  
    this.setJoiner(ROUND_ROBIN(2)); 
  }
} 

/**
 * ComputeStage is a set of butterfly groups and implements a  
 * a particular FFT stage (which is determined by D).  
 * 
 */  
class ComputeStage extends SplitJoin 
{
  public ComputeStage(int D, int N, float W_re3a[], float W_im3a[]) 
  { 
    super(D, N, W_re3a, W_im3a); 
  } 
  public void init(final int D, final int N, final float W_re3[], final float W_im3[]) 
  {
    /* the length of a butterfly group in terms of points */ 
    final int grplen = D + D;  
    /* the number of bfly groups and bflies per group */ 
    final int numgrps = N/grplen; 
    final int numbflies = D; 
    float w_re, w_im; 

    /* The splitjoin routes the complex points "grplen" each to/from the constituent bflygrps. */
    this.setSplitter(ROUND_ROBIN(2*grplen)); 
    /* for each butterfly group in this stage */
    for (int g=0; g<numgrps; g++) 
    { 
      /* each butterfly group uses only one root of unity. actually, it is the bitrev of this group's number g.
       * BUT 'bitrev' it as a logn-1 bit number because we are using a lookup array of nth root of unity and
       * using cancellation lemma to scale nth root to n/2, n/4,... th root.
       *
       * Basically, it turns out like the foll.
       *   w_re = W_re[bitrev(g, logn-1)];
       *   w_im = W_im[bitrev(g, logn-1)];
       * Still, we just use g, because the lookup array itself is bit-reversal permuted.
       */
      w_re = W_re3[g];  
      w_im = W_im3[g]; 

      this.add(new ButterflyGroup(w_re, w_im, numbflies));
    }  
    this.setJoiner(ROUND_ROBIN(2*grplen)); 
  }  
} 

/**
 * Same as ComputeStage but this class is only for the last fft stage, where there are 
 * there are N/2 butterfly groups with ONLY one butterfly each (So, the butterfly group 
 * degenerates from a splitjoin to a filter) 
 */  
class LastComputeStage extends SplitJoin 
{
  public LastComputeStage(int D, int N, float W_re4[], float W_im4[]) 
  { 
    super(D, N, W_re4, W_im4); 
  } 
  public void init(final int D, final int N, final float W_re5[], final float W_im5[]) 
  {
    /* the length of a butterfly group in terms of points */ 
    final int grplen = D + D;  
    /* the number of bfly groups and bflies per group */ 
    final int numgrps = N/grplen; 
    final int numbflies = D; 
    float w_re, w_im; 

    /* assertions for the last fft stage */ 
    //ASSERT(numgrps==N/2 && numbflies==1); 

    /* The splitjoin routes the complex points "grplen" each to/from the constituent bflygrps. */
    this.setSplitter(ROUND_ROBIN(2*grplen)); 
    /* for each butterfly group in this stage */
    for (int g=0; g<numgrps; g++) 
    {
      /* see ComputeStage class for details on indexing W[] */  
      w_re = W_re5[g];  
      w_im = W_im5[g]; 

      /* bflygrp of the last fft stage is simply a bfly */ 
      this.add(new Butterfly(w_re, w_im));
    }  
    this.setJoiner(ROUND_ROBIN(2*grplen)); 
  }  
}
 
/** 
 * BitReverse - coarse-grained implementation using peek 
 */ 
class BitReverse extends Filter  
{
  int N, logN; 

  public BitReverse(int N, int logN) 
  { 
    super(N, logN); 
  }  
  public void init(final int N, final int logN) 
  { 
    input = new Channel(Float.TYPE, 2*N, 2*N); 
    output = new Channel(Float.TYPE, 2*N);  
    this.N = N; 
    this.logN = logN; 
  } 
  public void work() 
  { 
    for (int i=0; i<N; i++) 
    { 
      int br = bitrev(i, logN); 
      /* treat real and imag part of one point together */  
      output.pushFloat(input.peekFloat(2*br)); 
      output.pushFloat(input.peekFloat(2*br+1)); 
    }   
    for (int i=0; i<N; i++)
    {  
      input.popFloat(); 
      input.popFloat(); 
    } 
  }  
  /* The same bitrev function as in class FFT3 
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
} 

/** 
 * The top-level stream construct of the FFT kernel  
 */  
class FFT3Kernel extends Pipeline
{
  public FFT3Kernel(int N, int logN, float W_re2a[], float W_im2a[])  
  { 
    super(N, logN, W_re2a, W_im2a); 
  } 
  public void init(final int N, final int logN, final float W_re2[], final float W_im2[]) 
  {
    /* The logN computation stages followed by a bit-reversal phase. 
     * For more info on indexing "roots of unity" array W[], see ComputeStage class.
     *  
     * (The first and last fft stages need special care bcoz - If we use  
     * the same ComputeStage class, it will result in a splitjoin with only one branch!)  
     */ 

    /* the first ComputeStage - 1 bflygrp with N/2 bflies */ 
    this.add(new ButterflyGroup(W_re2[0], W_im2[0], N/2));   

    /* the middle ComputeStages - N/2i bflygrps with i bflies each */ 
    for (int i=(N/4); i>1; i=i/2) 
      this.add(new ComputeStage(i, N, W_re2, W_im2)); 

    /* the last ComputeStage - N/2 bflygrps with 1 bfly each */ 
    this.add(new LastComputeStage(1, N, W_re2, W_im2)); 

    /* the bit-reversal phase */ 
    this.add(new BitReverse(N, logN)); 
  } 
} 

/** 
 * Sends out real followed by imag part of each input point  
 */ 
class ComplexSource extends Filter  
{
  /* the input array A[] */  
  float A_re[], A_im[]; 
  int N; 
  int idx; 

  public ComplexSource(int N) 
  {
    super(N);  
  } 
  public void init(final int N) 
  { 
    output = new Channel(Float.TYPE, 2); 
    this.N = N; 
    idx = 0; 

    /* Initialize the input. In future, might 
     * want to read from file. 
     */ 
    A_re = new float[N]; 
    A_im = new float[N]; 
    for (int i=0; i<N; i++) 
    { 
      A_re[i] = (float)0.0; 
      A_im[i] = (float)0.0; 
    } 
    A_re[1] = (float)1.0; /* to get sin/cos wave for re/im */  
  } 
  public void work() 
  { 
    output.pushFloat(A_re[idx]); 
    output.pushFloat(A_im[idx]); 
    idx++; 
    if (idx >= N) idx=0; 
  } 
} 

/** 
 * Prints the real and imag part of each output point 
 */ 
class ComplexPrinter extends Filter 
{ 
  public void init() 
  { 
    input = new Channel(Float.TYPE, 2); 
  }  
  public void work() 
  {
    //System.out.println(input.popFloat() + "\t\t" + input.popFloat()); 
    System.out.println(input.popFloat()); System.out.println(input.popFloat()); 
  } 
} 

/** 
 * The driver class 
 */ 
class FFT3 extends StreamIt 
{
  //int N; 
  //int logN; 
  //float W_re[]; 
  //float W_im[]; 

  public static void main(String args[]) 
  { 
    (new FFT3()).run(args); 
  }  
  public void init() 
  { 
    /* Make sure N is a power_of_2, N >= 4 and 2^logN = N */  
    final int N =  32; //16; 
    final int logN = 5; //4;   
    float W_re1[]; 
    float W_im1[]; 

    /* Initialize roots of unity array W[].   
     * W[] is bit-reversal permuted for easier access later -   
     * see the comment inside 'class ComputeStage' 
     */ 
    W_re1 = new float[N/2]; 
    W_im1 = new float[N/2]; 
    for (int i=0; i<(N/2); i++) 
    {
	//int br = bitrev(i, logN-1);  
	//inlined bitrev to help constant propagation 
	int br=0;
	int j, temp;
	temp=i;
	for (j=0; j < (logN-1); j++)
	{
		br = (br << 1) | (temp & 1);
		temp >>= 1;
	}
	W_re1[br] = (float) Math.cos(((double)i*2.0*Math.PI)/((double)N)); 
	W_im1[br] = (float) Math.sin(((double)i*2.0*Math.PI)/((double)N)); 
    }  
    //for (int i=0; i<(N/2); i++) 
    //  System.out.println("WW "+W_re[i]+" "+W_im[i]); 
    
    /* the tapes contain real followed by imag parts of each complex point */ 
    this.add(new ComplexSource(N)); 
    this.add(new FFT3Kernel(N, logN, W_re1, W_im1)); 
    this.add(new ComplexPrinter()); 
  } 
  /* Helper fn - treats inp as a numbits number and bitreverses it.
   * inp < 2^(numbits) for meaningful bit-reversal
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
  */
} 

/************************************************************************************************************/ 
/************************************************************************************************************/ 
/* The following unused classes are not removed because they might be of some use in future implementations */
/* not a general bitreverse. eg. it didn't work for N=16 */  
/* class BitReverse extends SplitJoin 
{
  public BitReverse(int N) 
  { 
    super(N); 
  }  
  public void init(final int N) 
  {
    this.setSplitter(WEIGHTED_ROUND_ROBIN(2*(N/2), 2*(N/2)));  
    for (int i=1; i<=2; i++) 
      this.add(new SplitJoin() 
      { 
        public void init() 
        {
          this.setSplitter(WEIGHTED_ROUND_ROBIN(2, 2));
          this.add(new IdentityComplex()); 
          this.add(new IdentityComplex());  
          this.setJoiner(WEIGHTED_ROUND_ROBIN(2*(N/4), 2*(N/4)));    
        } 
      });  
    this.setJoiner(WEIGHTED_ROUND_ROBIN(2, 2)); 
  } 
} 
*/ 
/* takes a complex point and passes it through */ 
/* class IdentityComplex extends Filter 
{ 
  public void init() 
  { 
    input = new Channel(Float.TYPE, 2); 
    output = new Channel(Float.TYPE, 2); 
  } 
  public void work() 
  { 
    output.pushFloat(input.popFloat()); 
    output.pushFloat(input.popFloat()); 
  } 
} */ 

 


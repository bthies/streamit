/** 
 * VectAdd1.java - a very simple program -> add two vectors (C[] = A[] + B[])  
 * 
 * Same as VectAdd.java but the only difference is the class where the two 
 * input arrays (A[] and B[]) reside.    
 */ 

import streamit.library.*; 


/**
 * The main kernel: i1 + i2 -> o1 (add two numbers to produce an output)  
 */  
class VectAddKernel extends Filter  
{ 
  public void init() 
  {
    input = new Channel(Integer.TYPE, 2); 
    output = new Channel(Integer.TYPE, 1); 
  } 
  public void work() 
  {
    output.pushInt(input.popInt() + input.popInt()); 
  } 
} 

/** 
 * A simple source that sends out elements from a vector repeatedly.  
 */ 
class VectSource extends Filter  
{
  int N;
  int idx;  
  int Z[]; 

  /* the input vectors */  
  int A[], B[];  

  //public VectSource(int N, int Z[]) 
  public VectSource(int N, int selsrc) 
  {
    //super(N, Z);  
    super(N, selsrc);  
  } 
  //public void init(final int N, final int Z[]) 
  public void init(final int N, final int selsrc) 
  { 
    output = new Channel(Integer.TYPE, 1); 
    this.N = N; 
    this.idx = 0;
  
    /* set up the input vectors */ 
    A = new int[N]; 
    B = new int[N]; 
    for (int i=0; i<N; i++) 
    { 
      A[i] = i; 
      B[i] = i; //N-i; 
    }  
    
    if (selsrc == 1) 
      this.Z = A; 
    else 
      this.Z = B; 

    //this.Z = Z; 
  } 
  public void work() 
  { 
    output.pushInt(Z[idx]); 
    idx++; 
    if (idx >= N) idx = 0; 
  } 
} 

/** 
 * Sends out elements from the two input vectors 
 */ 
class TwoVectSource extends SplitJoin 
{ 
  /* the input vectors */  
  /* int A[], B[]; */ 

  public TwoVectSource(int N) 
  { 
    super(N); 
  } 
  public void init(final int N) 
  { 
    /* set up the input vectors */ 
    /* A = new int[N]; 
    B = new int[N]; 
    for (int i=0; i<N; i++) 
    { 
      A[i] = i; 
      B[i] = i; //N-i; 
    } */ 

    /* generate and mix the two streams */ 
    this.setSplitter(NULL()); 
    this.add(new VectSource(N, 1)); 
    this.add(new VectSource(N, 2));
    this.setJoiner(ROUND_ROBIN());  
  } 
} 

/** 
 * Prints the output vector  
 */ 
class VectPrinter extends Filter 
{ 
  public void init() 
  { 
    input = new Channel(Integer.TYPE, 1); 
  }  
  public void work() 
  {
    System.out.println(input.popInt()); 
  } 
} 

/** 
 * The driver class 
 */ 
class VectAdd1 extends StreamIt 
{
  public static void main(String args[]) 
  { 
    (new VectAdd1()).run(args); 
  }  
  public void init() 
  { 
    final int N =  10; 

    this.add(new TwoVectSource(N)); 
    this.add(new VectAddKernel()); 
    this.add(new VectPrinter()); 
  } 
} 


/** 
 * BitonicSort.java - Batcher's bitonic sort network 
 *                    Implementation works only for power-of-2 sizes starting from 2. 
 * 
 * Note: 
 * 1. Each input element is also referred to as a key in the comments in this file.  
 * 2. BitonicSort of N keys is done using logN merge stages and each merge stage is made up of 
 *    lopP steps (P goes like 2, 4, ... N for the logN merge stages)  
 *  
 * See Knuth "The Art of Computer Programming" Section 5.3.4 - "Networks for Sorting" (particularly the diagram titled "A nonstandard 
 * sorting network based on bitonic sorting" in the First Set of Exercises - Fig 56 in second edition)  
 * Here is an online reference: http://www.iti.fh-flensburg.de/lang/algorithmen/sortieren/bitonic/bitonicen.htm 
 */ 

import streamit.library.*; 


/**
 * Compares the two input keys and exchanges their order if they are not 
 * sorted.  
 * 
 * sortdir determines if the sort is nondecreasing (UP) or nonincreasing (DOWN).  
 * 'true' indicates UP sort and 'false' indicates DOWN sort.  
 */ 
class CompareExchange extends Filter  
{ 
  boolean sortdir; 
  public CompareExchange(boolean sortdir) 
  { 
    super(sortdir); 
  } 
  public void init(final boolean sortdir) 
  {
    input = new Channel(Integer.TYPE, 2); 
    output = new Channel(Integer.TYPE, 2); 
    this.sortdir = sortdir; 
  } 
  public void work() 
  {
    /* the input keys and min,max keys */ 
    int k1, k2, mink, maxk; 

    k1 = input.popInt(); 
    k2 = input.popInt(); 
    if (k1 <= k2)
    {  
      mink = k1; 
      maxk = k2; 
    } 
    else /* k1 > k2 */ 
    { 
      mink = k2; 
      maxk = k1; 
    } 
 
    if (sortdir == true) 
    { 
      /* UP sort */ 
      output.pushInt(mink); 
      output.pushInt(maxk); 
    } 
    else /* sortdir == false */ 
    { 
      /* DOWN sort */ 
      output.pushInt(maxk); 
      output.pushInt(mink); 
    } 
  } 
} 
 
/** 
 * Partition the input bitonic sequence of length L into two bitonic sequences 
 * of length L/2, with all numbers in the first sequence <= all numbers in the 
 * second sequence if sortdir is UP (similar case for DOWN sortdir)  
 * 
 * Graphically, it is a bunch of CompareExchanges with same sortdir, clustered 
 * together in the sort network at a particular step (of some merge stage). 
 */ 
class PartitionBitonicSequence extends SplitJoin 
{ 
  public PartitionBitonicSequence(int L, boolean sortdir) 
  { 
   super(L, sortdir); 
  }
  public void init(final int L, final boolean sortdir) 
  { 
    /* Each CompareExchange examines keys that are L/2 elements apart */  
    this.setSplitter(ROUND_ROBIN()); 
    for (int i=0; i<(L/2); i++) 
      this.add(new CompareExchange(sortdir)); 
    this.setJoiner(ROUND_ROBIN()); 
  } 
} 

/** 
 * One step of a particular merge stage (used by all merge stages except the last) 
 * 
 * dircnt determines which step we are in the current merge stage 
 * (which in turn is determined by <L, numseqp>) 
 */  
class StepOfMerge extends SplitJoin
{ 
  public StepOfMerge(int L, int numseqp, int dircnt) 
  { 
    super(L, numseqp, dircnt); 
  } 
  public void init(final int L, final int numseqp, final int dircnt) 
  { 
    boolean curdir;   
    this.setSplitter(ROUND_ROBIN(L));  
    for (int j=0; j<numseqp; j++) 
    {
      /* finding out the curdir is a bit tricky - the direction depends 
       * only on the subsequence number during the FIRST step. So to 
       * determine the FIRST step subsequence to which this sequence belongs, 
       * divide this sequence's number j by dircnt (bcoz 'dircnt' tells how many 
       * subsequences of the current step make up one subsequence of the FIRST step). Then, 
       * test if that result is even or odd to determine if curdir is UP or DOWN respec.  
       */   
      curdir = ( (j/dircnt)%2 == 0 );
      /* The last step needs special care to avoid splitjoins with just one branch. */  
      if (L > 2)  
        this.add(new PartitionBitonicSequence(L, curdir)); 
      else /* PartitionBitonicSequence of the last step (L=2) is simply a CompareExchange */   
        this.add(new CompareExchange(curdir)); 
    } 
    this.setJoiner(ROUND_ROBIN(L)); 
  } 
} 

/** 
 * One step of the last merge stage  
 *
 * Main difference form StepOfMerge is the direction of sort. 
 * It is always in the same direction - sortdir.  
 */  
class StepOfLastMerge extends SplitJoin
{ 
  public StepOfLastMerge(int L, int numseqp, boolean sortdir) 
  { 
    super(L, numseqp, sortdir); 
  } 
  public void init(final int L, final int numseqp, final boolean sortdir) 
  { 
    this.setSplitter(ROUND_ROBIN(L));  
    for (int j=0; j<numseqp; j++) 
    {  
      /* The last step needs special care to avoid splitjoins with just one branch. */  
      if (L > 2)  
        this.add(new PartitionBitonicSequence(L, sortdir)); 
      else /* PartitionBitonicSequence of the last step (L=2) is simply a CompareExchange */   
        this.add(new CompareExchange(sortdir)); 
    } 
    this.setJoiner(ROUND_ROBIN(L)); 
  } 
} 

/* Divide the input sequence of length N into subsequences of length P and sort each of them 
 * (either UP or DOWN depending on what subsequence number [0 to N/P-1] they get - All even  
 * subsequences are sorted UP and all odd subsequences are sorted DOWN) 
 * In short, a MergeStage is N/P Bitonic Sorters of order P each.    
 * 
 * But, this MergeStage is implemented *iteratively* as logP STEPS. 
 */ 
class MergeStage extends Pipeline 
{
  public MergeStage(int P, int N) 
  { 
    super(P, N); 
  }
  public void init(final int P, final int N) 
  { 
    int L, numseqp, dircnt;   
    /* for each of the lopP steps (except the last step) of this merge stage */  
    for (int i=1; i<P; i=i*2) 
    {
      /* length of each sequence for the current step - goes like P,P/2,...,2 */ 
      L = P/i;
      /* numseqp is the number of PartitionBitonicSequence-rs in this step */ 
      numseqp = (N/P)*i;
      dircnt = i;  
      
      this.add(new StepOfMerge(L, numseqp, dircnt));  
    } 
  } 
} 
  
/**  
 * The LastMergeStage is basically one Bitonic Sorter of order N i.e., it takes the  
 * bitonic sequence produced by the previous merge stages and applies a  
 * bitonic merge on it to produce the final sorted sequence.    
 *  
 * This is implemented iteratively as logN steps
 */ 
class LastMergeStage extends Pipeline 
{
  public LastMergeStage(int N, boolean sortdir) 
  { 
    super(N, sortdir); 
  }
  public void init(final int N, final boolean sortdir) 
  { 
    int L, numseqp;   
    /* for each of the logN steps (except the last step) of this merge stage */  
    for (int i=1; i<N; i=i*2) 
    {
      /* length of each sequence for the current step - goes like N,N/2,...,2 */ 
      L = N/i;
      /* numseqp is the number of PartitionBitonicSequence-rs in this step */ 
      numseqp = i; 
     
      this.add(new StepOfLastMerge(L, numseqp, sortdir)); 
    }
  } 
} 

/** 
 * The top-level kernel of bitonic-sort (iterative version) -  
 * It has logN merge stages and all merge stages except the last 
 * progressively builds a bitonic sequence out of the input 
 * sequence. 
 * The last merge stage acts on the resultant bitonic sequence  
 * to produce the final sorted sequence (sortdir determines if it is 
 * UP or DOWN). 
 */  
class BitonicSortKernel extends Pipeline 
{
  public BitonicSortKernel(int N, boolean sortdir) 
  { 
    super(N, sortdir); 
  } 
  public void init(final int N, final boolean sortdir) 
  {
    for (int i=2; i<=(N/2); i=2*i)
      this.add(new MergeStage(i, N)); 
    this.add(new LastMergeStage(N, sortdir)); 
  } 
} 

/**
 * Creates N keys and sends it out  
 */
class KeySource extends Filter
{
  int N;
  int A[]; 
 
  public KeySource(int N)
  {
    super(N);
  }
  public void init(final int N)
  {
    output = new Channel(Integer.TYPE, N);
    this.N = N;

    /* Initialize the input. In future, might
     * want to read from file or generate a random 
     * permutation.
     */
    A = new int[N];
    for (int i=0; i<N; i++)
      A[i] = (N-i);
  }
  public void work()
  {
    for (int i=0; i<N; i++)
      output.pushInt(A[i]);
  }                          
} 

/**
 * Prints out the sorted keys and verifies if they 
 * are sorted.  
 */
class KeyPrinter extends Filter
{
  int N; 
  public KeyPrinter(int N)
  {
    super(N);
  }
  public void init(final int N)
  {
    input = new Channel(Integer.TYPE, N);
    this.N = N;
  }
  public void work()
  {
    for (int i=0; i<(N-1); i++)
    {
      /* verifies if it is UP sorted */  
      //ASSERT(input.peekInt(0) <= input.peekInt(1));   
      /* verifies if it is DOWN sorted */  
      //ASSERT(input.peekInt(0) >= input.peekInt(1));   
      System.out.println(input.popInt()); 
    } 
    System.out.println(input.popInt()); 
  }                          
}
 
/** 
 * The driver class 
 */ 
class BitonicSort extends StreamIt 
{
  public static void main(String args[]) 
  { 
    (new BitonicSort()).run(args); 
  }  
  public void init() 
  { 
    /* Make sure N is a power_of_2 */  
    final int N = 32; //16;
    /* true for UP sort and false for DOWN sort */ 
    final boolean sortdir = true;  
 
    this.add(new KeySource(N)); 
    this.add(new BitonicSortKernel(N, sortdir)); 
    //this.add(new BitonicSortKernel(N, !sortdir)); 
    this.add(new KeyPrinter(N)); 
  } 
} 

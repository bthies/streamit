/**
 *  
 * BitonicSort.java - Batcher's bitonic sort network 
 *                    Implementation works only for power-of-2 sizes starting from 2. 
 * 
 * Note: 
 * 1. Each input element is also referred to as a key in the comments in this file.  
 * 2. BitonicSort of N keys is done using logN merge stages and each merge stage is made up of 
 *    lopP steps (P goes like 2, 4, ... N)  
 *  
 * See Knuth "The Art of Computer Programming" Section 5.3.4 - "Networks for Sorting" (particularly 
 * the diagram titled "A nonstandard sorting network based on bitonic sorting" in the First Set of 
 * Exercises - Fig 56 in second edition)
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
		//ASSERT(input.peekInt(0) <= input.peekInt(1));
		System.out.println(input.popInt()); 
	    } 
	System.out.println(input.popInt()); 
    }                          
}
 
/** 
 * The driver class 
 */ 
class BitonicSortRecursive extends StreamIt 
{
    public static void main(String args[]) 
    { 
	(new BitonicSortRecursive()).run(args); 
    }  
    public void init() 
    { 
	/* Make sure N is a power_of_2 */  
	final int N = 32; //16;
 
	this.add(new KeySource(N)); 
	this.add(new BitonicSortKernelRecursive(N, true)); /* true for UP sort */ 
	this.add(new KeyPrinter(N)); 
    } 
} 

/** 
 * The top-level kernel of bitonic-sort (recursive version) -  
 * First produces a bitonic sequence by recursively sorting its two halves in 
 * opposite directions and then uses BitonicMerge to merge them into one 
 * sorted sequence. 
 */  
class BitonicSortKernelRecursive extends Pipeline 
{
    public BitonicSortKernelRecursive(int L, boolean sortdir) 
    { 
	super(L, sortdir); 
    } 
    public void init(final int L, final boolean sortdir) 
    {
	if (L > 1) 
	    { 
		/* Produce a bitonic sequence */  
		this.add(new SplitJoin(L, sortdir) /* ProduceBitonicSequence */  
		    {  
			public void init(final int L, final boolean sortdir) 
			{ 
			    this.setSplitter(WEIGHTED_ROUND_ROBIN(L/2, L/2)); 
			    this.add(new BitonicSortKernelRecursive(L/2, sortdir)); 
			    this.add(new BitonicSortKernelRecursive(L/2, !sortdir)); 
			    this.setJoiner(WEIGHTED_ROUND_ROBIN(L/2, L/2)); 
			} 
		    });
		/* BitonicMerge the resulting bitonic sequence */ 
		this.add(new BitonicMergeRecursive(L, sortdir));  
	    }
	else 
	    { 
		this.add(new Filter() /* IdentityFilter */  
		    { 
			public void init() 
			{ 
			    input = new Channel(Integer.TYPE, 1); 
			    output = new Channel(Integer.TYPE, 1); 
			} 
			public void work() 
			{ 
			    output.pushInt(input.popInt()); 
			} 
		    }); 
	    }  
    } 
}

/** 
 * BitonicMerge recursively sorts a bitonic sequence of length L.    
 * It sorts UP if the sortdir is true and sorts DOWN otherwise.  
 */ 
class BitonicMergeRecursive extends Pipeline 
{ 
    public BitonicMergeRecursive(int L, boolean sortdir) 
    { 
	super(L, sortdir); 
    }
    public void init(final int L, final boolean sortdir) 
    { 
	/* Partition the bitonic sequence into two bitonic sequences 
	 * with all numbers in the first sequence <= all numbers in the 
	 * second sequence if sortdir is UP (similar case for DOWN sortdir)  
	 */ 
	this.add(new SplitJoin() /* PartitionBitonicSequence */  
	    { 
		public void init() 
		{
		    /* Each CompareExchange examines keys that are L/2 elements apart */  
		    this.setSplitter(ROUND_ROBIN()); 
		    for (int i=0; i<L/2; i++) 
			this.add(new CompareExchange(sortdir)); 
		    this.setJoiner(ROUND_ROBIN());
		} 
	    }); 
	/* Recursively sort the two bitonic sequences obtained from  
	 * the PartitionBitonicSequence step  
	 */
	if (L>2) {
	    this.add(new SplitJoin() 
		{
		    public void init() 
		    {
			this.setSplitter(WEIGHTED_ROUND_ROBIN(L/2, L/2));  
			this.add(new BitonicMergeRecursive(L/2, sortdir)); 
			this.add(new BitonicMergeRecursive(L/2, sortdir)); 
			this.setJoiner(WEIGHTED_ROUND_ROBIN(L/2, L/2));  
		    } 
		}); 
	}
    } 
}

import streamit.*;

/**
 * The merger component of the merge sort.  Combines two sorted
 * streams into another sorted stream, producing a total of <N>
 * elements.
 */
class Merger extends Filter {
    // the number of inputs to merge
    int N;

    public Merger(int N) {
	super(N);
    }

    public void init(int N) {
	this.N = N;
	input = new Channel(Integer.TYPE, N);
	output = new Channel(Integer.TYPE, N);
    }

    public void work() {
	// initialize indices
	int index1 = 0;
	int index2 = 1;
	
	// merge values
	while (index1<=N-2 && index2<=N-1) {
	    int val1 = input.peekInt(index1);
	    int val2 = input.peekInt(index2);
	    if (val1 <= val2) {
		output.pushInt(val1);
		index1++;
	    } else {
		output.pushInt(val2);
		index2++;
	    }
	}

	// merge remainder if one stream dries out
	if (index1<=N-2) {
	    // then index1 <= N-2
	    for (int i=index1; i<=N-2; i+=2) {
		output.pushInt(input.peekInt(index1));
	    }
	} else {
	    // then index2 <= N-1
	    for (int i=index2; i<=N-1; i+=2) {
		output.pushInt(input.peekInt(index2));
	    }
	}

	// pop all the inputs
	for (int i=0; i<N; i++) {
	    input.popInt();
	}
    }
    
}

/**
 * Sorts a stream of integers.
 */
class Sorter extends Pipeline {
    
    public Sorter(int N) {
	super(N);
    }

    public void init(final int N) {
	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    this.add(new Sorter(N/2));
		    this.add(new Sorter(N/2));
		    setJoiner(ROUND_ROBIN());
		}
	    });
	add(new Merger(N));
    }

}

/**
 * Repeatedly pushes a sequence of <N> items in reverse order.
 */
class SortInput extends Filter {
    // the number to count up to
    int N;

    public SortInput(int N) {
	super(N);
    }
    
    public void init(int N) {
	this.N = N;
	output = new Channel(Integer.TYPE, N);
    }
    
    public void work() {
	for (int i=0; i<N; i++) {
	    output.pushInt(N-i);
	}
    }
}

/**
 * Prints an integer stream.
 */
class IntPrinter extends Filter {

    public void init () {
        input = new Channel(Integer.TYPE, 1);
    }

    public void work () {
        System.out.println(input.popInt());
    }
}

/**
 * The toplevel class.  Should run the resulting input <MULT> times.
 */
class MergeSort extends StreamIt {

    public static void main(String args[]) {
	new MergeSort().run(args);
    }

    public void init() {
	// we assume an input length that is a power of two
	final int NUM_INPUTS = 16;
	// the number of times to run the input (this is the number of
	// times we break up the input sequence--just a testing thing.)
	final int MULT = 4;
	
	// add the input generator
	add(new SortInput(NUM_INPUTS/MULT));
	// add the sorters mergers
	add(new Sorter(NUM_INPUTS));
	// add a printer
	add(new IntPrinter());
    }

}

/*
 * Simple Trellis decoder. Uses the ideas
 * from http://pw1.netcom.com/~chip.f/viterbi/algrthms2.html
 * This decoder decodes a different Convolutional Code than the one
 * presented in the web page, but the idea is the same.
 *
 * This decoder inputs several two bit symbols and decodes
 * it into 1 bit of data. Note that the decoder needs
 * to process several input symbols before producing output symbols.
 *
 * See the HDTV writeup for a more detailed description
 * and example worked through
 **/
import streamit.library.*;

class UngerboeckDecoder extends Filter
{
    static final int SYMBOLSIZE = 2;
    static final int UNINIT = -1;

    /** number of possible states for the decoder to be in **/
    static final int NUMSTATES = 4;
    /** amount of data we are going to process before producing results **/
    static final int DATASIZE = 5;
    static final int TABLELENGTH = DATASIZE + 1;
    
    /** state transition matrix **/
    int[][] nextStateTable;
    /** reverse state transition matrix -- given a state s1 and input, what state s0 causes s0-input->s1 **/
    int[][] prevStateTable;
    /** state output table -- eg given a state and an input, what is the output **/
    int[][] outputTable;
    /** state input table -- eg given the current state and the next state, what input would cause that transition **/
    int[][] inputTable;

    /** records the predecessor state that lead to each of the possible states 0 - 3 **/
    int[][] predecessorHistoryStates;
    /** accumulated error metric **/
    int[][] accumulatedErrorStates;

    /** The current time (eg the current index of the symbol that we are looking at) **/
    int currentTime;

    public void init ()
    {
	int i;
	int twoToTheSymbolSize;

	input  = new Channel (Integer.TYPE, DATASIZE*SYMBOLSIZE); /* pops an entire chunk of data in one go (2 * 17) [peek] */
	output = new Channel (Integer.TYPE, DATASIZE);     /* pushs all the data (eg 17) */

	// initialize the transition tables -- only happens once
	initializeTables();
	// initialize the decoder state to prepare for decoding this chunk of data
	// (eg zero out all accumulated error state, etc).
	initializeState();
    }


    public void work ()
    {
	int receivedSymbol;
	int i,j;
	int symbol[];
	int decodedData[];
	
	// if this is not the first invocation of the decoder,
	// we have already decoded a chunk/window/frame of data,
	// so we need to copy the final error states
	// for the last time chunk to the table as initialization for
	// this time frame
	if (this.currentTime > 0) {
	    for (i=0; i<NUMSTATES; i++) {
		accumulatedErrorStates[i][0] = accumulatedErrorStates[i][TABLELENGTH-1];
	    }
	    // reset current time
	    this.currentTime = 0;
	}
	
	// loop for all of the data
	for (i=0; i<DATASIZE; i++) {
	    // grab a symbol off the input, lsb comes first on the tape
	    symbol = new int[SYMBOLSIZE];
	    for (j=0; j<SYMBOLSIZE; j++) {
		symbol[SYMBOLSIZE - j - 1] = input.popInt();
	    }
	    
	    // incremement the time value of which we are processing
	    this.currentTime++;
	    
	    // switch based on which time we are processing. The first and second time step
	    // have to be special cased because we know what state the encoder started in (00)
	    // which limits our possible choices.
//  	    if (this.currentTime == 1) {
//  		workTimeOne(symbol);
//  	    } else if (this.currentTime == 2) {
//  		workTimeTwo(symbol);
//  	    } else {
//  		workSteadyState(symbol);
//  	    }

	    workSteadyState(symbol);
	    
	    // debugging -- output the various state tables
	    //printState();
	    
	}

	// we are done processing this set of symbols, and we need to
	// walk back through the table 
	
	decodedData = doTableDecode();
	//System.out.print("Decoded data: ");
	for (i=1; i<TABLELENGTH; i++) {
	    // lsb is in decodedData[0]
	    //System.out.print(decodedData[i] + " ");
	    output.pushInt(decodedData[i]);
	}
	//System.out.println("\n");
    }


    /** the work function in steady state **/
    public void workSteadyState(int[] symbol) {
	// now, no more special cases. All we need to do is for each
	// state that we could be at in the current time, calculate the predecessor on the
	// path that would yield the minimum error (eg calculate the error of following the
	// path up to the successor, and then going to the current node via the path
	// under consideration. Choose the most likely of the two possible paths. If there is a 
	// tie, choose an arbitrary one.

	int[] currentState;
	int   intCurrentState;
	int   predecessorStateZero; // predecessor state if a 0 was the input
	int   predecessorStateOne;  // predecessor state if a 1 was the input

	int errorZero; // error for predecessor 0 to currentState
	int errorOne; // error for predecessor 1 to currentState

	int i,j;

	// set up arrays
	currentState = new int[SYMBOLSIZE];

	
	// iterate through all of the possible current states
	for (i=0; i<SYMBOLSIZE; i++) {
	    for (j=0; j<SYMBOLSIZE; j++) {
		currentState[1] = i;
		currentState[0] = j;
		intCurrentState = convertFromBase2(currentState);
		// lookup the predecessor states for both possible inputs
		predecessorStateZero = prevStateTable[intCurrentState][0];
		predecessorStateOne  = prevStateTable[intCurrentState][1];


	//  	System.out.println("Current state: " + convertFromBase2(currentState) +
//  				   " predecessor (0): " + convertFromBase2(predecessorStateZero) +
//  				   " predecessor (1): " + convertFromBase2(predecessorStateOne));
				   
		
		// now, calculate the cumulative error that occurs by
		// following each predecessor to the current state
		errorZero= calculateCumulativeError(predecessorStateZero,
						    intCurrentState,
						    this.currentTime - 1,
						    symbol);
		errorOne = calculateCumulativeError(predecessorStateOne,
						    intCurrentState,
						    this.currentTime - 1,
						    symbol);

		// now, choose the smaller of the two errors (or transition caused by 0 if a tie)
		if (errorZero <= errorOne) {
		    // use predecessor state zero
		    accumulatedErrorStates[intCurrentState][this.currentTime] = errorZero;
		    predecessorHistoryStates[intCurrentState][this.currentTime] = predecessorStateZero;
		} else {
		    // use predecessor state zero
		    accumulatedErrorStates[intCurrentState][this.currentTime] = errorOne;
		    predecessorHistoryStates[intCurrentState][this.currentTime] = predecessorStateOne;
		}
	    }
	}

	// now, we are done with the current symbol, carry on
	

    }


    /**
     * Traces the state back from the end of the table to reconstruct
     * the input to the trellis encoder. This is the tricky part of the dynamic programming that
     * they conveniently don't deal with in 6.046 and dynamic programming.
     **/
    public int[] doTableDecode() {
	int[] decodedData;
	int lastState;
	int lastStateError;

	int previousState;

	int i;
	
	// the first element of the array we return will have the 
	// the first input that was encoded

	decodedData = new int[TABLELENGTH];

	// trace back through the table to reconstruct the original path
	// by choosing the ending state with the smallest combined error, and working
	// back from there.

	// find min error of the ending states
	lastState = 0;
	lastStateError = accumulatedErrorStates[lastState][TABLELENGTH-1];
	for (i=1; i<NUMSTATES; i++) {
	    // if the current state we are examining has a lower error in the last state,
	    if (accumulatedErrorStates[i][TABLELENGTH-1] <
		lastStateError) {
		// remember this state
		lastState = i;
		lastStateError = accumulatedErrorStates[i][TABLELENGTH-1];
	    }
	}
	
	// now, walk the previous state table backward to recover the output
	for (i=TABLELENGTH-1; i>0; i--) {
	    // look up what the previous state was
	    previousState = predecessorHistoryStates[lastState][i];
	    
	    // figure out what the last input was
	    decodedData[i] = inputTable[previousState][lastState];
	    lastState = previousState;
	    
	}

	return decodedData;
    }

    /**
     * calculate culmulative error given that we started at state s1
     * at time t1 and we are going to state s2 given that
     * we received symbol receivedSymbol at time t1+1
     **/
    public int calculateCumulativeError(int s1, int s2, int t1, int[] receivedSymbol) {
	int s1error;
	int inputValue;
	int branchError;
	int[] expectedSymbol;

	// lookup what input caused this transition
	inputValue = inputTable[s1][s2];
	if (inputValue == UNINIT) {
	    //System.out.println("Error!!! invalid transition from " + s1 + " to " + s2);
	    System.out.println("Error!!! invalid transition");
	//  throw new RuntimeException("Error");
	}
	
	// look up the cumulative error of s1 
	s1error = accumulatedErrorStates[s1][t1];

	// now, calculate the output symbol that this transition causes
	expectedSymbol = new int[SYMBOLSIZE];
	expectedSymbol = convertToBase2(outputTable[s1][inputValue]);

	// now, compute the error that this branch causes
	// (eg the hamming distance between the received symbol and the symbol that
	// this transition would generate)
	branchError = hammingDistance(expectedSymbol, receivedSymbol);

//  	System.out.println("Calculating transition from " + s1 + " to " + s2 +
//  			   " i= " + inputValue +
//  			   " o= " + convertFromBase2(expectedSymbol) +
//  			   " rs= " + convertFromBase2(receivedSymbol) + 
//  			   " be= " + branchError +
//  			   " ce= " + s1error);
	
	// the total error that choosing this transition would incur
	// is the branch error plus the cumulative error for the
	// path up to s1.
	return s1error + branchError;
    }
						    

    
    public void initializeTables() {
	// calculate the state transition matrix (either 0 or 1 as input)
	// this is a simple lookup table for calculating the next state
	// given the current state and the input
	// hand code the transitions (for now)
	// nextStateTables[current state][input] has the next state
	nextStateTable = new int[NUMSTATES][2];
	nextStateTable[0][0] = 0;
	nextStateTable[0][1] = 1;
	nextStateTable[1][0] = 2;
	nextStateTable[1][1] = 3;
	nextStateTable[2][0] = 1;
	nextStateTable[2][1] = 0;
	nextStateTable[3][0] = 3;
	nextStateTable[3][1] = 2;

	// make the table for output of the encoder given
	// the current state and the input
	// outputTable[current state][input] hold output
	outputTable = new int[NUMSTATES][2];
	outputTable[0][0] = 0;
	outputTable[0][1] = 2;
	outputTable[1][0] = 1;
	outputTable[1][1] = 3;
	outputTable[2][0] = 0;
	outputTable[2][1] = 2;
	outputTable[3][0] = 1;
	outputTable[3][1] = 3;

	// make the table for the previous state -- this table
	// is the inverse of the next state table. It tells you which
	// state the encoder would have been in to transition to
	// the current state given a particular input.
	// prevStateTable[currentState][input] = previous state
	prevStateTable = new int[NUMSTATES][2];
	prevStateTable[0][0] = 0;
	prevStateTable[0][1] = 2;
	prevStateTable[1][0] = 2;
	prevStateTable[1][1] = 0;
	prevStateTable[2][0] = 1;
	prevStateTable[2][1] = 3;
	prevStateTable[3][0] = 3;
	prevStateTable[3][1] = 1;

	
	// make the table for the input that caused transitions
	// this is the reverse state transition table
	inputTable = new int[NUMSTATES][NUMSTATES];

	// handcode the input (for now)
	// input is inputTable[currentState][nextState] = input caused
	// UNINIT means that that transition is impossible
	inputTable[0][0] = 0;
	inputTable[0][1] = 1;
	inputTable[0][2] = UNINIT;
	inputTable[0][3] = UNINIT;

	inputTable[1][0] = UNINIT;
	inputTable[1][1] = UNINIT;
	inputTable[1][2] = 0;
	inputTable[1][3] = 1;
	
	inputTable[2][0] = 1;
	inputTable[2][1] = 0;
	inputTable[2][2] = UNINIT;
	inputTable[2][3] = UNINIT;

	inputTable[3][0] = UNINIT;
	inputTable[3][1] = UNINIT;
	inputTable[3][2] = 1;
	inputTable[3][3] = 0;
    }
	    
    public void initializeState() {
	int i;
	int j;

	// predecessorHistoryStates has an entry for the predecessor state
	// that lead to the current state
	// predecessorHistoryState[state][t]=previous state
	predecessorHistoryStates = new int[NUMSTATES][TABLELENGTH];

	// initially all previous states are 0
	for (i=0; i<TABLELENGTH; i++) {
	    for (j=0; j<NUMSTATES; j++) {
	    predecessorHistoryStates[j][i] = 0;
	    }
	}

	// now, initialize the accumulatedErrorStates to all 0
	// accumulatedErrorStates[state][time] = accumulated error
	accumulatedErrorStates = new int[NUMSTATES][TABLELENGTH];
	for(i=0; i<TABLELENGTH; i++) {
	    for (j=0; j<NUMSTATES; j++) {
		accumulatedErrorStates[j][i] = 0;
	    }
	}

	// set the accumulated error states to be a large constant
	// for states that are  not possible because we know that the starting state
	// was 00
	int LARGE = 20000;
	accumulatedErrorStates[0][0] = 0;
	accumulatedErrorStates[1][0] = LARGE;
	accumulatedErrorStates[2][0] = LARGE;
	accumulatedErrorStates[3][0] = LARGE;
	
	
	// set the current time to be 0
	this.currentTime = 0;
    }

//      /** prints out the state of the decoder **/    
//      public void printState() {
//  	printTable("Predecessor States", predecessorHistoryStates);
//  	printTable("Accumulated Errors", accumulatedErrorStates);
//      }

//      /** prints out a table (no way that this is going to work in streamit propper) **/
//      public void printTable(String title, int[][] table) {
//  	int i,j;
//  	System.out.println(title);
//  	for (i=0; i<table.length; i++) {
//  	    for (j=0; j<table[i].length; j++) {
//  		System.out.print(table[i][j] + " ");
//  	    }
//  	    System.out.println();
//  	}

//      }
						  
    
    /**
     * convert a base 2 symbol in an integer array (each element is a 0 or a 1) 
     * into a base 10 number in an integer. The lsb of the binary number is in
     * symbol[0].
     **/
    public int convertFromBase2(int[] symbol) {
	int i;
	int value;

	// start off with a value of 0 in our container
	value = 0;

	// shift in the bits from the symbol
	for (i=0; i<SYMBOLSIZE; i++) {
	    value = value << 1;
	    value = value | symbol[SYMBOLSIZE - i - 1];
	}

	return value;
    }

    /** converts an integer _to_ a base 2 symbol. return[0] is lsb. **/
    public int[] convertToBase2(int value) {
	int i;
	int temp;
	int[] returnValue;
	returnValue = new int[SYMBOLSIZE];

	temp = value;
	// wack off 1 bit at a time off the end of temp into value
	for (i=0; i<SYMBOLSIZE;i++) {
	    returnValue[i] = temp & 0x1; // grab lowest bit
	    temp = temp >> 1; // right shift
	}

	return returnValue;
    }
    
    /** Compute hamming distance between two symbols **/
    public int hammingDistance(int[] a, int[] b) {
	int distance;
	int i;
	distance = 0;
	// increment distance counter once for each bit that differs
	for (i=0; i<SYMBOLSIZE; i++) {
	    if (a[i] != b[i]) {
		distance++;
	    }
	}
	return distance;
    }

    /** ensures that an integer is actually "binary", eg a 1 or 0 **/
    public void dataCheck(int d) {
//  	if ((d != 0) && (d != 1)) {
//  	    throw new RuntimeException("non binary digit: " + d);
	//	}
    }
}


/*
 * Simple Trellis decoder. Uses the ideas and algorithms
 * from http://pw1.netcom.com/~chip.f/viterbi/algrthms2.html
 * basically this decoder takes a two bit symbol and decodes
 * it into 1 bit data. Note that the decoder keeps a bunch
 * of state to accomplish this task.
 *
 * The main idea of the vertibi decoding algorithm is to
 * fill out two tables (a la dynamic programming) as we trace out all possible
 * paths in the trellis. Since a path in the trellis corresponds exactly to one input
 * sequence, by recovering the path, we will be able to recover the input.
 * 
 * each table is two dimensional: states and time. Each unit of time corresponds to
 * receiving 1 symbol (so in this case receiving 2 bits). Each cell represents something
 * for a particular state at a particular time. The algorithm fills up both tables as
 * time progresses. When the tables are full, it traces back through the table and finds the
 * path that results in the fewest errors occuring.
 *
 * The error metric that is used is hamming distance.
 *
 * The first table keeps track of accumulated errors. accumulated_error[s,t] is the hamming distance
 * between the output that the current min path to state s at time t would generate and the
 * symbols that have been received. 
 *
 * The second table keeps track of the previous states, so that the min path
 * can be reconstructed. predecessor_states[s,t] represents the state s' of the encoder
 * in the minimum path that leads to s at time (t-1) 
 */

import streamit.library.*;

class TrellisDecoder extends Filter
{
    static int SYMBOLSIZE = 2;
    static int UNINIT = -1;

    /** number of possible states for the decoder to be in **/
    static int NUMSTATES = 4;
    /** amount of data we are going to process before producing results **/
    static int TABLELENGTH = 0;
    static int DATASIZE = 0;
    
    /** state transition matrix **/
    int[][] nextStateTable;
    /** state output table -- eg given a state and an input, what is the output **/
    int[][] outputTable;
    /** state input table -- eg given the current state and the next state, what input would cause that transition **/
    int[][] inputTable;

    /** records the predecessor state that lead to each of the possible states 0 - 3 **/
    int[][] predecessorHistoryStates;
    /** accumulated error metric **/
    int[][] accumulatedErrorStates;

    /** The current time (eg the current symbol that we are looking at) **/
    int currentTime;

    public TrellisDecoder(int dataSize) {
	DATASIZE = dataSize;
	TABLELENGTH = dataSize + 1;
    }
    
    public void init ()
    {
	int i;
	int twoToTheSymbolSize;

	input  = new Channel (Integer.TYPE, DATASIZE*SYMBOLSIZE); /* pops an entire chunk of data in one go (2 * 17) [peek] */
	output = new Channel (Integer.TYPE, DATASIZE);     /* pushs all the data (eg 17) */

	// initialize the transition tables -- only happens once
	initializeTables();

    }


    public void work ()
    {
	int receivedSymbol;
	int i,j;
	int symbol[];
	int decodedData[];

	// initialize the decoder state to prepare for decoding this chunk of data
	initializeState();

	// loop for all of the data
	for (i=0; i<DATASIZE; i++) {
	    // grab a symbol from the input, lsb comes first on the tape
	    symbol = new int[SYMBOLSIZE];
	    for (j=0; j<SYMBOLSIZE; j++) {
		symbol[SYMBOLSIZE - j - 1] = input.popInt();
	    }
	    
	    // incrememnt the time value
	    this.currentTime++;
	    
	    // switch based on which time we are processing. The first and second time step
	    // have to be special cased because we know what state the encoder started in (00)
	    // which limits our possible choices.
	    if (this.currentTime == 1) {
		workTimeOne(symbol);
	    } else if (this.currentTime == 2) {
		workTimeTwo(symbol);
	    } else {
		workSteadyState(symbol);
	    }
	    
	    // debugging -- output the various state tables
	    // printState();
	    
	}
		
	// if we have hit the end of the block of data that we are processing, do a decode
	// and print the results to the screen
	if (this.currentTime == TABLELENGTH-1) {
	    decodedData = doTableDecode();
	    //System.out.print("Decoded data: ");
	    for (i=1; i<TABLELENGTH; i++) {
		// lsb is in decodedData[0]
		//System.out.print(decodedData[i] + " ");
		output.pushInt(decodedData[i]);
	    }
	    //System.out.println("\n");
	}
	
    }


    /** the work function for time 1 **/
    public void workTimeOne(int[] symbol) {
	int time;

	time = 1;

	// since we started in state 00, there are only two possible states that
	// we could go to at time 1:
	// 00 -0-> 00, output 00
	// 00 -1-> 10, output 11

	// compare both of these outputs with the symbol that we get

	// 00-1->00, output = 00
	accumulatedErrorStates[0][time] = hammingDistance(symbol,
							  convertToBase2(outputTable[0][0])); // the distance starting from 00 to 00 with an input of 0
	predecessorHistoryStates[0][time] = 0; // this says that the prevous state was 0

	// 00-1->10, output = 11
	accumulatedErrorStates[2][time] = hammingDistance(symbol,
							  convertToBase2(outputTable[0][1])); // the distance starting from 00 to 10 with an input of 1
	predecessorHistoryStates[2][time] = 0; // this says that the prevous state was 0

    }
    /** the work function for time 2 **/
    public void workTimeTwo(int[] symbol) {
	int zzTzz, zzToz, ozTzo, ozToo;
	    

	int time;
	time = 2;

	// time 2 is a little more complicated because we now can reach all possible states
	// because we could be in either state 00 or state 10
	// 00 -0-> 00, output 00
	// 00 -1-> 10, output 11
	// 10 -0-> 01, output 10
	// 10 -1-> 11, output 01

	// 00 -0-> 00, output 00
	zzTzz = calculateCumulativeError(0,0,time-1,symbol);
	// 00 -1-> 10, output 11
	zzToz = calculateCumulativeError(0,2,time-1,symbol);

	// 10 -0-> 01, output 10
	ozTzo = calculateCumulativeError(2,1,time-1,symbol);
	// 10 -1-> 11, output 01
	ozToo = calculateCumulativeError(2,3,time-1,symbol);

	// now, note that we have computed the cumulative errors for transitioning to all possible states
	// (eg there is some path from 00 to all states by the second time step)
	// therefore, all that remains to do is to update the error table and the predecessor table
	// 00 -0-> 00, output 00
	accumulatedErrorStates[0][time] = zzTzz;
	predecessorHistoryStates[0][time] = 0;
	// 00 -1-> 10, output 11
	accumulatedErrorStates[2][time] = zzToz;
	predecessorHistoryStates[2][time] = 0;
	// 10 -0-> 01, output 10
	accumulatedErrorStates[1][time] = ozTzo;
	predecessorHistoryStates[1][time] = 2;
	// 10 -1-> 11, output 01
	accumulatedErrorStates[3][time] = ozToo;
	predecessorHistoryStates[3][time] = 2;

	// and we are done
	
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
	int[] predecessorStateZero; // predecessor state if a 0 was shifted off
	int[] predecessorStateOne;  // predecessor state if a 1 was shifted off

	int errorZero; // error for predecessor 0 to currentState
	int errorOne; // error for predecessor 1 to currentState

	int i,j;

	// set up arrays
	currentState = new int[SYMBOLSIZE];
	predecessorStateZero = new int[SYMBOLSIZE];
	predecessorStateOne = new int[SYMBOLSIZE];

	
	// iterate through all of the possible current states
	for (i=0; i<SYMBOLSIZE; i++) {
	    for (j=0; j<SYMBOLSIZE; j++) {
		currentState[1] = i;
		currentState[0] = j;
		intCurrentState = convertFromBase2(currentState);
		// because we know how states are formed, figure out the two possile previous states
		predecessorStateZero[1] = currentState[0];
		predecessorStateZero[0] = 0;
		predecessorStateOne[1] = currentState[0];
		predecessorStateOne[0] = 1;

	//  	System.out.println("Current state: " + convertFromBase2(currentState) +
//  				   " predecessor (0): " + convertFromBase2(predecessorStateZero) +
//  				   " predecessor (1): " + convertFromBase2(predecessorStateOne));
				   
		
		// now, calculate the cumulative error that occurs by
		// following each predecessor to the current node at this time
		errorZero= calculateCumulativeError(convertFromBase2(predecessorStateZero),
						    intCurrentState,
						    this.currentTime - 1,
						    symbol);
		errorOne = calculateCumulativeError(convertFromBase2(predecessorStateOne),
						    intCurrentState,
						    this.currentTime - 1,
						    symbol);

		// now, choose the smaller of the two errors (or error 0 if a tie)
		if (errorZero <= errorOne) {
		    // use predecessor state zero
		    accumulatedErrorStates[intCurrentState][this.currentTime] = errorZero;
		    predecessorHistoryStates[intCurrentState][this.currentTime] = convertFromBase2(predecessorStateZero);
		} else {
		    // use predecessor state zero
		    accumulatedErrorStates[intCurrentState][this.currentTime] = errorOne;
		    predecessorHistoryStates[intCurrentState][this.currentTime] = convertFromBase2(predecessorStateOne);
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
	    System.out.println("Error!!! invalid transition from " + s1 + " to " + s2);
	    throw new RuntimeException("Error");
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
	// calculate the state transition matrix
	nextStateTable = new int[NUMSTATES][2];

	// hand code the transitions (for now)
	// transitions are nextStateTables[current state][input] has value of new state
	nextStateTable[0][0] = 0;
	nextStateTable[0][1] = 2;
	nextStateTable[1][0] = 0;
	nextStateTable[1][1] = 2;
	nextStateTable[2][0] = 1;
	nextStateTable[2][1] = 3;
	nextStateTable[3][0] = 1;
	nextStateTable[3][1] = 3;

	// make the table for output state
	outputTable = new int[NUMSTATES][2];

	// handcode the output (for now)
	outputTable[0][0] = 0;
	outputTable[0][1] = 3;
	outputTable[1][0] = 3;
	outputTable[1][1] = 0;
	outputTable[2][0] = 2;
	outputTable[2][1] = 1;
	outputTable[3][0] = 1;
	outputTable[3][1] = 2;

	// make the table for the input that caused transitions
	inputTable = new int[NUMSTATES][NUMSTATES];

	// handcode the input (for now)
	// input is inputTable[currentState][nextState] = input caused
	// UNINIT means that that transition is impossible
	inputTable[0][0] = 0;
	inputTable[0][1] = UNINIT;
	inputTable[0][2] = 1;
	inputTable[0][3] = UNINIT;
	inputTable[1][0] = 0;
	inputTable[1][1] = UNINIT;
	inputTable[1][2] = 1;
	inputTable[1][3] = UNINIT;
	
	inputTable[2][0] = UNINIT;
	inputTable[2][1] = 0;
	inputTable[2][2] = UNINIT;
	inputTable[2][3] = 1;
	inputTable[3][0] = UNINIT;
	inputTable[3][1] = 0;
	inputTable[3][2] = UNINIT;
	inputTable[3][3] = 1;
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
	
	// set the current time to be 0
	this.currentTime = 0;
    }

    /** prints out the state of the decoder **/    
    public void printState() {
	printTable("Predecessor States", predecessorHistoryStates);
	printTable("Accumulated Errors", accumulatedErrorStates);
    }

    /** prints out a table (no way that this is going to work in streamit propper) **/
    public void printTable(String title, int[][] table) {
	int i,j;
	System.out.println(title);
	for (i=0; i<table.length; i++) {
	    for (j=0; j<table[i].length; j++) {
		System.out.print(table[i][j] + " ");
	    }
	    System.out.println();
	}

    }
						  
    
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
	if ((d != 0) && (d != 1)) {
	    throw new RuntimeException("non binary digit: " + d);
	}
    }
}

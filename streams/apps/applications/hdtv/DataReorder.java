/**
 * The Data reorder takes the data bound for the symbol mapper
 * (eg so it is in bits) and reorders it based on what data segment type
 * we are currently processing. There are three data segment types:0,1,2.
 *
 * Data segment 1 is type 0, segment 2 is type 1, segment 3 is type 2
 * segment 4 is type 0, etc.
 *
 * The interleaver works based based on 12 bytes worth of data (unfortunately
 * they are bits at this point in the pipeline)
 *
 * Basically, the 12 bytes come in like this:
 * [0 1 2 3 4 5 6 7 8 9 10 11]
 *
 * with type 0 they leave like this:
 * [0 1 2 3 4 5 6 7 8 9 10 11]
 * with type 1 they leave like this:
 * [4 5 6 7 8 9 10 11 0 1 2 3]
 * with type 1 they leave like this:
 * [8 9 10 11 0 1 2 3 4 5 6 7]
 *
 *
 * To un reorder the data, the segment stuff works slightly differetly
 * Basically, the 12 bytes come in like this:
 * [0 1 2 3 4 5 6 7 8 9 10 11]
 *
 * with type 0 they leave like this:
 * [0 1 2 3 4 5 6 7 8 9 10 11]
 * with type 1 they leave like this:
 * [8 9 10 11 0 1 2 3 4 5 6 7]
 * with type 1 they leave like this:
 * [4 5 6 7 8 9 10 11 0 1 2 3 ]
 **/

import streamit.library.*;

class DataReorder extends Filter {
    static final int BYTES = 12;
    static final int BITS = BYTES * 8;
    static final int BYTE_SEGMENT_SIZE = 828;

    /**
     * if direction is 1, reorders data
     * if direction is 2, unreorders data.
     **/
    int direction;
    
    int type;
    int segmentCount;

    public DataReorder(int dir) {
	super(dir);
    }
    
    /**
     * pass 1 as direction for reordering, pass 2
     * as direction to undo reordering.
     **/
    public void init(int dir) {
	this.direction = dir;
	this.type = 0;
	this.segmentCount = 0;
	input  = new Channel(Integer.TYPE, BITS);
	output = new Channel(Integer.TYPE, BITS);
    }

    public void work() {
	// read in all the bits into a temp buffer
	int[] buff = new int[BITS];
	for (int i=0; i<BITS; i++) {
	    buff[i] = input.popInt();
	}
	// now, depending on the current type of the segment
	// copy the data back out in a particular ways
	int currentIndex = 0;
	if (this.type == 0) {
	    currentIndex = 0;
	} else if (this.type == 1) {
	    if (this.direction == 1) {
		currentIndex = 4;
	    } else {
		currentIndex = 8;
	    }
	} else if (this.type == 2) {
	    if (this.direction == 1) {
		currentIndex = 8;
	    } else {
		currentIndex = 4;
	    }
	} else {
	    System.out.println("Error in Data Reorderer...");
	}
	
	// now, copy out the bits in the correct order
	currentIndex *= 8;
	for (int i=0; i<BITS; i++) {
	    output.pushInt(buff[currentIndex]);
	    currentIndex = (currentIndex + 1) % BITS;
	}

	// increment the number of 12 byte sections we have seen
	this.segmentCount++;

	// if we have seen an entire segment, increment the type
	// of segment we are in, and reset count
	int totalBytesSeen = segmentCount*12;
	if (totalBytesSeen == BYTE_SEGMENT_SIZE) {
	    this.type = (this.type + 1) % 3; // wrap around types
	    this.segmentCount = 0;
	}
    }



}

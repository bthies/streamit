/**
 * Symbol Encoder -- maps sequences of 3 bits to
 * a symbol that is to be transmitted over the
 * the airwaves. Therefore it takes in 3 "bits"
 * and produces one "symbol" as outputChannel. LSB is brought in first.
 **/
import streamit.library.*;

class SymbolMapper extends Filter {
    int[] map;
    public void init() {
	inputChannel= new Channel(Integer.TYPE, 3); // input three bits
	outputChannel= new Channel(Integer.TYPE, 1); // output one symbol
	setupMap();
    }
    public void work() {
	// shift in the bits (msb is first)
	int index = 0;
	for (int i=0; i<3; i++) {
	    index = index << 1;
	    index = index | inputChannel.popInt();
	}
	// do a symbol lookup on the index
	outputChannel.pushInt(this.map[index]);
	
    }

    void setupMap() {
	this.map = new int[8];
	map[0] = -7;
	map[1] = -5;
	map[2] = -3;
	map[3] = -1;
	map[4] = 1;
	map[5] = 3;
	map[6] = 5;
	map[7] = 7;
    }
}


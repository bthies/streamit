/**
 * Symbol Decoder -- maps a symbol to a sequence of 3 bits.
 **/
import streamit.library.*;

class SymbolUnMapper extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 1); // input 1 symbol
	output = new Channel(Integer.TYPE, 3); // output 3 bits
    }
    public void work() {
	int sym = input.popInt();
	int index = (sym+7)/2; // easy formula to recover the data from symbol

	//now, shift out the bits, msb first
	for (int i=0; i<3; i++) {
	    output.pushInt((index & 0x04) >> 2);
	    index = index << 1;
	}
    }

}


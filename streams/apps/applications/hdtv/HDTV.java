/**
 * Example HDTV decoder application. Eventually this
 * application might be able to take a trace from the
 * airwaves and output an MPEG2 video stream.
 **/
import streamit.*;

public class HDTV extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new HDTV().run(args); 
	
    }

    public void init() {
	// add a data source
	this.add(new DataSource());
	
	// add an encoder
	this.add(new HDTVEncodePipeline());

	// add a decoder
	this.add(new HDTVDecodePipeline());

	// print what comes out of the decoder
	this.add(new DataSink());

    }

}

class HDTVEncodePipeline extends Pipeline {
    public void init() {
	// split integers into "bytes"
	this.add(new IntegerSplitter());

	// encode using reed-solomon encoder
	this.add(new ReedSolomonEncoder());

	// split "bytes" into "bits"
	this.add(new Bitifier());

	// convolutionally interleave
	// NOTE:not sure that this is possible with streamIT

	// interleave and trellis encode
	this.add(new TrellisEncoderPipeline());
	
	// create our symbols to transmit over the noisy channel
	this.add(new SymbolMapper());
    }
}


class HDTVDecodePipeline extends Pipeline {
    public void init() {
	

	// decode symbols received over the channel
	this.add(new SymbolUnMapper());

	// trellis decode and de-interleave
	this.add(new TrellisDecoderPipeline());
	
	// convolutionally deinterleave and decode
	// NOTE: Not sure that this is possible with streamIT

	// recombine "bits" into "bytes"
	this.add(new UnBitifier());

	// decode (and correct errors with reed solomon
	//this.add(new ReedSolomonDecoder());
	
	// recombine "bytes" back into integers
	this.add(new IntegerCombiner());
	
    }    
}


/** Trellis Interleave/Encoder pipeline **/
class TrellisEncoderPipeline extends SplitJoin {
    public void init() {
	int NUM = 12; // number of ways to interleave
	this.setSplitter(ROUND_ROBIN());
	for (int i=0; i<NUM; i++) {
	    this.add(new TrellisEncoder());
	}
	this.setJoiner(ROUND_ROBIN());
    }
}

/** Trellis Decoder/Deinterleaver pipeline **/
class TrellisDecoderPipeline extends SplitJoin {
    public void init() {
	int NUM = 12; // number of ways to deinterleave
	this.setSplitter(ROUND_ROBIN());
	for (int i=0; i<NUM; i++) {
	    this.add(new TrellisDecoder());
	}
	this.setJoiner(ROUND_ROBIN());
    }
}


/**
 * Data source for sample application
 **/
class DataSource extends Filter {
    int x=0;
    public void init() {
	output = new Channel(Integer.TYPE, 1); // pushes 1 item per cycle
    }
    public void work() {
	output.pushInt(this.x);
	this.x++;
    }
}

/**
 * Data sink for sample app (prints out data).
 **/
class DataSink extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE,1); // pops 1 item per cycle
    }
    public void work() {
	System.out.println(input.popInt());
    }
}

/**
 * Prints data on the way by.
 **/
class DataSniffer extends Filter {
    String name;
    public void init() {
	input  = new Channel(Integer.TYPE,1);
	output = new Channel(Integer.TYPE,1);
    }
    public void work() {
	int in = input.popInt();
	System.out.print(this.name);
	System.out.println(in);
	output.pushInt(in);
    }
}	




/**
 * Splits in incoming integer into 4 outgoing bytes.
 * The first byte pushed is the least significant.
 **/
class IntegerSplitter extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 1); // pops 1 integer
	output = new Channel(Integer.TYPE, 4); // pushes out the 4 "bytes"
    }

    public void work() {
	int t = input.popInt(); // pop off the integer;
	output.pushInt((t & 0x000000FF) >> 0);
	output.pushInt((t & 0x0000FF00) >> 8);
	output.pushInt((t & 0x00FF0000) >> 16);
	output.pushInt((t & 0xFF000000) >> 24);
    }
    

}

/**
 * Reassembles integers from bytes that have
 * been split with an IntegerSplitter.
 **/
class IntegerCombiner extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 4); // pops the 4 "bytes"
	output = new Channel(Integer.TYPE, 1); // pushes 1 integer
    }

    public void work() {
	int byte1 = input.popInt();
	int byte2 = input.popInt() << 8;
	int byte3 = input.popInt() << 16;
	int byte4 = input.popInt() << 24;

	// push the created output on to the tape
	output.pushInt(byte1 | byte2 | byte3 | byte4);
    }
    
}


/**
 * Bitifier -- converts a byte (masquarading as an integer)
 * into a stream of bits (also masquarading as integers).
 * In digital systems, this is known as a shift register
 * (MSB is shifted out first).
 **/
class Bitifier extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 1); // pops 1 "bytes"
	output = new Channel(Integer.TYPE, 8); // pushes 8 "bits"
    }

    public void work() {
	int left = input.popInt(); // pull off the byte
	for (int i=0; i<8; i++) {
	    // shift out the bits one by one (msb first)
	    output.pushInt((left & 0x80) >> 7);
	    // set up next shift
	    left = left << 1;
	}
    }    
}


/**
 * UnBitifier -- converts a stream of bits (masquarading as integers)
 * into byte (also masquarading as an integer).
 * In digital systems, this is also a shift register
 * (MSB is shifted in first).
 **/
class UnBitifier extends Filter {
    public void init() {
	input  = new Channel(Integer.TYPE, 8); // pops 1 "bits"
	output = new Channel(Integer.TYPE, 1); // pushes 1 "byte"
    }

    public void work() {
	int accum = 0;
	for (int i=0; i<8; i++) {
	    // shift in 8 bits
	    accum = accum << 1;
	    accum = accum | input.popInt();
	}
	output.pushInt(accum);
    }    
}












/**
 * Symbol Encoder -- maps sequences of 3 bits to
 * a symbol that is to be transmitted over the
 * the airwaves. Therefore it takes in 3 "bits"
 * and produces one "symbol" as output. LSB is brought in first.
 **/
class SymbolMapper extends Filter {
    int[] map;
    public void init() {
	input  = new Channel(Integer.TYPE, 3); // input three bits
	output = new Channel(Integer.TYPE, 1); // output one symbol
	setupMap();
    }
    public void work() {
	// shift in the bits (msb is first)
	int index = 0;
	for (int i=0; i<3; i++) {
	    index = index << 1;
	    index = index | input.popInt();
	}
	// do a symbol lookup on the index
	output.pushInt(this.map[index]);
	
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


/**
 * Symbol Decoder -- maps a symbol to a sequence of 3 bits.
 **/
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






class TrellisEncoder extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE,1);
	output = new Channel(Integer.TYPE,1);
    }
    public void work() {
	output.pushInt(input.popInt());
    }
}

class TrellisDecoder extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE,1);
	output = new Channel(Integer.TYPE,1);
    }
    public void work() {
	output.pushInt(input.popInt());
    }
}


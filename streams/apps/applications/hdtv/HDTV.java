/**
 * Example HDTV decoder application. Eventually this
 * application might be able to take a trace from the
 * airwaves and output an MPEG2 video stream.
 **/
import streamit.library.*;

public class HDTV extends StreamIt 
{
    //static final int NUM_TRELLIS_ENCODERS = 12;
    static final int NUM_TRELLIS_ENCODERS = 12;
    //static final int INTERLEAVE_DEPTH = 52;
    static final int INTERLEAVE_DEPTH = 5;

    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new HDTV().run(args); 
	
    }

    public void init() {
	// add a data source
	this.add(new DataSegmentGenerator());
	
	// add an encoder
	this.add(new HDTVEncodePipeline());

	// add a decoder
	this.add(new HDTVDecodePipeline());

	// print what comes out of the decoder
	this.add(new DataSegmentSink());

    }

}

class HDTVEncodePipeline extends Pipeline {
    public void init() {
	// -- data is integers in integers--

	// split integers into "bytes"
	this.add(new IntegerSplitter());

	// -- data is bytes in integers--
	
	// encode using reed-solomon encoder
	this.add(new ReedSolomonEncoder());

	// -- data is bytes in integers--
	
	// convolutionally interleave
	this.add(new ConvolutionalInterleaver(HDTV.INTERLEAVE_DEPTH));
	
	// -- data is bytes in integers--
	
	// interleave and trellis encode
	this.add(new TrellisEncoderPipeline(HDTV.NUM_TRELLIS_ENCODERS));

	// -- data is bits in integers--

	// reorder the bits
	this.add(new DataReorder(1)); // 1 means reorder

	// -- data is bits in integers--

	// insert some noise
	//this.add(new NoiseSource(1000));
	
	// create our symbols to transmit over the noisy channel
	this.add(new SymbolMapper());

	// -- data is 8 level symbols in integers--
	// add the four symbol sync to the symbols
	this.add(new SyncGenerator());

	// -- data is 8 level symbols in integers--	
    }
}


class HDTVDecodePipeline extends Pipeline {
    public void init() {
	// -- data is 8 level symbols in integers--

	// remove the sync field from the symbols that come
	// in over the channel
	this.add(new SyncRemover());
	
	// -- data is 8 level symbols in integers--	

	// decode symbols received over the channel
	this.add(new SymbolUnMapper());

	// -- data is bits in integers--

	// un-reorder the bits
	this.add(new DataReorder(2)); // 1 means un-reorder

	// -- data is bits in integers--

	// trellis decode and de-interleave
	this.add(new TrellisDecoderPipeline(HDTV.NUM_TRELLIS_ENCODERS));

	// -- data is bytes in integers--
	
	// convolutionally deinterleave and decode
	this.add(new ConvolutionalDeinterleaver(HDTV.INTERLEAVE_DEPTH));

	// -- data is bytes in integers--

	// decode (and correct errors with reed solomon
	this.add(new ReedSolomonDecoder());

	// -- data is bytes in integers--
	
	// recombine "bytes" back into integers
	this.add(new IntegerCombiner());

	// -- data is integers in integers--
	
    }    
}


/**
 * Data source for sample application
 **/
class DataSegmentGenerator extends Filter {
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
class DataSegmentSink extends Filter {
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
 * Adds a Sync field (I do not know what this contains at the present)
 * on the front of a data segment.
 *
 * 828 8-level-symbols are transmitted per data segment, and attached
 * to the front of those 828 symbols are 4 binary sync levels, for a
 * grand total of 832 symbols sent over the channel per data segment.
 **/
class SyncGenerator extends Filter {
    final int DATA_SEGMENT_SIZE = 828;
    final int SYNC_SIZE         = 4;
    public void init() {
	input  = new Channel(Integer.TYPE, DATA_SEGMENT_SIZE);
	output = new Channel(Integer.TYPE, DATA_SEGMENT_SIZE + SYNC_SIZE);
    }
    public void work() {
	// push out the sync value on to the output tape
      	// the sync, according to the a53, rev b spec
	// is binary 1001, encoded as symbols
	// 5 -5 -5 5
	output.pushInt(5);
	output.pushInt(-5);
	output.pushInt(-5);
	output.pushInt(5);

	// copy the remaining 828 symbols from the input to the output
	for (int i=0; i<DATA_SEGMENT_SIZE; i++) {
	    output.pushInt(input.popInt());
	}
    }

}


/**
 * Removes a Sync field (I do not know what this contains at the present)
 * the front of a data segment. Totally ignores the sync data
 **/
class SyncRemover extends Filter {
    final int DATA_SEGMENT_SIZE = 828;
    final int SYNC_SIZE         = 4;
    public void init() {
	input  = new Channel(Integer.TYPE, DATA_SEGMENT_SIZE + SYNC_SIZE);
	output = new Channel(Integer.TYPE, DATA_SEGMENT_SIZE);
    }
    public void work() {
	int i;
	// pop off the sync
	for (i=0; i<SYNC_SIZE;i++) {
	    input.popInt();
	}
	// copy the rest of the data over
	for (i=0; i<DATA_SEGMENT_SIZE; i++) {
	    output.pushInt(input.popInt());
	}
    }
}

















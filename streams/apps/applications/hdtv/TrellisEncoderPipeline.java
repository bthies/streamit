/** Trellis Interleave/Encoder pipeline **/

import streamit.library.*;
class TrellisEncoderPipeline extends SplitJoin {
    // this is a consequence of encoding an 8 bit byte
    // with an 2/3 convolutional code, resulting in
    // 8 * 3/2 = 12 bits being produced
    static final int NUM_ENCODED_BITS_IN_BYTE = 12;

    /** @param numTrellis is the number of trellis encoders to use **/
    public TrellisEncoderPipeline(int numTrellis) {
	super(numTrellis);
    }

    
    public void init(int numTrellis) {
	this.setSplitter(ROUND_ROBIN());
	for (int i=0; i<numTrellis; i++) {
	    this.add(new TrellisEncoderPipelineElement());
	}
	this.setJoiner(ROUND_ROBIN(NUM_ENCODED_BITS_IN_BYTE));
    }
}


class TrellisEncoderPipelineElement extends Pipeline {
    public void init() {
	// split the "bytes" that are coming into the trellis encoder
	// into "bits" that will be interleaved
	this.add(new Bitifier());
	// perform the actual trellis encoding
	this.add(new TrellisEncoder());
    }
}


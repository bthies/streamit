/** Trellis Decoder/Deinterleaver pipeline **/
import streamit.library.*;
class TrellisDecoderPipeline extends SplitJoin {
    // this is a consequence of encoding an 8 bit byte
    // with an 2/3 convolutional code, resulting in
    // 8 * 3/2 = 12 bits being produced
    static final int NUM_ENCODED_BITS_IN_BYTE = 12;

    /** @param numTrellis is the number of trellis decoders to use **/
    public TrellisDecoderPipeline(int numTrellis) {
	super(numTrellis);
    }
    public void init(int numTrellis) {

	// we need to send 12 bits to each convolutional decoder to produce one
	// byte of data
	this.setSplitter(ROUND_ROBIN(NUM_ENCODED_BITS_IN_BYTE));
	for (int i=0; i<numTrellis; i++) {
	    this.add(new TrellisDecoderPipelineElement());
	}
	this.setJoiner(ROUND_ROBIN());
    }
}


class TrellisDecoderPipelineElement extends Pipeline {
    public void init() {
	// decode the data that is coming in to the decoder
	this.add(new TrellisDecoder());
	// merge the bits back into bytes
	this.add(new UnBitifier());
    }
}



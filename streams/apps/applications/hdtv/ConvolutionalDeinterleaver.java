/**
 * Convolutionally Interleaves data.
 **/
import streamit.library.*;

public class ConvolutionalDeinterleaver extends SplitJoin {
    /** Creates a convolutional encoder with width size. **/
    public ConvolutionalDeinterleaver(int size) {
	super(size);
    }
    public void init(int size) {
	// split each integer off into its own seperate delay pipeline
	this.setSplitter(ROUND_ROBIN());
	for(int i=0; i<size; i++) {
	    this.add(new DelayPipeline(size - i));
	}
	// recombine the data as it comes out
	this.setJoiner(ROUND_ROBIN());
	
    }
}
    

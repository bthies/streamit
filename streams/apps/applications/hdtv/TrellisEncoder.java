/*
 * Simple 2/3 trellis encoder. Every other bit is
 * alternated between a "precoder" and a 
 * 1/2 trellis encoder.
 *
 * Both filters operate on "bits" which we are currently
 * simulating by sending ints over the tape with values 1 or 0.
 *
 * These filters come directly out of the ATSC standard for HDTV:
 * Standard A53 revision b, which can be found at:
 * http://www.atsc.org/standards/a_53b.pdf
 */
import streamit.library.*;

class TrellisEncoder extends SplitJoin {
    public void init() {
	// set up the pipeline (see ATSC standard A53 revision b)
	// switch between precoder and trellis encoder
	this.setSplitter(WEIGHTED_ROUND_ROBIN(1,1));
	this.add(new PreCoder());
	this.add(new UngerboeckEncoder());
	// take one input from precoder, two from 1/2 trellis encoder
	this.setJoiner(WEIGHTED_ROUND_ROBIN(1,2));
    }
}				 
	    





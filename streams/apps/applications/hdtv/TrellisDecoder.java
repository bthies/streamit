/**
 * A decoder for the trellis encoder present in
 * the HDTV encoding system. The encoder comes directly
 * out of the ATSC standard for HDTV:
 * Standard A53 revision b, which can be found at:
 * http://www.atsc.org/standards/a_53b.pdf
 **/

import streamit.*;

class TrellisDecoder extends SplitJoin {
        public void init() {
	// set up the pipeline (see ATSC standard A53 revision b)
	// switch between de-precoder and trellis decoder
	this.setSplitter(WEIGHTED_ROUND_ROBIN(1,2));
	this.add(new PreCoder());
	this.add(new UngerboeckEncoder());
	// take one input from precoder, two from 1/2 trellis encoder
	this.setJoiner(ROUND_ROBIN());
    }
}



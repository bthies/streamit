package streamit;

// the feedback loop
public class FeedbackLoop extends Stream {

    // constructor with delay left unspecified
    public FeedbackLoop() {
	super();
    }

    // set delay of feedback loop--that is, difference in original
    // stream position between items that arrive to joiner at same time,
    // assuming that all blocks in loop are 1-to-1 (otherwise we need to
    // work at defining exactly what it means).
    public void setDelay(int delay) {};

    // specifies the header
    public Stream header(Joiner s) {
	return null;
    }

    // adds something to the body of the loop
    public Stream add(Stream s) {
	return null;
    }

    // specifies processor in loop, if any
    public Stream loop(Stream s) {
	return null;
    }
}

